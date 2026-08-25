package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.facechanger.faceswap.enhance.controller.AppFaceRevenueCatManager;
import com.facechanger.faceswap.enhance.utils.AppFaceAppSystem;
import com.facechanger.faceswap.enhance.utils.AppFaceInstallReferrerInfo;
import com.facechanger.faceswap.enhance.utils.AppFaceLanguagePrefs;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceStaticValue;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.faceenhance.facechanger.callback.SplashAdCallback;
import com.faceenhance.facechanger.controller.AdManager;
import com.faceenhance.facechanger.controller.SplashInterstitialAdManager;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppFaceAppDialogController;
import com.facechanger.faceswap.enhance.controller.AppFaceOnDialogActionListener;
import com.facechanger.faceswap.enhance.model.api.AppFaceSplashDataResponse;
import com.facechanger.faceswap.enhance.utils.AppFaceApiRepository;
import com.facechanger.faceswap.enhance.utils.AppFaceNetworkUtils;
import com.facechanger.faceswap.enhance.utils.AppFaceSessionManager;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.revenuecat.purchases.UiConfig;

/**
 * Splash screen shown on app launch.
 * <p>
 * First checks for a mandatory app update by comparing the installed version
 * with the latest version on Google Play. If an update is required, navigates
 * to {@link AppFaceAppUpdateActivity} and does NOT proceed to fetch splash data.
 * <p>
 * If the app is up-to-date, calls the {@code POST /api/User/splash_data} API
 * to retrieve the session token and app configuration. On success, stores the
 * session in {@link AppFaceSessionManager} and navigates to {@link AppFaceMainActivity}.
 * <p>
 * Ensures a minimum display time so the branding is visible even if
 * the API responds instantly.
 */
public class AppFaceSplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    private static final String TAG = "SplashActivity";
    private static final long MIN_DISPLAY_MS = 1500;

    private long startTime;
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_splash_screen);
        AppFaceTools.setEdgetoEdge(getWindow(), findViewById(android.R.id.content), false, true);

        startTime = System.currentTimeMillis();

        // Initialise API base URL
        AppFaceApiRepository.initialise();

        // Step 1: Fetch splash data directly
        View root = findViewById(android.R.id.content);
        root.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        fetchSplashData();
                    }
                });
    }

    /**
     * Navigates to the forced update screen and finishes this activity.
     */
    private void navigateToUpdateScreen(String currentVersion, String latestVersion) {
        if (hasNavigated)
            return;
        hasNavigated = true;

        Intent intent = new Intent(this, AppFaceAppUpdateActivity.class);
        intent.putExtra(AppFaceAppUpdateActivity.EXTRA_CURRENT_VERSION, currentVersion);
        intent.putExtra(AppFaceAppUpdateActivity.EXTRA_LATEST_VERSION, latestVersion);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.app_nav_fade_in, R.anim.app_nav_fade_out);
    }

    // ──────────────────────────────────────────────
    // API call
    // ──────────────────────────────────────────────

    boolean isPremium_n = false;


    private void fetchSplashData() {
        if (!AppFaceNetworkUtils.isConnected(this)) {
            Log.w(TAG, "No internet connection on splash.");
            showRetryDialog(
                    R.drawable.app_transparent_close_choose,
                    getString(R.string.app_face_no_internet_title_text),
                    getString(R.string.app_no_internet_desc_text));
            return;
        }

        AppFaceRevenueCatManager.getInstance().checkPremiumStatus(new AppFaceRevenueCatManager.PremiumStatusCallback() {
            @Override
            public void onResult(boolean isPremium) {
                if (isPremium) {
                    isPremium_n = true;
                    Log.d("PremiumCheck", "User has an active subscription.");
                } else {
                    isPremium_n = false;
                    Log.d("PremiumCheck", "User does not have an active subscription.");
                }

                AppFaceSessionManager.getInstance().setPremium(isPremium_n);
                AdManager.getInstance().setPremiumUser(isPremium_n);
            }
        });

        new AppFaceInstallReferrerInfo().getInstallReferrer(AppFaceSplashActivity.this, new AppFaceInstallReferrerInfo.ReferrerCallback() {
            @Override
            public void onReferrerReceived(String referrer) {
                AppFaceApiRepository.fetchSplashData(AppFaceSplashActivity.this, referrer, new AppFaceApiRepository.SplashDataCallback() {
                    @Override
                    public void onSuccess(@NonNull AppFaceSplashDataResponse response, String responseBody) {
                        if (response.isSuccess() && response.getData() != null) {
                            AppFaceSessionManager.getInstance().initialize(response.getData());
                            Log.d(TAG, "Session initialized — token acquired" + responseBody);

                            if (AppFaceAppSystem.isDebugMode()) {
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.APP_EXP, 0);
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.IS_IN_APP_AFT_SPL, 1);
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.IS_PRM_PRC_SHOW, 1);
                            } else {
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.APP_EXP,
                                        response.getData().getApp_exp());
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.IS_IN_APP_AFT_SPL,
                                        response.getData().getIs_in_app_aft_spl());
                                GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.IS_PRM_PRC_SHOW,
                                        response.getData().getIs_prm_prc_show());
                            }

                            AdManager.getInstance().onConfigApiResponse(responseBody);

                            Log.w(TAG, "APP_UPDATE: IS_LATEST" + response.getData().isLatest());

                            if (!AppFaceAppSystem.isDebugMode()) {
                                if (!response.getData().isLatest()) {
                                    String currentVersion = com.facechanger.faceswap.enhance.BuildConfig.VERSION_NAME;
                                    navigateToUpdateScreen(currentVersion, null);
                                } else {
                                    navigateAfterMinDisplayTime();
                                }
                            } else {
                                navigateAfterMinDisplayTime();
                            }


                        } else {

                            Log.e("dsfsd", "2");

                            GlobleMMKVManager.getInstance().putInt(AppFaceStaticValue.APP_EXP, 1);

                            Log.w(TAG, "Splash data returned isSuccess=false: " + response.getMessage());
                            showRetryDialog(R.drawable.app_transparent_close_choose, getString(R.string.app_api_error_title_text),
                                    !response.getMessage().isEmpty()
                                            ? response.getMessage()
                                            : getString(R.string.app_api_error_text));
                        }
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        Log.e(TAG, "Splash data error: " + errorMessage);
                        showRetryDialog(
                                R.drawable.app_transparent_close_choose,
                                getString(R.string.app_api_error_title_text),
                                getString(R.string.app_api_error_text));
                    }
                });
            }
        });
    }

    private void showRetryDialog(int iconRes, String title, String message) {
        if (isFinishing() || isDestroyed())
            return;

        AppFaceAppDialogController.showRetryDialog(
                this,
                iconRes,
                title,
                message,
                getString(R.string.app_api_retry_button_text),
                new AppFaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        fetchSplashData();
                    }

                    @Override
                    public void onDismiss() {
                        finish();
                    }
                });
    }

    // ──────────────────────────────────────────────
    // Navigation
    // ──────────────────────────────────────────────

    /**
     * Navigates to MainActivity after ensuring the splash screen
     * has been displayed for at least {@link #MIN_DISPLAY_MS}.
     */
    private void navigateAfterMinDisplayTime() {

        SplashInterstitialAdManager.getInstance().showSplashAd(this, true, new SplashAdCallback() {
            @Override
            public void onAdFailed(String s) {
                moveToNextScreen();
            }

            @Override
            public void onAdDismiss() {
                moveToNextScreen();
            }
        });

    }

    @Override
    public void onBackPressed() {

    }

    private void moveToNextScreen() {
        Class<?> nextActivity;

        if (AdManager.getInstance().isPremiumUser()) {
            nextActivity = AppFaceMainActivity.class;
        } else {
            if (!AppFaceLanguagePrefs.isOnboardingCompleted(this)) {
                nextActivity = AppFaceOnboardingActivity.class;
            } else {
                int IS_IN_APP_AFT_SPL = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.IS_IN_APP_AFT_SPL, 0);
                if (IS_IN_APP_AFT_SPL == 1) {
                    nextActivity = AppFacePaywallActivity.class;
                } else {
                    nextActivity = AppFaceMainActivity.class;
                }
            }
        }

        Intent intent = new Intent(AppFaceSplashActivity.this, nextActivity);
        intent.putExtra("isFromSplash", true);
        startActivity(intent);
        finish();
    }
}

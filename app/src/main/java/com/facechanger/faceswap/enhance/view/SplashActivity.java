package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.facechanger.faceswap.enhance.controller.RevenueCatManager;
import com.facechanger.faceswap.enhance.utils.AppSystem;
import com.facechanger.faceswap.enhance.utils.InstallReferrerInfo;
import com.facechanger.faceswap.enhance.utils.LanguagePrefs;
import com.facechanger.faceswap.enhance.utils.StaticValue;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.faceenhance.facechanger.callback.SplashAdCallback;
import com.faceenhance.facechanger.controller.AdManager;
import com.faceenhance.facechanger.controller.SplashInterstitialAdManager;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppDialogController;
import com.facechanger.faceswap.enhance.controller.OnDialogActionListener;
import com.facechanger.faceswap.enhance.model.api.SplashDataResponse;
import com.facechanger.faceswap.enhance.utils.ApiRepository;
import com.facechanger.faceswap.enhance.utils.NetworkUtils;
import com.facechanger.faceswap.enhance.utils.SessionManager;
import com.facechanger.faceswap.enhance.utils.Tools;

/**
 * Splash screen shown on app launch.
 * <p>
 * First checks for a mandatory app update by comparing the installed version
 * with the latest version on Google Play. If an update is required, navigates
 * to {@link AppUpdateActivity} and does NOT proceed to fetch splash data.
 * <p>
 * If the app is up-to-date, calls the {@code POST /api/User/splash_data} API
 * to retrieve the session token and app configuration. On success, stores the
 * session in {@link SessionManager} and navigates to {@link MainActivity}.
 * <p>
 * Ensures a minimum display time so the branding is visible even if
 * the API responds instantly.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
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
        Tools.setEdgetoEdge(getWindow(), findViewById(android.R.id.content), false, true);

        startTime = System.currentTimeMillis();

        // Initialise API base URL
        ApiRepository.initialise();

        // Step 1: Fetch splash data directly
        fetchSplashData();
    }
    //
    // @Override
    // public void onBackPressed() {
    //
    // }

    /**
     * Navigates to the forced update screen and finishes this activity.
     */
    private void navigateToUpdateScreen(String currentVersion, String latestVersion) {
        if (hasNavigated)
            return;
        hasNavigated = true;

        Intent intent = new Intent(this, AppUpdateActivity.class);
        intent.putExtra(AppUpdateActivity.EXTRA_CURRENT_VERSION, currentVersion);
        intent.putExtra(AppUpdateActivity.EXTRA_LATEST_VERSION, latestVersion);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
    }

    // ──────────────────────────────────────────────
    // API call
    // ──────────────────────────────────────────────

    boolean isPremium_n = false;


    private void fetchSplashData() {
        if (!NetworkUtils.isConnected(this)) {
            Log.w(TAG, "No internet connection on splash.");
            showRetryDialog(
                    R.drawable.ic_trans_close,
                    getString(R.string.app_face_no_internet_title_text),
                    getString(R.string.app_no_internet_desc_text));
            return;
        }

        RevenueCatManager.getInstance().checkPremiumStatus(new RevenueCatManager.PremiumStatusCallback() {
            @Override
            public void onResult(boolean isPremium) {
                if (isPremium) {
                    isPremium_n = true;
                    Log.d("PremiumCheck", "User has an active subscription.");
                } else {
                    isPremium_n = false;
                    Log.d("PremiumCheck", "User does not have an active subscription.");
                }

                SessionManager.getInstance().setPremium(isPremium_n);
                AdManager.getInstance().setPremiumUser(isPremium_n);
            }
        });

        new InstallReferrerInfo().getInstallReferrer(SplashActivity.this, new InstallReferrerInfo.ReferrerCallback() {
            @Override
            public void onReferrerReceived(String referrer) {
                ApiRepository.fetchSplashData(SplashActivity.this, referrer, new ApiRepository.SplashDataCallback() {
                    @Override
                    public void onSuccess(@NonNull SplashDataResponse response, String responseBody) {
                        if (response.isSuccess() && response.getData() != null) {
                            SessionManager.getInstance().initialize(response.getData());
                            Log.d(TAG, "Session initialized — token acquired" + responseBody);

                            if (AppSystem.isDebugMode()) {
                                GlobleMMKVManager.getInstance().putInt(StaticValue.APP_EXP, 1);
                                GlobleMMKVManager.getInstance().putInt(StaticValue.IS_IN_APP_AFT_SPL, 1);
                                GlobleMMKVManager.getInstance().putInt(StaticValue.IS_PRM_PRC_SHOW, 0);
                            } else {
                                GlobleMMKVManager.getInstance().putInt(StaticValue.APP_EXP,
                                        response.getData().getApp_exp());
                                GlobleMMKVManager.getInstance().putInt(StaticValue.IS_IN_APP_AFT_SPL,
                                        response.getData().getIs_in_app_aft_spl());
                                GlobleMMKVManager.getInstance().putInt(StaticValue.IS_PRM_PRC_SHOW,
                                        response.getData().getIs_prm_prc_show());
                            }

                            AdManager.getInstance().onConfigApiResponse(responseBody);

                            Log.w(TAG, "APP_UPDATE: IS_LATEST" + response.getData().isLatest());

                            if (!response.getData().isLatest()) {
                                String currentVersion = com.facechanger.faceswap.enhance.BuildConfig.VERSION_NAME;
                                navigateToUpdateScreen(currentVersion, null);
                            } else {
                                navigateAfterMinDisplayTime();
                            }
                        } else {

                            Log.e("dsfsd", "2");

                            GlobleMMKVManager.getInstance().putInt(StaticValue.APP_EXP, 1);

                            Log.w(TAG, "Splash data returned isSuccess=false: " + response.getMessage());
                            showRetryDialog(R.drawable.ic_trans_close, getString(R.string.app_api_error_title_text),
                                    response.getMessage() != null && !response.getMessage().isEmpty()
                                            ? response.getMessage()
                                            : getString(R.string.app_api_error_text));
                        }
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        Log.e(TAG, "Splash data error: " + errorMessage);
                        showRetryDialog(
                                R.drawable.ic_trans_close,
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

        AppDialogController.showRetryDialog(
                this,
                iconRes,
                title,
                message,
                getString(R.string.app_api_retry_button_text),
                new OnDialogActionListener() {
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

    private void moveToNextScreen() {
        Class<?> nextActivity;

        if (AdManager.getInstance().isPremiumUser()) {
            nextActivity = MainActivity.class;
        } else {
            if (!LanguagePrefs.isOnboardingCompleted(this)) {
                nextActivity = OnboardingActivity.class;
            } else {
                int IS_IN_APP_AFT_SPL = GlobleMMKVManager.getInstance().getInt(StaticValue.IS_IN_APP_AFT_SPL, 0);
                if (IS_IN_APP_AFT_SPL == 1) {
                    nextActivity = PaywallActivity.class;
                } else {
                    nextActivity = MainActivity.class;
                }
            }
        }

        Intent intent = new Intent(SplashActivity.this, nextActivity);
        intent.putExtra("isFromSplash", true);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
    }
}

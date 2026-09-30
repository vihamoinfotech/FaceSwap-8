package com.perfect.faceeditor.facechanger.io.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceRevCatMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceApiClient;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceAppSystem;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceInstallRefrenceInfo;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLanguagePrfrnce;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceStaticValue;
import com.perfect.faceeditor.facechanger.io.utils.SFS_FierbusAuthMngr;
import com.faceeditor.io.Utils.GlobleMMKVManager;
import com.faceeditor.io.callback.SplashAdCallback;
import com.faceeditor.io.controller.AdManager;
import com.faceeditor.io.controller.SplashInterstitialAdManager;
import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceApDlogController;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceOnDialogActionListener;
import com.perfect.faceeditor.facechanger.io.model.api.SFS_RefaceSplDataResponse;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceApiRepo;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceNetworkUtility;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceSessionMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceUtils;

/**
 * Splash screen shown on app launch.
 * <p>
 * First checks for a mandatory app update by comparing the installed version
 * with the latest version on Google Play. If an update is required, navigates
 * to {@link SFS_RefaceAppUpdateActivity} and does NOT proceed to fetch splash data.
 * <p>
 * If the app is up-to-date, calls the {@code POST /api/User/splash_data} API
 * to retrieve the session token and app configuration. On success, stores the
 * session in {@link SFS_RefaceSessionMngr} and navigates to {@link SFS_RefaceMainActivity}.
 * <p>
 * Ensures a minimum display time so the branding is visible even if
 * the API responds instantly.
 */
public class SFS_RefaceSplActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    private static final String TAG = "SplashActivity";
    private static final long MIN_DISPLAY_MS = 1500;
    private long startTime;
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sfs_face_reactv__spl_view);
        SFS_RefaceUtils.setEdgetoEdge(getWindow(), findViewById(android.R.id.content), true, true);

        startTime = SystemClock.elapsedRealtime();

        // Initialise API base URL
        SFS_RefaceApiRepo.initialise();

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

        Intent intent = new Intent(this, SFS_RefaceAppUpdateActivity.class);
        intent.putExtra(SFS_RefaceAppUpdateActivity.EXTRA_CURRENT_VERSION, currentVersion);
        intent.putExtra(SFS_RefaceAppUpdateActivity.EXTRA_LATEST_VERSION, latestVersion);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.sfs_re_navi_fade_in, R.anim.sfs_re_navi_fade_out);
    }

    // ──────────────────────────────────────────────
    // API call
    // ──────────────────────────────────────────────

    boolean isPremium_n = false;


    private void fetchSplashData() {
        if (!SFS_RefaceNetworkUtility.isConnected(this)) {
            Log.w(TAG, "No internet connection on splash.");
            showRetryDialog(
                    R.drawable.sfs_re_trans_close_choose,
                    getString(R.string.sfs_face_re_no_internet_title_text),
                    getString(R.string.sfs_re_no_internet_desc_text));
            return;
        }

        SFS_RefaceRevCatMngr.getInstance().checkPremiumStatus(new SFS_RefaceRevCatMngr.PremiumStatusCallback() {
            @Override
            public void onResult(boolean isPremium) {
                if (isPremium) {
                    isPremium_n = true;
                    Log.d("PremiumCheck", "User has an active subscription.");
                } else {
                    isPremium_n = false;
                    Log.d("PremiumCheck", "User does not have an active subscription.");
                }

                SFS_RefaceSessionMngr.getInstance().setPremium(isPremium_n);
                AdManager.getInstance().setPremiumUser(isPremium_n);
            }
        });

        new SFS_RefaceInstallRefrenceInfo().getInstallReferrer(SFS_RefaceSplActivity.this, new SFS_RefaceInstallRefrenceInfo.ReferrerCallback() {
            @Override
            public void onReferrerReceived(String referrer) {
                SFS_RefaceApiRepo.fetchSplashData(SFS_RefaceSplActivity.this, referrer, new SFS_RefaceApiRepo.SplashDataCallback() {
                    @Override
                    public void onSuccess(@NonNull SFS_RefaceSplDataResponse response, String responseBody) {
                        if (response.isSuccess() && response.getData() != null) {
                            SFS_RefaceSessionMngr.getInstance().initialize(response.getData());
                            Log.d(TAG, "Session initialized — token acquired" + responseBody);

                            if (SFS_RefaceAppSystem.isDebugMode()) {
                                GlobleMMKVManager.getInstance().putInt(SFS_RefaceStaticValue.APP_EXP, 0);
                                GlobleMMKVManager.getInstance().putInt(SFS_RefaceStaticValue.IS_IN_APP_AFT_SPL, 1);
                                GlobleMMKVManager.getInstance().putInt(SFS_RefaceStaticValue.IS_PRM_PRC_SHOW, 0);
                            } else {
                                GlobleMMKVManager.getInstance().putInt(SFS_RefaceStaticValue.APP_EXP,
                                        response.getData().getApp_exp());
                                GlobleMMKVManager.getInstance().putInt(SFS_RefaceStaticValue.IS_IN_APP_AFT_SPL,
                                        response.getData().getIs_in_app_aft_spl());
                                GlobleMMKVManager.getInstance().putInt(SFS_RefaceStaticValue.IS_PRM_PRC_SHOW,
                                        response.getData().getIs_prm_prc_show());
                            }

                            AdManager.getInstance().onConfigApiResponse(responseBody);

                            Log.w(TAG, "APP_UPDATE: IS_LATEST" + response.getData().isLatest());

                            if (!SFS_RefaceAppSystem.isDebugMode()) {
                                if (!response.getData().isLatest()) {
                                    String currentVersion = com.perfect.faceeditor.facechanger.io.BuildConfig.VERSION_NAME;
                                    navigateToUpdateScreen(currentVersion, null);
                                } else {
                                    navigateAfterMinDisplayTime();
                                }
                            } else {
                                navigateAfterMinDisplayTime();
                            }


                        } else {

                            Log.e("dsfsd", "2");

                            GlobleMMKVManager.getInstance().putInt(SFS_RefaceStaticValue.APP_EXP, 1);

                            Log.w(TAG, "Splash data returned isSuccess=false: " + response.getMessage());
                            showRetryDialog(R.drawable.sfs_re_trans_close_choose, getString(R.string.sfs_re_api_err_title_text),
                                    !response.getMessage().isEmpty()
                                            ? response.getMessage()
                                            : getString(R.string.sfs_re_api_err_text));
                        }
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        Log.e(TAG, "Splash data error: " + errorMessage);
                        showRetryDialog(
                                R.drawable.sfs_re_trans_close_choose,
                                getString(R.string.sfs_re_api_err_title_text),
                                getString(R.string.sfs_re_api_err_text));
                    }
                });
            }
        });
    }

    private void showRetryDialog(int iconRes, String title, String message) {
        if (isFinishing() || isDestroyed())
            return;

        SFS_RefaceApDlogController.showRetryDialog(
                this,
                iconRes,
                title,
                message,
                getString(R.string.sfs_re_api_retry_button_text),
                new SFS_RefaceOnDialogActionListener() {
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
        if (hasNavigated || isFinishing()) return;
        long remainingMs = MIN_DISPLAY_MS - (SystemClock.elapsedRealtime() - startTime);
        if (remainingMs > 0) {
            new Handler(Looper.getMainLooper()).postDelayed(this::navigateAfterMinDisplayTime, remainingMs);
            return;
        }

        int APP_EXP = GlobleMMKVManager.getInstance().getInt(SFS_RefaceStaticValue.APP_EXP, 1);
        if (APP_EXP == 1) {
            moveToNextScreen();
        } else {
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
    }

    @Override
    public void onBackPressed() {

    }

    private void moveToNextScreen() {

        int APP_EXP = GlobleMMKVManager.getInstance().getInt(SFS_RefaceStaticValue.APP_EXP, 1);
        if (APP_EXP == 1) {
            if (SFS_FierbusAuthMngr.getInstance().isLoggedIn()) {
                SFS_FierbusAuthMngr.getInstance().updateUserCoin(SFS_RefaceSessionMngr.getInstance().getCurrentCredits());

                SFS_FierbusAuthMngr.getInstance().fetchUserProfile(
                        new SFS_FierbusAuthMngr.ProfileCallback() {
                            @Override
                            public void onSuccess(@androidx.annotation.NonNull java.util.Map<String, Object> userData) {
                                runOnUiThread(() -> {
                                    syncFirebaseProfileToSession(userData);
                                    navigateToActivity(SFS_RefaceMainActivity.class);
                                });
                            }

                            @Override
                            public void onError(@androidx.annotation.NonNull String errorMessage) {
                                // Even if profile fetch fails, user is logged in, proceed
                                runOnUiThread(() -> navigateToActivity(SFS_RefaceMainActivity.class));
                            }
                        });
            } else {
                navigateToActivity(SFS_LgnActivity.class);
            }
        } else {

            Class<?> nextActivity;

            if (AdManager.getInstance().isPremiumUser()) {
                nextActivity = SFS_RefaceMainActivity.class;
            } else {
                if (!SFS_RefaceLanguagePrfrnce.isOnboardingCompleted(this)) {
                    nextActivity = SFS_RefaceIntroingActivity.class;
                } else {
                    int IS_IN_APP_AFT_SPL = GlobleMMKVManager.getInstance().getInt(SFS_RefaceStaticValue.IS_IN_APP_AFT_SPL, 0);
                    if (IS_IN_APP_AFT_SPL == 1) {
                        nextActivity = SFS_RefacePremiumActivity.class;
                    } else {
                        nextActivity = SFS_RefaceMainActivity.class;
                    }
                }
            }

            Intent intent = new Intent(SFS_RefaceSplActivity.this, nextActivity);
            intent.putExtra("isFromSplash", true);
            startActivity(intent);
            finish();
        }

    }

    private void syncFirebaseProfileToSession(java.util.Map<String, Object> userData) {
        try {
            Object tokenObj = userData.get("getToken");
            String token = tokenObj != null ? String.valueOf(tokenObj) : "";

            Object userIdObj = userData.get("userId");
            String userId = userIdObj != null ? String.valueOf(userIdObj) : "";

            Object remainObj = userData.get("remainingLimit");
            double remainingLimit = 0.0;
            if (remainObj instanceof Number) {
                remainingLimit = ((Number) remainObj).doubleValue();
            }

            if (!token.isEmpty()) {
                SFS_RefaceApiClient.getInstance().setAuthToken(token);
            }
            SFS_RefaceSessionMngr.getInstance().setCachedCredits(remainingLimit);

            Log.d(TAG, "Firebase profile synced to session");
        } catch (Exception e) {
            Log.e(TAG, "Error syncing Firebase profile", e);
        }
    }

    private void navigateToActivity(Class<?> activityClass) {
        if (hasNavigated) return;
        hasNavigated = true;

        Intent intent = new Intent(SFS_RefaceSplActivity.this, activityClass);
        intent.putExtra("isFromSplash", true);
        startActivity(intent);
        finish();
    }
}

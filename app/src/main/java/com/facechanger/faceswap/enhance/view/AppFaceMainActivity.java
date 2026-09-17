package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facechanger.faceswap.enhance.utils.AppFaceApiClient;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceStaticValue;
import com.facechanger.faceswap.enhance.utils.FirebaseAuthManager;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.AppFaceAppSystem;
import com.facechanger.faceswap.enhance.utils.AppFaceCoinManager;
import com.facechanger.faceswap.enhance.utils.AppFaceSessionManager;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.faceenhance.facechanger.controller.AdManager;
import com.faceenhance.facechanger.controller.FirebaseManager;
import com.izooto.iZooto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Home screen — "Feature Hub" layout.
 * <p>
 * Displays a hero banner with Face Swap + AI Create CTAs,
 * an Explore Templates entry card, and a 2×2 Quick Actions grid
 * (Video Swap, Remove BG, Enhance, My Work).
 * Templates are now in {@link AppFaceTemplateGalleryActivity}.
 */
public class AppFaceMainActivity extends BaseAppActivity {

    private static final String TAG = "MainActivity";


    private View coinBalancePill;
    private android.widget.TextView tvCoinBalance;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_main_screen);

        AppFaceTools.setEdgetoEdge(getWindow(), findViewById(R.id.mainContent), true, false);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            private long backPressedTime = 0;
            private Toast backToast;

            @Override
            public void handleOnBackPressed() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - backPressedTime < 2000) {
                    if (backToast != null) {
                        backToast.cancel();
                    }

                    int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);
                    if (APP_EXP == 0) {
                        showDirectInterstitial(new InterstitialAdCallback() {
                            @Override
                            public void onAdDismissed() {
                                finishAffinity();
                            }
                        });
                    } else {
                        finishAffinity();
                    }
                } else {
                    backPressedTime = currentTime;
                    backToast = Toast.makeText(AppFaceMainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT);
                    backToast.show();
                }
            }
        });

        setupFeatureHub();
        setupClickListeners();
        setupCoinHeader();

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
                loadSecondAds();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        adjustLayoutForAds();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                iZooto.setSubscription(true);
            } else {
                iZooto.promptForPushNotifications();
            }
        }

        syncFirebaseProfileIfNeeded();
    }

    private void syncFirebaseProfileIfNeeded() {
        int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);

        if (APP_EXP != 1) return;

        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance();

        if (!authManager.isLoggedIn()) return;

        // Update Firebase with latest splash data
        String currentToken = AppFaceSessionManager.getInstance().getToken();
        String currentUserId = AppFaceSessionManager.getInstance().getUserId();
        String currentDeviceId = AppFaceSessionManager.getInstance().getDeviceId();
        double currentCredits = AppFaceSessionManager.getInstance().getCurrentCredits();

        if (!currentToken.isEmpty()) {
            authManager.updateSplashData(currentToken, currentUserId, currentDeviceId, currentCredits);
        }

        // Fetch and sync profile from Firebase
        authManager.fetchUserProfile(new FirebaseAuthManager.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull java.util.Map<String, Object> userData) {
                runOnUiThread(() -> {
                    try {
                        Object tokenObj = userData.get("getToken");
                        String token = tokenObj != null ? String.valueOf(tokenObj) : "";

                        Object remainObj = userData.get("remainingLimit");
                        double remainingLimit = 0.0;
                        if (remainObj instanceof Number) {
                            remainingLimit = ((Number) remainObj).doubleValue();
                        }

                        if (!token.isEmpty()) {
                            AppFaceApiClient.getInstance().setAuthToken(token);
                        }
                        AppFaceSessionManager.getInstance().setCachedCredits(remainingLimit);
                        refreshCoinBalance();

                        Log.d(TAG, "Firebase profile synced in Dashboard");
                    } catch (Exception e) {
                        Log.e(TAG, "Error syncing Firebase profile in Dashboard", e);
                    }
                });
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.w(TAG, "Failed to sync Firebase profile: " + errorMessage);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCoinBalance();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // ──────────────────────────────────────────────
    //  UI Setup
    // ──────────────────────────────────────────────

    private void setupFeatureHub() {
        // Hero Banner CTAs
        View btnFaceSwap = findViewById(R.id.btnFaceSwap);
        if (btnFaceSwap != null) {
            btnFaceSwap.setOnClickListener(v -> onClickInterstitial(true, () -> {
                Intent intent = new Intent(AppFaceMainActivity.this, AppFaceSwapActivity.class);
                intent.putExtra("is_edit_image", false);
                startActivity(intent);
            }));
        }

        View btnAiCreate = findViewById(R.id.btnAiCreate);
        if (btnAiCreate != null) {
            btnAiCreate.setOnClickListener(v -> onClickInterstitial(true, () -> {
                Intent intent = new Intent(AppFaceMainActivity.this, AppFaceAiImageGenActivity.class);
                startActivity(intent);
            }));
        }

        // Explore Templates Card
        View cardExploreTemplates = findViewById(R.id.cardExploreTemplates);
        if (cardExploreTemplates != null) {
            cardExploreTemplates.setOnClickListener(v -> onClickInterstitial(true, () -> {
                Intent intent = new Intent(AppFaceMainActivity.this, AppFaceTemplateGalleryActivity.class);
                startActivity(intent);
            }));
        }

        // Quick Actions
        View cardVideoSwap = findViewById(R.id.cardVideoSwap);
        if (cardVideoSwap != null) {
            cardVideoSwap.setOnClickListener(v -> {
                if (FirebaseAuthManager.getInstance().isLoggedIn()) {
                    Intent intent = new Intent(AppFaceMainActivity.this, AppFaceVideoFaceSwapActivity.class);
                    startActivity(intent);
                } else {
                    if (AdManager.getInstance().isPremiumUser()) {
                        FirebaseManager.getInstance().logEvent("VIDEO_OPEN");
                        Intent intent = new Intent(AppFaceMainActivity.this, AppFaceVideoFaceSwapActivity.class);
                        startActivity(intent);
                    } else {
                        FirebaseManager.getInstance().logEvent("VIDEO_PREMIUM_OPEN");
                        int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);
                        if (APP_EXP == 1) {
                            startActivity(new Intent(this, AppFacePaywallActivity.class));
                        } else {
                            startActivity(new Intent(this, VideoPaywallActivity.class));
                        }
                    }
                }
            });
        }

        View cardRemoveBg = findViewById(R.id.cardRemoveBg);
        if (cardRemoveBg != null) {
            cardRemoveBg.setOnClickListener(v -> onClickInterstitial(true, () -> {
                Intent intent = new Intent(AppFaceMainActivity.this, AppFaceRemoveBgActivity.class);
                startActivity(intent);
            }));
        }

        View cardEnhanceFace = findViewById(R.id.cardEnhanceFace);
        if (cardEnhanceFace != null) {
            cardEnhanceFace.setOnClickListener(v -> onClickInterstitial(true, () -> {
                Intent intent = new Intent(AppFaceMainActivity.this, AppFaceFaceEnhanceActivity.class);
                startActivity(intent);
            }));
        }

        View cardMyWork = findViewById(R.id.cardMyWork);
        if (cardMyWork != null) {
            cardMyWork.setOnClickListener(v -> onClickInterstitial(true, () -> {
                Intent intent = new Intent(AppFaceMainActivity.this, AppFaceMyWorkActivity.class);
                startActivity(intent);
            }));
        }

        // Sparkle animation for Hero Banner

        View ivSparkle = findViewById(R.id.ivSparkle);
        if (ivSparkle != null) {
            android.animation.ObjectAnimator alphaAnim = android.animation.ObjectAnimator.ofFloat(ivSparkle, "alpha", 1f, 0.4f);
            alphaAnim.setDuration(1200);
            alphaAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            alphaAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);

            android.animation.ObjectAnimator scaleXAnim = android.animation.ObjectAnimator.ofFloat(ivSparkle, "scaleX", 1f, 1.2f);
            scaleXAnim.setDuration(1200);
            scaleXAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleXAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);

            android.animation.ObjectAnimator scaleYAnim = android.animation.ObjectAnimator.ofFloat(ivSparkle, "scaleY", 1f, 1.2f);
            scaleYAnim.setDuration(1200);
            scaleYAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleYAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);

            android.animation.AnimatorSet sparkleSet = new android.animation.AnimatorSet();
            sparkleSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim);
            sparkleSet.start();
        }
    }

    private void setupClickListeners() {
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, AppFaceSettingsActivity.class);
                startActivity(intent);
            });
        }
    }


    // ──────────────────────────────────────────────
    //  Coin Balance Header
    // ──────────────────────────────────────────────

    private void setupCoinHeader() {
        coinBalancePill = findViewById(R.id.coinBalancePill);
        tvCoinBalance = findViewById(R.id.tvCoinBalance);

        if (coinBalancePill != null) {
            coinBalancePill.setOnClickListener(v -> {
                Intent intent = new Intent(this, AppFaceStoreActivity.class);
                startActivity(intent);
            });
        }

        refreshCoinBalance();
    }

    private void refreshCoinBalance() {
        if (!AppFaceAppSystem.isFeatureEnabled(AppFaceAppSystem.KEY_COIN_SYSTEM_ENABLED)
                || !AppFaceAppSystem.isFeatureEnabled(AppFaceAppSystem.KEY_SHOW_COIN_BALANCE_HEADER)) {
            if (coinBalancePill != null) coinBalancePill.setVisibility(View.GONE);
            return;
        }

        if (coinBalancePill != null) {
            coinBalancePill.setVisibility(View.VISIBLE);
        }
        if (tvCoinBalance != null) {
            double balance = AppFaceSessionManager.getInstance().getCurrentCredits();
            tvCoinBalance.setText(AppFaceCoinManager.formatCost(balance));
        }

    }

    private void adjustLayoutForAds() {
        boolean adsFeatureEnabled = false;
        if (AppFaceSessionManager.getInstance().getConfig() != null) {
            adsFeatureEnabled = AppFaceSessionManager.getInstance().getConfig().isAdsEnable();
        }

        if (!adsFeatureEnabled) {
            View adContainer = findViewById(R.id.ad_view_container);
            if (adContainer != null) adContainer.setVisibility(View.GONE);
            View secondAdContainer = findViewById(R.id.second_ad_view_container);
            if (secondAdContainer != null) secondAdContainer.setVisibility(View.GONE);
        }
    }
}

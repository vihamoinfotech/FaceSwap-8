package com.perfect.faceeditor.facechanger.io.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceApiClient;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceStaticValue;
import com.perfect.faceeditor.facechanger.io.utils.SFS_FierbusAuthMngr;
import com.faceeditor.io.Utils.GlobleMMKVManager;
import com.faceeditor.io.callback.InterstitialAdCallback;
import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceAppSystem;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceCoinMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceSessionMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceUtils;
import com.faceeditor.io.controller.AdManager;
import com.faceeditor.io.controller.FirebaseManager;


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
 * Templates are now in {@link SFS_RefacePosterGalleryActivity}.
 */
public class SFS_RefaceMainActivity extends SFS_BaseAppActivity {

    private static final String TAG = "MainActivity";


    private View coinBalancePill;
    private android.widget.TextView tvCoinBalance;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sfs_face_reactv__main_view);

        SFS_RefaceUtils.setEdgetoEdge(getWindow(), findViewById(R.id.mainContent), true, false);

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

                    int APP_EXP = GlobleMMKVManager.getInstance().getInt(SFS_RefaceStaticValue.APP_EXP, 1);
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
                    backToast = Toast.makeText(SFS_RefaceMainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT);
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
        syncFirebaseProfileIfNeeded();
    }

    private void syncFirebaseProfileIfNeeded() {
        int APP_EXP = GlobleMMKVManager.getInstance().getInt(SFS_RefaceStaticValue.APP_EXP, 1);

        if (APP_EXP != 1) return;

        SFS_FierbusAuthMngr authManager = SFS_FierbusAuthMngr.getInstance();

        if (!authManager.isLoggedIn()) return;

        // Update Firebase with latest splash data
        String currentToken = SFS_RefaceSessionMngr.getInstance().getToken();
        String currentUserId = SFS_RefaceSessionMngr.getInstance().getUserId();
        String currentDeviceId = SFS_RefaceSessionMngr.getInstance().getDeviceId();
        double currentCredits = SFS_RefaceSessionMngr.getInstance().getCurrentCredits();

        if (!currentToken.isEmpty()) {
            authManager.updateSplashData(currentToken, currentUserId, currentDeviceId, currentCredits);
        }

        // Fetch and sync profile from Firebase
        authManager.fetchUserProfile(new SFS_FierbusAuthMngr.ProfileCallback() {
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
                            SFS_RefaceApiClient.getInstance().setAuthToken(token);
                        }
                        SFS_RefaceSessionMngr.getInstance().setCachedCredits(remainingLimit);
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
                Intent intent = new Intent(SFS_RefaceMainActivity.this, SFS_RefaceSwapActivity.class);
                intent.putExtra("is_edit_image", false);
                startActivity(intent);
            }));
        }

        View btnAiCreate = findViewById(R.id.btnAiCreate);
        if (btnAiCreate != null) {
            if (SFS_FierbusAuthMngr.getInstance().isLoggedIn()) {
                btnAiCreate.setVisibility(View.GONE);
            } else {
                btnAiCreate.setVisibility(View.VISIBLE);
                btnAiCreate.setOnClickListener(v -> onClickInterstitial(true, () -> {
                    Intent intent = new Intent(SFS_RefaceMainActivity.this, SFS_RefaceAiPhotoGenActivity.class);
                    startActivity(intent);
                }));
            }

        }

        // Explore Templates Card
        View cardExploreTemplates = findViewById(R.id.cardExploreTemplates);
        if (cardExploreTemplates != null) {
            cardExploreTemplates.setOnClickListener(v -> onClickInterstitial(true, () -> {
                Intent intent = new Intent(SFS_RefaceMainActivity.this, SFS_RefacePosterGalleryActivity.class);
                startActivity(intent);
            }));
        }

        // Quick Actions
        View cardVideoSwap = findViewById(R.id.cardVideoSwap);
        if (cardVideoSwap != null) {
            cardVideoSwap.setOnClickListener(v -> {
                if (SFS_FierbusAuthMngr.getInstance().isLoggedIn()) {
                    Intent intent = new Intent(SFS_RefaceMainActivity.this, SFS_RefaceMovieFaceSwapActivity.class);
                    startActivity(intent);
                } else {
                    if (AdManager.getInstance().isPremiumUser()) {
                        FirebaseManager.getInstance().logEvent("VIDEO_OPEN");
                        Intent intent = new Intent(SFS_RefaceMainActivity.this, SFS_RefaceMovieFaceSwapActivity.class);
                        startActivity(intent);
                    } else {
                        FirebaseManager.getInstance().logEvent("VIDEO_PREMIUM_OPEN");
                        int APP_EXP = GlobleMMKVManager.getInstance().getInt(SFS_RefaceStaticValue.APP_EXP, 1);
                        if (APP_EXP == 1) {
                            startActivity(new Intent(this, SFS_RefacePremiumActivity.class));
                        } else {
                            startActivity(new Intent(this, SFS_MoviePremiumActivity.class));
                        }
                    }
                }
            });
        }

        View cardRemoveBg = findViewById(R.id.cardRemoveBg);
        if (cardRemoveBg != null) {
            cardRemoveBg.setOnClickListener(v -> onClickInterstitial(true, () -> {
                Intent intent = new Intent(SFS_RefaceMainActivity.this, SFS_RefaceRemoveBgActivity.class);
                startActivity(intent);
            }));
        }

        View cardEnhanceFace = findViewById(R.id.cardEnhanceFace);
        if (cardEnhanceFace != null) {
            cardEnhanceFace.setOnClickListener(v -> onClickInterstitial(true, () -> {
                Intent intent = new Intent(SFS_RefaceMainActivity.this, SFS_RefaceFaceEnhanceActivity.class);
                startActivity(intent);
            }));
        }

        View cardMyWork = findViewById(R.id.cardMyWork);
        if (cardMyWork != null) {
            cardMyWork.setOnClickListener(v -> onClickInterstitial(true, () -> {
                Intent intent = new Intent(SFS_RefaceMainActivity.this, SFS_RefaceMyWorkActivity.class);
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
                Intent intent = new Intent(this, SFS_RefaceSettingsActivity.class);
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
                Intent intent = new Intent(this, SFS_RefaceCoinStreActivity.class);
                startActivity(intent);
            });
        }

        refreshCoinBalance();
    }

    private void refreshCoinBalance() {
        if (!SFS_RefaceAppSystem.isFeatureEnabled(SFS_RefaceAppSystem.KEY_COIN_SYSTEM_ENABLED)
                || !SFS_RefaceAppSystem.isFeatureEnabled(SFS_RefaceAppSystem.KEY_SHOW_COIN_BALANCE_HEADER)) {
            if (coinBalancePill != null) coinBalancePill.setVisibility(View.GONE);
            return;
        }

        if (coinBalancePill != null) {
            coinBalancePill.setVisibility(View.VISIBLE);
        }
        if (tvCoinBalance != null) {
            double balance = SFS_RefaceSessionMngr.getInstance().getCurrentCredits();
            tvCoinBalance.setText(SFS_RefaceCoinMngr.formatCost(balance));
        }

    }

    private void adjustLayoutForAds() {
        boolean adsFeatureEnabled = false;
        if (SFS_RefaceSessionMngr.getInstance().getConfig() != null) {
            adsFeatureEnabled = SFS_RefaceSessionMngr.getInstance().getConfig().isAdsEnable();
        }

        if (!adsFeatureEnabled) {
            View adContainer = findViewById(R.id.ad_view_container);
            if (adContainer != null) adContainer.setVisibility(View.GONE);
            View secondAdContainer = findViewById(R.id.second_ad_view_container);
            if (secondAdContainer != null) secondAdContainer.setVisibility(View.GONE);
        }
    }
}

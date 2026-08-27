package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facechanger.faceswap.enhance.utils.AppFaceApiCall;
import com.facechanger.faceswap.enhance.utils.AppFaceApiClient;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceNetworkUtils;
import com.facechanger.faceswap.enhance.utils.AppFaceStaticValue;
import com.facechanger.faceswap.enhance.utils.FirebaseAuthManager;
import com.facechanger.faceswap.enhance.view.adapter.AppFaceMainCategoryAdapter;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.model.api.AppFaceTemplateCategory;
import com.facechanger.faceswap.enhance.utils.AppFaceApiRepository;
import com.facechanger.faceswap.enhance.utils.AppFaceAppSystem;
import com.facechanger.faceswap.enhance.utils.AppFaceCoinManager;
import com.facechanger.faceswap.enhance.utils.AppFaceSessionManager;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.faceenhance.facechanger.controller.AdManager;
import com.faceenhance.facechanger.controller.FirebaseManager;
import com.google.firebase.Firebase;
import com.izooto.iZooto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * Home screen displaying categorized face-swap templates.
 * <p>
 * Templates are fetched from {@code POST /api/templates/list} on launch,
 * grouped by category, and rendered as horizontal image sections.
 * Template images are loaded via {@code GET /api/templates/{id}/image}
 * with Bearer auth headers.
 */
public class AppFaceMainActivity extends BaseAppActivity {

    private static final String TAG = "MainActivity";

    private androidx.recyclerview.widget.RecyclerView rvMainCategories;
    private AppFaceMainCategoryAdapter adapter;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerViewContainer;

    private View coinBalancePill;
    private android.widget.TextView tvCoinBalance;

    private AppFaceApiCall fetchTemplatesCall;
    private View llErrorRetryContainer;

    private android.widget.TextView tvErrorTitle, tvErrorMessage, btnRetryMain;


    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.app_face_activity_main_screen);

        AppFaceTools.setEdgetoEdge(getWindow(), findViewById(R.id.mainContent), false, false);

        shimmerViewContainer = findViewById(R.id.shimmer_view_container);
        llErrorRetryContainer = findViewById(R.id.llErrorRetryContainer);

        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnRetryMain = findViewById(R.id.btnRetryMain);

        if (btnRetryMain != null) {
            btnRetryMain.setOnClickListener(v -> fetchTemplatesFromApi());
        }

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


        rvMainCategories = findViewById(R.id.rvMainCategories);
        rvMainCategories.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        adapter = new AppFaceMainCategoryAdapter(this);
        adapter.setHeaderClickListener(() -> {
            onClickInterstitial(true, new InterstitialAdCallback() {
                @Override
                public void onAdDismissed() {
                    Intent intent = new Intent(AppFaceMainActivity.this, AppFaceSwapActivity.class);
                    intent.putExtra("is_edit_image", false);
                    startActivity(intent);
                }
            });
        });

        rvMainCategories.setAdapter(adapter);

        setupBottomNav();
        setupClickListeners();
        setupCoinHeader();
        fetchTemplatesFromApi();

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
        if (fetchTemplatesCall != null) {
            fetchTemplatesCall.cancel();
        }
        super.onDestroy();
    }

    // ──────────────────────────────────────────────
    //  API — fetch templates
    // ──────────────────────────────────────────────

    private void fetchTemplatesFromApi() {
        if (llErrorRetryContainer != null) {
            llErrorRetryContainer.setVisibility(View.GONE);
        }
        if (shimmerViewContainer != null) {
            shimmerViewContainer.setVisibility(View.VISIBLE);
            shimmerViewContainer.startShimmer();
        }
        if (rvMainCategories != null) rvMainCategories.setVisibility(View.GONE);

        fetchTemplatesCall = AppFaceApiRepository.fetchTemplates(new AppFaceApiRepository.TemplatesCallback() {
            @Override
            public void onSuccess(@NonNull List<AppFaceTemplateCategory> categories) {
                fetchTemplatesCall = null;
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }

                if (categories.isEmpty()) {
                    // No data found -> show retry container
                    if (rvMainCategories != null) rvMainCategories.setVisibility(View.GONE);
                    if (llErrorRetryContainer != null) {
                        if (tvErrorTitle != null) tvErrorTitle.setText(getString(R.string.MainActivity_could_not_load_templates));
                        if (tvErrorMessage != null) tvErrorMessage.setText(getString(R.string.dialog_api_error_msg));
                        llErrorRetryContainer.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (llErrorRetryContainer != null) llErrorRetryContainer.setVisibility(View.GONE);
                    if (rvMainCategories != null) rvMainCategories.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Loaded " + categories.size() + " categories from API");
                    renderTemplates(categories);
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                fetchTemplatesCall = null;
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }
                if (rvMainCategories != null) rvMainCategories.setVisibility(View.GONE);

                if (llErrorRetryContainer != null) {
                    if (tvErrorTitle != null) tvErrorTitle.setText(getString(R.string.dialog_api_error_title));
                    if (tvErrorMessage != null) {
                        tvErrorMessage.setText(AppFaceNetworkUtils.isConnected(AppFaceMainActivity.this)
                                ? getString(R.string.dialog_api_error_msg)
                                : getString(R.string.dialog_no_internet_msg));
                    }
                    llErrorRetryContainer.setVisibility(View.VISIBLE);
                }


                Log.e(TAG, "Failed to load templates: " + errorMessage);
            }
        });
    }

    // ──────────────────────────────────────────────
    //  Render template sections
    // ──────────────────────────────────────────────

    private void renderTemplates(@NonNull List<AppFaceTemplateCategory> categories) {
        if (categories.isEmpty()) {
            Log.w(TAG, "No template categories to display");
            return;
        }

        // Pass the categories to the adapter
        adapter.setCategories(categories);
    }

    // ──────────────────────────────────────────────
    //  UI Setup
    // ──────────────────────────────────────────────

    private void setupClickListeners() {
        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, AppFaceSettingsActivity.class);
                startActivity(intent);
            });
        }

        View btnGenerateNow = findViewById(R.id.btnGenerateNow);
        if (btnGenerateNow != null) {
            btnGenerateNow.setOnClickListener(v -> {
                onClickInterstitial(true, new InterstitialAdCallback() {
                    @Override
                    public void onAdDismissed() {

                        // FaceSwap Action Below

                        Intent intent = new Intent(AppFaceMainActivity.this, AppFaceSwapActivity.class);
                        intent.putExtra("is_edit_image", false);
                        startActivity(intent);

                        // AIImage action Below

//                        Intent intent = new Intent(MainActivity.this, AiImageGenActivity.class);
//                        startActivity(intent);
                    }
                });
            });
        }
    }

    private void setupBottomNav() {

        View aiImage = findViewById(R.id.navAiImage);
        if (aiImage != null) {
            aiImage.setOnClickListener(v -> {
                onClickInterstitial(true, new InterstitialAdCallback() {
                    @Override
                    public void onAdDismissed() {
                        Intent intent = new Intent(AppFaceMainActivity.this, AppFaceAiImageGenActivity.class);
                        startActivity(intent);
                    }
                });
            });
        }

        View navFaceSwap = findViewById(R.id.navFaceSwap);
        if (navFaceSwap != null) {
            navFaceSwap.setOnClickListener(v -> {
                onClickInterstitial(true, new InterstitialAdCallback() {
                    @Override
                    public void onAdDismissed() {
                        Intent intent = new Intent(AppFaceMainActivity.this, AppFaceSwapActivity.class);
                        intent.putExtra("is_edit_image", false);
                        startActivity(intent);
                    }
                });
            });

        }

        View navMultiSwap = findViewById(R.id.navMultiSwap);
        if (navMultiSwap != null) {
            navMultiSwap.setOnClickListener(v -> {
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

        View navMyWork = findViewById(R.id.navMyWork);
        if (navMyWork != null) {
            navMyWork.setOnClickListener(v -> {
                onClickInterstitial(true, new InterstitialAdCallback() {
                    @Override
                    public void onAdDismissed() {
                        Intent intent = new Intent(AppFaceMainActivity.this, AppFaceMyWorkActivity.class);
                        startActivity(intent);
                    }
                });
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
        boolean isPremium = AppFaceSessionManager.getInstance().isPremium();
        boolean adsFeatureEnabled = false;
        if (AppFaceSessionManager.getInstance().getConfig() != null) {
            adsFeatureEnabled = AppFaceSessionManager.getInstance().getConfig().isAdsEnable();
        }

        boolean showAds = adsFeatureEnabled;

        if (!showAds) {
            // Reduce bottom padding when no ads are shown
            int paddingBottom = getResources().getDimensionPixelSize(R.dimen.app_clip_home_main_scroll_content_bottom_padding_points_no_ads);
            if (rvMainCategories != null) {
                rvMainCategories.setPadding(
                    rvMainCategories.getPaddingLeft(),
                    rvMainCategories.getPaddingTop(),
                    rvMainCategories.getPaddingRight(),
                    paddingBottom
                );
            }
        }
    }
}

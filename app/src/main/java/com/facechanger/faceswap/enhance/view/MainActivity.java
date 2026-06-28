package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.facechanger.faceswap.enhance.controller.AppDialogController;
import com.facechanger.faceswap.enhance.controller.OnCoinDialogListener;
import com.facechanger.faceswap.enhance.utils.ImagePickerHelper;
import com.facechanger.faceswap.enhance.utils.PermissionHelper;
import com.faceenhance.facechanger.activity.BaseAdActivity;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.model.api.TemplateCategory;
import com.facechanger.faceswap.enhance.utils.ApiRepository;
import com.facechanger.faceswap.enhance.utils.AppSystem;
import com.facechanger.faceswap.enhance.utils.CoinManager;
import com.facechanger.faceswap.enhance.utils.RewardedAdHelper;
import com.facechanger.faceswap.enhance.utils.SessionManager;
import com.facechanger.faceswap.enhance.utils.Tools;
import com.faceenhance.facechanger.controller.AdManager;

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
public class MainActivity extends BaseAppActivity {

    private static final String TAG = "MainActivity";

    private androidx.recyclerview.widget.RecyclerView rvMainCategories;
    private com.facechanger.faceswap.enhance.view.adapter.MainCategoryAdapter adapter;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerViewContainer;

    private View coinBalancePill;
    private android.widget.TextView tvCoinBalance;

    private com.facechanger.faceswap.enhance.utils.ApiCall fetchTemplatesCall;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        Tools.setEdgetoEdge(getWindow(), findViewById(R.id.mainContent), false, true);

        shimmerViewContainer = findViewById(R.id.shimmer_view_container);

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
                    finishAffinity();
                } else {
                    backPressedTime = currentTime;
                    backToast = Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT);
                    backToast.show();
                }
            }
        });


        rvMainCategories = findViewById(R.id.rvMainCategories);
        rvMainCategories.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        adapter = new com.facechanger.faceswap.enhance.view.adapter.MainCategoryAdapter(this);
        adapter.setHeaderClickListener(() -> {
            onClickInterstitial(true, new InterstitialAdCallback() {
                @Override
                public void onAdDismissed() {
                    Intent intent = new Intent(MainActivity.this, FaceSwapActivity.class);
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

        loadAds();
        loadSecondAds();

        // Pre-load rewarded ad for coin-gated features
        RewardedAdHelper.preload(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        adjustLayoutForAds();
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
        if (shimmerViewContainer != null) {
            shimmerViewContainer.setVisibility(View.VISIBLE);
            shimmerViewContainer.startShimmer();
        }
        if (rvMainCategories != null) rvMainCategories.setVisibility(View.GONE);

        fetchTemplatesCall = ApiRepository.fetchTemplates(new ApiRepository.TemplatesCallback() {
            @Override
            public void onSuccess(@NonNull List<TemplateCategory> categories) {
                fetchTemplatesCall = null;
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }
                if (rvMainCategories != null) rvMainCategories.setVisibility(View.VISIBLE);

                Log.d(TAG, "Loaded " + categories.size() + " categories from API");
                renderTemplates(categories);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                fetchTemplatesCall = null;
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }
                if (rvMainCategories != null) rvMainCategories.setVisibility(View.VISIBLE);

                Log.e(TAG, "Failed to load templates: " + errorMessage);
                Toast.makeText(MainActivity.this, getString(R.string.MainActivity_could_not_load_templates),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ──────────────────────────────────────────────
    //  Render template sections
    // ──────────────────────────────────────────────

    private void renderTemplates(@NonNull List<TemplateCategory> categories) {
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
                Intent intent = new Intent(this, SettingsActivity.class);
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

                        Intent intent = new Intent(MainActivity.this, FaceSwapActivity.class);
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
                        Intent intent = new Intent(MainActivity.this, AiImageGenActivity.class);
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
                        Intent intent = new Intent(MainActivity.this, FaceSwapActivity.class);
                        intent.putExtra("is_edit_image", false);
                        startActivity(intent);
                    }
                });
            });

        }

        View navMultiSwap = findViewById(R.id.navMultiSwap);
        if (navMultiSwap != null) {
            navMultiSwap.setOnClickListener(v -> {
                if (AdManager.getInstance().isPremiumUser()) {
                    Intent intent = new Intent(MainActivity.this, VideoFaceSwapActivity.class);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(this, PaywallActivity.class));
                }
            });

        }

        View navMyWork = findViewById(R.id.navMyWork);
        if (navMyWork != null) {
            navMyWork.setOnClickListener(v -> {
                onClickInterstitial(true, new InterstitialAdCallback() {
                    @Override
                    public void onAdDismissed() {
                        Intent intent = new Intent(MainActivity.this, MyWorkActivity.class);
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
                Intent intent = new Intent(this, StoreActivity.class);
                startActivity(intent);
            });
        }

        refreshCoinBalance();
    }

    private void refreshCoinBalance() {
        if (!AppSystem.isFeatureEnabled(AppSystem.KEY_COIN_SYSTEM_ENABLED)
                || !AppSystem.isFeatureEnabled(AppSystem.KEY_SHOW_COIN_BALANCE_HEADER)) {
            if (coinBalancePill != null) coinBalancePill.setVisibility(View.GONE);
            return;
        }

        if (coinBalancePill != null) {
            coinBalancePill.setVisibility(View.VISIBLE);
        }
        if (tvCoinBalance != null) {
            double balance = SessionManager.getInstance().getCurrentCredits();
            tvCoinBalance.setText(CoinManager.formatCost(balance));
        }

    }

    private void adjustLayoutForAds() {
        boolean isPremium = SessionManager.getInstance().isPremium();
        boolean adsFeatureEnabled = false;
        if (SessionManager.getInstance().getConfig() != null) {
            adsFeatureEnabled = SessionManager.getInstance().getConfig().isAdsEnable();
        }

        boolean showAds = adsFeatureEnabled;

        if (!showAds) {
            // Reduce bottom padding when no ads are shown
            int paddingBottom = getResources().getDimensionPixelSize(R.dimen.main_scroll_content_bottom_padding_no_ads);
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

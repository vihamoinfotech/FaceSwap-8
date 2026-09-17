package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.model.api.AppFaceTemplateCategory;
import com.facechanger.faceswap.enhance.utils.AppFaceApiCall;
import com.facechanger.faceswap.enhance.utils.AppFaceApiRepository;
import com.facechanger.faceswap.enhance.utils.AppFaceAppSystem;
import com.facechanger.faceswap.enhance.utils.AppFaceCoinManager;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceNetworkUtils;
import com.facechanger.faceswap.enhance.utils.AppFaceSessionManager;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.facechanger.faceswap.enhance.view.adapter.AppFaceGalleryCategoryAdapter;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;

import android.widget.TextView;

import java.util.List;

/**
 * Displays template categories fetched from the API.
 * This activity was extracted from the home screen to keep the
 * main screen as a clean "Feature Hub".
 *
 * Templates are fetched via {@code POST /api/templates/list},
 * grouped by category, and rendered as horizontal image sections.
 */
public class AppFaceTemplateGalleryActivity extends BaseAppActivity {

    private static final String TAG = "TemplateGallery";

    private androidx.recyclerview.widget.RecyclerView rvGalleryCategories;
    private AppFaceGalleryCategoryAdapter adapter;
    private ShimmerFrameLayout shimmerViewContainer;

    private AppFaceApiCall fetchTemplatesCall;
    private View llErrorRetryContainer;
    private TextView tvErrorTitle, tvErrorMessage, btnRetryMain;

    private View coinBalancePill;
    private TextView tvCoinBalance;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_template_gallery_screen);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.templateGalleryContent), true);

        // Shimmer + Error/Retry
        shimmerViewContainer = findViewById(R.id.shimmer_view_container);
        llErrorRetryContainer = findViewById(R.id.llErrorRetryContainer);
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnRetryMain = findViewById(R.id.btnRetryMain);

        if (btnRetryMain != null) {
            btnRetryMain.setOnClickListener(v -> fetchTemplatesFromApi());
        }

        // Toolbar
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        // RecyclerView
        rvGalleryCategories = findViewById(R.id.rvGalleryCategories);
        rvGalleryCategories.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(this));
        adapter = new AppFaceGalleryCategoryAdapter(this);
        rvGalleryCategories.setAdapter(adapter);

        // Coin header
        setupCoinHeader();

        // Load ads after layout
        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
            }
        });

        // Fetch templates
        fetchTemplatesFromApi();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCoinBalance();
    }

    @Override
    public void onBackPressed() {
        onBackInterstitial(new InterstitialAdCallback() {
            @Override
            public void onAdDismissed() {
                finish();
            }
        });
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
        if (rvGalleryCategories != null) rvGalleryCategories.setVisibility(View.GONE);

        fetchTemplatesCall = AppFaceApiRepository.fetchTemplates(new AppFaceApiRepository.TemplatesCallback() {
            @Override
            public void onSuccess(@NonNull List<AppFaceTemplateCategory> categories) {
                fetchTemplatesCall = null;
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }

                if (categories.isEmpty()) {
                    if (rvGalleryCategories != null) rvGalleryCategories.setVisibility(View.GONE);
                    if (llErrorRetryContainer != null) {
                        if (tvErrorTitle != null) tvErrorTitle.setText(getString(R.string.MainActivity_could_not_load_templates));
                        if (tvErrorMessage != null) tvErrorMessage.setText(getString(R.string.dialog_api_error_msg));
                        llErrorRetryContainer.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (llErrorRetryContainer != null) llErrorRetryContainer.setVisibility(View.GONE);
                    if (rvGalleryCategories != null) rvGalleryCategories.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Loaded " + categories.size() + " categories from API");
                    adapter.setCategories(categories);
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                fetchTemplatesCall = null;
                if (shimmerViewContainer != null) {
                    shimmerViewContainer.stopShimmer();
                    shimmerViewContainer.setVisibility(View.GONE);
                }
                if (rvGalleryCategories != null) rvGalleryCategories.setVisibility(View.GONE);

                if (llErrorRetryContainer != null) {
                    if (tvErrorTitle != null) tvErrorTitle.setText(getString(R.string.dialog_api_error_title));
                    if (tvErrorMessage != null) {
                        tvErrorMessage.setText(AppFaceNetworkUtils.isConnected(AppFaceTemplateGalleryActivity.this)
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
}

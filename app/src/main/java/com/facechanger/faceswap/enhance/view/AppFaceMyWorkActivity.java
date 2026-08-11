package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facechanger.faceswap.enhance.utils.AppFaceApiCall;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppFaceAppDialogController;
import com.facechanger.faceswap.enhance.controller.AppFaceOnDialogActionListener;
import com.facechanger.faceswap.enhance.model.api.AppFaceHistoryResponse;
import com.facechanger.faceswap.enhance.utils.AppFaceApiRepository;
import com.facechanger.faceswap.enhance.utils.AppFaceAppSystem;
import com.facechanger.faceswap.enhance.utils.AppFaceNetworkUtils;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.facechanger.faceswap.enhance.view.adapter.AppFaceHistoryAdapter;

/**
 * Displays the user's processing history with paginated loading.
 * <p>
 * Features:
 * <ul>
 *   <li>Paginated API calls (20 items per page)</li>
 *   <li>Infinite scroll with footer loading indicator</li>
 *   <li>Network connectivity check before loading</li>
 *   <li>Empty state and error handling</li>
 *   <li>Click to open result in DownloadShareActivity</li>
 * </ul>
 */
public class AppFaceMyWorkActivity extends BaseAppActivity {

    private static final String TAG = "MyWorkActivity";
    private static final int PAGE_SIZE = 20;

    private RecyclerView rvHistory;
    private View progressBar;
    private View tvEmpty;        // the whole empty-state container (LinearLayout)
    private TextView tvEmptyMsg; // inner message text, used for setText
    private View retentionNotice;
    private AppFaceHistoryAdapter adapter;

    private AppFaceApiCall fetchHistoryCall;

    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_my_work_screen);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.myWorkContent), false);

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
                loadSecondAds();
            }
        });

        setupToolbar();
        setupRetentionNotice();
        setupViews();
        loadHistory(1);
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
        if (fetchHistoryCall != null) {
            fetchHistoryCall.cancel();
        }
        super.onDestroy();
    }

    // ──────────────────────────────────────────────
    //  UI Setup
    // ──────────────────────────────────────────────

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    /**
     * Sets up the 7-day retention notice banner.
     * Allows the user to dismiss it for the current session.
     */
    private void setupRetentionNotice() {
        retentionNotice = findViewById(R.id.retentionNotice);
        View btnDismiss = findViewById(R.id.btnDismissNotice);

        if (btnDismiss != null && retentionNotice != null) {
            btnDismiss.setOnClickListener(v -> {
                // Animate collapse for a polished feel
                retentionNotice.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> retentionNotice.setVisibility(View.GONE))
                        .start();
            });
        }
    }

    private void setupViews() {
        rvHistory = findViewById(R.id.rvHistory);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmptyMsg = tvEmpty.findViewById(R.id.tvEmptyMsg);

        // Setup adapter
        adapter = new AppFaceHistoryAdapter(this);
        adapter.setClickListener(item -> {
            if (item.hasValidUrl()) {
                Intent intent = new Intent(AppFaceMyWorkActivity.this, AppFaceDownloadShareActivity.class);
                intent.putExtra("image_url", item.getResultUrl());
                String urlLower = item.getResultUrl().toLowerCase();
                String pathOnly = urlLower;
                int queryIdx = urlLower.indexOf('?');
                if (queryIdx != -1) {
                    pathOnly = urlLower.substring(0, queryIdx);
                }
                if (pathOnly.endsWith(".mp4") || pathOnly.endsWith(".avi") || pathOnly.endsWith(".mov") || 
                    pathOnly.endsWith(".3gp") || pathOnly.endsWith(".mkv") || pathOnly.endsWith(".webm")) {
                    intent.putExtra("is_video", true);
                }
                startActivity(intent);
            } else if (item.isSuccess() && item.isUrlExpired()) {
                Toast.makeText(AppFaceMyWorkActivity.this, getString(R.string.app_ai_face_this_image_link_has_text), Toast.LENGTH_SHORT).show();
            } else {
                AppFaceAppSystem.showDebugToast(AppFaceMyWorkActivity.this,
                        "Status: " + item.getStatus() +
                                (item.getErrorMessage() != null ? " — " + item.getErrorMessage() : ""));
            }
        });

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvHistory.setLayoutManager(layoutManager);
        rvHistory.setAdapter(adapter);

        // Infinite scroll listener
        rvHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) return; // Only trigger on scroll down

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                // Load next page when we're near the end (5 items threshold)
                if (!isLoading && currentPage < totalPages) {
                    if ((visibleItemCount + firstVisibleItemPosition + 5) >= totalItemCount) {
                        loadHistory(currentPage + 1);
                    }
                }
            }
        });
    }

    // ──────────────────────────────────────────────
    //  Data Loading
    // ──────────────────────────────────────────────

    private void loadHistory(int page) {
        // Network check
        if (!AppFaceNetworkUtils.isConnected()) {
            showNoInternetDialog();
            return;
        }

        if (isLoading) return;
        isLoading = true;

        if (page == 1) {
            // Initial load — show centered spinner
            progressBar.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        } else {
            // Subsequent pages — show footer loader
            adapter.setLoadingFooter(true);
        }

        fetchHistoryCall = AppFaceApiRepository.fetchHistory(page, PAGE_SIZE, new AppFaceApiRepository.HistoryCallback() {
            @Override
            public void onSuccess(@NonNull AppFaceHistoryResponse response) {
                fetchHistoryCall = null;
                isLoading = false;

                if (page == 1) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    adapter.setLoadingFooter(false);
                }

                currentPage = response.getPage();
                totalPages = response.getTotalPages();

                if (page == 1) {
                    adapter.setItems(response.getItems());
                } else {
                    adapter.addItems(response.getItems());
                }

                // Show/hide empty state
                if (adapter.getDataItemCount() == 0) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvHistory.setVisibility(View.VISIBLE);
                }

                Log.d(TAG, "Loaded page " + currentPage + "/" + totalPages
                        + " (" + response.getItems().size() + " items, total: " + response.getTotalCount() + ")");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                fetchHistoryCall = null;
                isLoading = false;

                if (page == 1) {
                    progressBar.setVisibility(View.GONE);
                    // Show empty state with error for first page
                    tvEmptyMsg.setText("Failed to load history. Tap to retry.");
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setOnClickListener(v -> {
                        tvEmptyMsg.setText(getString(R.string.app_face_my_work_hint_text));
                        tvEmpty.setOnClickListener(null);
                        loadHistory(1);
                    });
                } else {
                    adapter.setLoadingFooter(false);
                }

                Log.e(TAG, "History load failed: " + errorMessage);
                AppFaceAppSystem.showDebugToast(AppFaceMyWorkActivity.this, errorMessage);
            }
        });
    }

    // ──────────────────────────────────────────────
    //  Dialogs
    // ──────────────────────────────────────────────

    private void showNoInternetDialog() {
        if (isFinishing() || isDestroyed()) return;
        AppFaceAppDialogController.showRetryDialog(
                this,
                R.drawable.app_transparent_close_choose,
                getString(R.string.app_face_no_internet_title_text),
                getString(R.string.app_no_internet_desc_text),
                getString(R.string.app_api_retry_button_text),
                new AppFaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        if (AppFaceNetworkUtils.isConnected()) {
                            loadHistory(currentPage);
                        } else {
                            showNoInternetDialog();
                        }
                    }

                    @Override
                    public void onDismiss() {
                        // Show empty state if no data loaded yet
                        if (adapter.getDataItemCount() == 0) {
                            progressBar.setVisibility(View.GONE);
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }
}

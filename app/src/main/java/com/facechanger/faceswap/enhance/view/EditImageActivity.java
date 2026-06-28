package com.facechanger.faceswap.enhance.view;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppDialogController;
import com.facechanger.faceswap.enhance.controller.OnDialogActionListener;
import com.facechanger.faceswap.enhance.controller.OnServerBusyListener;
import com.facechanger.faceswap.enhance.model.EditImageData;
import com.facechanger.faceswap.enhance.model.api.FaceSwapResponse;
import com.facechanger.faceswap.enhance.utils.ApiRepository;
import com.facechanger.faceswap.enhance.utils.AppSystem;
import com.facechanger.faceswap.enhance.utils.NetworkUtils;
import com.facechanger.faceswap.enhance.utils.PermissionHelper;
import com.facechanger.faceswap.enhance.utils.Tools;
import com.faceenhance.facechanger.activity.BaseAdActivity;
import com.facechanger.faceswap.enhance.utils.CoinManager;
import com.faceenhance.facechanger.controller.AdManager;
import com.google.android.gms.ads.AdActivity;

import java.io.File;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditImageActivity extends BaseAppActivity {

    private static final int RC_PICK_BACKGROUND = 2001;

    private EditImageData editImageData;
    private Dialog permissionDialog;

    private Stack<String> imageHistory = new Stack<>();
    private ImageView btnUndo;
    private View loaderOverlay;
    private ImageView ivEditPreview;

    /** Tracks last action for retry: true = removeBg, false = enhance */
    private boolean lastIsRemoveBg;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();



    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.editImageContent), false);

        String imageUrl = getIntent().getStringExtra("image_url");
        editImageData = new EditImageData(imageUrl);
        if (imageUrl != null) {
            imageHistory.push(imageUrl);
        }

        setupToolbar();
        setupViews();
        setupPreview();
        setupClickListeners();
        setupSystemInsets();
        loadAds();

        AdManager.getInstance().preloadBigMediaNative();

    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void setupViews() {
        ivEditPreview = findViewById(R.id.ivEditPreview);
        btnUndo = findViewById(R.id.btnUndo);
        loaderOverlay = findViewById(R.id.loaderOverlay);

        btnUndo.setOnClickListener(v -> handleUndo());
    }

    private void setupPreview() {
        if (!imageHistory.isEmpty()) {
            updateImagePreview(imageHistory.peek());
        }
    }

    private void updateImagePreview(String url) {
        if (isFinishing() || isDestroyed()) return;
        Glide.with(this)
                .load(url)
                .placeholder(R.color.card_background)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivEditPreview);
        updateUndoButton();
    }

    private void handleUndo() {
        if (imageHistory.size() > 1) {
            imageHistory.pop();
            updateImagePreview(imageHistory.peek());
        }
    }

    private void updateUndoButton() {
        if (btnUndo != null) {
            btnUndo.setVisibility(imageHistory.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }

    private void setupClickListeners() {
        // Remove BG
        findViewById(R.id.btnChangeBG1).setOnClickListener(v -> {
            if (AdManager.getInstance().isPremiumUser()) {
                processImage(true);
            } else {
                startActivity(new Intent(this, PaywallActivity.class));
            }
        });

        // Change BG → open background picker
        findViewById(R.id.btnChangeBG2).setOnClickListener(v -> {
            if (AdManager.getInstance().isPremiumUser()) {
                Intent intent = new Intent(this, BackgroundPickerActivity.class);
                startActivityForResult(intent, RC_PICK_BACKGROUND);
            } else {
                startActivity(new Intent(this, PaywallActivity.class));
            }
        });

        // Enhance
        findViewById(R.id.btnChangeBG3).setOnClickListener(v -> {
            if (AdManager.getInstance().isPremiumUser()) {
                processImage(false);
            } else {
                startActivity(new Intent(this, PaywallActivity.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PICK_BACKGROUND && resultCode == RESULT_OK && data != null) {
            String bgUrl = data.getStringExtra(BackgroundPickerActivity.EXTRA_SELECTED_BG_URL);
            if (bgUrl != null) {
                editImageData.setSelectedBackgroundUrl(bgUrl);
                Toast.makeText(this, getString(R.string.EditImageActivity_background_selected), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // If returning from settings and permissions are now granted, dismiss dialog
        if (permissionDialog != null && permissionDialog.isShowing()) {
            if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
                permissionDialog.dismiss();
                permissionDialog = null;
                Toast.makeText(this, getString(R.string.EditImageActivity_permission_granted), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.RC_CAMERA_GALLERY) {
            if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
                Toast.makeText(this, getString(R.string.EditImageActivity_permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = AppDialogController.showPermissionDeniedDialog(
                this,
                "Permission Required",
                "Camera and gallery access are needed to upload and edit images. Please enable them in Settings.",
                new OnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        PermissionHelper.openAppSettings(EditImageActivity.this);
                    }

                    @Override
                    public void onDismiss() {
                        permissionDialog = null;
                    }
                }
        );
    }

    private void setupSystemInsets() {
        View content = findViewById(R.id.editImageContent);
        if (content != null) {
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    private void showLoader(boolean show) {
        if (loaderOverlay != null) {
            loaderOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void processImage(boolean isRemoveBg) {
        if (imageHistory.isEmpty()) return;
        lastIsRemoveBg = isRemoveBg;

        // Network check
        if (!NetworkUtils.isConnected()) {
            showNoInternetDialog();
            return;
        }

        String currentUrl = imageHistory.peek();

        if (AppSystem.USE_LOTTIE_LOADER) {
            // Route through LottieLoadingActivity (with ads)
            processViaLottie(currentUrl, isRemoveBg);
        } else {
            // Inline flow (original behavior)
            processInline(currentUrl, isRemoveBg);
        }
    }

    /**
     * Routes through LottieLoadingActivity which shows ads.
     * On success, LottieLoadingActivity navigates directly to DownloadShareActivity.
     */
    private void processViaLottie(String currentUrl, boolean isRemoveBg) {
        showLoader(true);
        new Thread(() -> {
            try {
                File file = Glide.with(EditImageActivity.this)
                        .downloadOnly()
                        .load(currentUrl)
                        .submit()
                        .get();

                // Copy to a stable temp file (Glide cache files can be cleaned)
                File tempFile = new File(getCacheDir(),
                        isRemoveBg ? "edit_remove_bg.jpg" : "edit_upscale.jpg");
                copyFile(file, tempFile);

                runOnUiThread(() -> {
                    showLoader(false);
                    String coinAction = isRemoveBg
                            ? LottieLoadingActivity.ACTION_REMOVE_BG
                            : LottieLoadingActivity.ACTION_UPSCALE;
                    CoinManager.checkAndProceed(EditImageActivity.this, coinAction, () -> {
                        Intent intent = new Intent(EditImageActivity.this, LottieLoadingActivity.class);
                        intent.putExtra("action", coinAction);
                        intent.putExtra("file_path", tempFile.getAbsolutePath());
                        startActivity(intent);
                    });
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoader(false);
                    AppSystem.showDebugToast(EditImageActivity.this, "Failed to load image: " + e.getMessage());
                    showErrorDialog();
                });
            }
        }).start();
    }

    /**
     * Original inline flow — calls the API directly from this activity.
     */
    private void processInline(String currentUrl, boolean isRemoveBg) {
        showLoader(true);
        new Thread(() -> {
            try {
                File file = Glide.with(EditImageActivity.this)
                        .downloadOnly()
                        .load(currentUrl)
                        .submit()
                        .get();

                runOnUiThread(() -> {
                    if (isRemoveBg) {
                        callRemoveBgApi(file);
                    } else {
                        callEnhanceApi(file);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoader(false);
                    AppSystem.showDebugToast(EditImageActivity.this, "Failed to load image: " + e.getMessage());
                    showErrorDialog();
                });
            }
        }).start();
    }

    private void callRemoveBgApi(File file) {
        ApiRepository.removeBackground(file, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                handleApiSuccess(response, file);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                handleApiError(errorMessage);
            }
        });
    }

    private void callEnhanceApi(File file) {
        ApiRepository.upscaleImage(file, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                handleApiSuccess(response, file);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                handleApiError(errorMessage);
            }
        });
    }

    private void handleApiSuccess(FaceSwapResponse response, File file) {
        showLoader(false);
        if (response.isSuccess() && response.hasResultImage()) {
            String resultUrl = response.getImageUrl();
            if (resultUrl == null || resultUrl.trim().isEmpty()) {
                resultUrl = response.getImageBase64();
            }
            if (resultUrl != null && !resultUrl.trim().isEmpty()) {
                // Navigate to download/share screen with the result (Option A)
                Intent intent = new Intent(EditImageActivity.this, DownloadShareActivity.class);
                intent.putExtra("image_url", resultUrl);
                startActivity(intent);
            }
        } else {
            AppSystem.showDebugToast(this, response.getMessage());
            showErrorDialog();
        }
    }

    private void handleApiError(String errorMessage) {
        showLoader(false);
        AppSystem.showDebugToast(this, errorMessage);
        showErrorDialog();
    }

    // ──────────────────────────────────────────────
    //  Error dialogs
    // ──────────────────────────────────────────────

    /**
     * Shows the appropriate error dialog based on {@link AppSystem#USE_SERVER_BUSY_DIALOG}.
     */
    private void showErrorDialog() {
        if (isFinishing() || isDestroyed()) return;

        if (AppSystem.USE_SERVER_BUSY_DIALOG) {
            AppDialogController.showServerBusyDialog(this, new OnServerBusyListener() {
                @Override
                public void onRetry() {
                    processImage(lastIsRemoveBg);
                }

                @Override
                public void onGoBack() {
                    // Stay on screen, just dismiss
                }
            });
        } else {
            AppDialogController.showRetryDialog(
                    this,
                    R.drawable.ic_trans_close,
                    getString(R.string.dialog_api_error_title),
                    getString(R.string.dialog_api_error_msg),
                    getString(R.string.dialog_retry_button),
                    new OnDialogActionListener() {
                        @Override
                        public void onPositiveClick() {
                            processImage(lastIsRemoveBg);
                        }

                        @Override
                        public void onDismiss() {
                            // Stay on screen
                        }
                    });
        }
    }

    private void showNoInternetDialog() {
        if (isFinishing() || isDestroyed()) return;
        AppDialogController.showRetryDialog(
                this,
                R.drawable.ic_trans_close,
                getString(R.string.dialog_no_internet_title),
                getString(R.string.dialog_no_internet_msg),
                getString(R.string.dialog_retry_button),
                new OnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        processImage(lastIsRemoveBg);
                    }

                    @Override
                    public void onDismiss() {
                        // Stay on screen
                    }
                });
    }

    // ──────────────────────────────────────────────
    //  File utilities
    // ──────────────────────────────────────────────

    private void copyFile(File source, File dest) throws java.io.IOException {
        try (java.io.InputStream in = new java.io.FileInputStream(source);
             java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

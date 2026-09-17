package com.facechanger.faceswap.enhance.view;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.app.ProgressDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.facechanger.faceswap.enhance.utils.AppFaceSessionManager;
import com.facechanger.faceswap.enhance.utils.AppFaceRatingPrefs;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppFaceAppDialogController;
import com.facechanger.faceswap.enhance.controller.AppFaceOnDialogActionListener;
import com.facechanger.faceswap.enhance.controller.AppFaceOnServerBusyListener;
import com.facechanger.faceswap.enhance.model.AppFaceEditImageData;
import com.facechanger.faceswap.enhance.model.api.AppFaceFaceSwapResponse;
import com.facechanger.faceswap.enhance.utils.AppFaceApiRepository;
import com.facechanger.faceswap.enhance.utils.AppFaceAppSystem;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceNetworkUtils;
import com.facechanger.faceswap.enhance.utils.AppFacePermissionHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.facechanger.faceswap.enhance.utils.AppFaceCoinManager;
import com.faceenhance.facechanger.controller.AdManager;

import java.io.File;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppFaceEditImageActivity extends BaseAppActivity {

    private static final int RC_PICK_BACKGROUND = 2001;

    private AppFaceEditImageData editImageData;
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
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_image_edit);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.editImageContent), true);

        String imageUrl = getIntent().getStringExtra("image_url");
        String originalImageUrl = getIntent().getStringExtra("original_image_url");
        editImageData = new AppFaceEditImageData(imageUrl);
        
        if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
            imageHistory.push(originalImageUrl);
        }
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageHistory.push(imageUrl);
        }

        setupToolbar();
        setupViews();
        setupPreview();
        setupClickListeners();
        setupSystemInsets();
        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
            }
        });

        AdManager.getInstance().preloadBigMediaNative();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String imageUrl = intent.getStringExtra("image_url");
        if (imageUrl != null) {
            imageHistory.push(imageUrl);
            updateImagePreview(imageUrl);
        }
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
                .placeholder(R.color.app_base_card_background)
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
        // Download button
        findViewById(R.id.btnDownload).setOnClickListener(v -> handleDownloadClick());

        // Remove BG
        findViewById(R.id.btnChangeBG1).setOnClickListener(v -> {
            if (AdManager.getInstance().isPremiumUser()) {
                processImage(true);
            } else {
                startActivity(new Intent(this, AppFacePaywallActivity.class));
            }
        });

        // Change BG → open background picker
        findViewById(R.id.btnChangeBG2).setOnClickListener(v -> {
            if (AdManager.getInstance().isPremiumUser()) {
                Intent intent = new Intent(this, AppFaceBackgroundPickerActivity.class);
                startActivityForResult(intent, RC_PICK_BACKGROUND);
            } else {
                startActivity(new Intent(this, AppFacePaywallActivity.class));
            }
        });

        // Enhance
        findViewById(R.id.btnChangeBG3).setOnClickListener(v -> {
            if (AdManager.getInstance().isPremiumUser()) {
                processImage(false);
            } else {
                startActivity(new Intent(this, AppFacePaywallActivity.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PICK_BACKGROUND && resultCode == RESULT_OK && data != null) {
            String bgUrl = data.getStringExtra(AppFaceBackgroundPickerActivity.EXTRA_SELECTED_BG_URL);
            if (bgUrl != null) {
                editImageData.setSelectedBackgroundUrl(bgUrl);
                Toast.makeText(this, getString(R.string.app_edit_background_selected_text), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // If returning from settings and permissions are now granted, dismiss dialog
        if (permissionDialog != null && permissionDialog.isShowing()) {
            if (AppFacePermissionHelper.hasCameraAndGalleryPermission(this)) {
                permissionDialog.dismiss();
                permissionDialog = null;
                Toast.makeText(this, getString(R.string.app_edit_permission_granted_text), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppFacePermissionHelper.RC_CAMERA_GALLERY) {
            if (AppFacePermissionHelper.hasCameraAndGalleryPermission(this)) {
                Toast.makeText(this, getString(R.string.app_edit_permission_granted_text), Toast.LENGTH_SHORT).show();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = AppFaceAppDialogController.showPermissionDeniedDialog(
                this,
                "Permission Required",
                "Camera and gallery access are needed to upload and edit images. Please enable them in Settings.",
                new AppFaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        AppFacePermissionHelper.openAppSettings(AppFaceEditImageActivity.this);
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
        if (!AppFaceNetworkUtils.isConnected()) {
            showNoInternetDialog();
            return;
        }

        String currentUrl = imageHistory.peek();

        if (AppFaceAppSystem.USE_LOTTIE_LOADER) {
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
                File file = Glide.with(AppFaceEditImageActivity.this)
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
                            ? AppFaceLoadingActivity.ACTION_REMOVE_BG
                            : AppFaceLoadingActivity.ACTION_UPSCALE;
                    AppFaceCoinManager.checkAndProceed(AppFaceEditImageActivity.this, coinAction, () -> {
                        Intent intent = new Intent(AppFaceEditImageActivity.this, AppFaceLoadingActivity.class);
                        intent.putExtra("action", coinAction);
                        intent.putExtra("file_path", tempFile.getAbsolutePath());
                        startActivity(intent);
                    });
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoader(false);
                    AppFaceAppSystem.showDebugToast(AppFaceEditImageActivity.this, "Failed to load image: " + e.getMessage());
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
                File file = Glide.with(AppFaceEditImageActivity.this)
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
                    AppFaceAppSystem.showDebugToast(AppFaceEditImageActivity.this, "Failed to load image: " + e.getMessage());
                    showErrorDialog();
                });
            }
        }).start();
    }

    private void callRemoveBgApi(File file) {
        AppFaceApiRepository.removeBackground(file, new AppFaceApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull AppFaceFaceSwapResponse response) {
                handleApiSuccess(response, file);
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                handleApiError(errorMessage);
            }
        });
    }

    private void callEnhanceApi(File file) {
        AppFaceApiRepository.upscaleImage(file, new AppFaceApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull AppFaceFaceSwapResponse response) {
                handleApiSuccess(response, file);
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                handleApiError(errorMessage);
            }
        });
    }

    private void handleApiSuccess(AppFaceFaceSwapResponse response, File file) {
        showLoader(false);
        if (response.isSuccess() && response.hasResultImage()) {
            String resultUrl = response.getImageUrl();
            if (resultUrl == null || resultUrl.trim().isEmpty()) {
                resultUrl = response.getImageBase64();
            }
            if (resultUrl != null && !resultUrl.trim().isEmpty()) {
                // Navigate to download/share screen with the result (Option A)
                Intent intent = new Intent(AppFaceEditImageActivity.this, AppFaceDownloadShareActivity.class);
                intent.putExtra("image_url", resultUrl);
                startActivity(intent);
            }
        } else {
            AppFaceAppSystem.showDebugToast(this, response.getMessage());
            showErrorDialog();
        }
    }

    private void handleApiError(String errorMessage) {
        showLoader(false);
        AppFaceAppSystem.showDebugToast(this, errorMessage);
        showErrorDialog();
    }

    // ──────────────────────────────────────────────
    //  Error dialogs
    // ──────────────────────────────────────────────

    /**
     * Shows the appropriate error dialog based on {@link AppFaceAppSystem#USE_SERVER_BUSY_DIALOG}.
     */
    private void showErrorDialog() {
        if (isFinishing() || isDestroyed()) return;

        if (AppFaceAppSystem.USE_SERVER_BUSY_DIALOG) {
            AppFaceAppDialogController.showServerBusyDialog(this, "", new AppFaceOnServerBusyListener() {
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
            AppFaceAppDialogController.showRetryDialog(
                    this,
                    R.drawable.app_transparent_close_choose,
                    getString(R.string.app_api_error_title_text),
                    getString(R.string.app_api_error_text),
                    getString(R.string.app_api_retry_button_text),
                    new AppFaceOnDialogActionListener() {
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
        AppFaceAppDialogController.showRetryDialog(
                this,
                R.drawable.app_transparent_close_choose,
                getString(R.string.app_face_no_internet_title_text),
                getString(R.string.app_no_internet_desc_text),
                getString(R.string.app_api_retry_button_text),
                new AppFaceOnDialogActionListener() {
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

    private static final int RC_WRITE_STORAGE = 101;
    private static final String TAG = "AppFaceEditImageActivity";

    private void handleDownloadClick() {
        if (imageHistory.isEmpty()) {
            Toast.makeText(this, getString(R.string.app_face_no_image_to_download_text), Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        RC_WRITE_STORAGE);
                return;
            }
        }

        downloadImageToGallery();
    }

    private void downloadImageToGallery() {
        if (imageHistory.isEmpty()) return;
        String currentUrl = imageHistory.peek();
        
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving to Gallery…");
        progressDialog.setCancelable(false);
        progressDialog.show();

        executor.execute(() -> {
            try {
                Bitmap bitmap;
                if (currentUrl.startsWith("data:image") || isBase64(currentUrl)) {
                    String base64Data = currentUrl;
                    if (base64Data.contains(",")) {
                        base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                    }
                    byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
                    bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                } else {
                    bitmap = downloadBitmapFromUrl(currentUrl);
                }

                if (bitmap == null) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, getString(R.string.app_face_image_save_error_text), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                boolean saved = saveBitmapToGallery(bitmap,
                        "FaceSwap_" + System.currentTimeMillis() + ".png");
                bitmap.recycle();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (saved) {
                        Toast.makeText(this, getString(R.string.app_face_gallery_save_text), Toast.LENGTH_SHORT).show();
                        if (!AppFaceRatingPrefs.hasShownRatingDialog(this)) {
                            AppFaceRatingPrefs.markRatingDialogShown(this);
                            getWindow().getDecorView().postDelayed(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    AppFaceAppDialogController.showRatingDialog(this);
                                }
                            }, 600);
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.app_face_image_save_error_text), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    AppFaceAppSystem.showDebugToast(this, "Download error: " + e.getMessage());
                    Toast.makeText(this, getString(R.string.app_face_image_save_error_text), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Bitmap downloadBitmapFromUrl(String urlStr) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);

            String token = AppFaceSessionManager.getInstance().getToken();
            if (!token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            InputStream in = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            in.close();
            return bitmap;
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    private boolean saveBitmapToGallery(Bitmap bitmap, String fileName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FaceSwap");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return false;

                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) return false;
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }

                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
                return true;
            } else {
                File picturesDir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "FaceSwap");
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs();
                }

                File file = new File(picturesDir, fileName);
                try (OutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }

                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scanIntent.setData(Uri.fromFile(file));
                sendBroadcast(scanIntent);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBase64(String str) {
        if (str == null || str.isEmpty() || str.length() < 100) return false;
        return !str.startsWith("http://") && !str.startsWith("https://") && !str.startsWith("/");
    }
}

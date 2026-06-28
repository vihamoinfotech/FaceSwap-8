package com.facechanger.faceswap.enhance.view;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.faceenhance.facechanger.activity.BaseAdActivity;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppDialogController;
import com.facechanger.faceswap.enhance.controller.OnDialogActionListener;
import com.facechanger.faceswap.enhance.model.api.FaceSwapResponse;
import com.facechanger.faceswap.enhance.utils.ActivityNavHelper;
import com.facechanger.faceswap.enhance.utils.ApiRepository;
import com.facechanger.faceswap.enhance.utils.CoinManager;
import com.facechanger.faceswap.enhance.utils.DashedGradientBorderDrawable;
import com.facechanger.faceswap.enhance.utils.GlideHelper;
import com.facechanger.faceswap.enhance.utils.ImagePickerHelper;
import com.facechanger.faceswap.enhance.utils.PermissionHelper;
import com.facechanger.faceswap.enhance.utils.SessionManager;
import com.facechanger.faceswap.enhance.utils.RewardedAdHelper;
import com.facechanger.faceswap.enhance.utils.Tools;
import com.faceenhance.facechanger.controller.AdManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Face swap screen where users select a template (target image)
 * and their face photo (source image), then call the face swap API.
 * <p>
 * Flow:
 * 1. Template image displayed as main preview (from intent or API)
 * 2. User picks their face photo from gallery
 * 3. User taps "Generate"
 * 4. App downloads template to temp file, copies face URI to temp file
 * 5. Calls {@code POST /api/Image/faceswap/basic}
 * 6. On success: navigates to DownloadShareActivity with result URL
 */
public class FaceSwapActivity extends BaseAppActivity {

    private static final String TAG = "FaceSwapActivity";
    private static final boolean USE_LOTTIE_LOADER = true;

    private String imageUrl;
    private Dialog permissionDialog;

    private boolean isEditImage;
    private String prompt;
    private View llSelectTarget;
    private ImageView ivMainPreview;

    private View btnAddFace;
    private View rlUploadedFace;
    private ImageView ivUploadedFace;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> targetGalleryLauncher;
    private ActivityResultLauncher<Uri> cameraFaceLauncher;
    private ActivityResultLauncher<Uri> cameraTargetLauncher;

    /**
     * URI of the user's selected face photo
     */
    private Uri selectedFaceUri;
    /**
     * URI of the selected target image (when not using URL)
     */
    private Uri selectedTargetUri;
    private Uri cameraFaceUri;
    private Uri cameraTargetUri;

    private int pendingGalleryAction = 0; // 1 = face, 2 = target

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_swap);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.faceSwapContent), false);

        loadAds();
        loadSecondAds();

        AdManager.getInstance().preloadBigMediaNative();

        imageUrl = getIntent().getStringExtra("image_url");
        isEditImage = getIntent().getBooleanExtra("is_edit_image", false);

        Log.e("#######", "" + isEditImage);

        prompt = getIntent().getStringExtra("prompt");

        btnAddFace = findViewById(R.id.btnAddFace);
        rlUploadedFace = findViewById(R.id.rlUploadedFace);
        ivUploadedFace = findViewById(R.id.ivUploadedFace);
        llSelectTarget = findViewById(R.id.llSelectTarget);
        ivMainPreview = findViewById(R.id.ivMainPreview);

        setupGalleryLauncher();
        setupTargetGalleryLauncher();
        setupCameraLaunchers();

        setupToolbar();
        setupPreview();

        RewardedAdHelper.preload(this);

        setupAddFaceDashedGradientBorder();
        setupClickListeners();
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

    // ──────────────────────────────────────────────
    //  Gallery picker
    // ──────────────────────────────────────────────

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            selectedFaceUri = imageUri;
                            showUploadedFace(imageUri);
                        }
                    }
                }
        );
    }

    private void setupCameraLaunchers() {
        cameraFaceLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraFaceUri != null) {
                        selectedFaceUri = cameraFaceUri;
                        pendingGalleryAction = 0;
                        showUploadedFace(cameraFaceUri);
                    }
                });
        cameraTargetLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraTargetUri != null) {
                        selectedTargetUri = cameraTargetUri;
                        pendingGalleryAction = 0;
                        showTargetImage(cameraTargetUri);
                    }
                });
    }

    private void setupTargetGalleryLauncher() {
        targetGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            selectedTargetUri = imageUri;
                            showTargetImage(imageUri);
                        }
                    }
                }
        );
    }

    private void showTargetImage(Uri uri) {
        if (llSelectTarget != null) llSelectTarget.setVisibility(View.GONE);
        if (ivMainPreview != null) ivMainPreview.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(ivMainPreview);
    }

    private void showUploadedFace(Uri uri) {
        btnAddFace.setVisibility(View.GONE);
        rlUploadedFace.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(ivUploadedFace);
    }

    private void removeUploadedFace() {
        rlUploadedFace.setVisibility(View.GONE);
        btnAddFace.setVisibility(View.VISIBLE);
        Glide.with(ivUploadedFace).clear(ivUploadedFace);
        selectedFaceUri = null;
    }

    // ──────────────────────────────────────────────
    //  Face Swap API call
    // ──────────────────────────────────────────────

    /**
     * Validates inputs, prepares files, and calls the face swap API.
     */
    private void performFaceSwap() {
        if (!isEditImage && (imageUrl == null || imageUrl.isEmpty()) && selectedTargetUri == null) {
            Toast.makeText(this, getString(R.string.FaceSwapActivity_please_select_a_target), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFaceUri == null) {
            Toast.makeText(this, getString(R.string.FaceSwapActivity_please_add_your_face), Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        if (!USE_LOTTIE_LOADER) {
            progressDialog.setMessage("Creating your image…");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        executor.execute(() -> {
            try {
                File templateFile = null;
                if (!isEditImage) {
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        templateFile = downloadUrlToTempFile(imageUrl, "face_target.jpg");
                    } else if (selectedTargetUri != null) {
                        templateFile = copyUriToTempFile(this, selectedTargetUri, "face_target.jpg");
                    }

                    if (templateFile == null) {
                        runOnUiThread(() -> {
                            if (!USE_LOTTIE_LOADER) progressDialog.dismiss();
                            Toast.makeText(this, getString(R.string.FaceSwapActivity_failed_to_prepare_template), Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                }

                File faceFile = copyUriToTempFile(this, selectedFaceUri, "face_source.jpg");
                if (faceFile == null) {
                    runOnUiThread(() -> {
                        if (!USE_LOTTIE_LOADER) progressDialog.dismiss();
                        Toast.makeText(this, getString(R.string.FaceSwapActivity_failed_to_read_face), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                final File finalTemplateFile = templateFile;
                final File finalFaceFile = faceFile;

                runOnUiThread(() -> {
                    if (USE_LOTTIE_LOADER) {
                        String coinAction = isEditImage
                                ? LottieLoadingActivity.ACTION_AI_IMAGE
                                : LottieLoadingActivity.ACTION_FACE_SWAP;

                        CoinManager.checkAndProceed(FaceSwapActivity.this, coinAction, () -> {
                            Intent intent = new Intent(FaceSwapActivity.this, LottieLoadingActivity.class);

                            // BEFORE image for the slider:
                            // - Template from home → use imageUrl (HTTP URL)
                            // - Gallery/camera pick → use the local temp file
//                            if (imageUrl != null && !imageUrl.isEmpty()) {
//                                intent.putExtra("original_image_url", imageUrl);
//                            } else if (!isEditImage && finalTemplateFile != null) {
//                                intent.putExtra("original_image_url", finalTemplateFile.getAbsolutePath());
//                            }

                            if (isEditImage) {
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    Log.e("#####", "imageUrl");
                                    intent.putExtra("original_image_url", imageUrl);
                                } else if (finalTemplateFile != null) {
                                    Log.e("#####", "finalTemplateFile");
                                    intent.putExtra("original_image_url", finalTemplateFile.getAbsolutePath());
                                }
                            } else {

                                if (finalTemplateFile != null) {
                                    Log.e("#####", "finalTemplateFile");
                                    intent.putExtra("original_image_url", finalTemplateFile.getAbsolutePath());
                                }
                            }

                            if (isEditImage) {
                                intent.putExtra("action", LottieLoadingActivity.ACTION_AI_IMAGE);
                                intent.putExtra("prompt", prompt != null ? prompt : "");
                                intent.putExtra("file_path", finalFaceFile.getAbsolutePath());
                            } else {
                                intent.putExtra("action", LottieLoadingActivity.ACTION_FACE_SWAP);
                                if (finalTemplateFile != null) {
                                    intent.putExtra("source_path", finalTemplateFile.getAbsolutePath());
                                }
                                intent.putExtra("target_path", finalFaceFile.getAbsolutePath());
                            }

                            startActivity(intent);
                        });
                    } else {
                        if (isEditImage) {
                            ApiRepository.editImage(finalFaceFile, prompt != null ? prompt : "", new ApiRepository.FaceSwapCallback() {
                                @Override
                                public void onSuccess(@NonNull FaceSwapResponse response) {
                                    handleSuccessAndCleanup(progressDialog, response, finalTemplateFile, finalFaceFile);
                                }

                                @Override
                                public void onError(@NonNull String errorMessage) {
                                    handleErrorAndCleanup(progressDialog, errorMessage, finalTemplateFile, finalFaceFile);
                                }
                            });
                        } else {
                            ApiRepository.faceSwapBasic(finalTemplateFile, finalFaceFile, false,
                                    new ApiRepository.FaceSwapCallback() {
                                        @Override
                                        public void onSuccess(@NonNull FaceSwapResponse response) {
                                            handleSuccessAndCleanup(progressDialog, response, finalTemplateFile, finalFaceFile);
                                        }

                                        @Override
                                        public void onError(@NonNull String errorMessage) {
                                            handleErrorAndCleanup(progressDialog, errorMessage, finalTemplateFile, finalFaceFile);
                                        }
                                    });
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "File preparation failed", e);
                runOnUiThread(() -> {
                    if (!USE_LOTTIE_LOADER) progressDialog.dismiss();
                    Toast.makeText(this, getString(R.string.FaceSwapActivity_an_error_occurred_please),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void handleSuccessAndCleanup(ProgressDialog progressDialog, FaceSwapResponse response, File f1, File f2) {
        progressDialog.dismiss();
        // NOTE: Do NOT cleanup f1 (template/original) here — it is passed as the
        // "before" image to DownloadShareActivity. Only cleanup f2 (face source).
        cleanupTempFiles(f2);

        if (response.isSuccess() && response.hasResultImage()) {
            String resultUrl = response.getImageUrl();
            if (resultUrl == null || resultUrl.isEmpty()) {
                resultUrl = response.getImageBase64();
            }

            Intent intent = new Intent(FaceSwapActivity.this, DownloadShareActivity.class);
            intent.putExtra("image_url", resultUrl);
            // BEFORE image: template (f1) or imageUrl
            String original;
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Template from home screen — use the URL directly
                original = imageUrl;
            } else if (f1 != null && f1.exists()) {
                // User picked from gallery/camera — use local file path
                original = f1.getAbsolutePath();
            } else {
                original = null;
            }
            if (original != null) {
                intent.putExtra("original_image_url", original);
            }
            ActivityNavHelper.start(FaceSwapActivity.this, intent);
        } else {
            // Cleanup f1 too on failure since we won't navigate
            cleanupTempFiles(f1);
            Toast.makeText(FaceSwapActivity.this,
                    response.getMessage() == null || response.getMessage().isEmpty() ? "Operation failed. Please try again." : response.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void handleErrorAndCleanup(ProgressDialog progressDialog, String errorMessage, File f1, File f2) {
        progressDialog.dismiss();
        cleanupTempFiles(f1, f2);
        Log.e(TAG, "API error: " + errorMessage);
        Toast.makeText(FaceSwapActivity.this, getString(R.string.FaceSwapActivity_operation_failed_please_try), Toast.LENGTH_SHORT).show();
    }

    // ──────────────────────────────────────────────
    //  File utilities
    // ──────────────────────────────────────────────

    /**
     * Copies content from a content URI to a temp file in the app's cache directory.
     */
    private static File copyUriToTempFile(@NonNull Context context,
                                          @NonNull Uri uri,
                                          @NonNull String fileName) {
        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(context.getCacheDir(), fileName);
            try (OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            Log.e("FaceSwapActivity", "copyUriToTempFile failed", e);
            return null;
        }
    }

    /**
     * Downloads an image URL to a temp file.
     * Includes Bearer auth header if the URL points to our API.
     */
    private File downloadUrlToTempFile(@NonNull String urlStr,
                                       @NonNull String fileName) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.setRequestMethod("GET");

            // Add auth header for API URLs
            String token = SessionManager.getInstance().getToken();
            if (!token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Download failed: HTTP " + responseCode + " for " + urlStr);
                return null;
            }

            File tempFile = new File(getCacheDir(), fileName);
            try (InputStream in = connection.getInputStream();
                 OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "downloadUrlToTempFile failed", e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /**
     * Silently delete temp files after use.
     */
    private void cleanupTempFiles(File... files) {
        executor.execute(() -> {
            for (File f : files) {
                if (f != null && f.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
        });
    }

    // ──────────────────────────────────────────────
    //  UI setup (unchanged logic, cleaned up)
    // ──────────────────────────────────────────────

    private void setupAddFaceDashedGradientBorder() {
        View addFace = findViewById(R.id.btnAddFace);
        int start = ContextCompat.getColor(this, R.color.dashed_border_gradient_start);
        int end = ContextCompat.getColor(this, R.color.dashed_border_gradient_end);
        addFace.setBackground(new DashedGradientBorderDrawable(
                getResources(),
                start,
                end,
                12f,
                1.5f,
                6f,
                4f));
    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void setupPreview() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (llSelectTarget != null) llSelectTarget.setVisibility(View.GONE);
            if (ivMainPreview != null) ivMainPreview.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(GlideHelper.authorizedUrl(imageUrl))
                    .placeholder(R.color.card_background)
                    .error(R.color.card_background)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(ivMainPreview);
        } else {
            if (llSelectTarget != null) llSelectTarget.setVisibility(View.VISIBLE);
            if (ivMainPreview != null) {
                ivMainPreview.setVisibility(View.GONE);
                Glide.with(ivMainPreview).clear(ivMainPreview);
                int bg = ContextCompat.getColor(this, R.color.card_background);
                ivMainPreview.setBackgroundColor(bg);
            }
        }
    }

    private void setupClickListeners() {
        btnAddFace.setOnClickListener(v -> handleUploadFaceClick());
        findViewById(R.id.ivRemoveFace).setOnClickListener(v -> removeUploadedFace());
        if (llSelectTarget != null) {
            llSelectTarget.setOnClickListener(v -> handleSelectTargetClick());
        }
        if (ivMainPreview != null) {
            ivMainPreview.setOnClickListener(v -> {
                if (imageUrl == null || imageUrl.isEmpty()) {
                    handleSelectTargetClick();
                }
            });
        }
        // Generate button now calls the face swap API
        findViewById(R.id.btnGenerate).setOnClickListener(v -> performFaceSwap());
    }

    private void handleSelectTargetClick() {
        pendingGalleryAction = 2;
        if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
            ImagePickerHelper.show(this,
                    () -> {
                        cameraTargetUri = ImagePickerHelper.createCameraOutputUri(this);
                        if (cameraTargetUri != null) cameraTargetLauncher.launch(cameraTargetUri);
                    },
                    this::openTargetGallery);
        } else {
            PermissionHelper.requestCameraAndGalleryPermission(this);
        }
    }

    private void openTargetGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        targetGalleryLauncher.launch(intent);
    }

    private void handleUploadFaceClick() {
        AppDialogController.showPhotoTipsIfNeeded(this, () -> {
            pendingGalleryAction = 1;
            if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
                ImagePickerHelper.show(this,
                        () -> {
                            cameraFaceUri = ImagePickerHelper.createCameraOutputUri(this);
                            if (cameraFaceUri != null) cameraFaceLauncher.launch(cameraFaceUri);
                        },
                        this::openGallery);
            } else {
                PermissionHelper.requestCameraAndGalleryPermission(this);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.RC_CAMERA_GALLERY) {
            if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
                if (pendingGalleryAction == 2) {
                    openTargetGallery();
                } else {
                    openGallery();
                }
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (permissionDialog != null && permissionDialog.isShowing()) {
            if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
                permissionDialog.dismiss();
                permissionDialog = null;
                if (pendingGalleryAction == 2) {
                    openTargetGallery();
                } else {
                    openGallery();
                }
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = AppDialogController.showPermissionDeniedDialog(
                this,
                "Permission Required",
                "Camera and gallery access are needed to upload your face photo. Please enable them in Settings.",
                new OnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        PermissionHelper.openAppSettings(FaceSwapActivity.this);
                    }

                    @Override
                    public void onDismiss() {
                        permissionDialog = null;
                    }
                }
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

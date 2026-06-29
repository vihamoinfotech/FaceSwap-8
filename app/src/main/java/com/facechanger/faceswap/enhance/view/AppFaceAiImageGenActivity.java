package com.facechanger.faceswap.enhance.view;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppFaceAppDialogController;
import com.facechanger.faceswap.enhance.controller.AppFaceOnDialogActionListener;
import com.facechanger.faceswap.enhance.model.api.AppFaceFaceSwapResponse;
import com.facechanger.faceswap.enhance.utils.AppFaceActivityNavHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceApiRepository;
import com.facechanger.faceswap.enhance.utils.AppFaceCoinManager;
import com.facechanger.faceswap.enhance.utils.AppFaceImagePickerHelper;
import com.facechanger.faceswap.enhance.utils.AppFacePermissionHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.faceenhance.facechanger.controller.AdManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppFaceAiImageGenActivity extends BaseAppActivity {

    private static final String TAG = "AiImageGenActivity";
    private static final boolean USE_LOTTIE_LOADER = true;
    private Dialog permissionDialog;

    private View llUploadContainer;
    private View rlUploadedFace;
    private ImageView ivUploadedFace;
    private EditText etPrompt;
    private View loaderOverlay;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri selectedFaceUri;
    private Uri cameraUri;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_ai_activity_image_generation);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.faceSwapContent), false);

        loadAds();
        loadSecondAds();

        AdManager.getInstance().preloadBigMediaNative();


        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        llUploadContainer = findViewById(R.id.llUploadContainer);
        rlUploadedFace = findViewById(R.id.rlUploadedFace);
        ivUploadedFace = findViewById(R.id.ivUploadedFace);
        etPrompt = findViewById(R.id.etPrompt);
        loaderOverlay = findViewById(R.id.loaderOverlay);

        setupGalleryLauncher();
        setupCameraLauncher();

        if (llUploadContainer != null) {
            llUploadContainer.setOnClickListener(v -> handleUploadClick());
        }

        View ivRemoveFace = findViewById(R.id.ivRemoveFace);
        if (ivRemoveFace != null) {
            ivRemoveFace.setOnClickListener(v -> removeUploadedFace());
        }

        View btnGenerate = findViewById(R.id.btnGenerate);
        if (btnGenerate != null) {
            btnGenerate.setOnClickListener(v -> performGenerate());
        }

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

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraUri != null) {
                        selectedFaceUri = cameraUri;
                        showUploadedFace(cameraUri);
                    }
                });
    }

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

    private void showUploadedFace(Uri uri) {
        if (llUploadContainer != null) llUploadContainer.setVisibility(View.GONE);
        if (rlUploadedFace != null) rlUploadedFace.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(ivUploadedFace);
    }

    private void removeUploadedFace() {
        if (rlUploadedFace != null) rlUploadedFace.setVisibility(View.GONE);
        if (llUploadContainer != null) llUploadContainer.setVisibility(View.VISIBLE);
        Glide.with(ivUploadedFace).clear(ivUploadedFace);
        selectedFaceUri = null;
    }

    public void handleUploadClick() {
        AppFaceAppDialogController.showPhotoTipsIfNeeded(this, () -> {
            if (AppFacePermissionHelper.hasCameraAndGalleryPermission(this)) {
                AppFaceImagePickerHelper.show(this,
                        () -> {
                            cameraUri = AppFaceImagePickerHelper.createCameraOutputUri(this);
                            if (cameraUri != null) cameraLauncher.launch(cameraUri);
                        },
                        this::openGallery);
            } else {
                AppFacePermissionHelper.requestCameraAndGalleryPermission(this);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(intent);
    }

    private void performGenerate() {
        String prompt = etPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, getString(R.string.app_ai_please_enter_a_prompt_text), Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedFaceUri == null) {
            Toast.makeText(this, getString(R.string.app_ai_please_upload_your_image_text), Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!USE_LOTTIE_LOADER) {
            showLoader(true);
        }

        executor.execute(() -> {
            try {
                File imageFile = copyUriToTempFile(this, selectedFaceUri, "ai_prompt_image.jpg");
                if (imageFile == null) {
                    runOnUiThread(() -> {
                        if (!USE_LOTTIE_LOADER) showLoader(false);
                        Toast.makeText(this, getString(R.string.app_ai_failed_to_read_image_text), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                runOnUiThread(() -> {
                    if (USE_LOTTIE_LOADER) {
                        AppFaceCoinManager.checkAndProceed(AppFaceAiImageGenActivity.this,
                                AppFaceLoadingActivity.ACTION_IMAGE_GENERATE, () -> {
                            Intent intent = new Intent(AppFaceAiImageGenActivity.this, AppFaceLoadingActivity.class);
                            intent.putExtra("action", AppFaceLoadingActivity.ACTION_IMAGE_GENERATE);
                            intent.putExtra("prompt", prompt);
                            intent.putExtra("file_path", imageFile.getAbsolutePath());
                            // Removed original_image_url extra to hide slider in DownloadShareActivity

                            startActivity(intent);
                        });
                    } else {
                        AppFaceApiRepository.editImage(imageFile, prompt, new AppFaceApiRepository.FaceSwapCallback() {
                            @Override
                            public void onSuccess(@NonNull AppFaceFaceSwapResponse response) {
                                showLoader(false);
                                // NOTE: Do NOT delete imageFile here — it's used as the BEFORE image
                                // in DownloadShareActivity. It will be cleaned up when the cache is cleared.

                                if (response.isSuccess() && response.hasResultImage()) {
                                    String resultUrl = response.getImageUrl();
                                    if (resultUrl == null || resultUrl.isEmpty()) {
                                        resultUrl = response.getImageBase64();
                                    }

                                    Intent intent = new Intent(AppFaceAiImageGenActivity.this,
                                            AppFaceDownloadShareActivity.class);
                                    intent.putExtra("image_url", resultUrl);
                                    // Removed original_image_url extra to hide slider in DownloadShareActivity

                                    AppFaceActivityNavHelper.start(AppFaceAiImageGenActivity.this, intent);
                                } else {
                                    cleanupTempFiles(imageFile);
                                    Toast.makeText(AppFaceAiImageGenActivity.this,
                                            response.getMessage() == null || response.getMessage().isEmpty()
                                                    ? "Image generation failed. Please try again."
                                                    : response.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onError(@NonNull String errorMessage) {
                                showLoader(false);
                                cleanupTempFiles(imageFile);
                                Log.e(TAG, "Image generation error: " + errorMessage);
                                Toast.makeText(AppFaceAiImageGenActivity.this, getString(R.string.app_ai_photo_generation_failed_please_text),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "File preparation failed", e);
                runOnUiThread(() -> {
                    if (!USE_LOTTIE_LOADER) showLoader(false);
                    Toast.makeText(this, getString(R.string.app_ai_an_error_occurred_please_text), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

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
            Log.e(TAG, "copyUriToTempFile failed", e);
            return null;
        }
    }

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

    private void showLoader(boolean show) {
        if (loaderOverlay != null) {
            loaderOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppFacePermissionHelper.RC_CAMERA_GALLERY) {
            if (AppFacePermissionHelper.hasCameraAndGalleryPermission(this)) {
                Toast.makeText(this, getString(R.string.app_ai_permission_granted_text), Toast.LENGTH_SHORT).show();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (permissionDialog != null && permissionDialog.isShowing()) {
            if (AppFacePermissionHelper.hasCameraAndGalleryPermission(this)) {
                permissionDialog.dismiss();
                permissionDialog = null;
                openGallery();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = AppFaceAppDialogController.showPermissionDeniedDialog(
                this,
                "Permission Required",
                "Camera and gallery access are needed to upload your face photo for AI image generation. Please enable them in Settings.",
                new AppFaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        AppFacePermissionHelper.openAppSettings(AppFaceAiImageGenActivity.this);
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

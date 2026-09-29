package com.perfect.faceeditor.facechanger.io.view;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.faceeditor.io.callback.InterstitialAdCallback;
import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceApDlogController;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceOnDialogActionListener;
import com.perfect.faceeditor.facechanger.io.model.api.SFS_RefaceFaceSwapResponse;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceActivityNaviHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceApiRepo;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceCoinMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefacePhotoSelectionHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefacePermissionHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SFS_RefaceAiPhotoGenActivity extends SFS_BaseAppActivity {

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
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sfs_face_re_aiactv__image_generation);
        SFS_RefaceUtils.setStatusBarBleed(getWindow(), findViewById(R.id.faceSwapContent), true);


        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
                loadSecondAds();
            }
        });



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
        SFS_RefaceApDlogController.showPhotoTipsIfNeeded(this, () -> {
            if (SFS_RefacePermissionHlpr.hasCameraAndGalleryPermission(this)) {
                SFS_RefacePhotoSelectionHlpr.show(this,
                        () -> {
                            cameraUri = SFS_RefacePhotoSelectionHlpr.createCameraOutputUri(this);
                            if (cameraUri != null) cameraLauncher.launch(cameraUri);
                        },
                        this::openGallery);
            } else {
                SFS_RefacePermissionHlpr.requestCameraAndGalleryPermission(this);
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
            Toast.makeText(this, getString(R.string.sfs_re_ai_please_enter_a_prompt_text), Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedFaceUri == null) {
            Toast.makeText(this, getString(R.string.sfs_re_ai_please_upload_your_image_text), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, getString(R.string.sfs_re_ai_failed_to_read_image_text), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                runOnUiThread(() -> {
                    if (USE_LOTTIE_LOADER) {
                        SFS_RefaceCoinMngr.checkAndProceed(SFS_RefaceAiPhotoGenActivity.this,
                                SFS_RefaceLoadingActivity.ACTION_IMAGE_GENERATE, () -> {
                            Intent intent = new Intent(SFS_RefaceAiPhotoGenActivity.this, SFS_RefaceLoadingActivity.class);
                            intent.putExtra("action", SFS_RefaceLoadingActivity.ACTION_IMAGE_GENERATE);
                            intent.putExtra("prompt", prompt);
                            intent.putExtra("file_path", imageFile.getAbsolutePath());
                            // Removed original_image_url extra to hide slider in DownloadShareActivity

                            startActivity(intent);
                        });
                    } else {
                        SFS_RefaceApiRepo.editImage(imageFile, prompt, new SFS_RefaceApiRepo.FaceSwapCallback() {
                            @Override
                            public void onSuccess(@NonNull SFS_RefaceFaceSwapResponse response) {
                                showLoader(false);
                                // NOTE: Do NOT delete imageFile here — it's used as the BEFORE image
                                // in DownloadShareActivity. It will be cleaned up when the cache is cleared.

                                if (response.isSuccess() && response.hasResultImage()) {
                                    String resultUrl = response.getImageUrl();
                                    if (resultUrl == null || resultUrl.isEmpty()) {
                                        resultUrl = response.getImageBase64();
                                    }

                                    Intent intent = new Intent(SFS_RefaceAiPhotoGenActivity.this,
                                            SFS_RefaceDownloadShareActivity.class);
                                    intent.putExtra("image_url", resultUrl);
                                    // Removed original_image_url extra to hide slider in DownloadShareActivity

                                    SFS_RefaceActivityNaviHlpr.start(SFS_RefaceAiPhotoGenActivity.this, intent);
                                } else {
                                    cleanupTempFiles(imageFile);
                                    Toast.makeText(SFS_RefaceAiPhotoGenActivity.this,
                                            response.getMessage() == null || response.getMessage().isEmpty()
                                                    ? "Image generation failed. Please try again."
                                                    : response.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onError(int statusCode, @NonNull String errorMessage) {
                                showLoader(false);
                                cleanupTempFiles(imageFile);
                                Log.e(TAG, "Image generation error: " + errorMessage);
                                Toast.makeText(SFS_RefaceAiPhotoGenActivity.this, getString(R.string.sfs_re_ai_photo_generation_failed_please_text),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "File preparation failed", e);
                runOnUiThread(() -> {
                    if (!USE_LOTTIE_LOADER) showLoader(false);
                    Toast.makeText(this, getString(R.string.sfs_re_ai_an_err_occurred_please_text), Toast.LENGTH_SHORT).show();
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
        if (requestCode == SFS_RefacePermissionHlpr.RC_CAMERA_GALLERY) {
            if (SFS_RefacePermissionHlpr.hasCameraAndGalleryPermission(this)) {
                Toast.makeText(this, getString(R.string.sfs_re_ai_permission_granted_text), Toast.LENGTH_SHORT).show();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (permissionDialog != null && permissionDialog.isShowing()) {
            if (SFS_RefacePermissionHlpr.hasCameraAndGalleryPermission(this)) {
                permissionDialog.dismiss();
                permissionDialog = null;
                openGallery();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = SFS_RefaceApDlogController.showPermissionDeniedDialog(
                this,
                "Permission Required",
                "Camera and gallery access are needed to upload your face photo for AI image generation. Please enable them in Settings.",
                new SFS_RefaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        SFS_RefacePermissionHlpr.openAppSettings(SFS_RefaceAiPhotoGenActivity.this);
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

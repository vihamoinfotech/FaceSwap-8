package com.facechanger.faceswap.enhance.view;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.faceenhance.facechanger.controller.AdManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppDialogController;
import com.facechanger.faceswap.enhance.controller.OnDialogActionListener;
import com.facechanger.faceswap.enhance.utils.CoinManager;
import com.facechanger.faceswap.enhance.utils.ImagePickerHelper;
import com.facechanger.faceswap.enhance.utils.PermissionHelper;
import com.facechanger.faceswap.enhance.utils.RewardedAdHelper;
import com.facechanger.faceswap.enhance.utils.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceEnhanceActivity extends BaseAppActivity {

    private static final String TAG = "FaceEnhanceActivity";

    private Dialog permissionDialog;

    private LinearLayout llUploadContainer;
    private View framePreview;
    private ShapeableImageView ivPreview;
    private Button btnGenerate;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    private Uri selectedImageUri;
    private Uri cameraUri;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_enhance_face);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.faceEnhanceContent), false);

        loadAds();
        loadSecondAds();

        AdManager.getInstance().preloadBigMediaNative();


        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        llUploadContainer = findViewById(R.id.llUploadContainer);
        framePreview       = findViewById(R.id.framePreview);
        ivPreview          = findViewById(R.id.ivPreview);
        btnGenerate        = findViewById(R.id.btnGenerate);

        setupGalleryLauncher();
        setupCameraLauncher();

        RewardedAdHelper.preload(this);

        llUploadContainer.setOnClickListener(v -> handleUploadClick());
        ivPreview.setOnClickListener(v -> handleUploadClick());
        findViewById(R.id.ivRemove).setOnClickListener(v -> removeImage());

        btnGenerate.setOnClickListener(v -> handleGenerate());
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

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedImageUri = uri;
                            showPreview(uri);
                        }
                    }
                });
    }

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraUri != null) {
                        selectedImageUri = cameraUri;
                        showPreview(cameraUri);
                    }
                });
    }

    private void showPreview(Uri uri) {
        llUploadContainer.setVisibility(View.GONE);
        framePreview.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).centerCrop().into(ivPreview);
    }

    private void removeImage() {
        framePreview.setVisibility(View.GONE);
        llUploadContainer.setVisibility(View.VISIBLE);
        Glide.with(ivPreview).clear(ivPreview);
        selectedImageUri = null;
    }

    public void handleUploadClick() {
        AppDialogController.showPhotoTipsIfNeeded(this, () -> {
            if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
                ImagePickerHelper.show(this,
                        () -> {
                            cameraUri = ImagePickerHelper.createCameraOutputUri(this);
                            if (cameraUri != null) cameraLauncher.launch(cameraUri);
                        },
                        () -> {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            galleryLauncher.launch(intent);
                        });
            } else {
                PermissionHelper.requestCameraAndGalleryPermission(this);
            }
        });
    }

    private void handleGenerate() {
        if (selectedImageUri == null) {
            Toast.makeText(this, getString(R.string.app_face_please_upload_a_photo_text), Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerate.setEnabled(false);

        executor.execute(() -> {
            File imageFile = copyUriToTempFile(this, selectedImageUri, "face_enhance_input.jpg");
            if (imageFile == null) {
                runOnUiThread(() -> {
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.app_face_failed_to_read_image_text), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            runOnUiThread(() -> {
                btnGenerate.setEnabled(true);
                CoinManager.checkAndProceed(FaceEnhanceActivity.this,
                        LottieLoadingActivity.ACTION_ENHANCE_GFPGAN, () -> {
                    Intent intent = new Intent(FaceEnhanceActivity.this, LottieLoadingActivity.class);
                    intent.putExtra("action", LottieLoadingActivity.ACTION_ENHANCE_GFPGAN);
                    intent.putExtra("file_path", imageFile.getAbsolutePath());
                    startActivity(intent);
                });
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.RC_CAMERA_GALLERY) {
            if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
                handleUploadClick();
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
                handleUploadClick();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = AppDialogController.showPermissionDeniedDialog(
                this,
                "Permission Required",
                "Camera and gallery access are needed to upload your photo. Please enable them in Settings.",
                new OnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        PermissionHelper.openAppSettings(FaceEnhanceActivity.this);
                    }

                    @Override
                    public void onDismiss() {
                        permissionDialog = null;
                    }
                });
    }

    private static File copyUriToTempFile(@NonNull Context context,
                                          @NonNull Uri uri,
                                          @NonNull String fileName) {
        try {
            ContentResolver resolver = context.getContentResolver();
            InputStream inputStream  = resolver.openInputStream(uri);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.faceenhance.facechanger.controller.AdManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppFaceAppDialogController;
import com.facechanger.faceswap.enhance.controller.AppFaceOnDialogActionListener;
import com.facechanger.faceswap.enhance.utils.AppFaceCoinManager;
import com.facechanger.faceswap.enhance.utils.AppFaceImagePickerHelper;
import com.facechanger.faceswap.enhance.utils.AppFacePermissionHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppFaceBackgroundReplaceActivity extends BaseAppActivity {

    private static final String TAG = "BackgroundReplaceActivity";

    private Dialog permissionDialog;

    private LinearLayout llUploadContainer;
    private View framePreview;
    private ShapeableImageView ivPreview;
    private EditText etPrompt;
    private Button btnGenerate;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    private Uri selectedImageUri;
    private Uri cameraUri;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_background_replace);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.bgReplaceContent), false);

        loadAds();
        loadSecondAds();

        AdManager.getInstance().preloadBigMediaNative();

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        llUploadContainer = findViewById(R.id.llUploadContainer);
        framePreview       = findViewById(R.id.framePreview);
        ivPreview          = findViewById(R.id.ivPreview);
        etPrompt           = findViewById(R.id.etPrompt);
        btnGenerate        = findViewById(R.id.btnGenerate);

        setupGalleryLauncher();
        setupCameraLauncher();

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
        AppFaceAppDialogController.showPhotoTipsIfNeeded(this, () -> {
            if (AppFacePermissionHelper.hasCameraAndGalleryPermission(this)) {
                AppFaceImagePickerHelper.show(this,
                        () -> {
                            cameraUri = AppFaceImagePickerHelper.createCameraOutputUri(this);
                            if (cameraUri != null) cameraLauncher.launch(cameraUri);
                        },
                        () -> {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            galleryLauncher.launch(intent);
                        });
            } else {
                AppFacePermissionHelper.requestCameraAndGalleryPermission(this);
            }
        });
    }

    private void handleGenerate() {
        String prompt = etPrompt.getText().toString().trim();
        if (selectedImageUri == null) {
            Toast.makeText(this, getString(R.string.app_background_please_upload_a_photo_text), Toast.LENGTH_SHORT).show();
            return;
        }
        if (prompt.isEmpty()) {
            Toast.makeText(this, getString(R.string.app_background_please_describe_the_new_text), Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerate.setEnabled(false);

        executor.execute(() -> {
            File imageFile = copyUriToTempFile(this, selectedImageUri, "bg_replace_input.jpg");
            if (imageFile == null) {
                runOnUiThread(() -> {
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.app_background_failed_to_read_image_text), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            runOnUiThread(() -> {
                btnGenerate.setEnabled(true);
                AppFaceCoinManager.checkAndProceed(AppFaceBackgroundReplaceActivity.this,
                        AppFaceLoadingActivity.ACTION_BG_REPLACE, () -> {
                    Intent intent = new Intent(AppFaceBackgroundReplaceActivity.this, AppFaceLoadingActivity.class);
                    intent.putExtra("action", AppFaceLoadingActivity.ACTION_BG_REPLACE);
                    intent.putExtra("file_path", imageFile.getAbsolutePath());
                    intent.putExtra("prompt", prompt);
                    startActivity(intent);
                });
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AppFacePermissionHelper.RC_CAMERA_GALLERY) {
            if (AppFacePermissionHelper.hasCameraAndGalleryPermission(this)) {
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
            if (AppFacePermissionHelper.hasCameraAndGalleryPermission(this)) {
                permissionDialog.dismiss();
                permissionDialog = null;
                handleUploadClick();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = AppFaceAppDialogController.showPermissionDeniedDialog(
                this,
                "Permission Required",
                "Camera and gallery access are needed to upload your photo. Please enable them in Settings.",
                new AppFaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        AppFacePermissionHelper.openAppSettings(AppFaceBackgroundReplaceActivity.this);
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

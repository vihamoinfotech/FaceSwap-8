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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.google.android.material.imageview.ShapeableImageView;
import com.faceeditor.io.callback.InterstitialAdCallback;
import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceApDlogController;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceOnDialogActionListener;
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

public class SFS_RefaceRemoveBgActivity extends SFS_BaseAppActivity {

    private static final String TAG = "SFS_RefaceRemoveBgActivity";

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
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sfs_face_reactv__rmv_bg);
        SFS_RefaceUtils.setStatusBarBleed(getWindow(), findViewById(R.id.bgReplaceContent), true);

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
                loadSecondAds();
            }
        });


        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        llUploadContainer = findViewById(R.id.llUploadContainer);
        framePreview       = findViewById(R.id.framePreview);
        ivPreview          = findViewById(R.id.ivPreview);

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
        SFS_RefaceApDlogController.showPhotoTipsIfNeeded(this, () -> {
            if (SFS_RefacePermissionHlpr.hasCameraAndGalleryPermission(this)) {
                SFS_RefacePhotoSelectionHlpr.show(this,
                        () -> {
                            cameraUri = SFS_RefacePhotoSelectionHlpr.createCameraOutputUri(this);
                            if (cameraUri != null) cameraLauncher.launch(cameraUri);
                        },
                        () -> {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            galleryLauncher.launch(intent);
                        });
            } else {
                SFS_RefacePermissionHlpr.requestCameraAndGalleryPermission(this);
            }
        });
    }

    private void handleGenerate() {
        if (selectedImageUri == null) {
            Toast.makeText(this, getString(R.string.sfs_re_bkg_please_upload_a_photo_text), Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerate.setEnabled(false);

        executor.execute(() -> {
            File imageFile = copyUriToTempFile(this, selectedImageUri, "remove_bg_input.jpg");
            if (imageFile == null) {
                runOnUiThread(() -> {
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.sfs_re_bkg_failed_to_read_image_text), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            runOnUiThread(() -> {
                btnGenerate.setEnabled(true);
                SFS_RefaceCoinMngr.checkAndProceed(SFS_RefaceRemoveBgActivity.this,
                        SFS_RefaceLoadingActivity.ACTION_REMOVE_BG, () -> {
                    Intent intent = new Intent(SFS_RefaceRemoveBgActivity.this, SFS_RefaceLoadingActivity.class);
                    intent.putExtra("action", SFS_RefaceLoadingActivity.ACTION_REMOVE_BG);
                    intent.putExtra("file_path", imageFile.getAbsolutePath());
                    startActivity(intent);
                    finish();
                });
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SFS_RefacePermissionHlpr.RC_CAMERA_GALLERY) {
            if (SFS_RefacePermissionHlpr.hasCameraAndGalleryPermission(this)) {
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
            if (SFS_RefacePermissionHlpr.hasCameraAndGalleryPermission(this)) {
                permissionDialog.dismiss();
                permissionDialog = null;
                handleUploadClick();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = SFS_RefaceApDlogController.showPermissionDeniedDialog(
                this,
                "Permission Required",
                "Camera and gallery access are needed to upload your photo. Please enable them in Settings.",
                new SFS_RefaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        SFS_RefacePermissionHlpr.openAppSettings(SFS_RefaceRemoveBgActivity.this);
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

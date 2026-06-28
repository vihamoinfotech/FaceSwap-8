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
import com.faceenhance.facechanger.activity.BaseAdActivity;
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

public class CoupleFaceSwapActivity extends BaseAppActivity {

    private static final String TAG = "CoupleFaceSwapActivity";

    private Dialog permissionDialog;

    private LinearLayout llSelectTarget;
    private View frameTargetPreview;
    private ShapeableImageView ivTargetPreview;
    private LinearLayout llUploadFace;
    private View frameFacePreview;
    private ShapeableImageView ivSourceFace;
    private Button btnGenerate;

    private ActivityResultLauncher<Intent> faceGalleryLauncher;
    private ActivityResultLauncher<Intent> targetGalleryLauncher;
    private ActivityResultLauncher<Uri> cameraFaceLauncher;
    private ActivityResultLauncher<Uri> cameraTargetLauncher;

    private Uri selectedFaceUri;
    private Uri selectedTargetUri;
    private Uri cameraFaceUri;
    private Uri cameraTargetUri;

    private int pendingAction = 0; // 1 = face, 2 = target

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_couple_swap);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.coupleSwapContent), false);

        loadAds();
        loadSecondAds();

        AdManager.getInstance().preloadBigMediaNative();


        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        llSelectTarget   = findViewById(R.id.llSelectTarget);
        frameTargetPreview = findViewById(R.id.frameTargetPreview);
        ivTargetPreview  = findViewById(R.id.ivTargetPreview);
        llUploadFace     = findViewById(R.id.llUploadFace);
        frameFacePreview = findViewById(R.id.frameFacePreview);
        ivSourceFace     = findViewById(R.id.ivSourceFace);
        btnGenerate      = findViewById(R.id.btnGenerate);

        setupTargetGalleryLauncher();
        setupFaceGalleryLauncher();
        setupCameraLaunchers();

        RewardedAdHelper.preload(this);

        llSelectTarget.setOnClickListener(v -> handleUploadTargetClick());
        ivTargetPreview.setOnClickListener(v -> handleUploadTargetClick());
        findViewById(R.id.ivRemoveTarget).setOnClickListener(v -> removeTargetImage());

        llUploadFace.setOnClickListener(v -> handleUploadFaceClick());
        ivSourceFace.setOnClickListener(v -> handleUploadFaceClick());
        findViewById(R.id.ivRemoveFace).setOnClickListener(v -> removeFaceImage());

        btnGenerate.setOnClickListener(v -> handleGenerateClick());
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

    private void setupCameraLaunchers() {
        cameraFaceLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraFaceUri != null) {
                        selectedFaceUri = cameraFaceUri;
                        pendingAction = 0;
                        llUploadFace.setVisibility(View.GONE);
                        frameFacePreview.setVisibility(View.VISIBLE);
                        Glide.with(this).load(cameraFaceUri).centerCrop().into(ivSourceFace);
                    }
                });

        cameraTargetLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraTargetUri != null) {
                        selectedTargetUri = cameraTargetUri;
                        pendingAction = 0;
                        llSelectTarget.setVisibility(View.GONE);
                        frameTargetPreview.setVisibility(View.VISIBLE);
                        Glide.with(this).load(cameraTargetUri).centerCrop().into(ivTargetPreview);
                    }
                });
    }

    private void setupTargetGalleryLauncher() {
        targetGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedTargetUri = uri;
                            llSelectTarget.setVisibility(View.GONE);
                            frameTargetPreview.setVisibility(View.VISIBLE);
                            Glide.with(this).load(uri).centerCrop().into(ivTargetPreview);
                        }
                    }
                });
    }

    private void setupFaceGalleryLauncher() {
        faceGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedFaceUri = uri;
                            llUploadFace.setVisibility(View.GONE);
                            frameFacePreview.setVisibility(View.VISIBLE);
                            Glide.with(this).load(uri).centerCrop().into(ivSourceFace);
                        }
                    }
                });
    }

    private void handleUploadTargetClick() {
        if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
            ImagePickerHelper.show(this,
                    () -> {
                        cameraTargetUri = ImagePickerHelper.createCameraOutputUri(this);
                        if (cameraTargetUri != null) cameraTargetLauncher.launch(cameraTargetUri);
                    },
                    () -> {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        targetGalleryLauncher.launch(intent);
                    });
        } else {
            pendingAction = 2;
            PermissionHelper.requestCameraAndGalleryPermission(this);
        }
    }

    private void handleUploadFaceClick() {
        AppDialogController.showPhotoTipsIfNeeded(this, () -> {
            if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
                ImagePickerHelper.show(this,
                        () -> {
                            cameraFaceUri = ImagePickerHelper.createCameraOutputUri(this);
                            if (cameraFaceUri != null) cameraFaceLauncher.launch(cameraFaceUri);
                        },
                        () -> {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            faceGalleryLauncher.launch(intent);
                        });
            } else {
                pendingAction = 1;
                PermissionHelper.requestCameraAndGalleryPermission(this);
            }
        });
    }

    private void removeTargetImage() {
        frameTargetPreview.setVisibility(View.GONE);
        llSelectTarget.setVisibility(View.VISIBLE);
        Glide.with(ivTargetPreview).clear(ivTargetPreview);
        selectedTargetUri = null;
    }

    private void removeFaceImage() {
        frameFacePreview.setVisibility(View.GONE);
        llUploadFace.setVisibility(View.VISIBLE);
        Glide.with(ivSourceFace).clear(ivSourceFace);
        selectedFaceUri = null;
    }

    private void handleGenerateClick() {
        if (selectedTargetUri == null) {
            Toast.makeText(this, getString(R.string.CoupleFaceSwapActivity_please_select_the_coupletar), Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedFaceUri == null) {
            Toast.makeText(this, getString(R.string.CoupleFaceSwapActivity_please_upload_your_face), Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerate.setEnabled(false);

        executor.execute(() -> {
            File targetFile = copyUriToTempFile(this, selectedTargetUri, "couple_target.jpg");
            if (targetFile == null) {
                runOnUiThread(() -> {
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.CoupleFaceSwapActivity_failed_to_prepare_target), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            File faceFile = copyUriToTempFile(this, selectedFaceUri, "couple_source.jpg");
            if (faceFile == null) {
                runOnUiThread(() -> {
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.CoupleFaceSwapActivity_failed_to_read_face), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            runOnUiThread(() -> {
                btnGenerate.setEnabled(true);
                CoinManager.checkAndProceed(CoupleFaceSwapActivity.this,
                        LottieLoadingActivity.ACTION_COUPLE_SWAP, () -> {
                    Intent intent = new Intent(CoupleFaceSwapActivity.this, LottieLoadingActivity.class);
                    intent.putExtra("action", LottieLoadingActivity.ACTION_COUPLE_SWAP);
                    intent.putExtra("source_path", targetFile.getAbsolutePath());
                    intent.putExtra("target_path", faceFile.getAbsolutePath());
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
                if (pendingAction == 1) handleUploadFaceClick();
                else if (pendingAction == 2) handleUploadTargetClick();
                pendingAction = 0;
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
                if (pendingAction == 1) handleUploadFaceClick();
                else if (pendingAction == 2) handleUploadTargetClick();
                pendingAction = 0;
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = AppDialogController.showPermissionDeniedDialog(
                this,
                "Permission Required",
                "Camera and gallery access are needed to upload photos. Please enable them in Settings.",
                new OnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        PermissionHelper.openAppSettings(CoupleFaceSwapActivity.this);
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

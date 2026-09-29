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

public class SFS_RefaceCoupleFaceSwapActivity extends SFS_BaseAppActivity {

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
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sfs_face_reactv__swap_couple);
        SFS_RefaceUtils.setStatusBarBleed(getWindow(), findViewById(R.id.coupleSwapContent), true);



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
        if (SFS_RefacePermissionHlpr.hasCameraAndGalleryPermission(this)) {
            SFS_RefacePhotoSelectionHlpr.show(this,
                    () -> {
                        cameraTargetUri = SFS_RefacePhotoSelectionHlpr.createCameraOutputUri(this);
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
            SFS_RefacePermissionHlpr.requestCameraAndGalleryPermission(this);
        }
    }

    private void handleUploadFaceClick() {
        SFS_RefaceApDlogController.showPhotoTipsIfNeeded(this, () -> {
            if (SFS_RefacePermissionHlpr.hasCameraAndGalleryPermission(this)) {
                SFS_RefacePhotoSelectionHlpr.show(this,
                        () -> {
                            cameraFaceUri = SFS_RefacePhotoSelectionHlpr.createCameraOutputUri(this);
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
                SFS_RefacePermissionHlpr.requestCameraAndGalleryPermission(this);
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
            Toast.makeText(this, getString(R.string.sfs_re_couple_face_please_select_the_couple_text), Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedFaceUri == null) {
            Toast.makeText(this, getString(R.string.sfs_re_couple_face_please_upload_your_face_text), Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerate.setEnabled(false);

        executor.execute(() -> {
            File targetFile = copyUriToTempFile(this, selectedTargetUri, "couple_target.jpg");
            if (targetFile == null) {
                runOnUiThread(() -> {
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.sfs_re_couple_face_failed_to_prepare_target_text), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            File faceFile = copyUriToTempFile(this, selectedFaceUri, "couple_source.jpg");
            if (faceFile == null) {
                runOnUiThread(() -> {
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.sfs_re_couple_face_failed_to_read_face_text), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            runOnUiThread(() -> {
                btnGenerate.setEnabled(true);
                SFS_RefaceCoinMngr.checkAndProceed(SFS_RefaceCoupleFaceSwapActivity.this,
                        SFS_RefaceLoadingActivity.ACTION_COUPLE_SWAP, () -> {
                    Intent intent = new Intent(SFS_RefaceCoupleFaceSwapActivity.this, SFS_RefaceLoadingActivity.class);
                    intent.putExtra("action", SFS_RefaceLoadingActivity.ACTION_COUPLE_SWAP);
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
        if (requestCode == SFS_RefacePermissionHlpr.RC_CAMERA_GALLERY) {
            if (SFS_RefacePermissionHlpr.hasCameraAndGalleryPermission(this)) {
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
            if (SFS_RefacePermissionHlpr.hasCameraAndGalleryPermission(this)) {
                permissionDialog.dismiss();
                permissionDialog = null;
                if (pendingAction == 1) handleUploadFaceClick();
                else if (pendingAction == 2) handleUploadTargetClick();
                pendingAction = 0;
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = SFS_RefaceApDlogController.showPermissionDeniedDialog(
                this,
                "Permission Required",
                "Camera and gallery access are needed to upload photos. Please enable them in Settings.",
                new SFS_RefaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        SFS_RefacePermissionHlpr.openAppSettings(SFS_RefaceCoupleFaceSwapActivity.this);
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

package com.facechanger.faceswap.enhance.view;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.facechanger.faceswap.enhance.utils.VideoPickerHelper;
import com.facechanger.faceswap.enhance.utils.PermissionHelper;
import com.facechanger.faceswap.enhance.utils.RewardedAdHelper;
import com.facechanger.faceswap.enhance.utils.SessionManager;
import com.facechanger.faceswap.enhance.utils.Tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoFaceSwapActivity extends BaseAppActivity {

    private static final String TAG = "VideoFaceSwapActivity";
    private Dialog permissionDialog;

    private LinearLayout llSelectTarget;
    private View frameTargetPreview;
    private ShapeableImageView ivVideoPreviewThumb;
    private LinearLayout llUploadFace;
    private View frameFacePreview;
    private ShapeableImageView ivSourceFace;
    private Button btnGenerate;
    private View llCostContainer;
    private TextView tvFeatureCost;

    private ActivityResultLauncher<Intent> faceGalleryLauncher;
    private ActivityResultLauncher<Intent> videoGalleryLauncher;
    private ActivityResultLauncher<Uri> cameraFaceLauncher;
    private ActivityResultLauncher<Uri> cameraVideoLauncher;

    private Uri selectedFaceUri;
    private Uri selectedVideoUri;
    private Uri cameraFaceUri;
    private Uri cameraVideoUri;
    private File cameraVideoFile;

    private int pendingAction = 0; // 1 = face, 2 = video
    
    private double currentCalculatedCost = 0.0;
    private boolean isCoinSystemEnabled;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_face_swap);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.multiSwapContent), false);

        loadAds();
        loadSecondAds();

        AdManager.getInstance().preloadBigMediaNative();

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        llSelectTarget = findViewById(R.id.llSelectTarget);
        frameTargetPreview = findViewById(R.id.frameTargetPreview);
        ivVideoPreviewThumb = findViewById(R.id.ivVideoPreviewThumb);
        llUploadFace = findViewById(R.id.llUploadFace);
        frameFacePreview = findViewById(R.id.frameFacePreview);
        ivSourceFace = findViewById(R.id.ivSourceFace);
        btnGenerate = findViewById(R.id.btnGenerate);
        llCostContainer = findViewById(R.id.llCostContainer);
        tvFeatureCost = findViewById(R.id.tvFeatureCost);

        isCoinSystemEnabled = com.facechanger.faceswap.enhance.utils.AppSystem.isFeatureEnabled(
                com.facechanger.faceswap.enhance.utils.AppSystem.KEY_COIN_SYSTEM_ENABLED) &&
                com.facechanger.faceswap.enhance.utils.AppSystem.isFeatureEnabled(
                com.facechanger.faceswap.enhance.utils.AppSystem.KEY_REQUIRE_COINS_VIDEO_FACE_SWAP);

        TextView tvVideoLimitNote = findViewById(R.id.tvVideoLimitNote);
        if (tvVideoLimitNote != null) {
            com.facechanger.faceswap.enhance.model.api.SplashDataResponse.VideoFaceSwapConfig videoConfig = 
                    SessionManager.getInstance().getVideoFaceSwapConfig();
            tvVideoLimitNote.setText(String.format(Locale.US, "Note: Max duration is %ds and max file size is %dMB.", 
                    videoConfig.getMaxDurationSeconds(), videoConfig.getMaxUploadSizeMb()));
        }

        setupVideoGalleryLauncher();
        setupFaceGalleryLauncher();
        setupCameraLaunchers();

        RewardedAdHelper.preload(this);

        llSelectTarget.setOnClickListener(v -> handleUploadVideoClick());
        ivVideoPreviewThumb.setOnClickListener(v -> handleUploadVideoClick());
        findViewById(R.id.ivRemoveTarget).setOnClickListener(v -> removeVideoFile());

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
        cameraVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakeVideo(),
                success -> {
                    if (cameraVideoFile != null && cameraVideoFile.exists() && cameraVideoFile.length() > 0) {
                        validateAndSetVideo(Uri.fromFile(cameraVideoFile));
                    } else if (cameraVideoUri != null && checkUriValidity(cameraVideoUri)) {
                        validateAndSetVideo(cameraVideoUri);
                    } else {
                        Log.w(TAG, "Camera video recording cancelled or empty file.");
                    }
                });
    }

    private boolean checkUriValidity(Uri uri) {
        try {
            ContentResolver resolver = getContentResolver();
            try (InputStream in = resolver.openInputStream(uri)) {
                return in != null;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void setupVideoGalleryLauncher() {
        videoGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri videoUri = result.getData().getData();
                        if (videoUri != null) {
                            validateAndSetVideo(videoUri);
                        }
                    }
                }
        );
    }

    private void setupFaceGalleryLauncher() {
        faceGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            selectedFaceUri = imageUri;
                            llUploadFace.setVisibility(View.GONE);
                            frameFacePreview.setVisibility(View.VISIBLE);
                            Glide.with(this).load(imageUri).centerCrop().into(ivSourceFace);
                        }
                    }
                }
        );
    }

    private void validateAndSetVideo(Uri videoUri) {
        // Force check: ensure it is a video and not an image
        if (videoUri != null) {
            String path = videoUri.getPath();
            if (path != null) {
                String pathLower = path.toLowerCase();
                if (pathLower.endsWith(".jpg") || pathLower.endsWith(".jpeg") || 
                    pathLower.endsWith(".png") || pathLower.endsWith(".webp") || 
                    pathLower.endsWith(".heic") || pathLower.endsWith(".bmp")) {
                    Toast.makeText(this, "Please select a valid video. Images are not accepted.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            String mimeType = getContentResolver().getType(videoUri);
            if (mimeType != null && mimeType.startsWith("image/")) {
                Toast.makeText(this, "Please select a valid video. Images are not accepted.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Fetch dynamic configurations
        com.facechanger.faceswap.enhance.model.api.SplashDataResponse.VideoFaceSwapConfig videoConfig = 
                SessionManager.getInstance().getVideoFaceSwapConfig();
        int minSec = videoConfig.getMinDurationSeconds();
        int maxSec = videoConfig.getMaxDurationSeconds();
        int maxSizeMb = videoConfig.getMaxUploadSizeMb();

        long fileSize = getFileSize(videoUri);
        if (fileSize > (long) maxSizeMb * 1024 * 1024) {
            Toast.makeText(this, String.format(Locale.US, "Video size exceeds the limit of %d MB.", maxSizeMb), Toast.LENGTH_LONG).show();
            return;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, videoUri);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long timeInMillisec = Long.parseLong(time);
            
            if (timeInMillisec > (long) maxSec * 1000) {
                Toast.makeText(this, String.format(Locale.US, "Video duration exceeds the limit of %d seconds.", maxSec), Toast.LENGTH_LONG).show();
                return;
            }
            
            // Calculate cost (Ceil to next whole second)
            double durationSec = Math.ceil(timeInMillisec / 1000.0);
            
            // Minimum billing duration is dynamic minSec
            if (durationSec < minSec) {
                durationSec = minSec;
            }
            
            double costPerSecond = SessionManager.getInstance().getFeatureCost(LottieLoadingActivity.ACTION_VIDEO_FACE_SWAP);
            currentCalculatedCost = durationSec * costPerSecond;
            
            updateCostUI();
            
            selectedVideoUri = videoUri;
            llSelectTarget.setVisibility(View.GONE);
            frameTargetPreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(videoUri).centerCrop().into(ivVideoPreviewThumb);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to read video", e);
            Toast.makeText(this, "Invalid video. Please select or record a valid video file.", Toast.LENGTH_LONG).show();
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    private void updateCostUI() {
        if (!isCoinSystemEnabled || currentCalculatedCost <= 0) {
            llCostContainer.setVisibility(View.GONE);
            return;
        }
        llCostContainer.setVisibility(View.VISIBLE);
        tvFeatureCost.setText(String.format(Locale.US, getString(R.string.video_swap_cost_label), CoinManager.formatCost(currentCalculatedCost)));
    }

    private long getFileSize(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (!cursor.isNull(sizeIndex)) {
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size;
            }
            cursor.close();
        }
        return 0;
    }

    private void handleUploadVideoClick() {
        if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
            VideoPickerHelper.show(this,
                    () -> {
                        try {
                            File dir = new File(getCacheDir(), "camera_videos");
                            if (!dir.exists()) {
                                dir.mkdirs();
                            }
                            cameraVideoFile = new File(dir, "capture_" + System.currentTimeMillis() + ".mp4");
                            cameraVideoUri = androidx.core.content.FileProvider.getUriForFile(
                                    this, getPackageName() + ".fileprovider", cameraVideoFile);
                            cameraVideoLauncher.launch(cameraVideoUri);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to launch video camera", e);
                            Toast.makeText(this, "Failed to launch camera", Toast.LENGTH_SHORT).show();
                        }
                    },
                    () -> {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("video/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        videoGalleryLauncher.launch(intent);
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

    private void removeVideoFile() {
        if (frameTargetPreview != null) frameTargetPreview.setVisibility(View.GONE);
        if (llSelectTarget != null) llSelectTarget.setVisibility(View.VISIBLE);
        Glide.with(ivVideoPreviewThumb).clear(ivVideoPreviewThumb);
        selectedVideoUri = null;
        currentCalculatedCost = 0.0;
        updateCostUI();
    }

    private void removeFaceImage() {
        if (frameFacePreview != null) frameFacePreview.setVisibility(View.GONE);
        if (llUploadFace != null) llUploadFace.setVisibility(View.VISIBLE);
        Glide.with(ivSourceFace).clear(ivSourceFace);
        selectedFaceUri = null;
    }

    private void handleGenerateClick() {
        if (selectedVideoUri == null) {
            Toast.makeText(this, getString(R.string.video_swap_please_select_video), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFaceUri == null) {
            Toast.makeText(this, getString(R.string.MultiSwapActivity_please_add_your_face), Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerate.setEnabled(false);

        executor.execute(() -> {
            File targetFile = copyUriToTempFile(this, selectedVideoUri, "swap_target.mp4");
            if (targetFile == null) {
                runOnUiThread(() -> {
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.MultiSwapActivity_failed_to_prepare_target), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            File faceFile = copyUriToTempFile(this, selectedFaceUri, "swap_source.jpg");
            if (faceFile == null) {
                runOnUiThread(() -> {
                    btnGenerate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.MultiSwapActivity_failed_to_read_face), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            runOnUiThread(() -> {
                btnGenerate.setEnabled(true);
                CoinManager.checkAndProceedWithCost(VideoFaceSwapActivity.this,
                        LottieLoadingActivity.ACTION_VIDEO_FACE_SWAP, currentCalculatedCost, () -> {
                    Intent intent = new Intent(VideoFaceSwapActivity.this, LottieLoadingActivity.class);
                    intent.putExtra("action", LottieLoadingActivity.ACTION_VIDEO_FACE_SWAP);
                    // "source_path" parameter maps to the target video file in API request
                    intent.putExtra("source_path", targetFile.getAbsolutePath());
                    // "target_path" parameter maps to the uploaded face file in API request
                    intent.putExtra("target_path", faceFile.getAbsolutePath());
                    intent.putExtra("video_coin_cost", currentCalculatedCost);
                    startActivity(intent);
                });
            });
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
            Log.e("VideoFaceSwapActivity", "copyUriToTempFile failed", e);
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.RC_CAMERA_GALLERY) {
            if (PermissionHelper.hasCameraAndGalleryPermission(this)) {
                if (pendingAction == 1) {
                    handleUploadFaceClick();
                } else if (pendingAction == 2) {
                    handleUploadVideoClick();
                }
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
                if (pendingAction == 1) {
                    handleUploadFaceClick();
                } else if (pendingAction == 2) {
                    handleUploadVideoClick();
                }
                pendingAction = 0;
            }
        }
    }

    private void showPermissionDeniedDialog() {
        permissionDialog = AppDialogController.showPermissionDeniedDialog(
                this,
                getString(R.string.permission_denied_permission_required),
                getString(R.string.permission_denied_camera_and_gallery_access),
                new OnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        PermissionHelper.openAppSettings(VideoFaceSwapActivity.this);
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

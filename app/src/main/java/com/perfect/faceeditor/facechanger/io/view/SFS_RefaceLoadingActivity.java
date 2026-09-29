package com.perfect.faceeditor.facechanger.io.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceApDlogController;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceOnDialogActionListener;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceOnServerBusyListener;
import com.perfect.faceeditor.facechanger.io.model.api.SFS_RefaceFaceSwapResponse;
import com.perfect.faceeditor.facechanger.io.model.api.SFS_RefaceMovieFaceSwapResponse;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceActivityNaviHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceApiRepo;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceAppSystem;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceNetworkUtility;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceSessionMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dedicated loading screen with a high-quality Lottie animation.
 * <p>
 * Handles the actual API network call for generating the AI image in the background.
 * If successful, routes to DownloadShareActivity.
 * If it fails, finishes automatically to return to the AI Generator screen with error handling.
 */
public class SFS_RefaceLoadingActivity extends SFS_BaseAppActivity {

    private static final String TAG = "LottieLoadingActivity";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static final String ACTION_AI_IMAGE = "action_ai_image";
    public static final String ACTION_FACE_SWAP = "action_face_swap";
    public static final String ACTION_MULTI_FACE_SWAP = "action_multi_face_swap";
    public static final String ACTION_REMOVE_BG = "action_remove_bg";
    public static final String ACTION_UPSCALE = "action_upscale";
    public static final String ACTION_COUPLE_SWAP = "action_couple_swap";
    public static final String ACTION_BG_REPLACE = "action_bg_replace";
    public static final String ACTION_ENHANCE_GFPGAN = "action_enhance_gfpgan";
    public static final String ACTION_ENHANCE_FACE = "action_enhance_face";
    public static final String ACTION_HAIR_COLOR = "action_hair_color";
    public static final String ACTION_HAIR_STYLE = "action_hair_style";
    public static final String ACTION_VIRTUAL_TRY_ON = "action_virtual_try_on";
    public static final String ACTION_TEXT_TO_IMAGE = "action_text_to_image";
    public static final String ACTION_IMAGE_GENERATE = "action_image_generate";
    public static final String ACTION_VIDEO_FACE_SWAP = "action_video_face_swap";

    /** Key for the result URL returned to calling activity. */
    public static final String EXTRA_RESULT_URL = "result_url";

    private String action;
    private String prompt;
    private String originalImageUrl; // passed through to DownloadShareActivity for the slider
    private File imageFile1; // used as source or main image
    private File imageFile2; // used as target

    private android.os.Handler timeoutHandler;
    private Runnable timeoutRunnable;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sfs_face_re_commonactv__lottie_loading_view);
//        SFS_RefaceUtils.setEdgetoEdge(getWindow(), findViewById(android.R.id.content), false, true);
        SFS_RefaceUtils.setStatusBarBleed(getWindow(), findViewById(R.id.faceSwapLoadingContent), true);

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
            }
        });

        setupSystemInsets();
        disableBackButton();

        action = getIntent().getStringExtra("action");
        originalImageUrl = getIntent().getStringExtra("original_image_url");

        if (ACTION_VIDEO_FACE_SWAP.equals(action)) {
            android.widget.TextView tvLoadingMessage = findViewById(R.id.tvLoadingMessage);
            if (tvLoadingMessage != null) {
                tvLoadingMessage.setText("Creating your AI Video…");
            }
        }

        if (action == null) {
            handleError("Missing action parameter.");
            return;
        }

        // Preflight: Network check
        if (!SFS_RefaceNetworkUtility.isConnected()) {
            showNoInternetDialog();
            return;
        }

        timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        timeoutRunnable = () -> {
            android.widget.TextView tvTimeoutMessage = findViewById(R.id.tvTimeoutMessage);
            if (tvTimeoutMessage != null) {
                tvTimeoutMessage.setVisibility(android.view.View.VISIBLE);
                tvTimeoutMessage.setAlpha(0f);
                tvTimeoutMessage.animate().alpha(1f).setDuration(500).start();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 15000);

        dispatchAction();
    }

    /**
     * Routes to the correct API call based on the action string.
     * Separated into its own method so retry can re-invoke it.
     */
    private void dispatchAction() {
        switch (action) {
            case ACTION_AI_IMAGE: {
                prompt = getIntent().getStringExtra("prompt");
                String filePath = getIntent().getStringExtra("file_path");

                if (prompt == null || filePath == null) {
                    handleError("Missing generation data.");
                    return;
                }

                imageFile1 = new File(filePath);
                if (!imageFile1.exists()) {
                    handleError("Failed to read image.");
                    return;
                }

                startAiGeneration(prompt, imageFile1);
                break;
            }

            case ACTION_FACE_SWAP:
            case ACTION_MULTI_FACE_SWAP: {
                String sourcePath = getIntent().getStringExtra("source_path");
                String targetPath = getIntent().getStringExtra("target_path");

                if (sourcePath == null || targetPath == null) {
                    handleError("Missing face swap data.");
                    return;
                }

                imageFile1 = new File(sourcePath);
                imageFile2 = new File(targetPath);

                if (!imageFile1.exists() || !imageFile2.exists()) {
                    handleError("Failed to read required images.");
                    return;
                }

                if (ACTION_FACE_SWAP.equals(action)) {
                    startFaceSwap(imageFile1, imageFile2);
                } else {
                    startMultiFaceSwap(imageFile1, imageFile2);
                }
                break;
            }

            case ACTION_REMOVE_BG:
            case ACTION_UPSCALE: {
                String filePath = getIntent().getStringExtra("file_path");
                if (filePath == null) {
                    handleError("Missing image data.");
                    return;
                }

                imageFile1 = new File(filePath);
                if (!imageFile1.exists()) {
                    handleError("Failed to read image.");
                    return;
                }

                if (ACTION_REMOVE_BG.equals(action)) {
                    startRemoveBg(imageFile1);
                } else {
                    startUpscale(imageFile1);
                }
                break;
            }

            case ACTION_COUPLE_SWAP: {
                String sourcePath = getIntent().getStringExtra("source_path");
                String targetPath = getIntent().getStringExtra("target_path");
                if (sourcePath == null || targetPath == null) {
                    handleError("Missing couple swap data.");
                    return;
                }
                imageFile1 = new File(sourcePath);
                imageFile2 = new File(targetPath);
                if (!imageFile1.exists() || !imageFile2.exists()) {
                    handleError("Failed to read required images.");
                    return;
                }
                startCoupleSwap(imageFile1, imageFile2);
                break;
            }


            case ACTION_ENHANCE_GFPGAN: {
                String filePath = getIntent().getStringExtra("file_path");
                if (filePath == null) {
                    handleError("Missing image data.");
                    return;
                }
                imageFile1 = new File(filePath);
                if (!imageFile1.exists()) {
                    handleError("Failed to read image.");
                    return;
                }
                startEnhanceGfpgan(imageFile1);
                break;
            }

            case ACTION_ENHANCE_FACE: {
                String filePath = getIntent().getStringExtra("file_path");
                if (filePath == null) {
                    handleError("Missing image data.");
                    return;
                }
                imageFile1 = new File(filePath);
                if (!imageFile1.exists()) {
                    handleError("Failed to read image.");
                    return;
                }
                startEnhanceFace(imageFile1);
                break;
            }

            case ACTION_VIRTUAL_TRY_ON: {
                String sourcePath = getIntent().getStringExtra("source_path");
                String targetPath = getIntent().getStringExtra("target_path");
                if (sourcePath == null || targetPath == null) {
                    handleError("Missing virtual try-on data.");
                    return;
                }
                imageFile1 = new File(sourcePath);
                imageFile2 = new File(targetPath);
                if (!imageFile1.exists() || !imageFile2.exists()) {
                    handleError("Failed to read required images.");
                    return;
                }
            }

            case ACTION_VIDEO_FACE_SWAP: {
                String videoPath = getIntent().getStringExtra("source_path");
                String facePath = getIntent().getStringExtra("target_path");
                if (videoPath == null || facePath == null) {
                    handleError("Missing video face swap data.");
                    return;
                }
                imageFile1 = new File(videoPath);
                imageFile2 = new File(facePath);
                if (!imageFile1.exists() || !imageFile2.exists()) {
                    handleError("Failed to read required files.");
                    return;
                }
                startVideoFaceSwap(imageFile1, imageFile2);
                break;
            }

            case ACTION_TEXT_TO_IMAGE: {
                prompt = getIntent().getStringExtra("prompt");
                if (prompt == null) {
                    handleError("Missing prompt.");
                    return;
                }
                startTextToImage(prompt);
                break;
            }

            case ACTION_IMAGE_GENERATE: {
                prompt = getIntent().getStringExtra("prompt");
                String genFilePath = getIntent().getStringExtra("file_path");

                if (prompt == null || genFilePath == null) {
                    handleError("Missing generation data.");
                    return;
                }

                imageFile1 = new File(genFilePath);
                if (!imageFile1.exists()) {
                    handleError("Failed to read image.");
                    return;
                }

                startAiGeneration(prompt, imageFile1);
                break;
            }

            default:
                handleError("Unknown action.");
                break;
        }
    }

    // ──────────────────────────────────────────────
    //  API calls
    // ──────────────────────────────────────────────

    private void startAiGeneration(String prompt, File file) {
        SFS_RefaceApiRepo.editImage(file, prompt, new SFS_RefaceApiRepo.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull SFS_RefaceFaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleResponse(response, "Image generation failed. Please try again.");
            }

            public void onError(int statusCode, @NonNull String errorMessage) {
                Log.e(TAG, "Image generation error: " + errorMessage);
                SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), "Image generation failed HTTP:" + statusCode);

                handleErrorResponse(errorMessage);
            }
        });
    }

    private void startFaceSwap(File sourceFile, File targetFile) {
        SFS_RefaceApiRepo.faceSwapBasic(sourceFile, targetFile, true, new SFS_RefaceApiRepo.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull SFS_RefaceFaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleResponse(response, "Face swap failed. Please try again.");
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                Log.e(TAG, "Face swap error: " + errorMessage);
                SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), "Face swap failed HTTP:" + statusCode);

                handleErrorResponse(errorMessage);
            }
        });
    }

    private void startMultiFaceSwap(File sourceFile, File targetFile) {
        SFS_RefaceApiRepo.faceSwapMulti(sourceFile, targetFile, true, new SFS_RefaceApiRepo.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull SFS_RefaceFaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleResponse(response, "Face swap failed. Please try again.");
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                Log.e(TAG, "Face swap error: " + errorMessage);
                SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), "Multi swap failed HTTP:" + statusCode);

                handleErrorResponse(errorMessage);
            }
        });
    }

    private void startRemoveBg(File file) {
        SFS_RefaceApiRepo.removeBackground(file, new SFS_RefaceApiRepo.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull SFS_RefaceFaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleEditImageResponse(response, "Remove background failed. Please try again.");
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                Log.e(TAG, "Remove BG error: " + errorMessage);
                SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), "Remove BG failed HTTP:" + statusCode);

                handleErrorResponse(errorMessage);
            }
        });
    }

    private void startUpscale(File file) {
        SFS_RefaceApiRepo.upscaleImage(file, new SFS_RefaceApiRepo.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull SFS_RefaceFaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleEditImageResponse(response, "Image enhancement failed. Please try again.");
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                Log.e(TAG, "Upscale error: " + errorMessage);
                SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), "Upscale failed HTTP:" + statusCode);

                handleErrorResponse(errorMessage);
            }
        });
    }

    private void startCoupleSwap(File sourceFile, File targetFile) {
        SFS_RefaceApiRepo.faceSwapCouple(sourceFile, targetFile, true, new SFS_RefaceApiRepo.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull SFS_RefaceFaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleResponse(response, "Couple face swap failed. Please try again.");
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                Log.e(TAG, "Couple swap error: " + errorMessage);
                SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), "Couple swap failed HTTP:" + statusCode);

                handleErrorResponse(errorMessage);
            }
        });
    }


    private void startEnhanceGfpgan(File file) {
        SFS_RefaceApiRepo.enhanceGfpgan(file, new SFS_RefaceApiRepo.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull SFS_RefaceFaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleEditImageResponse(response, "Face enhancement failed. Please try again.");
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                Log.e(TAG, "GFPGAN error: " + errorMessage);
                SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), "GFPGAN failed HTTP:" + statusCode);

                handleErrorResponse(errorMessage);
            }
        });
    }

    private void startEnhanceFace(File file) {
        SFS_RefaceApiRepo.enhanceFace(file, new SFS_RefaceApiRepo.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull SFS_RefaceFaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleEditImageResponse(response, "Face enhancement failed. Please try again.");
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                Log.e(TAG, "Face enhance error: " + errorMessage);
                SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), "Face enhance failed HTTP:" + statusCode);
                handleErrorResponse(errorMessage);
            }
        });
    }

    private void startTextToImage(String prompt) {
        SFS_RefaceApiRepo.textToImage(prompt, new SFS_RefaceApiRepo.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull SFS_RefaceFaceSwapResponse response) {
                handleResponse(response, "Image generation failed. Please try again.");
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                Log.e(TAG, "Text-to-image error: " + errorMessage);
                SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), "Text-to-image failed HTTP:" + statusCode);
                handleErrorResponse(errorMessage);
            }
        });
    }


    private void handleErrorResponse(String errorMessage) {

        try {
            org.json.JSONObject obj = new org.json.JSONObject(errorMessage);
            String msg = obj.optString("message", "");
            showErrorDialog(msg);
        } catch (Exception e) {
            showErrorDialog("");
        }
    }

    // ──────────────────────────────────────────────
    //  Video Face Swap
    // ──────────────────────────────────────────────

    private void startVideoFaceSwap(File videoFile, File faceFile) {
        if (!SFS_RefaceNetworkUtility.isConnected()) {
            showNoInternetDialog();
            return;
        }

        executor.execute(() -> SFS_RefaceApiRepo.videoFaceSwap(videoFile, faceFile, new SFS_RefaceApiRepo.VideoFaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull SFS_RefaceMovieFaceSwapResponse response) {
                runOnUiThread(() -> handleVideoResponse(response));
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                runOnUiThread(() -> {
                    SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), "Video face swap failed (HTTP " + statusCode);
                    handleErrorResponse(errorMessage);
                });
            }
        }));
    }

    private void handleVideoResponse(SFS_RefaceMovieFaceSwapResponse response) {
        if (isFinishing() || isDestroyed()) return;

        if (response.isSuccess() && response.hasResultVideo()) {
            // Cost is already calculated and passed via intent for video, but we can also deduct it here if needed
            // The cost is dynamic, so we'll use the intent extra we passed from VideoFaceSwapActivity
            double dynamicCost = getIntent().getDoubleExtra("video_coin_cost", 0.0);
            if (dynamicCost > 0) {
                SFS_RefaceSessionMngr.getInstance().deductCredits(dynamicCost);
            }

            Intent intent = new Intent(SFS_RefaceLoadingActivity.this, SFS_RefaceDownloadShareActivity.class);
            intent.putExtra("image_url", response.getVideoUrl()); // DownloadShareActivity uses image_url for the media url
            intent.putExtra("is_video", true);
            SFS_RefaceActivityNaviHlpr.start(SFS_RefaceLoadingActivity.this, intent);
            finish();
        } else {
            SFS_RefaceAppSystem.showDebugToast(getApplicationContext(),
                    response.getMessage() == null || response.getMessage().isEmpty()
                            ? "Video processing failed" : response.getMessage());
            showErrorDialog("");
        }
    }

    // ──────────────────────────────────────────────
    //  Response Handlers (Images)
    // ──────────────────────────────────────────────

    /**
     * Handles responses for AI image, face swap, multi-swap — navigates to DownloadShareActivity.
     */
    private void handleResponse(SFS_RefaceFaceSwapResponse response, String defaultErrorMsg) {
        if (isFinishing() || isDestroyed()) return;

        if (response.isSuccess() && response.hasResultImage()) {
            // Deduct credits locally on success only
            if (action != null) {
                double cost = SFS_RefaceSessionMngr.getInstance().getFeatureCost(action);
                SFS_RefaceSessionMngr.getInstance().deductCredits(cost);
            }

            String resultUrl = response.getImageUrl();
            if (resultUrl == null || resultUrl.isEmpty()) {
                resultUrl = response.getImageBase64();
            }

            // Navigate directly to download screen and destroy this loading screen in history
            Intent intent = new Intent(SFS_RefaceLoadingActivity.this, SFS_RefaceDownloadShareActivity.class);
            intent.putExtra("image_url", resultUrl);
            if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                intent.putExtra("original_image_url", originalImageUrl);
            }
            SFS_RefaceActivityNaviHlpr.start(SFS_RefaceLoadingActivity.this, intent);
            finish();
        } else {
            SFS_RefaceAppSystem.showDebugToast(getApplicationContext(),
                    response.getMessage() == null || response.getMessage().isEmpty()
                            ? defaultErrorMsg : response.getMessage());
            showErrorDialog("");
        }
    }

    /**
     * Handles responses for Remove BG / Upscale — navigates to DownloadShareActivity.
     * Same flow as face swap and AI image (Option A).
     */
    private void handleEditImageResponse(SFS_RefaceFaceSwapResponse response, String defaultErrorMsg) {
        if (isFinishing() || isDestroyed()) return;

        if (response.isSuccess() && response.hasResultImage()) {
            // Deduct credits locally on success only
            if (action != null) {
                double cost = SFS_RefaceSessionMngr.getInstance().getFeatureCost(action);
                SFS_RefaceSessionMngr.getInstance().deductCredits(cost);
            }

            String resultUrl = response.getImageUrl();
            if (resultUrl == null || resultUrl.isEmpty()) {
                resultUrl = response.getImageBase64();
            }

            // Redirect to editor to allow further modifications
            Intent intent = new Intent(SFS_RefaceLoadingActivity.this, SFS_RefaceEditPhotoActivity.class);
            intent.putExtra("image_url", resultUrl);
            if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                intent.putExtra("original_image_url", originalImageUrl);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            SFS_RefaceActivityNaviHlpr.start(SFS_RefaceLoadingActivity.this, intent);
            finish();
        } else {
            SFS_RefaceAppSystem.showDebugToast(getApplicationContext(),
                    response.getMessage() == null || response.getMessage().isEmpty()
                            ? defaultErrorMsg : response.getMessage());
            showErrorDialog("");
        }
    }

    // ──────────────────────────────────────────────
    //  Error handling
    // ──────────────────────────────────────────────

    /**
     * Shows the appropriate error dialog based on {@link SFS_RefaceAppSystem#USE_SERVER_BUSY_DIALOG}.
     */
    private void showErrorDialog(String msg) {
        if (isFinishing() || isDestroyed()) return;

        if (SFS_RefaceAppSystem.USE_SERVER_BUSY_DIALOG) {
            SFS_RefaceApDlogController.showServerBusyDialog(this, msg, new SFS_RefaceOnServerBusyListener() {
                @Override
                public void onRetry() {
                    if (!SFS_RefaceNetworkUtility.isConnected()) {
                        showNoInternetDialog();
                        return;
                    }
                    dispatchAction();
                }

                @Override
                public void onGoBack() {
                    cleanupTempFiles();
                    finish();
                }
            });
        } else {
            SFS_RefaceApDlogController.showRetryDialog(
                    this,
                    R.drawable.sfs_re_trans_close_choose,
                    getString(R.string.sfs_re_api_err_title_text),
                    getString(R.string.sfs_re_api_err_text),
                    getString(R.string.sfs_re_api_retry_button_text),
                    new SFS_RefaceOnDialogActionListener() {
                        @Override
                        public void onPositiveClick() {
                            if (!SFS_RefaceNetworkUtility.isConnected()) {
                                showNoInternetDialog();
                                return;
                            }
                            dispatchAction();
                        }

                        @Override
                        public void onDismiss() {
                            cleanupTempFiles();
                            finish();
                        }
                    });
        }
    }

    private void showNoInternetDialog() {
        if (isFinishing() || isDestroyed()) return;
        SFS_RefaceApDlogController.showRetryDialog(
                this,
                R.drawable.sfs_re_trans_close_choose,
                getString(R.string.sfs_face_re_no_internet_title_text),
                getString(R.string.sfs_re_no_internet_desc_text),
                getString(R.string.sfs_re_api_retry_button_text),
                new SFS_RefaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        if (SFS_RefaceNetworkUtility.isConnected()) {
                            dispatchAction();
                        } else {
                            showNoInternetDialog();
                        }
                    }

                    @Override
                    public void onDismiss() {
                        cleanupTempFiles();
                        finish();
                    }
                });
    }

    private void handleError(String message) {
        if (isFinishing() || isDestroyed()) return;
        SFS_RefaceAppSystem.showDebugToast(getApplicationContext(), message);
        showErrorDialog("");
    }

    // ──────────────────────────────────────────────
    //  Utilities
    // ──────────────────────────────────────────────

    private void cleanupTempFiles() {
        executor.execute(() -> {
            if (imageFile1 != null && imageFile1.exists()) {
                //noinspection ResultOfMethodCallIgnored
                imageFile1.delete();
            }
            if (imageFile2 != null && imageFile2.exists()) {
                //noinspection ResultOfMethodCallIgnored
                imageFile2.delete();
            }
        });
    }

    /**
     * Cleans up temp files but preserves the file referenced by {@link #originalImageUrl}
     * so that {@link SFS_RefaceDownloadShareActivity} can load it as the "Before" image in the slider.
     */
    private void cleanupTempFilesKeepOriginal() {
        executor.execute(() -> {
            String keepPath = originalImageUrl;
            if (imageFile1 != null && imageFile1.exists()) {
                if (keepPath == null || !imageFile1.getAbsolutePath().equals(keepPath)) {
                    //noinspection ResultOfMethodCallIgnored
                    imageFile1.delete();
                }
            }
//            if (imageFile2 != null && imageFile2.exists()) {
//                if (keepPath == null || !imageFile2.getAbsolutePath().equals(keepPath)) {
//                    //noinspection ResultOfMethodCallIgnored
//                    imageFile2.delete();
//                }
//            }
        });
    }

    private void setupSystemInsets() {
        View content = findViewById(R.id.lottieLoadingContent);
        if (content != null) {
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    private void disableBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(SFS_RefaceLoadingActivity.this, getString(R.string.sfs_re_loader_please_wait_generation_text), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
        super.onDestroy();
        executor.shutdown();
    }
}

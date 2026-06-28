package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.faceenhance.facechanger.activity.BaseAdActivity;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppDialogController;
import com.facechanger.faceswap.enhance.controller.OnDialogActionListener;
import com.facechanger.faceswap.enhance.controller.OnServerBusyListener;
import com.facechanger.faceswap.enhance.model.api.FaceSwapResponse;
import com.facechanger.faceswap.enhance.utils.ActivityNavHelper;
import com.facechanger.faceswap.enhance.utils.ApiRepository;
import com.facechanger.faceswap.enhance.utils.AppSystem;
import com.facechanger.faceswap.enhance.utils.NetworkUtils;
import com.facechanger.faceswap.enhance.utils.SessionManager;
import com.facechanger.faceswap.enhance.utils.Tools;

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
public class LottieLoadingActivity extends BaseAppActivity {

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

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottie_loading);
        Tools.setEdgetoEdge(getWindow(), findViewById(android.R.id.content), false, true);

        loadAds();

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
        if (!NetworkUtils.isConnected()) {
            showNoInternetDialog();
            return;
        }

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

            case ACTION_BG_REPLACE: {
                String filePath = getIntent().getStringExtra("file_path");
                prompt = getIntent().getStringExtra("prompt");
                if (filePath == null || prompt == null) {
                    handleError("Missing background replace data.");
                    return;
                }
                imageFile1 = new File(filePath);
                if (!imageFile1.exists()) {
                    handleError("Failed to read image.");
                    return;
                }
                startBgReplace(imageFile1, prompt);
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
        ApiRepository.editImage(file, prompt, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleResponse(response, "Image generation failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "Image generation error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    private void startFaceSwap(File sourceFile, File targetFile) {
        ApiRepository.faceSwapBasic(sourceFile, targetFile, true, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleResponse(response, "Face swap failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "Face swap error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    private void startMultiFaceSwap(File sourceFile, File targetFile) {
        ApiRepository.faceSwapMulti(sourceFile, targetFile, true, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleResponse(response, "Face swap failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "Face swap error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    private void startRemoveBg(File file) {
        ApiRepository.removeBackground(file, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleEditImageResponse(response, "Remove background failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "Remove BG error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    private void startUpscale(File file) {
        ApiRepository.upscaleImage(file, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleEditImageResponse(response, "Image enhancement failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "Upscale error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    private void startCoupleSwap(File sourceFile, File targetFile) {
        ApiRepository.faceSwapCouple(sourceFile, targetFile, true, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleResponse(response, "Couple face swap failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "Couple swap error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    private void startBgReplace(File file, String prompt) {
        ApiRepository.replaceBackground(file, prompt, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleResponse(response, "Background replace failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "BG replace error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    private void startEnhanceGfpgan(File file) {
        ApiRepository.enhanceGfpgan(file, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleEditImageResponse(response, "Face enhancement failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "GFPGAN error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    private void startEnhanceFace(File file) {
        ApiRepository.enhanceFace(file, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleEditImageResponse(response, "Face enhancement failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "Face enhance error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    private void startVirtualTryOn(File personFile, File productFile) {
        ApiRepository.virtualTryOn(personFile, productFile, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                cleanupTempFilesKeepOriginal();
                handleResponse(response, "Virtual try-on failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "Virtual try-on error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    private void startTextToImage(String prompt) {
        ApiRepository.textToImage(prompt, new ApiRepository.FaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull FaceSwapResponse response) {
                handleResponse(response, "Image generation failed. Please try again.");
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                Log.e(TAG, "Text-to-image error: " + errorMessage);
                AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                showErrorDialog();
            }
        });
    }

    // ──────────────────────────────────────────────
    //  Video Face Swap
    // ──────────────────────────────────────────────

    private void startVideoFaceSwap(File videoFile, File faceFile) {
        if (!NetworkUtils.isConnected()) {
            showNoInternetDialog();
            return;
        }

        executor.execute(() -> ApiRepository.videoFaceSwap(videoFile, faceFile, new ApiRepository.VideoFaceSwapCallback() {
            @Override
            public void onSuccess(@NonNull com.facechanger.faceswap.enhance.model.api.VideoFaceSwapResponse response) {
                runOnUiThread(() -> handleVideoResponse(response));
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                runOnUiThread(() -> {
                    AppSystem.showDebugToast(getApplicationContext(), errorMessage);
                    showErrorDialog();
                });
            }
        }));
    }

    private void handleVideoResponse(com.facechanger.faceswap.enhance.model.api.VideoFaceSwapResponse response) {
        if (isFinishing() || isDestroyed()) return;

        if (response.isSuccess() && response.hasResultVideo()) {
            // Cost is already calculated and passed via intent for video, but we can also deduct it here if needed
            // The cost is dynamic, so we'll use the intent extra we passed from VideoFaceSwapActivity
            double dynamicCost = getIntent().getDoubleExtra("video_coin_cost", 0.0);
            if (dynamicCost > 0) {
                SessionManager.getInstance().deductCredits(dynamicCost);
            }

            Intent intent = new Intent(LottieLoadingActivity.this, DownloadShareActivity.class);
            intent.putExtra("image_url", response.getVideoUrl()); // DownloadShareActivity uses image_url for the media url
            intent.putExtra("is_video", true);
            ActivityNavHelper.start(LottieLoadingActivity.this, intent);
            finish();
        } else {
            AppSystem.showDebugToast(getApplicationContext(),
                    response.getMessage() == null || response.getMessage().isEmpty()
                            ? "Video processing failed" : response.getMessage());
            showErrorDialog();
        }
    }

    // ──────────────────────────────────────────────
    //  Response Handlers (Images)
    // ──────────────────────────────────────────────

    /**
     * Handles responses for AI image, face swap, multi-swap — navigates to DownloadShareActivity.
     */
    private void handleResponse(FaceSwapResponse response, String defaultErrorMsg) {
        if (isFinishing() || isDestroyed()) return;

        if (response.isSuccess() && response.hasResultImage()) {
            // Deduct credits locally on success only
            if (action != null) {
                double cost = SessionManager.getInstance().getFeatureCost(action);
                SessionManager.getInstance().deductCredits(cost);
            }

            String resultUrl = response.getImageUrl();
            if (resultUrl == null || resultUrl.isEmpty()) {
                resultUrl = response.getImageBase64();
            }

            // Navigate directly to download screen and destroy this loading screen in history
            Intent intent = new Intent(LottieLoadingActivity.this, DownloadShareActivity.class);
            intent.putExtra("image_url", resultUrl);
            if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                intent.putExtra("original_image_url", originalImageUrl);
            }
            ActivityNavHelper.start(LottieLoadingActivity.this, intent);
            finish();
        } else {
            AppSystem.showDebugToast(getApplicationContext(),
                    response.getMessage() == null || response.getMessage().isEmpty()
                            ? defaultErrorMsg : response.getMessage());
            showErrorDialog();
        }
    }

    /**
     * Handles responses for Remove BG / Upscale — navigates to DownloadShareActivity.
     * Same flow as face swap and AI image (Option A).
     */
    private void handleEditImageResponse(FaceSwapResponse response, String defaultErrorMsg) {
        if (isFinishing() || isDestroyed()) return;

        if (response.isSuccess() && response.hasResultImage()) {
            // Deduct credits locally on success only
            if (action != null) {
                double cost = SessionManager.getInstance().getFeatureCost(action);
                SessionManager.getInstance().deductCredits(cost);
            }

            String resultUrl = response.getImageUrl();
            if (resultUrl == null || resultUrl.isEmpty()) {
                resultUrl = response.getImageBase64();
            }

            // Navigate to download/share screen (same as face swap, AI image)
            Intent intent = new Intent(LottieLoadingActivity.this, DownloadShareActivity.class);
            intent.putExtra("image_url", resultUrl);
            if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                intent.putExtra("original_image_url", originalImageUrl);
            }
            ActivityNavHelper.start(LottieLoadingActivity.this, intent);
            finish();
        } else {
            AppSystem.showDebugToast(getApplicationContext(),
                    response.getMessage() == null || response.getMessage().isEmpty()
                            ? defaultErrorMsg : response.getMessage());
            showErrorDialog();
        }
    }

    // ──────────────────────────────────────────────
    //  Error handling
    // ──────────────────────────────────────────────

    /**
     * Shows the appropriate error dialog based on {@link AppSystem#USE_SERVER_BUSY_DIALOG}.
     */
    private void showErrorDialog() {
        if (isFinishing() || isDestroyed()) return;

        if (AppSystem.USE_SERVER_BUSY_DIALOG) {
            AppDialogController.showServerBusyDialog(this, new OnServerBusyListener() {
                @Override
                public void onRetry() {
                    if (!NetworkUtils.isConnected()) {
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
            AppDialogController.showRetryDialog(
                    this,
                    R.drawable.ic_trans_close,
                    getString(R.string.dialog_api_error_title),
                    getString(R.string.dialog_api_error_msg),
                    getString(R.string.dialog_retry_button),
                    new OnDialogActionListener() {
                        @Override
                        public void onPositiveClick() {
                            if (!NetworkUtils.isConnected()) {
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
        AppDialogController.showRetryDialog(
                this,
                R.drawable.ic_trans_close,
                getString(R.string.dialog_no_internet_title),
                getString(R.string.dialog_no_internet_msg),
                getString(R.string.dialog_retry_button),
                new OnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        if (NetworkUtils.isConnected()) {
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
        AppSystem.showDebugToast(getApplicationContext(), message);
        showErrorDialog();
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
     * so that {@link DownloadShareActivity} can load it as the "Before" image in the slider.
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
                Toast.makeText(LottieLoadingActivity.this, getString(R.string.LottieLoadingActivity_please_wait_generation_in), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

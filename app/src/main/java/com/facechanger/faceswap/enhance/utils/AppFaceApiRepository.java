package com.facechanger.faceswap.enhance.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.facechanger.faceswap.enhance.model.api.AppFaceFaceSwapResponse;
import com.facechanger.faceswap.enhance.model.api.AppFaceHistoryResponse;
import com.facechanger.faceswap.enhance.model.api.AppFaceSplashDataResponse;
import com.facechanger.faceswap.enhance.model.api.AppFaceTemplateCategory;
import com.facechanger.faceswap.enhance.model.api.AppFaceVideoFaceSwapResponse;
import com.faceenhance.facechanger.controller.AdManager;
import com.faceenhance.facechanger.controller.FirebaseManager;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralised API repository for all network calls.
 * <p>
 * Each method wraps the {@link AppFaceApiClient} with proper endpoint paths,
 * request body construction, and response parsing. Activities call
 * these methods via typed callbacks instead of dealing with raw JSON.
 * <p>
 * All callbacks are delivered on the <b>main thread</b>.
 */
public final class AppFaceApiRepository {

    private static final String BASE_URL = AppFaceAppSystem.isDebugMode() ? "https://acc-faceswap.runasp.net" : "https://faceswap.runasp.net";
    private static final int IMAGE_PROCESS_TIMEOUT_MS = 180_000; // 3 minutes

    private AppFaceApiRepository() { /* non-instantiable */ }

    // ══════════════════════════════════════════════════
    //  Typed callback interfaces
    // ══════════════════════════════════════════════════

    /** Callback for splash data API. */
    public interface SplashDataCallback {
        void onSuccess(@NonNull AppFaceSplashDataResponse response, String responseBody);
        void onError(@NonNull String errorMessage);
    }

    /** Callback for templates list API. */
    public interface TemplatesCallback {
        void onSuccess(@NonNull List<AppFaceTemplateCategory> categories);
        void onError(@NonNull String errorMessage);
    }

    /** Callback for face swap API. */
    public interface FaceSwapCallback {
        void onSuccess(@NonNull AppFaceFaceSwapResponse response);
        void onError(@NonNull String errorMessage);
    }

    // ══════════════════════════════════════════════════
    //  Initialisation
    // ══════════════════════════════════════════════════

    /**
     * Configures the global {@link AppFaceApiClient} with the base URL.
     * Call once on app startup (e.g. in SplashActivity).
     */
    public static void initialise() {
        AppFaceApiClient.getInstance().setBaseUrl(BASE_URL);
    }

    /**
     * @return The configured base URL for constructing asset URLs
     */
    @NonNull
    public static String getBaseUrl() {
        return BASE_URL;
    }

    // ══════════════════════════════════════════════════
    //  POST /api/User/splash_data
    // ══════════════════════════════════════════════════

    /**
     * Fetches session data on app launch. The response contains the
     * auth token, user info, app configuration, and feature costs.
     *
     * @param context  Any context (for DeviceUtils)
     * @param callback Typed callback with parsed response
     */
    public static void fetchSplashData(@NonNull Context context,
                                       @NonNull String referrer,
                                       @NonNull SplashDataCallback callback) {

        AppFaceDeviceUtils.getDeviceId(context, deviceId -> {
            try {

                JSONObject body = new JSONObject();
                body.put("deviceId", AppFaceAppSystem.isDebugMode() ? (deviceId + "_test") : deviceId);
//                body.put("deviceId", AppSystem.isDebugMode() ? "4d015d16-1a11-5f9c-fac1-58a854e24c45_test" : deviceId);
//                body.put("deviceId", AppSystem.isDebugMode() ? "DA5EAEB6DB860CE8EDC4C7BFE3344069" : deviceId);
                body.put("deviceType", AppFaceDeviceUtils.getDeviceType());
                body.put("is_prm", AdManager.getInstance().isPremiumUser());
                body.put("appVersion", AppFaceDeviceUtils.getAppVersion(context));
                body.put("referrer", referrer);

                AppFaceApiClient.getInstance().post("/api/User/splash_data", body.toString(),
                        new AppFaceApiClient.ApiCallback() {
                            @Override
                            public void onSuccess(@NonNull String responseBody) {
                                try {
                                    AppFaceSplashDataResponse response = AppFaceSplashDataResponse.fromJson(responseBody);
                                    callback.onSuccess(response, responseBody);
                                } catch (Exception e) {
                                    callback.onError("Failed to parse splash data: " + e.getMessage());
                                }
                            }

                            @Override
                            public void onError(int statusCode, @NonNull String errorMessage) {
                                FirebaseManager.getInstance().logEvent("SPLASH_API_FAILED");
                                callback.onError("Splash data failed (HTTP " + statusCode + "): " + errorMessage);
                            }
                        });
            } catch (Exception e) {
                callback.onError("Failed to build splash request: " + e.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/templates/list
    // ══════════════════════════════════════════════════

    /**
     * Fetches all available templates for the home screen.
     * <p>
     * The API expects an empty JSON body {@code {}} and returns
     * templates as an array (either at root or inside a "message" key).
     *
     * @param callback Typed callback with parsed template list
     */
    public static AppFaceApiCall fetchTemplates(@NonNull TemplatesCallback callback) {
        // The GetTemplatesRequest schema has no fields — send empty object
        String body = "{}";

        return AppFaceApiClient.getInstance().post("/api/templates/list", body,
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            List<AppFaceTemplateCategory> categories = AppFaceTemplateCategory.fromApiResponse(responseBody);
                            callback.onSuccess(categories);
                        } catch (Exception e) {
                            callback.onError("Failed to parse templates: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        FirebaseManager.getInstance().logEvent("TEMPLATE_LIST_FAILED");

                        callback.onError("Templates failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/faceswap/basic
    // ══════════════════════════════════════════════════

    /**
     * Performs a basic face swap. Sends two images (source face + target body)
     * as multipart/form-data.
     *
     * @param sourceImage The file containing the user's face photo
     * @param targetImage The file containing the template/target image
     * @param enhance     Whether to enhance the result image quality
     * @param callback    Typed callback with parsed ProcessingResult
     */
    public static void faceSwapBasic(@NonNull File sourceImage,
                                     @NonNull File targetImage,
                                     boolean enhance,
                                     @NonNull FaceSwapCallback callback) {
        // Build file parameters
        Map<String, File> files = new HashMap<>();
        files.put("sourceImage", sourceImage);
        files.put("targetImage", targetImage);

        // Build text parameters
        Map<String, String> fields = new HashMap<>();
        fields.put("enhance", String.valueOf(enhance));

        AppFaceApiClient.getInstance().multipartUpload(
                "/api/Image/faceswap/basic",
                files,
                fields,
                IMAGE_PROCESS_TIMEOUT_MS,
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse face swap result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        FirebaseManager.getInstance().logEvent("BASIC_FAILED");

                        callback.onError("Face swap failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/faceswap/multi
    // ══════════════════════════════════════════════════

    /**
     * Performs a multiple face swap. Sends two images (source face + target body)
     * as multipart/form-data. Swaps all faces in the target body matched by the source face.
     *
     * @param sourceImage The file containing the user's face photo
     * @param targetImage The file containing the template/group photo
     * @param enhance     Whether to enhance the result image quality
     * @param callback    Typed callback with parsed ProcessingResult
     */
    public static void faceSwapMulti(@NonNull File sourceImage,
                                     @NonNull File targetImage,
                                     boolean enhance,
                                     @NonNull FaceSwapCallback callback) {
        // Build file parameters
        Map<String, File> files = new HashMap<>();
        files.put("sourceImage", sourceImage);
        files.put("targetImage", targetImage);

        // Build text parameters
        Map<String, String> fields = new HashMap<>();
        fields.put("enhance", String.valueOf(enhance));

        AppFaceApiClient.getInstance().multipartUpload(
                "/api/Image/faceswap/multi",
                files,
                fields,
                IMAGE_PROCESS_TIMEOUT_MS,
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse multi-face swap result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        FirebaseManager.getInstance().logEvent("MULTI_FAILED");

                        callback.onError("Multi face swap failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }
    // ══════════════════════════════════════════════════
    //  POST /api/Image/background/remove
    // ══════════════════════════════════════════════════

    /**
     * Removes the background from an image.
     *
     * @param imageFile The file containing the user's photo
     * @param callback  Typed callback with parsed ProcessingResult
     */
    public static void removeBackground(@NonNull File imageFile,
                                        @NonNull FaceSwapCallback callback) {
        AppFaceApiClient.getInstance().multipartImageUpload(
                "/api/Image/background/remove",
                imageFile,
                "image",
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse remove background result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        FirebaseManager.getInstance().logEvent("BACKGROUND_REMOVE_FAILED");

                        callback.onError("Remove background failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/upscale
    // ══════════════════════════════════════════════════

    /**
     * Upscales/enhances the given image.
     *
     * @param imageFile The file containing the user's photo
     * @param callback  Typed callback with parsed ProcessingResult
     */
    public static void upscaleImage(@NonNull File imageFile,
                                    @NonNull FaceSwapCallback callback) {
        AppFaceApiClient.getInstance().multipartImageUpload(
                "/api/Image/upscale/pro",
                imageFile,
                "image",
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse upscale result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        FirebaseManager.getInstance().logEvent("UPSCALE_PRO_FAILED");

                        callback.onError("Upscale failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/edit
    // ══════════════════════════════════════════════════

    /**
     * Edits an image with a text prompt.
     *
     * @param imageFile The file containing the user's photo
     * @param prompt    The text prompt describing the edit
     * @param callback  Typed callback with parsed ProcessingResult
     */
    public static void editImage(@NonNull File imageFile,
                                 @NonNull String prompt,
                                 @NonNull FaceSwapCallback callback) {
        Map<String, File> files = new HashMap<>();
        files.put("image", imageFile);

        Map<String, String> fields = new HashMap<>();
        fields.put("prompt", prompt);

        AppFaceApiClient.getInstance().multipartUpload(
                "/api/Image/edit_image",
                files,
                fields,
                IMAGE_PROCESS_TIMEOUT_MS,
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse edit image result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        FirebaseManager.getInstance().logEvent("EDIT_IMAGE_FAILED");

                        callback.onError("Edit image failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/faceswap/couple
    // ══════════════════════════════════════════════════

    /**
     * Performs a couple face swap. Swaps a face in a couple/duo target photo.
     *
     * @param sourceImage The file containing the user's face photo
     * @param targetImage The file containing the couple/target image
     * @param enhance     Whether to enhance the result image quality
     * @param callback    Typed callback with parsed result
     */
    public static void faceSwapCouple(@NonNull File sourceImage,
                                      @NonNull File targetImage,
                                      boolean enhance,
                                      @NonNull FaceSwapCallback callback) {
        Map<String, File> files = new HashMap<>();
        files.put("sourceImage", sourceImage);
        files.put("targetImage", targetImage);

        Map<String, String> fields = new HashMap<>();
        fields.put("enhance", String.valueOf(enhance));

        AppFaceApiClient.getInstance().multipartUpload(
                "/api/Image/faceswap/couple",
                files, fields, IMAGE_PROCESS_TIMEOUT_MS,
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse couple swap result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        callback.onError("Couple swap failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/background/replace
    // ══════════════════════════════════════════════════

    /**
     * Replaces the background of an image using an AI text prompt.
     *
     * @param imageFile The file containing the user's photo
     * @param prompt    Text description of the desired background
     * @param callback  Typed callback with parsed result
     */
    public static void replaceBackground(@NonNull File imageFile,
                                         @NonNull String prompt,
                                         @NonNull FaceSwapCallback callback) {
        Map<String, File> files = new HashMap<>();
        files.put("image", imageFile);

        Map<String, String> fields = new HashMap<>();
        fields.put("prompt", prompt);

        AppFaceApiClient.getInstance().multipartUpload(
                "/api/Image/background/replace",
                files, fields, IMAGE_PROCESS_TIMEOUT_MS,
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse BG replace result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        callback.onError("BG replace failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/enhance/gfpgan
    // ══════════════════════════════════════════════════

    /**
     * Enhances a face photo using GFPGAN AI face restoration.
     *
     * @param imageFile The file containing the user's photo
     * @param callback  Typed callback with parsed result
     */
    public static void enhanceGfpgan(@NonNull File imageFile,
                                     @NonNull FaceSwapCallback callback) {
        AppFaceApiClient.getInstance().multipartImageUpload(
                "/api/Image/enhance/gfpgan",
                imageFile, "image",
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse GFPGAN result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        callback.onError("GFPGAN enhance failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/enhance/face
    // ══════════════════════════════════════════════════

    /**
     * Enhances facial features in a photo using AI.
     *
     * @param imageFile The file containing the user's photo
     * @param callback  Typed callback with parsed result
     */
    public static void enhanceFace(@NonNull File imageFile,
                                   @NonNull FaceSwapCallback callback) {
        AppFaceApiClient.getInstance().multipartImageUpload(
                "/api/Image/enhance/face",
                imageFile, "image",
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse face enhance result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        FirebaseManager.getInstance().logEvent("ENHANCE_FACE_FAILED");

                        callback.onError("Face enhance failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/virtual-try-on
    // ══════════════════════════════════════════════════

    /**
     * Virtually tries on clothing/product on a person photo.
     *
     * @param personImage  The file containing the person photo
     * @param productImage The file containing the clothing/product photo
     * @param callback     Typed callback with parsed result
     */
    public static void virtualTryOn(@NonNull File personImage,
                                    @NonNull File productImage,
                                    @NonNull FaceSwapCallback callback) {
        Map<String, File> files = new HashMap<>();
        files.put("personImage", personImage);
        files.put("productImage", productImage);

        AppFaceApiClient.getInstance().multipartUpload(
                "/api/Image/virtual-try-on",
                files, new HashMap<>(), IMAGE_PROCESS_TIMEOUT_MS,
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse virtual try-on result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        callback.onError("Virtual try-on failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/text-to-image
    // ══════════════════════════════════════════════════

    /**
     * Generates an image from a text prompt only (no source image needed).
     *
     * @param prompt   Text description of the desired image
     * @param callback Typed callback with parsed result
     */
    public static void textToImage(@NonNull String prompt,
                                   @NonNull FaceSwapCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("prompt", prompt);

            AppFaceApiClient.getInstance().post("/api/Image/text-to-image", body.toString(),
                    new AppFaceApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(@NonNull String responseBody) {
                            try {
                                AppFaceFaceSwapResponse response = AppFaceFaceSwapResponse.fromJson(responseBody);
                                callback.onSuccess(response);
                            } catch (Exception e) {
                                callback.onError("Failed to parse text-to-image result: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onError(int statusCode, @NonNull String errorMessage) {
                            FirebaseManager.getInstance().logEvent("TEXT_TO_IMAGE_FAILED");

                            callback.onError("Text-to-image failed (HTTP " + statusCode + "): " + errorMessage);
                        }
                    });
        } catch (Exception e) {
            callback.onError("Failed to build text-to-image request: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════
    //  History
    // ══════════════════════════════════════════════════

    /**
     * Callback for history API responses.
     */
    public interface HistoryCallback {
        void onSuccess(@NonNull AppFaceHistoryResponse response);
        void onError(@NonNull String errorMessage);
    }

    /**
     * Fetches paginated processing history.
     *
     * @param page     1-based page number
     * @param pageSize Number of items per page (typically 20)
     * @param callback Result callback
     */
    public static AppFaceApiCall fetchHistory(int page, int pageSize,
                                              @NonNull HistoryCallback callback) {
        String url = BASE_URL + "/api/History?page=" + page + "&pageSize=" + pageSize;

        return AppFaceApiClient.getInstance().get(url, new AppFaceApiClient.ApiCallback() {
            @Override
            public void onSuccess(@NonNull String responseBody) {
                try {
                    AppFaceHistoryResponse response =
                            AppFaceHistoryResponse.fromJson(responseBody);
                    callback.onSuccess(response);
                } catch (Exception e) {
                    callback.onError("Failed to parse history: " + e.getMessage());
                }
            }

            @Override
            public void onError(int statusCode, @NonNull String errorMessage) {
                callback.onError("History fetch failed (HTTP " + statusCode + "): " + errorMessage);
            }
        });
    }

    // ══════════════════════════════════════════════════
    //  POST /api/purchases/verify  (silent fire-and-forget) COINS
    // ══════════════════════════════════════════════════

    /**
     * Sends purchase receipt data to the server for verification.
     * This is a silent call — no UI feedback on success or failure.
     * On success, updates the cached credit balance from the server response.
     */
    public interface VerificationCallback {
        void onSuccess();
        void onError(String error);
    }

    public static void verifyPurchase(@NonNull String productId,
                                      @NonNull String receiptData,
                                      @NonNull String transactionId,
                                      @NonNull String orderId,
                                      VerificationCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("platform", "google");
            body.put("productId", productId);
            body.put("receiptData", receiptData);
            body.put("transactionId", transactionId);
            body.put("orderId", orderId);

            android.util.Log.d("ApiRepository", "Verifying purchase: " + productId);

            AppFaceApiClient.getInstance().post("/api/purchases/verify", body.toString(),
                    new AppFaceApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(@NonNull String responseBody) {
                            try {
                                JSONObject response = new JSONObject(responseBody);
                                boolean success = response.optBoolean("success", false);
                                if (success) {
                                    double newBalance = response.optDouble("newCreditBalance", -1);
                                    if (newBalance >= 0) {
                                        AppFaceSessionManager.getInstance().setCachedCredits(newBalance);
                                    }
                                    android.util.Log.d("ApiRepository", "Purchase verified. New balance: " + newBalance);
                                    if (callback != null) callback.onSuccess();
                                } else {
                                    String errorMsg = response.optString("message", "Verification failed");
                                    android.util.Log.w("ApiRepository", "Purchase verification failed: " + errorMsg);
                                    if (callback != null) callback.onError(errorMsg);
                                }
                            } catch (Exception e) {
                                android.util.Log.e("ApiRepository", "Failed to parse verify response", e);
                                if (callback != null) callback.onError(e.getMessage());
                            }
                        }

                        @Override
                        public void onError(int statusCode, @NonNull String errorMessage) {
                            android.util.Log.e("ApiRepository", "Purchase verify API error (HTTP " + statusCode + "): " + errorMessage);
                            if (callback != null) callback.onError(errorMessage);
                        }
                    });
        } catch (Exception e) {
            android.util.Log.e("ApiRepository", "Failed to build verify request", e);
        }
    }


    // ══════════════════════════════════════════════════
    //  POST /api/purchases/verify  (silent fire-and-forget)
    // ══════════════════════════════════════════════════

    /**
     * Sends purchase receipt data to the server for verification.
     * This is a silent call — no UI feedback on success or failure.
     * On success, updates the cached credit balance from the server response.
     */


    public static void verifyInAppPurchase(@NonNull String productId,
                                      @NonNull String receiptData,
                                      @NonNull String transactionId,
                                      @NonNull String orderId,
                                      VerificationCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("platform", "google");
            body.put("productId", productId);
            body.put("receiptData", receiptData);
            body.put("transactionId", transactionId);
            body.put("orderId", orderId);

            android.util.Log.d("ApiRepository", "Verifying purchase: " + productId);

            AppFaceApiClient.getInstance().post("/api/purchases/log", body.toString(),
                    new AppFaceApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(@NonNull String responseBody) {
                            try {
                                JSONObject response = new JSONObject(responseBody);
                                boolean success = response.optBoolean("success", false);
                                if (success) {
                                    android.util.Log.d("ApiRepository", "Purchase verified.");
                                    if (callback != null) callback.onSuccess();
                                } else {
                                    String errorMsg = response.optString("message", "Verification failed");
                                    android.util.Log.w("ApiRepository", "Purchase verification failed: " + errorMsg);
                                    if (callback != null) callback.onError(errorMsg);
                                }
                            } catch (Exception e) {
                                android.util.Log.e("ApiRepository", "Failed to parse verify response", e);
                                if (callback != null) callback.onError(e.getMessage());
                            }
                        }

                        @Override
                        public void onError(int statusCode, @NonNull String errorMessage) {
                            android.util.Log.e("ApiRepository", "Purchase verify API error (HTTP " + statusCode + "): " + errorMessage);
                            if (callback != null) callback.onError(errorMessage);
                        }
                    });
        } catch (Exception e) {
            android.util.Log.e("ApiRepository", "Failed to build verify request", e);
        }
    }

    // ══════════════════════════════════════════════════
    //  POST /api/Image/video/faceswap
    // ══════════════════════════════════════════════════

    /** Callback for video face swap API. */
    public interface VideoFaceSwapCallback {
        void onSuccess(@NonNull AppFaceVideoFaceSwapResponse response);
        void onError(@NonNull String errorMessage);
    }

    /**
     * Performs a video face swap. Sends a video file and a face image
     * as multipart/form-data.
     *
     * @param videoFile The video file to process
     * @param faceImage The face image file to swap into the video
     * @param callback  Typed callback with parsed VideoFaceSwapResponse
     */
    public static void videoFaceSwap(@NonNull File videoFile,
                                     @NonNull File faceImage,
                                     @NonNull VideoFaceSwapCallback callback) {
        Map<String, File> files = new HashMap<>();
        files.put("video", videoFile);
        files.put("faceImage", faceImage);

        AppFaceApiClient.getInstance().multipartUpload(
                "/api/Image/video/faceswap",
                files,
                new HashMap<>(),
                IMAGE_PROCESS_TIMEOUT_MS,
                new AppFaceApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(@NonNull String responseBody) {
                        try {
                            AppFaceVideoFaceSwapResponse response =
                                    AppFaceVideoFaceSwapResponse.fromJson(responseBody);
                            callback.onSuccess(response);
                        } catch (Exception e) {
                            callback.onError("Failed to parse video face swap result: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int statusCode, @NonNull String errorMessage) {
                        FirebaseManager.getInstance().logEvent("VIDEO_FACESWAP_FAILED");
                        callback.onError("Video face swap failed (HTTP " + statusCode + "): " + errorMessage);
                    }
                });
    }
}

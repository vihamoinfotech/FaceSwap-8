package com.facechanger.faceswap.enhance.model.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Response model for {@code POST /api/Image/faceswap/basic} and other
 * image-processing endpoints that return a {@code ProcessingResult}.
 * <p>
 * Contains the result image (as URL or Base64), processing metrics,
 * remaining usage limits, and error details on failure.
 */
public final class FaceSwapResponse {

    private final boolean success;
    private final String message;
    @Nullable private final String imageUrl;
    @Nullable private final String imageBase64;
    private final double remainingLimit;
    private final int processingTimeMs;
    private final double creditCost;
    @Nullable private final String errorCode;

    private FaceSwapResponse(boolean success, String message,
                             @Nullable String imageUrl, @Nullable String imageBase64,
                             double remainingLimit, int processingTimeMs,
                             double creditCost, @Nullable String errorCode) {
        this.success = success;
        this.message = message;
        this.imageUrl = imageUrl;
        this.imageBase64 = imageBase64;
        this.remainingLimit = remainingLimit;
        this.processingTimeMs = processingTimeMs;
        this.creditCost = creditCost;
        this.errorCode = errorCode;
    }

    // ── Accessors ────────────────────────────────────

    public boolean isSuccess()               { return success; }
    @NonNull public String getMessage()      { return message != null ? message : ""; }
    @Nullable public String getImageUrl()    { return imageUrl; }
    @Nullable public String getImageBase64() { return imageBase64; }
    public double getRemainingLimit()        { return remainingLimit; }
    public int getProcessingTimeMs()         { return processingTimeMs; }
    public double getCreditCost()            { return creditCost; }
    @Nullable public String getErrorCode()   { return errorCode; }

    /**
     * @return true if a result image is available (either as URL or Base64)
     */
    public boolean hasResultImage() {
        return (imageUrl != null && !imageUrl.isEmpty())
                || (imageBase64 != null && !imageBase64.isEmpty());
    }

    // ── Factory ──────────────────────────────────────

    /**
     * Parses a {@code ProcessingResult} JSON response.
     */
    @NonNull
    public static FaceSwapResponse fromJson(@NonNull String json) throws org.json.JSONException {
        JSONObject obj = new JSONObject(json);
        return new FaceSwapResponse(
                obj.optBoolean("success", false),
                obj.optString("message", ""),
                obj.optString("imageUrl", null),
                obj.optString("imageBase64", null),
                obj.optDouble("credits", obj.optDouble("remainingLimit", 0.0)),
                obj.optInt("processingTimeMs", 0),
                obj.optDouble("creditCost", 0.0),
                obj.optString("errorCode", null)
        );
    }
}

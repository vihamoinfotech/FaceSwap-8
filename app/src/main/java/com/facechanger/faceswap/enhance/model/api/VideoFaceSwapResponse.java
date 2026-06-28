package com.facechanger.faceswap.enhance.model.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Response model for {@code POST /api/Image/video/faceswap}.
 * <p>
 * Contains the result video URL, duration info, credit cost details,
 * processing metrics, and error information on failure.
 */
public final class VideoFaceSwapResponse {

    private final boolean success;
    @Nullable private final String message;
    @Nullable private final String videoUrl;
    private final int videoDurationSeconds;
    private final double baseCreditCost;
    private final double creditCost;
    private final int processingTimeMs;
    @Nullable private final String errorCode;

    private VideoFaceSwapResponse(boolean success,
                                   @Nullable String message,
                                   @Nullable String videoUrl,
                                   int videoDurationSeconds,
                                   double baseCreditCost,
                                   double creditCost,
                                   int processingTimeMs,
                                   @Nullable String errorCode) {
        this.success = success;
        this.message = message;
        this.videoUrl = videoUrl;
        this.videoDurationSeconds = videoDurationSeconds;
        this.baseCreditCost = baseCreditCost;
        this.creditCost = creditCost;
        this.processingTimeMs = processingTimeMs;
        this.errorCode = errorCode;
    }

    // ── Accessors ────────────────────────────────────

    public boolean isSuccess()                    { return success; }
    @NonNull public String getMessage()           { return message != null ? message : ""; }
    @Nullable public String getVideoUrl()         { return videoUrl; }
    public int getVideoDurationSeconds()          { return videoDurationSeconds; }
    public double getBaseCreditCost()             { return baseCreditCost; }
    public double getCreditCost()                 { return creditCost; }
    public int getProcessingTimeMs()              { return processingTimeMs; }
    @Nullable public String getErrorCode()        { return errorCode; }

    /**
     * @return true if a result video URL is available
     */
    public boolean hasResultVideo() {
        return videoUrl != null && !videoUrl.isEmpty();
    }

    // ── Factory ──────────────────────────────────────

    /**
     * Parses a video face swap JSON response.
     */
    @NonNull
    public static VideoFaceSwapResponse fromJson(@NonNull String json) throws org.json.JSONException {
        JSONObject obj = new JSONObject(json);
        return new VideoFaceSwapResponse(
                obj.optBoolean("success", false),
                obj.optString("message", null),
                obj.optString("videoUrl", null),
                obj.optInt("videoDurationSeconds", 0),
                obj.optDouble("baseCreditCost", 0.0),
                obj.optDouble("creditCost", 0.0),
                obj.optInt("processingTimeMs", 0),
                obj.optString("errorCode", null)
        );
    }
}

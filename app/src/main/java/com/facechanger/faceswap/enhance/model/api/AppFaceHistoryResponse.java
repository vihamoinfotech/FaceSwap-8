package com.facechanger.faceswap.enhance.model.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Response model for {@code GET /api/History}.
 * <p>
 * Contains paginated history items and metadata (totalCount, page, pageSize, totalPages).
 */
public final class AppFaceHistoryResponse {

    private final List<HistoryItem> items;
    private final int totalCount;
    private final int page;
    private final int pageSize;
    private final int totalPages;

    private AppFaceHistoryResponse(List<HistoryItem> items, int totalCount, int page,
                                   int pageSize, int totalPages) {
        this.items = items;
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

    // ── Accessors ────────────────────────────────────

    @NonNull
    public List<HistoryItem> getItems() { return items; }
    public int getTotalCount()         { return totalCount; }
    public int getPage()               { return page; }
    public int getPageSize()           { return pageSize; }
    public int getTotalPages()         { return totalPages; }

    /**
     * @return true if there are more pages after the current one
     */
    public boolean hasMorePages() {
        return page < totalPages;
    }

    // ── Factory ──────────────────────────────────────

    @NonNull
    public static AppFaceHistoryResponse fromJson(@NonNull String json) throws org.json.JSONException {
        JSONObject obj = new JSONObject(json);

        List<HistoryItem> items = new ArrayList<>();
        JSONArray itemsArray = obj.optJSONArray("items");
        if (itemsArray != null) {
            for (int i = 0; i < itemsArray.length(); i++) {
                items.add(HistoryItem.fromJson(itemsArray.getJSONObject(i)));
            }
        }

        return new AppFaceHistoryResponse(
                items,
                obj.optInt("totalCount", 0),
                obj.optInt("page", 1),
                obj.optInt("pageSize", 20),
                obj.optInt("totalPages", 1)
        );
    }

    // ══════════════════════════════════════════════════
    //  History Item
    // ══════════════════════════════════════════════════

    /**
     * A single history entry for a processed image/operation.
     */
    public static final class HistoryItem {

        private final String id;
        private final String featureType;
        @Nullable private final String resultUrl;
        private final String status;
        private final double creditsConsumed;
        private final int processingTimeMs;
        @Nullable private final String errorMessage;
        private final String createdAt;
        @Nullable private final String urlExpiresAt;
        private final boolean isUrlExpired;
        @Nullable private final String originalFileNames;

        private HistoryItem(String id, String featureType, @Nullable String resultUrl,
                            String status, double creditsConsumed, int processingTimeMs,
                            @Nullable String errorMessage, String createdAt,
                            @Nullable String urlExpiresAt, boolean isUrlExpired,
                            @Nullable String originalFileNames) {
            this.id = id;
            this.featureType = featureType;
            this.resultUrl = resultUrl;
            this.status = status;
            this.creditsConsumed = creditsConsumed;
            this.processingTimeMs = processingTimeMs;
            this.errorMessage = errorMessage;
            this.createdAt = createdAt;
            this.urlExpiresAt = urlExpiresAt;
            this.isUrlExpired = isUrlExpired;
            this.originalFileNames = originalFileNames;
        }

        // ── Accessors ────────────────────────────────

        @NonNull public String getId()              { return id != null ? id : ""; }
        @NonNull public String getFeatureType()     { return featureType != null ? featureType : ""; }
        @Nullable public String getResultUrl()      { return resultUrl; }
        @NonNull public String getStatus()          { return status != null ? status : ""; }
        public double getCreditsConsumed()          { return creditsConsumed; }
        public int getProcessingTimeMs()            { return processingTimeMs; }
        @Nullable public String getErrorMessage()   { return errorMessage; }
        @NonNull public String getCreatedAt()       { return createdAt != null ? createdAt : ""; }
        @Nullable public String getUrlExpiresAt()   { return urlExpiresAt; }
        public boolean isUrlExpired()               { return isUrlExpired; }
        @Nullable public String getOriginalFileNames() { return originalFileNames; }

        /**
         * @return true if the operation completed successfully
         */
        public boolean isSuccess() {
            return "Completed".equalsIgnoreCase(status) || "Success".equalsIgnoreCase(status);
        }

        /**
         * @return true if a valid, non-expired result URL is available
         */
        public boolean hasValidUrl() {
            return resultUrl != null && !resultUrl.isEmpty() && !isUrlExpired;
        }

        /**
         * @return A human-readable display name for the feature type.
         */
        @NonNull
        public String getFeatureDisplayName() {
            if (featureType == null) return "Unknown";
            switch (featureType) {
                case "FaceSwapBasic":    return "Face Swap";
                case "FaceSwapMulti":    return "Multi Swap";
                case "RemoveBackground": return "Remove BG";
                case "ImageUpscalePro":  return "Enhance";
                case "ImageEdit":        return "AI Image";
                default:                 return featureType;
            }
        }

        // ── Factory ──────────────────────────────────

        @NonNull
        static HistoryItem fromJson(@NonNull JSONObject obj) {
            return new HistoryItem(
                    obj.optString("id", ""),
                    obj.optString("featureType", ""),
                    obj.optString("resultUrl", null),
                    obj.optString("status", ""),
                    obj.optDouble("creditsConsumed", 0.0),
                    obj.optInt("processingTimeMs", 0),
                    obj.optString("errorMessage", null),
                    obj.optString("createdAt", ""),
                    obj.optString("urlExpiresAt", null),
                    obj.optBoolean("isUrlExpired", false),
                    obj.optString("originalFileNames", null)
            );
        }
    }
}

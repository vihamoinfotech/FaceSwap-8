package com.facechanger.faceswap.enhance.model.api;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups {@link TemplateItem}s by their category for section-based
 * display on the home screen. Each instance represents one section
 * (header + horizontal image row).
 */
public final class TemplateCategory {

    private final int categoryId;
    private final String categoryName;
    private final List<TemplateItem> templates;

    public TemplateCategory(int categoryId, @NonNull String categoryName,
                            @NonNull List<TemplateItem> templates) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.templates = templates;
    }

    // ── Accessors ────────────────────────────────────

    public int getCategoryId()                      { return categoryId; }
    @NonNull public String getCategoryName()        { return categoryName; }
    @NonNull public List<TemplateItem> getTemplates() { return templates; }

    // ── Parsing utility ─────────────────────────────

    /**
     * Parses the API response array of sections into Categories.
     * Each JSON object corresponds to a category containing 'titletext' and an 'items' array.
     */
    @NonNull
    public static List<TemplateCategory> fromApiResponse(@NonNull String json) throws org.json.JSONException {
        json = json.trim();
        List<TemplateCategory> categories = new ArrayList<>();

        // Depending on whether it's wrapped in an object or an array directly at root
        org.json.JSONArray rootArray = null;

        if (json.startsWith("[")) {
            rootArray = new org.json.JSONArray(json);
        } else if (json.startsWith("{")) {
            org.json.JSONObject rootObj = new org.json.JSONObject(json);
            if (rootObj.has("message")) {
                Object msgValue = rootObj.get("message");
                if (msgValue instanceof org.json.JSONArray) {
                    rootArray = (org.json.JSONArray) msgValue;
                } else {
                    String msgStr = msgValue.toString().trim();
                    if (msgStr.startsWith("[")) {
                        rootArray = new org.json.JSONArray(msgStr);
                    }
                }
            } else if (rootObj.has("data")) {
                Object dataValue = rootObj.get("data");
                if (dataValue instanceof org.json.JSONArray) {
                    rootArray = (org.json.JSONArray) dataValue;
                }
            }
        }

        if (rootArray == null) {
            return categories; // empty
        }

        for (int i = 0; i < rootArray.length(); i++) {
            org.json.JSONObject sectionObj = rootArray.optJSONObject(i);
            if (sectionObj != null) {
                String title = sectionObj.optString("title", "Category");
                int id = sectionObj.optInt("categoryId", i); // Fallback to index if needed

                org.json.JSONArray itemsArray = sectionObj.optJSONArray("items");
                List<TemplateItem> items = new ArrayList<>();
                if (itemsArray != null) {
                    items = TemplateItem.fromJsonArray(itemsArray);
                }

                // If a section has no items, we can optionally skip it, but let's include it
                if (!items.isEmpty()) {
                    categories.add(new TemplateCategory(id, title, items));
                }
            }
        }


        return categories;
    }
}

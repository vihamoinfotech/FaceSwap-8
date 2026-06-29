package com.facechanger.faceswap.enhance.model.api;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single face-swap template fetched from {@code POST /api/templates/list}.
 * <p>
 * Each template belongs to a category and has an associated demo image
 * served by {@code GET /api/templates/{id}/image}.
 */
public final class AppFaceTemplateItem implements Serializable {

    private final int id;
    private final String templateName;
    private final String prompt;
    private final int categoryId;
    private final String categoryName;
    private final String noteMessage;
    private final boolean isActive;

    public AppFaceTemplateItem(int id, String templateName, String prompt,
                               int categoryId, String categoryName,
                               String noteMessage, boolean isActive) {
        this.id = id;
        this.templateName = templateName;
        this.prompt = prompt;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.noteMessage = noteMessage;
        this.isActive = isActive;
    }

    // ── Accessors ────────────────────────────────────

    public int getId()                  { return id; }

    @NonNull public String getName()    { return templateName != null ? templateName : ""; }
    @NonNull public String getPrompt()  { return prompt != null ? prompt : ""; }
    public int getCategoryId()          { return categoryId; }
    @NonNull public String getCategoryName() { return categoryName != null ? categoryName : ""; }
    @NonNull public String getNoteMessage()  { return noteMessage != null ? noteMessage : ""; }
    public boolean isActive()           { return isActive; }

    /**
     * Constructs the full image URL for this template.
     *
     * @param baseUrl API base URL (e.g. {@code https://acc-faceswapstudioai.runasp.net})
     * @return Absolute URL to the template's demo image
     */
    @NonNull
    public String getImageUrl(@NonNull String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/api/templates/" + id + "/image";
    }

    // ── Parsing ──────────────────────────────────────

    /**
     * Parse a single template from a JSON object.
     */
    @NonNull
    public static AppFaceTemplateItem fromJson(@NonNull JSONObject obj) {
        // Fallbacks for keys that might differ in the API
        String catName = obj.has("categoryName") ? obj.optString("categoryName", "") : obj.optString("title", "");
        String tplName = obj.has("templateName") ? obj.optString("templateName", "") : obj.optString("name", "");
        
        return new AppFaceTemplateItem(
                obj.optInt("id", 0),
                tplName,
                obj.optString("prompt", ""),
                obj.optInt("categoryId", 0),
                catName,
                obj.optString("noteMessage", ""),
                obj.optBoolean("isActive", true)
        );
    }

    /**
     * Parse a list of templates from a JSON array.
     */
    @NonNull
    public static List<AppFaceTemplateItem> fromJsonArray(@NonNull JSONArray array) {
        List<AppFaceTemplateItem> items = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null) {
                items.add(fromJson(obj));
            }
        }
        return items;
    }

    /**
     * Parse the full API response.
     * <p>
     * Handles two formats:
     * <ul>
     *   <li>{@code { "message": [...] }} — templates inside "message" key</li>
     *   <li>{@code [...]} — plain array at root</li>
     * </ul>
     */
    @NonNull
    public static List<AppFaceTemplateItem> fromApiResponse(@NonNull String json) throws org.json.JSONException {
        json = json.trim();

        // Format 1: Root is an array
        if (json.startsWith("[")) {
            return fromJsonArray(new JSONArray(json));
        }

        // Format 2: Wrapped in an object
        JSONObject root = new JSONObject(json);

        // Try "message" key (could be array)
        if (root.has("message")) {
            Object msgValue = root.get("message");
            if (msgValue instanceof JSONArray) {
                return fromJsonArray((JSONArray) msgValue);
            }
            // If message is a JSON string containing an array
            String msgStr = msgValue.toString().trim();
            if (msgStr.startsWith("[")) {
                return fromJsonArray(new JSONArray(msgStr));
            }
        }

        // Try "data" key
        if (root.has("data")) {
            Object dataValue = root.get("data");
            if (dataValue instanceof JSONArray) {
                return fromJsonArray((JSONArray) dataValue);
            }
        }

        // Fallback: empty list
        return new ArrayList<>();
    }
}

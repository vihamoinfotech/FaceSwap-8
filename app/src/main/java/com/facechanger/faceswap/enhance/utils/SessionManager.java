package com.facechanger.faceswap.enhance.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facechanger.faceswap.enhance.model.api.FaceSwapResponse;
import com.facechanger.faceswap.enhance.model.api.SplashDataResponse;
import com.facechanger.faceswap.enhance.view.LottieLoadingActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory session holder for the current app session.
 * <p>
 * Initialized once from the splash screen with the API response data.
 * Holds the auth token, user info, app configuration, and feature costs
 * so they are accessible from any screen without re-fetching.
 * <p>
 * <b>Note:</b> Data lives only in memory — it is NOT persisted to disk
 * (SharedPreferences, database, etc.). A fresh session is created on
 * every app launch via the splash data API.
 * <p>
 * Usage:
 * <pre>
 *   // On splash screen after API success:
 *   SessionManager.getInstance().initialize(responseData);
 *
 *   // From any screen:
 *   String token = SessionManager.getInstance().getToken();
 *   boolean isPremium = SessionManager.getInstance().isPremium();
 *   double credits = SessionManager.getInstance().getCurrentCredits();
 * </pre>
 */
public final class SessionManager {

    private static volatile SessionManager instance;

    @Nullable
    private SplashDataResponse.Data sessionData;

    /** Locally cached credit balance (synced with server responses). */
    private double cachedCredits = 0.0;

    /** Locally overridden premium state (e.g. set after successful IAP purchase). */
    @Nullable
    private Boolean localPremiumOverride = null;

    /** Maps LottieLoadingActivity ACTION_* constants to featureCost keys. */
    private static final Map<String, String> ACTION_TO_FEATURE_KEY = new HashMap<>();

    static {
        // AI Image / Generation
        registerActionKeys(LottieLoadingActivity.ACTION_AI_IMAGE, "edit_image", "ai_image", "aiimage");
        registerActionKeys(LottieLoadingActivity.ACTION_IMAGE_GENERATE, "image/generate", "image_generate", "imagegen");
        registerActionKeys(LottieLoadingActivity.ACTION_TEXT_TO_IMAGE, "image/generate", "text_to_image", "texttoimage");

        // Face Swap
        registerActionKeys(LottieLoadingActivity.ACTION_FACE_SWAP, "faceswap/basic", "face_swap", "faceswap");
        registerActionKeys(LottieLoadingActivity.ACTION_MULTI_FACE_SWAP, "faceswap/multi", "multi_face_swap", "multifaceswap");
        registerActionKeys(LottieLoadingActivity.ACTION_COUPLE_SWAP, "faceswap/couple", "couple_swap", "coupleswap");
        registerActionKeys(LottieLoadingActivity.ACTION_VIDEO_FACE_SWAP, "video/faceswap", "video_face_swap", "videofaceswap");

        // Editing / Processing
        registerActionKeys(LottieLoadingActivity.ACTION_REMOVE_BG, "background/remove", "remove_bg", "removebg");
        registerActionKeys(LottieLoadingActivity.ACTION_BG_REPLACE, "background/replace", "bg_replace", "bgreplace");
        registerActionKeys(LottieLoadingActivity.ACTION_UPSCALE, "upscale/pro", "upscale", "image_upscale");
        registerActionKeys(LottieLoadingActivity.ACTION_ENHANCE_GFPGAN, "enhance", "face_enhance", "face_restoration");
        registerActionKeys(LottieLoadingActivity.ACTION_ENHANCE_FACE, "enhance", "face_enhance", "face_restoration");

        // Misc Features
        registerActionKeys(LottieLoadingActivity.ACTION_HAIR_COLOR, "hair/color", "hair_color", "haircolor");
        registerActionKeys(LottieLoadingActivity.ACTION_HAIR_STYLE, "hair/style", "hair_style", "hairstyle");
        registerActionKeys(LottieLoadingActivity.ACTION_VIRTUAL_TRY_ON, "virtual-try-on", "virtual_try_on", "try_on");
    }

    private static void registerActionKeys(String action, String... keys) {
        for (String key : keys) {
            ACTION_TO_FEATURE_KEY.put(action + "_" + key, key);
        }
        // Also map the first key as the "primary" key for that action
        ACTION_TO_FEATURE_KEY.put(action, keys[0]);
    }

    private SessionManager() { }

    /** Thread-safe singleton accessor. */
    @NonNull
    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    // ══════════════════════════════════════════════════
    //  Initialization
    // ══════════════════════════════════════════════════

    /**
     * Stores the session data and configures the API client's auth token.
     * Call this once from the splash screen after a successful API response.
     *
     * @param data The parsed {@link SplashDataResponse.Data} from the splash API
     */
    public void initialize(@NonNull SplashDataResponse.Data data) {
        this.sessionData = data;
        this.cachedCredits = data.getRemainingLimit();

        // Auto-configure the global API client with the auth token
        ApiClient.getInstance().setAuthToken(data.getToken());

        // Apply server-driven coin system flags
        AppSystem.setFeatureEnabled(AppSystem.KEY_COIN_SYSTEM_ENABLED, data.isCoinSystemEnabled());
        AppSystem.setFeatureEnabled(AppSystem.KEY_SKIP_COIN_CHECK_IF_ENOUGH, data.isBypassCoinCheckWhenSufficient());

        // Apply server-driven version flag
        AppSystem.setFeatureEnabled(AppSystem.KEY_IS_LATEST, data.isLatest());
        AppSystem.setFeatureEnabled(AppSystem.KEY_SHOW_COINS_DIRECT_PURCHASE, data.show_coins_direct_purchase());
    }

    /**
     * @return true if the session has been initialized with valid data
     */
    public boolean isInitialized() {
        return sessionData != null && !sessionData.getToken().isEmpty();
    }

    /**
     * Clears the session (e.g. on logout or session expiry).
     */
    public void clear() {
        sessionData = null;
        cachedCredits = 0.0;
        ApiClient.getInstance().setAuthToken(null);
    }

    // ══════════════════════════════════════════════════
    //  Accessors — safe to call from any thread / screen
    // ══════════════════════════════════════════════════

    /**
     * @return The Bearer auth token, or empty string if not initialized
     */
    @NonNull
    public String getToken() {
        return sessionData != null ? sessionData.getToken() : "";
    }

    /**
     * @return The user's UUID, or empty string if not initialized
     */
    @NonNull
    public String getUserId() {
        return sessionData != null ? sessionData.getUserId() : "";
    }

    /**
     * @return The device ID echoed back from the server
     */
    @NonNull
    public String getDeviceId() {
        return sessionData != null ? sessionData.getDeviceId() : "";
    }

    /**
     * @return The user's subscription plan type (e.g. "free", "premium")
     */
    @NonNull
    public String getPlanType() {
        return sessionData != null ? sessionData.getPlanType() : "free";
    }

    /**
     * @return true if the user has an active premium subscription
     */
    public boolean isPremium() {
        if (localPremiumOverride != null) {
            return localPremiumOverride;
        }
        return sessionData != null && sessionData.isPremium();
    }

    /**
     * Sets the local premium override state (useful after a verified purchase or restore).
     *
     * @param premium true to enable premium features, false to disable
     */
    public void setPremium(boolean premium) {
        this.localPremiumOverride = premium;
    }

    /**
     * @return The plan end date string, or null if free plan
     */
    @Nullable
    public String getPlanEndDate() {
        return sessionData != null ? sessionData.getPlanEndDate() : null;
    }

    /**
     * @return The app configuration, or null if not initialized
     */
    @Nullable
    public SplashDataResponse.AppConfiguration getConfig() {
        return sessionData != null ? sessionData.getConfiguration() : null;
    }

    /**
     * @return Feature costs map (feature name → credit cost), or empty map
     */
    @NonNull
    public Map<String, Double> getFeatureCosts() {
        if (sessionData != null) return sessionData.getFeatureCosts();
        return java.util.Collections.emptyMap();
    }

    /**
     * @return The full session data object, or null if not initialized
     */
    @Nullable
    public SplashDataResponse.Data getSessionData() {
        return sessionData;
    }

    @NonNull
    public SplashDataResponse.VideoFaceSwapConfig getVideoFaceSwapConfig() {
        if (sessionData != null && sessionData.getVideoFaceSwapConfig() != null) {
            return sessionData.getVideoFaceSwapConfig();
        }
        return SplashDataResponse.VideoFaceSwapConfig.fromJson(null);
    }

    // ══════════════════════════════════════════════════
    //  Credit / Coin Balance Management
    // ══════════════════════════════════════════════════

    /**
     * @return The current cached credit balance (server-synced)
     */
    public double getCurrentCredits() {
        return cachedCredits;
    }

    /**
     * Returns the credit cost for a given feature action.
     *
     * @param action LottieLoadingActivity.ACTION_* constant
     * @return The cost in credits, or 0.0 if not configured
     */
    public double getFeatureCost(@NonNull String action) {
        Map<String, Double> costs = getFeatureCosts();
        if (costs.isEmpty()) return 0.0;

        // 1. Try primary key from mapping
        String primaryKey = ACTION_TO_FEATURE_KEY.get(action);
        if (primaryKey != null && costs.containsKey(primaryKey)) {
            return costs.get(primaryKey);
        }

        // 2. Try all variant keys for this action
        for (Map.Entry<String, String> entry : ACTION_TO_FEATURE_KEY.entrySet()) {
            if (entry.getKey().startsWith(action + "_")) {
                String variantKey = entry.getValue();
                if (costs.containsKey(variantKey)) {
                    return costs.get(variantKey);
                }
            }
        }

        // 3. Fallback: Fuzzy matching (case-insensitive, contains)
        String normalizedAction = action.toLowerCase().replace("action_", "").replace("_", "");
        for (String serverKey : costs.keySet()) {
            String normalizedServerKey = serverKey.toLowerCase().replace("_", "");
            if (normalizedServerKey.contains(normalizedAction) || normalizedAction.contains(normalizedServerKey)) {
                return costs.get(serverKey);
            }
        }

        return 0.0;
    }

    /**
     * Returns the feature cost key name for a given action.
     *
     * @param action LottieLoadingActivity.ACTION_* constant
     * @return The feature cost key (e.g. "face_swap"), or the action itself as fallback
     */
    @NonNull
    public String getFeatureKey(@NonNull String action) {
        String key = ACTION_TO_FEATURE_KEY.get(action);
        return key != null ? key : action;
    }

    /**
     * Checks whether the user has enough credits for a given feature.
     *
     * @param action LottieLoadingActivity.ACTION_* constant
     * @return true if balance >= feature cost
     */
    public boolean hasEnoughCredits(@NonNull String action) {
        return cachedCredits >= getFeatureCost(action);
    }

    /**
     * Updates the cached credit balance from a successful API response.
     * The server returns the remaining limit after deduction.
     *
     * @param response The API response containing updated balance info
     */
    public void updateCreditsFromResponse(@NonNull FaceSwapResponse response) {
        if (response.getRemainingLimit() >= 0) {
            this.cachedCredits = response.getRemainingLimit();
        }
    }

    /**
     * Directly sets the cached credit balance (e.g. after a coin purchase).
     *
     * @param credits The new credit balance
     */
    public void setCachedCredits(double credits) {
        this.cachedCredits = Math.max(0, credits);
    }

    /**
     * Deducts a specified amount from the cached credit balance.
     * Used after a successful feature API call to reflect the cost locally.
     * Credits are only replenished via splash_data or verify_purchase APIs.
     *
     * @param amount The cost to deduct
     */
    public void deductCredits(double amount) {
        if (amount > 0) {
            this.cachedCredits = Math.max(0, this.cachedCredits - amount);
        }
    }
}

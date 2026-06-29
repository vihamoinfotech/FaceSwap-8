package com.facechanger.faceswap.enhance.model.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Response model for {@code POST /api/User/splash_data}.
 * <p>
 * Wraps the top-level API envelope ({@code message}, {@code isSuccess},
 * {@code data})
 * and contains the full session payload including auth token, user info,
 * device geo-data, subscription plan, app configuration, and feature costs.
 * <p>
 * Parse from raw JSON via {@link #fromJson(String)}.
 */
public final class AppFaceSplashDataResponse {

    private final String message;
    private final boolean isSuccess;
    @Nullable
    private final Data data;

    private AppFaceSplashDataResponse(String message, boolean isSuccess, @Nullable Data data) {
        this.message = message;
        this.isSuccess = isSuccess;
        this.data = data;
    }

    // ── Accessors ────────────────────────────────────

    @NonNull
    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    @Nullable
    public Data getData() {
        return data;
    }

    // ── Factory ──────────────────────────────────────

    /**
     * Parses the full API response envelope.
     *
     * @param json Raw JSON string from the API
     * @return Parsed response, never null
     * @throws org.json.JSONException if the JSON is malformed
     */
    @NonNull
    public static AppFaceSplashDataResponse fromJson(@NonNull String json) throws org.json.JSONException {
        JSONObject root = new JSONObject(json);
        String message = root.optString("message", "");
        boolean success = root.optBoolean("isSuccess", false);
        Data data = root.has("data") && !root.isNull("data")
                ? Data.fromJson(root.getJSONObject("data"))
                : null;
        return new AppFaceSplashDataResponse(message, success, data);
    }

    // ══════════════════════════════════════════════════
    // Data — the main session payload
    // ══════════════════════════════════════════════════

    public static final class Data {

        // Auth
        private final String token;
        private final String userId;
        private final String deviceId;

        // Geo / network
        private final String ip;
        private final String country;
        private final String postal;
        private final String city;
        private final String region;
        private final String network;
        private final double latitude;
        private final double longitude;
        private final int app_exp;
        private final int is_in_app_aft_spl;
        private final int is_prm_prc_show;

        // Device
        private final String deviceType;
        private final String createdAt;

        // Subscription
        private final String planType;
        @Nullable
        private final String planEndDate;

        // Configuration & costs
        @Nullable
        private final AppConfiguration configuration;
        @NonNull
        private final Map<String, Double> featureCosts;
        @Nullable
        private final VideoFaceSwapConfig videoFaceSwapConfig;

        // Credit balance
        private final double remainingLimit;

        // Server-driven coin system flags
        private final boolean coinSystemEnabled;
        private final boolean show_coins_direct_purchase;
        private final boolean bypassCoinCheckWhenSufficient;

        // Server-driven version check
        private final boolean isLatest;

        private Data(String token, String userId, String deviceId,
                String ip, String country, String postal, String city,
                String region, String network, double latitude, double longitude, int appExp,
                int isInAppAftSpl, int isPrmPrcShow,
                String deviceType, String createdAt,
                String planType, @Nullable String planEndDate,
                @Nullable AppConfiguration configuration,
                @NonNull Map<String, Double> featureCosts,
                @Nullable VideoFaceSwapConfig videoFaceSwapConfig,
                double remainingLimit,
                boolean coinSystemEnabled,
                boolean bypassCoinCheckWhenSufficient,
                boolean showCoinsDirectPurchase,
                boolean isLatest) {
            this.token = token;
            this.userId = userId;
            this.deviceId = deviceId;
            this.ip = ip;
            this.country = country;
            this.postal = postal;
            this.city = city;
            this.region = region;
            this.network = network;
            this.latitude = latitude;
            this.longitude = longitude;
            this.app_exp = appExp;
            this.is_in_app_aft_spl = isInAppAftSpl;
            this.is_prm_prc_show = isPrmPrcShow;
            this.deviceType = deviceType;
            this.createdAt = createdAt;
            this.planType = planType;
            this.planEndDate = planEndDate;
            this.configuration = configuration;
            this.featureCosts = featureCosts;
            this.videoFaceSwapConfig = videoFaceSwapConfig;
            this.remainingLimit = remainingLimit;
            this.coinSystemEnabled = coinSystemEnabled;
            this.bypassCoinCheckWhenSufficient = bypassCoinCheckWhenSufficient;
            this.show_coins_direct_purchase = showCoinsDirectPurchase;
            this.isLatest = isLatest;
        }

        // ── Accessors ──

        @NonNull
        public String getToken() {
            return token;
        }

        @NonNull
        public String getUserId() {
            return userId;
        }

        @NonNull
        public String getDeviceId() {
            return deviceId;
        }

        @NonNull
        public String getIp() {
            return ip;
        }

        @NonNull
        public String getCountry() {
            return country;
        }

        @NonNull
        public String getPostal() {
            return postal;
        }

        @NonNull
        public String getCity() {
            return city;
        }

        @NonNull
        public String getRegion() {
            return region;
        }

        @NonNull
        public String getNetwork() {
            return network;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @NonNull
        public String getDeviceType() {
            return deviceType;
        }

        @NonNull
        public String getCreatedAt() {
            return createdAt;
        }

        @NonNull
        public String getPlanType() {
            return planType;
        }

        @Nullable
        public String getPlanEndDate() {
            return planEndDate;
        }

        @Nullable
        public AppConfiguration getConfiguration() {
            return configuration;
        }

        @NonNull
        public Map<String, Double> getFeatureCosts() {
            return featureCosts;
        }

        @Nullable
        public VideoFaceSwapConfig getVideoFaceSwapConfig() {
            return videoFaceSwapConfig;
        }

        public double getRemainingLimit() {
            return remainingLimit;
        }

        public boolean isCoinSystemEnabled() {
            return coinSystemEnabled;
        }

        public boolean isBypassCoinCheckWhenSufficient() {
            return bypassCoinCheckWhenSufficient;
        }

        public boolean isLatest() {
            return isLatest;
        }

        public boolean show_coins_direct_purchase() {
            return show_coins_direct_purchase;
        }

        /** @return true if the user has an active premium plan */
        public boolean isPremium() {
            return planType != null && !planType.isEmpty()
                    && !"free".equalsIgnoreCase(planType);
        }

        // ── Factory ──

        @NonNull
        static Data fromJson(@NonNull JSONObject obj) throws org.json.JSONException {
            // Parse feature costs map
            Map<String, Double> costs = new HashMap<>();
            JSONObject costsObj = obj.optJSONObject("featureCosts");
            if (costsObj != null) {
                Iterator<String> keys = costsObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    costs.put(key, costsObj.optDouble(key, 0.0));
                }
            }

            // Parse configuration
            AppConfiguration config = null;
            JSONObject configObj = obj.optJSONObject("configuration");
            if (configObj != null) {
                config = AppConfiguration.fromJson(configObj);
            }

            // Parse video_face_swap config
            VideoFaceSwapConfig videoConfig = null;
            JSONObject videoConfigObj = obj.optJSONObject("video_face_swap");
            if (videoConfigObj != null) {
                videoConfig = VideoFaceSwapConfig.fromJson(videoConfigObj);
            } else {
                videoConfig = VideoFaceSwapConfig.fromJson(null);
            }

            return new Data(
                    obj.optString("token", ""),
                    obj.optString("userId", ""),
                    obj.optString("deviceId", ""),
                    obj.optString("ip", ""),
                    obj.optString("country", ""),
                    obj.optString("postal", ""),
                    obj.optString("city", ""),
                    obj.optString("region", ""),
                    obj.optString("network", ""),
                    obj.optDouble("latitude", 0.0),
                    obj.optDouble("longitude", 0.0),
                    obj.optInt("app_exp", 1),
                    obj.optInt("is_in_app_aft_spl", 1),
                    obj.optInt("is_prm_prc_show", 1),
                    obj.optString("deviceType", ""),
                    obj.optString("createdAt", ""),
                    obj.optString("planType", "free"),
                    obj.optString("planEndDate", null),
                    config,
                    costs,
                    videoConfig,
                    obj.optDouble("credits", 0.0),
                    obj.optBoolean("COIN_SYSTEM_ENABLED", true),
                    obj.optBoolean("BYPASS_COIN_CHECK_WHEN_SUFFICIENT", false),
                    obj.optBoolean("show_coins_direct_purchase", false),
                    obj.optBoolean("is_latest", obj.optBoolean("is_Latest", true)));
        }

        public int getApp_exp() {
            return app_exp;
        }

        public int getIs_in_app_aft_spl() {
            return is_in_app_aft_spl;
        }

        public int getIs_prm_prc_show() {
            return is_prm_prc_show;
        }
    }

    // ══════════════════════════════════════════════════
    // AppConfiguration — ads, screens, AI token
    // ══════════════════════════════════════════════════

    public static final class AppConfiguration {

        // Primary ad unit IDs
        private final String appOpenAdsId;
        private final String bannerAdsId;
        private final String interstitialAdsId;
        private final String nativeAdsId;
        private final String rewardsAdsId;

        // Ad unit ID arrays (waterfall / mediation)
        private final List<String> bannerAdsIds;
        private final List<String> interstitialAdsIds;
        private final List<String> rewardsAdsIds;
        private final List<String> nativeAdsIds;
        private final List<String> appOpenAdsIds;

        // Ad settings
        private final int adsCount;
        private final boolean adsEnable;

        // AI
        private final String aiToken;

        // Feature flags / screen configs
        private final int isAppCheck;
        private final int isSecondaryPremiumEnable;
        private final int appUpdateScreen;
        private final int homeScreen;
        private final int onBoardingScreen;
        private final int settingsScreen;
        private final int exitPopUp;
        private final int topOnBoardingScreen;

        private AppConfiguration(String appOpenAdsId, String bannerAdsId,
                String interstitialAdsId, String nativeAdsId,
                String rewardsAdsId,
                List<String> bannerAdsIds, List<String> interstitialAdsIds,
                List<String> rewardsAdsIds, List<String> nativeAdsIds,
                List<String> appOpenAdsIds,
                int adsCount, boolean adsEnable,
                String aiToken,
                int isAppCheck, int isSecondaryPremiumEnable,
                int appUpdateScreen, int homeScreen,
                int onBoardingScreen, int settingsScreen,
                int exitPopUp, int topOnBoardingScreen) {
            this.appOpenAdsId = appOpenAdsId;
            this.bannerAdsId = bannerAdsId;
            this.interstitialAdsId = interstitialAdsId;
            this.nativeAdsId = nativeAdsId;
            this.rewardsAdsId = rewardsAdsId;
            this.bannerAdsIds = bannerAdsIds;
            this.interstitialAdsIds = interstitialAdsIds;
            this.rewardsAdsIds = rewardsAdsIds;
            this.nativeAdsIds = nativeAdsIds;
            this.appOpenAdsIds = appOpenAdsIds;
            this.adsCount = adsCount;
            this.adsEnable = adsEnable;
            this.aiToken = aiToken;
            this.isAppCheck = isAppCheck;
            this.isSecondaryPremiumEnable = isSecondaryPremiumEnable;
            this.appUpdateScreen = appUpdateScreen;
            this.homeScreen = homeScreen;
            this.onBoardingScreen = onBoardingScreen;
            this.settingsScreen = settingsScreen;
            this.exitPopUp = exitPopUp;
            this.topOnBoardingScreen = topOnBoardingScreen;
        }

        // ── Accessors ──

        @NonNull
        public String getAppOpenAdsId() {
            return appOpenAdsId;
        }

        @NonNull
        public String getBannerAdsId() {
            return bannerAdsId;
        }

        @NonNull
        public String getInterstitialAdsId() {
            return interstitialAdsId;
        }

        @NonNull
        public String getNativeAdsId() {
            return nativeAdsId;
        }

        @NonNull
        public String getRewardsAdsId() {
            return rewardsAdsId;
        }

        @NonNull
        public List<String> getBannerAdsIds() {
            return bannerAdsIds;
        }

        @NonNull
        public List<String> getInterstitialAdsIds() {
            return interstitialAdsIds;
        }

        @NonNull
        public List<String> getRewardsAdsIds() {
            return rewardsAdsIds;
        }

        @NonNull
        public List<String> getNativeAdsIds() {
            return nativeAdsIds;
        }

        @NonNull
        public List<String> getAppOpenAdsIds() {
            return appOpenAdsIds;
        }

        public int getAdsCount() {
            return adsCount;
        }

        public boolean isAdsEnable() {
            return adsEnable;
        }

        @NonNull
        public String getAiToken() {
            return aiToken;
        }

        public int getIsAppCheck() {
            return isAppCheck;
        }

        public int getIsSecondaryPremiumEnable() {
            return isSecondaryPremiumEnable;
        }

        public int getAppUpdateScreen() {
            return appUpdateScreen;
        }

        public int getHomeScreen() {
            return homeScreen;
        }

        public int getOnBoardingScreen() {
            return onBoardingScreen;
        }

        public int getSettingsScreen() {
            return settingsScreen;
        }

        public int getExitPopUp() {
            return exitPopUp;
        }

        public int getTopOnBoardingScreen() {
            return topOnBoardingScreen;
        }

        // ── Factory ──

        @NonNull
        static AppConfiguration fromJson(@NonNull JSONObject obj) {
            return new AppConfiguration(
                    obj.optString("ads_appOpenAdsId", ""),
                    obj.optString("ads_bannerAdsId", ""),
                    obj.optString("ads_interstitialAdsId", ""),
                    obj.optString("ads_nativeAdsId", ""),
                    obj.optString("ads_rewardsAdsId", ""),
                    parseStringArray(obj.optJSONArray("ads_bannerAdsIds")),
                    parseStringArray(obj.optJSONArray("ads_interstitialAdsIds")),
                    parseStringArray(obj.optJSONArray("ads_rewardsAdsIds")),
                    parseStringArray(obj.optJSONArray("ads_nativeAdsIds")),
                    parseStringArray(obj.optJSONArray("ads_appOpenAdsIds")),
                    obj.optInt("ads_count", 0),
                    obj.optBoolean("ads_enable", false),
                    obj.optString("ai_token", ""),
                    obj.optInt("is_app_check", 0),
                    obj.optInt("is_scnd_prm_enable", 0),
                    obj.optInt("app_update_screen", 0),
                    obj.optInt("home_screen", 0),
                    obj.optInt("on_boarding_screen", 0),
                    obj.optInt("settings_screen", 0),
                    obj.optInt("exit_pop_up", 0),
                    obj.optInt("top_on_boarding_screen", 0));
        }

        private static List<String> parseStringArray(@Nullable JSONArray arr) {
            List<String> list = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    String val = arr.optString(i, "");
                    if (!val.isEmpty())
                        list.add(val);
                }
            }
            return list;
        }
    }

    public static final class VideoFaceSwapConfig {
        private final int minDurationSeconds;
        private final int maxDurationSeconds;
        private final int maxUploadSizeMb;

        private VideoFaceSwapConfig(int minDurationSeconds, int maxDurationSeconds, int maxUploadSizeMb) {
            this.minDurationSeconds = minDurationSeconds;
            this.maxDurationSeconds = maxDurationSeconds;
            this.maxUploadSizeMb = maxUploadSizeMb;
        }

        public int getMinDurationSeconds() {
            return minDurationSeconds;
        }

        public int getMaxDurationSeconds() {
            return maxDurationSeconds;
        }

        public int getMaxUploadSizeMb() {
            return maxUploadSizeMb;
        }

        @NonNull
        public static VideoFaceSwapConfig fromJson(@Nullable JSONObject obj) {
            if (obj == null) {
                return new VideoFaceSwapConfig(5, 30, 50);
            }
            return new VideoFaceSwapConfig(
                    obj.optInt("min_duration_seconds", 5),
                    obj.optInt("max_duration_seconds", 30),
                    obj.optInt("max_upload_size_mb", 50));
        }
    }
}

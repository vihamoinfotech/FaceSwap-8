package com.facechanger.faceswap.enhance.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facechanger.faceswap.enhance.BuildConfig;
import com.facechanger.faceswap.enhance.FaceSwap;

import java.util.HashMap;
import java.util.Map;

/**
 * Central system configuration for app-wide behaviors.
 * <p>
 * Provides clean static toggles for:
 * <ul>
 *   <li>{@link #keepScreenOn(boolean)} — Prevent screen timeout globally</li>
 *   <li>{@link #isDebugMode()} — Check if running in debug build</li>
 *   <li>{@link #showDebugToast(Context, String)} — Show toast only in debug builds</li>
 * </ul>
 * <p>
 * Feature flags control optional behavior:
 * <ul>
 *   <li>{@link #USE_LOTTIE_LOADER} — Route Remove BG / Enhance through LottieLoadingActivity</li>
 *   <li>{@link #USE_SERVER_BUSY_DIALOG} — Show premium "High Demand" dialog on API failures</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>
 *   // In Application.onCreate():
 *   AppSystem.keepScreenOn(true);
 *
 *   // Anywhere:
 *   if (AppSystem.isDebugMode()) { ... }
 *   AppSystem.showDebugToast(context, "Debug info");
 *
 *   // Toggle at any time:
 *   AppSystem.keepScreenOn(false);
 * </pre>
 */
public final class AppSystem {

    // ══════════════════════════════════════════════════
    //  Feature Flags
    // ══════════════════════════════════════════════════

    /**
     * When {@code true}, Remove Background and Enhance Image in EditImageActivity
     * route through {@code LottieLoadingActivity} (which shows ads).
     * When {@code false}, uses the current inline loader flow.
     */
    public static final boolean USE_LOTTIE_LOADER = true;

    /**
     * When {@code true}, API failures show a premium "High Demand" dialog
     * with Retry / Go Back options.
     * When {@code false}, falls back to simple retry dialog.
     */
    public static final boolean USE_SERVER_BUSY_DIALOG = true;

    // ══════════════════════════════════════════════════
    //  Coin System Feature Flags
    // ══════════════════════════════════════════════════

    /** Feature flag keys for the coin system */
    public static final String KEY_COIN_SYSTEM_ENABLED       = "coin_system_enabled";

    /** Server-driven flag indicating the app is on the latest version. */
    public static final String KEY_IS_LATEST                 = "is_latest";
    public static final String KEY_SHOW_COINS_DIRECT_PURCHASE = "show_coins_direct_purchase";
    public static final String KEY_SHOW_COIN_BALANCE_HEADER  = "show_coin_balance_header";
    public static final String KEY_SHOW_WATCH_AD_OPTION      = "show_watch_ad_option";
    public static final String KEY_SHOW_BUY_COINS_OPTION     = "show_buy_coins_option";
    public static final String KEY_SKIP_COIN_CHECK_IF_ENOUGH = "skip_coin_check_if_enough";
    public static final String KEY_REQUIRE_COINS_AI_IMAGE    = "require_coins_ai_image";
    public static final String KEY_REQUIRE_COINS_FACE_SWAP   = "require_coins_face_swap";
    public static final String KEY_REQUIRE_COINS_MULTI_SWAP  = "require_coins_multi_swap";
    public static final String KEY_REQUIRE_COINS_REMOVE_BG   = "require_coins_remove_bg";
    public static final String KEY_REQUIRE_COINS_UPSCALE     = "require_coins_upscale";
    public static final String KEY_REQUIRE_COINS_COUPLE_SWAP = "require_coins_couple_swap";
    public static final String KEY_REQUIRE_COINS_BG_REPLACE  = "require_coins_bg_replace";
    public static final String KEY_REQUIRE_COINS_ENHANCE     = "require_coins_enhance";
    public static final String KEY_REQUIRE_COINS_TEXT_TO_IMG  = "require_coins_text_to_image";
    public static final String KEY_REQUIRE_COINS_HAIR_COLOR  = "require_coins_hair_color";
    public static final String KEY_REQUIRE_COINS_HAIR_STYLE  = "require_coins_hair_style";
    public static final String KEY_REQUIRE_COINS_VIRTUAL_TRY = "require_coins_virtual_try_on";
    public static final String KEY_REQUIRE_COINS_IMAGE_GENERATE = "require_coins_image_generate";
    public static final String KEY_REQUIRE_COINS_VIDEO_FACE_SWAP = "require_coins_video_faceswap";

    private static final Map<String, Boolean> featureFlags = new HashMap<>();

    static {
        featureFlags.put(KEY_COIN_SYSTEM_ENABLED, true);
        featureFlags.put(KEY_IS_LATEST, true);
        featureFlags.put(KEY_SHOW_COIN_BALANCE_HEADER, true);
        featureFlags.put(KEY_SHOW_WATCH_AD_OPTION, true);
        featureFlags.put(KEY_SHOW_BUY_COINS_OPTION, true);
        featureFlags.put(KEY_SKIP_COIN_CHECK_IF_ENOUGH, false);
        featureFlags.put(KEY_REQUIRE_COINS_AI_IMAGE, true);
        featureFlags.put(KEY_REQUIRE_COINS_FACE_SWAP, true);
        featureFlags.put(KEY_REQUIRE_COINS_MULTI_SWAP, true);
        featureFlags.put(KEY_REQUIRE_COINS_REMOVE_BG, true);
        featureFlags.put(KEY_REQUIRE_COINS_UPSCALE, true);
        featureFlags.put(KEY_REQUIRE_COINS_COUPLE_SWAP, true);
        featureFlags.put(KEY_REQUIRE_COINS_BG_REPLACE, true);
        featureFlags.put(KEY_REQUIRE_COINS_ENHANCE, true);
        featureFlags.put(KEY_REQUIRE_COINS_TEXT_TO_IMG, true);
        featureFlags.put(KEY_REQUIRE_COINS_HAIR_COLOR, true);
        featureFlags.put(KEY_REQUIRE_COINS_HAIR_STYLE, true);
        featureFlags.put(KEY_REQUIRE_COINS_VIRTUAL_TRY, true);
        featureFlags.put(KEY_REQUIRE_COINS_IMAGE_GENERATE, true);
        featureFlags.put(KEY_REQUIRE_COINS_VIDEO_FACE_SWAP, true);
        featureFlags.put(KEY_SHOW_COINS_DIRECT_PURCHASE, false);
    }

    /**
     * Checks whether a feature flag is enabled.
     *
     * @param key Feature flag key (use KEY_* constants)
     * @return {@code true} if enabled, {@code false} if disabled or key not found
     */
    public static boolean isFeatureEnabled(@NonNull String key) {
        Boolean value = featureFlags.get(key);
        return value != null && value;
    }

    /**
     * Sets a feature flag at runtime (e.g. from server config).
     *
     * @param key     Feature flag key
     * @param enabled Whether the feature should be enabled
     */
    public static void setFeatureEnabled(@NonNull String key, boolean enabled) {
        featureFlags.put(key, enabled);
    }
    
    /**
     * Checks if the app is currently running in Right-To-Left (RTL) mode based on the saved language.
     *
     * @param context Context used to retrieve saved preferences.
     * @return {@code true} if RTL language is active, {@code false} otherwise.
     */
    public static boolean isRTLMode(@NonNull Context context) {
        String lang = com.facechanger.faceswap.enhance.utils.LocaleHelper.getSavedLanguage(context);
        return "ar".equals(lang) || "fa".equals(lang) || "ur".equals(lang) || "iw".equals(lang) || "he".equals(lang);
    }

    // ══════════════════════════════════════════════════
    //  Screen Power Management
    // ══════════════════════════════════════════════════

    private static boolean keepScreenEnabled = false;
    @Nullable
    private static Application.ActivityLifecycleCallbacks screenCallback;

    private AppSystem() {
        // Utility class — no instances
    }

    /**
     * Globally enables or disables FLAG_KEEP_SCREEN_ON for all activities.
     * <p>
     * When enabled, the flag is added to every activity's window on creation.
     * When disabled, the callback is unregistered (existing activities keep their
     * current state until recreated).
     * <p>
     * Safe to call multiple times — idempotent.
     *
     * @param enabled {@code true} to keep screen on, {@code false} to allow normal timeout
     */
    public static void keepScreenOn(boolean enabled) {
        Application app = FaceSwap.getInstance();
        if (app == null) return;

        if (enabled && !keepScreenEnabled) {
            screenCallback = new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                    try {
                        if (activity.getWindow() != null) {
                            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override public void onActivityStarted(@NonNull Activity activity) {}
                @Override public void onActivityResumed(@NonNull Activity activity) {}
                @Override public void onActivityPaused(@NonNull Activity activity) {}
                @Override public void onActivityStopped(@NonNull Activity activity) {}
                @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
                @Override public void onActivityDestroyed(@NonNull Activity activity) {}
            };
            app.registerActivityLifecycleCallbacks(screenCallback);
            keepScreenEnabled = true;

        } else if (!enabled && keepScreenEnabled) {
            if (screenCallback != null) {
                app.unregisterActivityLifecycleCallbacks(screenCallback);
                screenCallback = null;
            }
            keepScreenEnabled = false;
        }
    }

    // ══════════════════════════════════════════════════
    //  Debug Utilities
    // ══════════════════════════════════════════════════

    /**
     * @return {@code true} if the app is running in a debug build
     */
    public static boolean isDebugMode() {
        return BuildConfig.DEBUG;
    }

    /**
     * Shows a Toast only when the app is running in debug mode.
     * <p>
     * In production/release builds, this is a silent no-op.
     * Use this for API error details, HTTP status codes, and other
     * technical messages that the end user should never see.
     *
     * @param context Context for the Toast
     * @param message The debug message to display
     */
    public static void showDebugToast(@NonNull Context context, @NonNull String message) {
        if (isDebugMode()) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
}

package com.facechanger.faceswap.enhance.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Shared preferences utility to track first-launch flows (Language Selected & Onboarding Completed).
 */
public final class AppFaceLanguagePrefs {

    private static final String PREF_NAME = "language_flow_prefs";
    private static final String KEY_LANG_SET = "language_is_set";
    private static final String KEY_ONBOARD_DONE = "onboarding_is_done";

    private AppFaceLanguagePrefs() {
        // No instances
    }

    /**
     * @return true if the user has chosen a language at least once
     */
    public static boolean isLanguageSet(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_LANG_SET, false);
    }

    /**
     * Mark that the user has successfully configured their initial language.
     */
    public static void markLanguageSet(@NonNull Context context) {
        prefs(context).edit().putBoolean(KEY_LANG_SET, true).apply();
    }

    /**
     * @return true if the user completed the onboarding pages
     */
    public static boolean isOnboardingCompleted(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_ONBOARD_DONE, false);
    }

    /**
     * Mark that onboarding is completed.
     */
    public static void markOnboardingCompleted(@NonNull Context context) {
        prefs(context).edit().putBoolean(KEY_ONBOARD_DONE, true).apply();
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}

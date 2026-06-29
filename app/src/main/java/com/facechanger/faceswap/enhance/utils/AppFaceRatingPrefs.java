package com.facechanger.faceswap.enhance.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * SharedPreferences helper that tracks whether the one-time "Rate App"
 * dialog has been shown to the user.
 * <p>
 * Persistent across app restarts so that the user is only asked once per installation.
 */
public final class AppFaceRatingPrefs {

    private static final String PREF_NAME = "rating_dialog_prefs";
    private static final String KEY_SHOWN = "rating_dialog_shown";

    private AppFaceRatingPrefs() {
        // Utility class - no instances
    }

    /**
     * @return {@code true} if the rating dialog has already been shown
     */
    public static boolean hasShownRatingDialog(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_SHOWN, false);
    }

    /**
     * Marks the rating dialog as shown. Future calls to
     * {@link #hasShownRatingDialog(Context)} will return {@code true}.
     */
    public static void markRatingDialogShown(@NonNull Context context) {
        prefs(context).edit().putBoolean(KEY_SHOWN, true).apply();
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}

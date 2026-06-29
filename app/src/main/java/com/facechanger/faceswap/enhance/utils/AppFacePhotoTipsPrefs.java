package com.facechanger.faceswap.enhance.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * SharedPreferences helper that tracks whether the one-time "Photo Tips"
 * info dialog has been shown to the user.
 * <p>
 * The flag is persisted across app launches so the dialog only appears once —
 * the very first time the user taps an upload button on any screen.
 * <p>
 * Uses a dedicated SharedPreferences file to avoid coupling with other
 * persistence layers (SessionManager is in-memory only, DeviceUtils is
 * for device IDs).
 * <p>
 * Usage:
 * <pre>
 *   if (!PhotoTipsPrefs.hasShownInfoDialog(context)) {
 *       PhotoTipsPrefs.markInfoDialogShown(context);
 *       // show the dialog …
 *   }
 * </pre>
 */
public final class AppFacePhotoTipsPrefs {

    private static final String PREF_NAME = "photo_tips_prefs";
    private static final String KEY_SHOWN = "info_dialog_shown";

    private AppFacePhotoTipsPrefs() {
        // Utility class — no instances
    }

    /**
     * @return {@code true} if the photo-tips dialog has already been shown
     */
    public static boolean hasShownInfoDialog(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_SHOWN, false);
    }

    /**
     * Marks the photo-tips dialog as shown. Future calls to
     * {@link #hasShownInfoDialog(Context)} will return {@code true}.
     */
    public static void markInfoDialogShown(@NonNull Context context) {
        prefs(context).edit().putBoolean(KEY_SHOWN, true).apply();
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}

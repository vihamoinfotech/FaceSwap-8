package com.facechanger.faceswap.enhance.utils;

import android.app.Activity;
import android.content.Intent;

import com.facechanger.faceswap.enhance.R;

/**
 * Faster, lighter activity transitions than the default Material window animation
 * (~300ms+), which often feels like "lag" when opening screens.
 */
public final class ActivityNavHelper {

    private ActivityNavHelper() {
        // Utility class — no instances
    }

    /** Start an activity with a quick fade transition. */
    public static void start(Activity from, Intent intent) {
        from.startActivity(intent);
        from.overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
    }
}

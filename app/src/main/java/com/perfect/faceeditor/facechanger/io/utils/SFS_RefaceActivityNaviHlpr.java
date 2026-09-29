package com.perfect.faceeditor.facechanger.io.utils;

import android.app.Activity;
import android.content.Intent;

import com.perfect.faceeditor.facechanger.io.R;

/**
 * Faster, lighter activity transitions than the default Material window animation
 * (~300ms+), which often feels like "lag" when opening screens.
 */
public final class SFS_RefaceActivityNaviHlpr {

    private SFS_RefaceActivityNaviHlpr() {
        // Utility class — no instances
    }

    /** Start an activity with a quick fade transition. */
    public static void start(Activity from, Intent intent) {
        from.startActivity(intent);
        from.overridePendingTransition(R.anim.sfs_re_navi_fade_in, R.anim.sfs_re_navi_fade_out);
    }
}

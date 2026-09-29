package com.perfect.faceeditor.facechanger.io.view;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceAppSystem;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.faceeditor.io.activity.BaseAdActivity;

/**
 * Base activity for the application that correctly handles RTL/LTR layout direction
 * explicitly on the Window DecorView, preventing Android system caching bugs.
 */
public abstract class SFS_BaseAppActivity extends BaseAdActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Enforce the selected language context wrapping
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Explicitly set the layout direction on the window to bypass Android caching bugs
        if (SFS_RefaceAppSystem.isRTLMode(this)) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        } else {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
    }
}

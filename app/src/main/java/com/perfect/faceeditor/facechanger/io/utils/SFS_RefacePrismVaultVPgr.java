package com.perfect.faceeditor.facechanger.io.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class SFS_RefacePrismVaultVPgr extends ViewPager {

    public SFS_RefacePrismVaultVPgr(Context context) {
        super(context);
    }

    public SFS_RefacePrismVaultVPgr(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
}
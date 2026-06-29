package com.facechanger.faceswap.enhance.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class AppFacePrismVaultVPgr extends ViewPager {

    public AppFacePrismVaultVPgr(Context context) {
        super(context);
    }

    public AppFacePrismVaultVPgr(Context context, AttributeSet attrs) {
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
package com.facechanger.faceswap.enhance.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class PrismVaultVPgr extends ViewPager {

    public PrismVaultVPgr(Context context) {
        super(context);
    }

    public PrismVaultVPgr(Context context, AttributeSet attrs) {
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
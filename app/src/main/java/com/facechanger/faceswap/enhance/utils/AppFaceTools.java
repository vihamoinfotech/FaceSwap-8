package com.facechanger.faceswap.enhance.utils;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class AppFaceTools {

    public static void setEdgetoEdge(Window window, View vs, boolean isDarkIcon, boolean shouldHideNavBar) {
        setEdgetoEdge(window, vs, isDarkIcon, shouldHideNavBar, android.R.color.transparent);
    }

    /**
     * Standardized way to enable status bar bleed while protecting UI content.
     * Use this when you want the background to bleed behind the status bar, 
     * but the UI elements (header/buttons) to stay below it.
     * 
     * @param window The activity window.
     * @param contentToPad The view that should be padded down (e.g., your header or main content container).
     * @param isDarkIcon True for dark status bar icons.
     */
    public static void setStatusBarBleed(Window window, View contentToPad, boolean isDarkIcon) {
        if (window == null) return;
        
        // 1. Enable Edge-to-Edge window
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        // 2. Set Icon contrast
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(isDarkIcon);
            controller.setAppearanceLightNavigationBars(isDarkIcon);
        }

        // 3. Apply safety insets to the content container ONLY
        if (contentToPad != null) {
            ViewCompat.setOnApplyWindowInsetsListener(contentToPad, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    public static void setEdgetoEdge(Window window, View vs, boolean isDarkIcon, boolean shouldHideNavBar, int navbarColor) {
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false);
            window.setStatusBarColor(Color.TRANSPARENT);

            if (!shouldHideNavBar) {
                window.setNavigationBarColor(ContextCompat.getColor(window.getContext(), navbarColor));
            } else {
                window.setNavigationBarColor(Color.TRANSPARENT);
            }

            if (vs != null) {
                WindowInsetsControllerCompat controllerCompat = WindowCompat.getInsetsController(window, vs);
                controllerCompat.setAppearanceLightStatusBars(isDarkIcon);

                if (shouldHideNavBar) {
                    controllerCompat.setAppearanceLightNavigationBars(false);
                } else {
                    controllerCompat.setAppearanceLightNavigationBars(isDarkIcon);
                }

                ViewCompat.setOnApplyWindowInsetsListener(vs, (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    int bottom = systemBars.bottom;
                    if (shouldHideNavBar) {
                        bottom = 0;
                        hideNavBar(window);
                    }

                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom);
                    return insets;
                });
            }
        }
    }


    public static void hideNavBar(Window window) {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }

            window.getDecorView().setSystemUiVisibility(flags);
        }
    }
}

package com.facechanger.faceswap.enhance.controller;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.facebook.appevents.AppEventsConstants;
import com.facebook.appevents.AppEventsLogger;

import java.math.BigDecimal;
import java.util.Currency;

public class AppFaceFacebookEventsManager {
    private static final String TAG = "FacebookEventsManager";
    private static AppFaceFacebookEventsManager instance;
    private AppEventsLogger logger;

    private AppFaceFacebookEventsManager() {}

    public static synchronized AppFaceFacebookEventsManager getInstance() {
        if (instance == null) {
            instance = new AppFaceFacebookEventsManager();
        }
        return instance;
    }

    public void init(Context context) {
        try {
            logger = AppEventsLogger.newLogger(context);
            Log.d(TAG, "Facebook AppEventsLogger initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Facebook AppEventsLogger", e);
        }
    }

    /**
     * Logs the App Open / Activation event.
     */
    public void logAppOpen(Context context) {
        try {
            AppEventsLogger.activateApp((Application) context.getApplicationContext());
            if (logger != null) {
                logger.logEvent("app_open");
            }
            Log.d(TAG, "Facebook App Open event logged");
        } catch (Exception e) {
            Log.e(TAG, "Failed to log App Open event to Facebook", e);
        }
    }

    /**
     * Logs the Subscription Purchased event.
     */
    public void logSubscriptionPurchased(String productId, double price, String currencyCode) {
        try {
            if (logger != null) {
                Bundle params = new Bundle();
                params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, productId);
                params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "subscription");
                
                // Log standard subscribe/purchase event with price and currency
                logger.logPurchase(
                        BigDecimal.valueOf(price),
                        Currency.getInstance(currencyCode),
                        params
                );
                
                // Also log a custom subscription purchased event
                Bundle customParams = new Bundle();
                customParams.putString("product_id", productId);
                customParams.putDouble("price", price);
                customParams.putString("currency", currencyCode);
                logger.logEvent("subscription_purchased", customParams);
                
                Log.d(TAG, "Facebook Subscription Purchased logged: " + productId + " (" + price + " " + currencyCode + ")");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to log Subscription Purchased event to Facebook", e);
        }
    }
}

package com.facechanger.faceswap.enhance.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.appset.AppSet;
import com.google.android.gms.appset.AppSetIdClient;

import java.util.UUID;

/**
 * Provides device-level identifiers and app metadata.
 * <p>
 * <b>Device ID:</b> Generated once per app installation and stored locally
 * in private SharedPreferences so users cannot see it. Uses Google Play 
 * Services App Set ID for anonymous cross-app tracking, and falls back to 
 * a UUID if Play Services is unavailable.
 * <p>
 * <b>App Version:</b> Reads from the manifest's {@code versionName}.
 */
public final class DeviceUtils {

    private static final String PREFS_NAME = "faceswap_device_prefs";
    private static final String KEY_DEVICE_ID = "device_internal_id"; // Obfuscated key name
    
    private static String cachedId = null;

    private DeviceUtils() { /* non-instantiable */ }

    public interface DeviceIdCallback {
        void onResult(@NonNull String deviceId);
    }

    /**
     * Returns a stable ID for this device + app installation via callback.
     * Uses AppSet ID if available, otherwise generates and persists a UUID.
     *
     * @param context Any context (application context is used internally)
     * @param callback Callback to receive the device ID
     */
    public static void getDeviceId(@NonNull Context context, @NonNull DeviceIdCallback callback) {
        if (cachedId != null) {
            callback.onResult(cachedId);
            return;
        }

        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String storedId = prefs.getString(KEY_DEVICE_ID, null);

        // 1. Check local secure storage
        if (storedId != null && !storedId.isEmpty()) {
            cachedId = storedId;
            callback.onResult(cachedId);
            return;
        }

        // 2. Try AppSet ID (Google Play Services)
        try {
            AppSetIdClient client = AppSet.getClient(appContext);
            client.getAppSetIdInfo()
                    .addOnSuccessListener(appSetIdInfo -> {
                        String appSetId = appSetIdInfo.getId();
                        if (appSetId != null && !appSetId.isEmpty()) {
                            saveAndReturn(appContext, appSetId, callback);
                        } else {
                            generateUUID(appContext, callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DeviceUtils", "AppSet ID failed: " + e.getMessage());
                        generateUUID(appContext, callback);
                    });
        } catch (Exception e) {
            Log.e("DeviceUtils", "AppSet client initialization failed: " + e.getMessage());
            generateUUID(appContext, callback);
        }
    }

    private static void generateUUID(@NonNull Context context, @NonNull DeviceIdCallback callback) {
        String uuid = UUID.randomUUID().toString();
        saveAndReturn(context, uuid, callback);
    }

    private static void saveAndReturn(@NonNull Context context, @NonNull String id, @NonNull DeviceIdCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DEVICE_ID, id).apply();
        cachedId = id;
        callback.onResult(id);
    }

    /**
     * Returns the app's version name from the manifest (e.g. "1.0").
     *
     * @param context Any context
     * @return Version name string, or "1.0" if unavailable
     */
    @NonNull
    public static String getAppVersion(@NonNull Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return info.versionName != null ? info.versionName : "1.0";
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    /**
     * Returns the device type string to send to the API.
     *
     * @return Always "android" for this platform
     */
    @NonNull
    public static String getDeviceType() {
        return "android";
    }
}

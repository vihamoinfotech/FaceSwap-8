package com.facechanger.faceswap.enhance.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Debug;
import android.util.Log;

import com.facechanger.faceswap.enhance.BuildConfig;

import java.io.File;
import java.security.MessageDigest;

public class SecurityUtils {

    private static final String TAG = "SecurityUtils";

    // SHA-256 for the Debug Keystore (replace with Release SHA-256 before publishing!)
//    private static final String EXPECTED_SIGNATURE_SHA256 = "4B:AA:5C:75:4E:8D:39:5C:4A:EA:31:96:B9:90:41:00:88:44:CD:00:B8:CE:43:06:2D:A3:4B:41:1C:64:31:4C";
    private static final String EXPECTED_SIGNATURE_SHA256 = "00:A5:B5:57:5F:87:4B:54:E9:5B:E3:BC:F7:D1:47:1B:A8:27:CC:01:DE:A6:7B:54:D7:A4:77:12:32:E3:53:D7";

    public static void runComprehensiveSecurityCheck(Context context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Debug build detected. Skipping security checks for local development.");
            return;
        }

        if (isEmulator()) {
            Log.e(TAG, "Emulator detected. Exiting.");
            System.exit(0);
        }

        if (isDebuggerConnected(context)) {
            Log.e(TAG, "Debugger detected. Exiting.");
            System.exit(0);
        }

        if (isDeviceRooted()) {
            Log.e(TAG, "Root detected. Exiting.");
            System.exit(0);
        }

        if (isAppTampered(context, EXPECTED_SIGNATURE_SHA256)) {
            Log.e(TAG, "App signature mismatch. Exiting.");
            System.exit(0);
        }
        
        if (detectHooking()) {
            Log.e(TAG, "Hooking framework detected. Exiting.");
            System.exit(0);
        }
    }

    private static boolean isEmulator() {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT));
    }

    private static boolean isDebuggerConnected(Context context) {
        boolean isDebuggable = false;
        try {
            isDebuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            // Ignore
        }
        return Debug.isDebuggerConnected() || isDebuggable;
    }

    private static boolean isDeviceRooted() {
        String[] paths = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        
        String buildTags = Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        return false;
    }

    private static boolean isAppTampered(Context context, String expectedSha256) {
        try {
            android.content.pm.PackageInfo packageInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            } else {
                packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
            }

            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                signatures = packageInfo.signingInfo != null ? packageInfo.signingInfo.getApkContentsSigners() : null;
            } else {
                signatures = packageInfo.signatures;
            }

            if (signatures != null && signatures.length > 0) {
                for (Signature signature : signatures) {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(signature.toByteArray());
                    byte[] digest = md.digest();
                    StringBuilder hexString = new StringBuilder();
                    for (byte b : digest) {
                        String hex = Integer.toHexString(0xFF & b);
                        if (hex.length() == 1) {
                            hexString.append('0');
                        }
                        hexString.append(hex).append(":");
                    }
                    String currentSha256 = hexString.toString().toUpperCase();
                    if (currentSha256.endsWith(":")) {
                        currentSha256 = currentSha256.substring(0, currentSha256.length() - 1);
                    }
                    if (currentSha256.equals(expectedSha256)) {
                        return false; // Signature matches
                    }
                }
            }
        } catch (Exception e) {
            return true; // Tampered or cannot verify
        }
        return true; // Signature mismatch
    }

    private static boolean detectHooking() {
        try {
            throw new Exception("DUMMY");
        } catch (Exception e) {
            int zygoteInitCallCount = 0;
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                if ("com.android.internal.os.ZygoteInit".equals(stackTraceElement.getClassName())) {
                    zygoteInitCallCount++;
                    if (zygoteInitCallCount == 2) {
                        return true;
                    }
                }
                if ("com.saurik.substrate.MS$2".equals(stackTraceElement.getClassName()) && "invoke".equals(stackTraceElement.getMethodName())) {
                    return true;
                }
                if ("de.robv.android.xposed.XposedBridge".equals(stackTraceElement.getClassName()) && "main".equals(stackTraceElement.getMethodName())) {
                    return true;
                }
                if ("de.robv.android.xposed.XposedBridge".equals(stackTraceElement.getClassName()) && "handleHookedMethod".equals(stackTraceElement.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }
}

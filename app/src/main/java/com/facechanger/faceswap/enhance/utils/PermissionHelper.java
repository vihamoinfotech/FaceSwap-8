package com.facechanger.faceswap.enhance.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Centralized runtime permission helper for camera and gallery access.
 * <p>
 * Usage:
 * <pre>
 *   // Check and request camera permission
 *   if (PermissionHelper.hasCameraPermission(this)) {
 *       openCamera();
 *   } else {
 *       PermissionHelper.requestCameraPermission(this);
 *   }
 *
 *   // Check and request gallery/storage permission
 *   if (PermissionHelper.hasGalleryPermission(this)) {
 *       openGallery();
 *   } else {
 *       PermissionHelper.requestGalleryPermission(this);
 *   }
 *
 *   // Handle results in Activity:
 *   @Override
 *   public void onRequestPermissionsResult(int requestCode, ...) {
 *       if (requestCode == PermissionHelper.RC_CAMERA) { ... }
 *       if (requestCode == PermissionHelper.RC_GALLERY) { ... }
 *   }
 * </pre>
 */
public final class PermissionHelper {

    /** Request code for camera permission. */
    public static final int RC_CAMERA = 1001;

    /** Request code for gallery/storage permission. */
    public static final int RC_GALLERY = 1002;

    /** Request code for camera + gallery combined. */
    public static final int RC_CAMERA_GALLERY = 1003;

    private PermissionHelper() {
        // Utility class — no instances
    }

    // ──────────────────────────────────────────────
    // Camera
    // ──────────────────────────────────────────────

    /**
     * Checks if the app has camera permission.
     */
    public static boolean hasCameraPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests camera permission from the user.
     */
    public static void requestCameraPermission(@NonNull Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.CAMERA},
                RC_CAMERA
        );
    }

    /**
     * Checks if camera permission rationale should be shown.
     */
    public static boolean shouldShowCameraRationale(@NonNull Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.CAMERA);
    }

    // ──────────────────────────────────────────────
    // Gallery / Storage
    // ──────────────────────────────────────────────

    /**
     * Checks if the app has gallery/storage read permission.
     * <p>
     * Handles the API 33+ (Tiramisu) split where READ_EXTERNAL_STORAGE was
     * replaced by READ_MEDIA_IMAGES.
     */
    public static boolean hasGalleryPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Requests gallery/storage read permission.
     */
    public static void requestGalleryPermission(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    RC_GALLERY
            );
        } else {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    RC_GALLERY
            );
        }
    }

    /**
     * Checks if gallery permission rationale should be shown.
     */
    public static boolean shouldShowGalleryRationale(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    // ──────────────────────────────────────────────
    // Combined (Camera + Gallery)
    // ──────────────────────────────────────────────

    /**
     * Checks if both camera and gallery permissions are granted.
     */
    public static boolean hasCameraAndGalleryPermission(@NonNull Context context) {
        return hasCameraPermission(context) && hasGalleryPermission(context);
    }

    /**
     * Requests both camera and gallery permissions at once.
     */
    public static void requestCameraAndGalleryPermission(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_MEDIA_IMAGES
                    },
                    RC_CAMERA_GALLERY
            );
        } else {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    RC_CAMERA_GALLERY
            );
        }
    }

    /**
     * Helper to check if a specific permission result was granted.
     *
     * @param grantResults The grant results array from onRequestPermissionsResult
     * @param index        The index to check
     * @return true if the permission at the given index was granted
     */
    public static boolean isGranted(@NonNull int[] grantResults, int index) {
        return index < grantResults.length
                && grantResults[index] == PackageManager.PERMISSION_GRANTED;
    }

    // ──────────────────────────────────────────────
    // Settings redirect
    // ──────────────────────────────────────────────

    /**
     * Opens the app's system settings page so the user can manually grant permissions.
     */
    public static void openAppSettings(@NonNull Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }

    /**
     * Checks if a permission was permanently denied (user selected "Don't ask again").
     * This is true when the permission is NOT granted AND the system will NOT show the rationale.
     *
     * @param activity   The activity to check from
     * @param permission The permission string to check
     * @return true if permanently denied
     */
    public static boolean isPermanentlyDenied(@NonNull Activity activity,
                                               @NonNull String permission) {
        return ContextCompat.checkSelfPermission(activity, permission)
                != PackageManager.PERMISSION_GRANTED
                && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }
}

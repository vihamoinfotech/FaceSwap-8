package com.facechanger.faceswap.enhance.utils;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;

import androidx.core.content.FileProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.facechanger.faceswap.enhance.R;

import java.io.File;

/**
 * Helper for showing a "Camera / Gallery" bottom-sheet picker and creating
 * a FileProvider URI for camera capture output.
 *
 * Usage in any Activity:
 * <pre>
 *   // Fields
 *   private Uri cameraUri;
 *   private ActivityResultLauncher<Uri> cameraLauncher;
 *
 *   // In onCreate (before any launcher is launched)
 *   cameraLauncher = registerForActivityResult(
 *       new ActivityResultContracts.TakePicture(), success -> {
 *           if (success && cameraUri != null) handleCapturedImage(cameraUri);
 *       });
 *
 *   // When the upload zone is tapped
 *   ImagePickerHelper.show(this,
 *       () -> { cameraUri = ImagePickerHelper.createCameraOutputUri(this);
 *               if (cameraUri != null) cameraLauncher.launch(cameraUri); },
 *       this::openGallery);
 * </pre>
 */
public final class ImagePickerHelper {

    private ImagePickerHelper() {}

    // ──────────────────────────────────────────────
    //  Camera URI helper
    // ──────────────────────────────────────────────

    /**
     * Creates a FileProvider content URI pointing to a fresh JPEG file inside
     * {@code getCacheDir()/camera_images/}. Pass this URI to
     * {@code ActivityResultContracts.TakePicture#launch(Uri)}.
     *
     * @return a valid content URI, or {@code null} if creation failed
     */
    public static Uri createCameraOutputUri(Context context) {
        try {
            File dir = new File(context.getCacheDir(), "camera_images");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File file = new File(dir, "capture_" + System.currentTimeMillis() + ".jpg");
            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    file);
        } catch (Exception e) {
            return null;
        }
    }

    // ──────────────────────────────────────────────
    //  Bottom-sheet picker
    // ──────────────────────────────────────────────

    /**
     * Shows a dark-themed bottom sheet with "Take Photo" and "Choose from Gallery" options.
     *
     * @param context   the Activity context
     * @param onCamera  called when the user picks "Take Photo"
     * @param onGallery called when the user picks "Choose from Gallery"
     */
    public static void show(Context context, Runnable onCamera, Runnable onGallery) {
        BottomSheetDialog sheet = new BottomSheetDialog(context);
        sheet.setContentView(R.layout.app_image_source_dialog);

        // Clear the default Material container background so our rounded drawable shows
        View container = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (container != null) container.setBackgroundColor(Color.TRANSPARENT);

        View optionCamera  = sheet.findViewById(R.id.optionCamera);
        View optionGallery = sheet.findViewById(R.id.optionGallery);
        View btnCancel     = sheet.findViewById(R.id.btnCancel);

        if (optionCamera != null)  optionCamera.setOnClickListener(v  -> { sheet.dismiss(); onCamera.run();  });
        if (optionGallery != null) optionGallery.setOnClickListener(v -> { sheet.dismiss(); onGallery.run(); });
        if (btnCancel != null)     btnCancel.setOnClickListener(v     -> sheet.dismiss());

        sheet.show();
    }
}

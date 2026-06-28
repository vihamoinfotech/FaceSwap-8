package com.facechanger.faceswap.enhance.view;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.AppSystem;
import com.facechanger.faceswap.enhance.utils.SessionManager;
import com.facechanger.faceswap.enhance.utils.Tools;
import com.facechanger.faceswap.enhance.view.widget.BeforeAfterSliderView;
import com.faceenhance.facechanger.activity.BaseAdActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Result screen displaying the generated/swapped image with a Before/After
 * comparison slider.
 * <p>
 * Features:
 * <ul>
 * <li><b>Before/After Slider</b> — Drag left-right to compare original vs
 * result</li>
 * <li><b>Download</b> — saves the <i>result</i> image to device gallery via
 * MediaStore (API 29+)
 * or external storage (API 28-) with permission check</li>
 * <li><b>Share</b> — copies image to cache, creates FileProvider URI,
 * launches native share sheet</li>
 * <li><b>Edit</b> — opens EditImageActivity with the result URL</li>
 * </ul>
 */
public class DownloadShareActivity extends BaseAppActivity {

    private static final String TAG = "DownloadShareActivity";
    private static final int RC_WRITE_STORAGE = 3001;

    /** Intent extra key for the generated/result image (URL or Base64). */
    public static final String EXTRA_IMAGE_URL = "image_url";
    /** Intent extra key for the original/source image (URL or Base64). */
    public static final String EXTRA_ORIGINAL_IMAGE_URL = "original_image_url";

    private String imageUrl;
    private String originalImageUrl;
    private boolean isVideo = false;
    private ExoPlayer exoPlayer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_share);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.downloadShareContent), false);

        imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        originalImageUrl = getIntent().getStringExtra(EXTRA_ORIGINAL_IMAGE_URL);
        
        // Robust fallback: dynamically check if the URL points to a video format
        boolean isVideoDetected = false;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String extension = getFileExtension(imageUrl);
            if (extension.equalsIgnoreCase("mp4") || extension.equalsIgnoreCase("avi") || 
                extension.equalsIgnoreCase("mov") || extension.equalsIgnoreCase("3gp") || 
                extension.equalsIgnoreCase("mkv") || extension.equalsIgnoreCase("webm")) {
                isVideoDetected = true;
            }
        }
        isVideo = getIntent().getBooleanExtra("is_video", isVideoDetected);

        setupToolbar();
        setupPreview();
        setupClickListeners();
        loadAds();
    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void setupPreview() {
        BeforeAfterSliderView slider = findViewById(R.id.beforeAfterSlider);
        ImageView ivResultPreview = findViewById(R.id.ivResultPreview);
        PlayerView playerView = findViewById(R.id.playerView);
        ImageView btnedit = findViewById(R.id.btnedit);

        if (isVideo) {
            slider.setVisibility(View.GONE);
            ivResultPreview.setVisibility(View.GONE);
            btnedit.setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);

            exoPlayer = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(exoPlayer);
            MediaItem mediaItem;
            if (imageUrl.startsWith("http")) {
                // If it requires auth header we would configure it, but ExoPlayer can play direct public urls or we can configure a custom DataSource.
                // Assuming it's a direct url or file path for now
                mediaItem = MediaItem.fromUri(Uri.parse(imageUrl));
            } else {
                mediaItem = MediaItem.fromUri(Uri.parse(imageUrl));
            }
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();
            return;
        }

        // Build the correct Glide model for the BEFORE image (user's original)
        Object beforeModel = buildGlideModel(originalImageUrl);

        // Build the correct Glide model for the AFTER image (API response)
        Object afterModel = buildGlideModel(imageUrl);

        if (beforeModel != null && afterModel != null) {
            // ── Both images available → use the comparison slider ──
            Log.d(TAG, "Slider: Before=" + originalImageUrl + " | After=" + imageUrl);
            slider.setVisibility(View.VISIBLE);
            ivResultPreview.setVisibility(View.GONE);
            slider.setImages(beforeModel, afterModel);
        } else if (afterModel != null) {
            // ── Only the result image → show as regular ImageView ──
            Log.w(TAG, "Before image unavailable, showing single image in regular ImageView");
            slider.setVisibility(View.GONE);
            ivResultPreview.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(afterModel)
                    .placeholder(R.color.card_background)
                    .error(R.color.card_background)
                    .into(ivResultPreview);
        }
    }

    /**
     * Builds the correct Glide model object from a string that could be:
     * - An HTTP/HTTPS URL (needs authorization header)
     * - A local file path (needs File object)
     * - A base64 data URI (pass as-is)
     * - null or empty → returns null
     */
    @Nullable
    private Object buildGlideModel(@Nullable String source) {
        if (source == null || source.isEmpty())
            return null;

        if (source.startsWith("http://") || source.startsWith("https://")) {
            // Remote URL — wrap with auth header
            return com.facechanger.faceswap.enhance.utils.GlideHelper.authorizedUrl(source);
        } else if (source.startsWith("/")) {
            // Local file path — convert to File for Glide
            java.io.File file = new java.io.File(source);
            return file.exists() ? file : null;
        } else {
            // Base64 data URI or other format — pass as-is
            return source;
        }
    }

    private void setupClickListeners() {
        // Download button
        findViewById(R.id.btnDownload).setOnClickListener(v -> handleDownloadClick());

        // Share button (in header, top-right)
        findViewById(R.id.btnShare).setOnClickListener(v -> handleShareClick());

        // Edit button
        findViewById(R.id.btnedit).setOnClickListener(v -> openEditScreen());
    }

    private void openEditScreen() {
        Intent intent = new Intent(this, EditImageActivity.class);
        intent.putExtra("image_url", imageUrl);
        startActivity(intent);
    }

    // ──────────────────────────────────────────────
    // Download to Gallery
    // ──────────────────────────────────────────────

    private void handleDownloadClick() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.DownloadShareActivity_no_image_to_download), Toast.LENGTH_SHORT).show();
            return;
        }

        // API 28 and below: request WRITE_EXTERNAL_STORAGE permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                        RC_WRITE_STORAGE);
                return;
            }
        }

        downloadImageToGallery();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadImageToGallery();
            } else {
                Toast.makeText(this, getString(R.string.DownloadShareActivity_storage_permission_is_needed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Downloads the image and saves it to the device gallery.
     * Handles both URL images and Base64 encoded images.
     */
    private void downloadImageToGallery() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving to Gallery…");
        progressDialog.setCancelable(false);
        progressDialog.show();

        executor.execute(() -> {
            try {
                if (isVideo) {
                    boolean saved = downloadVideoToGallery();
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (saved) {
                            Toast.makeText(this, getString(R.string.download_saved), Toast.LENGTH_SHORT).show();
                            // Show rating dialog if it hasn't been shown yet
                            if (!com.facechanger.faceswap.enhance.utils.RatingPrefs.hasShownRatingDialog(this)) {
                                com.facechanger.faceswap.enhance.utils.RatingPrefs.markRatingDialogShown(this);
                                getWindow().getDecorView().postDelayed(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        com.facechanger.faceswap.enhance.controller.AppDialogController.showRatingDialog(this);
                                    }
                                }, 600);
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                Bitmap bitmap;

                if (imageUrl.startsWith("data:image") || isBase64(imageUrl)) {
                    // Decode Base64 image
                    String base64Data = imageUrl;
                    if (base64Data.contains(",")) {
                        base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                    }
                    byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
                    bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                } else {
                    // Download from URL
                    bitmap = downloadBitmapFromUrl(imageUrl);
                }

                if (bitmap == null) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Save to gallery
                boolean saved = saveBitmapToGallery(bitmap,
                        "FaceSwap_" + System.currentTimeMillis() + ".png");
                bitmap.recycle();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (saved) {
                        Toast.makeText(this, getString(R.string.download_saved), Toast.LENGTH_SHORT).show();
                        // Show rating dialog if it hasn't been shown yet
                        if (!com.facechanger.faceswap.enhance.utils.RatingPrefs.hasShownRatingDialog(this)) {
                            com.facechanger.faceswap.enhance.utils.RatingPrefs.markRatingDialogShown(this);
                            // Add a small delay so the "Saved to Gallery" Toast shows clearly before the dialog opens
                            getWindow().getDecorView().postDelayed(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    com.facechanger.faceswap.enhance.controller.AppDialogController.showRatingDialog(this);
                                }
                            }, 600);
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    AppSystem.showDebugToast(this, "Download error: " + e.getMessage());
                    Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Downloads a bitmap from a URL, adding Bearer auth for API URLs.
     */
    private Bitmap downloadBitmapFromUrl(String urlStr) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);

            String token = SessionManager.getInstance().getToken();
            if (!token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Download failed: HTTP " + connection.getResponseCode());
                return null;
            }

            InputStream in = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            in.close();
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "downloadBitmapFromUrl failed", e);
            return null;
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    /**
     * Saves bitmap to the device gallery using MediaStore (API 29+)
     * or external storage directory (API 28-).
     */
    private boolean saveBitmapToGallery(Bitmap bitmap, String fileName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+: Use MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FaceSwap");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null)
                    return false;

                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null)
                        return false;
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }

                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
                return true;
            } else {
                // API 28-: Write directly to Pictures directory
                File picturesDir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "FaceSwap");
                if (!picturesDir.exists()) {
                    // noinspection ResultOfMethodCallIgnored
                    picturesDir.mkdirs();
                }

                File file = new File(picturesDir, fileName);
                try (OutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }

                // Notify media scanner
                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                scanIntent.setData(Uri.fromFile(file));
                sendBroadcast(scanIntent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "saveBitmapToGallery failed", e);
            return false;
        }
    }

    private String getFileExtension(String url) {
        if (url == null || url.isEmpty()) {
            return "mp4";
        }
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            if (path != null) {
                int lastDot = path.lastIndexOf('.');
                if (lastDot != -1) {
                    return path.substring(lastDot + 1).toLowerCase();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse URL for extension: " + url, e);
        }
        return "mp4";
    }

    private String getMimeType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "video/mp4";
        }
        String mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimeType == null || mimeType.isEmpty()) {
            switch (extension.toLowerCase()) {
                case "mov":
                    return "video/quicktime";
                case "avi":
                    return "video/x-msvideo";
                case "wmv":
                    return "video/x-ms-wmv";
                case "flv":
                    return "video/x-flv";
                case "3gp":
                    return "video/3gpp";
                default:
                    return "video/mp4";
            }
        }
        return mimeType;
    }

    /**
     * Downloads a file from the given URL and writes it directly to the specified OutputStream.
     * This eliminates duplicate networking code for both downloading to gallery and downloading for sharing.
     */
    private boolean downloadUrlToStream(String urlStr, OutputStream output) {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.setDoInput(true);
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Download failed: HTTP " + connection.getResponseCode() + " for " + urlStr);
                return false;
            }

            input = connection.getInputStream();
            byte[] data = new byte[8192];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "downloadUrlToStream error for URL: " + urlStr, e);
            return false;
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (Exception ignored) {}
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean downloadVideoToGallery() {
        String extension = getFileExtension(imageUrl);
        String mimeType = getMimeType(extension);
        String fileName = "FaceSwap_" + System.currentTimeMillis() + "." + extension;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.TITLE, fileName);
                values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
                values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/FaceSwap");

                Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                        if (output != null && downloadUrlToStream(imageUrl, output)) {
                            return true;
                        }
                    }
                    // Clean up partial/empty database record if stream writing failed
                    getContentResolver().delete(uri, null, null);
                }
                return false;
            } else {
                File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                File appDir = new File(publicDir, "FaceSwap");
                if (!appDir.exists()) {
                    appDir.mkdirs();
                }
                File file = new File(appDir, fileName);

                try (OutputStream output = new FileOutputStream(file)) {
                    if (downloadUrlToStream(imageUrl, output)) {
                        // Notify media scanner
                        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        scanIntent.setData(Uri.fromFile(file));
                        sendBroadcast(scanIntent);
                        return true;
                    }
                }
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving video to gallery", e);
            return false;
        }
    }

    // ──────────────────────────────────────────────
    // Share
    // ──────────────────────────────────────────────

    private void handleShareClick() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.DownloadShareActivity_no_image_to_share), Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, getString(R.string.share_preparing), Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                if (isVideo) {
                    String extension = getFileExtension(imageUrl);
                    String mimeType = getMimeType(extension);

                    File shareDir = new File(getCacheDir(), "shared_images");
                    if (!shareDir.exists()) {
                        shareDir.mkdirs();
                    }
                    File shareFile = new File(shareDir, "faceswap_share_" + System.currentTimeMillis() + "." + extension);

                    try (OutputStream output = new FileOutputStream(shareFile)) {
                        if (!downloadUrlToStream(imageUrl, output)) {
                            runOnUiThread(() -> Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_SHORT).show());
                            return;
                        }
                    }

                    Uri shareUri = FileProvider.getUriForFile(this,
                            getPackageName() + ".fileprovider", shareFile);

                    runOnUiThread(() -> {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType(mimeType.startsWith("video/") ? "video/*" : mimeType);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(shareIntent, "Share Video"));
                    });
                    return;
                }

                // Download/decode the image into a temp file
                File shareDir = new File(getCacheDir(), "shared_images");
                if (!shareDir.exists()) {
                    // noinspection ResultOfMethodCallIgnored
                    shareDir.mkdirs();
                }

                File shareFile = new File(shareDir, "faceswap_share_" + System.currentTimeMillis() + ".png");

                Bitmap bitmap;
                if (imageUrl.startsWith("data:image") || isBase64(imageUrl)) {
                    String base64Data = imageUrl;
                    if (base64Data.contains(",")) {
                        base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                    }
                    byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
                    bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                } else {
                    bitmap = downloadBitmapFromUrl(imageUrl);
                }

                if (bitmap == null) {
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.DownloadShareActivity_failed_to_prepare_image), Toast.LENGTH_SHORT)
                            .show());
                    return;
                }

                try (OutputStream out = new FileOutputStream(shareFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                bitmap.recycle();

                // Create FileProvider URI
                Uri shareUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", shareFile);

                runOnUiThread(() -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share Image"));
                });

            } catch (Exception e) {
                Log.e(TAG, "Share failed", e);
                runOnUiThread(() -> AppSystem.showDebugToast(this, "Share error: " + e.getMessage()));
            }
        });
    }

    // ──────────────────────────────────────────────
    // Utilities
    // ──────────────────────────────────────────────

    /**
     * Simple heuristic to detect if a string is Base64-encoded image data.
     */
    private boolean isBase64(String str) {
        if (str == null || str.isEmpty())
            return false;
        // Check if it doesn't look like a URL and is long enough
        return !str.startsWith("http") && str.length() > 100;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}

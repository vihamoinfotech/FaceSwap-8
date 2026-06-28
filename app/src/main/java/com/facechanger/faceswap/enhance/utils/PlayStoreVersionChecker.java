package com.facechanger.faceswap.enhance.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the Google Play Store for the latest published version of the app
 * and compares it with the currently installed version.
 * <p>
 * Uses a lightweight HTML scrape of the Play Store listing page to extract
 * the latest version string — no additional SDK or library required.
 */
public class PlayStoreVersionChecker {

    private static final String TAG = "PlayStoreVersionChecker";
    private static final int TIMEOUT_MS = 8000;

    public interface VersionCheckCallback {
        /** Called on the main thread when the check completes. */
        void onResult(boolean updateAvailable, @NonNull String currentVersion, @NonNull String latestVersion);

        /** Called on the main thread if the check fails (network error, parse error, etc.). */
        void onError(@NonNull String errorMessage);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Asynchronously checks whether a newer version is available on Google Play.
     *
     * @param context  Application or activity context.
     * @param callback Result callback, always invoked on the main thread.
     */
    public static void check(@NonNull Context context, @NonNull VersionCheckCallback callback) {
        String packageName = context.getPackageName();
        String currentVersion = getCurrentVersionName(context);

        executor.execute(() -> {
            try {
                String latestVersion = fetchLatestVersionFromPlayStore(packageName);
                boolean needsUpdate = isNewerVersion(currentVersion, latestVersion);

                String finalCurrentVersion = currentVersion;
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onResult(needsUpdate, finalCurrentVersion, latestVersion));
            } catch (Exception e) {
                Log.e(TAG, "Version check failed", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    /**
     * Fetches the latest version string from the Google Play Store listing page.
     */
    private static String fetchLatestVersionFromPlayStore(String packageName) throws Exception {
        String playStoreUrl = "https://play.google.com/store/apps/details?id=" + packageName + "&hl=en";
        HttpURLConnection connection = null;

        try {
            URL url = new URL(playStoreUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("Play Store returned HTTP " + responseCode);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String html = sb.toString();
            return parseVersionFromHtml(html);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Parses the version string from the Play Store HTML page.
     * Looks for patterns like [[[\"1.2.3\"]]] which is how Google embeds version info.
     */
    private static String parseVersionFromHtml(String html) throws Exception {
        // Pattern 1: Modern Play Store pages embed version like [[["X.Y.Z"]]]
        Pattern pattern1 = Pattern.compile("\\[\\[\\[\"(\\d+\\.\\d+(?:\\.\\d+)*)\"\\]\\]\\]");
        Matcher matcher1 = pattern1.matcher(html);
        if (matcher1.find()) {
            return matcher1.group(1);
        }

        // Pattern 2: Look for "Current Version" followed by version
        Pattern pattern2 = Pattern.compile("Current Version[^>]*>\\s*<[^>]*>\\s*([\\d.]+)");
        Matcher matcher2 = pattern2.matcher(html);
        if (matcher2.find()) {
            return matcher2.group(1);
        }

        // Pattern 3: Generic version pattern in structured data
        Pattern pattern3 = Pattern.compile("\"softwareVersion\"\\s*:\\s*\"([\\d.]+)\"");
        Matcher matcher3 = pattern3.matcher(html);
        if (matcher3.find()) {
            return matcher3.group(1);
        }

        throw new Exception("Could not parse version from Play Store page");
    }

    /**
     * Compares two version strings (e.g. "1.2.0" vs "1.3.0").
     *
     * @return {@code true} if {@code latestVersion} is strictly greater than {@code currentVersion}.
     */
    static boolean isNewerVersion(@NonNull String currentVersion, @NonNull String latestVersion) {
        try {
            int[] current = parseVersionParts(currentVersion);
            int[] latest  = parseVersionParts(latestVersion);

            int maxLen = Math.max(current.length, latest.length);
            for (int i = 0; i < maxLen; i++) {
                int c = i < current.length ? current[i] : 0;
                int l = i < latest.length  ? latest[i]  : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
            return false; // versions are equal
        } catch (Exception e) {
            Log.e(TAG, "Version comparison failed: current=" + currentVersion + " latest=" + latestVersion, e);
            return false;
        }
    }

    private static int[] parseVersionParts(@NonNull String version) {
        // Strip any non-numeric suffix (e.g. "1.0-beta")
        String cleaned = version.replaceAll("[^\\d.]", "");
        String[] parts = cleaned.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
        }
        return result;
    }

    /**
     * Returns the current app versionName from the PackageManager.
     */
    @NonNull
    private static String getCurrentVersionName(@NonNull Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "0.0.0";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get package info", e);
            return "0.0.0";
        }
    }
}

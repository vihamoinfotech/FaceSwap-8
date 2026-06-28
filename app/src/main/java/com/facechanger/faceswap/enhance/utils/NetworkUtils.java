package com.facechanger.faceswap.enhance.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Utility class for checking network connectivity.
 */
public final class NetworkUtils {

    private NetworkUtils() {
        // Utility class
    }

    /**
     * Checks if the device is currently connected to the internet.
     *
     * @param context Context needed to access ConnectivityManager
     * @return true if connected or connecting, false otherwise
     */
    public static boolean isConnected(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true; // Fail open if we can't determine, though this is rare

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) {
                // Fallback attempt before failing
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            if (capabilities == null) return false;
            
            // Just verify it actually has internet capabilities, instead of strictly verifying the transport type.
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            // Fallback for older devices
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    /**
     * Convenience overload that uses the application context.
     * <p>
     * Allows calling {@code NetworkUtils.isConnected()} from anywhere
     * without passing a Context parameter.
     *
     * @return true if connected, false otherwise
     */
    public static boolean isConnected() {
        return isConnected(com.facechanger.faceswap.enhance.FaceSwap.getAppContext());
    }
}

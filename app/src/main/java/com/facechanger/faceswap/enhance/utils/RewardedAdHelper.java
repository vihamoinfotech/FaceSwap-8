package com.facechanger.faceswap.enhance.utils;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Helper for loading and showing Google Rewarded Ads.
 * <p>
 * Used when user has insufficient coins and taps "Watch Ad to Continue".
 * The ad unit ID is pulled from the server config via SessionManager.
 * <p>
 * Usage:
 * <pre>
 *   RewardedAdHelper.preload(activity);
 *   // Later:
 *   RewardedAdHelper.show(activity, callback);
 * </pre>
 */
public final class RewardedAdHelper {

    private static final String TAG = "RewardedAdHelper";

    /** Test ad unit ID (replace with real ID from server config) */
    private static final String TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    @Nullable
    private static RewardedAd rewardedAd;
    private static boolean isLoading = false;

    private RewardedAdHelper() { /* Utility class — no instances */ }

    /**
     * Callback interface for rewarded ad completion.
     */
    public interface OnRewardedAdCallback {
        /** Called when user fully watched the ad and earned the reward. */
        void onAdCompleted();

        /** Called when ad failed to load or show. */
        void onAdFailed(@NonNull String error);
    }

    /**
     * Returns the configured ad unit ID (from server or test fallback).
     */
    @NonNull
    private static String getAdUnitId() {
        SessionManager session = SessionManager.getInstance();
        if (session.getConfig() != null) {
            String configId = session.getConfig().getRewardsAdsId();
            if (configId != null && !configId.isEmpty()) {
                return configId;
            }
        }
        return TEST_AD_UNIT_ID;
    }

    /**
     * Pre-loads a rewarded ad so it is ready when needed.
     * Safe to call multiple times — skips if already loading or loaded.
     *
     * @param activity Activity context for ad loading
     */
    public static void preload(@NonNull Activity activity) {
        if (rewardedAd != null || isLoading) return;

        isLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(activity, getAdUnitId(), adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        isLoading = false;
                        Log.d(TAG, "Rewarded ad loaded successfully");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        isLoading = false;
                        Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }

    /**
     * @return true if a rewarded ad is loaded and ready to show
     */
    public static boolean isAdReady() {
        return rewardedAd != null;
    }

    /**
     * Shows a rewarded ad. If no ad is pre-loaded, attempts to load one first.
     *
     * @param activity Activity context
     * @param callback Callback for ad completion or failure
     */
    public static void show(@NonNull Activity activity,
                            @NonNull OnRewardedAdCallback callback) {
        if (rewardedAd != null) {
            showLoadedAd(activity, callback);
        } else {
            // Attempt to load and show immediately
            isLoading = true;
            AdRequest adRequest = new AdRequest.Builder().build();

            RewardedAd.load(activity, getAdUnitId(), adRequest,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            rewardedAd = ad;
                            isLoading = false;
                            showLoadedAd(activity, callback);
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            isLoading = false;
                            Log.e(TAG, "Ad failed to load on-demand: " + loadAdError.getMessage());
                            callback.onAdFailed("Ad not available. Please try again later.");
                        }
                    });
        }
    }

    /**
     * Shows an already-loaded rewarded ad with proper lifecycle callbacks.
     */
    private static void showLoadedAd(@NonNull Activity activity,
                                      @NonNull OnRewardedAdCallback callback) {
        if (rewardedAd == null) {
            callback.onAdFailed("Ad not loaded");
            return;
        }

        final boolean[] rewarded = {false};

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                // Pre-load the next ad
                preload(activity);

                if (rewarded[0]) {
                    callback.onAdCompleted();
                } else {
                    callback.onAdFailed("Ad was dismissed before completion.");
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                rewardedAd = null;
                preload(activity);
                callback.onAdFailed(adError.getMessage());
            }
        });

        rewardedAd.show(activity, rewardItem -> {
            rewarded[0] = true;
            Log.d(TAG, "User earned reward: " + rewardItem.getAmount()
                    + " " + rewardItem.getType());
        });
    }

    /**
     * Clears any loaded ad (call on app exit or cleanup).
     */
    public static void release() {
        rewardedAd = null;
        isLoading = false;
    }
}

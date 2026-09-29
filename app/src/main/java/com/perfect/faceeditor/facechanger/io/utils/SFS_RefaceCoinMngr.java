package com.perfect.faceeditor.facechanger.io.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceApDlogController;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceOnCoinDialogListener;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceOnDialogActionListener;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefacePurchaseListener;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceRevCatMngr;
import com.perfect.faceeditor.facechanger.io.view.SFS_RefaceLoadingActivity;
import com.perfect.faceeditor.facechanger.io.view.SFS_RefaceCoinStreActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized coin validation and gating utility.
 * <p>
 * Provides a single entry-point for all feature activities to validate
 * credit balance before proceeding with AI image generation.
 * <p>
 * Usage:
 * <pre>
 *   CoinManager.checkAndProceed(this, LottieLoadingActivity.ACTION_FACE_SWAP, () -> {
 *       // launch LottieLoadingActivity with intent
 *   });
 * </pre>
 */
public final class SFS_RefaceCoinMngr {

    private static final String TAG = "CoinManager";

    private SFS_RefaceCoinMngr() { /* Utility class — no instances */ }

    /** Maps LottieLoadingActivity ACTION_* to AppSystem feature flag keys. */
    private static final Map<String, String> ACTION_TO_FLAG = new HashMap<>();

    static {
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_FACE_SWAP, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_FACE_SWAP);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_AI_IMAGE, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_AI_IMAGE);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_MULTI_FACE_SWAP, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_MULTI_SWAP);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_REMOVE_BG, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_REMOVE_BG);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_UPSCALE, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_UPSCALE);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_COUPLE_SWAP, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_COUPLE_SWAP);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_BG_REPLACE, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_BG_REPLACE);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_ENHANCE_GFPGAN, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_ENHANCE);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_ENHANCE_FACE, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_ENHANCE);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_TEXT_TO_IMAGE, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_TEXT_TO_IMG);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_IMAGE_GENERATE, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_IMAGE_GENERATE);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_VIRTUAL_TRY_ON, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_VIRTUAL_TRY);
        ACTION_TO_FLAG.put(SFS_RefaceLoadingActivity.ACTION_VIDEO_FACE_SWAP, SFS_RefaceAppSystem.KEY_REQUIRE_COINS_VIDEO_FACE_SWAP);
    }

    /**
     * Main entry-point for coin validation before any AI feature.
     * <p>
     * Flow:
     * <ol>
     *   <li>If coin system is disabled → proceed immediately</li>
     *   <li>If this feature doesn't require coins → proceed immediately</li>
     *   <li>If user has enough coins AND {@code skip_coin_check_if_enough} → proceed immediately</li>
     *   <li>If user has enough coins AND NOT skip → show confirmation dialog</li>
     *   <li>If insufficient coins → show two-option dialog (Buy / Watch Ad)</li>
     * </ol>
     *
     * @param activity  The calling activity
     * @param action    LottieLoadingActivity.ACTION_* constant
     * @param onProceed Runnable to execute when user is cleared to proceed
     */
    public static void checkAndProceed(@NonNull Activity activity,
                                       @NonNull String action,
                                       @NonNull Runnable onProceed) {
        // Gate 1: Coin system globally disabled
        if (!SFS_RefaceAppSystem.isFeatureEnabled(SFS_RefaceAppSystem.KEY_COIN_SYSTEM_ENABLED)) {
            onProceed.run();
            return;
        }

        // Gate 2: This feature doesn't require coins
        if (!isCoinsRequired(action)) {
            onProceed.run();
            return;
        }

        SFS_RefaceSessionMngr session = SFS_RefaceSessionMngr.getInstance();
        double balance = session.getCurrentCredits();
        double cost = session.getFeatureCost(action);

        // Gate 3: User has enough coins
        if (balance >= cost) {
            if (SFS_RefaceAppSystem.isFeatureEnabled(SFS_RefaceAppSystem.KEY_SKIP_COIN_CHECK_IF_ENOUGH)) {
                // Skip dialog — proceed directly
                onProceed.run();
            } else {
                // Show confirmation dialog
                showCoinConfirmationDialog(activity, action, cost, balance, onProceed);
            }
            return;
        }

        // Gate 4: Insufficient coins — show two-option dialog
        Log.d(TAG, "Insufficient coins: balance=" + balance + ", cost=" + cost);
        showInsufficientCoinsDialog(activity, action, cost, balance, onProceed);
    }

    /**
     * Variant of checkAndProceed that takes a custom, dynamically calculated cost.
     * Useful for video features where cost depends on duration.
     *
     * @param activity   The calling activity
     * @param action     LottieLoadingActivity.ACTION_* constant
     * @param customCost The pre-calculated dynamic cost
     * @param onProceed  Runnable to execute when user is cleared to proceed
     */
    public static void checkAndProceedWithCost(@NonNull Activity activity,
                                               @NonNull String action,
                                               double customCost,
                                               @NonNull Runnable onProceed) {
        // Gate 1: Coin system globally disabled
        if (!SFS_RefaceAppSystem.isFeatureEnabled(SFS_RefaceAppSystem.KEY_COIN_SYSTEM_ENABLED)) {
            onProceed.run();
            return;
        }

        // Gate 2: This feature doesn't require coins
        if (!isCoinsRequired(action)) {
            onProceed.run();
            return;
        }

        SFS_RefaceSessionMngr session = SFS_RefaceSessionMngr.getInstance();
        double balance = session.getCurrentCredits();

        // Gate 3: User has enough coins
        if (balance >= customCost) {
            if (SFS_RefaceAppSystem.isFeatureEnabled(SFS_RefaceAppSystem.KEY_SKIP_COIN_CHECK_IF_ENOUGH)) {
                onProceed.run();
            } else {
                showCoinConfirmationDialog(activity, action, customCost, balance, onProceed);
            }
            return;
        }

        // Gate 4: Insufficient coins
        Log.d(TAG, "Insufficient coins: balance=" + balance + ", cost=" + customCost);
        showInsufficientCoinsDialog(activity, action, customCost, balance, onProceed);
    }

    /**
     * Shows a confirmation dialog when user HAS enough coins.
     * "This will use X coins. Continue?"
     */
    private static void showCoinConfirmationDialog(@NonNull Activity activity,
                                                    @NonNull String action,
                                                    double cost,
                                                    double balance,
                                                    @NonNull Runnable onProceed) {

//        String title = "Coins Required!";
//        String message = "Continuing will use " + formatCost(cost) + " " + costUnit + " from your balance. Please confirm to proceed.";
//        String buttonText = "Continue";

        String title = "Use Coins?";
        String costUnit = (cost == 1) ? "coin" : "coins";
        String message = "This action will use " + formatCost(cost) + " " + costUnit + " from your balance. Do you want to proceed?";
        String buttonText = "Continue";

        SFS_RefaceApDlogController.showCoinsRequiredDialog(
                activity,
                com.perfect.faceeditor.facechanger.io.R.drawable.sfs_face_re_coin_new,
                title,
                message,
                buttonText,
                new SFS_RefaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        onProceed.run();
                    }

                    @Override
                    public void onDismiss() {
                        // User cancelled
                    }
                }
        );
    }

    /**
     * Shows the two-option Insufficient Coins dialog.
     * Buttons: "Buy Coins" + "Watch Ad" (configurable visibility).
     */
    private static void showInsufficientCoinsDialog(@NonNull Activity activity,
                                                     @NonNull String action,
                                                     double cost,
                                                     double balance,
                                                     @NonNull Runnable onProceed) {
        SFS_RefaceApDlogController.showInsufficientCoinsDialog(
                activity,
                cost,
                balance,
                new SFS_RefaceOnCoinDialogListener() {
                    @Override
                    public void onBuyCoins() {
                        if (!SFS_RefaceAppSystem.isFeatureEnabled(SFS_RefaceAppSystem.KEY_SHOW_COINS_DIRECT_PURCHASE)) {
                            try {
                                Intent intent = new Intent(activity, SFS_RefaceCoinStreActivity.class);
                                activity.startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to open StoreActivity", e);
                            }
                        } else {
                            SFS_RefaceRevCatMngr.getInstance().fetchOfferings(new SFS_RefacePurchaseListener() {
                                @Override
                                public void onProductsLoaded(java.util.List<com.revenuecat.purchases.Package> packages) {
                                    if (packages != null && !packages.isEmpty()) {

                                        com.revenuecat.purchases.Package firstPackage = null;

                                        for (com.revenuecat.purchases.Package pkg : packages) {

                                            String productId = pkg.getProduct().getId();

                                            if (SFS_RefaceItemsID.COINS_75.equals(productId)) {
                                                firstPackage = pkg;
                                                break;
                                            }
                                        }
//                                        com.revenuecat.purchases.Package firstPackage = packages.get(packages.getLast());
//                                        com.revenuecat.purchases.Package firstPackage = packages.get(packages.size() - 1);

                                        if (firstPackage != null) {

                                            com.revenuecat.purchases.Package finalFirstPackage = firstPackage;

                                            activity.runOnUiThread(() -> {
                                                SFS_RefaceRevCatMngr.getInstance().purchasePackage(activity, false, finalFirstPackage, new SFS_RefacePurchaseListener() {
                                                    @Override
                                                    public void onProductsLoaded(java.util.List<com.revenuecat.purchases.Package> packages) {
                                                    }

                                                    @Override
                                                    public void onPurchaseSuccess(String productId) {
                                                        Log.d(TAG, "Auto purchase success: " + productId);
                                                    }

                                                    @Override
                                                    public void onPurchaseError(String message) {
                                                        Log.e(TAG, "Auto purchase error: " + message);
                                                    }

                                                    @Override
                                                    public void onPurchaseCancelled() {
                                                        Log.d(TAG, "Auto purchase cancelled");
                                                        try {
                                                            Intent intent = new Intent(activity, SFS_RefaceCoinStreActivity.class);
                                                            activity.startActivity(intent);
                                                        } catch (Exception e) {
                                                            Log.e(TAG, "Failed to open StoreActivity", e);
                                                        }
                                                    }

                                                    @Override
                                                    public void onPurchaseVerified() {
                                                        Log.d(TAG, "Auto purchase verified successfully");

                                                    }

                                                    @Override
                                                    public void onPurchaseVerificationFailed(String error) {
                                                        Log.e(TAG, "Auto purchase verification failed: " + error);
                                                    }
                                                });
                                            });
                                        } else {
                                            Log.e(TAG, "COINS_75 package not found in offerings.");
                                        }
                                    } else {
                                        Log.w(TAG, "No RevenueCat coin offerings available for auto purchase.");
                                    }
                                }

                                @Override
                                public void onPurchaseSuccess(String productId) {}

                                @Override
                                public void onPurchaseError(String message) {
                                    Log.e(TAG, "Failed to fetch offerings for auto purchase: " + message);
                                }

                                @Override
                                public void onPurchaseCancelled() {}

                                @Override
                                public void onPurchaseVerified() {}

                                @Override
                                public void onPurchaseVerificationFailed(String error) {}
                            });
                        }
                    }

                    @Override
                    public void onDismiss() {
                        // User cancelled
                    }
                }
        );
    }

    /**
     * Checks if coins are required for a specific action based on per-feature flags.
     *
     * @param action LottieLoadingActivity.ACTION_* constant
     * @return true if coins are required
     */
    public static boolean isCoinsRequired(@NonNull String action) {
        String flagKey = ACTION_TO_FLAG.get(action);
        if (flagKey == null) return false;
        return SFS_RefaceAppSystem.isFeatureEnabled(flagKey);
    }

    /**
     * Formats a credit cost value for display.
     *
     * @param cost The cost value
     * @return Formatted string (e.g. "10" or "2.5")
     */
    @NonNull
    public static String formatCost(double cost) {
        if (cost == (long) cost) {
            return String.valueOf((long) cost);
        }
        return String.format(java.util.Locale.US, "%.1f", cost);
    }

    /**
     * Returns a human-readable label for the feature action.
     *
     * @param action LottieLoadingActivity.ACTION_* constant
     * @return Feature display name
     */
    @NonNull
    public static String getFeatureDisplayName(@NonNull String action) {
        switch (action) {
            case SFS_RefaceLoadingActivity.ACTION_FACE_SWAP:      return "Face Swap";
            case SFS_RefaceLoadingActivity.ACTION_AI_IMAGE:       return "AI Image";
            case SFS_RefaceLoadingActivity.ACTION_IMAGE_GENERATE: return "AI Image Generate";
            case SFS_RefaceLoadingActivity.ACTION_MULTI_FACE_SWAP: return "Multi Face Swap";
            case SFS_RefaceLoadingActivity.ACTION_REMOVE_BG:      return "Remove Background";
            case SFS_RefaceLoadingActivity.ACTION_UPSCALE:        return "Upscale Image";
            case SFS_RefaceLoadingActivity.ACTION_COUPLE_SWAP:    return "Couple Face Swap";
            case SFS_RefaceLoadingActivity.ACTION_BG_REPLACE:     return "Replace Background";
            case SFS_RefaceLoadingActivity.ACTION_ENHANCE_GFPGAN: return "Face Enhance";
            case SFS_RefaceLoadingActivity.ACTION_ENHANCE_FACE:   return "Face Enhance";
            case SFS_RefaceLoadingActivity.ACTION_TEXT_TO_IMAGE:   return "Text to Image";
            case SFS_RefaceLoadingActivity.ACTION_VIRTUAL_TRY_ON: return "Virtual Try-On";
            case SFS_RefaceLoadingActivity.ACTION_VIDEO_FACE_SWAP: return "Video Face Swap";
            default: return "AI Feature";
        }
    }
    /**
     * Helper to set feature cost on a simple TextView.
     */
    public static void setupCostLabel(@NonNull String action, @NonNull android.widget.TextView textView) {
        if (!SFS_RefaceAppSystem.isFeatureEnabled(SFS_RefaceAppSystem.KEY_COIN_SYSTEM_ENABLED) || !isCoinsRequired(action)) {
            textView.setVisibility(android.view.View.GONE);
            return;
        }
        double cost = SFS_RefaceSessionMngr.getInstance().getFeatureCost(action);
        if (cost > 0) {
            textView.setText(formatCost(cost) + " Coins");
            textView.setVisibility(android.view.View.VISIBLE);
        } else {
            textView.setVisibility(android.view.View.GONE);
        }
    }

    /**
     * Helper to set feature cost on a complex Badge (LinearLayout with icon + text).
     */
    public static void setupCostBadge(@NonNull String action, @NonNull android.view.View badgeContainer, @NonNull android.widget.TextView textView) {
        if (!SFS_RefaceAppSystem.isFeatureEnabled(SFS_RefaceAppSystem.KEY_COIN_SYSTEM_ENABLED) || !isCoinsRequired(action)) {
            badgeContainer.setVisibility(android.view.View.GONE);
            return;
        }
        double cost = SFS_RefaceSessionMngr.getInstance().getFeatureCost(action);
        if (cost > 0) {
            textView.setText(formatCost(cost) + " Coins");
            badgeContainer.setVisibility(android.view.View.VISIBLE);
        } else {
            badgeContainer.setVisibility(android.view.View.GONE);
        }
    }
}

package com.facechanger.faceswap.enhance.controller;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.AppFaceAppSystem;
import com.facechanger.faceswap.enhance.utils.AppFaceCoinManager;
import com.facechanger.faceswap.enhance.utils.AppFacePhotoTipsPrefs;
import com.facechanger.faceswap.enhance.utils.AppFaceStaticValue;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;

/**
 * Reusable dialog controller for showing custom-styled dialogs.
 * <p>
 * Usage (Coins Required):
 * <pre>
 *   AppDialogController.showCoinsRequiredDialog(
 *       this,
 *       R.drawable.ic_coin,
 *       "Coins Required!",
 *       "Continuing will use 10 coins...",
 *       "Ok, Continue",
 *       new OnDialogActionListener() {
 *           public void onPositiveClick() { /* proceed *\/ }
 *           public void onDismiss() { /* cancelled *\/ }
 *       }
 *   );
 * </pre>
 * <p>
 * Usage (Info With Images):
 * <pre>
 *   AppDialogController.showInfoWithImagesDialog(
 *       this,
 *       R.drawable.img_photo_tip_11,
 *       "Select the right selfies!",
 *       "Pick the selfies that...",
 *       "Ok, Got It",
 *       listener
 *   );
 * </pre>
 */
public final class AppFaceAppDialogController {

    private AppFaceAppDialogController() {
        // Utility class — no instances
    }

    // ──────────────────────────────────────────────
    // Dialog 1: Coins Required
    // ──────────────────────────────────────────────

    /**
     * Shows a "Coins Required" style dialog with an icon, title, message and action button.
     *
     * @param context   Activity context
     * @param iconRes   Drawable resource for the top icon (e.g. R.drawable.ic_coin)
     * @param title     Dialog title text
     * @param message   Dialog description text
     * @param buttonText Positive button label
     * @param listener  Callback for button click and dismiss events (nullable)
     * @return The created Dialog instance for external control if needed
     */
    public static Dialog showCoinsRequiredDialog(
            @NonNull Context context,
            @DrawableRes int iconRes,
            @NonNull String title,
            @NonNull String message,
            @NonNull String buttonText,
            @Nullable AppFaceOnDialogActionListener listener) {

        Dialog dialog = createBaseDialog(context);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.app_coins_required_dialog, null);

        // Bind views
        ImageView ivIcon = view.findViewById(R.id.ivDialogIcon);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        TextView btnPositive = view.findViewById(R.id.btnDialogPositive);
        ImageView btnClose = view.findViewById(R.id.btnDialogClose);

        // Set data
        ivIcon.setImageResource(iconRes);
        tvTitle.setText(title);
        tvMessage.setText(message);
        btnPositive.setText(buttonText);

        // Click handlers
        final boolean[] actionClicked = {false};
        btnPositive.setOnClickListener(v -> {
            actionClicked[0] = true;
            dialog.dismiss();
            if (listener != null) {
                listener.onPositiveClick();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Dismiss callback
        dialog.setOnDismissListener(dialogInterface -> {
            if (listener != null && !actionClicked[0]) {
                listener.onDismiss();
            }
        });

        dialog.setContentView(view);
        setupDialogWindow(dialog);
        safeShowDialog(dialog);

        return dialog;
    }

    // ──────────────────────────────────────────────
    // Dialog: Retry (Network/API Errors)
    // ──────────────────────────────────────────────

    /**
     * Shows a non-cancelable Retry dialog for blocking errors.
     *
     * @param context   Activity context
     * @param iconRes   Drawable resource for the top icon
     * @param title     Dialog title text
     * @param message   Dialog description text
     * @param buttonText Positive button label
     * @param listener  Callback for button click Events
     * @return The created Dialog instance
     */
    public static Dialog showRetryDialog(
            @NonNull Context context,
            @DrawableRes int iconRes,
            @NonNull String title,
            @NonNull String message,
            @NonNull String buttonText,
            @Nullable AppFaceOnDialogActionListener listener) {

        Dialog dialog = createBaseDialog(context);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.app_retry_dialog, null);

        ImageView ivIcon = view.findViewById(R.id.ivDialogIcon);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        TextView btnPositive = view.findViewById(R.id.btnDialogPositive);

        ivIcon.setImageResource(iconRes);
        tvTitle.setText(title);
        tvMessage.setText(message);
        btnPositive.setText(buttonText);

        final boolean[] actionClicked = {false};
        btnPositive.setOnClickListener(v -> {
            actionClicked[0] = true;
            dialog.dismiss();
            if (listener != null) {
                listener.onPositiveClick();
            }
        });

        // Although it is set non-cancelable, handle dismiss callback safely
        dialog.setOnDismissListener(dialogInterface -> {
            if (listener != null && !actionClicked[0]) {
                listener.onDismiss();
            }
        });

        dialog.setContentView(view);
        setupDialogWindow(dialog);
        safeShowDialog(dialog);

        return dialog;
    }

    // ──────────────────────────────────────────────
    // Dialog 2: Info With Images
    // ──────────────────────────────────────────────

    /**
     * Shows an "Info With Images" style dialog with a single image, title, message and action button.
     *
     * @param context    Activity context
     * @param imageRes   Drawable resource for the tip image
     * @param title      Dialog title text
     * @param message    Dialog description text
     * @param buttonText Positive button label
     * @param listener   Callback for button click and dismiss events (nullable)
     * @return The created Dialog instance for external control if needed
     */
    public static Dialog showInfoWithImagesDialog(
            @NonNull Context context,
            @DrawableRes int imageRes,
            @NonNull String title,
            @NonNull String message,
            @NonNull String buttonText,
            @Nullable AppFaceOnDialogActionListener listener) {

        Dialog dialog = createBaseDialog(context);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.app_info_with_images_dialog, null);

        // Bind views
        ImageView ivImage = view.findViewById(R.id.ivDialogImage);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = view.findViewById(R.id.tvDialogMessage);
        TextView btnPositive = view.findViewById(R.id.btnDialogPositive);

        // Set data
        ivImage.setImageResource(imageRes);
        tvTitle.setText(title);
        tvMessage.setText(message);
        btnPositive.setText(buttonText);

        // Click handlers
        final boolean[] actionClicked = {false};
        btnPositive.setOnClickListener(v -> {
            actionClicked[0] = true;
            dialog.dismiss();
            if (listener != null) {
                listener.onPositiveClick();
            }
        });

        // Dismiss callback
        dialog.setOnDismissListener(dialogInterface -> {
            if (listener != null && !actionClicked[0]) {
                listener.onDismiss();
            }
        });

        dialog.setContentView(view);
        setupDialogWindow(dialog);
        safeShowDialog(dialog);

        return dialog;
    }

    // ──────────────────────────────────────────────
    // Photo Tips — One-Time Info Dialog
    // ──────────────────────────────────────────────

    /**
     * Shows the "Photo Tips" info dialog <b>only once</b> — the very first time
     * the user taps any upload button across the entire app.
     * <p>
     * If the user has already seen the dialog (tracked via
     * {@link AppFacePhotoTipsPrefs}), the {@code onProceed} callback fires
     * immediately without any UI.
     * <p>
     * After the dialog is dismissed (button, back press, or outside tap),
     * the upload flow continues via the {@code onProceed} callback.
     *
     * @param context   Activity context (must be alive)
     * @param onProceed Callback that opens the camera/gallery picker
     */
    public static void showPhotoTipsIfNeeded(
            @NonNull Context context,
            @NonNull Runnable onProceed) {

        // Already shown → go straight to picker
        if (AppFacePhotoTipsPrefs.hasShownInfoDialog(context)) {
            onProceed.run();
            return;
        }

        // Mark as shown BEFORE displaying — prevents double-show on quick taps
        // and avoids re-showing if activity is killed while dialog is visible
        AppFacePhotoTipsPrefs.markInfoDialogShown(context);

        showInfoWithImagesDialog(
                context,
                R.drawable.app_face_tip_upload,
                context.getString(R.string.app_info_dialog_title_text),
                context.getString(R.string.app_info_dialog_message_text),
                context.getString(R.string.app_info_dialog_button_text),
                new AppFaceOnDialogActionListener() {
                    @Override
                    public void onPositiveClick() {
                        onProceed.run();
                    }

                    @Override
                    public void onDismiss() {
                        // User dismissed via back or outside tap — still proceed
                        onProceed.run();
                    }
                }
        );
    }

    // ──────────────────────────────────────────────
    // Dialog 3: Permission Denied
    // ──────────────────────────────────────────────

    /**
     * Shows a dark-themed "Permission Denied" dialog with an "Open Settings" button.
     *
     * @param context    Activity context
     * @param title      Dialog title (e.g. "Permission Required")
     * @param message    Detailed message about which permissions are needed
     * @param listener   Callback — onPositiveClick fires when "Open Settings" is tapped
     * @return The created Dialog instance (keep a reference to dismiss from onResume)
     */
    public static Dialog showPermissionDeniedDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @Nullable AppFaceOnDialogActionListener listener) {

        Dialog dialog = createBaseDialog(context);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.app_permission_denied_dialog, null);

        TextView tvTitle = view.findViewById(R.id.tvPermTitle);
        TextView tvMessage = view.findViewById(R.id.tvPermMessage);
        TextView btnOpenSettings = view.findViewById(R.id.btnOpenSettings);
        TextView btnCancel = view.findViewById(R.id.btnCancel);

        tvTitle.setText(title);
        tvMessage.setText(message);

        final boolean[] actionClicked = {false};
        btnOpenSettings.setOnClickListener(v -> {
            actionClicked[0] = true;
            if (listener != null) {
                listener.onPositiveClick();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(dialogInterface -> {
            if (listener != null && !actionClicked[0]) {
                listener.onDismiss();
            }
        });

        dialog.setContentView(view);
        setupDialogWindow(dialog);
        safeShowDialog(dialog);

        return dialog;
    }

    // ──────────────────────────────────────────────
    // Dialog: Server Busy (High Traffic)
    // ──────────────────────────────────────────────

    /**
     * Shows a premium "Server Busy" dialog when API calls fail.
     * <p>
     * The dialog is non-cancelable and provides two actions:
     * <ul>
     *   <li><b>Try Again</b> — retries the failed operation</li>
     *   <li><b>Go Back</b> — navigates back / dismisses</li>
     * </ul>
     *
     * @param context  Activity context
     * @param listener Callback for retry and go-back actions
     * @return The created Dialog instance
     */
    public static Dialog showServerBusyDialog(
            @NonNull Context context, String message,
            @Nullable AppFaceOnServerBusyListener listener) {

        Dialog dialog = createBaseDialog(context);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.app_server_busy_dialog, null);

        TextView btnRetry = view.findViewById(R.id.btnRetry);
        TextView btnGoBack = view.findViewById(R.id.btnGoBack);

        TextView title = view.findViewById(R.id.tvBusyTitle);
        TextView desc = view.findViewById(R.id.tvBusyMessage);

        if (!message.isEmpty()) {
            title.setText("Unable to Process Image");
            desc.setText(message);
        }

        btnRetry.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onRetry();
            }
        });

        btnGoBack.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onGoBack();
            }
        });

        dialog.setOnDismissListener(dialogInterface -> {
            // No-op: dialog is non-cancelable, actions are handled above
        });

        dialog.setContentView(view);
        setupDialogWindow(dialog);
        safeShowDialog(dialog);

        return dialog;
    }

    // ──────────────────────────────────────────────
    // Dialog: Rate App (Option 2)
    // ──────────────────────────────────────────────

    /**
     * Shows a premium interactive rating dialog.
     * Tapping 4 or 5 stars immediately opens the Play Store details page and dismisses.
     * Tapping 1, 2, or 3 stars immediately dismisses the dialog.
     *
     * @param context Activity context
     * @return The created Dialog instance
     */
    public static Dialog showRatingDialog(@NonNull Context context) {
        Dialog dialog = createBaseDialog(context);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.app_rate_app_dialog, null);

        ImageView ivStar1 = view.findViewById(R.id.ivStar1);
        ImageView ivStar2 = view.findViewById(R.id.ivStar2);
        ImageView ivStar3 = view.findViewById(R.id.ivStar3);
        ImageView ivStar4 = view.findViewById(R.id.ivStar4);
        ImageView ivStar5 = view.findViewById(R.id.ivStar5);
        ImageView btnClose = view.findViewById(R.id.btnDialogClose);
        TextView tvFeedbackHint = view.findViewById(R.id.tvFeedbackHint);

        ImageView[] stars = new ImageView[]{ivStar1, ivStar2, ivStar3, ivStar4, ivStar5};

        for (int i = 0; i < stars.length; i++) {
            final int starIndex = i;
            stars[i].setOnClickListener(v -> {
                // Update stars visually
                for (int j = 0; j <= starIndex; j++) {
                    stars[j].setImageResource(R.drawable.app_face_star_center_yellow_filled);
                    animateStar(stars[j]);
                }
                for (int j = starIndex + 1; j < stars.length; j++) {
                    stars[j].setImageResource(R.drawable.app_star_center_empty);
                }

                // Update text hint dynamically
                int rating = starIndex + 1;
                if (rating >= 4) {
                    tvFeedbackHint.setText("Awesome! Opening Play Store...");
                    tvFeedbackHint.setTextColor(Color.parseColor("#4AE3B5")); // Nice dynamic green
                    dialog.dismiss();
                    String packageName = context.getPackageName();
                    try {
                        // 1. Try to open Google Play Store app directly by explicitly targeting its package name
                        android.content.Intent intent = new android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("market://details?id=" + packageName)
                        );
                        intent.setPackage("com.android.vending");
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        try {
                            // 2. If Play Store app is disabled or targeting package fails, try general market deep link
                            android.content.Intent intent = new android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("market://details?id=" + packageName)
                            );
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        } catch (Exception e2) {
                            // 3. Safe fallback to browser
                            android.content.Intent intent = new android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
                            );
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                    }
                } else {
                    tvFeedbackHint.setText(""); // No feedback message for 1, 2, 3
                    v.postDelayed(dialog::dismiss, 350);
                }
            });
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        setupDialogWindow(dialog);
        safeShowDialog(dialog);

        return dialog;
    }

    private static void animateStar(@NonNull View starView) {
        starView.setScaleX(0.7f);
        starView.setScaleY(0.7f);
        starView.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .setDuration(150)
                .withEndAction(() -> starView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start())
                .start();
    }

    // ──────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────

    /**
     * Shows a premium Exit dialog when user presses back on the home screen.
     *
     * @param context  Activity context
     * @param listener Callback for yes (exit) actions
     * @return The created Dialog instance
     */
    public static Dialog showExitDialog(
            @NonNull Context context,
            @Nullable AppFaceOnDialogActionListener listener) {

        Dialog dialog = createBaseDialog(context);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.app_exit_dialog, null);

        TextView btnYes = view.findViewById(R.id.btnExitYes);
        TextView btnNo  = view.findViewById(R.id.btnExitNo);
        ImageView btnClose = view.findViewById(R.id.btnExitClose);

        final boolean[] actionClicked = {false};
        btnYes.setOnClickListener(v -> {
            actionClicked[0] = true;
            dialog.dismiss();
            if (listener != null) {
                listener.onPositiveClick();
            }
        });

        btnNo.setOnClickListener(v -> {
            dialog.dismiss();
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.setOnDismissListener(dialogInterface -> {
            if (listener != null && !actionClicked[0]) {
                listener.onDismiss();
            }
        });

        dialog.setContentView(view);
        setupDialogWindow(dialog);
        safeShowDialog(dialog);

        return dialog;
    }

    /**
     * Creates a base Dialog with no title, transparent background, and cancelable.
     */
    private static Dialog createBaseDialog(@NonNull Context context) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    /**
     * Configures the dialog window — full width, center gravity, transparent background,
     * and dim behind.
     */
    private static void setupDialogWindow(@NonNull Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
        );
        window.setGravity(Gravity.CENTER);

        WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = 0.6f;
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setAttributes(params);
    }

    /**
     * Safely shows a dialog, catching WindowManager exceptions to avoid crashes
     * when an activity is no longer in a valid state to display windows.
     */
    private static void safeShowDialog(@NonNull Dialog dialog) {
        try {
            dialog.show();
        } catch (WindowManager.BadTokenException e) {
            android.util.Log.e("AppDialogController", "Failed to show dialog: Bad Window Token", e);
        } catch (Exception e) {
            android.util.Log.e("AppDialogController", "Error showing dialog", e);
        }
    }

    // ──────────────────────────────────────────────
    // Dialog: Insufficient Coins (Two-Option)
    // ──────────────────────────────────────────────

    /**
     * Shows the "Insufficient Coins" dialog with two action buttons:
     * Buy Coins (navigates to Store) and Watch Ad (shows rewarded ad).
     * Button visibility is controlled by AppSystem feature flags.
     *
     * @param context  Activity context
     * @param cost     Required cost for the feature
     * @param balance  Current user coin balance
     * @param listener Callback for Buy/WatchAd/Dismiss actions
     * @return The created Dialog instance
     */
    public static Dialog showInsufficientCoinsDialog(
            @NonNull Context context,
            double cost,
            double balance,
            @Nullable AppFaceOnCoinDialogListener listener) {

        Dialog dialog = createBaseDialog(context);

        View view = LayoutInflater.from(context)
                .inflate(R.layout.app_insufficient_coins_dialog, null);

        TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvCostInfo = view.findViewById(R.id.tvCostInfo);
        TextView tvBalanceInfo = view.findViewById(R.id.tvBalanceInfo);
        TextView btnBuyCoins = view.findViewById(R.id.btnBuyCoins);
        ImageView btnClose = view.findViewById(R.id.btnDialogClose);

        // Set dynamic content
        // Set dynamic content with proper coin/coins grammar
        String costUnit = (cost == 1) ? "coin" : "coins";
        String balanceUnit = (balance == 1) ? "coin" : "coins";
        tvCostInfo.setText("Required: " + AppFaceCoinManager.formatCost(cost) + " " + costUnit);
        tvBalanceInfo.setText("Your balance: " + AppFaceCoinManager.formatCost(balance) + " " + balanceUnit);

        // Configure button visibility based on AppSystem flags
        boolean showBuy = AppFaceAppSystem.isFeatureEnabled(AppFaceAppSystem.KEY_SHOW_BUY_COINS_OPTION);
        btnBuyCoins.setVisibility(showBuy ? View.VISIBLE : View.GONE);

        Log.e("StaticValue.APP_EXP", "" + GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1));

        if (!AppFaceAppSystem.isFeatureEnabled(AppFaceAppSystem.KEY_SHOW_COINS_DIRECT_PURCHASE)) {
            tvCostInfo.setVisibility(View.VISIBLE);
        } else {
            tvDialogTitle.setText("Not Enough Coins");
            tvCostInfo.setVisibility(View.GONE);
            tvBalanceInfo.setText("You need more coins to use this feature. Get coins now and continue instantly");
        }

        final boolean[] actionClicked = {false};

        btnBuyCoins.setOnClickListener(v -> {
            actionClicked[0] = true;
            dialog.dismiss();
            if (listener != null) listener.onBuyCoins();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(dialogInterface -> {
            if (listener != null && !actionClicked[0]) listener.onDismiss();
        });

        dialog.setContentView(view);
        setupDialogWindow(dialog);
        safeShowDialog(dialog);

        return dialog;
    }
}


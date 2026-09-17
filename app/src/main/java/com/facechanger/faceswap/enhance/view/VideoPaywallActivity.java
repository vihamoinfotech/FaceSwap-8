package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppFaceFacebookEventsManager;
import com.facechanger.faceswap.enhance.controller.AppFacePurchaseListener;
import com.facechanger.faceswap.enhance.controller.AppFaceRevenueCatManager;
import com.facechanger.faceswap.enhance.utils.AppFaceSessionManager;
import com.facechanger.faceswap.enhance.utils.AppFaceStaticValue;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.faceenhance.facechanger.callback.SplashAdCallback;
import com.faceenhance.facechanger.controller.AdManager;
import com.faceenhance.facechanger.controller.FirebaseManager;
import com.faceenhance.facechanger.controller.SplashInterstitialAdManager;
import com.google.android.material.snackbar.Snackbar;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.models.StoreProduct;
import com.revenuecat.purchases.models.SubscriptionOption;

import java.util.List;

public class VideoPaywallActivity extends AppCompatActivity {

    private Boolean isFromSplash = false;
    private ImageView btnClose;
    private TextView btnCta, tv_offer_desc;
    private ProgressBar progressBar;

    private TextView btnRestore, btnPrivacy, btnTerms;

    private com.revenuecat.purchases.Package subscriptionPackage;

    private PlayerView playerView;
    private ExoPlayer exoPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        setContentView(R.layout.activity_video_paywall);

        AppFaceTools.setEdgetoEdge(getWindow(), findViewById(R.id.main), true, true);
        if (getIntent() != null) {
            isFromSplash = getIntent().getBooleanExtra("isFromSplash", false);
        }

        setupVideoBackground();
        setupIDsAndListeners();
        loadOfferings();
    }

    private void setupVideoBackground() {
        playerView = findViewById(R.id.playerViewBackground);
        if (playerView == null) return;

        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        // Mute video
        exoPlayer.setVolume(0f);

        // Loop indefinitely
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);

        // Determine video source for paywall_video.mp4
        Uri videoUri = null;

        // 1. Check raw resource (res/raw/paywall_video.mp4)
        int rawResId = getResources().getIdentifier("paywall_video", "raw", getPackageName());
        if (rawResId != 0) {
            videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + rawResId);
        }

        if (videoUri != null) {
            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayer != null) {
            exoPlayer.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (isFromSplash) {
            Intent intent = new Intent(VideoPaywallActivity.this, AppFaceMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }

    private void setupIDsAndListeners() {

        btnClose = findViewById(R.id.btnPaywallClose);
        btnCta = findViewById(R.id.btnPaywallCta);
        tv_offer_desc = findViewById(R.id.tv_gol_offer_desc);

        btnRestore = findViewById(R.id.btnPaywallRestore);
        btnPrivacy = findViewById(R.id.btnPaywallPrivacy);
        btnTerms = findViewById(R.id.btnPaywallTerms);
        progressBar = findViewById(R.id.paywallProgressBar);



        btnCta.setOnClickListener(v -> {

            if (AdManager.getInstance().isPremiumUser()) {
                onBackPressed();
                return;
            }

            if (subscriptionPackage != null) {
                showLoading(true);
                AppFaceRevenueCatManager.getInstance().purchasePackage(this, true, subscriptionPackage, new AppFacePurchaseListener() {
                    @Override
                    public void onProductsLoaded(List<Package> packages) {}

                    @Override
                    public void onPurchaseSuccess(String productId) {
                        Log.e("purchase", "2");
                        runOnUiThread(() -> {
                            showSnackbar("Verifying purchase...");
                        });
                    }

                    @Override
                    public void onPurchaseError(String message) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            showSnackbar("Purchase failed: " + message);
                        });
                    }

                    @Override
                    public void onPurchaseCancelled() {
                        runOnUiThread(() -> showLoading(false));
                    }

                    @Override
                    public void onPurchaseVerified() {
                        Log.e("purchase", "1");
                        runOnUiThread(() -> {
                            showLoading(false);
                            AdManager.getInstance().setPremiumUser(true);
                            AppFaceSessionManager.getInstance().setPremium(true);
                            logFacebookSubscriptionEvent();
                            btnCta.postDelayed(() -> {
                                Intent intent = new Intent(VideoPaywallActivity.this, AppFaceSplashActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }, 1000);
                        });
                    }

                    @Override
                    public void onPurchaseVerificationFailed(String error) {
                        Log.e("purchase", "3");
                        runOnUiThread(() -> {
                            showLoading(false);
                            showSnackbar("Verification failed: " + error);

                            AdManager.getInstance().setPremiumUser(true);
                            AppFaceSessionManager.getInstance().setPremium(true);
                            logFacebookSubscriptionEvent();
                            btnCta.postDelayed(() -> {

                                FirebaseManager.getInstance().logEvent("PURCHASED_VIDEO_PREMIUM");

                                Intent intent = new Intent(VideoPaywallActivity.this, AppFaceSplashActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }, 1000);

                        });
                    }
                });
            } else {
                showSnackbar("Plan details still loading, please wait...");
            }
        });



        // Restore purchases
        btnRestore.setOnClickListener(v -> {
            restorePurchases();
        });

        // Privacy Policy
        btnPrivacy.setOnClickListener(v -> openUrl("https://resumebuilder-2.blogspot.com/2026/06/privacy-faceswap-2.html"));

        // Terms
        btnTerms.setOnClickListener(v -> openUrl("resumebuilder-2.blogspot.com/2026/06/terms-faceswap-2.html"));

        // Close button
        btnClose.setOnClickListener(v -> onBackPressed());

    }

    private void logFacebookSubscriptionEvent() {
        try {
            if (subscriptionPackage != null) {
                String productId = subscriptionPackage.getProduct().getId();
                double price = subscriptionPackage.getProduct().getPrice().getAmountMicros() / 1000000.0;
                String currencyCode = subscriptionPackage.getProduct().getPrice().getCurrencyCode();
                AppFaceFacebookEventsManager.getInstance().logSubscriptionPurchased(productId, price, currencyCode);
            }
        } catch (Exception e) {
            Log.e("PaywallActivity", "Failed to log Facebook purchase event", e);
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            // No browser available — silently ignore
        }
    }

    private void loadOfferings() {
        showLoading(true);
        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsCallback() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
                runOnUiThread(() -> {
                    showLoading(false);
                    com.revenuecat.purchases.Offering offering = offerings.get(AppFaceRevenueCatManager.VIDEO_OFFERING);
                    if (offering != null && !offering.getAvailablePackages().isEmpty()) {
                        subscriptionPackage = offering.getAvailablePackages().get(0);
                        updatePaywallUI(subscriptionPackage);
                    } else {
                        showSnackbar("Subscription offerings not available");
                    }
                });
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showSnackbar("Failed to load offerings: " + error.getMessage());
                });
            }
        });
    }

    private void restorePurchases() {
        showLoading(true);
        Purchases.getSharedInstance().restorePurchases(new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(@NonNull com.revenuecat.purchases.CustomerInfo customerInfo) {
                runOnUiThread(() -> {
                    showLoading(false);
                    com.revenuecat.purchases.EntitlementInfo entitlement = customerInfo.getEntitlements().get(AppFaceRevenueCatManager.ENTITLEMENT_ID);
                    if (entitlement != null && entitlement.isActive()) {
                        AppFaceSessionManager.getInstance().setPremium(true);
                        AdManager.getInstance().setPremiumUser(true);
                        showSnackbar("Purchases restored successfully!");
                        btnRestore.postDelayed(() -> {
                            Intent intent = new Intent(VideoPaywallActivity.this, AppFaceSplashActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }, 1000);
                    } else {
                        showSnackbar("No active premium subscription found.");
                    }
                });
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showSnackbar("Failed to restore purchases: " + error.getMessage());
                });
            }
        });
    }

    private void updatePaywallUI(com.revenuecat.purchases.Package pkg) {
        if (pkg == null) return;

        StoreProduct product = pkg.getProduct();
        SubscriptionOption defaultOption = product.getDefaultOption();

        if (defaultOption != null) {
            java.util.List<com.revenuecat.purchases.models.PricingPhase> phases = defaultOption.getPricingPhases();
            int trialDays = 0;
            String introPriceFormatted = "";
            String introPeriodIso = "";
            int introCycles = 1;
            String basePriceFormatted = "";
            String basePeriodIso = "";

            if (phases != null && !phases.isEmpty()) {
                // The base recurring phase is the last phase in the subscription option
                com.revenuecat.purchases.models.PricingPhase basePhase = phases.get(phases.size() - 1);
                basePriceFormatted = basePhase.getPrice().getFormatted();
                basePeriodIso = basePhase.getBillingPeriod().getIso8601();

                // Check preceding phases for trial or intro pricing
                for (int i = 0; i < phases.size() - 1; i++) {
                    com.revenuecat.purchases.models.PricingPhase phase = phases.get(i);
                    if (phase.getPrice().getAmountMicros() == 0) {
                        trialDays += parsePeriodToDays(phase.getBillingPeriod().getIso8601());
                    } else {
                        introPriceFormatted = phase.getPrice().getFormatted();
                        introPeriodIso = phase.getBillingPeriod().getIso8601();
                        Integer cycles = phase.getBillingCycleCount();
                        if (cycles != null && cycles > 0) {
                            introCycles = cycles;
                        }
                    }
                }
            }

            String unit = formatPeriodUnit(basePeriodIso);
            int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);

            // Update trial text with dynamic days
            if (trialDays > 0) {
                btnCta.setText("Start Free Trial");
                tv_offer_desc.setText(String.format(java.util.Locale.US, "Try free for %d days, then renews at %s/%s automatically.", trialDays, basePriceFormatted, unit));
            } else if (!introPriceFormatted.isEmpty()) {
                String introPeriodText = formatIntroPeriod(introPeriodIso, introCycles);
                tv_offer_desc.setText(String.format(java.util.Locale.US, "Get full access for %s for %s. It then renews at %s/%s automatically.", introPriceFormatted, introPeriodText, basePriceFormatted, unit));
                if (APP_EXP == 1) {
                    btnCta.setText("Subscribe Now");
                } else {
                    btnCta.setText("Continue");
                }
            } else {
                tv_offer_desc.setText(String.format(java.util.Locale.US, "Enjoy premium features. Your subscription renews automatically at %s/%s.", basePriceFormatted, unit));
                if (APP_EXP == 1) {
                    btnCta.setText("Subscribe Now");
                } else {
                    btnCta.setText("Continue");
                }
            }
        } else {
            int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);
            if (APP_EXP == 1) {
                btnCta.setText("Subscribe Now");
                tv_offer_desc.setText("");
            } else {
                btnCta.setText("Start Free trial");
                tv_offer_desc.setText("");
            }
        }
    }

    private String formatIntroPeriod(String iso8601, int cycleCount) {
        if (iso8601 == null || iso8601.isEmpty()) return "the first period";
        String temp = iso8601.toUpperCase();
        if (temp.startsWith("P")) {
            temp = temp.substring(1);
        }
        int count = Math.max(1, cycleCount);
        if (temp.endsWith("W")) {
            int weeks = 1;
            try {
                weeks = Integer.parseInt(temp.replace("W", ""));
            } catch (Exception ignored) {}
            int totalWeeks = weeks * count;
            return totalWeeks == 1 ? "the first week" : "the first " + totalWeeks + " weeks";
        }
        if (temp.endsWith("M")) {
            int months = 1;
            try {
                months = Integer.parseInt(temp.replace("M", ""));
            } catch (Exception ignored) {}
            int totalMonths = months * count;
            return totalMonths == 1 ? "the first month" : "the first " + totalMonths + " months";
        }
        if (temp.endsWith("Y")) {
            int years = 1;
            try {
                years = Integer.parseInt(temp.replace("Y", ""));
            } catch (Exception ignored) {}
            int totalYears = years * count;
            return totalYears == 1 ? "the first year" : "the first " + totalYears + " years";
        }
        if (temp.endsWith("D")) {
            int days = 1;
            try {
                days = Integer.parseInt(temp.replace("D", ""));
            } catch (Exception ignored) {}
            int totalDays = days * count;
            return totalDays == 1 ? "the first day" : "the first " + totalDays + " days";
        }
        return "the first period";
    }

    private int parsePeriodToDays(String iso8601) {
        if (iso8601 == null || iso8601.isEmpty()) return 0;
        try {
            String temp = iso8601.toUpperCase();
            if (temp.startsWith("P")) {
                temp = temp.substring(1);
            }
            if (temp.endsWith("D")) {
                return Integer.parseInt(temp.replace("D", ""));
            }
            if (temp.endsWith("W")) {
                return Integer.parseInt(temp.replace("W", "")) * 7;
            }
            if (temp.endsWith("M")) {
                return Integer.parseInt(temp.replace("M", "")) * 30;
            }
            if (temp.endsWith("Y")) {
                return Integer.parseInt(temp.replace("Y", "")) * 365;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String formatPeriodUnit(String iso8601) {
        if (iso8601 == null || iso8601.isEmpty()) return "month";
        String temp = iso8601.toUpperCase();
        if (temp.startsWith("P")) {
            temp = temp.substring(1);
        }
        if (temp.endsWith("M")) {
            int count = 1;
            try {
                count = Integer.parseInt(temp.replace("M", ""));
            } catch (Exception ignored) {}
            return count == 1 ? "month" : count + " months";
        }
        if (temp.endsWith("Y")) {
            int count = 1;
            try {
                count = Integer.parseInt(temp.replace("Y", ""));
            } catch (Exception ignored) {}
            return count == 1 ? "year" : count + " years";
        }
        if (temp.endsWith("W")) {
            int count = 1;
            try {
                count = Integer.parseInt(temp.replace("W", ""));
            } catch (Exception ignored) {}
            return count == 1 ? "week" : count + " weeks";
        }
        if (temp.endsWith("D")) {
            int count = 1;
            try {
                count = Integer.parseInt(temp.replace("D", ""));
            } catch (Exception ignored) {}
            return count == 1 ? "day" : count + " days";
        }
        return "month";
    }
    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnCta != null) btnCta.setEnabled(!loading);
        if (btnRestore != null) btnRestore.setEnabled(!loading);
    }

    private void showSnackbar(String message) {
        View root = findViewById(R.id.main);
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }
}
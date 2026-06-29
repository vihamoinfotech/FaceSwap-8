package com.facechanger.faceswap.enhance.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.AppFaceFacebookEventsManager;
import com.facechanger.faceswap.enhance.controller.AppFacePurchaseListener;
import com.facechanger.faceswap.enhance.controller.AppFaceRevenueCatManager;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceSessionManager;
import com.facechanger.faceswap.enhance.utils.AppFaceStaticValue;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.faceenhance.facechanger.controller.AdManager;
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

/**
 * Premium paywall screen — displays feature comparison and trial CTA.
 */
public class AppFacePaywallActivity extends AppCompatActivity {

    private ImageView btnClose;
    private TextView btnCta;
    private TextView btnRestore;
    private TextView btnPrivacy;
    private TextView btnTerms;

    private TextView tvHeroTrial;
    private TextView tvPriceDetail;
    private ProgressBar progressBar;

    private com.revenuecat.purchases.Package subscriptionPackage;

    // ── Animatable sections ──
    private View heroBadge;
    private View heroText;
    private View featurePanel;
    private View footer;
    private TextView tv_gol_offer_title, tv_gol_offer_desc;

    private Boolean isFromSplash = false;

    // ───────────────────────────────── Lifecycle ──

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Force LTR to prevent Google Play strings from getting corrupted in RTL
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        setContentView(R.layout.app_face_activity_paywall_screen);

        AppFaceTools.setEdgetoEdge(getWindow(), findViewById(R.id.paywallRoot), false, true);

        if (getIntent() != null) {
            isFromSplash = getIntent().getBooleanExtra("isFromSplash", false);
        }

        initViews();
        setupListeners();
        playEntranceAnimations();
        loadOfferings();
    }

    // ───────────────────────────────── Init ──

    private void initViews() {
        btnClose = findViewById(R.id.btnPaywallClose);
        btnCta = findViewById(R.id.btnPaywallCta);
        btnRestore = findViewById(R.id.btnPaywallRestore);
        btnPrivacy = findViewById(R.id.btnPaywallPrivacy);
        btnTerms = findViewById(R.id.btnPaywallTerms);

        tvHeroTrial = findViewById(R.id.tvPaywallHeroTrial);
        tvPriceDetail = findViewById(R.id.tvPaywallPriceDetail);
        progressBar = findViewById(R.id.paywallProgressBar);

        heroBadge = findViewById(R.id.paywallHeroBadge);
        heroText = findViewById(R.id.paywallHeroText);
        featurePanel = findViewById(R.id.paywallFeaturePanel);
        footer = findViewById(R.id.paywallFooter);

        tv_gol_offer_title = findViewById(R.id.tv_gol_offer_title);
        tv_gol_offer_desc = findViewById(R.id.tv_gol_offer_desc);

        LinearLayout ll_exclusive_content = findViewById(R.id.ll_exclusive_content);

        int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);

        if (APP_EXP == 1) {

            ll_exclusive_content.setVisibility(View.GONE);
            tvPriceDetail.setVisibility(View.VISIBLE);

            tv_gol_offer_title.setVisibility(View.VISIBLE);
            tv_gol_offer_desc.setVisibility(View.VISIBLE);

            tvPriceDetail.setText("Start Premium Today • Cancel Anytime");

        } else {

            tv_gol_offer_title.setVisibility(View.GONE);
            tv_gol_offer_desc.setVisibility(View.GONE);

            int IS_PRM_PRC_SHOW = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.IS_PRM_PRC_SHOW, 1);

            if (IS_PRM_PRC_SHOW == 1) {
                ll_exclusive_content.setVisibility(View.GONE);
                tvPriceDetail.setVisibility(View.GONE);
            } else {
                ll_exclusive_content.setVisibility(View.VISIBLE);
                tvPriceDetail.setVisibility(View.GONE);
            }
        }


    }

    // ───────────────────────────────── Listeners ──


    @Override
    public void onBackPressed() {
        if (isFromSplash) {
            Intent intent = new Intent(AppFacePaywallActivity.this, AppFaceMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }

    private void setupListeners() {
        // Close button
        btnClose.setOnClickListener(v -> onBackPressed());

        // CTA — purchase package
        btnCta.setOnClickListener(v -> {
            animatePress(v);

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
                                Intent intent = new Intent(AppFacePaywallActivity.this, AppFaceSplashActivity.class);
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
                                Intent intent = new Intent(AppFacePaywallActivity.this, AppFaceSplashActivity.class);
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
            animatePress(v);
            restorePurchases();
        });

        // Privacy Policy
        btnPrivacy.setOnClickListener(v -> openUrl("https://resumebuilder-2.blogspot.com/2026/06/privacy-faceswap-2.html"));

        // Terms
        btnTerms.setOnClickListener(v -> openUrl("resumebuilder-2.blogspot.com/2026/06/terms-faceswap-2.html"));
    }

    // ───────────────────────────────── Animations ──

    /**
     * Staggered entrance animation: each section fades in + slides up
     * with a 120ms stagger for a polished, premium feel.
     */
    private void playEntranceAnimations() {
        View[] sections = {heroBadge, heroText, featurePanel, footer};

        for (int i = 0; i < sections.length; i++) {
            View section = sections[i];
            if (section == null) continue;

            section.setAlpha(0f);
            section.setTranslationY(40f);

            ObjectAnimator alpha = ObjectAnimator.ofFloat(section, "alpha", 0f, 1f);
            ObjectAnimator translateY = ObjectAnimator.ofFloat(section, "translationY", 40f, 0f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(alpha, translateY);
            set.setDuration(450);
            set.setStartDelay(i * 120L);
            set.setInterpolator(new DecelerateInterpolator(1.8f));
            set.start();
        }
    }

    /**
     * Micro press animation on the CTA button for tactile feedback.
     */
    private void animatePress(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(200);
        set.start();
    }

    // ───────────────────────────────── Helpers ──

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            // No browser available — silently ignore
        }
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

    // ── RevenueCat Helpers ──────────────────

    private void loadOfferings() {
        showLoading(true);
        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsCallback() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
                runOnUiThread(() -> {
                    showLoading(false);
                    // Find offering by identifier "sub_offer"
                    com.revenuecat.purchases.Offering offering = offerings.get("sub_offer");
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
                    com.revenuecat.purchases.EntitlementInfo entitlement = customerInfo.getEntitlements().get("faceswap_subscription");
                    if (entitlement != null && entitlement.isActive()) {
                        AppFaceSessionManager.getInstance().setPremium(true);
                        AdManager.getInstance().setPremiumUser(true);
                        showSnackbar("Purchases restored successfully!");
                        btnRestore.postDelayed(() -> {
                            Intent intent = new Intent(AppFacePaywallActivity.this, AppFaceSplashActivity.class);
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
            String basePriceFormatted = "";
            String basePeriodIso = "";

            for (com.revenuecat.purchases.models.PricingPhase phase : phases) {
                boolean isFree = phase.getPrice().getAmountMicros() == 0;
                if (isFree) {
                    trialDays += parsePeriodToDays(phase.getBillingPeriod().getIso8601());
                } else {
                    basePriceFormatted = phase.getPrice().getFormatted();
                    basePeriodIso = phase.getBillingPeriod().getIso8601();
                }
            }

            String unit = formatPeriodUnit(basePeriodIso);


            // Update trial text with dynamic days
            if (trialDays > 0) {

                int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);

                if (APP_EXP == 1) {
                    tvHeroTrial.setText("All Premium Features.");
                    btnCta.setText("START FREE TRIAL");

                    tv_gol_offer_title.setText((String.format(java.util.Locale.US, "%d Days Free trial", trialDays)));
                    tv_gol_offer_desc.setText((String.format(java.util.Locale.US, "Then %s/%s", basePriceFormatted, unit)));

                } else {

                    int IS_PRM_PRC_SHOW = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.IS_PRM_PRC_SHOW, 1);

                    if (IS_PRM_PRC_SHOW == 1) {
                        tvHeroTrial.setText("All Premium Features.");
                        btnCta.setText("START FREE TRIAL");
                    } else {
                        tvHeroTrial.setText(String.format(java.util.Locale.US, "Try free for %d days.", trialDays));
                        btnCta.setText(String.format(java.util.Locale.US, "%d Days Free trial", trialDays));
                    }

                    if (!basePriceFormatted.isEmpty()) {
                        tvPriceDetail.setText(String.format(java.util.Locale.US, "%d Days Free trial, then %s/%s.", trialDays, basePriceFormatted, unit));
                    } else {
                        tvPriceDetail.setVisibility(View.GONE);
                    }
                }
            } else {
                tvHeroTrial.setText("All Premium Features.");
                btnCta.setText("Subscribe Now");

                int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);

                if (APP_EXP == 1) {

                    tv_gol_offer_title.setText("");
                    tv_gol_offer_desc.setText((String.format(java.util.Locale.US, "%s/%s", basePriceFormatted, unit)));

                } else {

                    tvPriceDetail.setVisibility(View.VISIBLE);

                    if (!basePriceFormatted.isEmpty()) {
                        tvPriceDetail.setText(String.format(java.util.Locale.US, "%s/%s", basePriceFormatted, unit));
                    } else {
                        tvPriceDetail.setText("");
                    }
                }
            }
        } else {
            // Fallback for non-sub or no default option: retain the default "3 days" messaging
            String price = product.getPrice().getFormatted();
            tvHeroTrial.setText("Try free for 3 days.");
            btnCta.setText("3 Days Free trial");
            tvPriceDetail.setText(price + "/month after trial");
            tvPriceDetail.setVisibility(View.VISIBLE);
        }
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
        View root = findViewById(R.id.paywallRoot);
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }
}

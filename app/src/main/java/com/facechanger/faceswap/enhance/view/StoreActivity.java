package com.facechanger.faceswap.enhance.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.controller.PurchaseListener;
import com.facechanger.faceswap.enhance.controller.RevenueCatManager;
import com.facechanger.faceswap.enhance.utils.CoinManager;
import com.facechanger.faceswap.enhance.utils.LocaleHelper;
import com.facechanger.faceswap.enhance.utils.SessionManager;
import com.facechanger.faceswap.enhance.utils.Tools;
import com.facechanger.faceswap.enhance.view.adapter.StoreAdapter;
import com.faceenhance.facechanger.controller.FirebaseManager;
import com.google.android.material.snackbar.Snackbar;
import com.revenuecat.purchases.Package;

import java.util.List;

public class StoreActivity extends AppCompatActivity implements PurchaseListener {

    private RecyclerView rvStore;
    private ProgressBar progressBar;
    private View llRetry;
    private TextView btnRetry, tvErrorMessage;
    private TextView tvTotalCoins;
    private ImageView btnBack;
    private TextView btnBuy;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Force LTR layout direction to prevent Google Play strings from getting corrupted in RTL
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        
        setContentView(R.layout.app_face_activity_store_screen);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.root), false);

        initViews();
        setupListeners();
        loadOfferings();
    }

    private void initViews() {
        rvStore = findViewById(R.id.rvStore);
        progressBar = findViewById(R.id.progressBar);
        llRetry = findViewById(R.id.llRetry);
        btnRetry = findViewById(R.id.btnRetry);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        tvTotalCoins = findViewById(R.id.tvTotalCoins);
        btnBack = findViewById(R.id.btnBack);
        
        // Force raw left-pointing icon to prevent ldrtl resources from incorrectly mirroring it
        btnBack.setImageResource(R.drawable.ic_arrow_back_img);
        
        btnBuy = findViewById(R.id.btnBuy);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTotalCoins != null) {
            double balance = SessionManager.getInstance().getCurrentCredits();
            tvTotalCoins.setText(CoinManager.formatCost(balance));
        }
    }

    private void setupListeners() {
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadOfferings());
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnBuy != null) {
            btnBuy.setOnClickListener(v -> {
                animatePress(v);
                if (rvStore.getAdapter() instanceof StoreAdapter) {
                    StoreAdapter adapter = (StoreAdapter) rvStore.getAdapter();
                    Package selectedPackage = adapter.getSelectedPackage();
                    if (selectedPackage != null) {
                        showLoading(true);
                        RevenueCatManager.getInstance().purchasePackage(this, false, selectedPackage, this);
                    } else {
                        showSnackbar("Please select a package first");
                    }
                }
            });
        }
    }

    private void loadOfferings() {
        showLoading(true);
        if (llRetry != null) llRetry.setVisibility(View.GONE);
        RevenueCatManager.getInstance().fetchOfferings(this);
    }

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onProductsLoaded(List<Package> packages) {
        runOnUiThread(() -> {
            showLoading(false);
            if (packages.isEmpty()) {
                if (llRetry != null) llRetry.setVisibility(View.VISIBLE);
                if (tvErrorMessage != null) tvErrorMessage.setText(R.string.app_face_coins_fetching_offerings_error_text);
            } else {
                if (llRetry != null) llRetry.setVisibility(View.GONE);

                com.revenuecat.purchases.Package firstPackage = packages.get(packages.size() - 1);

                rvStore.setLayoutManager(new LinearLayoutManager(this));

                StoreAdapter adapter = new StoreAdapter(packages, packageItem -> {
                    showLoading(true);
                    RevenueCatManager.getInstance().purchasePackage(StoreActivity.this, false, packageItem, StoreActivity.this);
                });
                rvStore.setAdapter(adapter);
            }
        });
    }

    @Override
    public void onPurchaseSuccess(String productId) {
        runOnUiThread(() -> {
            FirebaseManager.getInstance().logEvent("TOKEN_PURCHASED");
            showSnackbar("Verifying purchase with server...");
        });
    }

    @Override
    public void onPurchaseVerified() {
        runOnUiThread(() -> {
            showLoading(false);
            showSnackbar(getString(R.string.app_face_coins_purchase_success_text));
            finish();
        });
    }

    @Override
    public void onPurchaseVerificationFailed(String error) {
        runOnUiThread(() -> {
            showLoading(false);
            showSnackbar("Verification failed: " + error);
        });
    }

    @Override
    public void onPurchaseError(String message) {
        runOnUiThread(() -> {
            showLoading(false);
            if (rvStore.getAdapter() == null || rvStore.getAdapter().getItemCount() == 0) {
                if (llRetry != null) llRetry.setVisibility(View.VISIBLE);
                if (tvErrorMessage != null) tvErrorMessage.setText(getString(R.string.app_coins_purchase_failed_text, message));
            } else {
                showSnackbar(getString(R.string.app_coins_purchase_failed_text, message));
            }
        });
    }

    @Override
    public void onPurchaseCancelled() {
        runOnUiThread(() -> showLoading(false));
    }

    private void animatePress(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(200);
        set.start();
    }

    private void showSnackbar(String message) {
        View content = findViewById(android.R.id.content);
        if (content != null) {
            Snackbar.make(content, message, Snackbar.LENGTH_LONG).show();
        }
    }
}

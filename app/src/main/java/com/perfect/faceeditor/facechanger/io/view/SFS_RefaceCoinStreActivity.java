package com.perfect.faceeditor.facechanger.io.view;

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

import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefacePurchaseListener;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceRevCatMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceCoinMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceSessionMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceUtils;
import com.perfect.faceeditor.facechanger.io.view.adapter.SFS_RefaceCoinStreAdapter;
import com.faceeditor.io.controller.FirebaseManager;
import com.google.android.material.snackbar.Snackbar;
import com.revenuecat.purchases.Package;

import java.util.List;

public class SFS_RefaceCoinStreActivity extends AppCompatActivity implements SFS_RefacePurchaseListener {

    private RecyclerView rvStore;
    private ProgressBar progressBar;
    private View llRetry;
    private TextView btnRetry, tvErrorMessage;
    private TextView tvTotalCoins;
    private ImageView btnBack;
    private TextView btnBuy;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Force LTR layout direction to prevent Google Play strings from getting corrupted in RTL
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        
        setContentView(R.layout.sfs_face_reactv__store_view);
        SFS_RefaceUtils.setStatusBarBleed(getWindow(), findViewById(R.id.root), true);

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
        btnBack.setImageResource(R.drawable.sfs_face_re_arw_back_icon);
        
        btnBuy = findViewById(R.id.btnBuy);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTotalCoins != null) {
            double balance = SFS_RefaceSessionMngr.getInstance().getCurrentCredits();
            tvTotalCoins.setText(SFS_RefaceCoinMngr.formatCost(balance));
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
                if (rvStore.getAdapter() instanceof SFS_RefaceCoinStreAdapter) {
                    SFS_RefaceCoinStreAdapter adapter = (SFS_RefaceCoinStreAdapter) rvStore.getAdapter();
                    Package selectedPackage = adapter.getSelectedPackage();
                    if (selectedPackage != null) {
                        showLoading(true);
                        SFS_RefaceRevCatMngr.getInstance().purchasePackage(this, false, selectedPackage, this);
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
        SFS_RefaceRevCatMngr.getInstance().fetchOfferings(this);
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
                if (tvErrorMessage != null) tvErrorMessage.setText(R.string.sfs_face_re_coins_fetching_offerings_err_text);
            } else {
                if (llRetry != null) llRetry.setVisibility(View.GONE);

                com.revenuecat.purchases.Package firstPackage = packages.get(packages.size() - 1);

                rvStore.setLayoutManager(new LinearLayoutManager(this));

                SFS_RefaceCoinStreAdapter adapter = new SFS_RefaceCoinStreAdapter(packages, packageItem -> {
                    showLoading(true);
                    SFS_RefaceRevCatMngr.getInstance().purchasePackage(SFS_RefaceCoinStreActivity.this, false, packageItem, SFS_RefaceCoinStreActivity.this);
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
            showSnackbar(getString(R.string.sfs_face_re_coins_purchase_success_text));
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
                if (tvErrorMessage != null) tvErrorMessage.setText(getString(R.string.sfs_re_coins_purchase_failed_text, message));
            } else {
                showSnackbar(getString(R.string.sfs_re_coins_purchase_failed_text, message));
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

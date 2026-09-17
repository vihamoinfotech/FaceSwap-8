package com.facechanger.faceswap.enhance.view;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.AppFaceCoinManager;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceSessionManager;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.facechanger.faceswap.enhance.view.adapter.AppFaceCoinHistoryAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the user's coin transaction history.
 * <p>
 * Currently shows the current balance and an empty state.
 * When the server-side transaction history API is available,
 * this will be populated with real data.
 */
public class AppFaceCoinHistoryActivity extends BaseAppActivity {

    private RecyclerView rvTransactions;
    private AppFaceCoinHistoryAdapter adapter;
    private View emptyState;
    private TextView tvHistoryBalance;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_coin_history_activity);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.coinHistoryRoot), true);

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvHistoryBalance = findViewById(R.id.tvHistoryBalance);
        rvTransactions = findViewById(R.id.rvTransactions);
        emptyState = findViewById(R.id.emptyState);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppFaceCoinHistoryAdapter();
        rvTransactions.setAdapter(adapter);

        loadBalance();
        loadTransactions();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBalance();
    }

    private void loadBalance() {
        double balance = AppFaceSessionManager.getInstance().getCurrentCredits();
        tvHistoryBalance.setText(AppFaceCoinManager.formatCost(balance));
    }

    private void loadTransactions() {
        // TODO: Replace with server API call when available.
        // For now, show empty state.
        List<AppFaceCoinHistoryAdapter.Transaction> transactions = new ArrayList<>();

        if (transactions.isEmpty()) {
            rvTransactions.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvTransactions.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            adapter.setTransactions(transactions);
        }
    }
}

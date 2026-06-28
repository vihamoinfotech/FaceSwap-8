package com.facechanger.faceswap.enhance.view;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.CoinManager;
import com.facechanger.faceswap.enhance.utils.SessionManager;
import com.facechanger.faceswap.enhance.utils.Tools;
import com.facechanger.faceswap.enhance.view.adapter.CoinHistoryAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the user's coin transaction history.
 * <p>
 * Currently shows the current balance and an empty state.
 * When the server-side transaction history API is available,
 * this will be populated with real data.
 */
public class CoinHistoryActivity extends BaseAppActivity {

    private RecyclerView rvTransactions;
    private CoinHistoryAdapter adapter;
    private View emptyState;
    private TextView tvHistoryBalance;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_coin_history_activity);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.coinHistoryRoot), false);

        loadAds();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tvHistoryBalance = findViewById(R.id.tvHistoryBalance);
        rvTransactions = findViewById(R.id.rvTransactions);
        emptyState = findViewById(R.id.emptyState);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CoinHistoryAdapter();
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
        double balance = SessionManager.getInstance().getCurrentCredits();
        tvHistoryBalance.setText(CoinManager.formatCost(balance));
    }

    private void loadTransactions() {
        // TODO: Replace with server API call when available.
        // For now, show empty state.
        List<CoinHistoryAdapter.Transaction> transactions = new ArrayList<>();

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

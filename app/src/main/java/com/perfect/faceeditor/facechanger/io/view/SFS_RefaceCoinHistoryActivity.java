package com.perfect.faceeditor.facechanger.io.view;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceCoinMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceSessionMngr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceUtils;
import com.perfect.faceeditor.facechanger.io.view.adapter.SFS_RefaceCoinHistoryAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the user's coin transaction history.
 * <p>
 * Currently shows the current balance and an empty state.
 * When the server-side transaction history API is available,
 * this will be populated with real data.
 */
public class SFS_RefaceCoinHistoryActivity extends SFS_BaseAppActivity {

    private RecyclerView rvTransactions;
    private SFS_RefaceCoinHistoryAdapter adapter;
    private View emptyState;
    private TextView tvHistoryBalance;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sfs_face_re_coin_historyactv_);
        SFS_RefaceUtils.setStatusBarBleed(getWindow(), findViewById(R.id.coinHistoryRoot), true);

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
        adapter = new SFS_RefaceCoinHistoryAdapter();
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
        double balance = SFS_RefaceSessionMngr.getInstance().getCurrentCredits();
        tvHistoryBalance.setText(SFS_RefaceCoinMngr.formatCost(balance));
    }

    private void loadTransactions() {
        // TODO: Replace with server API call when available.
        // For now, show empty state.
        List<SFS_RefaceCoinHistoryAdapter.Transaction> transactions = new ArrayList<>();

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

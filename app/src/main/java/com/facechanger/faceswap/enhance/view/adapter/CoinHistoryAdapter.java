package com.facechanger.faceswap.enhance.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facechanger.faceswap.enhance.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying coin transaction history.
 * <p>
 * Note: Currently shows placeholder/static data.
 * In production, this will be populated from a server API.
 */
public class CoinHistoryAdapter extends RecyclerView.Adapter<CoinHistoryAdapter.TransactionViewHolder> {

    /** Simple data class for a transaction entry. */
    public static class Transaction {
        public final String title;
        public final String date;
        public final double amount;
        public final boolean isCredit;

        public Transaction(String title, String date, double amount, boolean isCredit) {
            this.title = title;
            this.date = date;
            this.amount = amount;
            this.isCredit = isCredit;
        }
    }

    private final List<Transaction> transactions = new ArrayList<>();

    public void setTransactions(@NonNull List<Transaction> items) {
        transactions.clear();
        transactions.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_coin_transaction_item, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction tx = transactions.get(position);
        holder.tvTitle.setText(tx.title);
        holder.tvDate.setText(tx.date);

        if (tx.isCredit) {
            holder.tvAmount.setText("+" + formatAmount(tx.amount));
            holder.tvAmount.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.app_success_color, null));
        } else {
            holder.tvAmount.setText("-" + formatAmount(tx.amount));
            holder.tvAmount.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.app_error_color, null));
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    private String formatAmount(double amount) {
        if (amount == (long) amount) return String.valueOf((long) amount);
        return String.format(java.util.Locale.US, "%.1f", amount);
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvDate;
        final TextView tvAmount;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTransactionTitle);
            tvDate = itemView.findViewById(R.id.tvTransactionDate);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
        }
    }
}

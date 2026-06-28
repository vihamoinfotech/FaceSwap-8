package com.facechanger.faceswap.enhance.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.model.api.HistoryResponse;
import com.facechanger.faceswap.enhance.utils.GlideHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for display history items with a footer
 * loading indicator for pagination.
 */
public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    private final List<HistoryResponse.HistoryItem> items = new ArrayList<>();
    private final Context context;
    private boolean showLoadingFooter = false;
    private OnHistoryItemClickListener clickListener;

    public interface OnHistoryItemClickListener {
        void onItemClick(HistoryResponse.HistoryItem item);
    }

    public HistoryAdapter(@NonNull Context context) {
        this.context = context;
    }

    public void setClickListener(OnHistoryItemClickListener listener) {
        this.clickListener = listener;
    }

    // ── Data management ──────────────────────────────

    /**
     * Appends new items (for pagination).
     */
    public void addItems(@NonNull List<HistoryResponse.HistoryItem> newItems) {
        int startPos = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(startPos, newItems.size());
    }

    /**
     * Replaces all items (for initial load or refresh).
     */
    public void setItems(@NonNull List<HistoryResponse.HistoryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * Clears all items.
     */
    public void clearItems() {
        items.clear();
        notifyDataSetChanged();
    }

    /**
     * Shows/hides the footer loading spinner.
     */
    public void setLoadingFooter(boolean show) {
        if (showLoadingFooter == show) return;
        showLoadingFooter = show;
        if (show) {
            notifyItemInserted(items.size());
        } else {
            notifyItemRemoved(items.size());
        }
    }

    public int getItemCount() {
        return items.size() + (showLoadingFooter ? 1 : 0);
    }

    public int getDataItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (showLoadingFooter && position == items.size()) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_ITEM;
    }

    // ── ViewHolder creation ──────────────────────────

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_LOADING) {
            View view = inflater.inflate(R.layout.item_history_loading, parent, false);
            return new LoadingViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HistoryViewHolder && position < items.size()) {
            ((HistoryViewHolder) holder).bind(items.get(position));
        }
    }

    // ══════════════════════════════════════════════════
    //  History ViewHolder
    // ══════════════════════════════════════════════════

    class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivThumbnail;
        private final TextView tvFeatureType;
        private final TextView tvStatus;
        private final TextView tvDate;
        private final TextView tvCredits;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivHistoryThumbnail);
            tvFeatureType = itemView.findViewById(R.id.tvFeatureType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCredits = itemView.findViewById(R.id.tvCredits);
        }

        void bind(@NonNull HistoryResponse.HistoryItem item) {
            // Feature type
            tvFeatureType.setText(item.getFeatureDisplayName());

            // Status with color
            String statusText = item.getStatus();
            tvStatus.setText(statusText);
            if (item.isSuccess()) {
                tvStatus.setTextColor(0xFF4CAF50); // Green
            } else if ("Failed".equalsIgnoreCase(statusText)) {
                tvStatus.setTextColor(0xFFF44336); // Red
            } else {
                tvStatus.setTextColor(0xFFFF9800); // Orange (Processing, etc.)
            }

            // Date — parse ISO and format nicely
            tvDate.setText(formatDate(item.getCreatedAt()));

            // Credits
            double credits = item.getCreditsConsumed();
            if (credits > 0) {
                tvCredits.setText(String.format(Locale.US, "%.1f", credits));
                tvCredits.setVisibility(View.VISIBLE);
                // Make coin icon visible too (it's in the parent)
                View coinLayout = (View) tvCredits.getParent();
                if (coinLayout != null) coinLayout.setVisibility(View.VISIBLE);
            } else {
                View coinLayout = (View) tvCredits.getParent();
                if (coinLayout != null) coinLayout.setVisibility(View.GONE);
            }

            ImageView ivPlayIcon = itemView.findViewById(R.id.ivPlayIcon);

            // Thumbnail
            if (item.hasValidUrl()) {
                Glide.with(context)
                        .load(GlideHelper.authorizedUrl(item.getResultUrl()))
                        .placeholder(R.color.card_background)
                        .error(R.color.card_background)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(ivThumbnail);

                String urlLower = item.getResultUrl().toLowerCase();
                String pathOnly = urlLower;
                int queryIdx = urlLower.indexOf('?');
                if (queryIdx != -1) {
                    pathOnly = urlLower.substring(0, queryIdx);
                }
                if (pathOnly.endsWith(".mp4") || pathOnly.endsWith(".avi") || pathOnly.endsWith(".mov") || 
                    pathOnly.endsWith(".3gp") || pathOnly.endsWith(".mkv") || pathOnly.endsWith(".webm")) {
                    if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.VISIBLE);
                } else {
                    if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.GONE);
                }
            } else {
                Glide.with(context).clear(ivThumbnail);
                ivThumbnail.setImageResource(R.color.card_background);
                if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item);
                }
            });
        }
    }

    // ══════════════════════════════════════════════════
    //  Loading ViewHolder (footer spinner)
    // ══════════════════════════════════════════════════

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // ══════════════════════════════════════════════════
    //  Utilities
    // ══════════════════════════════════════════════════

    /**
     * Parses an ISO 8601 date string and formats it for display.
     */
    private String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            // Handle ISO 8601 with fractional seconds
            String cleaned = isoDate;
            if (cleaned.contains(".")) {
                // Trim extra fractional digits to 3 (milliseconds)
                int dotIndex = cleaned.indexOf(".");
                int endIndex = cleaned.length();
                // Find where the fraction ends (before T/Z/+/-)
                for (int i = dotIndex + 1; i < cleaned.length(); i++) {
                    char c = cleaned.charAt(i);
                    if (!Character.isDigit(c)) {
                        endIndex = i;
                        break;
                    }
                }
                String fraction = cleaned.substring(dotIndex + 1, endIndex);
                if (fraction.length() > 3) {
                    fraction = fraction.substring(0, 3);
                }
                cleaned = cleaned.substring(0, dotIndex + 1) + fraction +
                        cleaned.substring(endIndex);
            }

            SimpleDateFormat inputFormat;

            if (cleaned.endsWith("Z")) {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            } else if (cleaned.contains("+") || cleaned.lastIndexOf("-") > 10) {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            } else {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
            }

            Date date = inputFormat.parse(cleaned);
            if (date == null) return isoDate;

            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US);
            return outputFormat.format(date);
        } catch (Exception e) {
            // Fallback: return truncated original
            if (isoDate.length() > 16) {
                return isoDate.substring(0, 10) + " " + isoDate.substring(11, 16);
            }
            return isoDate;
        }
    }
}

package com.facechanger.faceswap.enhance.view.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facechanger.faceswap.enhance.R;
import com.revenuecat.purchases.Package;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {

    public interface OnPlanClickListener {
        void onPlanClicked(Package packageItem);
    }

    private final List<Package> packages;
    private final OnPlanClickListener listener;
    private int selectedPosition = -1;
    private int lastAnimatedPosition = -1;

    public StoreAdapter(List<Package> packages, OnPlanClickListener listener) {
        this.listener = listener;
        this.packages = packages;
        if (packages != null && !packages.isEmpty()) {
            this.selectedPosition = Math.max(0, packages.size() - 2);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_plan_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Package packageItem = packages.get(position);

        String planName = packageItem.getProduct().getName();
        String formattedPrice = packageItem.getProduct().getPrice().getFormatted();
        int coins = extractCoinsCount(packageItem);

        String subtitle = coins + " Coins credit into your account.";

        holder.tvDescription.setText(planName);
        holder.tvCoinAmount.setText(subtitle);
        holder.tvPrice.setText(formattedPrice);

        // Apply badge
        applyBadge(holder, position);
        
        // Apply selection style
        applySelectionStyle(holder, position);

        // Selection click listeners
        View.OnClickListener clickListener = v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                if (currentPos != selectedPosition) {
                    int previousSelected = selectedPosition;
                    selectedPosition = currentPos;
                    notifyItemChanged(previousSelected);
                    notifyItemChanged(selectedPosition);
                    animatePress(holder.llPlanRoot);
                }
                if (listener != null) {
                    listener.onPlanClicked(packages.get(currentPos));
                }
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.tvPrice.setOnClickListener(clickListener);

        // Entrance animation
        if (position > lastAnimatedPosition) {
            animateEntrance(holder.itemView, position);
            lastAnimatedPosition = position;
        }
    }

    private void applyBadge(ViewHolder holder, int position) {
        int totalItems = packages.size();

        if (totalItems >= 3 && position == totalItems - 2) {
            // Most Popular badge
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(R.string.app_face_multi_popular_text);
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_popular);
        } else if (totalItems >= 2 && position == totalItems - 1) {
            // Best Value badge
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(R.string.face_app_best_value_text);
            holder.tvBadge.setBackgroundResource(R.drawable.bg_badge_best_value);
        } else {
            // Standard badge hidden
            holder.tvBadge.setVisibility(View.GONE);
        }
    }
    
    private void applySelectionStyle(ViewHolder holder, int position) {
        if (position == selectedPosition) {
            holder.llPlanRoot.setBackgroundResource(R.drawable.bg_plan_card_selected);
        } else {
            holder.llPlanRoot.setBackgroundResource(R.drawable.bg_plan_card_unselected);
        }
    }

    private int extractCoinsCount(Package packageItem) {
        String description = packageItem.getProduct().getDescription();
        String name = packageItem.getProduct().getName();
        
        String textToSearch = (description != null && !description.isEmpty()) ? description : name;
        if (textToSearch != null) {
            Matcher matcher = Pattern.compile("\\d+").matcher(textToSearch);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group());
                } catch (NumberFormatException e) {
                    // Ignore and fallback
                }
            }
        }
        return 1; // Fallback
    }

    private void animateEntrance(View view, int position) {
        view.setAlpha(0f);
        view.setTranslationY(40f);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator translationY = ObjectAnimator.ofFloat(view, "translationY", 40f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, translationY);
        set.setDuration(400);
        set.setStartDelay(position * 80L);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.start();
    }

    private void animatePress(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(200);
        set.start();
    }

    public Package getSelectedPackage() {
        if (selectedPosition >= 0 && selectedPosition < packages.size()) {
            return packages.get(selectedPosition);
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return packages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCoinAmount, tvDescription, tvPrice, tvBadge;
        LinearLayout llPlanRoot;

        ViewHolder(View itemView) {
            super(itemView);
            tvCoinAmount = itemView.findViewById(R.id.tvCoinAmount);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            llPlanRoot = itemView.findViewById(R.id.llPlanRoot);
        }
    }
}

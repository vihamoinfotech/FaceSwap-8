package com.facechanger.faceswap.enhance.view.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facechanger.faceswap.enhance.R;

import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {

    public static class LanguageItem {
        public final String code;
        public final String nativeName;
        public final String englishName;
        public final String flag; // Stores flag emoji e.g. "🇺🇸"

        public LanguageItem(String code, String nativeName, String englishName, String flag) {
            this.code = code;
            this.nativeName = nativeName;
            this.englishName = englishName;
            this.flag = flag;
        }
    }

    public interface OnLanguageClickListener {
        void onLanguageClick(LanguageItem item);
    }

    private final List<LanguageItem> items;
    private final OnLanguageClickListener listener;
    private String selectedCode;

    public LanguageAdapter(List<LanguageItem> items, String selectedCode, OnLanguageClickListener listener) {
        this.items = items;
        this.selectedCode = selectedCode;
        this.listener = listener;
    }

    public String getSelectedCode() {
        return selectedCode;
    }

    public void setSelectedCode(String selectedCode) {
        this.selectedCode = selectedCode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LanguageItem item = items.get(position);
        holder.tvLangFlag.setText(item.flag);
        holder.tvNativeName.setText(item.nativeName);
        holder.tvEnglishName.setText(item.englishName);

        boolean isSelected = item.code.equalsIgnoreCase(selectedCode);

        // Apply Premium Selection Styles
        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.bg_language_item_selected);
            holder.itemView.setAlpha(1.0f);
            holder.viewRadio.setBackgroundResource(R.drawable.bg_radio_selected);
            holder.viewRadio.setImageTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getResources().getColor(R.color.white)));
            holder.flIconContainer.setBackgroundResource(R.drawable.bg_flag_circle_selected);
            // Subtle scale-up for selected item text
            holder.tvNativeName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_language_item_unselected);
            holder.itemView.setAlpha(1.0f);
            holder.viewRadio.setBackgroundResource(R.drawable.bg_radio_unselected);
            holder.viewRadio.setImageTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getResources().getColor(android.R.color.transparent)));
            holder.flIconContainer.setBackgroundResource(R.drawable.bg_flag_circle);
            holder.tvNativeName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
        }

        holder.itemView.setOnClickListener(v -> {
            String previousCode = selectedCode;
            selectedCode = item.code;

            // Animate only affected items for smooth transition
            if (!previousCode.equalsIgnoreCase(item.code)) {
                // Deselect previous
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).code.equalsIgnoreCase(previousCode)) {
                        notifyItemChanged(i);
                        break;
                    }
                }
                // Select current with a subtle scale animation
                notifyItemChanged(holder.getAdapterPosition());
                animateSelection(holder.itemView);
            }

            if (listener != null) {
                listener.onLanguageClick(item);
            }
        });
    }

    /**
     * Plays a subtle scale pulse animation on the selected item
     * for a micro-interaction "pop" feel.
     */
    private void animateSelection(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1.02f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1.02f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(300);
        set.start();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvLangFlag;
        final TextView tvNativeName;
        final TextView tvEnglishName;
        final android.widget.ImageView viewRadio;
        final FrameLayout flIconContainer;

        ViewHolder(View view) {
            super(view);
            tvLangFlag = view.findViewById(R.id.tvLangFlag);
            tvNativeName = view.findViewById(R.id.tvNativeName);
            tvEnglishName = view.findViewById(R.id.tvEnglishName);
            viewRadio = view.findViewById(R.id.viewRadio);
            flIconContainer = view.findViewById(R.id.flIconContainer);
        }
    }
}

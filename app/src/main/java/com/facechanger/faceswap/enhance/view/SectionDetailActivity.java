package com.facechanger.faceswap.enhance.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.model.SectionData;
import com.facechanger.faceswap.enhance.utils.ActivityNavHelper;
import com.facechanger.faceswap.enhance.utils.GlideHelper;
import com.facechanger.faceswap.enhance.utils.Tools;

/**
 * Displays all templates in a category in a 2-column grid.
 * Images are loaded with Bearer auth headers via {@link GlideHelper}.
 */
public class SectionDetailActivity extends BaseAppActivity {

    private SectionData sectionData;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_section_detail_screen);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.sectionDetailContent), false);

        loadAds();

        sectionData = (SectionData) getIntent().getSerializableExtra("section_data");

        setupToolbar();
        setupRecyclerView();
    }

    @Override
    public void onBackPressed() {
        onBackInterstitial(new InterstitialAdCallback() {
            @Override
            public void onAdDismissed() {
                finish();
            }
        });
    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
        if (sectionData != null) {
            TextView tvTitle = findViewById(R.id.tvToolbarTitle);
            tvTitle.setText(sectionData.getTitle());
        }
    }

    private void setupRecyclerView() {
        RecyclerView rvImages = findViewById(R.id.rvImages);
        rvImages.setLayoutManager(new GridLayoutManager(this, 2));

        // Pull securely from TemplateCache instead of reading a massive string array
        // from Intent
        java.util.List<com.facechanger.faceswap.enhance.model.api.TemplateItem> items = com.facechanger.faceswap.enhance.model.TemplateCache
                .getCurrentTemplates();
        if (items == null)
            items = new java.util.ArrayList<>();

        rvImages.setAdapter(new ImageAdapter(items));
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private final java.util.List<com.facechanger.faceswap.enhance.model.api.TemplateItem> items;
        private final String baseUrl = com.facechanger.faceswap.enhance.utils.ApiRepository.getBaseUrl();

        public ImageAdapter(java.util.List<com.facechanger.faceswap.enhance.model.api.TemplateItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_grid_detail_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            com.facechanger.faceswap.enhance.model.api.TemplateItem item = items.get(position);
            String url = item.getImageUrl(baseUrl);
            final int templateId = item.getId();

            // Load with auth header for API template images
            Glide.with(holder.imageView.getContext())
                    .load(GlideHelper.authorizedUrl(url))
                    .placeholder(R.color.app_base_card_background)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(holder.imageView.getContext(), FaceSwapActivity.class);
                intent.putExtra("image_url", url);
                intent.putExtra("template_id", templateId);
                intent.putExtra("is_edit_image", true);
                Activity act = (Activity) holder.imageView.getContext();
                ActivityNavHelper.start(act, intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = (ImageView) itemView;
            }
        }
    }
}

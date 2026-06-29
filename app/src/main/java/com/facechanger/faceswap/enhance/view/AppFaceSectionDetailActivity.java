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
import com.facechanger.faceswap.enhance.model.AppFaceTemplateCache;
import com.facechanger.faceswap.enhance.model.api.AppFaceTemplateItem;
import com.facechanger.faceswap.enhance.utils.AppFaceApiRepository;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.model.AppFaceSectionData;
import com.facechanger.faceswap.enhance.utils.AppFaceActivityNavHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceGlideHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;

/**
 * Displays all templates in a category in a 2-column grid.
 * Images are loaded with Bearer auth headers via {@link AppFaceGlideHelper}.
 */
public class AppFaceSectionDetailActivity extends BaseAppActivity {

    private AppFaceSectionData sectionData;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_section_detail_screen);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.sectionDetailContent), false);

        loadAds();

        sectionData = (AppFaceSectionData) getIntent().getSerializableExtra("section_data");

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
        java.util.List<AppFaceTemplateItem> items = AppFaceTemplateCache
                .getCurrentTemplates();
        if (items == null)
            items = new java.util.ArrayList<>();

        rvImages.setAdapter(new ImageAdapter(items));
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private final java.util.List<AppFaceTemplateItem> items;
        private final String baseUrl = AppFaceApiRepository.getBaseUrl();

        public ImageAdapter(java.util.List<AppFaceTemplateItem> items) {
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
            AppFaceTemplateItem item = items.get(position);
            String url = item.getImageUrl(baseUrl);
            final int templateId = item.getId();

            // Load with auth header for API template images
            Glide.with(holder.imageView.getContext())
                    .load(AppFaceGlideHelper.authorizedUrl(url))
                    .placeholder(R.color.app_base_card_background)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(holder.imageView.getContext(), AppFaceSwapActivity.class);
                intent.putExtra("image_url", url);
                intent.putExtra("template_id", templateId);
                intent.putExtra("is_edit_image", true);
                Activity act = (Activity) holder.imageView.getContext();
                AppFaceActivityNavHelper.start(act, intent);
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

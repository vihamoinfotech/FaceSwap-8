package com.perfect.faceeditor.facechanger.io.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.perfect.faceeditor.facechanger.io.model.SFS_RefacePosterCache;
import com.perfect.faceeditor.facechanger.io.model.api.SFS_RefacePosterItem;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceApiRepo;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.faceeditor.io.callback.InterstitialAdCallback;
import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.model.SFS_RefaceSectionData;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceActivityNaviHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceGlideHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceUtils;

/**
 * Displays all templates in a category in a 2-column grid.
 * Images are loaded with Bearer auth headers via {@link SFS_RefaceGlideHlpr}.
 */
public class SFS_RefaceSectionDetailActivity extends SFS_BaseAppActivity {

    private SFS_RefaceSectionData sectionData;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sfs_face_reactv__section_detail_view);
        SFS_RefaceUtils.setStatusBarBleed(getWindow(), findViewById(R.id.sectionDetailContent), true);

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
            }
        });

        sectionData = (SFS_RefaceSectionData) getIntent().getSerializableExtra("section_data");

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
        java.util.List<SFS_RefacePosterItem> items = SFS_RefacePosterCache
                .getCurrentTemplates();
        if (items == null)
            items = new java.util.ArrayList<>();

        rvImages.setAdapter(new ImageAdapter(items));
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private final java.util.List<SFS_RefacePosterItem> items;
        private final String baseUrl = SFS_RefaceApiRepo.getBaseUrl();

        public ImageAdapter(java.util.List<SFS_RefacePosterItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sfs_re_grid_detail_view, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SFS_RefacePosterItem item = items.get(position);
            String url = item.getImageUrl(baseUrl);
            final int templateId = item.getId();

            // Load with auth header for API template images
            Glide.with(holder.imageView.getContext())
                    .load(SFS_RefaceGlideHlpr.authorizedUrl(url))
                    .placeholder(R.color.sfs_re_base_card_bkg)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> {
                Intent intent = new Intent(holder.imageView.getContext(), SFS_RefaceSwapActivity.class);
                intent.putExtra("image_url", url);
                intent.putExtra("template_id", templateId);
                intent.putExtra("is_edit_image", true);
                Activity act = (Activity) holder.imageView.getContext();
                SFS_RefaceActivityNaviHlpr.start(act, intent);
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

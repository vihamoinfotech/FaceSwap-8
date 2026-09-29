package com.perfect.faceeditor.facechanger.io.view.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.model.SFS_RefaceSectionData;
import com.perfect.faceeditor.facechanger.io.model.SFS_RefacePosterCache;
import com.perfect.faceeditor.facechanger.io.model.api.SFS_RefacePosterCategory;
import com.perfect.faceeditor.facechanger.io.model.api.SFS_RefacePosterItem;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceApiRepo;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceGlideHlpr;
import com.perfect.faceeditor.facechanger.io.view.SFS_RefaceSwapActivity;
import com.perfect.faceeditor.facechanger.io.view.SFS_RefaceSectionDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class SFS_RefaceGalleryCategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private List<SFS_RefacePosterCategory> categories = new ArrayList<>();
    private final String baseUrl = SFS_RefaceApiRepo.getBaseUrl();

    public SFS_RefaceGalleryCategoryAdapter(Context context) {
        this.context = context;
    }

    public void setCategories(List<SFS_RefacePosterCategory> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.sfs_re_category_row_view, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CategoryViewHolder catHolder = (CategoryViewHolder) holder;
        SFS_RefacePosterCategory category = categories.get(position);
        catHolder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {

        final TextView tvTitle;
        final View btnViewAll;
        final ImageView image1;
        final ImageView image2;
        final ImageView image3;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSectionTitle);
            btnViewAll = itemView.findViewById(R.id.btnViewAll);
            image1 = (ImageView) itemView.findViewById(R.id.image1);
            image2 = (ImageView) itemView.findViewById(R.id.image2);
            image3 = (ImageView) itemView.findViewById(R.id.image3);
        }

        void bind(SFS_RefacePosterCategory category) {
            tvTitle.setText(category.getCategoryName());

            btnViewAll.setOnClickListener(v -> {
                SFS_RefacePosterCache.setCurrentTemplates(category.getTemplates());
                SFS_RefaceSectionData sectionData = new SFS_RefaceSectionData(category.getCategoryName(), new ArrayList<>());

                Intent intent = new Intent(context, SFS_RefaceSectionDetailActivity.class);
                intent.putExtra("section_data", sectionData);
                context.startActivity(intent);
            });

            List<SFS_RefacePosterItem> templates = category.getTemplates();
            int previewCount = Math.min(3, templates.size());

            bindImage(image1, templates, 0, previewCount);
            bindImage(image2, templates, 1, previewCount);
            bindImage(image3, templates, 2, previewCount);
        }

        private void bindImage(ImageView imageView, List<SFS_RefacePosterItem> templates, int index, int previewCount) {
            if (index < previewCount) {
                imageView.setVisibility(View.VISIBLE);
                SFS_RefacePosterItem template = templates.get(index);
                String imageUrl = template.getImageUrl(baseUrl);

                Glide.with(context)
                        .load(SFS_RefaceGlideHlpr.authorizedUrl(imageUrl))
                        .placeholder(R.color.sfs_re_base_card_bkg)
                        .error(R.color.sfs_re_base_card_bkg)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(imageView);

                final int templateId = template.getId();
                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, SFS_RefaceSwapActivity.class);
                    intent.putExtra("image_url", imageUrl);
                    intent.putExtra("template_id", templateId);
                    intent.putExtra("is_edit_image", true);
                    intent.putExtra("prompt", template.getPrompt());
                    context.startActivity(intent);
                });
            } else {
                imageView.setVisibility(View.INVISIBLE);
            }
        }
    }
}

package com.facechanger.faceswap.enhance.view.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
import com.facechanger.faceswap.enhance.model.AppFaceSectionData;
import com.facechanger.faceswap.enhance.model.AppFaceTemplateCache;
import com.facechanger.faceswap.enhance.model.api.AppFaceTemplateCategory;
import com.facechanger.faceswap.enhance.model.api.AppFaceTemplateItem;
import com.facechanger.faceswap.enhance.utils.AppFaceApiRepository;
import com.facechanger.faceswap.enhance.utils.AppFaceGlideHelper;
import com.facechanger.faceswap.enhance.view.AppFaceSwapActivity;
import com.facechanger.faceswap.enhance.view.AppFaceSectionDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class AppFaceGalleryCategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private List<AppFaceTemplateCategory> categories = new ArrayList<>();
    private final String baseUrl = AppFaceApiRepository.getBaseUrl();

    public AppFaceGalleryCategoryAdapter(Context context) {
        this.context = context;
    }

    public void setCategories(List<AppFaceTemplateCategory> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.app_category_row_item, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CategoryViewHolder catHolder = (CategoryViewHolder) holder;
        AppFaceTemplateCategory category = categories.get(position);
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

        void bind(AppFaceTemplateCategory category) {
            tvTitle.setText(category.getCategoryName());

            btnViewAll.setOnClickListener(v -> {
                AppFaceTemplateCache.setCurrentTemplates(category.getTemplates());
                AppFaceSectionData sectionData = new AppFaceSectionData(category.getCategoryName(), new ArrayList<>());

                Intent intent = new Intent(context, AppFaceSectionDetailActivity.class);
                intent.putExtra("section_data", sectionData);
                context.startActivity(intent);
            });

            List<AppFaceTemplateItem> templates = category.getTemplates();
            int previewCount = Math.min(3, templates.size());

            bindImage(image1, templates, 0, previewCount);
            bindImage(image2, templates, 1, previewCount);
            bindImage(image3, templates, 2, previewCount);
        }

        private void bindImage(ImageView imageView, List<AppFaceTemplateItem> templates, int index, int previewCount) {
            if (index < previewCount) {
                imageView.setVisibility(View.VISIBLE);
                AppFaceTemplateItem template = templates.get(index);
                String imageUrl = template.getImageUrl(baseUrl);

                Glide.with(context)
                        .load(AppFaceGlideHelper.authorizedUrl(imageUrl))
                        .placeholder(R.color.app_base_card_background)
                        .error(R.color.app_base_card_background)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(imageView);

                final int templateId = template.getId();
                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, AppFaceSwapActivity.class);
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

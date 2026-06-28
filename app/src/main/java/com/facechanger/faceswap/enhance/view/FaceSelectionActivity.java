package com.facechanger.faceswap.enhance.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facechanger.faceswap.enhance.utils.Tools;
import com.facechanger.faceswap.enhance.R;

import java.util.ArrayList;
import java.util.Arrays;

public class FaceSelectionActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_selection);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.faceSelectionContent), false);

        setupToolbar();
        setupRecyclerView();
        setupSystemInsets();
    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        RecyclerView rvFaces = findViewById(R.id.rvFaces);
        rvFaces.setLayoutManager(new GridLayoutManager(this, 3));

        String[] faceUrls = {
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=500&q=80",
                "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=500&q=80",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=500&q=80",
                "https://images.unsplash.com/photo-1554151228-14d9def656e4?w=500&q=80",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&q=80",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&q=80",
                "https://images.unsplash.com/photo-1599566150163-29194dcaad36?w=500&q=80",
                "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=500&q=80",
                "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=500&q=80"
        };

        rvFaces.setAdapter(new FaceAdapter(new ArrayList<>(Arrays.asList(faceUrls))));
    }

    private void setupSystemInsets() {
        View content = findViewById(R.id.faceSelectionContent);
        if (content != null) {
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    private class FaceAdapter extends RecyclerView.Adapter<FaceAdapter.ViewHolder> {
        private final ArrayList<String> imageUrls;

        public FaceAdapter(ArrayList<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_face_grid, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Glide.with(holder.imageView.getContext())
                    .load(imageUrls.get(position))
                    .placeholder(R.color.card_background)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.imageView);

            holder.imageView.setOnClickListener(v -> {
                Toast.makeText(FaceSelectionActivity.this, getString(R.string.FaceSelectionActivity_face_selected), Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
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

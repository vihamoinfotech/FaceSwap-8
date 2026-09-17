package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.facechanger.faceswap.enhance.R;

import java.util.ArrayList;
import java.util.Arrays;

public class AppFaceBackgroundPickerActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_BG_URL = "selected_bg_url";

    private static final String[] BACKGROUND_URLS = {
            "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=600&q=80",
            "https://images.unsplash.com/photo-1518684079-3c830dcef090?w=600&q=80",
            "https://images.unsplash.com/photo-1582672060674-bc2bd808a8b5?w=600&q=80",
            "https://images.unsplash.com/photo-1546412414-e1885259563a?w=600&q=80",
            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&q=80",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&q=80",
            "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=600&q=80",
            "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=600&q=80",
            "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=600&q=80",
            "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=600&q=80"
    };

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.app_slide_up_enter, 0);
        setContentView(R.layout.app_face_background_picker);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(android.R.id.content), true);

        setupToolbar();
        setupRecyclerView();
        setupSystemInsets();
    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvBackgrounds);
        rv.setLayoutManager(new GridLayoutManager(this, 2));
        rv.setAdapter(new BgAdapter(new ArrayList<>(Arrays.asList(BACKGROUND_URLS))));
    }

    private void setupSystemInsets() {
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.app_slide_down_exit);
    }

    // ── Adapter ──────────────────────────────────────

    private class BgAdapter extends RecyclerView.Adapter<BgAdapter.VH> {
        private final ArrayList<String> urls;

        BgAdapter(ArrayList<String> urls) {
            this.urls = urls;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.app_background_grid_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String url = urls.get(position);
            Glide.with(holder.iv.getContext())
                    .load(url)
                    .placeholder(R.color.app_base_card_background)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.iv);

            holder.iv.setOnClickListener(v -> {
                Intent data = new Intent();
                data.putExtra(EXTRA_SELECTED_BG_URL, url);
                setResult(RESULT_OK, data);
                finish();
            });
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView iv;

            VH(@NonNull View itemView) {
                super(itemView);
                iv = (ImageView) itemView;
            }
        }
    }
}

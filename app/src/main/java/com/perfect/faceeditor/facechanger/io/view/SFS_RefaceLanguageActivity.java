package com.perfect.faceeditor.facechanger.io.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLanguagePrfrnce;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceUtils;
import com.perfect.faceeditor.facechanger.io.view.adapter.SFS_RefaceLanguageAdapter;
import com.faceeditor.io.callback.InterstitialAdCallback;

import java.util.ArrayList;
import java.util.List;

public class SFS_RefaceLanguageActivity extends SFS_BaseAppActivity {

    public static final String EXTRA_FROM_SETTINGS = "extra_from_settings";

    private RecyclerView rvLanguages;
    private SFS_RefaceLanguageAdapter adapter;
    private boolean isFromSettings = false;
    private String currentSelectedCode;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply persisted locale wrapper
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sfs_face_reactv__lnguage);
        SFS_RefaceUtils.setStatusBarBleed(getWindow(), findViewById(R.id.root), true);

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
            }
        });

        isFromSettings = getIntent().getBooleanExtra(EXTRA_FROM_SETTINGS, false);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView btnSave = findViewById(R.id.btnSave);
        rvLanguages = findViewById(R.id.rvLanguages);

        // Hide back button on initial onboarding launch to enforce selection
        if (!isFromSettings) {
            btnBack.setVisibility(View.GONE);
        } else {
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // Initialize language options list
        List<SFS_RefaceLanguageAdapter.LanguageItem> languages = new ArrayList<>();
        languages.add(new SFS_RefaceLanguageAdapter.LanguageItem("en", "English", "English", "🇺🇸"));
        languages.add(new SFS_RefaceLanguageAdapter.LanguageItem("in", "Indonesia", "Indonesian", "🇮🇩"));
        languages.add(new SFS_RefaceLanguageAdapter.LanguageItem("es", "Español", "Spanish", "🇪🇸"));
        languages.add(new SFS_RefaceLanguageAdapter.LanguageItem("ar", "العربية", "Arabic", "🇸🇦"));
        languages.add(new SFS_RefaceLanguageAdapter.LanguageItem("fr", "Français", "French", "🇫🇷"));
        languages.add(new SFS_RefaceLanguageAdapter.LanguageItem("pt", "Português", "Portuguese", "🇵🇹"));
        languages.add(new SFS_RefaceLanguageAdapter.LanguageItem("nl", "Nederlands", "Dutch", "🇳🇱"));

        // Get saved or auto-detected code
        currentSelectedCode = SFS_RefaceLocaleHlpr.getSavedLanguage(this);
        // Standardise "id" to "in" for adapter
        if (currentSelectedCode.equalsIgnoreCase("id")) {
            currentSelectedCode = "in";
        }

        adapter = new SFS_RefaceLanguageAdapter(languages, currentSelectedCode, item -> {
            currentSelectedCode = item.code;
        });

        rvLanguages.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        rvLanguages.setAdapter(adapter);

        // Entrance animation for the list
        rvLanguages.setAlpha(0f);
        rvLanguages.setTranslationY(40f);
        rvLanguages.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        btnSave.setOnClickListener(v -> {

            // Persist & apply language
            SFS_RefaceLocaleHlpr.setLocale(this, currentSelectedCode);
            SFS_RefaceLanguagePrfrnce.markLanguageSet(this);

            if (isFromSettings) {
                onClickInterstitial(true, new InterstitialAdCallback() {
                    @Override
                    public void onAdDismissed() {
                        // Return to dashboard and clear stack, SFS_BaseAppActivity will handle RTL layout forcing
                        Intent intent = new Intent(SFS_RefaceLanguageActivity.this, SFS_RefaceMainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            } else {
                // First open flow -> Go to Onboarding Activity
                Intent intent = new Intent(SFS_RefaceLanguageActivity.this, SFS_RefaceIntroingActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.sfs_re_navi_fade_in, R.anim.sfs_re_navi_fade_out);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isFromSettings) {
            onBackInterstitial(new InterstitialAdCallback() {
                @Override
                public void onAdDismissed() {
                    finish();
                }
            });
        } else {
            SFS_RefaceLanguagePrfrnce.markLanguageSet(this);
            Intent intent = new Intent(SFS_RefaceLanguageActivity.this, SFS_RefaceIntroingActivity.class);
            startActivity(intent);
            finish();
        }
    }
}

package com.facechanger.faceswap.enhance.view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.LocaleHelper;
import com.facechanger.faceswap.enhance.utils.LanguagePrefs;
import com.facechanger.faceswap.enhance.utils.Tools;
import com.facechanger.faceswap.enhance.view.adapter.LanguageAdapter;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;

import java.util.ArrayList;
import java.util.List;

public class LanguageActivity extends BaseAppActivity {

    public static final String EXTRA_FROM_SETTINGS = "extra_from_settings";

    private RecyclerView rvLanguages;
    private LanguageAdapter adapter;
    private boolean isFromSettings = false;
    private String currentSelectedCode;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply persisted locale wrapper
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_language);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.root), false);

        // Load premium layout ads automatically via base class
        loadAds();

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
        List<LanguageAdapter.LanguageItem> languages = new ArrayList<>();
        languages.add(new LanguageAdapter.LanguageItem("en", "English", "English", "🇺🇸"));
        languages.add(new LanguageAdapter.LanguageItem("in", "Indonesia", "Indonesian", "🇮🇩"));
        languages.add(new LanguageAdapter.LanguageItem("es", "Español", "Spanish", "🇪🇸"));
        languages.add(new LanguageAdapter.LanguageItem("ar", "العربية", "Arabic", "🇸🇦"));
        languages.add(new LanguageAdapter.LanguageItem("fr", "Français", "French", "🇫🇷"));
        languages.add(new LanguageAdapter.LanguageItem("pt", "Português", "Portuguese", "🇵🇹"));
        languages.add(new LanguageAdapter.LanguageItem("nl", "Nederlands", "Dutch", "🇳🇱"));

        // Get saved or auto-detected code
        currentSelectedCode = LocaleHelper.getSavedLanguage(this);
        // Standardise "id" to "in" for adapter
        if (currentSelectedCode.equalsIgnoreCase("id")) {
            currentSelectedCode = "in";
        }

        adapter = new LanguageAdapter(languages, currentSelectedCode, item -> {
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
            LocaleHelper.setLocale(this, currentSelectedCode);
            LanguagePrefs.markLanguageSet(this);

            if (isFromSettings) {
                onClickInterstitial(true, new InterstitialAdCallback() {
                    @Override
                    public void onAdDismissed() {
                        // Return to dashboard and clear stack, BaseAppActivity will handle RTL layout forcing
                        Intent intent = new Intent(LanguageActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            } else {
                // First open flow -> Go to Onboarding Activity
                Intent intent = new Intent(LanguageActivity.this, OnboardingActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.nav_fade_in, R.anim.nav_fade_out);
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
            LanguagePrefs.markLanguageSet(this);
            Intent intent = new Intent(LanguageActivity.this, OnboardingActivity.class);
            startActivity(intent);
            finish();
        }
    }
}

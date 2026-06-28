package com.facechanger.faceswap.enhance.view;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.facechanger.faceswap.enhance.utils.AppSystem;
import com.facechanger.faceswap.enhance.utils.Tools;
import com.facechanger.faceswap.enhance.R;
import com.faceenhance.facechanger.controller.AdManager;

public class SettingsActivity extends BaseAppActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_settings_screen);
        Tools.setStatusBarBleed(getWindow(), findViewById(R.id.root), false);

        loadAds();

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        findViewById(R.id.llLanguage).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.facechanger.faceswap.enhance.view.LanguageActivity.class);
            intent.putExtra(com.facechanger.faceswap.enhance.view.LanguageActivity.EXTRA_FROM_SETTINGS, true);
            startActivity(intent);
        });

        findViewById(R.id.llFeedback).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@vihamo.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for FaceSwap");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.app_face_no_email_app_found_text), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.llShareApp).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_builder_name));
            String shareMessage = "Check out this amazing app: \nhttps://play.google.com/store/apps/details?id=" + getPackageName();
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        findViewById(R.id.llRateApp).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.app_open_coming_soon_text), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.llTerms).setOnClickListener(v -> openBrowser("https://resumebuilder-2.blogspot.com/2026/06/terms-faceswap-2.html"));

        findViewById(R.id.llPrivacy).setOnClickListener(v -> openBrowser("https://resumebuilder-2.blogspot.com/2026/06/privacy-faceswap-2.html"));

        // Coins Section
        View sectionCoins = findViewById(R.id.sectionCoins);
        View coinsGroup = findViewById(R.id.coinsGroup);

        if (sectionCoins != null && coinsGroup != null) {
            if (AppSystem.isFeatureEnabled(AppSystem.KEY_COIN_SYSTEM_ENABLED)) {
                sectionCoins.setVisibility(View.VISIBLE);
                coinsGroup.setVisibility(View.VISIBLE);

//                findViewById(R.id.llCoinHistory).setOnClickListener(v -> {
//                    startActivity(new Intent(this, CoinHistoryActivity.class));
//                });

                if (AdManager.getInstance().isPremiumUser()) {
                    sectionCoins.setVisibility(View.GONE);
                    coinsGroup.setVisibility(View.GONE);
                } else {
                    sectionCoins.setVisibility(View.VISIBLE);
                    coinsGroup.setVisibility(View.VISIBLE);
                }

                findViewById(R.id.llBuyCoins).setOnClickListener(v -> {
                    startActivity(new Intent(this, PaywallActivity.class));
                });
            } else {
                sectionCoins.setVisibility(View.GONE);
                coinsGroup.setVisibility(View.GONE);
            }
        }
    }

    private void openBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.app_open_no_browser_found_text), Toast.LENGTH_SHORT).show();
        }
    }
}

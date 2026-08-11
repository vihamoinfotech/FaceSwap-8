package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.faceenhance.facechanger.callback.InterstitialAdCallback;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.AppFaceCoinManager;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.faceenhance.facechanger.controller.AdManager;

public class AppFaceTextToImageActivity extends BaseAppActivity {

    private EditText etPrompt;
    private Button btnGenerate;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_text_to_image_screen);
        AppFaceTools.setStatusBarBleed(getWindow(), findViewById(R.id.textToImageContent), false);

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                loadAds();
                loadSecondAds();
            }
        });

        AdManager.getInstance().preloadBigMediaNative();

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        etPrompt    = findViewById(R.id.etPrompt);
        btnGenerate = findViewById(R.id.btnGenerate);

        // Quick prompt chips
        setupPromptChips();

        btnGenerate.setOnClickListener(v -> handleGenerate());
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

    private void setupPromptChips() {
        String[] suggestions = {
                "Portrait of a warrior",
                "Anime character",
                "Fantasy landscape",
                "Cyberpunk city",
                "Cute cartoon avatar",
                "Watercolor painting"
        };

        android.widget.LinearLayout chipRow = findViewById(R.id.chipSuggestions);
        if (chipRow == null) return;

        for (String suggestion : suggestions) {
            android.widget.TextView chip = new android.widget.TextView(this);
            chip.setText(suggestion);
            chip.setTextSize(11f);
            chip.setTextColor(0xFFFFFFFF);
            int padH = dp(12); int padV = dp(7);
            chip.setPadding(padH, padV, padH, padV);
            chip.setBackground(getResources().getDrawable(R.drawable.app_chip_face_unselected_bg, getTheme()));

            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            lp.bottomMargin = dp(8);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                String current = etPrompt.getText().toString().trim();
                if (current.isEmpty()) {
                    etPrompt.setText(suggestion);
                } else {
                    etPrompt.setText(suggestion);
                }
                etPrompt.setSelection(etPrompt.getText().length());
            });

            chipRow.addView(chip);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void handleGenerate() {
        String prompt = etPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, getString(R.string.app_face_please_enter_a_prompt_text), Toast.LENGTH_SHORT).show();
            return;
        }

        AppFaceCoinManager.checkAndProceed(this, AppFaceLoadingActivity.ACTION_TEXT_TO_IMAGE, () -> {
            Intent intent = new Intent(AppFaceTextToImageActivity.this, AppFaceLoadingActivity.class);
            intent.putExtra("action", AppFaceLoadingActivity.ACTION_TEXT_TO_IMAGE);
            intent.putExtra("prompt", prompt);
            startActivity(intent);
        });
    }
}

package com.facechanger.faceswap.enhance.Fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.faceenhance.facechanger.callback.SplashAdCallback;
import com.faceenhance.facechanger.controller.SplashInterstitialAdManager;
import com.faceenhance.facechanger.fragment.BaseAdFragment;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.StaticValue;

public class OnboardFrgOne extends BaseAdFragment {

    private OnButtonClickListener listener;

    private Integer currentIndex;

    public interface OnButtonClickListener {
        void onNextButtonClicked();
    }

    public OnboardFrgOne(OnButtonClickListener listenera, Integer currentIndex) {
        this.listener=listenera;
        this.currentIndex = currentIndex;
    }

    public OnboardFrgOne() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.item_onboarding_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getViews(view);
    }

    public void loadFragAds() {
        int APP_EXP = GlobleMMKVManager.getInstance().getInt(StaticValue.APP_EXP, 1);

        if (APP_EXP == 0) {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()) {
                        if (currentIndex != null && currentIndex == 1) {
                            Log.e("Log___", "Load Ads 1");
                            loadAds();
                            hideSecondAds();
                        } else {
                            Log.e("Log___", "Load Ads 2");
                            hideAds();
                            loadSecondAds();
                        }
                    }
                }
            }, 100);
        } else {
            hideAds();
            hideSecondAds();
        }
    }

    private void getViews(View view) {
        ImageView ivOnboardImage = view.findViewById(R.id.ivOnboardImage);
        TextView tvOnboardTitle = view.findViewById(R.id.tvOnboardTitle);
        TextView tvOnboardSubtitle = view.findViewById(R.id.tvOnboardSubtitle);
        TextView btnContinue = view.findViewById(R.id.btnContinue);
        TextView btnNext = view.findViewById(R.id.btnNext);

        if (currentIndex != null && currentIndex == 1) {

            ivOnboardImage.setImageResource(R.drawable.first_on_board);
            tvOnboardTitle.setText(R.string.onboarding_title_1);
            tvOnboardSubtitle.setText(R.string.onboarding_subtitle_1);
            btnNext.setVisibility(View.GONE);
            btnContinue.setVisibility(View.VISIBLE);

        } else {
            ivOnboardImage.setImageResource(R.drawable.second_on_board);
            tvOnboardTitle.setText(R.string.onboarding_title_2);
            tvOnboardSubtitle.setText(R.string.onboarding_subtitle_2);

            btnNext.setVisibility(View.GONE);
            btnContinue.setVisibility(View.VISIBLE);

//            int APP_EXP = GlobleMMKVManager.getInstance().getInt(StaticValue.APP_EXP, 1);
//
//            if (APP_EXP == 1) {
//                btnNext.setVisibility(View.GONE);
//                btnContinue.setVisibility(View.VISIBLE);
//            } else {
//                btnNext.setVisibility(View.VISIBLE);
//                btnContinue.setVisibility(View.GONE);
//            }
        }

        btnContinue.setText(R.string.onboarding_continue);
        btnNext.setText(R.string.onboarding_continue);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onNextButtonClicked();
                }
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishWithAds();
            }
        });

    }

    void finishWithAds() {
        SplashInterstitialAdManager.getInstance().showSplashAd(getActivity(), false, new SplashAdCallback() {

            @Override
            public void onAdFailed(String s) {
                if (listener != null) {
                    listener.onNextButtonClicked();
                }
            }

            @Override
            public void onAdDismiss() {
                if (listener != null) {
                    listener.onNextButtonClicked();
                }
            }

        });
    }
}
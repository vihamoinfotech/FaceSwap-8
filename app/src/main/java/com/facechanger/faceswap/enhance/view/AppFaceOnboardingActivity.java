package com.facechanger.faceswap.enhance.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.facechanger.faceswap.enhance.utils.AppFaceLanguagePrefs;
import com.facechanger.faceswap.enhance.utils.AppFaceLocaleHelper;
import com.facechanger.faceswap.enhance.utils.AppFaceStaticValue;
import com.facechanger.faceswap.enhance.utils.AppFaceTools;
import com.facechanger.faceswap.enhance.Fragment.AppFaceOnboardFrgOne;
import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.utils.AppFacePrismVaultVPgr;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.faceenhance.facechanger.controller.AdManager;
import com.izooto.iZooto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class AppFaceOnboardingActivity extends BaseAppActivity {

    private AppFacePrismVaultVPgr viewPager;

    private VPagerAdapter viewPagerAdapter;

    private AppFaceOnboardFrgOne fragHelper1 = null;
    private AppFaceOnboardFrgOne fragHelper2 = null;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(AppFaceLocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_face_activity_onboarding_screen);
        AppFaceTools.setEdgetoEdge(getWindow(), findViewById(android.R.id.content), false, false);

        viewPager = findViewById(R.id.viewPager);

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int APP_EXP = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.APP_EXP, 1);
                if (APP_EXP == 0) {
                    loadAds();
                }
                viewPagerSetUp();
            }
        });


        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        iZooto.promptForPushNotifications();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                this.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
//            }
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.e("Permission", "RequestCode = " + requestCode);

        if (requestCode == 101) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                Log.e("iZooto", "Permission Granted");
                iZooto.setSubscription(true);
            } else {
                Log.e("iZooto", "Permission Denied");
            }
        }
    }

    @Override
    public void onBackPressed() {
        changPostion();
    }

    private void viewPagerSetUp() {

        if (viewPagerAdapter != null) return;

        viewPagerAdapter = new VPagerAdapter(getSupportFragmentManager());

        if (fragHelper1 == null) {
            fragHelper1 = new AppFaceOnboardFrgOne(new AppFaceOnboardFrgOne.OnButtonClickListener() {
                @Override
                public void onNextButtonClicked() {
                    changPostion();
                }
            }, 1);

            Bundle bundle = new Bundle();
            bundle.putInt("int_data", 0);
            fragHelper1.setArguments(bundle);
        }

        if (fragHelper2 == null) {
            fragHelper2 = new AppFaceOnboardFrgOne(new AppFaceOnboardFrgOne.OnButtonClickListener() {
                @Override
                public void onNextButtonClicked() {
                    changPostion();
                }
            }, 2);

            Bundle bundle = new Bundle();
            bundle.putInt("int_data", 1);
            fragHelper2.setArguments(bundle);
        }

        viewPagerAdapter.addFragment(fragHelper1, "0");
        viewPagerAdapter.addFragment(fragHelper2, "1");
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setCurrentItem(0);
        viewPager.setOffscreenPageLimit(2);
        viewPagerAdapter.notifyDataSetChanged();

    }

    private void changPostion(){
        int current = viewPager.getCurrentItem();

        if (current < viewPagerAdapter.getCount() - 1) {
            viewPager.setCurrentItem(current + 1, true);

        } else {
            AppFaceLanguagePrefs.markOnboardingCompleted(AppFaceOnboardingActivity.this);
            Class<?> nextActivity;

            if (AdManager.getInstance().isPremiumUser()) {
                nextActivity = AppFaceMainActivity.class;
            } else {
                int IS_IN_APP_AFT_SPL = GlobleMMKVManager.getInstance().getInt(AppFaceStaticValue.IS_IN_APP_AFT_SPL, 0);
                if (IS_IN_APP_AFT_SPL == 1) {
                    nextActivity = AppFacePaywallActivity.class;
                } else {
                    nextActivity = AppFaceMainActivity.class;
                }
            }

            Intent intent = new Intent(AppFaceOnboardingActivity.this, nextActivity);
            intent.putExtra("isFromSplash", true);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.app_nav_fade_in, R.anim.app_nav_fade_out);
        }
    }

    public static class VPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragList = new ArrayList<>();
        private final List<String> mFragTitleList = new ArrayList<>();

        public VPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return mFragList.get(position);
        }

        @Override
        public int getCount() {
            return mFragList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragList.add(fragment);
            mFragTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragTitleList.get(position);
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return super.getItemPosition(object);
        }

    }
}

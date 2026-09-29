package com.perfect.faceeditor.facechanger.io.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLanguagePrfrnce;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceStaticValue;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceUtils;
import com.perfect.faceeditor.facechanger.io.Fragment.SFS_RefaceIntroFrgmentOne;
import com.perfect.faceeditor.facechanger.io.R;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefacePrismVaultVPgr;
import com.faceeditor.io.Utils.GlobleMMKVManager;
import com.faceeditor.io.controller.AdManager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;

public class SFS_RefaceIntroingActivity extends SFS_BaseAppActivity {

    private SFS_RefacePrismVaultVPgr viewPager;

    private VPagerAdapter viewPagerAdapter;

    private SFS_RefaceIntroFrgmentOne fragHelper1 = null;
    private SFS_RefaceIntroFrgmentOne fragHelper2 = null;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sfs_face_reactv__onboarding_view);
        SFS_RefaceUtils.setEdgetoEdge(getWindow(), findViewById(android.R.id.content), true, false);

        viewPager = findViewById(R.id.viewPager);

        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int APP_EXP = GlobleMMKVManager.getInstance().getInt(SFS_RefaceStaticValue.APP_EXP, 1);
                if (APP_EXP == 0) {
                    loadAds();
                }
                viewPagerSetUp();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }


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
            fragHelper1 = new SFS_RefaceIntroFrgmentOne(new SFS_RefaceIntroFrgmentOne.OnButtonClickListener() {
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
            fragHelper2 = new SFS_RefaceIntroFrgmentOne(new SFS_RefaceIntroFrgmentOne.OnButtonClickListener() {
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
            SFS_RefaceLanguagePrfrnce.markOnboardingCompleted(SFS_RefaceIntroingActivity.this);
            Class<?> nextActivity;

            if (AdManager.getInstance().isPremiumUser()) {
                nextActivity = SFS_RefaceMainActivity.class;
            } else {
                int IS_IN_APP_AFT_SPL = GlobleMMKVManager.getInstance().getInt(SFS_RefaceStaticValue.IS_IN_APP_AFT_SPL, 0);
                if (IS_IN_APP_AFT_SPL == 1) {
                    nextActivity = SFS_RefacePremiumActivity.class;
                } else {
                    nextActivity = SFS_RefaceMainActivity.class;
                }
            }

            Intent intent = new Intent(SFS_RefaceIntroingActivity.this, nextActivity);
            intent.putExtra("isFromSplash", true);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.sfs_re_navi_fade_in, R.anim.sfs_re_navi_fade_out);
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

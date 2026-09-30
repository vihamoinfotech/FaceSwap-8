package com.perfect.faceeditor.facechanger.io;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceLocaleHlpr;
import com.perfect.faceeditor.facechanger.io.view.SFS_RefaceLoadingActivity;
import com.perfect.faceeditor.facechanger.io.view.SFS_RefaceSplActivity;
import com.perfect.faceeditor.facechanger.io.view.SFS_RefaceCoinStreActivity;
import com.faceeditor.io.Utils.GlobleMMKVManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.faceeditor.io.controller.AdManager;
import com.faceeditor.io.controller.AppOpenAdManager;
import com.faceeditor.io.model.AdConfig;
import com.perfect.faceeditor.facechanger.io.utils.SFS_RefaceAppSystem;
import com.perfect.faceeditor.facechanger.io.view.SFS_RefaceIntroingActivity;
import com.perfect.faceeditor.facechanger.io.controller.SFS_RefaceRevCatMngr;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.perfect.faceeditor.facechanger.io.worker.SFS_RefaceLocalNotiReceiver;


public class SFS_RefaceSwap extends Application {

    private static final String TAG = "FaceSwap";
    private static SFS_RefaceSwap instance;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(SFS_RefaceLocaleHlpr.onAttach(newBase));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        // Initialize Facebook Events SDK and log App Open
//        SFS_RefaceFacebookEventsMngr.getInstance().init(this);
//        SFS_RefaceFacebookEventsMngr.getInstance().logAppOpen(this);

        // TODO: Reverse Engineering Security Enable Here
        // Anti-reverse engineering security checks
        // SecurityUtils.runComprehensiveSecurityCheck(this);

        if (!SFS_RefaceAppSystem.isDebugMode()) {
            FirebaseApp.initializeApp(this);

            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "FCM token retrieval failed", task.getException());
                            return;
                        }
                        String token = task.getResult();
                        Log.d(TAG, "FCM Token: " + token);
                    });
        }


        createNotificationChannels();

        initializeAdsSdk();

        SFS_RefaceRevCatMngr.getInstance().init((Application) this);

        SFS_RefaceAppSystem.keepScreenOn(true);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(android.app.Activity activity, android.os.Bundle savedInstanceState) {
                activity.getWindow().setFlags(
                        android.view.WindowManager.LayoutParams.FLAG_SECURE,
                        android.view.WindowManager.LayoutParams.FLAG_SECURE);
            }

            @Override
            public void onActivityStarted(android.app.Activity activity) {
            }

            @Override
            public void onActivityResumed(android.app.Activity activity) {
            }

            @Override
            public void onActivityPaused(android.app.Activity activity) {
            }

            @Override
            public void onActivityStopped(android.app.Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(android.app.Activity activity, android.os.Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(android.app.Activity activity) {
            }
        });

        // Track application background state for local notification
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_STOP) {

                    int local_notif_scheduled = GlobleMMKVManager.getInstance().getInt("local_notif_scheduled", 0);

                    // Allow repeated testing in debug mode, otherwise only run once
                    if (local_notif_scheduled == 0) {

                        GlobleMMKVManager.getInstance().putInt("local_notif_scheduled", 1);

                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        Intent intent = new Intent(SFS_RefaceSwap.this, SFS_RefaceLocalNotiReceiver.class);

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(SFS_RefaceSwap.this, 1001, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                        long triggerAtMillis = System.currentTimeMillis() + 60 * 1000; // 1 minute

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                        }
                    }
                }
            }
        });
    }

    public static SFS_RefaceSwap getInstance() {
        return instance;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    private void initializeAdsSdk() {

        Typeface manropeRegular = ResourcesCompat.getFont(this, R.font.sfs_re_manrope_regular);
        Typeface manropeMedium = ResourcesCompat.getFont(this, R.font.sfs_re_manrope_medium);
        Typeface manropeSemibold = ResourcesCompat.getFont(this, R.font.sfs_re_manrope_semibold);

        // Configure AD SDK with custom styling
        AdConfig config = new AdConfig.Builder()
                .muteVideoAds(true)
                .fontRegular(manropeRegular)
                .fontMedium(manropeMedium)
                .fontBold(manropeSemibold)
                .build();

        // Initialize once
        AdManager.getInstance().init(this, config);

        AppOpenAdManager.getInstance().addExcludedActivity(SFS_RefaceIntroingActivity.class);
        AppOpenAdManager.getInstance().addExcludedActivity(SFS_RefaceSplActivity.class);
        AppOpenAdManager.getInstance().addExcludedActivity(SFS_RefaceCoinStreActivity.class);
        AppOpenAdManager.getInstance().addExcludedActivity(SFS_RefaceLoadingActivity.class);

        if (SFS_RefaceAppSystem.isDebugMode()) {
            AdManager.getInstance().setDebugMode(true);
        } else {
            AdManager.getInstance().setDebugMode(false);
        }
    }

    /**
     * Pre-creates notification channels at app startup.
     * Required for Android 8.0+ — channels must exist before any notification
     * (including background FCM messages) tries to use them.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Channel for Firebase push notifications
            NotificationChannel pushChannel = new NotificationChannel(
                    "faceswap_push_channel",
                    "FaceSwap Push Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            pushChannel.setDescription("Notifications from FaceSwap app");
            manager.createNotificationChannel(pushChannel);

            // Channel for local reminder notifications
            NotificationChannel localChannel = new NotificationChannel(
                    "faceswap_local_channel",
                    "FaceSwap Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            localChannel.setDescription("Reminders from FaceSwap app");
            manager.createNotificationChannel(localChannel);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        AdManager.getInstance().onTrimMemory(level);
    }
}

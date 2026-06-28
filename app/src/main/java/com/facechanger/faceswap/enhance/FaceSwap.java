package com.facechanger.faceswap.enhance;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;

import androidx.core.content.res.ResourcesCompat;

import com.facechanger.faceswap.enhance.view.LottieLoadingActivity;
import com.facechanger.faceswap.enhance.view.SplashActivity;
import com.facechanger.faceswap.enhance.view.StoreActivity;
import com.faceenhance.facechanger.Utils.GlobleMMKVManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.faceenhance.facechanger.controller.AdManager;
import com.faceenhance.facechanger.controller.AppOpenAdManager;
import com.faceenhance.facechanger.model.AdConfig;
import com.facechanger.faceswap.enhance.utils.AppSystem;
import com.facechanger.faceswap.enhance.view.OnboardingActivity;
import com.facechanger.faceswap.enhance.controller.RevenueCatManager;

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
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import com.facechanger.faceswap.enhance.worker.LocalNotificationReceiver;
import com.facechanger.faceswap.enhance.utils.SecurityUtils;
import com.facechanger.faceswap.enhance.controller.FacebookEventsManager;

public class FaceSwap extends Application {

    private static final String TAG = "FaceSwap";
    private static FaceSwap instance;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.facechanger.faceswap.enhance.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        // Initialize Facebook Events SDK and log App Open
        FacebookEventsManager.getInstance().init(this);
        FacebookEventsManager.getInstance().logAppOpen(this);

        // TODO: Reverse Engineering Security Enable Here
        // Anti-reverse engineering security checks
        // SecurityUtils.runComprehensiveSecurityCheck(this);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Retrieve and log the FCM token for testing push notifications
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "FCM token retrieval failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "════════════════════════════════════════");
                    Log.d(TAG, "FCM Device Token: " + token);
                    Log.d(TAG, "════════════════════════════════════════");
                });

        // Pre-create notification channels (must exist before background FCM messages
        // arrive)
        createNotificationChannels();

        // Initialize Ads SDK here
        initializeAdsSdk();

        // Initialize RevenueCat SDK
        RevenueCatManager.getInstance().init((Application) this);

        // Keep screen always on throughout the app
        AppSystem.keepScreenOn(true);

        // Globally restrict screenshots for all activities
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
                        Intent intent = new Intent(FaceSwap.this, LocalNotificationReceiver.class);

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(FaceSwap.this, 1001, intent,
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

    public static FaceSwap getInstance() {
        return instance;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    private void initializeAdsSdk() {

        Typeface poppinsReg = ResourcesCompat.getFont(this, R.font.poppins_regular);
        Typeface poppinsMed = ResourcesCompat.getFont(this, R.font.poppins_medium);
        Typeface poppinsBold = ResourcesCompat.getFont(this, R.font.poppins_semibold);

        // Configure AD SDK with custom styling
        AdConfig config = new AdConfig.Builder()
                .muteVideoAds(true)
                .fontRegular(poppinsReg)
                .fontMedium(poppinsMed)
                .fontBold(poppinsBold)
                .build();

        // Initialize once
        AdManager.getInstance().init(this, config);

        AppOpenAdManager.getInstance().addExcludedActivity(OnboardingActivity.class);
        AppOpenAdManager.getInstance().addExcludedActivity(SplashActivity.class);
        AppOpenAdManager.getInstance().addExcludedActivity(StoreActivity.class);
        AppOpenAdManager.getInstance().addExcludedActivity(LottieLoadingActivity.class);

        if (AppSystem.isDebugMode()) {
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
package com.facechanger.faceswap.enhance.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.view.SplashActivity;

/**
 * Fires the one-time local notification ~1 minute after the user
 * first backgrounds the app. Triggered via {@link android.app.AlarmManager}.
 */
public class LocalNotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "faceswap_local_channel";
    private static final String CHANNEL_NAME = "FaceSwap Reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(
                context,
                "Your FaceSwap is Ready 🎭",
                "Your FaceSwap just finished processing. Tap to reveal the transformation!"
        );
    }

    private void showNotification(Context context, String title, String message) {
        // Create channel first
        createNotificationChannel(context);

        Intent activityIntent = new Intent(context, SplashActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, activityIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        // Large icon = full-color app icon (shown in expanded notification)
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_noti_face)       // Monochromatic silhouette
                .setLargeIcon(largeIcon)                        // Full-color app icon
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1001, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders from FaceSwap app");

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }
}

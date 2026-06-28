package com.facechanger.faceswap.enhance.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.facechanger.faceswap.enhance.R;
import com.facechanger.faceswap.enhance.view.SplashActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Handles Firebase Cloud Messaging (FCM) push notifications.
 * <p>
 * When the app is in the <b>foreground</b>, Android does NOT auto-display
 * the notification — {@link #onMessageReceived} fires and we build it manually.
 * <p>
 * When the app is in the <b>background / killed</b>, the system tray
 * auto-displays notification-payload messages. Only data-payload-only
 * messages reach {@link #onMessageReceived} while backgrounded.
 * <p>
 * To cover BOTH scenarios (foreground + background), this service handles
 * notification payloads, data payloads, and combined payloads.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM_Service";
    private static final String CHANNEL_ID = "faceswap_push_channel";
    private static final String CHANNEL_NAME = "FaceSwap Push Notifications";

    // ─────────────────────────────────────────────
    //  Message Received
    // ─────────────────────────────────────────────

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "─── FCM Message Received ───");
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message ID: " + remoteMessage.getMessageId());

        String title = null;
        String body = null;

        // 1. Try notification payload first (Firebase Console sends this)
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notification payload — title: " + title + ", body: " + body);
        }

        // 2. Try data payload (backend API sends this)
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            Log.d(TAG, "Data payload: " + data);
            // Data payload can override or supplement notification fields
            if (data.containsKey("title")) {
                title = data.get("title");
            }
            if (data.containsKey("body")) {
                body = data.get("body");
            }
            if (data.containsKey("message")) {
                body = data.get("message");
            }
        }

        // 3. If we have something to show, build the notification
        if (title != null || body != null) {
            showNotification(
                    title != null ? title : getString(R.string.app_builder_name),
                    body != null ? body : ""
            );
        }
    }

    // ─────────────────────────────────────────────
    //  Token Refresh
    // ─────────────────────────────────────────────

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "════════════════════════════════════════");
        Log.d(TAG, "FCM Token Refreshed: " + token);
        Log.d(TAG, "════════════════════════════════════════");
        // TODO: Send this token to your backend server if needed
    }

    // ─────────────────────────────────────────────
    //  Build & Show Notification
    // ─────────────────────────────────────────────

    private void showNotification(String title, String messageBody) {
        // Create the notification channel (required for Android 8.0+)
        createNotificationChannel();

        // Tap action → opens the app
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        // Large icon = full-color app icon (shown in expanded notification)
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_noti_face)       // Monochromatic silhouette
                .setLargeIcon(largeIcon)                        // Full-color app icon
                .setContentTitle(title)
                .setContentText(messageBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications from FaceSwap app");

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }
}

package com.bro.barbershop.utils.messagingService;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.bro.barbershop.R;
import com.bro.barbershop.SplashScreenActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.UUID;

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class BarberShopMessagingService extends FirebaseMessagingService {

    public static final String NOTIFICATION_URL = "https://fcm.googleapis.com/fcm/send";
    public static final String SERVER_KEY = "AAAAw69xLx8:APA91bEnnB3FgtqFo8okV1RH8AIk1eYFVSo8zCl867dljGYkmIey2RN5ZlHyIyOBTuqRcYw8bLkkAR0LxAQRjnJy4EpSaRtZPkqAyYRmF5sCQuiwHNmNExdGdPbW_IEIpwfXlVRj97O6";
    private static final CharSequence CHANNEL_NAME = "barber_shop_bro";
    private static final String TAG = "BarberShopMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            Map<String, String> map = remoteMessage.getData();
            String title = map.get("title");
            String message = map.get("message");

            sendNotification(title, message);
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void sendNotification(String title, String message) {
        String notificationId = UUID.randomUUID().toString();

        Intent intent = new Intent(this, SplashScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, title)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(title, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_NAME.toString());
            builder.setChannelId(title);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(notificationId.hashCode(), notification);
        }

    }
}

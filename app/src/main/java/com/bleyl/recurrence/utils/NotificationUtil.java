package com.bleyl.recurrence.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.bleyl.recurrence.R;
import com.bleyl.recurrence.activities.MainActivity;
import com.bleyl.recurrence.activities.ViewActivity;
import com.bleyl.recurrence.models.Reminder;
import com.bleyl.recurrence.receivers.DismissReceiver;
import com.bleyl.recurrence.receivers.NagReceiver;

import java.util.Calendar;

public class NotificationUtil {

    private static final String CHANNEL_ID = "recurrence_reminders";

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            String soundUri = prefs.getString("NotificationSound", "content://settings/system/notification_sound");
            Uri sound = (soundUri == null || soundUri.isEmpty()) ? null : Uri.parse(soundUri);

            long[] vibrationPattern = prefs.getBoolean("checkBoxVibrate", true) ? new long[]{0, 300, 0} : null;

            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableLights(prefs.getBoolean("checkBoxLED", true));
            channel.setLightColor(Color.BLUE);
            if (sound != null) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
                channel.setSound(sound, audioAttributes);
            }
            if (vibrationPattern != null) {
                channel.enableVibration(true);
                channel.setVibrationPattern(vibrationPattern);
            }

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    public static void createNotification(Context context, Reminder reminder) {
        createNotificationChannel(context);

        // Create intent for notification onClick behaviour
        Intent viewIntent = new Intent(context, ViewActivity.class);
        viewIntent.putExtra("NOTIFICATION_ID", reminder.getId());
        viewIntent.putExtra("NOTIFICATION_DISMISS", true);
        PendingIntent pending = PendingIntent.getActivity(context, reminder.getId(), viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create intent for notification snooze click behaviour — direct activity PendingIntent
        // so the system can reliably bring MainActivity to the foreground from any state.
        Intent snoozeIntent = new Intent(context, MainActivity.class);
        snoozeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        snoozeIntent.putExtra("NOTIFICATION_ID", reminder.getId());
        snoozeIntent.putExtra("SHOW_SNOOZE_DIALOG", true);
        PendingIntent pendingSnooze = PendingIntent.getActivity(context, reminder.getId(), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int imageResId = context.getResources().getIdentifier(reminder.getIcon(), "drawable", context.getPackageName());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(imageResId)
                .setColor(Color.parseColor(reminder.getColour()))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(reminder.getContent()))
                .setContentTitle(reminder.getTitle())
                .setContentText(reminder.getContent())
                .setTicker(reminder.getTitle())
                .setContentIntent(pending);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.getBoolean("checkBoxNagging", false)) {
            Intent swipeIntent = new Intent(context, DismissReceiver.class);
            swipeIntent.putExtra("NOTIFICATION_ID", reminder.getId());
            PendingIntent pendingDismiss = PendingIntent.getBroadcast(context, reminder.getId(), swipeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.setDeleteIntent(pendingDismiss);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, sharedPreferences.getInt("nagMinutes", context.getResources().getInteger(R.integer.default_nag_minutes)));
            calendar.add(Calendar.SECOND, sharedPreferences.getInt("nagSeconds", context.getResources().getInteger(R.integer.default_nag_seconds)));
            Intent alarmIntent = new Intent(context, NagReceiver.class);
            AlarmUtil.setAlarm(context, alarmIntent, reminder.getId(), calendar);
        }


        if (sharedPreferences.getBoolean("checkBoxSnooze", false)) {
            builder.addAction(R.drawable.ic_snooze_white_24dp, context.getString(R.string.snooze), pendingSnooze);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(reminder.getId(), builder.build());
    }

    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }
}

package com.bleyl.recurrence.receivers;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.preference.PreferenceManager;

import com.bleyl.recurrence.activities.MainActivity;
import com.bleyl.recurrence.utils.AlarmUtil;

public class SnoozeActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int reminderId = intent.getIntExtra("NOTIFICATION_ID", 0);

        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("checkBoxNagging", false)) {
            Intent alarmIntent = new Intent(context, NagReceiver.class);
            AlarmUtil.cancelAlarm(context, alarmIntent, reminderId);
        }

        Intent snoozeIntent = new Intent(context, MainActivity.class);
        snoozeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        snoozeIntent.putExtra("NOTIFICATION_ID", reminderId);
        snoozeIntent.putExtra("SHOW_SNOOZE_DIALOG", true);
        // Suppress the activity enter animation so the dialog appears without a background flash
        ActivityOptions options = ActivityOptions.makeCustomAnimation(context, 0, 0);
        context.startActivity(snoozeIntent, options.toBundle());
    }
}
package com.bleyl.recurrence.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.bleyl.recurrence.R;
import com.bleyl.recurrence.adapters.ReminderAdapter;
import com.bleyl.recurrence.adapters.ViewPageAdapter;
import com.bleyl.recurrence.receivers.SnoozeReceiver;
import com.bleyl.recurrence.utils.AlarmUtil;
import com.bleyl.recurrence.utils.NotificationUtil;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements ReminderAdapter.RecyclerListener {

    @BindView(R.id.tabs) TabLayout tabLayout;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.viewpager) ViewPager viewPager;
    @BindView(R.id.fab_button) FloatingActionButton floatingActionButton;

    private boolean fabIsHidden = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(null);
        }

        ViewPageAdapter adapter = new ViewPageAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.selector_icon_active);
        tabLayout.getTabAt(1).setIcon(R.drawable.selector_icon_inactive);

        requestRequiredPermissions();
        checkForSnoozeIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkForSnoozeIntent(intent);
    }

    private void checkForSnoozeIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("SHOW_SNOOZE_DIALOG", false)) {
            // Clear the flag so rotation/resume doesn't re-show the dialog
            intent.removeExtra("SHOW_SNOOZE_DIALOG");
            final int reminderId = intent.getIntExtra("NOTIFICATION_ID", 0);
            // Cancel nag alarm if nagging is enabled (previously done in SnoozeActionReceiver)
            if (PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("checkBoxNagging", false)) {
                Intent alarmIntent = new Intent(this, com.bleyl.recurrence.receivers.NagReceiver.class);
                AlarmUtil.cancelAlarm(this, alarmIntent, reminderId);
            }
            showSnoozeDialog(reminderId);
        }
    }

    private void showSnoozeDialog(final int reminderId) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        View view = getLayoutInflater().inflate(R.layout.number_picker, null);

        final NumberPicker hourPicker = view.findViewById(R.id.picker1);
        final NumberPicker minutePicker = view.findViewById(R.id.picker2);

        // Hour picker 0–24
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(24);
        hourPicker.setValue(prefs.getInt("snoozeHours", getResources().getInteger(R.integer.default_snooze_hours)));
        String[] hourValues = new String[25];
        for (int i = 0; i < hourValues.length; i++) {
            hourValues[i] = String.format(getResources().getQuantityString(R.plurals.time_hour, i), i);
        }
        hourPicker.setDisplayedValues(hourValues);

        // Minute picker 0–60
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(60);
        minutePicker.setValue(prefs.getInt("snoozeMinutes", getResources().getInteger(R.integer.default_snooze_minutes)));
        String[] minuteValues = new String[61];
        for (int i = 0; i < minuteValues.length; i++) {
            minuteValues[i] = String.format(getResources().getQuantityString(R.plurals.time_minute, i), i);
        }
        minutePicker.setDisplayedValues(minuteValues);

        new AlertDialog.Builder(this, R.style.Dialog)
            .setTitle(R.string.snooze_length)
            .setView(view)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (hourPicker.getValue() != 0 || minutePicker.getValue() != 0) {
                        NotificationUtil.cancelNotification(getApplicationContext(), reminderId);
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.MINUTE, minutePicker.getValue());
                        calendar.add(Calendar.HOUR, hourPicker.getValue());
                        Intent alarmIntent = new Intent(getApplicationContext(), SnoozeReceiver.class);
                        AlarmUtil.setAlarm(getApplicationContext(), alarmIntent, reminderId, calendar);
                        prefs.edit()
                            .putInt("snoozeHours", hourPicker.getValue())
                            .putInt("snoozeMinutes", minutePicker.getValue())
                            .apply();
                    }
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void requestRequiredPermissions() {
        // POST_NOTIFICATIONS — required on API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // SCHEDULE_EXACT_ALARM — direct user to system settings if not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    @OnClick(R.id.fab_button)
    public void fabClicked() {
        Intent intent = new Intent(this, CreateEditActivity.class);
        startActivity(intent);
    }

    @Override
    public void hideFab() {
        floatingActionButton.hide();
        fabIsHidden = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fabIsHidden) {
            floatingActionButton.show();
            fabIsHidden = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent preferenceIntent = new Intent(this, PreferenceActivity.class);
                startActivity(preferenceIntent);
                return true;
            case R.id.action_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

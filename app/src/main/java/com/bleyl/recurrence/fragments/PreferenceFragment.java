package com.bleyl.recurrence.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.bleyl.recurrence.R;
import com.bleyl.recurrence.dialogs.PreferenceNagTimePicker;

public class PreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.prefs);

        // Open the system notification channel settings for sound, vibration and LED.
        // These are locked to the channel after first creation and must be changed via the system UI.
        Preference channelPref = findPreference("notificationChannelSettings");
        if (channelPref != null) {
            channelPref.setOnPreferenceClickListener(pref -> {
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, "recurrence_reminders");
                startActivity(intent);
                return true;
            });
        }

        updatePreferenceSummary();
    }

    public void updatePreferenceSummary() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Set nagging preference summary
        int nagMinutes = sharedPreferences.getInt("nagMinutes", getResources().getInteger(R.integer.default_nag_minutes));
        int nagSeconds = sharedPreferences.getInt("nagSeconds", getResources().getInteger(R.integer.default_nag_seconds));
        Preference nagPreference = findPreference("nagInterval");
        if (nagPreference != null) {
            String nagMinutesText = String.format(getActivity().getResources().getQuantityString(R.plurals.time_minute, nagMinutes), nagMinutes);
            String nagSecondsText = String.format(getActivity().getResources().getQuantityString(R.plurals.time_second, nagSeconds), nagSeconds);
            nagPreference.setSummary(String.format("%s %s", nagMinutesText, nagSecondsText));
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreferenceSummary();
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof PreferenceNagTimePicker) {
            PreferenceNagTimePicker.NagTimePickerDialogFragment dialogFragment =
                PreferenceNagTimePicker.NagTimePickerDialogFragment.newInstance(preference.getKey());
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), "NagTimePicker");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(this);
    }
}

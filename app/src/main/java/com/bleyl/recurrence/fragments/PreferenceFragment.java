package com.bleyl.recurrence.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.bleyl.recurrence.R;
import com.bleyl.recurrence.dialogs.PreferenceNagTimePicker;

public class PreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ActivityResultLauncher<Intent> ringtonePicker;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.prefs);

        ringtonePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    String uriString = (uri != null) ? uri.toString() : "";
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit().putString("NotificationSound", uriString).apply();
                }
            });

        Preference soundPref = findPreference("NotificationSound");
        if (soundPref != null) {
            soundPref.setOnPreferenceClickListener(pref -> {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                String current = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("NotificationSound", "content://settings/system/notification_sound");
                if (!current.isEmpty()) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current));
                }
                ringtonePicker.launch(intent);
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

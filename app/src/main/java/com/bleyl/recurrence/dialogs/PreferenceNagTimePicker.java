package com.bleyl.recurrence.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceManager;

import com.bleyl.recurrence.R;

public class PreferenceNagTimePicker extends DialogPreference {

    public static final int MAX_VALUE = 60;
    public static final int MIN_VALUE = 0;

    public PreferenceNagTimePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.number_picker);
        setPersistent(false);
    }

    // -----------------------------------------------------------------------
    // Inner dialog fragment that actually drives the dialog UI
    // -----------------------------------------------------------------------
    public static class NagTimePickerDialogFragment extends PreferenceDialogFragmentCompat {

        private NumberPicker minutePicker;
        private NumberPicker secondPicker;
        private SharedPreferences sharedPreferences;

        public static NagTimePickerDialogFragment newInstance(String key) {
            NagTimePickerDialogFragment fragment = new NagTimePickerDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_KEY, key);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        protected void onBindDialogView(@NonNull View view) {
            super.onBindDialogView(view);
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

            minutePicker = view.findViewById(R.id.picker1);
            minutePicker.setMaxValue(MAX_VALUE);
            minutePicker.setMinValue(MIN_VALUE);
            minutePicker.setValue(sharedPreferences.getInt("nagMinutes",
                requireContext().getResources().getInteger(R.integer.default_nag_minutes)));

            String[] minuteValues = new String[61];
            for (int i = 0; i < minuteValues.length; i++) {
                minuteValues[i] = String.format(
                    requireContext().getResources().getQuantityString(R.plurals.time_minute, i), i);
            }
            minutePicker.setDisplayedValues(minuteValues);

            secondPicker = view.findViewById(R.id.picker2);
            secondPicker.setMaxValue(MAX_VALUE);
            secondPicker.setMinValue(MIN_VALUE);
            secondPicker.setValue(sharedPreferences.getInt("nagSeconds",
                requireContext().getResources().getInteger(R.integer.default_nag_seconds)));

            String[] secondValues = new String[61];
            for (int i = 0; i < secondValues.length; i++) {
                secondValues[i] = String.format(
                    requireContext().getResources().getQuantityString(R.plurals.time_second, i), i);
            }
            secondPicker.setDisplayedValues(secondValues);
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult && minutePicker != null && secondPicker != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("nagMinutes", minutePicker.getValue());
                editor.putInt("nagSeconds", secondPicker.getValue());
                editor.apply();
            }
        }
    }
}

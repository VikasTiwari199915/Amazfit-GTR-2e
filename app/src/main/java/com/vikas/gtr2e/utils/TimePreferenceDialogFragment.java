package com.vikas.gtr2e.utils;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

public class TimePreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    private TimePicker timePicker;

    public static TimePreferenceDialogFragment newInstance(String key) {
        TimePreferenceDialogFragment fragment = new TimePreferenceDialogFragment();
        Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected View onCreateDialogView(@NonNull Context context) {
        timePicker = new TimePicker(context);
        timePicker.setIs24HourView(true);
        return timePicker;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);

        TimePreference pref = (TimePreference) getPreference();
        String[] parts = pref.getTime().split(":");

        timePicker.setHour(Integer.parseInt(parts[0]));
        timePicker.setMinute(Integer.parseInt(parts[1]));
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            String value = String.format("%02d:%02d", hour, minute);

            TimePreference pref = (TimePreference) getPreference();
            if (pref.callChangeListener(value)) {
                pref.setTime(value);
            }
        }
    }
}

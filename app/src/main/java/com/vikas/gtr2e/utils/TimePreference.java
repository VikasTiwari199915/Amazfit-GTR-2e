package com.vikas.gtr2e.utils;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

public class TimePreference extends DialogPreference {
    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public String getTime() {
        return getPersistedString("08:00");
    }

    public void setTime(String value) {
        persistString(value);
        notifyChanged();
    }
}

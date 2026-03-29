package com.vikas.gtr2e;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import com.mikhaellopez.circularprogressbar.BuildConfig;
import com.vikas.gtr2e.utils.AppAutoUpdater;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static void tintIcons(Preference preference, int color) {
        if (preference instanceof PreferenceGroup group) {
            if (group.getIcon() != null) {
                DrawableCompat.setTint(group.getIcon(), color);
            }
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                tintIcons(group.getPreference(i), color);
            }
        } else {
            Drawable icon = preference.getIcon();
            if (icon != null) {
                DrawableCompat.setTint(icon, color);
            }
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        int colorAttr = android.R.attr.colorAccent;

        TypedArray ta = requireContext().getTheme().obtainStyledAttributes(new int[]{colorAttr});
        int iconColor = ta.getColor(0, 0);
        ta.recycle();
        tintIcons(getPreferenceScreen(), iconColor);

        setVersionName();
    }

    private void setVersionName() {
        Preference versionPref = findPreference("version");
        if (versionPref != null) {
            try {
                versionPref.setSummary(AppAutoUpdater.getAppVersionName(requireContext()));
            } catch (Exception e) {
                versionPref.setSummary("N/A");
            }
            versionPref.setOnPreferenceClickListener(pref -> {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);

                ClipData clip = ClipData.newPlainText("version", versionPref.getSummary());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }
}
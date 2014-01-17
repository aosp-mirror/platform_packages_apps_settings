/*
 * Copyright (C) 2012 Slimroms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.mahdi;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.util.mahdi.DeviceUtils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class NotificationDrawerSettings extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener  {

    public static final String TAG = "NotificationDrawerSettings";
    private static final String PREF_NOTIFICATION_OPTIONS = "options";
    private static final String PREF_NOTIFICATION_HIDE_LABELS = "notification_hide_labels";
    private static final String STATUS_BAR_CUSTOM_HEADER = "custom_status_bar_header";

    private ListPreference mHideLabels;
    private CheckBoxPreference mStatusBarCustomHeader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.notification_drawer_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mHideLabels = (ListPreference) findPreference(PREF_NOTIFICATION_HIDE_LABELS);
        int hideCarrier = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_HIDE_LABELS, 0);
        mHideLabels.setValue(String.valueOf(hideCarrier));
        mHideLabels.setOnPreferenceChangeListener(this);
        updateHideNotificationLabelsSummary(hideCarrier);

        mStatusBarCustomHeader = (CheckBoxPreference) findPreference(STATUS_BAR_CUSTOM_HEADER);
        mStatusBarCustomHeader.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1);
        mStatusBarCustomHeader.setOnPreferenceChangeListener(this);

        PreferenceCategory additionalOptions =
            (PreferenceCategory) prefs.findPreference(PREF_NOTIFICATION_OPTIONS);

        PackageManager pm = getPackageManager();
        boolean isMobileData = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        if (!DeviceUtils.isPhone(getActivity())
            || !DeviceUtils.deviceSupportsMobileData(getActivity())) {
            // Nothing for tablets, large screen devices and non mobile devices which doesn't show
            // information in notification drawer.....remove options
            additionalOptions.removePreference(mHideLabels);
            prefs.removePreference(additionalOptions);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mHideLabels) {
            int hideLabels = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_HIDE_LABELS,
                    hideLabels);
            updateHideNotificationLabelsSummary(hideLabels);
            return true;
        } else if (preference == mStatusBarCustomHeader) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver,
                Settings.System.STATUS_BAR_CUSTOM_HEADER, value ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateHideNotificationLabelsSummary(int value) {
        Resources res = getResources();

        StringBuilder text = new StringBuilder();

        switch (value) {
        case 1  : text.append(res.getString(R.string.notification_hide_labels_carrier));
                break;
        case 2  : text.append(res.getString(R.string.notification_hide_labels_wifi));
                break;
        case 3  : text.append(res.getString(R.string.notification_hide_labels_all));
                break;
        default : text.append(res.getString(R.string.notification_hide_labels_disable));
                break;
        }

        text.append(" " + res.getString(R.string.notification_hide_labels_text));

        mHideLabels.setSummary(text.toString());
    }

}

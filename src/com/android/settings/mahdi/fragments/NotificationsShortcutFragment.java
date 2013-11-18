/*
 * Copyright (C) 2013 Slimroms
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

package com.android.settings.mahdi.fragments;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class NotificationsShortcutFragment extends SettingsPreferenceFragment {

    private static final String NOTIFICATION_SHORTCUTS_COLOR =
        "pref_notification_shortcuts_color";
    private static final String NOTIFICATION_SHORTCUTS_COLOR_MODE =
        "pref_notification_shortcuts_color_mode";

    private ColorPickerPreference mNotificationShortcutsColor;
    private ListPreference mNotificationShortcutsColorMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.notifications_shortcut_fragment);

        PreferenceScreen prefSet = getPreferenceScreen();

        PackageManager pm = getPackageManager();
        Resources systemUiResources = null;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            // we are lost if this will ever happen
        }

        mNotificationShortcutsColor =
            (ColorPickerPreference) findPreference(NOTIFICATION_SHORTCUTS_COLOR);
        int intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.NOTIFICATION_SHORTCUTS_COLOR, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/notification_shortcut_color", null, null));
            mNotificationShortcutsColor.setSummary(
                getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mNotificationShortcutsColor.setSummary(hexColor);
        }
        mNotificationShortcutsColor.setNewPreviewColor(intColor);

        mNotificationShortcutsColor.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                String hex = ColorPickerPreference.convertToARGB(
                        Integer.valueOf(String.valueOf(newValue)));
                preference.setSummary(hex);
                int intHex = ColorPickerPreference.convertToColorInt(hex);
                Settings.System.putInt(getActivity().getContentResolver(),
                        Settings.System.NOTIFICATION_SHORTCUTS_COLOR, intHex);
                return true;
            }
        });

        mNotificationShortcutsColorMode = (ListPreference) prefSet.findPreference(
                NOTIFICATION_SHORTCUTS_COLOR_MODE);
        mNotificationShortcutsColorMode.setValue(String.valueOf(
                Settings.System.getIntForUser(getContentResolver(),
                Settings.System.NOTIFICATION_SHORTCUTS_COLOR_MODE, 0,
                UserHandle.USER_CURRENT_OR_SELF)));
        mNotificationShortcutsColorMode.setSummary(mNotificationShortcutsColorMode.getEntry());

        mNotificationShortcutsColorMode.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                String val = (String) newValue;
                Settings.System.putInt(getContentResolver(),
                    Settings.System.NOTIFICATION_SHORTCUTS_COLOR_MODE,
                    Integer.valueOf(val));
                int index = mNotificationShortcutsColorMode.findIndexOfValue(val);
                mNotificationShortcutsColorMode.setSummary(
                    mNotificationShortcutsColorMode.getEntries()[index]);
                updateColorPreference();
                return true;
            }
        });
        updateColorPreference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView list = (ListView) view.findViewById(android.R.id.list);
        // our container already takes care of the padding
        if (list != null) {
            int paddingTop = list.getPaddingTop();
            int paddingBottom = list.getPaddingBottom();
            list.setPadding(0, paddingTop, 0, paddingBottom);
        }
        return view;
    }

    private void updateColorPreference() {
        int navigationBarButtonColorMode = Settings.System.getInt(getContentResolver(),
                Settings.System.NOTIFICATION_SHORTCUTS_COLOR_MODE, 0);
        mNotificationShortcutsColor.setEnabled(navigationBarButtonColorMode != 3);
    }

}

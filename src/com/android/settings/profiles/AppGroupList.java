/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import java.util.UUID;

import android.app.NotificationGroup;
import android.app.ProfileManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class AppGroupList extends SettingsPreferenceFragment {

    private static final String TAG = "AppGroupSettings";
    public static final String PROFILE_SERVICE = "profile";

    private ProfileManager mProfileManager;

    // constant value that can be used to check return code from sub activity.
    private static final int APP_GROUP_CONFIG = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.appgroup_list);
            mProfileManager = (ProfileManager) getActivity().getSystemService(PROFILE_SERVICE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();

        // On tablet devices remove the padding
        /*if (Utils.isScreenLarge()) {
            getListView().setPadding(0, 0, 0, 0);
        }*/
}

    public void refreshList() {
        // Only enable the preferences if system profiles are enabled
        boolean enabled = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1;

        PreferenceScreen appgroupList = getPreferenceScreen();
        appgroupList.removeAll();

        if (enabled) {
            // Add the existing app groups
            for (NotificationGroup group : mProfileManager.getNotificationGroups()) {
                PreferenceScreen pref = new PreferenceScreen(getActivity(), null);
                pref.setKey(group.getUuid().toString());
                pref.setTitle(group.getName());
                pref.setPersistent(false);
                pref.setEnabled(enabled);
                appgroupList.addPreference(pref);
            }
        } else {
            // Not enabled, display a message preference
            Preference npref = new Preference(getActivity());
            npref.setLayoutResource(R.layout.preference_empty_list);
            npref.setTitle(R.string.profile_empty_list_profiles_off);
            npref.setEnabled(false);
            appgroupList.addPreference(npref);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof PreferenceScreen) {
            NotificationGroup group = mProfileManager.getNotificationGroup(
                    UUID.fromString(preference.getKey()));
            editGroup(group);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void editGroup(NotificationGroup group) {
        Bundle args = new Bundle();
        args.putParcelable("NotificationGroup", group);

        PreferenceActivity pa = (PreferenceActivity) getActivity();
        pa.startPreferencePanel(AppGroupConfig.class.getName(), args,
                R.string.profile_appgroup_manage, null, this, APP_GROUP_CONFIG);
    }
}

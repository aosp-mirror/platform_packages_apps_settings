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

import android.app.Profile;
import android.app.ProfileManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class ProfilesList extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ProfilesSettings";

    public static final String EXTRA_POSITION = "position";
    public static final String PROFILE_SERVICE = "profile";
    public static final String RESTORE_CARRIERS_URI = "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";

    private String mSelectedKey;

    private ProfileManager mProfileManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.profiles_settings);
        mProfileManager = (ProfileManager) getActivity().getSystemService(PROFILE_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();

        // On tablet devices remove the padding
        if (Utils.isTablet(getActivity())) {
            getListView().setPadding(0, 0, 0, 0);
        }
    }

    public void refreshList() {
        // Only enable the preferences if system profiles are enabled
        boolean enabled = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SYSTEM_PROFILES_ENABLED, 1) == 1;

        PreferenceScreen plist = getPreferenceScreen();
        if (plist != null) {
            plist.removeAll();

            if (enabled && mProfileManager != null) {
                // Get active profile, if null
                Profile prof = mProfileManager.getActiveProfile();
                mSelectedKey = prof != null ? prof.getUuid().toString() : null;

                for(Profile profile : mProfileManager.getProfiles()) {
                    Bundle args = new Bundle();
                    args.putParcelable("Profile", profile);

                    ProfilesPreference ppref = new ProfilesPreference(this, args);
                    ppref.setKey(profile.getUuid().toString());
                    ppref.setTitle(profile.getName());
                    ppref.setPersistent(false);
                    ppref.setOnPreferenceChangeListener(this);
                    ppref.setSelectable(true);
                    ppref.setEnabled(true);

                    if ((mSelectedKey != null) && mSelectedKey.equals(ppref.getKey())) {
                        ppref.setChecked(true);
                    }

                    plist.addPreference(ppref);
                }
            } else {
                // Not enabled, display a message preference
                Preference npref = new Preference(getActivity());
                npref.setLayoutResource(R.layout.preference_empty_list);
                npref.setTitle(R.string.profile_empty_list_profiles_off);
                npref.setEnabled(false);

                plist.addPreference(npref);
            }
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue instanceof String) {
            setSelectedProfile((String) newValue);
            refreshList();
        }
        return true;
    }

    private void setSelectedProfile(String key) {
        try {
            UUID selectedUuid = UUID.fromString(key);
            mProfileManager.setActiveProfile(selectedUuid);
            mSelectedKey = key;
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }
}

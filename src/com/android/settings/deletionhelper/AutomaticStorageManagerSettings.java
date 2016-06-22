/**
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.deletionhelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;

/**
 * AutomaticStorageManagerSettings is the Settings screen for configuration and management of the
 * automatic storage manager.
 */
public class AutomaticStorageManagerSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    public static final int DEFAULT_DAYS_TO_RETAIN = 90;

    private static final String KEY_DAYS = "days";
    private static final String KEY_DELETION_HELPER = "deletion_helper";
    private static final String KEY_STORAGE_MANAGER_SWITCH = "storage_manager_active";

    private DropDownPreference mDaysToRetain;
    private Preference mDeletionHelper;
    private SwitchPreference mStorageManagerSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.automatic_storage_management_settings);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mDaysToRetain = (DropDownPreference) findPreference(KEY_DAYS);
        mDaysToRetain.setOnPreferenceChangeListener(this);

        mDeletionHelper = findPreference(KEY_DELETION_HELPER);
        mDeletionHelper.setOnPreferenceClickListener(this);

        mStorageManagerSwitch = (SwitchPreference) findPreference(KEY_STORAGE_MANAGER_SWITCH);
        mStorageManagerSwitch.setOnPreferenceChangeListener(this);

        int value = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN,
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN_DEFAULT);
        String[] stringValues =
                getResources().getStringArray(R.array.automatic_storage_management_days_values);
        mDaysToRetain.setValue(stringValues[daysValueToIndex(value, stringValues)]);
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean isChecked =
                Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED, 0) != 0;
        mStorageManagerSwitch.setChecked(isChecked);
        mDaysToRetain.setEnabled(isChecked);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case KEY_STORAGE_MANAGER_SWITCH:
                boolean checked = (boolean) newValue;
                mDaysToRetain.setEnabled(checked);
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED, checked ? 1 : 0);
                break;
            case KEY_DAYS:
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN,
                        Integer.parseInt((String) newValue));
                break;
        }
        return true;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.STORAGE_MANAGER_SETTINGS;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (KEY_DELETION_HELPER.equals(preference.getKey())) {
            Intent intent = new Intent(Settings.ACTION_DELETION_HELPER_SETTINGS);
            getContext().startActivity(intent);
        }
        return true;
    }

    private static int daysValueToIndex(int value, String[] indices) {
        for (int i = 0; i < indices.length; i++) {
            int thisValue = Integer.parseInt(indices[i]);
            if (value == thisValue) {
                return i;
            }
        }
        return indices.length - 1;
    }
}

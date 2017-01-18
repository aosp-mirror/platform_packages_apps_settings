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
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsLogger;
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
    private static final String KEY_FREED = "freed_bytes";
    private static final String KEY_STORAGE_MANAGER_SWITCH = "storage_manager_active";
    private static final String STORAGE_MANAGER_ENABLED_BY_DEFAULT_PROPERTY =
            "ro.storage_manager.enabled";

    private DropDownPreference mDaysToRetain;
    private Preference mFreedBytes;
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

        mFreedBytes = findPreference(KEY_FREED);

        mDeletionHelper = findPreference(KEY_DELETION_HELPER);
        mDeletionHelper.setOnPreferenceClickListener(this);

        mStorageManagerSwitch = (SwitchPreference) findPreference(KEY_STORAGE_MANAGER_SWITCH);
        mStorageManagerSwitch.setOnPreferenceChangeListener(this);

        ContentResolver cr = getContentResolver();
        int value = Settings.Secure.getInt(cr,
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN,
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN_DEFAULT);
        String[] stringValues =
                getResources().getStringArray(R.array.automatic_storage_management_days_values);
        mDaysToRetain.setValue(stringValues[daysValueToIndex(value, stringValues)]);

        long freedBytes = Settings.Secure.getLong(cr,
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_BYTES_CLEARED,
                0);
        long lastRunMillis = Settings.Secure.getLong(cr,
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_LAST_RUN,
                0);
        if (freedBytes == 0 || lastRunMillis == 0) {
            mFreedBytes.setVisible(false);
        } else {
            Activity activity = getActivity();
            mFreedBytes.setSummary(activity.getString(
                    R.string.automatic_storage_manager_freed_bytes,
                    Formatter.formatFileSize(activity, freedBytes),
                    DateUtils.formatDateTime(activity, lastRunMillis, DateUtils.FORMAT_SHOW_DATE)));
        }
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
                MetricsLogger.action(getContext(), MetricsEvent.ACTION_TOGGLE_STORAGE_MANAGER,
                        checked);
                mDaysToRetain.setEnabled(checked);
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED, checked ? 1 : 0);
                // Only show a warning if enabling.
                if (checked) {
                    maybeShowWarning();
                }
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
            Intent intent = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
            getContext().startActivity(intent);
        }
        return true;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_storage;
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

    private void maybeShowWarning() {
        // If the storage manager is on by default, we can use the normal message.
        boolean warningUnneeded = SystemProperties.getBoolean(
                STORAGE_MANAGER_ENABLED_BY_DEFAULT_PROPERTY, false);
        if (warningUnneeded) {
            return;
        }
        ActivationWarningFragment fragment = ActivationWarningFragment.newInstance();
        fragment.show(getFragmentManager(), ActivationWarningFragment.TAG);
    }
}

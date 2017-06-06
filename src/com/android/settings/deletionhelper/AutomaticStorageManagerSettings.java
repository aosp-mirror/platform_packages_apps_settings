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
import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;

/**
 * AutomaticStorageManagerSettings is the Settings screen for configuration and management of the
 * automatic storage manager.
 */
public class AutomaticStorageManagerSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {
    private static final String KEY_DAYS = "days";
    private static final String KEY_FREED = "freed_bytes";
    private static final String STORAGE_MANAGER_ENABLED_BY_DEFAULT_PROPERTY =
            "ro.storage_manager.enabled";

    private AutomaticStorageManagerSwitchBarController mSwitchController;
    private DropDownPreference mDaysToRetain;
    private Preference mFreedBytes;
    private SwitchBar mSwitchBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.automatic_storage_management_settings);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        initializeDaysToRetainPreference();
        initializeFreedBytesPreference();
        initializeSwitchBar();

        return view;
    }

    private void initializeDaysToRetainPreference() {
        mDaysToRetain = (DropDownPreference) findPreference(KEY_DAYS);
        mDaysToRetain.setOnPreferenceChangeListener(this);

        ContentResolver cr = getContentResolver();
        int photosDaysToRetain =
                Settings.Secure.getInt(
                        cr,
                        Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN,
                        Utils.getDefaultStorageManagerDaysToRetain(getResources()));
        String[] stringValues =
                getResources().getStringArray(R.array.automatic_storage_management_days_values);
        mDaysToRetain.setValue(stringValues[daysValueToIndex(photosDaysToRetain, stringValues)]);
    }

    private void initializeSwitchBar() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.show();
        mSwitchController =
                new AutomaticStorageManagerSwitchBarController(
                        getContext(),
                        mSwitchBar,
                        mMetricsFeatureProvider,
                        mDaysToRetain,
                        getFragmentManager());
    }

    private void initializeFreedBytesPreference() {
        ContentResolver cr = getContentResolver();
        mFreedBytes = findPreference(KEY_FREED);
        long freedBytes = Settings.Secure.getLong(cr,
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_BYTES_CLEARED,
                0);
        long lastRunMillis = Settings.Secure.getLong(cr,
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_LAST_RUN,
                0);
        if (freedBytes == 0 || lastRunMillis == 0) {
            mFreedBytes.setVisible(false);
        } else {
            final Activity activity = getActivity();
            mFreedBytes.setSummary(
                    activity.getString(
                            R.string.automatic_storage_manager_freed_bytes,
                            Formatter.formatFileSize(activity, freedBytes),
                            DateUtils.formatDateTime(
                                    activity, lastRunMillis, DateUtils.FORMAT_SHOW_DATE)));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean isStorageManagerChecked =
                Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED, 0) != 0;
        mDaysToRetain.setEnabled(isStorageManagerChecked);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mSwitchBar.hide();
        mSwitchController.tearDown();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_DAYS.equals(preference.getKey())) {
            Settings.Secure.putInt(
                    getContentResolver(),
                    Settings.Secure.AUTOMATIC_STORAGE_MANAGER_DAYS_TO_RETAIN,
                    Integer.parseInt((String) newValue));
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.STORAGE_MANAGER_SETTINGS;
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

}

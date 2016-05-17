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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
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
        OnSwitchChangeListener, OnPreferenceChangeListener {
    private static final String KEY_DAYS = "days";

    private SwitchBar mSwitchBar;
    private DropDownPreference mDaysToRetain;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.automatic_storage_management_settings);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        mSwitchBar = ((SettingsActivity) activity).getSwitchBar();
        mSwitchBar.show();
        mSwitchBar.addOnSwitchChangeListener(this);
        // TODO: Initialize the switch bar position based on if the storage manager is active.

        mDaysToRetain = (DropDownPreference) findPreference(KEY_DAYS);
        mDaysToRetain.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO: Initialize the switch bar position based on if the storage manager is active.
        maybeShowDayDropdown(mSwitchBar.isChecked());
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        // TODO: Flip a setting which controls if the storage manager should run.
        maybeShowDayDropdown(isChecked);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO: Configure a setting which controls how many days of data the storage manager
        // should retain.
        return true;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.STORAGE_MANAGER_SETTINGS;
    }

    private void maybeShowDayDropdown(boolean shouldShow) {
        PreferenceScreen screen = getPreferenceScreen();
        if (shouldShow) {
            screen.addPreference(mDaysToRetain);
        } else {
            screen.removePreference(mDaysToRetain);
        }
    }
}

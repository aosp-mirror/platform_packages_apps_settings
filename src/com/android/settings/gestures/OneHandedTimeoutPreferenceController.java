/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import android.content.Context;
import android.net.Uri;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.HashMap;
import java.util.Map;

/**
 * The Controller to handle one-handed mode timeout state.
 **/
public class OneHandedTimeoutPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver, OnStart, OnStop,
        OneHandedSettingsUtils.TogglesCallback {

    private final Map<String, String> mTimeoutMap;
    private Preference mTimeoutPreference;
    private final OneHandedSettingsUtils mUtils;

    public OneHandedTimeoutPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mTimeoutMap = new HashMap<>();
        initTimeoutMap();
        mUtils = new OneHandedSettingsUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        if (!(preference instanceof ListPreference)) {
            return false;
        }
        final int newValue = Integer.parseInt((String) object);
        OneHandedSettingsUtils.setSettingsOneHandedModeTimeout(mContext, newValue);
        updateState(preference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!(preference instanceof ListPreference)) {
            return;
        }
        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setValue(getTimeoutValue());

        final int availabilityStatus = getAvailabilityStatus();
        preference.setEnabled(
                availabilityStatus == AVAILABLE || availabilityStatus == AVAILABLE_UNSEARCHABLE);
    }

    @Override
    public CharSequence getSummary() {
        if (OneHandedSettingsUtils.getSettingsOneHandedModeTimeout(mContext) == 0) {
            return mContext.getResources().getString(R.string.screensaver_settings_summary_never);
        }
        return String.format(mContext.getResources().getString(
                R.string.one_handed_timeout_summary), mTimeoutMap.get(getTimeoutValue()));
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mTimeoutPreference = screen.findPreference(mPreferenceKey);
    }

    @Override
    public void onStart() {
        mUtils.registerToggleAwareObserver(this);
    }

    @Override
    public void onStop() {
        mUtils.unregisterToggleAwareObserver();
    }

    @Override
    public void onChange(Uri uri) {
        updateState(mTimeoutPreference);
    }

    private String getTimeoutValue() {
        return String.valueOf(OneHandedSettingsUtils.getSettingsOneHandedModeTimeout(mContext));
    }

    private void initTimeoutMap() {
        if (mTimeoutMap.size() != 0) {
            return;
        }

        final String[] timeoutValues = mContext.getResources().getStringArray(
                R.array.one_handed_timeout_values);
        final String[] timeoutTitles = mContext.getResources().getStringArray(
                R.array.one_handed_timeout_title);

        if (timeoutValues.length != timeoutTitles.length) {
            return;
        }

        for (int i = 0; i < timeoutValues.length; i++) {
            mTimeoutMap.put(timeoutValues[i], timeoutTitles[i]);
        }
    }
}

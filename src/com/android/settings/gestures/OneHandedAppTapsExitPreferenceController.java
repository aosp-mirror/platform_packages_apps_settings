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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * The Controller to handle app taps to exit of one-handed mode
 */
public class OneHandedAppTapsExitPreferenceController extends TogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop, OneHandedSettingsUtils.TogglesCallback {

    private final OneHandedSettingsUtils mUtils;

    private Preference mPreference;

    public OneHandedAppTapsExitPreferenceController(Context context, String key) {
        super(context, key);

        mUtils = new OneHandedSettingsUtils(context);

        // By default, app taps to stop one-handed is enabled, this will get default value once.
        OneHandedSettingsUtils.setTapsAppToExitEnabled(mContext, isChecked());
    }

    @Override
    public int getAvailabilityStatus() {
        return OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        final int availabilityStatus = getAvailabilityStatus();
        preference.setEnabled(
                availabilityStatus == AVAILABLE || availabilityStatus == AVAILABLE_UNSEARCHABLE);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return OneHandedSettingsUtils.setTapsAppToExitEnabled(mContext, isChecked);
    }

    @Override
    public boolean isChecked() {
        return OneHandedSettingsUtils.isTapsAppToExitEnabled(mContext);
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
        updateState(mPreference);
    }
}

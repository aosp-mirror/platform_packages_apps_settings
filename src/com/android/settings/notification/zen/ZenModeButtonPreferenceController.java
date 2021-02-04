/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.Settings;
import android.widget.Switch;

import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.SettingsEnableZenModeDialog;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

public class ZenModeButtonPreferenceController extends AbstractZenModePreferenceController
        implements PreferenceControllerMixin, OnMainSwitchChangeListener {

    private static final String TAG = "EnableZenModeButton";

    public static final String KEY = "zen_mode_toggle";

    private final FragmentManager mFragment;

    // DND can also be toggled from QS.
    private MainSwitchPreference mPreference;

    public ZenModeButtonPreferenceController(Context context, Lifecycle lifecycle, FragmentManager
            fragment) {
        super(context, KEY, lifecycle);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (MainSwitchPreference) screen.findPreference(getPreferenceKey());
        mPreference.addOnSwitchChangeListener(this);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            updateZenModeState(mPreference);
        } else {
            writeMetrics(mPreference, false);
            mBackend.setZenMode(Settings.Global.ZEN_MODE_OFF);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        updatePreference(preference);
    }

    private void updatePreference(Preference preference) {
        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_ALARMS:
            case Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                mPreference.updateStatus(true);
                break;
            case Settings.Global.ZEN_MODE_OFF:
            default:
                mPreference.updateStatus(false);
        }
    }

    private void updateZenModeState(Preference preference) {
        writeMetrics(preference, true);
        int zenDuration = getZenDuration();
        switch (zenDuration) {
            case Settings.Secure.ZEN_DURATION_PROMPT:
                new SettingsEnableZenModeDialog().show(mFragment, TAG);
                break;
            case Settings.Secure.ZEN_DURATION_FOREVER:
                mBackend.setZenMode(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
                break;
            default:
                mBackend.setZenModeForDuration(zenDuration);
        }
    }

    private void writeMetrics(Preference preference, boolean buttonOn) {
        mMetricsFeatureProvider.logClickedPreference(preference,
                preference.getExtras().getInt(DashboardFragment.CATEGORY));
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ZEN_TOGGLE_DND_BUTTON,
                buttonOn);
    }
}

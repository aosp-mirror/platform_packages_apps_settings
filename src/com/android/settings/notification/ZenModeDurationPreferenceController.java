/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.FragmentManager;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeDurationPreferenceController extends AbstractZenModePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceClickListener {

    private static final String TAG = "ZenModeDurationDialog";
    protected static final String KEY = "zen_mode_duration_settings";
    private FragmentManager mFragment;

    public ZenModeDurationPreferenceController(Context context, Lifecycle lifecycle, FragmentManager
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
        screen.findPreference(KEY).setOnPreferenceClickListener(this);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        String summary = "";
        int zenDuration = getZenDuration();
        if (zenDuration < 0) {
            summary = mContext.getString(R.string.zen_mode_duration_summary_always_prompt);
        } else if (zenDuration == 0) {
            summary = mContext.getString(R.string.zen_mode_duration_summary_forever);
        } else {
            if (zenDuration >= 60) {
                int hours = zenDuration / 60;
                summary = mContext.getResources().getQuantityString(
                        R.plurals.zen_mode_duration_summary_time_hours, hours, hours);
            } else {
                summary = mContext.getResources().getString(
                        R.string.zen_mode_duration_summary_time_minutes, zenDuration);
            }
        }

        preference.setSummary(summary);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        new SettingsZenDurationDialog().show(mFragment, TAG);
        return true;
    }
}
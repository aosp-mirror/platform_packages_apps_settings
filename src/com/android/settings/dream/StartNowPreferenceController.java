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

package com.android.settings.dream;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.SettingsMainSwitchPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.dream.DreamBackend;

/**
 * Controller that used to enable screen saver
 */
public class StartNowPreferenceController extends SettingsMainSwitchPreferenceController {

    private final DreamBackend mBackend;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public StartNowPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBackend = DreamBackend.getInstance(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        mSwitchPreference.setChecked(false);
        mSwitchPreference.setEnabled(mBackend.getWhenToDreamSetting() != DreamBackend.NEVER);
    }

    @Override
    public boolean isChecked() {
        return false;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            if (mSwitchPreference != null) {
                mMetricsFeatureProvider.logClickedPreference(mSwitchPreference,
                        mSwitchPreference.getExtras().getInt(DashboardFragment.CATEGORY));
            }
            mBackend.startDreaming();
        }
        return true;
    }
}

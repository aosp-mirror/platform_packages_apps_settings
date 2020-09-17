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

package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller for Enhanced Connectivity feature
 */
public class EnhancedConnectivityPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String ENHANCED_CONNECTIVITY_KEY = "enhanced_connectivity";

    @VisibleForTesting
    static final int ENHANCED_CONNECTIVITY_ON = 1;
    // default is enhanced connectivity enabled.
    @VisibleForTesting
    static final int ENHANCED_CONNECTIVITY_OFF = 0;

    public EnhancedConnectivityPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        final boolean isEnabled = (Boolean) o;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ENHANCED_CONNECTIVITY_ENABLED,
                isEnabled
                        ? ENHANCED_CONNECTIVITY_ON
                        : ENHANCED_CONNECTIVITY_OFF);
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return ENHANCED_CONNECTIVITY_KEY;
    }

    @Override
    public void updateState(Preference preference) {
        final int enhancedConnectivityEnabled = Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.ENHANCED_CONNECTIVITY_ENABLED,
                ENHANCED_CONNECTIVITY_ON);
        ((SwitchPreference) mPreference).setChecked(
                enhancedConnectivityEnabled == ENHANCED_CONNECTIVITY_ON);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_enhanced_connectivity);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ENHANCED_CONNECTIVITY_ENABLED,
                ENHANCED_CONNECTIVITY_ON);
        ((SwitchPreference) mPreference).setChecked(true);
    }
}

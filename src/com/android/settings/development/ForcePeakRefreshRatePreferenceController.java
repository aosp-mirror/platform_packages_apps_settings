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

import static com.android.internal.display.RefreshRateSettingsUtils.DEFAULT_REFRESH_RATE;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.display.RefreshRateSettingsUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ForcePeakRefreshRatePreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    @VisibleForTesting
    float mPeakRefreshRate;

    private static final String TAG = "ForcePeakRefreshRateCtr";
    private static final String PREFERENCE_KEY = "pref_key_peak_refresh_rate";

    public ForcePeakRefreshRatePreferenceController(Context context) {
        super(context);
        mPeakRefreshRate =
                RefreshRateSettingsUtils.findHighestRefreshRateForDefaultDisplay(context);
        Log.d(TAG, "DEFAULT_REFRESH_RATE : " + DEFAULT_REFRESH_RATE
            + " mPeakRefreshRate : " + mPeakRefreshRate);
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        forcePeakRefreshRate(isEnabled);

        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) mPreference).setChecked(isForcePeakRefreshRateEnabled());
    }

    @Override
    public boolean isAvailable() {
        if (mContext.getResources().getBoolean(R.bool.config_show_smooth_display)) {
            return mPeakRefreshRate > DEFAULT_REFRESH_RATE;
        } else {
            return false;
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.FORCE_PEAK_REFRESH_RATE, 0);

        ((SwitchPreference) mPreference).setChecked(false);
    }

    @VisibleForTesting
    void forcePeakRefreshRate(boolean enable) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.FORCE_PEAK_REFRESH_RATE, enable ? 1 : 0);
    }

    boolean isForcePeakRefreshRateEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.FORCE_PEAK_REFRESH_RATE, 0) == 1;
    }
}

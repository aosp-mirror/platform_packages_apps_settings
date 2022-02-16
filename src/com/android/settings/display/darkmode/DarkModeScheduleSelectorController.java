/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.PowerManager;

import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.display.TwilightLocationDialog;

/**
 * Controller for the dark ui option dropdown
 */
public class DarkModeScheduleSelectorController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = DarkModeScheduleSelectorController.class.getSimpleName();
    private final UiModeManager mUiModeManager;
    private PowerManager mPowerManager;
    private DropDownPreference mPreference;
    private LocationManager mLocationManager;
    private int mCurrentMode;

    public DarkModeScheduleSelectorController(Context context, String key) {
        super(context, key);
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mPowerManager = context.getSystemService(PowerManager.class);
        mLocationManager = context.getSystemService(LocationManager.class);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return BasePreferenceController.AVAILABLE;
    }

    @Override
    public final void updateState(Preference preference) {
        final boolean batterySaver = mPowerManager.isPowerSaveMode();
        mPreference.setEnabled(!batterySaver);
        mCurrentMode = getCurrentMode();
        mPreference.setValueIndex(mCurrentMode);
    }

    private int getCurrentMode() {
        int resId;
        switch (mUiModeManager.getNightMode()) {
            case UiModeManager.MODE_NIGHT_AUTO:
                resId = R.string.dark_ui_auto_mode_auto;
                break;
            case UiModeManager.MODE_NIGHT_CUSTOM:
                resId = R.string.dark_ui_auto_mode_custom;
                break;
            default:
                resId = R.string.dark_ui_auto_mode_never;
        }
        return mPreference.findIndexOfValue(mContext.getString(resId));
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object newValue) {
        final int newMode = mPreference.findIndexOfValue((String) newValue);
        if (newMode == mCurrentMode) {
            return false;
        }
        if (newMode == mPreference.findIndexOfValue(
                mContext.getString(R.string.dark_ui_auto_mode_never))) {
            boolean active = (mContext.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_YES) != 0;
            int mode = active ? UiModeManager.MODE_NIGHT_YES
                    : UiModeManager.MODE_NIGHT_NO;
            mUiModeManager.setNightMode(mode);
        } else if (newMode == mPreference.findIndexOfValue(
                mContext.getString(R.string.dark_ui_auto_mode_auto))) {
            if (!mLocationManager.isLocationEnabled()) {
                TwilightLocationDialog.show(mContext);
                return true;
            }
            mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_AUTO);
        } else if (newMode == mPreference.findIndexOfValue(
                mContext.getString(R.string.dark_ui_auto_mode_custom))) {
            mUiModeManager.setNightMode(UiModeManager.MODE_NIGHT_CUSTOM);
        }
        mCurrentMode = newMode;
        return true;
    }
}

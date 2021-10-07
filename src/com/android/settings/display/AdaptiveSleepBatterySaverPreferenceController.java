/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.widget.BannerMessagePreference;

/**
 * The controller of Screen attention's battery saver warning preference.
 * The preference appears when Screen Attention feature is disabled by battery saver mode.
 */
public class AdaptiveSleepBatterySaverPreferenceController {

    @VisibleForTesting
    BannerMessagePreference mPreference;
    private final PowerManager mPowerManager;
    private final Context mContext;

    public AdaptiveSleepBatterySaverPreferenceController(Context context) {
        mPowerManager = context.getSystemService(PowerManager.class);
        mContext = context;
    }

    /**
     * Adds the controlled preference to the provided preference screen.
     */
    public void addToScreen(PreferenceScreen screen) {
        initializePreference();
        screen.addPreference(mPreference);
        updateVisibility();
    }

    /**
     * Need this because all controller tests use RoboElectric. No easy way to mock this service,
     * so we mock the call we need
     */
    @VisibleForTesting
    boolean isPowerSaveMode() {
        return mPowerManager.isPowerSaveMode();
    }

    /**
     * Refreshes the visibility of the preference.
     */
    public void updateVisibility() {
        initializePreference();
        mPreference.setVisible(isPowerSaveMode());
    }

    private void initializePreference() {
        if (mPreference == null) {
            mPreference = new BannerMessagePreference(mContext);
            mPreference.setTitle(R.string.ambient_camera_summary_battery_saver_on);
            mPreference.setPositiveButtonText(R.string.disable_text);
            mPreference.setPositiveButtonOnClickListener(
                    p -> mPowerManager.setPowerSaveModeEnabled(false));
        }
    }
}

/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

/** Controls the update for battery tips card */
public class BatteryTipsController extends BasePreferenceController {

    private static final String TAG = "BatteryTipsController";
    private static final String ROOT_PREFERENCE_KEY = "battery_tips_category";
    private static final String CARD_PREFERENCE_KEY = "battery_tips_card";

    private final PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    private Context mPrefContext;
    private BatteryTipsCardPreference mCardPreference;

    public BatteryTipsController(Context context) {
        super(context, ROOT_PREFERENCE_KEY);
        mPowerUsageFeatureProvider = FeatureFactory.getFeatureFactory()
            .getPowerUsageFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrefContext = screen.getContext();
        mCardPreference = screen.findPreference(CARD_PREFERENCE_KEY);
    }

    /**
     * Update the card visibility and contents.
     * @param title a string not extend 2 lines.
     * @param summary a string not extend 10 lines.
     */
    // TODO: replace parameters with SettingsAnomaly Data Proto
    public void handleBatteryTipsCardUpdated(String title, String summary) {
        if (!mPowerUsageFeatureProvider.isBatteryTipsEnabled()) {
            mCardPreference.setVisible(false);
            return;
        }
        if (title == null || summary == null) {
            mCardPreference.setVisible(false);
            return;
        }
        mCardPreference.setTitle(title);
        mCardPreference.setSummary(summary);
        mCardPreference.setVisible(true);
    }

}

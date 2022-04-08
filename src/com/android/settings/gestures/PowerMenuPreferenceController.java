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
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class PowerMenuPreferenceController extends BasePreferenceController {

    private static final String KEY = "gesture_power_menu_summary";
    private static final String CONTROLS_ENABLED_SETTING = Settings.Secure.CONTROLS_ENABLED;
    private static final String CARDS_ENABLED_SETTING =
            Settings.Secure.GLOBAL_ACTIONS_PANEL_ENABLED;
    private static final String CARDS_AVAILABLE_SETTING =
            Settings.Secure.GLOBAL_ACTIONS_PANEL_AVAILABLE;

    public PowerMenuPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public CharSequence getSummary() {
        boolean controlsVisible = isControlsAvailable()
                && Settings.Secure.getInt(mContext.getContentResolver(),
                        CONTROLS_ENABLED_SETTING, 1) == 1;
        boolean cardsVisible = isCardsAvailable()
                && Settings.Secure.getInt(mContext.getContentResolver(),
                        CARDS_ENABLED_SETTING, 0) == 1;
        if (controlsVisible && cardsVisible) {
            return mContext.getText(R.string.power_menu_cards_passes_device_controls);
        } else if (controlsVisible) {
            return mContext.getText(R.string.power_menu_device_controls);
        } else if (cardsVisible) {
            return mContext.getText(R.string.power_menu_cards_passes);
        } else {
            return mContext.getText(R.string.power_menu_none);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return isCardsAvailable() || isControlsAvailable() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    private boolean isControlsAvailable() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CONTROLS);
    }

    private boolean isCardsAvailable() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                CARDS_AVAILABLE_SETTING, 0) == 1;
    }
}

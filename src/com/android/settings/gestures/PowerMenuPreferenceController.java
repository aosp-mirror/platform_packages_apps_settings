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

import static com.android.settings.gestures.PowerMenuSettingsUtils.LONG_PRESS_POWER_ASSISTANT_VALUE;
import static com.android.settings.gestures.PowerMenuSettingsUtils.LONG_PRESS_POWER_GLOBAL_ACTIONS;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class PowerMenuPreferenceController extends BasePreferenceController {

    public PowerMenuPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public CharSequence getSummary() {
        final int powerButtonValue = PowerMenuSettingsUtils.getPowerButtonSettingValue(mContext);
        if (powerButtonValue == LONG_PRESS_POWER_ASSISTANT_VALUE) {
            return mContext.getText(R.string.power_menu_summary_long_press_for_assist_enabled);
        } else if (powerButtonValue == LONG_PRESS_POWER_GLOBAL_ACTIONS) {
            return mContext.getText(
                    R.string.power_menu_summary_long_press_for_assist_disabled_with_power_menu);
        } else {
            return mContext.getText(
                    R.string.power_menu_summary_long_press_for_assist_disabled_no_action);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return isAssistInvocationAvailable() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private boolean isAssistInvocationAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable);
    }
}

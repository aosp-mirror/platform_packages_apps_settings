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

package com.android.settings.gestures;

import android.content.Context;

import com.android.settings.widget.SettingsMainSwitchPreferenceController;

/**
 * The controller to handle one-handed mode main switch enable or disable state.
 **/
public class OneHandedMainSwitchPreferenceController extends
        SettingsMainSwitchPreferenceController {

    public OneHandedMainSwitchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return (OneHandedSettingsUtils.isSupportOneHandedMode()
                && OneHandedSettingsUtils.getNavigationBarMode(mContext) != 0 /* 3-button mode */)
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    @Override
    public boolean isChecked() {
        return OneHandedSettingsUtils.isOneHandedModeEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            // Set default value for TapsAppToExit and Timeout
            OneHandedSettingsUtils.setTapsAppToExitEnabled(mContext, true);
            OneHandedSettingsUtils.setTimeoutValue(mContext,
                    OneHandedSettingsUtils.OneHandedTimeout.MEDIUM.getValue());
        }
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, isChecked);
        return true;
    }
}

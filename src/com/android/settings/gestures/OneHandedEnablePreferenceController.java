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
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.TogglePreferenceController;

/**
 * The controller to handle one-handed mode enable or disable state.
 **/
public class OneHandedEnablePreferenceController extends TogglePreferenceController {

    static final String SUPPORT_ONE_HANDED_MODE = "ro.support_one_handed_mode";

    public OneHandedEnablePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return SystemProperties.getBoolean(SUPPORT_ONE_HANDED_MODE, false)
                ? BasePreferenceController.AVAILABLE
                : BasePreferenceController.UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        OneHandedSettingsUtils.setSettingsOneHandedModeEnabled(mContext,
                isChecked);
        return true;
    }

    @Override
    public boolean isChecked() {
        return OneHandedSettingsUtils.isOneHandedModeEnabled(mContext);
    }

    @Override
    public CharSequence getSummary() {
        return OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)
                ? mContext.getText(R.string.switch_on_text)
                : mContext.getText(R.string.switch_off_text);
    }
}

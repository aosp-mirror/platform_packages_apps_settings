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

package com.android.settings.sound;

import android.content.Context;
import android.provider.DeviceConfig;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller for vibrate for calls settings.
 */
public class VibrateForCallsPreferenceController extends BasePreferenceController {

    private static final int ON = 1;
    private static final int OFF = 0;
    @VisibleForTesting
    static final String RAMPING_RINGER_ENABLED = "ramping_ringer_enabled";

    public VibrateForCallsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return Utils.isVoiceCapable(mContext) && !DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_TELEPHONY, RAMPING_RINGER_ENABLED, false)
            ? AVAILABLE
            : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        if (Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.APPLY_RAMPING_RINGER, OFF) == ON) {
            return mContext.getText(R.string.vibrate_when_ringing_option_ramping_ringer);
        } else if (Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.VIBRATE_WHEN_RINGING, OFF) == ON) {
            return mContext.getText(R.string.vibrate_when_ringing_option_always_vibrate);
        } else {
            return mContext.getText(R.string.vibrate_when_ringing_option_never_vibrate);
        }
    }
}

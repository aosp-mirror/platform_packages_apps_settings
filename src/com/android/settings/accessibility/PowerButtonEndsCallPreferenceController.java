/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.provider.Settings;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;

public class PowerButtonEndsCallPreferenceController extends TogglePreferenceController {

    public PowerButtonEndsCallPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        final int incallPowerBehavior = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
        return incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                (isChecked ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    @Override
    public int getAvailabilityStatus() {
        return !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                || !Utils.isVoiceCapable(mContext) ? UNSUPPORTED_ON_DEVICE : AVAILABLE;
    }
}

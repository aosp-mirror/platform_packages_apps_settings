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
import android.provider.Settings;

/** Common code for long press power settings shared between controllers. */
final class PowerMenuSettingsUtils {

    /**
     * Setting storing the current behaviour of long press power.
     */
    public static final String POWER_BUTTON_LONG_PRESS_SETTING =
            Settings.Global.POWER_BUTTON_LONG_PRESS;

    /**
     * Value used for long press power button behaviour when the Assist setting is disabled.
     *
     * If this value matches Assist setting, then it falls back to Global Actions panel or
     * power menu, depending on their respective settings.
     */
    public static final int POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE =
            com.android.internal.R.integer.config_longPressOnPowerBehavior;

    /**
     * Values used for long press power button behaviour when Assist setting is enabled.
     *
     * {@link com.android.server.policy.PhoneWindowManager#LONG_PRESS_POWER_GLOBAL_ACTIONS} for
     * source of the value.
     */
    static final int LONG_PRESS_POWER_NO_ACTION = 0;
    static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int LONG_PRESS_POWER_ASSISTANT_VALUE = 5; // Settings.Secure.ASSISTANT

    /**
     * @return current value of power button behaviour.
     */
    public static int getPowerButtonSettingValue(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                POWER_BUTTON_LONG_PRESS_SETTING,
                context.getResources().getInteger(POWER_BUTTON_LONG_PRESS_DEFAULT_VALUE_RESOURCE));
    }

    /**
     * @return true if long press power for assist is currently enabled.
     */
    public static boolean isLongPressPowerForAssistEnabled(Context context) {
        return getPowerButtonSettingValue(context) == LONG_PRESS_POWER_ASSISTANT_VALUE;
    }

    private PowerMenuSettingsUtils() {
    }
}

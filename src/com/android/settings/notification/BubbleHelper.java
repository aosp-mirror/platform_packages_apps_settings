/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.settings.notification;

import static android.provider.Settings.Secure.NOTIFICATION_BUBBLES;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

/**
 * Helper class for configuring notification bubbles.
 */
public class BubbleHelper {

    /**
     * {@link Settings.Secure.NOTIFICATION_BUBBLES} is enabled.
     */
    public static final int SYSTEM_WIDE_ON = 1;

    /**
     * {@link Settings.Secure.NOTIFICATION_BUBBLES} is disabled.
     */
    public static final int SYSTEM_WIDE_OFF = 0;

    /**
     * Returns true if the device supports bubbles.
     */
    public static boolean isSupportedByDevice(Context context) {
        ActivityManager am = context.getSystemService(ActivityManager.class);
        if (am.isLowRamDevice()) {
            return false;
        }
        if (!Resources.getSystem().getBoolean(com.android.internal.R.bool.config_supportsBubble)) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if the device supports bubbles and the global settings is enabled.
     */
    public static boolean isEnabledSystemWide(Context context) {
        if (!isSupportedByDevice(context)) {
            return false;
        }
        return Settings.Secure.getInt(context.getContentResolver(), NOTIFICATION_BUBBLES,
                SYSTEM_WIDE_ON) == SYSTEM_WIDE_ON;
    }
}

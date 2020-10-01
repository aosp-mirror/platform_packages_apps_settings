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

package com.android.settings.core.instrumentation;

import android.content.Context;
import android.provider.DeviceConfig;

import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settingslib.core.instrumentation.EventLogWriter;

public class SettingsEventLogWriter extends EventLogWriter {

    @Override
    public void visible(Context context, int source, int category, int latency) {
        if (shouldDisableGenericEventLogging()) {
            return;
        }
        super.visible(context, source, category, latency);
    }

    @Override
    public void hidden(Context context, int category, int visibleTime) {
        if (shouldDisableGenericEventLogging()) {
            return;
        }
        super.hidden(context, category, visibleTime);
    }

    @Override
    public void action(Context context, int category, String pkg) {
        if (shouldDisableGenericEventLogging()) {
            return;
        }
        super.action(context, category, pkg);
    }

    @Override
    public void action(Context context, int category, int value) {
        if (shouldDisableGenericEventLogging()) {
            return;
        }
        super.action(context, category, value);
    }

    @Override
    public void action(Context context, int category, boolean value) {
        if (shouldDisableGenericEventLogging()) {
            return;
        }
        super.action(context, category, value);
    }

    private static boolean shouldDisableGenericEventLogging() {
        return !DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.GENERIC_EVENT_LOGGING_ENABLED, true /* default */);
    }
}

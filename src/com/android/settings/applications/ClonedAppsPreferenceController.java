/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.applications;

import static com.android.settings.core.SettingsUIDeviceConfig.CLONED_APPS_ENABLED;

import android.content.Context;
import android.provider.DeviceConfig;

import com.android.settings.core.BasePreferenceController;

/**
 * A preference controller handling the logic for updating the summary of cloned apps.
 */
public class ClonedAppsPreferenceController extends BasePreferenceController {

    public ClonedAppsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public CharSequence getSummary() {
        // todo(b/249916469): Update summary once we have mechanism of allowlisting available
        //  for cloned apps.
        return null;
    }
    @Override
    public int getAvailabilityStatus() {
        return DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
                CLONED_APPS_ENABLED, false) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}

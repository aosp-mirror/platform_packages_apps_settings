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
package com.android.settings.display;

import android.content.Context;
import android.text.TextUtils;

import com.android.settings.core.BasePreferenceController;

/**
 * The top-level preference controller for device state based auto-rotation settings.
 *
 * It doesn't do anything on its own besides showing/hiding. The toggling of the settings will
 * always be done in the details screen when device state based auto-rotation is enabled.
 */
public class DeviceStateAutoRotateOverviewController extends BasePreferenceController {

    /** Preference key for when it is used in "accessibility_system_controls.xml". */
    private static final String ACCESSIBILITY_PREF_KEY = "device_state_auto_rotate_accessibility";

    public DeviceStateAutoRotateOverviewController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailableInternal() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private boolean isAvailableInternal() {
        return isA11yPage()
                ? DeviceStateAutoRotationHelper.isDeviceStateRotationEnabledForA11y(mContext)
                : DeviceStateAutoRotationHelper.isDeviceStateRotationEnabled(mContext);
    }

    private boolean isA11yPage() {
        return TextUtils.equals(getPreferenceKey(), ACCESSIBILITY_PREF_KEY);
    }
}

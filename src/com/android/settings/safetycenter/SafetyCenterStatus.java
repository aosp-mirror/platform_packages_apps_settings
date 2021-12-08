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

package com.android.settings.safetycenter;

import android.provider.DeviceConfig;

import com.android.internal.annotations.VisibleForTesting;

/** Knows whether safety center is enabled or disabled. */
public class SafetyCenterStatus {

    /** Whether SafetyCenter page is enabled. */
    @VisibleForTesting
    public static final String SAFETY_CENTER_IS_ENABLED = "safety_center_is_enabled";

    /** Returns true is SafetyCenter page is enabled, false otherwise. */
    public static boolean isEnabled() {
        // TODO(b/208625216): use SafetyManager API instead
        return DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_PRIVACY, SAFETY_CENTER_IS_ENABLED, false);
    }
}

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

package com.android.settings.privacy;

import android.content.Context;
import android.provider.DeviceConfig;

import androidx.annotation.NonNull;

import com.android.settings.core.BasePreferenceController;

/**
 * The preference controller for privacy hub top level preference.
 */
public final class PrivacyHubPreferenceController extends BasePreferenceController {
    public static final String PROPERTY_PRIVACY_HUB_ENABLED = "privacy_hub_enabled";

    public PrivacyHubPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                PROPERTY_PRIVACY_HUB_ENABLED, true) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }
}

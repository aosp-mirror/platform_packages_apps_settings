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

package com.android.settings.enterprise;

import static android.app.admin.DevicePolicyManager.DEVICE_OWNER_TYPE_FINANCED;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

/** Factory for creating the privacy settings preference for a managed device. */
public class PrivacySettingsPreferenceFactory {

    /**
     * Determines which preference to use in the Privacy Settings based off of the type of managed
     * device.
     */
    public static PrivacySettingsPreference createPrivacySettingsPreference(Context context) {
        if (isFinancedDevice(context)) {
            return createPrivacySettingsFinancedPreference(context);
        } else {
            return createPrivacySettingsEnterprisePreference(context);
        }
    }

    private static PrivacySettingsEnterprisePreference createPrivacySettingsEnterprisePreference(
            Context context) {
        return new PrivacySettingsEnterprisePreference(context);
    }

    private static PrivacySettingsFinancedPreference createPrivacySettingsFinancedPreference(
            Context context) {
        return new PrivacySettingsFinancedPreference(context);
    }

    private static boolean isFinancedDevice(Context context) {
        final DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        return dpm.isDeviceManaged() && dpm.getDeviceOwnerType(
                dpm.getDeviceOwnerComponentOnAnyUser()) == DEVICE_OWNER_TYPE_FINANCED;
    }
}

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
import static android.app.admin.DevicePolicyResources.Strings.Settings.MANAGED_DEVICE_INFO_SUMMARY;
import static android.app.admin.DevicePolicyResources.Strings.Settings.MANAGED_DEVICE_INFO_SUMMARY_WITH_NAME;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;

import java.util.Objects;

/** Helper class for the privacy preference in Settings for a managed device. */
class PrivacyPreferenceControllerHelper {

    private final Context mContext;
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;
    private final DevicePolicyManager mDevicePolicyManager;

    PrivacyPreferenceControllerHelper(Context context) {
        mContext = Objects.requireNonNull(context);
        mFeatureProvider = FeatureFactory.getFeatureFactory()
                .getEnterprisePrivacyFeatureProvider();
        mDevicePolicyManager = context.getSystemService(DevicePolicyManager.class);
    }

    /** Updates the privacy preference summary. */
    void updateState(Preference preference) {
        if (preference == null) {
            return;
        }

        final String organizationName = mFeatureProvider.getDeviceOwnerOrganizationName();
        if (organizationName == null) {
            preference.setSummary(mDevicePolicyManager.getResources().getString(
                    MANAGED_DEVICE_INFO_SUMMARY,
                    () -> mContext.getString(
                            R.string.enterprise_privacy_settings_summary_generic)));
        } else {
            preference.setSummary(mDevicePolicyManager.getResources().getString(
                    MANAGED_DEVICE_INFO_SUMMARY_WITH_NAME,
                    () -> mContext.getResources().getString(
                            R.string.enterprise_privacy_settings_summary_with_name,
                            organizationName), organizationName));
        }
    }

    /** Returns {@code true} if the device has a device owner. */
    boolean hasDeviceOwner() {
        return mFeatureProvider.hasDeviceOwner();
    }

    boolean isFinancedDevice() {
        return mDevicePolicyManager.isDeviceManaged() && mDevicePolicyManager.getDeviceOwnerType(
                mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser())
                == DEVICE_OWNER_TYPE_FINANCED;
    }
}

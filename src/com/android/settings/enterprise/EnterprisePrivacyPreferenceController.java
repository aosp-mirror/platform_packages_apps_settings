/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.enterprise;

import static android.app.admin.DevicePolicyManager.DEVICE_OWNER_TYPE_FINANCED;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

public class EnterprisePrivacyPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_ENTERPRISE_PRIVACY = "enterprise_privacy";
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public EnterprisePrivacyPreferenceController(Context context) {
        super(context);
        mFeatureProvider = FeatureFactory.getFactory(context)
                .getEnterprisePrivacyFeatureProvider(context);
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }
        if (isFinancedDevice()) {
            preference.setTitle(R.string.financed_privacy_settings);
        }

        final String organizationName = mFeatureProvider.getDeviceOwnerOrganizationName();
        if (organizationName == null) {
            preference.setSummary(R.string.enterprise_privacy_settings_summary_generic);
        } else {
            preference.setSummary(mContext.getResources().getString(
                    R.string.enterprise_privacy_settings_summary_with_name, organizationName));
        }
    }

    @Override
    public boolean isAvailable() {
        return mFeatureProvider.hasDeviceOwner();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ENTERPRISE_PRIVACY;
    }

    private boolean isFinancedDevice() {
        final DevicePolicyManager dpm = mContext.getSystemService(DevicePolicyManager.class);
        return dpm.isDeviceManaged() && dpm.getDeviceOwnerType(
                dpm.getDeviceOwnerComponentOnAnyUser()) == DEVICE_OWNER_TYPE_FINANCED;
    }
}

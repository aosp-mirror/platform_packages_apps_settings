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

import static android.app.admin.DevicePolicyResources.Strings.Settings.NUMBER_OF_DEVICE_ADMINS_NONE;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.utils.StringUtil;


public class ManageDeviceAdminPreferenceController extends BasePreferenceController {

    private final EnterprisePrivacyFeatureProvider mFeatureProvider;
    private final DevicePolicyManager mDevicePolicyManager;

    public ManageDeviceAdminPreferenceController(Context context, String key) {
        super(context, key);
        mFeatureProvider = FeatureFactory.getFeatureFactory()
                .getEnterprisePrivacyFeatureProvider();
        mDevicePolicyManager =
                mContext.getSystemService(DevicePolicyManager.class);
    }

    @Override
    public CharSequence getSummary() {
        final int activeAdmins
                = mFeatureProvider.getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile();

        if (activeAdmins == 0) {
            return mDevicePolicyManager.getResources().getString(NUMBER_OF_DEVICE_ADMINS_NONE,
                    () -> mContext.getResources().getString(R.string.number_of_device_admins_none));
        }

        // TODO: override
        return StringUtil.getIcuPluralsString(mContext, activeAdmins,
                R.string.number_of_device_admins);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_manage_device_admin)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

}

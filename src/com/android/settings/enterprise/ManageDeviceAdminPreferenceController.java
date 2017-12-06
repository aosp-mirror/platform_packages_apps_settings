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

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

public class ManageDeviceAdminPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_MANAGE_DEVICE_ADMIN = "manage_device_admin";
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public ManageDeviceAdminPreferenceController(Context context) {
        super(context);
        mFeatureProvider = FeatureFactory.getFactory(context)
                .getEnterprisePrivacyFeatureProvider(context);
    }

    @Override
    public void updateState(Preference preference) {
        final int activeAdmins
                = mFeatureProvider.getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile();
        preference.setSummary(activeAdmins == 0
                ? mContext.getResources().getString(R.string.number_of_device_admins_none)
                : mContext.getResources().getQuantityString(R.plurals.number_of_device_admins,
                        activeAdmins, activeAdmins));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_MANAGE_DEVICE_ADMIN;
    }
}

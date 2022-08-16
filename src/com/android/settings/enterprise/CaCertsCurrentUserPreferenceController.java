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

import static android.app.admin.DevicePolicyResources.Strings.Settings.CA_CERTS_DEVICE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CA_CERTS_PERSONAL_PROFILE;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;

public class CaCertsCurrentUserPreferenceController extends CaCertsPreferenceControllerBase {

    @VisibleForTesting
    static final String CA_CERTS_CURRENT_USER = "ca_certs_current_user";

    DevicePolicyManager mDevicePolicyManager;

    public CaCertsCurrentUserPreferenceController(Context context) {
        super(context);
        mDevicePolicyManager = mContext.getSystemService(DevicePolicyManager.class);
    }

    @Override
    public String getPreferenceKey() {
        return CA_CERTS_CURRENT_USER;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (mFeatureProvider.isInCompMode()) {
            preference.setTitle(mDevicePolicyManager.getResources().getString(
                    CA_CERTS_PERSONAL_PROFILE,
                    () -> mContext.getString(R.string.enterprise_privacy_ca_certs_personal)));
        } else {
            preference.setTitle(mDevicePolicyManager.getResources().getString(
                    CA_CERTS_DEVICE,
                    () -> mContext.getString(R.string.enterprise_privacy_ca_certs_device)));
        }
    }

    @Override
    protected int getNumberOfCaCerts() {
        return mFeatureProvider.getNumberOfOwnerInstalledCaCertsForCurrentUser();
    }
}

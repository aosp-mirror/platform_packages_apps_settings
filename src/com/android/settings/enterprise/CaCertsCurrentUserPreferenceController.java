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
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;

import com.android.settings.R;

import com.android.settingslib.core.lifecycle.Lifecycle;

public class CaCertsCurrentUserPreferenceController extends CaCertsPreferenceControllerBase {

    @VisibleForTesting
    static final String CA_CERTS_CURRENT_USER = "ca_certs_current_user";

    public CaCertsCurrentUserPreferenceController(Context context,
            Lifecycle lifecycle) {
        super(context, lifecycle);
    }

    @Override
    public String getPreferenceKey() {
        return CA_CERTS_CURRENT_USER;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setTitle(mFeatureProvider.isInCompMode()
                ? R.string.enterprise_privacy_ca_certs_personal
                : R.string.enterprise_privacy_ca_certs_device);
    }

    @Override
    protected int getNumberOfCaCerts() {
        return mFeatureProvider.getNumberOfOwnerInstalledCaCertsForCurrentUser();
    }
}

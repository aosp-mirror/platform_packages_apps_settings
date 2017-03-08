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
import android.content.res.Resources;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceController;
import com.android.settings.overlay.FeatureFactory;

public class CaCertsCurrentUserPreferenceController extends PreferenceController {

    private static final String CA_CERTS_CURRENT_USER = "ca_certs_current_user";
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public CaCertsCurrentUserPreferenceController(Context context) {
        super(context);
        mFeatureProvider = FeatureFactory.getFactory(context)
                .getEnterprisePrivacyFeatureProvider(context);
    }

    @Override
    public void updateState(Preference preference) {
        final int certs = mFeatureProvider.getNumberOfOwnerInstalledCaCertsInCurrentUser();
        if (certs == 0) {
            preference.setVisible(false);
            return;
        }
        preference.setTitle(mContext.getResources().getQuantityString(
                mFeatureProvider.isInCompMode() ? R.plurals.enterprise_privacy_ca_certs_personal :
                        R.plurals.enterprise_privacy_ca_certs_user, certs));
        preference.setSummary(mContext.getResources().getQuantityString(
                R.plurals.enterprise_privacy_number_ca_certs, certs, certs));
        preference.setVisible(true);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return CA_CERTS_CURRENT_USER;
    }
}

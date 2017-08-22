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
import com.android.settings.core.DynamicAvailabilityPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;

public abstract class CaCertsPreferenceControllerBase
        extends DynamicAvailabilityPreferenceController {

    protected final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public CaCertsPreferenceControllerBase(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
        mFeatureProvider = FeatureFactory.getFactory(context)
                .getEnterprisePrivacyFeatureProvider(context);
    }

    @Override
    public void updateState(Preference preference) {
        final int certs = getNumberOfCaCerts();
        preference.setSummary(mContext.getResources().getQuantityString(
                R.plurals.enterprise_privacy_number_ca_certs, certs, certs));
    }

    @Override
    public boolean isAvailable() {
        final boolean available = getNumberOfCaCerts() > 0;
        notifyOnAvailabilityUpdate(available);
        return available;
    }

    protected abstract int getNumberOfCaCerts();
}

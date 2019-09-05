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

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

public class AlwaysOnVpnCurrentUserPreferenceController
        extends AbstractPreferenceController implements PreferenceControllerMixin {

    private static final String KEY_ALWAYS_ON_VPN_PRIMARY_USER = "always_on_vpn_primary_user";
    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public AlwaysOnVpnCurrentUserPreferenceController(Context context) {
        super(context);
        mFeatureProvider = FeatureFactory.getFactory(context)
                .getEnterprisePrivacyFeatureProvider(context);
    }

    @Override
    public void updateState(Preference preference) {
        preference.setTitle(mFeatureProvider.isInCompMode()
                ? R.string.enterprise_privacy_always_on_vpn_personal
                : R.string.enterprise_privacy_always_on_vpn_device);
    }

    @Override
    public boolean isAvailable() {
        return mFeatureProvider.isAlwaysOnVpnSetInCurrentUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALWAYS_ON_VPN_PRIMARY_USER;
    }
}

/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.accounts;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.FooterPreferenceMixinCompat;

public class EnterpriseDisclosurePreferenceController extends BasePreferenceController {

    private final EnterprisePrivacyFeatureProvider mFeatureProvider;
    private FooterPreferenceMixinCompat mFooterPreferenceMixin;
    private PreferenceScreen mScreen;

    public EnterpriseDisclosurePreferenceController(Context context) {
        // Preference key doesn't matter as we are creating the preference in code.
        super(context, "add_account_enterprise_disclosure_footer");

        mFeatureProvider = FeatureFactory.getFactory(mContext)
                .getEnterprisePrivacyFeatureProvider(mContext);
    }

    public void setFooterPreferenceMixin(FooterPreferenceMixinCompat footerPreferenceMixin) {
        mFooterPreferenceMixin = footerPreferenceMixin;
    }

    @Override
    public int getAvailabilityStatus() {
        if (getDisclosure() == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        addEnterpriseDisclosure();
    }

    @VisibleForTesting
    CharSequence getDisclosure() {
        return mFeatureProvider.getDeviceOwnerDisclosure();
    }

    private void addEnterpriseDisclosure() {
        final CharSequence disclosure = getDisclosure();
        if (disclosure == null) {
            return;
        }
        final FooterPreference enterpriseDisclosurePreference =
                mFooterPreferenceMixin.createFooterPreference();
        enterpriseDisclosurePreference.setSelectable(false);
        enterpriseDisclosurePreference.setTitle(disclosure);
        mScreen.addPreference(enterpriseDisclosurePreference);
    }
}

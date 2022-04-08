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
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

public class EnterpriseDisclosurePreferenceController extends BasePreferenceController {

    private final EnterprisePrivacyFeatureProvider mFeatureProvider;

    public EnterpriseDisclosurePreferenceController(Context context, String key) {
        // Preference key doesn't matter as we are creating the preference in code.
        super(context, key);
        mFeatureProvider = FeatureFactory.getFactory(mContext)
                .getEnterprisePrivacyFeatureProvider(mContext);
    }

    @Override
    public int getAvailabilityStatus() {
        if (getDisclosure() == null) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @VisibleForTesting
    CharSequence getDisclosure() {
        return mFeatureProvider.getDeviceOwnerDisclosure();
    }

    @Override
    public void updateState(Preference preference) {
        final CharSequence disclosure = getDisclosure();
        if (disclosure == null) {
            return;
        }
        preference.setTitle(disclosure);
    }
}

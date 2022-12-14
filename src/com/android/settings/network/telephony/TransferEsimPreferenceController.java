/**
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.network.telephony;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

public class TransferEsimPreferenceController extends TelephonyBasePreferenceController {

    private Preference mPreference;
    private SubscriptionInfoEntity mSubscriptionInfoEntity;

    public TransferEsimPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void init(int subId, SubscriptionInfoEntity subInfoEntity) {
        mSubId = subId;
        mSubscriptionInfoEntity = subInfoEntity;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return CONDITIONALLY_UNAVAILABLE;
        // TODO(b/262195754): Need the intent to enabled the feature.
//        return mSubscriptionInfoEntity != null && mSubscriptionInfoEntity.isActiveSubscriptionId
//                && mSubscriptionInfoEntity.isEmbedded ? AVAILABLE
//                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        // Send intent to launch LPA
        return true;
    }

    @VisibleForTesting
    public void setSubscriptionInfoEntity(SubscriptionInfoEntity subscriptionInfoEntity) {
        mSubscriptionInfoEntity = subscriptionInfoEntity;
    }
}

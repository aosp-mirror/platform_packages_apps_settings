/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.sim;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialized version of SimListDialogFragment that fetches a list of SIMs which support calls.
 */
public class CallsSimListDialogFragment extends SimListDialogFragment {
    @Override
    protected List<SubscriptionInfo> getCurrentSubscriptions() {
        final Context context = getContext();
        final SubscriptionManager subscriptionManager = context.getSystemService(
                SubscriptionManager.class);
        final TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        final List<PhoneAccountHandle> phoneAccounts =
                telecomManager.getCallCapablePhoneAccounts();
        final List<SubscriptionInfo> result = new ArrayList<>();

        if (phoneAccounts == null) {
            return result;
        }
        for (PhoneAccountHandle handle : phoneAccounts) {
            final int subId = telephonyManager.getSubscriptionId(handle);

            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                continue;
            }
            result.add(subscriptionManager.getActiveSubscriptionInfo(subId));
        }
        return result;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_CALL_SIM_LIST;
    }
}

/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.accessibility.rtt;

import android.content.Context;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A util class checks some SIM card information and permissions.
 */
public abstract class TelecomUtil {

    private static final String TAG = "TelecomUtil";

    /** Get a list of phone accounts which are call capable. */
    public static List<PhoneAccountHandle> getCallCapablePhoneAccounts(Context context) {
        return Optional.ofNullable(getTelecomManager(context).getCallCapablePhoneAccounts())
                    .orElse(new ArrayList<>());
    }

    /** Returns a {@link TelecomManager} instance. */
    public static TelecomManager getTelecomManager(Context context) {
        return context.getApplicationContext().getSystemService(TelecomManager.class);
    }

    /** Returns a subscription id of the SIM. */
    public static int getSubIdForPhoneAccountHandle(
            Context context, PhoneAccountHandle phoneAccountHandle) {
        Optional<SubscriptionInfo> info = getSubscriptionInfo(context, phoneAccountHandle);
        return info.map(SubscriptionInfo::getSubscriptionId)
                .orElse(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    /**
     * @return the {@link SubscriptionInfo} of the SIM if {@code phoneAccountHandle} corresponds
     * to a valid SIM. Absent otherwise.
     */
    private static Optional<SubscriptionInfo> getSubscriptionInfo(
            Context context, PhoneAccountHandle phoneAccountHandle) {
        if (TextUtils.isEmpty(phoneAccountHandle.getId())) {
            return Optional.empty();
        }
        SubscriptionManager subscriptionManager = context.getSystemService(
                SubscriptionManager.class).createForAllUserProfiles();
        List<SubscriptionInfo> subscriptionInfos =
                subscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfos == null) {
            return Optional.empty();
        }
        for (SubscriptionInfo info : subscriptionInfos) {
            if (phoneAccountHandle.getId().startsWith(info.getIccId())) {
                return Optional.of(info);
            }
        }
        Log.d(TAG, "Failed to find SubscriptionInfo for phoneAccountHandle");
        return Optional.empty();
    }
}

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

package com.android.settings.network;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SubscriptionUtil {
    private static List<SubscriptionInfo> sAvailableResultsForTesting;
    private static List<SubscriptionInfo> sActiveResultsForTesting;

    @VisibleForTesting
    public static void setAvailableSubscriptionsForTesting(List<SubscriptionInfo> results) {
        sAvailableResultsForTesting = results;
    }

    @VisibleForTesting
    public static void setActiveSubscriptionsForTesting(List<SubscriptionInfo> results) {
        sActiveResultsForTesting = results;
    }

    public static List<SubscriptionInfo> getActiveSubscriptions(SubscriptionManager manager) {
        if (sActiveResultsForTesting != null) {
            return sActiveResultsForTesting;
        }
        List<SubscriptionInfo> subscriptions = manager.getActiveSubscriptionInfoList(true);
        if (subscriptions == null) {
            return new ArrayList<>();
        }
        return subscriptions;
    }

    public static List<SubscriptionInfo> getAvailableSubscriptions(SubscriptionManager manager) {
        if (sAvailableResultsForTesting != null) {
            return sAvailableResultsForTesting;
        }
        List<SubscriptionInfo> subscriptions = manager.getSelectableSubscriptionInfoList();
        if (subscriptions == null) {
            subscriptions = new ArrayList<>();
        }
        // With some carriers such as Google Fi which provide a sort of virtual service that spans
        // across multiple underlying networks, we end up with subscription entries for the
        // underlying networks that need to be hidden from the user in the UI.
        for (Iterator<SubscriptionInfo> iter = subscriptions.iterator(); iter.hasNext(); ) {
            SubscriptionInfo info = iter.next();
            if (TextUtils.isEmpty(info.getMncString())) {
                iter.remove();
            }
        }
        return subscriptions;
    }
}

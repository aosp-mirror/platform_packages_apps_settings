/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.core.BasePreferenceController;

/**
 * Class to show the footer that can't connect to 5G when device is in DSDS mode.
 */
public class NrDisabledInDsdsFooterPreferenceController extends BasePreferenceController {
    private int mSubId;

    /**
     * Constructor.
     */
    public NrDisabledInDsdsFooterPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    /**
     * Init and specify a subId.
     */
    public void init(int subId) {
        mSubId = subId;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        final TelephonyManager teleManager = ((TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE))
                .createForSubscriptionId(mSubId);
        final SubscriptionManager subManager = ((SubscriptionManager)
                mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE));
        final int[] activeSubIdList = subManager.getActiveSubscriptionIdList();
        final int activeSubCount = activeSubIdList == null ? 0 : activeSubIdList.length;
        // Show the footer only when DSDS is enabled, and mobile data is enabled on this SIM, and
        // 5G is supported on this device.
        if (teleManager.isDataEnabled() && activeSubCount >= 2 && is5GSupportedByRadio(teleManager)
                && !teleManager.canConnectTo5GInDsdsMode()) {
            return AVAILABLE;
        } else {
            return CONDITIONALLY_UNAVAILABLE;
        }
    }

    private boolean is5GSupportedByRadio(TelephonyManager tm) {
        return (tm.getSupportedRadioAccessFamily() & TelephonyManager.NETWORK_TYPE_BITMASK_NR) > 0;
    }
}

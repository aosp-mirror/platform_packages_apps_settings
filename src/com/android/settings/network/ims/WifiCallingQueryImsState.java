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

package com.android.settings.network.ims;

import android.content.Context;
import android.telephony.SubscriptionManager;

import com.android.ims.ImsManager;
import com.android.settings.network.SubscriptionUtil;

/**
 * Controller class for querying Wifi calling status
 */
public class WifiCallingQueryImsState extends ImsQueryController  {

    private Context mContext;
    private int mSubId;

    /**
     * Constructor
     *
     * @param context {@code Context}
     * @param subId subscription's id
     */
    public WifiCallingQueryImsState(Context context, int subId) {
        mContext = context;
        mSubId = subId;
    }

    /**
     * Get allowance status for user to alter configuration
     *
     * @return true when changing configuration by user is allowed.
     */
    public boolean isAllowUserControl() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }

        return ((!isSystemTtyEnabled(mContext).directQuery())
                || (isTtyOnVolteEnabled(mSubId).directQuery()));
    }

    /**
     * Get user's configuration
     *
     * @return true when user's configuration is ON otherwise false.
     */
    public boolean isEnabledByUser() {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        ImsManager imsManager = ImsManager.getInstance(mContext, SubscriptionUtil.getPhoneId(
                    mContext, mSubId));
        return imsManager.isWfcEnabledByUser();
    }
}

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

import android.telephony.ims.ImsMmTelManager;
import android.util.Log;


/**
 * An {@link ImsQuery} for accessing IMS VT enabled settings from user
 */
public class ImsQueryVtUserSetting implements ImsQuery {

    private static final String LOG_TAG = "QueryVtUserSetting";

    /**
     * Constructor
     * @param subId subscription id
     */
    public ImsQueryVtUserSetting(int subId) {
        mSubId = subId;
    }

    private volatile int mSubId;

    /**
     * Implementation of interface {@link ImsQuery#query()}
     *
     * @return result of query
     */
    public boolean query() {
        try {
            final ImsMmTelManager imsMmTelManager =
                    ImsMmTelManager.createForSubscriptionId(mSubId);
            return imsMmTelManager.isVtSettingEnabled();
        } catch (IllegalArgumentException exception) {
            Log.w(LOG_TAG, "fail to get VT settings. subId=" + mSubId, exception);
        }
        return false;
    }
}

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

package com.android.settings.network;

import android.content.Context;
import android.provider.Settings;
import android.telephony.SubscriptionManager;

/** Helper class to listen for changes in the enabled state of mobile data. */
public class MobileDataEnabledListener {
    private Context mContext;
    private Client mClient;
    private int mSubId;

    // There're 2 listeners both activated at the same time.
    // For project that access MOBILE_DATA, only first listener is functional.
    // For project that access "MOBILE_DATA + subId", first listener will be stopped when receiving
    // any onChange from second listener.

    private GlobalSettingsChangeListener mListener;
    private GlobalSettingsChangeListener mListenerForSubId;

    public interface Client {
        void onMobileDataEnabledChange();
    }

    /**
     * Constructor
     *
     * @param context of this listener
     * @param client callback when configuration changed
     */
    public MobileDataEnabledListener(Context context, Client client) {
        mContext = context;
        mClient = client;
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    /**
     * Starts listening to changes in the enabled state for data on the given subscription id.
     *
     * @param subId subscription id for enabled state of data subscription
     */
    public void start(int subId) {
        mSubId = subId;

        if (mListener == null) {
            mListener = new GlobalSettingsChangeListener(mContext,
                    Settings.Global.MOBILE_DATA) {
                public void onChanged(String field) {
                    mClient.onMobileDataEnabledChange();
                }
            };
        }
        stopMonitorSubIdSpecific();

        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return;
        }

        mListenerForSubId = new GlobalSettingsChangeListener(mContext,
                Settings.Global.MOBILE_DATA + mSubId) {
            public void onChanged(String field) {
                stopMonitor();
                mClient.onMobileDataEnabledChange();
            }
        };
    }

    /**
     * Get latest subscription id configured for listening
     *
     * @return subscription id
     */
    public int getSubId() {
        return mSubId;
    }

    /**
     * Stop listening to changes in the enabled state for data.
     */
    public void stop() {
        stopMonitor();
        stopMonitorSubIdSpecific();
    }

    private void stopMonitor() {
        if (mListener != null) {
            mListener.close();
            mListener = null;
        }
    }

    private void stopMonitorSubIdSpecific() {
        if (mListenerForSubId != null) {
            mListenerForSubId.close();
            mListenerForSubId = null;
        }
    }
}

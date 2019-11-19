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
 * limitations under the License.
 */

package com.android.settings.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.internal.telephony.TelephonyIntents;

import java.util.List;

/**
 * A listener for active subscription change
 */
public abstract class ActiveSubsciptionsListener
        extends SubscriptionManager.OnSubscriptionsChangedListener {

    private static final String TAG = "ActiveSubsciptions";

    /**
     * Constructor
     *
     * @param context of this listener
     */
    public ActiveSubsciptionsListener(Context context) {
        mContext = context;

        mSubscriptionChangeIntentFilter = new IntentFilter();
        mSubscriptionChangeIntentFilter.addAction(
                CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        mSubscriptionChangeIntentFilter.addAction(
                TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);

        mSubscriptionChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isInitialStickyBroadcast()) {
                    return;
                }
                final String action = intent.getAction();
                if (TextUtils.isEmpty(action)) {
                    return;
                }
                if (CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.equals(action)) {
                    final int subId = intent.getIntExtra(
                            CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX,
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                    if (!clearCachedSubId(subId)) {
                        return;
                    }
                }
                onSubscriptionsChanged();
            }
        };
    }

    private Context mContext;

    private boolean mIsMonitoringDataChange;
    private boolean mIsCachedDataAvailable;
    private SubscriptionManager mSubscriptionManager;

    private IntentFilter mSubscriptionChangeIntentFilter;

    @VisibleForTesting
    BroadcastReceiver mSubscriptionChangeReceiver;

    private Integer mMaxActiveSubscriptionInfos;
    private List<SubscriptionInfo> mCachedActiveSubscriptionInfo;

    /**
     * Active subscriptions got changed
     */
    public abstract void onChanged();

    @Override
    public void onSubscriptionsChanged() {
        // clear value in cache
        clearCache();
        listenerNotify();
    }

    /**
     * Start listening subscriptions change
     */
    public void start() {
        monitorSubscriptionsChange(true);
    }

    /**
     * Stop listening subscriptions change
     */
    public void stop() {
        monitorSubscriptionsChange(false);
    }

    /**
     * Get SubscriptionManager
     *
     * @return a SubscriptionManager
     */
    public SubscriptionManager getSubscriptionManager() {
        if (mSubscriptionManager == null) {
            mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        }
        return mSubscriptionManager;
    }

    /**
     * Get current max. number active subscription info(s) been setup within device
     *
     * @return max. number of active subscription info(s)
     */
    public int getActiveSubscriptionInfoCountMax() {
        int count = 0;
        if (mMaxActiveSubscriptionInfos == null) {
            count = getSubscriptionManager().getActiveSubscriptionInfoCountMax();
            if (mIsMonitoringDataChange) {
                mMaxActiveSubscriptionInfos = count;
            }
        } else {
            count = mMaxActiveSubscriptionInfos.intValue();
        }
        return count;
    }

    /**
     * Get a list of active subscription info
     *
     * @return A list of active subscription info
     */
    public List<SubscriptionInfo> getActiveSubscriptionsInfo() {
        if (mIsCachedDataAvailable) {
            return mCachedActiveSubscriptionInfo;
        }
        mIsCachedDataAvailable = mIsMonitoringDataChange;
        mCachedActiveSubscriptionInfo = getSubscriptionManager().getActiveSubscriptionInfoList();

        if ((mCachedActiveSubscriptionInfo == null)
                || (mCachedActiveSubscriptionInfo.size() <= 0)) {
            Log.d(TAG, "active subscriptions: " + mCachedActiveSubscriptionInfo);
        } else {
            final StringBuilder logString = new StringBuilder("active subscriptions:");
            for (SubscriptionInfo subInfo : mCachedActiveSubscriptionInfo) {
                logString.append(" " + subInfo.getSubscriptionId());
            }
            Log.d(TAG, logString.toString());
        }

        return mCachedActiveSubscriptionInfo;
    }

    /**
     * Get an active subscription info with given subscription ID
     *
     * @param subId target subscription ID
     * @return A subscription info which is active list
     */
    public SubscriptionInfo getActiveSubscriptionInfo(int subId) {
        final List<SubscriptionInfo> subInfoList = getActiveSubscriptionsInfo();
        if (subInfoList == null) {
            return null;
        }
        for (SubscriptionInfo subInfo : subInfoList) {
            if (subInfo.getSubscriptionId() == subId) {
                return subInfo;
            }
        }
        return null;
    }

    /**
     * Get a list of accessible subscription info
     *
     * @return A list of accessible subscription info
     */
    public List<SubscriptionInfo> getAccessibleSubscriptionsInfo() {
        return getSubscriptionManager().getAccessibleSubscriptionInfoList();
    }

    /**
     * Get an accessible subscription info with given subscription ID
     *
     * @param subId target subscription ID
     * @return A subscription info which is accessible list
     */
    public SubscriptionInfo getAccessibleSubscriptionInfo(int subId) {
        if (mIsCachedDataAvailable) {
            final SubscriptionInfo activeSubInfo = getActiveSubscriptionInfo(subId);
            if (activeSubInfo != null) {
                return activeSubInfo;
            }
        }

        final List<SubscriptionInfo> subInfoList = getAccessibleSubscriptionsInfo();
        if (subInfoList == null) {
            return null;
        }
        for (SubscriptionInfo subInfo : subInfoList) {
            if (subInfo.getSubscriptionId() == subId) {
                return subInfo;
            }
        }
        return null;
    }

    /**
     * Clear data cached within listener
     */
    public void clearCache() {
        mIsCachedDataAvailable = false;
        mMaxActiveSubscriptionInfos = null;
        mCachedActiveSubscriptionInfo = null;
    }

    private void monitorSubscriptionsChange(boolean on) {
        if (mIsMonitoringDataChange == on) {
            return;
        }
        mIsMonitoringDataChange = on;
        if (on) {
            mContext.registerReceiver(mSubscriptionChangeReceiver,
                    mSubscriptionChangeIntentFilter);
            getSubscriptionManager().addOnSubscriptionsChangedListener(this);
            listenerNotify();
        } else {
            mContext.unregisterReceiver(mSubscriptionChangeReceiver);
            getSubscriptionManager().removeOnSubscriptionsChangedListener(this);
            clearCache();
        }
    }

    private void listenerNotify() {
        if (!mIsMonitoringDataChange) {
            return;
        }
        onChanged();
    }

    private boolean clearCachedSubId(int subId) {
        if (!mIsCachedDataAvailable) {
            return false;
        }
        if (mCachedActiveSubscriptionInfo == null) {
            return false;
        }
        for (SubscriptionInfo subInfo : mCachedActiveSubscriptionInfo) {
            if (subInfo.getSubscriptionId() == subId) {
                clearCache();
                return true;
            }
        }
        return false;
    }
}

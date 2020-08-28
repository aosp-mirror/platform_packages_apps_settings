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
import android.os.Handler;
import android.os.Looper;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.internal.telephony.TelephonyIntents;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A listener for active subscription change
 */
public abstract class ActiveSubsciptionsListener
        extends SubscriptionManager.OnSubscriptionsChangedListener
        implements AutoCloseable {

    private static final String TAG = "ActiveSubsciptions";
    private static final boolean DEBUG = false;

    private Looper mLooper;
    private Context mContext;

    private static final int STATE_NOT_LISTENING = 0;
    private static final int STATE_STOPPING      = 1;
    private static final int STATE_PREPARING     = 2;
    private static final int STATE_LISTENING     = 3;
    private static final int STATE_DATA_CACHED   = 4;

    private AtomicInteger mCacheState;
    private SubscriptionManager mSubscriptionManager;

    private IntentFilter mSubscriptionChangeIntentFilter;
    private BroadcastReceiver mSubscriptionChangeReceiver;

    private static final int MAX_SUBSCRIPTION_UNKNOWN = -1;
    private final int mTargetSubscriptionId;

    private AtomicInteger mMaxActiveSubscriptionInfos;
    private List<SubscriptionInfo> mCachedActiveSubscriptionInfo;

    /**
     * Constructor
     *
     * @param looper {@code Looper} of this listener
     * @param context {@code Context} of this listener
     */
    public ActiveSubsciptionsListener(Looper looper, Context context) {
        this(looper, context, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    /**
     * Constructor
     *
     * @param looper {@code Looper} of this listener
     * @param context {@code Context} of this listener
     * @param subscriptionId for subscription on this listener
     */
    public ActiveSubsciptionsListener(Looper looper, Context context, int subscriptionId) {
        super(looper);
        mLooper = looper;
        mContext = context;
        mTargetSubscriptionId = subscriptionId;

        mCacheState = new AtomicInteger(STATE_NOT_LISTENING);
        mMaxActiveSubscriptionInfos = new AtomicInteger(MAX_SUBSCRIPTION_UNKNOWN);

        mSubscriptionChangeIntentFilter = new IntentFilter();
        mSubscriptionChangeIntentFilter.addAction(
                CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        mSubscriptionChangeIntentFilter.addAction(
                TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        mSubscriptionChangeIntentFilter.addAction(
                TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED);
    }

    @VisibleForTesting
    BroadcastReceiver getSubscriptionChangeReceiver() {
        return new BroadcastReceiver() {
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
                    if (SubscriptionManager.isValidSubscriptionId(mTargetSubscriptionId)) {
                        if (SubscriptionManager.isValidSubscriptionId(subId)
                                && (mTargetSubscriptionId != subId)) {
                            return;
                        }
                    }
                }
                onSubscriptionsChanged();
            }
        };
    }

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
     * Implementation of {@code AutoCloseable}
     */
    public void close() {
        stop();
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
        int cacheState = mCacheState.get();
        if (cacheState < STATE_LISTENING) {
            return getSubscriptionManager().getActiveSubscriptionInfoCountMax();
        }

        mMaxActiveSubscriptionInfos.compareAndSet(MAX_SUBSCRIPTION_UNKNOWN,
                getSubscriptionManager().getActiveSubscriptionInfoCountMax());
        return mMaxActiveSubscriptionInfos.get();
    }

    /**
     * Get a list of active subscription info
     *
     * @return A list of active subscription info
     */
    public List<SubscriptionInfo> getActiveSubscriptionsInfo() {
        if (mCacheState.get() >= STATE_DATA_CACHED) {
            return mCachedActiveSubscriptionInfo;
        }
        mCachedActiveSubscriptionInfo = getSubscriptionManager().getActiveSubscriptionInfoList();
        mCacheState.compareAndSet(STATE_LISTENING, STATE_DATA_CACHED);

        if (DEBUG) {
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
     * Get a list of all subscription info which accessible by Settings app
     *
     * @return A list of accessible subscription info
     */
    public List<SubscriptionInfo> getAccessibleSubscriptionsInfo() {
        return getSubscriptionManager().getAvailableSubscriptionInfoList();
    }

    /**
     * Get an accessible subscription info with given subscription ID
     *
     * @param subId target subscription ID
     * @return A subscription info which is accessible list
     */
    public SubscriptionInfo getAccessibleSubscriptionInfo(int subId) {
        // Always check if subId is part of activeSubscriptions
        // since there's cache design within SubscriptionManager.
        // That give us a chance to avoid from querying ContentProvider.
        final SubscriptionInfo activeSubInfo = getActiveSubscriptionInfo(subId);
        if (activeSubInfo != null) {
            return activeSubInfo;
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
        mMaxActiveSubscriptionInfos.set(MAX_SUBSCRIPTION_UNKNOWN);
        mCacheState.compareAndSet(STATE_DATA_CACHED, STATE_LISTENING);
        mCachedActiveSubscriptionInfo = null;
    }

    @VisibleForTesting
    void registerForSubscriptionsChange() {
        getSubscriptionManager().addOnSubscriptionsChangedListener(
                mContext.getMainExecutor(), this);
    }

    private void monitorSubscriptionsChange(boolean on) {
        if (on) {
            if (!mCacheState.compareAndSet(STATE_NOT_LISTENING, STATE_PREPARING)) {
                return;
            }

            if (mSubscriptionChangeReceiver == null) {
                mSubscriptionChangeReceiver = getSubscriptionChangeReceiver();
            }
            mContext.registerReceiver(mSubscriptionChangeReceiver,
                    mSubscriptionChangeIntentFilter, null, new Handler(mLooper));
            registerForSubscriptionsChange();
            mCacheState.compareAndSet(STATE_PREPARING, STATE_LISTENING);
            return;
        }

        final int currentState = mCacheState.getAndSet(STATE_STOPPING);
        if (currentState <= STATE_STOPPING) {
            mCacheState.compareAndSet(STATE_STOPPING, currentState);
            return;
        }
        if (mSubscriptionChangeReceiver != null) {
            mContext.unregisterReceiver(mSubscriptionChangeReceiver);
        }
        getSubscriptionManager().removeOnSubscriptionsChangedListener(this);
        clearCache();
        mCacheState.compareAndSet(STATE_STOPPING, STATE_NOT_LISTENING);
    }

    private void listenerNotify() {
        if (mCacheState.get() < STATE_LISTENING) {
            return;
        }
        onChanged();
    }

    private boolean clearCachedSubId(int subId) {
        if (mCacheState.get() < STATE_DATA_CACHED) {
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

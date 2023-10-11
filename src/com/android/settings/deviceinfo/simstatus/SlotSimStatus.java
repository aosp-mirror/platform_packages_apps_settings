/*
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

package com.android.settings.deviceinfo.simstatus;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.android.settings.network.SubscriptionsChangeListener;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A class for showing a summary of status of sim slots.
 */
public class SlotSimStatus extends LiveData<Long>
        implements DefaultLifecycleObserver,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private static final String TAG = "SlotSimStatus";

    private final AtomicInteger mNumberOfSlots = new AtomicInteger(0);
    private final ConcurrentHashMap<Integer, SubscriptionInfo> mSubscriptionMap =
            new ConcurrentHashMap<Integer, SubscriptionInfo>();
    private final Phaser mBlocker = new Phaser(1);
    private final AtomicLong mDataVersion = new AtomicLong(0);

    private Context mContext;
    private int mBasePreferenceOrdering;
    private SubscriptionsChangeListener mSubscriptionsChangeListener;

    private static final String KEY_SIM_STATUS = "sim_status";

    /**
     * Construct of class.
     * @param context Context
     */
    public SlotSimStatus(Context context) {
        this(context, null, null);
    }

    /**
     * Construct of class.
     * @param context Context
     * @param executor executor for offload to thread
     * @param lifecycle Lifecycle
     */
    public SlotSimStatus(Context context, Executor executor, Lifecycle lifecycle) {
        mContext = context;
        if (executor == null) {
            queryRecords(context);
        } else {
            executor.execute(() -> asyncQueryRecords(context));
        }
        if (lifecycle != null) {
            lifecycle.addObserver(this);
            mSubscriptionsChangeListener = new SubscriptionsChangeListener(context, this);
            mSubscriptionsChangeListener.start();
        }
    }

    protected void queryRecords(Context context) {
        queryDetails(context);
        setValue(mDataVersion.incrementAndGet());
        mBlocker.arrive();
    }

    protected void asyncQueryRecords(Context context) {
        queryDetails(context);
        postValue(mDataVersion.incrementAndGet());
        mBlocker.arrive();
    }

    protected void updateRecords() {
        queryDetails(mContext);
        setValue(mDataVersion.incrementAndGet());
    }

    protected void queryDetails(Context context) {
        TelephonyManager telMgr = context.getSystemService(TelephonyManager.class);
        if (telMgr != null) {
            mNumberOfSlots.set(telMgr.getPhoneCount());
        }

        SubscriptionManager subMgr = context.getSystemService(SubscriptionManager.class);
        if (subMgr == null) {
            mSubscriptionMap.clear();
            return;
        }

        List<SubscriptionInfo> subInfoList = subMgr.getActiveSubscriptionInfoList();
        if ((subInfoList == null) || (subInfoList.size() <= 0)) {
            mSubscriptionMap.clear();
            Log.d(TAG, "No active SIM.");
            return;
        }

        mSubscriptionMap.clear();
        subInfoList.forEach(subInfo -> {
            int slotIndex = subInfo.getSimSlotIndex();
            mSubscriptionMap.put(slotIndex, subInfo);
        });
        Log.d(TAG, "Number of active SIM: " + subInfoList.size());
    }

    protected void waitForResult() {
        mBlocker.awaitAdvance(0);
    }

    /**
     * Set base ordering of Preference.
     * @param baseOrdering the base ordering for SIM Status within "About Phone".
     */
    public void setBasePreferenceOrdering(int baseOrdering) {
        mBasePreferenceOrdering = baseOrdering;
    }

    /**
     * Number of slots available.
     * @return number of slots
     */
    public int size() {
        waitForResult();
        return mNumberOfSlots.get();
    }

    /**
     * Get ordering of Preference based on index of slot.
     * @param slotIndex index of slot
     * @return Preference ordering.
     */
    public int getPreferenceOrdering(int slotIndex) {
        return mBasePreferenceOrdering + 1 + slotIndex;
    }

    /**
     * Get key of Preference and PreferenceController based on index of slot.
     * @param slotIndex index of slot
     * @return Preference key.
     */
    public String getPreferenceKey(int slotIndex) {
        if (slotIndex == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            return KEY_SIM_STATUS;
        }
        return KEY_SIM_STATUS + (1 + slotIndex);
    }

    /**
     * Get subscription based on slot index.
     * @param slotIndex index of slot (starting from 0)
     * @return SubscriptionInfo based on index of slot.
     *         {@code null} means no subscription on slot.
     */
    public SubscriptionInfo getSubscriptionInfo(int slotIndex) {
        if (slotIndex >= size()) {
            return null;
        }
        return mSubscriptionMap.get(slotIndex);
    }

    /**
     * Get slot index based on Preference key
     * @param prefKey is the preference key
     * @return slot index.
     */
    public int findSlotIndexByKey(String prefKey) {
        int simSlotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX + 1;
        try {
            simSlotIndex = Integer.parseInt(prefKey.substring(KEY_SIM_STATUS.length()));
        } catch (Exception exception) {
            Log.w(TAG, "Preference key invalid: " + prefKey +
                       ". Error Msg: " + exception.getMessage());
        }
        return simSlotIndex - 1;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        if (airplaneModeEnabled) {
            /**
             * Only perform update when airplane mode ON.
             * Relay on #onSubscriptionsChanged() when airplane mode OFF.
             */
            updateRecords();
        }
    }

    @Override
    public void onSubscriptionsChanged() {
        updateRecords();
    }

    @Override
    public void onDestroy(LifecycleOwner lifecycleOwner) {
        if (mSubscriptionsChangeListener != null) {
            mSubscriptionsChangeListener.stop();
        }
        lifecycleOwner.getLifecycle().removeObserver(this);
    }
}

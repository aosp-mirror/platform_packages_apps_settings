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
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class for showing a summary of status of sim slots.
 */
public class SlotSimStatus {

    private static final String TAG = "SlotSimStatus";

    private final AtomicInteger mNumberOfSlots = new AtomicInteger(0);
    private final Phaser mBlocker = new Phaser(1);
    private int mBasePreferenceOrdering;

    private static final String KEY_SIM_STATUS = "sim_status";

    /**
     * Construct of class.
     * @param context Context
     */
    public SlotSimStatus(Context context) {
        this(context, null);
    }

    /**
     * Construct of class.
     * @param context Context
     * @param executor executor for offload to thread
     */
    public SlotSimStatus(Context context, Executor executor) {
        if (executor == null) {
            queryRecords(context);
        } else {
            executor.execute(() -> queryRecords(context));
        }
    }

    protected void queryRecords(Context context) {
        TelephonyManager telMgr = context.getSystemService(TelephonyManager.class);
        if (telMgr != null) {
            mNumberOfSlots.set(telMgr.getPhoneCount());
        }
        mBlocker.arrive();
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
}

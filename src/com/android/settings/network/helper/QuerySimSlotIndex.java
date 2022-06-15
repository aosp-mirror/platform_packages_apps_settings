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
package com.android.settings.network.helper;

import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * This is a Callable class which query slot index within device
 */
public class QuerySimSlotIndex implements Callable<AtomicIntegerArray> {
    private static final String TAG = "QuerySimSlotIndex";

    private TelephonyManager mTelephonyManager;
    private boolean mDisabledSlotsIncluded;
    private boolean mOnlySlotWithSim;

    /**
     * Constructor of class
     * @param TelephonyManager
     * @param disabledSlotsIncluded query both active and inactive slots when true,
     *                              only query active slot when false.
     * @param onlySlotWithSim query slot index with SIM available when true,
     *                        include absent ones when false.
     */
    public QuerySimSlotIndex(TelephonyManager telephonyManager,
            boolean disabledSlotsIncluded, boolean onlySlotWithSim) {
        mTelephonyManager = telephonyManager;
        mDisabledSlotsIncluded = disabledSlotsIncluded;
        mOnlySlotWithSim = onlySlotWithSim;
    }

    /**
     * Implementation of Callable
     * @return slot index in AtomicIntegerArray
     */
    public AtomicIntegerArray call() {
        UiccSlotInfo [] slotInfo = mTelephonyManager.getUiccSlotsInfo();
        if (slotInfo == null) {
            return new AtomicIntegerArray(0);
        }
        int slotIndexFilter = mOnlySlotWithSim ? 0 : SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        return new AtomicIntegerArray(Arrays.stream(slotInfo)
                .filter(slot -> filterSlot(slot))
                .mapToInt(slot -> mapToSlotIndex(slot))
                .filter(slotIndex -> (slotIndex >= slotIndexFilter))
                .toArray());
    }

    protected boolean filterSlot(UiccSlotInfo slotInfo) {
        if (mDisabledSlotsIncluded) {
            return true;
        }
        if (slotInfo == null) {
            return false;
        }
        return slotInfo.getIsActive();
    }

    protected int mapToSlotIndex(UiccSlotInfo slotInfo) {
        if (slotInfo == null) {
            return SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        }
        if (slotInfo.getCardStateInfo() == UiccSlotInfo.CARD_STATE_INFO_ABSENT) {
            return SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        }
        return slotInfo.getLogicalSlotIdx();
    }
}
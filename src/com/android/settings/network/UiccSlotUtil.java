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

package com.android.settings.network;

import android.annotation.IntDef;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;
import android.util.Log;

import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UiccSlotUtil {

    private static final String TAG = "UiccSlotUtil";

    private static final long DEFAULT_WAIT_AFTER_SWITCH_TIMEOUT_MILLIS = 25 * 1000L;
    ;

    public static final int INVALID_PHYSICAL_SLOT_ID = -1;

    /**
     * Mode for switching to eSIM slot which decides whether there is cleanup process, e.g.
     * disabling test profile, after eSIM slot is activated and whether we will wait it finished.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        SwitchingEsimMode.NO_CLEANUP,
        SwitchingEsimMode.ASYNC_CLEANUP,
        SwitchingEsimMode.SYNC_CLEANUP
    })
    public @interface SwitchingEsimMode {
        /** No cleanup process after switching to eSIM slot */
        int NO_CLEANUP = 0;
        /** Has cleanup process, but we will not wait it finished. */
        int ASYNC_CLEANUP = 1;
        /** Has cleanup process and we will wait until it's finished */
        int SYNC_CLEANUP = 2;
    }

    /**
     * Returns an immutable list of all UICC slots. If TelephonyManager#getUiccSlotsInfo returns, it
     * returns an empty list instead.
     */
    public static ImmutableList<UiccSlotInfo> getSlotInfos(TelephonyManager telMgr) {
        UiccSlotInfo[] slotInfos = telMgr.getUiccSlotsInfo();
        if (slotInfos == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(slotInfos);
    }

    /**
     * Switches to the removable slot. It waits for SIM_STATE_LOADED after switch. If slotId is
     * INVALID_PHYSICAL_SLOT_ID, the method will use the first detected inactive removable slot.
     *
     * @param slotId the physical removable slot id.
     * @param context the application context.
     * @throws UiccSlotsException if there is an error.
     */
    public static synchronized void switchToRemovableSlot(int slotId, Context context)
            throws UiccSlotsException {
        if (ThreadUtils.isMainThread()) {
            throw new IllegalThreadStateException(
                    "Do not call switchToRemovableSlot on the main thread.");
        }
        TelephonyManager telMgr = context.getSystemService(TelephonyManager.class);
        if (telMgr.isMultiSimEnabled()) {
            // If this device supports multiple active slots, don't mess with TelephonyManager.
            Log.i(TAG, "Multiple active slots supported. Not calling switchSlots.");
            return;
        }
        UiccSlotInfo[] slots = telMgr.getUiccSlotsInfo();
        if (slotId == INVALID_PHYSICAL_SLOT_ID) {
            for (int i = 0; i < slots.length; i++) {
                if (slots[i].isRemovable()
                        && !slots[i].getIsActive()
                        && slots[i].getCardStateInfo() != UiccSlotInfo.CARD_STATE_INFO_ERROR
                        && slots[i].getCardStateInfo() != UiccSlotInfo.CARD_STATE_INFO_RESTRICTED) {
                    performSwitchToRemovableSlot(i, context);
                    return;
                }
            }
        } else {
            if (slotId >= slots.length || !slots[slotId].isRemovable()) {
                throw new UiccSlotsException("The given slotId is not a removable slot: " + slotId);
            }
            if (!slots[slotId].getIsActive()) {
                performSwitchToRemovableSlot(slotId, context);
            }
        }
    }

    private static void performSwitchToRemovableSlot(int slotId, Context context)
            throws UiccSlotsException {
        CarrierConfigChangedReceiver receiver = null;
        long waitingTimeMillis =
                Settings.Global.getLong(
                        context.getContentResolver(),
                        Settings.Global.EUICC_SWITCH_SLOT_TIMEOUT_MILLIS,
                        DEFAULT_WAIT_AFTER_SWITCH_TIMEOUT_MILLIS);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            receiver = new CarrierConfigChangedReceiver(latch);
            receiver.registerOn(context);
            switchSlots(context, slotId);
            latch.await(waitingTimeMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "Failed switching to physical slot.", e);
        } finally {
            if (receiver != null) {
                context.unregisterReceiver(receiver);
            }
        }
    }

    /**
     * Changes the logical slot to physical slot mapping. OEM should override this to provide
     * device-specific implementation if the device supports switching slots.
     *
     * @param context the application context.
     * @param physicalSlots List of physical slot ids in the order of logical slots.
     */
    private static void switchSlots(Context context, int... physicalSlots)
            throws UiccSlotsException {
        TelephonyManager telMgr = context.getSystemService(TelephonyManager.class);
        if (telMgr.isMultiSimEnabled()) {
            // If this device supports multiple active slots, don't mess with TelephonyManager.
            Log.i(TAG, "Multiple active slots supported. Not calling switchSlots.");
            return;
        }
        if (!telMgr.switchSlots(physicalSlots)) {
            throw new UiccSlotsException("Failed to switch slots");
        }
    }
}

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
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;
import android.telephony.UiccSlotMapping;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.ImmutableList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// ToDo: to do the refactor for renaming
public class UiccSlotUtil {

    private static final String TAG = "UiccSlotUtil";

    private static final long DEFAULT_WAIT_AFTER_SWITCH_TIMEOUT_MILLIS = 25 * 1000L;

    public static final int INVALID_PHYSICAL_SLOT_ID = -1;
    public static final int INVALID_PORT_ID = -1;

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
     * @param slotId  the physical removable slot id.
     * @param context the application context.
     * @throws UiccSlotsException if there is an error.
     */
    //ToDo: delete this api and refactor the related code.
    public static synchronized void switchToRemovableSlot(int slotId, Context context)
            throws UiccSlotsException {
        switchToRemovableSlot(context, slotId, null);
    }

    /**
     * Switches to the removable slot. It waits for SIM_STATE_LOADED after switch. If slotId is
     * INVALID_PHYSICAL_SLOT_ID, the method will use the first detected inactive removable slot.
     *
     * @param slotId  the physical removable slot id.
     * @param context the application context.
     * @param removedSubInfo In the DSDS+MEP mode, if the all of slots have sims, it should
     *                        remove the one of active sim.
     *                       If the removedSubInfo is null, then use the default value.
     *                       The default value is the esim slot and portId 0.
     * @throws UiccSlotsException if there is an error.
     */
    public static synchronized void switchToRemovableSlot(Context context, int slotId,
            SubscriptionInfo removedSubInfo) throws UiccSlotsException {
        if (ThreadUtils.isMainThread()) {
            throw new IllegalThreadStateException(
                    "Do not call switchToRemovableSlot on the main thread.");
        }
        TelephonyManager telMgr = context.getSystemService(TelephonyManager.class);
        int inactiveRemovableSlot = getInactiveRemovableSlot(telMgr.getUiccSlotsInfo(), slotId);
        Log.i(TAG, "The InactiveRemovableSlot: " + inactiveRemovableSlot);

        Collection<UiccSlotMapping> uiccSlotMappings = telMgr.getSimSlotMapping();
        Log.i(TAG, "The SimSlotMapping: " + uiccSlotMappings);

        if (inactiveRemovableSlot == INVALID_PHYSICAL_SLOT_ID) {
            // The slot is invalid slot id, then to skip this.
            // The slot is active, then the sim can enable directly.
            return;
        }

        performSwitchToSlot(telMgr,
                prepareUiccSlotMappings(uiccSlotMappings,
                        /*slot is psim*/ true,
                        inactiveRemovableSlot,
                        /*removable sim's port Id*/ TelephonyManager.DEFAULT_PORT_INDEX,
                        removedSubInfo,
                        telMgr.isMultiSimEnabled()),
                context);
    }

    /**
     * Switches to the Euicc slot. It waits for SIM_STATE_LOADED after switch.
     *
     * @param context the application context.
     * @param physicalSlotId the Euicc slot id.
     * @param port the Euicc slot port id.
     * @param removedSubInfo In the DSDS+MEP mode, if the all of slots have sims, it should
     *                       remove the one of active sim.
     *                       If the removedSubInfo is null, then it uses the default value.
     *                       The default value is the esim slot and portId 0.
     * @throws UiccSlotsException if there is an error.
     */
    public static synchronized void switchToEuiccSlot(Context context, int physicalSlotId, int port,
            SubscriptionInfo removedSubInfo) throws UiccSlotsException {
        if (ThreadUtils.isMainThread()) {
            throw new IllegalThreadStateException(
                    "Do not call switchToRemovableSlot on the main thread.");
        }
        TelephonyManager telMgr = context.getSystemService(TelephonyManager.class);
        Collection<UiccSlotMapping> uiccSlotMappings = telMgr.getSimSlotMapping();
        Log.i(TAG, "The SimSlotMapping: " + uiccSlotMappings);

        if (isTargetSlotActive(uiccSlotMappings, physicalSlotId, port)) {
            Log.i(TAG, "The slot is active, then the sim can enable directly.");
            return;
        }

        performSwitchToSlot(telMgr,
                prepareUiccSlotMappings(uiccSlotMappings, /*slot is not psim*/ false,
                        physicalSlotId, port, removedSubInfo, telMgr.isMultiSimEnabled()),
                context);
    }

    /**
     * @param context the application context.
     * @return the esim slot. If the value is -1, there is not the esim.
     */
    public static int getEsimSlotId(Context context) {
        TelephonyManager telMgr = context.getSystemService(TelephonyManager.class);
        ImmutableList<UiccSlotInfo> slotInfos = UiccSlotUtil.getSlotInfos(telMgr);
        int firstEsimSlot = IntStream.range(0, slotInfos.size())
                .filter(
                        index -> {
                            UiccSlotInfo slotInfo = slotInfos.get(index);
                            if (slotInfo == null) {
                                return false;
                            }
                            return !slotInfo.isRemovable();
                        })
                .findFirst().orElse(-1);

        Log.i(TAG, "firstEsimSlot: " + firstEsimSlot);
        return firstEsimSlot;
    }

    private static boolean isTargetSlotActive(Collection<UiccSlotMapping> uiccSlotMappings,
            int physicalSlotId, int port) {
        return uiccSlotMappings.stream()
                .anyMatch(
                        uiccSlotMapping -> uiccSlotMapping.getPhysicalSlotIndex() == physicalSlotId
                                && uiccSlotMapping.getPortIndex() == port);
    }

    private static void performSwitchToSlot(TelephonyManager telMgr,
            Collection<UiccSlotMapping> uiccSlotMappings, Context context)
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
            telMgr.setSimSlotMapping(uiccSlotMappings);
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
     * @param slots  The UiccSlotInfo list.
     * @param slotId The physical removable slot id.
     * @return The inactive physical removable slot id. If the physical removable slot id is
     * active, then return -1.
     * @throws UiccSlotsException if there is an error.
     */
    private static int getInactiveRemovableSlot(UiccSlotInfo[] slots, int slotId)
            throws UiccSlotsException {
        if (slots == null) {
            throw new UiccSlotsException("UiccSlotInfo is null");
        }
        if (slotId == INVALID_PHYSICAL_SLOT_ID) {
            for (int i = 0; i < slots.length; i++) {
                if (slots[i].isRemovable()
                        && !slots[i].getPorts().stream().findFirst().get().isActive()
                        && slots[i].getCardStateInfo() != UiccSlotInfo.CARD_STATE_INFO_ERROR
                        && slots[i].getCardStateInfo() != UiccSlotInfo.CARD_STATE_INFO_RESTRICTED) {
                    return i;
                }
            }
        } else {
            if (slotId >= slots.length || !slots[slotId].isRemovable()) {
                throw new UiccSlotsException("The given slotId is not a removable slot: " + slotId);
            }
            if (!slots[slotId].getPorts().stream().findFirst().get().isActive()) {
                return slotId;
            }
        }
        return INVALID_PHYSICAL_SLOT_ID;
    }

    // Device |                                        |Slot   |
    // Working|                                        |Mapping|
    // State  |Type                                    |Mode   |Friendly name
    //--------------------------------------------------------------------------
    // Single |SIM pSIM [RIL 0]                        |1      |pSIM active
    // Single |SIM MEP Port #0 [RIL0]                  |2      |eSIM Port0 active
    // Single |SIM MEP Port #1 [RIL0]                  |2.1    |eSIM Port1 active
    // DSDS   |pSIM [RIL 0] + MEP Port #0 [RIL 1]      |3      |pSIM+Port0
    // DSDS   |pSIM [RIL 0] + MEP Port #1 [RIL 1]      |3.1    |pSIM+Port1
    // DSDS   |MEP Port #0 [RIL 0] + MEP Port #1 [RIL1]|3.2    |Dual-Ports-A
    // DSDS   |MEP Port #1 [RIL 0] + MEP Port #0 [RIL1]|4      |Dual-Ports-B
    //
    // The rules are:
    // 1. pSIM's logical slots always is [RIL 0].
    // 2. assign the new active port to the same stack that will be de-activated
    //    For example: mode#3->mode#4

    @VisibleForTesting
    static Collection<UiccSlotMapping> prepareUiccSlotMappings(
            Collection<UiccSlotMapping> uiccSlotMappings, boolean isPsim, int physicalSlotId,
            int port, SubscriptionInfo removedSubInfo, boolean isMultiSimEnabled) {
        Collection<UiccSlotMapping> newUiccSlotMappings = new ArrayList<>();
        if (!isMultiSimEnabled) {
            // In the 'SS mode', the port is 0.
            newUiccSlotMappings.add(new UiccSlotMapping(port, physicalSlotId, 0));
        } else if (removedSubInfo != null) {
            // DSDS+MEP
            // The target slot+port is not active, but the all of logical slots are full. It
            // needs to replace one of logical slots.
            Log.i(TAG,
                    String.format(
                            "Start to set SimSlotMapping from subId%d(LogicalSlot%d-Port%d) to "
                                    + "PhysicalSlotId%d-Port%d",
                            removedSubInfo.getSubscriptionId(), removedSubInfo.getSimSlotIndex(),
                            removedSubInfo.getPortIndex(), physicalSlotId, port));

            int logicalSlotIndex = 0;
            if (isPsim) {
                // The target slot is psim
                newUiccSlotMappings.add(
                        new UiccSlotMapping(port, physicalSlotId, logicalSlotIndex++));
            }
            Collection<UiccSlotMapping> tempUiccSlotMappings =
                    uiccSlotMappings.stream()
                            .sorted(Comparator.comparingInt(UiccSlotMapping::getLogicalSlotIndex))
                            .collect(Collectors.toList());
            for (UiccSlotMapping uiccSlotMapping : tempUiccSlotMappings) {
                if (isSubInfoMappingIntoUiccSlotMapping(uiccSlotMapping, removedSubInfo)) {
                    if (!isPsim) {
                        // Replace this uiccSlotMapping
                        newUiccSlotMappings.add(new UiccSlotMapping(port, physicalSlotId,
                                uiccSlotMapping.getLogicalSlotIndex()));
                    }
                    continue;
                }

                // If the psim is inserted, then change the
                // logicalSlotIndex for another uiccSlotMappings.
                newUiccSlotMappings.add(isPsim
                        ? new UiccSlotMapping(
                                uiccSlotMapping.getPortIndex(),
                                uiccSlotMapping.getPhysicalSlotIndex(),
                                logicalSlotIndex++
                        ) : uiccSlotMapping);
            }
        } else {
            // For no inserted psim case in DSDS+MEP, there is only one esim in device and
            // then user inserts another esim in DSDS+MEP.
            // If the target is esim, then replace the psim.
            Log.i(TAG, "The removedSubInfo is null");
            newUiccSlotMappings =
                    uiccSlotMappings.stream().map(uiccSlotMapping -> {
                        if (!isPsim && uiccSlotMapping.getPhysicalSlotIndex() != physicalSlotId) {
                            return new UiccSlotMapping(port, physicalSlotId,
                                    uiccSlotMapping.getLogicalSlotIndex());
                        }
                        return uiccSlotMapping;
                    }).collect(Collectors.toList());
        }

        Log.i(TAG, "The SimSlotMapping: " + newUiccSlotMappings);
        return newUiccSlotMappings;
    }

    private static boolean isSubInfoMappingIntoUiccSlotMapping(UiccSlotMapping uiccSlotMapping,
            SubscriptionInfo subscriptionInfo) {
        return uiccSlotMapping != null
                && uiccSlotMapping.getLogicalSlotIndex() == subscriptionInfo.getSimSlotIndex()
                && uiccSlotMapping.getPortIndex() == subscriptionInfo.getPortIndex();
    }
}

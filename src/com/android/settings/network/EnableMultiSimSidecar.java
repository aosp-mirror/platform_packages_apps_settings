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

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccPortInfo;
import android.telephony.UiccSlotInfo;
import android.util.ArraySet;
import android.util.Log;

import com.android.settings.AsyncTaskSidecar;
import com.android.settings.SidecarFragment;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * {@code EnableMultiSimSidecar} enables multi SIM on the device. It should only be called for
 * Android R+. After {@code run} is called, it sets the configuration on modem side to enable
 * multiple SIMs. Once the configuration is set successfully, it will listen to UICC card changes
 * until {@code TelMan.EXTRA_ACTIVE_SIM_SUPPORTED_COUNT} matches {@code mNumOfActiveSim} or timeout.
 */
public class EnableMultiSimSidecar extends AsyncTaskSidecar<Void, Boolean> {

    // Tags
    private static final String TAG = "EnableMultiSimSidecar";

    private static final long DEFAULT_ENABLE_MULTI_SIM_TIMEOUT_MILLS = 40 * 1000L;

    public static EnableMultiSimSidecar get(FragmentManager fm) {
        return SidecarFragment.get(fm, TAG, EnableMultiSimSidecar.class, null /* args */);
    }

    final CountDownLatch mSimCardStateChangedLatch = new CountDownLatch(1);
    private TelephonyManager mTelephonyManager;
    private int mNumOfActiveSim = 0;

    private final BroadcastReceiver mCarrierConfigChangeReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int readySimsCount = getReadySimsCount();
                    int activePortsCount = getActivePortsCount();
                    // If the number of ready SIM count and active ports equal to the number of SIMs
                    // need to be activated, the device is successfully switched to multiple active
                    // SIM mode.
                    if (readySimsCount == mNumOfActiveSim && activePortsCount == mNumOfActiveSim) {
                        Log.i(
                                TAG,
                                String.format("%d ports are active and ready.", mNumOfActiveSim));
                        mSimCardStateChangedLatch.countDown();
                        return;
                    }
                    Log.i(
                            TAG,
                            String.format(
                                    "%d ports are active and %d SIMs are ready. Keep waiting until"
                                            + " timeout.",
                                    activePortsCount, readySimsCount));
                }
            };

    @Override
    protected Boolean doInBackground(Void aVoid) {
        return updateMultiSimConfig();
    }

    @Override
    protected void onPostExecute(Boolean isDsdsEnabled) {
        if (isDsdsEnabled) {
            setState(State.SUCCESS, Substate.UNUSED);
        } else {
            setState(State.ERROR, Substate.UNUSED);
        }
    }

    public void run(int numberOfSimToActivate) {
        mTelephonyManager = getContext().getSystemService(TelephonyManager.class);
        mNumOfActiveSim = numberOfSimToActivate;

        if (mNumOfActiveSim > mTelephonyManager.getSupportedModemCount()) {
            Log.e(TAG, "Requested number of active SIM is greater than supported modem count.");
            setState(State.ERROR, Substate.UNUSED);
            return;
        }
        if (mTelephonyManager.doesSwitchMultiSimConfigTriggerReboot()) {
            Log.e(TAG, "The device does not support reboot free DSDS.");
            setState(State.ERROR, Substate.UNUSED);
            return;
        }
        super.run(null /* param */);
    }

    // This method registers a ACTION_SIM_CARD_STATE_CHANGED broadcast receiver and wait for slot
    // changes. If multi SIMs have been successfully enabled, it returns true. Otherwise, returns
    // false.
    private boolean updateMultiSimConfig() {
        try {
            getContext()
                    .registerReceiver(
                            mCarrierConfigChangeReceiver,
                            new IntentFilter(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED));
            mTelephonyManager.switchMultiSimConfig(mNumOfActiveSim);
            long waitingTimeMillis =
                    Settings.Global.getLong(
                            getContext().getContentResolver(),
                            Settings.Global.ENABLE_MULTI_SLOT_TIMEOUT_MILLIS,
                            DEFAULT_ENABLE_MULTI_SIM_TIMEOUT_MILLS);
            if (mSimCardStateChangedLatch.await(waitingTimeMillis, TimeUnit.MILLISECONDS)) {
                Log.i(TAG, "Multi SIM were successfully enabled.");
                return true;
            } else {
                Log.e(TAG, "Timeout for waiting SIM status.");
                return false;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Failed to enable multiple SIM due to InterruptedException", e);
            return false;
        } finally {
            getContext().unregisterReceiver(mCarrierConfigChangeReceiver);
        }
    }

    // Returns how many SIMs have SIM ready state, not ready state, or removable slot with absent
    // SIM state.
    private int getReadySimsCount() {
        int readyCardsCount = 0;
        int activeSlotCount = mTelephonyManager.getActiveModemCount();
        Set<Integer> activeRemovableLogicalSlots = getActiveRemovableLogicalSlotIds();
        for (int logicalSlotId = 0; logicalSlotId < activeSlotCount; logicalSlotId++) {
            int simState = mTelephonyManager.getSimState(logicalSlotId);
            if (simState == TelephonyManager.SIM_STATE_READY
                    || simState == TelephonyManager.SIM_STATE_NOT_READY
                    || simState == TelephonyManager.SIM_STATE_LOADED
                    || (simState == TelephonyManager.SIM_STATE_ABSENT
                            && activeRemovableLogicalSlots.contains(logicalSlotId))) {
                readyCardsCount++;
            }
        }
        return readyCardsCount;
    }

    // Get active port count from {@code TelephonyManager#getUiccSlotsInfo}.
    private int getActivePortsCount() {
        UiccSlotInfo[] slotsInfo = mTelephonyManager.getUiccSlotsInfo();
        if (slotsInfo == null) {
            return 0;
        }
        int activePorts = 0;
        for (UiccSlotInfo slotInfo : slotsInfo) {
            if (slotInfo == null) {
                continue;
            }
            for (UiccPortInfo portInfo : slotInfo.getPorts()) {
                if (portInfo.isActive()) {
                    activePorts++;
                }
            }

        }
        return activePorts;
    }

    /** Returns a list of active removable logical slot ids. */
    public Set<Integer> getActiveRemovableLogicalSlotIds() {
        UiccSlotInfo[] infos = mTelephonyManager.getUiccSlotsInfo();
        if (infos == null) {
            return Collections.emptySet();
        }
        Set<Integer> activeRemovableLogicalSlotIds = new ArraySet<>();
        for (UiccSlotInfo info : infos) {
            if (info == null) {
                continue;
            }
            for (UiccPortInfo portInfo : info.getPorts()) {
                if (portInfo.isActive() && info.isRemovable()) {
                    activeRemovableLogicalSlotIds.add(portInfo.getLogicalSlotIndex());
                }
            }
        }
        return activeRemovableLogicalSlotIds;
    }
}

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
import android.app.PendingIntent;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.UiccCardInfo;
import android.telephony.UiccPortInfo;
import android.telephony.UiccSlotInfo;
import android.telephony.UiccSlotMapping;
import android.telephony.euicc.EuiccManager;
import android.util.Log;

import com.android.settings.SidecarFragment;
import com.android.settings.network.telephony.EuiccOperationSidecar;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** A headless fragment encapsulating long-running eSIM enabling/disabling operations. */
public class SwitchToEuiccSubscriptionSidecar extends EuiccOperationSidecar {
    private static final String TAG = "SwitchToEuiccSidecar";
    private static final String ACTION_SWITCH_TO_SUBSCRIPTION =
            "com.android.settings.network.SWITCH_TO_SUBSCRIPTION";

    private PendingIntent mCallbackIntent;
    private int mSubId;
    private int mPort;
    private SubscriptionInfo mRemovedSubInfo;
    private boolean mIsDuringSimSlotMapping;
    private List<SubscriptionInfo> mActiveSubInfos;

    /** Returns a SwitchToEuiccSubscriptionSidecar sidecar instance. */
    public static SwitchToEuiccSubscriptionSidecar get(FragmentManager fm) {
        return SidecarFragment.get(
                fm, TAG, SwitchToEuiccSubscriptionSidecar.class, null /* args */);
    }

    @Override
    public String getReceiverAction() {
        return ACTION_SWITCH_TO_SUBSCRIPTION;
    }

    /** Returns the pendingIntent of the eSIM operations. */
    public PendingIntent getCallbackIntent() {
        return mCallbackIntent;
    }

    @Override
    public void onStateChange(SidecarFragment fragment) {
        if (fragment == mSwitchSlotSidecar) {
            onSwitchSlotSidecarStateChange();
        } else {
            Log.wtf(TAG, "Received state change from a sidecar not expected.");
        }
    }

    /**
     * Starts calling EuiccManager#switchToSubscription to enable/disable the eSIM profile.
     *
     * @param subscriptionId the esim's subscriptionId.
     * @param port the esim's portId. If user wants to inactivate esim, then user must to assign
     *             the corresponding port. If user wants to activate esim, then the port can be
     *             {@link UiccSlotUtil#INVALID_PORT_ID}. When it is
     *             {@link UiccSlotUtil#INVALID_PORT_ID}, the system will reassign a corresponding
     *             port id.
     * @param removedSubInfo if the all of slots have sims, it should remove the one of active sim.
     *                       If the removedSubInfo is null, then use the default value.
     *                       The default value is the esim slot and portId 0.
     */
    public void run(int subscriptionId, int port, SubscriptionInfo removedSubInfo) {
        setState(State.RUNNING, Substate.UNUSED);
        mCallbackIntent = createCallbackIntent();
        mSubId = subscriptionId;

        int targetSlot = getTargetSlot();
        if (targetSlot < 0) {
            Log.d(TAG, "There is no esim, the TargetSlot is " + targetSlot);
            setState(State.ERROR, Substate.UNUSED);
            return;
        }

        SubscriptionManager subscriptionManager = getContext().getSystemService(
                SubscriptionManager.class).createForAllUserProfiles();
        mActiveSubInfos = SubscriptionUtil.getActiveSubscriptions(subscriptionManager);

        // To check whether the esim slot's port is active. If yes, skip setSlotMapping. If no,
        // set this slot+port into setSimSlotMapping.
        mPort = (port < 0) ? getTargetPortId(targetSlot, removedSubInfo) : port;
        mRemovedSubInfo = removedSubInfo;
        Log.d(TAG,
                String.format("Set esim into the SubId%d Physical Slot%d:Port%d",
                        mSubId, targetSlot, mPort));
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            // If the subId is INVALID_SUBSCRIPTION_ID, disable the esim (the default esim slot
            // which is selected by the framework).
            switchToSubscription();
        } else if ((mTelephonyManager.isMultiSimEnabled() && removedSubInfo != null
                && removedSubInfo.isEmbedded())
                || isEsimEnabledAtTargetSlotPort(targetSlot, mPort)) {
            // Case 1: In DSDS mode+MEP, if the replaced esim is active, then the replaced esim
            // should be disabled before changing SimSlotMapping process.
            //
            // Case 2: If the user enables the esim A on the target slot:port which is active
            // and there is an active esim B on target slot:port, then the settings disables the
            // esim B before the settings enables the esim A on the target slot:port.
            //
            // Step:
            // 1) Disables the replaced esim.
            // 2) Switches the SimSlotMapping if the target slot:port is not active.
            // 3) Enables the target esim.
            // Note: Use INVALID_SUBSCRIPTION_ID to disable the esim profile.
            Log.d(TAG, "Disable the enabled esim before the settings enables the target esim");
            mIsDuringSimSlotMapping = true;
            mEuiccManager.switchToSubscription(SubscriptionManager.INVALID_SUBSCRIPTION_ID, mPort,
                    mCallbackIntent);
        } else {
            mSwitchSlotSidecar.runSwitchToEuiccSlot(targetSlot, mPort, removedSubInfo);
        }
    }

    private int getTargetPortId(int physicalEsimSlotIndex, SubscriptionInfo removedSubInfo) {
        if (!isMultipleEnabledProfilesSupported(physicalEsimSlotIndex)) {
            Log.d(TAG, "The slotId" + physicalEsimSlotIndex + " is no MEP, port is 0");
            return 0;
        }

        if (!mTelephonyManager.isMultiSimEnabled()) {
            // In the 'SS mode'
            // If there is the esim slot is active, the port is from the current esim slot.
            // If there is no esim slot in device, then the esim's port is 0.
            Collection<UiccSlotMapping> uiccSlotMappings = mTelephonyManager.getSimSlotMapping();
            Log.d(TAG, "In SS mode, the UiccSlotMapping: " + uiccSlotMappings);
            return uiccSlotMappings.stream()
                    .filter(i -> i.getPhysicalSlotIndex() == physicalEsimSlotIndex)
                    .mapToInt(i -> i.getPortIndex())
                    .findFirst().orElse(0);
        }

        // In the 'DSDS+MEP', if the removedSubInfo is esim, then the port is
        // removedSubInfo's port.
        if (removedSubInfo != null && removedSubInfo.isEmbedded()) {
            return removedSubInfo.getPortIndex();
        }

        // In DSDS+MEP mode, the removedSubInfo is psim or is null, it means this esim needs
        // a new corresponding port in the esim slot.
        // For example:
        // 1) If there is no enabled esim and the user add new esim. This new esim's port is 0.
        // 2) If there is one enabled esim in port0 and the user add new esim. This new esim's
        // port is 1.
        // 3) If there is one enabled esim in port1 and the user add new esim. This new esim's
        // port is 0.

        int port = 0;
        if(mActiveSubInfos == null){
            Log.d(TAG, "mActiveSubInfos is null.");
            return port;
        }
        List<SubscriptionInfo> activeEsimSubInfos =
                mActiveSubInfos.stream()
                        .filter(i -> i.isEmbedded())
                        .sorted(Comparator.comparingInt(SubscriptionInfo::getPortIndex))
                        .collect(Collectors.toList());
        for (SubscriptionInfo subscriptionInfo : activeEsimSubInfos) {
            if (subscriptionInfo.getPortIndex() == port) {
                port++;
            }
        }
        return port;
    }

    private int getTargetSlot() {
        return UiccSlotUtil.getEsimSlotId(getContext(), mSubId);
    }

    private boolean isEsimEnabledAtTargetSlotPort(int physicalSlotIndex, int portIndex) {
        int logicalSlotId = getLogicalSlotIndex(physicalSlotIndex, portIndex);
        if (logicalSlotId == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            return false;
        }
        return mActiveSubInfos != null
                && mActiveSubInfos.stream()
                .anyMatch(i -> i.isEmbedded() && i.getSimSlotIndex() == logicalSlotId);
    }

    private int getLogicalSlotIndex(int physicalSlotIndex, int portIndex) {
        ImmutableList<UiccSlotInfo> slotInfos = UiccSlotUtil.getSlotInfos(mTelephonyManager);
        if (slotInfos != null && physicalSlotIndex >= 0 && physicalSlotIndex < slotInfos.size()
                && slotInfos.get(physicalSlotIndex) != null) {
            for (UiccPortInfo portInfo : slotInfos.get(physicalSlotIndex).getPorts()) {
                if (portInfo.getPortIndex() == portIndex) {
                    return portInfo.getLogicalSlotIndex();
                }
            }
        }

        return SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    }

    private void onSwitchSlotSidecarStateChange() {
        switch (mSwitchSlotSidecar.getState()) {
            case State.SUCCESS:
                mSwitchSlotSidecar.reset();
                Log.i(TAG, "Successfully SimSlotMapping. Start to enable/disable esim");
                switchToSubscription();
                break;
            case State.ERROR:
                mSwitchSlotSidecar.reset();
                Log.i(TAG, "Failed to set SimSlotMapping");
                setState(State.ERROR, Substate.UNUSED);
                break;
        }
    }

    private boolean isMultipleEnabledProfilesSupported(int physicalEsimSlotIndex) {
        List<UiccCardInfo> cardInfos = mTelephonyManager.getUiccCardsInfo();
        return cardInfos.stream()
                .anyMatch(cardInfo -> cardInfo.getPhysicalSlotIndex() == physicalEsimSlotIndex
                        && cardInfo.isMultipleEnabledProfilesSupported());
    }

    private void switchToSubscription() {
        // The SimSlotMapping is ready, then to execute activate/inactivate esim.
        mEuiccManager.switchToSubscription(mSubId, mPort, mCallbackIntent);
    }

    @Override
    protected void onActionReceived() {
        if (getResultCode() == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK
                && mIsDuringSimSlotMapping) {
            // Continue to switch the SimSlotMapping, after the esim is disabled.
            mIsDuringSimSlotMapping = false;
            mSwitchSlotSidecar.runSwitchToEuiccSlot(getTargetSlot(), mPort, mRemovedSubInfo);
        } else {
            super.onActionReceived();
        }
    }
}

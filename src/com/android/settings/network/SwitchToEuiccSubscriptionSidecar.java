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
import android.telephony.UiccSlotMapping;
import android.telephony.euicc.EuiccManager;
import android.util.Log;

import com.android.settings.SidecarFragment;
import com.android.settings.network.telephony.EuiccOperationSidecar;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** A headless fragment encapsulating long-running eSIM enabling/disabling operations. */
public class SwitchToEuiccSubscriptionSidecar extends EuiccOperationSidecar {
    private static final String TAG = "SwitchToEuiccSidecar";
    private static final String ACTION_SWITCH_TO_SUBSCRIPTION =
            "com.android.settings.network.SWITCH_TO_SUBSCRIPTION";
    private static final int ESIM_SLOT_ID = 1;

    private PendingIntent mCallbackIntent;
    private int mSubId;
    private int mPort;
    private SubscriptionInfo mRemovedSubInfo;
    private boolean mIsDuringSimSlotMapping;

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

    /** Starts calling EuiccManager#switchToSubscription to enable/disable the eSIM profile. */
    // ToDo: delete this api and refactor the related code.
    public void run(int subscriptionId) {
        setState(State.RUNNING, Substate.UNUSED);
        mCallbackIntent = createCallbackIntent();
        mEuiccManager.switchToSubscription(subscriptionId, mCallbackIntent);
    }

    /**
     * Starts calling EuiccManager#switchToSubscription to enable/disable the eSIM profile.
     *
     * @param subscriptionId the esim's subscriptionId.
     * @param port the esim's portId. If user wants to inactivate esim, then user must to assign the
     *             the port. If user wants to activate esim, then the port can be -1.
     * @param removedSubInfo if the all of slots have sims, it should remove the one of active sim.
     *                       If the removedSubInfo is null, then use the default value.
     *                       The default value is the esim slot and portId 0.
     */
    public void run(int subscriptionId, int port, SubscriptionInfo removedSubInfo) {
        setState(State.RUNNING, Substate.UNUSED);
        mCallbackIntent = createCallbackIntent();
        mSubId = subscriptionId;
        // To check whether the esim slot's port is active. If yes, skip setSlotMapping. If no,
        // set this slot+port into setSimSlotMapping.
        mPort = (port < 0) ? getTargetPortId(removedSubInfo) : port;
        mRemovedSubInfo = removedSubInfo;
        Log.i(TAG, "The SubId is " + mSubId + ". The port is " + mPort);

        if (mTelephonyManager.isMultiSimEnabled() && removedSubInfo != null
                && removedSubInfo.isEmbedded()) {
            // In DSDS mode+MEP, if the replaced esim is active, then it should be disabled esim
            // profile before changing SimSlotMapping process.
            // Use INVALID_SUBSCRIPTION_ID to disable the esim profile.
            // The SimSlotMapping is ready, then to execute activate/inactivate esim.
            mIsDuringSimSlotMapping = true;
            mEuiccManager.switchToSubscription(SubscriptionManager.INVALID_SUBSCRIPTION_ID, mPort,
                    mCallbackIntent);
        } else {
            mSwitchSlotSidecar.runSwitchToEuiccSlot(getTargetSlot(), mPort, removedSubInfo);
        }
    }

    private int getTargetPortId(SubscriptionInfo removedSubInfo) {
        if (!mTelephonyManager.isMultiSimEnabled() || !isMultipleEnabledProfilesSupported()) {
            // In the 'SS mode' or 'DSDS+no MEP', the port is 0.
            return 0;
        }

        // In the 'DSDS+MEP', if the removedSubInfo is esim, then the port is
        // removedSubInfo's port.
        if (removedSubInfo != null && removedSubInfo.isEmbedded()) {
            return removedSubInfo.getPortIndex();
        }

        // In DSDS+MEP mode, the removedSubInfo is psim or is null, it means the this esim need
        // another port in the esim slot.
        // To find another esim's port and value is from 0;
        int port = 0;
        Collection<UiccSlotMapping> uiccSlotMappings = mTelephonyManager.getSimSlotMapping();
        for (UiccSlotMapping uiccSlotMapping :
                uiccSlotMappings.stream()
                        .filter(
                                uiccSlotMapping -> uiccSlotMapping.getPhysicalSlotIndex()
                                        == getTargetSlot())
                        .collect(Collectors.toList())) {
            if (uiccSlotMapping.getPortIndex() == port) {
                port++;
            }
        }
        return port;
    }

    private int getTargetSlot() {
        return ESIM_SLOT_ID;
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

    private boolean isMultipleEnabledProfilesSupported() {
        List<UiccCardInfo> cardInfos = mTelephonyManager.getUiccCardsInfo();
        if (cardInfos == null) {
            Log.w(TAG, "UICC cards info list is empty.");
            return false;
        }
        return cardInfos.stream().anyMatch(
                cardInfo -> cardInfo.isMultipleEnabledProfilesSupported());
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

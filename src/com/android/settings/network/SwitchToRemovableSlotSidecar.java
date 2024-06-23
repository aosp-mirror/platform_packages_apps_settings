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
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.settings.SidecarFragment;
import com.android.settings.network.telephony.EuiccOperationSidecar;

/**
 * This sidecar is responsible for switching to the removable slot. It disables the active eSIM
 * profile before switching if there is one.
 */
public class SwitchToRemovableSlotSidecar extends EuiccOperationSidecar
        implements SidecarFragment.Listener {
    private static final String TAG = "SwitchRemovableSidecar";
    private static final String ACTION_DISABLE_SUBSCRIPTION_AND_SWITCH_SLOT =
            "disable_subscription_and_switch_slot_sidecar";

    // Stateless members.
    private SwitchToEuiccSubscriptionSidecar mSwitchToSubscriptionSidecar;
    private int mPhysicalSlotId;
    private SubscriptionInfo mRemovedSubInfo;

    /** Returns a SwitchToRemovableSlotSidecar sidecar instance. */
    public static SwitchToRemovableSlotSidecar get(FragmentManager fm) {
        return SidecarFragment.get(fm, TAG, SwitchToRemovableSlotSidecar.class, null /* args */);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSwitchToSubscriptionSidecar =
                SwitchToEuiccSubscriptionSidecar.get(getChildFragmentManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitchToSubscriptionSidecar.addListener(this);
    }

    @Override
    public void onPause() {
        mSwitchToSubscriptionSidecar.removeListener(this);
        super.onPause();
    }

    @Override
    protected String getReceiverAction() {
        return ACTION_DISABLE_SUBSCRIPTION_AND_SWITCH_SLOT;
    }

    @Override
    public void onStateChange(SidecarFragment fragment) {
        if (fragment == mSwitchToSubscriptionSidecar) {
            onSwitchToSubscriptionSidecarStateChange();
        } else if (fragment == mSwitchSlotSidecar) {
            onSwitchSlotSidecarStateChange();
        } else {
            Log.wtf(TAG, "Received state change from a sidecar not expected.");
        }
    }

    /**
     * Starts switching to the removable slot.
     *
     * @param physicalSlotId removable physical SIM slot ID.
     * @param removedSubInfo if the all of slots have sims, it should remove the one of active sim.
     *                       If the removedSubInfo is null, then use the default value.
     *                       The default value is the removable physical SIM slot and portId 0.
     */
    public void run(int physicalSlotId, SubscriptionInfo removedSubInfo) {
        mPhysicalSlotId = physicalSlotId;
        mRemovedSubInfo = removedSubInfo;
        SubscriptionManager subscriptionManager =
                getContext().getSystemService(SubscriptionManager.class).createForAllUserProfiles();
        if (!mTelephonyManager.isMultiSimEnabled()
                && SubscriptionUtil.getActiveSubscriptions(subscriptionManager).stream().anyMatch(
                SubscriptionInfo::isEmbedded)) {
            // In SS mode, the esim is active, then inactivate the esim.
            Log.i(TAG, "There is an active eSIM profile. Disable the profile first.");
            // Use INVALID_SUBSCRIPTION_ID to disable the only active profile.
            mSwitchToSubscriptionSidecar.run(SubscriptionManager.INVALID_SUBSCRIPTION_ID, 0, null);
        } else if (mTelephonyManager.isMultiSimEnabled() && mRemovedSubInfo != null) {
            // In DSDS mode+MEP, if the replaced esim is active, then it should disable that esim
            // profile before changing SimSlotMapping process.
            // Use INVALID_SUBSCRIPTION_ID to disable the esim profile.
            mSwitchToSubscriptionSidecar.run(SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                    mRemovedSubInfo.getPortIndex(), null);
        } else {
            Log.i(TAG, "Start to switch to removable slot.");
            mSwitchSlotSidecar.runSwitchToRemovableSlot(mPhysicalSlotId, mRemovedSubInfo);
        }
    }

    private void onSwitchToSubscriptionSidecarStateChange() {
        switch (mSwitchToSubscriptionSidecar.getState()) {
            case State.SUCCESS:
                mSwitchToSubscriptionSidecar.reset();
                Log.i(TAG,
                        "Successfully disabled eSIM profile. Start to switch to Removable slot.");
                mSwitchSlotSidecar.runSwitchToRemovableSlot(mPhysicalSlotId, mRemovedSubInfo);
                break;
            case State.ERROR:
                mSwitchToSubscriptionSidecar.reset();
                Log.i(TAG, "Failed to disable the active eSIM profile.");
                setState(State.ERROR, Substate.UNUSED);
                break;
        }
    }

    private void onSwitchSlotSidecarStateChange() {
        switch (mSwitchSlotSidecar.getState()) {
            case State.SUCCESS:
                mSwitchSlotSidecar.reset();
                Log.i(TAG, "Successfully switched to removable slot.");
                setState(State.SUCCESS, Substate.UNUSED);
                break;
            case State.ERROR:
                mSwitchSlotSidecar.reset();
                Log.i(TAG, "Failed to switch to removable slot.");
                setState(State.ERROR, Substate.UNUSED);
                break;
        }
    }
}

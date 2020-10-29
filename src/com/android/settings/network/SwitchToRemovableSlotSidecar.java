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

    private static final String TAG = "DisableSubscriptionAndSwitchSlotSidecar";
    private static final String ACTION_DISABLE_SUBSCRIPTION_AND_SWITCH_SLOT =
            "disable_subscription_and_switch_slot_sidecar";

    // Stateless members.
    private SwitchToEuiccSubscriptionSidecar mSwitchToSubscriptionSidecar;
    private SwitchSlotSidecar mSwitchSlotSidecar;
    private int mPhysicalSlotId;

    /** Returns a SwitchToRemovableSlotSidecar sidecar instance. */
    public static SwitchToRemovableSlotSidecar get(FragmentManager fm) {
        return SidecarFragment.get(fm, TAG, SwitchToRemovableSlotSidecar.class, null /* args */);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSwitchToSubscriptionSidecar =
                SwitchToEuiccSubscriptionSidecar.get(getChildFragmentManager());
        mSwitchSlotSidecar = SwitchSlotSidecar.get(getChildFragmentManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitchToSubscriptionSidecar.addListener(this);
        mSwitchSlotSidecar.addListener(this);
    }

    @Override
    public void onPause() {
        mSwitchToSubscriptionSidecar.removeListener(this);
        mSwitchSlotSidecar.removeListener(this);
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
     * Starts switching to the removable slot. It disables the active eSIM profile before switching
     * if there is one.
     *
     * @param physicalSlotId removable physical SIM slot ID.
     */
    public void run(int physicalSlotId) {
        mPhysicalSlotId = physicalSlotId;
        SubscriptionManager subscriptionManager =
                getContext().getSystemService(SubscriptionManager.class);
        if (SubscriptionUtil.getActiveSubscriptions(subscriptionManager).stream()
                .anyMatch(SubscriptionInfo::isEmbedded)) {
            Log.i(TAG, "There is an active eSIM profile. Disable the profile first.");
            // Use INVALID_SUBSCRIPTION_ID to disable the only active profile.
            mSwitchToSubscriptionSidecar.run(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        } else {
            Log.i(TAG, "There is no active eSIM profiles. Start to switch to removable slot.");
            mSwitchSlotSidecar.runSwitchToRemovableSlot(mPhysicalSlotId);
        }
    }

    private void onSwitchToSubscriptionSidecarStateChange() {
        switch (mSwitchToSubscriptionSidecar.getState()) {
            case State.SUCCESS:
                mSwitchToSubscriptionSidecar.reset();
                Log.i(
                        TAG,
                        "Successfully disabled eSIM profile. Start to switch to Removable slot.");
                mSwitchSlotSidecar.runSwitchToRemovableSlot(mPhysicalSlotId);
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

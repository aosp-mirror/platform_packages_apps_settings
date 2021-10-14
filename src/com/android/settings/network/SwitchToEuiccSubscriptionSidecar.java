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

import com.android.settings.SidecarFragment;
import com.android.settings.network.telephony.EuiccOperationSidecar;

/** A headless fragment encapsulating long-running eSIM enabling/disabling operations. */
public class SwitchToEuiccSubscriptionSidecar extends EuiccOperationSidecar {
    private static final String TAG = "SwitchToEuiccSubscriptionSidecar";
    private static final String ACTION_SWITCH_TO_SUBSCRIPTION =
            "com.android.settings.network.SWITCH_TO_SUBSCRIPTION";

    private PendingIntent mCallbackIntent;

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

    /** Starts calling EuiccManager#switchToSubscription to enable/disable the eSIM profile. */
    public void run(int subscriptionId) {
        setState(State.RUNNING, Substate.UNUSED);
        mCallbackIntent = createCallbackIntent();
        mEuiccManager.switchToSubscription(subscriptionId, mCallbackIntent);
    }
}

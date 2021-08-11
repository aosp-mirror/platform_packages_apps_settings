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

package com.android.settings.network.telephony;

import android.app.FragmentManager;
import android.app.PendingIntent;
import android.telephony.SubscriptionInfo;
import android.telephony.euicc.EuiccManager;
import android.util.Log;

import com.android.settings.SidecarFragment;

import java.util.ArrayList;
import java.util.List;

/** A headless fragment encapsulating long-running eSIM erasing operations. */
public class DeleteEuiccSubscriptionSidecar extends EuiccOperationSidecar {
    private static final String TAG = "DeleteEuiccSubscriptionSidecar";
    private static final String ACTION_DELETE_SUBSCRIPTION =
            "com.android.settings.network.DELETE_SUBSCRIPTION";

    private List<SubscriptionInfo> mSubscriptions;

    @Override
    public String getReceiverAction() {
        return ACTION_DELETE_SUBSCRIPTION;
    }

    /** Returns a DeleteEuiccSubscriptionSidecar sidecar instance. */
    public static DeleteEuiccSubscriptionSidecar get(FragmentManager fm) {
        return SidecarFragment.get(fm, TAG, DeleteEuiccSubscriptionSidecar.class, null /* args */);
    }

    /** Starts calling EuiccManager#deleteSubscription to delete the eSIM profile. */
    public void run(List<SubscriptionInfo> subscriptions) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            throw new IllegalArgumentException("Subscriptions cannot be empty.");
        }

        setState(State.RUNNING, Substate.UNUSED);

        mSubscriptions = new ArrayList<>(subscriptions);
        deleteSubscription();
    }

    @Override
    protected void onActionReceived() {
        if (getResultCode() == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK
                && !mSubscriptions.isEmpty()) {
            // Continue to delete remaining subscriptions.
            deleteSubscription();
        } else {
            super.onActionReceived();
        }
    }

    private void deleteSubscription() {
        SubscriptionInfo subscription = mSubscriptions.remove(0);
        PendingIntent intent = createCallbackIntent();
        Log.i(TAG, "Deleting subscription ID: " + subscription.getSubscriptionId());
        mEuiccManager.deleteSubscription(subscription.getSubscriptionId(), intent);
    }
}

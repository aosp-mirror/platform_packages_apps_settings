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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SidecarFragment;
import com.android.settings.network.SubscriptionUtil;

import java.util.List;

/** This dialog activity handles deleting eSIM profiles. */
public class DeleteEuiccSubscriptionDialogActivity extends SubscriptionActionDialogActivity
        implements SidecarFragment.Listener, ConfirmDialogFragment.OnConfirmListener {

    private static final String TAG = "DeleteEuiccSubscriptionDialogActivity";
    // Dialog tags
    private static final int DIALOG_TAG_DELETE_SIM_CONFIRMATION = 1;

    /**
     * Returns an intent of DeleteEuiccSubscriptionDialogActivity.
     *
     * @param context The context used to start the DeleteEuiccSubscriptionDialogActivity.
     * @param subId The subscription ID of the subscription needs to be deleted. If the subscription
     *     belongs to a group of subscriptions, all subscriptions from the group will be deleted.
     */
    public static Intent getIntent(Context context, int subId) {
        Intent intent = new Intent(context, DeleteEuiccSubscriptionDialogActivity.class);
        intent.putExtra(ARG_SUB_ID, subId);
        return intent;
    }

    private DeleteEuiccSubscriptionSidecar mDeleteEuiccSubscriptionSidecar;
    private List<SubscriptionInfo> mSubscriptionsToBeDeleted;
    private SubscriptionInfo mSubscriptionToBeDeleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int subId = intent.getIntExtra(ARG_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mSubscriptionToBeDeleted = SubscriptionUtil.getSubById(mSubscriptionManager, subId);
        mSubscriptionsToBeDeleted =
                SubscriptionUtil.findAllSubscriptionsInGroup(mSubscriptionManager, subId);

        if (mSubscriptionToBeDeleted == null || mSubscriptionsToBeDeleted.isEmpty()) {
            Log.e(TAG, "Cannot find subscription with sub ID: " + subId);
            finish();
            return;
        }

        mDeleteEuiccSubscriptionSidecar = DeleteEuiccSubscriptionSidecar.get(getFragmentManager());
        if (savedInstanceState == null) {
            showDeleteSimConfirmDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDeleteEuiccSubscriptionSidecar.addListener(this);
    }

    @Override
    protected void onPause() {
        mDeleteEuiccSubscriptionSidecar.removeListener(this);
        super.onPause();
    }

    @Override
    public void onStateChange(SidecarFragment fragment) {
        if (fragment == mDeleteEuiccSubscriptionSidecar) {
            handleDeleteEuiccSubscriptionSidecarStateChange();
        }
    }

    @Override
    public void onConfirm(int tag, boolean confirmed, int itemPosition) {
        if (!confirmed) {
            finish();
            return;
        }

        switch (tag) {
            case DIALOG_TAG_DELETE_SIM_CONFIRMATION:
                Log.i(TAG, "Subscription deletion confirmed");
                showProgressDialog(getString(R.string.erasing_sim));
                mDeleteEuiccSubscriptionSidecar.run(mSubscriptionsToBeDeleted);
                break;
            default:
                Log.e(TAG, "Unrecognized confirmation dialog tag: " + tag);
                break;
        }
    }

    private void handleDeleteEuiccSubscriptionSidecarStateChange() {
        switch (mDeleteEuiccSubscriptionSidecar.getState()) {
            case SidecarFragment.State.SUCCESS:
                Log.i(TAG, "Successfully delete the subscription.");
                mDeleteEuiccSubscriptionSidecar.reset();
                dismissProgressDialog();
                finish();
                break;
            case SidecarFragment.State.ERROR:
                Log.e(TAG, "Failed to delete the subscription.");
                mDeleteEuiccSubscriptionSidecar.reset();
                showErrorDialog(
                        getString(R.string.erase_sim_fail_title),
                        getString(R.string.erase_sim_fail_text));
                break;
        }
    }

    /* Displays the eSIM deleting confirmation dialog. */
    private void showDeleteSimConfirmDialog() {
        ConfirmDialogFragment.show(
                this,
                ConfirmDialogFragment.OnConfirmListener.class,
                DIALOG_TAG_DELETE_SIM_CONFIRMATION,
                getString(R.string.erase_sim_dialog_title),
                getString(
                        R.string.erase_sim_dialog_text,
                        SubscriptionUtil.getUniqueSubscriptionDisplayName(
                                mSubscriptionToBeDeleted, this)),
                getString(R.string.erase_sim_confirm_button),
                getString(R.string.cancel));
    }
}

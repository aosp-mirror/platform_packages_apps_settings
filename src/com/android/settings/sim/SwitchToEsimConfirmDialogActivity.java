/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.sim;

import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SidecarFragment;
import com.android.settings.network.SwitchToEuiccSubscriptionSidecar;
import com.android.settings.network.UiccSlotUtil;
import com.android.settings.network.telephony.AlertDialogFragment;
import com.android.settings.network.telephony.ConfirmDialogFragment;
import com.android.settings.network.telephony.SubscriptionActionDialogActivity;

/**
 * Starts a confirm dialog asking the user to switch to the eSIM slot/subscription. The caller needs
 * to pass in the current enabled eSIM subscription, which is also the subscription to switch to.
 */
public class SwitchToEsimConfirmDialogActivity extends SubscriptionActionDialogActivity
        implements SidecarFragment.Listener, ConfirmDialogFragment.OnConfirmListener {

    public static final String KEY_SUB_TO_ENABLE = "sub_to_enable";

    private static final String TAG = "SwitchToEsimConfirmDialogActivity";
    private static final int TAG_CONFIRM = 1;

    private SubscriptionInfo mSubToEnabled = null;
    private SwitchToEuiccSubscriptionSidecar mSwitchToEuiccSubscriptionSidecar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSubToEnabled = getIntent().getParcelableExtra(KEY_SUB_TO_ENABLE);
        mSwitchToEuiccSubscriptionSidecar =
                SwitchToEuiccSubscriptionSidecar.get(getFragmentManager());

        if (mSubToEnabled == null) {
            Log.e(TAG, "Cannot find SIM to enable.");
            finish();
            return;
        }

        if (savedInstanceState == null) {
            ConfirmDialogFragment.show(
                    this,
                    ConfirmDialogFragment.OnConfirmListener.class,
                    TAG_CONFIRM,
                    getString(R.string.switch_sim_dialog_title, mSubToEnabled.getDisplayName()),
                    getString(R.string.switch_sim_dialog_text, mSubToEnabled.getDisplayName()),
                    getString(R.string.okay),
                    getString(R.string.cancel));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitchToEuiccSubscriptionSidecar.addListener(this);
    }

    @Override
    public void onPause() {
        mSwitchToEuiccSubscriptionSidecar.removeListener(this);
        super.onPause();
    }

    @Override
    public void onStateChange(SidecarFragment fragment) {
        if (fragment == mSwitchToEuiccSubscriptionSidecar) {
            switch (mSwitchToEuiccSubscriptionSidecar.getState()) {
                case SidecarFragment.State.SUCCESS:
                    mSwitchToEuiccSubscriptionSidecar.reset();
                    Log.i(TAG, "Successfully switched to eSIM slot.");
                    dismissProgressDialog();
                    finish();
                    break;
                case SidecarFragment.State.ERROR:
                    mSwitchToEuiccSubscriptionSidecar.reset();
                    Log.e(TAG, "Failed switching to eSIM slot.");
                    dismissProgressDialog();
                    finish();
                    break;
            }
        }
    }

    @Override
    public void onConfirm(int tag, boolean confirmed, int itemPosition) {
        if (!confirmed) {
            AlertDialogFragment.show(
                    this,
                    getString(R.string.switch_sim_dialog_no_switch_title),
                    getString(R.string.switch_sim_dialog_no_switch_text));
            return;
        }
        Log.i(TAG, "User confirmed to switch to embedded slot.");
        mSwitchToEuiccSubscriptionSidecar.run(mSubToEnabled.getSubscriptionId(),
                UiccSlotUtil.INVALID_PORT_ID, null);
        showProgressDialog(
                getString(
                        R.string.sim_action_switch_sub_dialog_progress,
                        mSubToEnabled.getDisplayName()));
    }
}

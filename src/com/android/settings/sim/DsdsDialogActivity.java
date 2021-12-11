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

import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SidecarFragment;
import com.android.settings.network.EnableMultiSimSidecar;
import com.android.settings.network.telephony.ConfirmDialogFragment;
import com.android.settings.network.telephony.SubscriptionActionDialogActivity;

/** Activity to show the enabling DSDS dialog. */
public class DsdsDialogActivity extends SubscriptionActionDialogActivity
        implements SidecarFragment.Listener, ConfirmDialogFragment.OnConfirmListener {

    private static final String TAG = "DsdsDialogActivity";
    // Dialog tags
    private static final int DIALOG_TAG_ENABLE_DSDS_CONFIRMATION = 1;
    private static final int DIALOG_TAG_ENABLE_DSDS_REBOOT_CONFIRMATION = 2;
    // Number of SIMs for DSDS
    private static final int NUM_OF_SIMS_FOR_DSDS = 2;

    private EnableMultiSimSidecar mEnableMultiSimSidecar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEnableMultiSimSidecar = EnableMultiSimSidecar.get(getFragmentManager());
        if (savedInstanceState == null) {
            showEnableDsdsConfirmDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEnableMultiSimSidecar.addListener(this);
    }

    @Override
    protected void onPause() {
        mEnableMultiSimSidecar.removeListener(this);
        super.onPause();
    }

    @Override
    public void onStateChange(SidecarFragment fragment) {
        if (fragment == mEnableMultiSimSidecar) {
            switch (fragment.getState()) {
                case SidecarFragment.State.SUCCESS:
                    mEnableMultiSimSidecar.reset();
                    Log.i(TAG, "Enabled DSDS successfully");
                    dismissProgressDialog();
                    finish();
                    break;
                case SidecarFragment.State.ERROR:
                    mEnableMultiSimSidecar.reset();
                    Log.e(TAG, "Failed to enable DSDS");
                    dismissProgressDialog();
                    showErrorDialog(
                            getString(R.string.dsds_activation_failure_title),
                            getString(R.string.dsds_activation_failure_body_msg2));
                    break;
            }
        }
    }

    @Override
    public void onConfirm(int tag, boolean confirmed, int itemPosition) {
        if (!confirmed) {
            Log.i(TAG, "User cancel the dialog to enable DSDS.");
            startChooseSimActivity();
            return;
        }

        TelephonyManager telephonyManager = getSystemService(TelephonyManager.class);
        switch (tag) {
            case DIALOG_TAG_ENABLE_DSDS_CONFIRMATION:
                if (telephonyManager.doesSwitchMultiSimConfigTriggerReboot()) {
                    Log.i(TAG, "Device does not support reboot free DSDS.");
                    showRebootConfirmDialog();
                    return;
                }
                Log.i(TAG, "Enabling DSDS without rebooting.");
                showProgressDialog(
                        getString(R.string.sim_action_enabling_sim_without_carrier_name));
                mEnableMultiSimSidecar.run(NUM_OF_SIMS_FOR_DSDS);
                break;
            case DIALOG_TAG_ENABLE_DSDS_REBOOT_CONFIRMATION:
                Log.i(TAG, "User confirmed reboot to enable DSDS.");
                SimActivationNotifier.setShowSimSettingsNotification(this, true);
                telephonyManager.switchMultiSimConfig(NUM_OF_SIMS_FOR_DSDS);
                break;
            default:
                Log.e(TAG, "Unrecognized confirmation dialog tag: " + tag);
                break;
        }
    }

    private void showEnableDsdsConfirmDialog() {
        ConfirmDialogFragment.show(
                this,
                ConfirmDialogFragment.OnConfirmListener.class,
                DIALOG_TAG_ENABLE_DSDS_CONFIRMATION,
                getString(R.string.sim_action_enable_dsds_title),
                getString(R.string.sim_action_enable_dsds_text),
                getString(R.string.sim_action_yes),
                getString(R.string.sim_action_no_thanks));
    }

    private void showRebootConfirmDialog() {
        ConfirmDialogFragment.show(
                this,
                ConfirmDialogFragment.OnConfirmListener.class,
                DIALOG_TAG_ENABLE_DSDS_REBOOT_CONFIRMATION,
                getString(R.string.sim_action_restart_title),
                getString(R.string.sim_action_enable_dsds_text),
                getString(R.string.sim_action_reboot),
                getString(R.string.cancel));
    }

    private void startChooseSimActivity() {
        Intent intent = ChooseSimActivity.getIntent(this);
        intent.putExtra(ChooseSimActivity.KEY_HAS_PSIM, true);
        startActivity(intent);
        finish();
    }
}

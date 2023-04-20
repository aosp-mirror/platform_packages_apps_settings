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

package com.android.settings.sim.smartForwarding;

import static com.android.settings.sim.smartForwarding.EnableSmartForwardingTask.FeatureResult;
import static com.android.settings.sim.smartForwarding.SmartForwardingUtils.TAG;
import static com.android.settings.sim.smartForwarding.SmartForwardingUtils.backupPrevStatus;
import static com.android.settings.sim.smartForwarding.SmartForwardingUtils.clearAllBackupData;
import static com.android.settings.sim.smartForwarding.SmartForwardingUtils.getAllSlotCallForwardingStatus;
import static com.android.settings.sim.smartForwarding.SmartForwardingUtils.getAllSlotCallWaitingStatus;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.telephony.CallForwardingInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toolbar;

import androidx.core.content.ContextCompat;

import com.android.settings.R;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.network.SubscriptionUtil;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executors;

public class SmartForwardingActivity extends SettingsBaseActivity {
    static final String LOG_TAG = SmartForwardingActivity.class.toString();
    final ListeningExecutorService service =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SubscriptionUtil.isSimHardwareVisible(this)) {
            Log.d(LOG_TAG, "Not support on device without SIM.");
            finish();
            return;
        }

        final Toolbar toolbar = findViewById(R.id.action_bar);
        toolbar.setVisibility(View.VISIBLE);
        setActionBar(toolbar);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new SmartForwardingFragment())
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void enableSmartForwarding(String[] phoneNumber) {
        // Pop-up ongoing dialog
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(R.string.smart_forwarding_ongoing_title);
        dialog.setIndeterminate(true);
        dialog.setMessage(getText(R.string.smart_forwarding_ongoing_text));
        dialog.setCancelable(false);
        dialog.show();

        // Enable feature
        ListenableFuture<FeatureResult> enableTask =
                service.submit(new EnableSmartForwardingTask(this, phoneNumber));
        Futures.addCallback(enableTask, new FutureCallback<FeatureResult>() {
            @Override
            public void onSuccess(FeatureResult result) {
                Log.e(TAG, "Enable Feature result: " + result.getResult());
                if (result.getResult()) {
                    backupPrevStatus(SmartForwardingActivity.this, result.getSlotUTData());

                    // Turn on switch preference
                    SmartForwardingFragment fragment =
                            (SmartForwardingFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.content_frame);
                    if (fragment != null) {
                        fragment.turnOnSwitchPreference();
                    }
                } else {
                    onError(result);
                }
                dialog.dismiss();
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Enable Feature exception", t);
                dialog.dismiss();

                // Pop-up error dialog
                AlertDialog mDialog = new AlertDialog.Builder(SmartForwardingActivity.this)
                        .setTitle(R.string.smart_forwarding_failed_title)
                        .setMessage(R.string.smart_forwarding_failed_text)
                        .setPositiveButton(
                                R.string.smart_forwarding_missing_alert_dialog_text,
                                (dialog, which) -> { dialog.dismiss(); })
                        .create();
                mDialog.show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public void disableSmartForwarding() {
        TelephonyManager tm = getSystemService(TelephonyManager.class);
        SubscriptionManager sm = getSystemService(SubscriptionManager.class);

        boolean[] callWaitingStatus = getAllSlotCallWaitingStatus(this, sm, tm);
        CallForwardingInfo[] callForwardingInfo = getAllSlotCallForwardingStatus(this, sm, tm);

        // Disable feature
        ListenableFuture disableTask = service.submit(new DisableSmartForwardingTask(
                tm, callWaitingStatus, callForwardingInfo));
        Futures.addCallback(disableTask, new FutureCallback() {
            @Override
            public void onSuccess(Object result) {
                clearAllBackupData(SmartForwardingActivity.this, sm, tm);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Disable Feature exception" + t);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public void onError(FeatureResult result) {
        int errorMsg;
        if (result.getReason() == FeatureResult.FailedReason.SIM_NOT_ACTIVE) {
            errorMsg = R.string.smart_forwarding_failed_not_activated_text;
        } else {
            errorMsg = R.string.smart_forwarding_failed_text;
        }

        // Pop-up error dialog
        AlertDialog mDialog = new AlertDialog.Builder(SmartForwardingActivity.this)
                .setTitle(R.string.smart_forwarding_failed_title)
                .setMessage(errorMsg)
                .setPositiveButton(
                        R.string.smart_forwarding_missing_alert_dialog_text,
                        (dialog, which) -> { dialog.dismiss(); })
                .create();
        mDialog.show();
    }
}

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

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;
import android.util.Log;

import com.android.settings.SidecarFragment;
import com.android.settings.network.SwitchSlotSidecar;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The sidecar base class that an Euicc sidecar can extend from. The extended class should implement
 * getReceiverAction() to return the action string for the broadcast receiver. The extended class
 * should implement its own get() function to return an instance of that class, and implement the
 * functional class like run() to actually trigger the function in EuiccManager.
 */
public abstract class EuiccOperationSidecar extends SidecarFragment
        implements SidecarFragment.Listener{
    private static final String TAG = "EuiccOperationSidecar";
    private static final int REQUEST_CODE = 0;
    private static final String EXTRA_OP_ID = "op_id";
    private static AtomicInteger sCurrentOpId =
            new AtomicInteger((int) SystemClock.elapsedRealtime());

    protected EuiccManager mEuiccManager;
    protected TelephonyManager mTelephonyManager;
    protected SwitchSlotSidecar mSwitchSlotSidecar;


    private int mResultCode;
    private int mDetailedCode;
    private Intent mResultIntent;
    private int mOpId;

    protected final BroadcastReceiver mReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (getReceiverAction().equals(intent.getAction())
                            && mOpId == intent.getIntExtra(EXTRA_OP_ID, -1)) {
                        mResultCode = getResultCode();
                        /* TODO: This relies on our LUI and LPA to coexist, should think about how
                        to generalize this further. */
                        mDetailedCode =
                                intent.getIntExtra(
                                        EuiccManager.EXTRA_EMBEDDED_SUBSCRIPTION_DETAILED_CODE,
                                        0 /* defaultValue*/);
                        mResultIntent = intent;
                        Log.i(
                                TAG,
                                String.format(
                                        "Result code : %d; detailed code : %d",
                                        mResultCode, mDetailedCode));
                        onActionReceived();
                    }
                }
            };

    /**
     * This is called when the broadcast action is received. The subclass may override this to
     * perform different logic. The broadcast result code may be obtained with {@link
     * #getResultCode()} and the Intent may be obtained with {@link #getResultIntent()}.
     */
    protected void onActionReceived() {
        if (mResultCode == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK) {
            setState(State.SUCCESS, Substate.UNUSED);
        } else {
            setState(State.ERROR, mResultCode);
        }
    }

    /**
     * The extended class should implement it to return a string for the broadcast action. The class
     * should be unique across all the child classes.
     */
    protected abstract String getReceiverAction();

    protected PendingIntent createCallbackIntent() {
        mOpId = sCurrentOpId.incrementAndGet();
        Intent intent = new Intent(getReceiverAction());
        intent.putExtra(EXTRA_OP_ID, mOpId);
        return PendingIntent.getBroadcast(
                getContext(), REQUEST_CODE, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEuiccManager = getContext().getSystemService(EuiccManager.class);
        mTelephonyManager = getContext().getSystemService(TelephonyManager.class);
        mSwitchSlotSidecar = SwitchSlotSidecar.get(getChildFragmentManager());

        getContext()
                .getApplicationContext()
                .registerReceiver(
                        mReceiver,
                        new IntentFilter(getReceiverAction()),
                        Manifest.permission.WRITE_EMBEDDED_SUBSCRIPTIONS,
                        null,
                        Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitchSlotSidecar.addListener(this);
    }

    @Override
    public void onPause() {
        mSwitchSlotSidecar.removeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        getContext().getApplicationContext().unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onStateChange(SidecarFragment fragment) {
        if (fragment == mSwitchSlotSidecar) {
            switch (mSwitchSlotSidecar.getState()) {
                case State.SUCCESS:
                    mSwitchSlotSidecar.reset();
                    Log.i(TAG, "mSwitchSlotSidecar SUCCESS");
                    break;
                case State.ERROR:
                    mSwitchSlotSidecar.reset();
                    Log.i(TAG, "mSwitchSlotSidecar ERROR");
                    break;
            }
        } else {
            Log.wtf(TAG, "Received state change from a sidecar not expected.");
        }
    }

    public int getResultCode() {
        return mResultCode;
    }

    public int getDetailedCode() {
        return mDetailedCode;
    }

    public Intent getResultIntent() {
        return mResultIntent;
    }
}

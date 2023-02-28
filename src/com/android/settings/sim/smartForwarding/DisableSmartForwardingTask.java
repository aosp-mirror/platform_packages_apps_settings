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

import static com.android.settings.sim.smartForwarding.SmartForwardingUtils.TAG;

import android.telephony.CallForwardingInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.settings.sim.smartForwarding.EnableSmartForwardingTask.UpdateCommand;

import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DisableSmartForwardingTask implements Runnable {
    private final TelephonyManager tm;
    private final boolean[] callWaitingStatus;
    private final CallForwardingInfo[] callForwardingInfo;

    public DisableSmartForwardingTask(TelephonyManager tm,
            boolean[] callWaitingStatus, CallForwardingInfo[] callForwardingInfo) {
        this.tm = tm;
        this.callWaitingStatus = callWaitingStatus;
        this.callForwardingInfo = callForwardingInfo;
    }

    @Override
    public void run() {
        FlowController controller = new FlowController();
        if (controller.init()) {
            controller.startProcess();
        }
    }

    class FlowController {
        private final ArrayList<UpdateCommand> mSteps = new ArrayList<>();

        /* package */ boolean init() {
            if (tm == null) {
                Log.e(TAG, "TelephonyManager is null");
                return false;
            }

            if (callWaitingStatus == null || callForwardingInfo == null) {
                Log.e(TAG, "CallWaitingStatus or CallForwardingInfo array is null");
                return false;
            }

            int slotCount = tm.getActiveModemCount();
            if (callWaitingStatus.length != slotCount || callForwardingInfo.length != slotCount) {
                Log.e(TAG, "The length of CallWaitingStatus and CallForwardingInfo array"
                        + " should be the same as phone count.");
                return false;
            }

            Executor executor = Executors.newSingleThreadExecutor();

            for (int i = 0; i < slotCount; i++) {
                int subId = getSubId(i);
                if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    continue;
                }

                mSteps.add(new RestoreCallWaitingCommand(
                        tm, executor, callWaitingStatus[i], subId));

                if (callForwardingInfo[i] != null) {
                    mSteps.add(new RestoreCallForwardingCommand(
                            tm, executor, callForwardingInfo[i], subId));
                }
            }

            return true;
        }

        /* package */ void startProcess() {
            int index = 0;

            while (index < mSteps.size()) {
                UpdateCommand currentStep = mSteps.get(index);
                Log.d(TAG, "processing : " + currentStep);

                try {
                    currentStep.process();
                    index++;
                } catch (Exception e) {
                    Log.e(TAG, "Failed on : " + currentStep, e);
                }
            }
        }
    }

    class RestoreCallForwardingCommand extends UpdateCommand<Integer> {
        private SettableFuture<Boolean> mResultFuture = SettableFuture.create();
        private CallForwardingInfo mCallForwardingInfo;

        /* package */ RestoreCallForwardingCommand(TelephonyManager tm, Executor executor,
                CallForwardingInfo mCallForwardingInfo, int subId) {
            super(tm, executor, subId);
            this.mCallForwardingInfo = mCallForwardingInfo;
        }

        @Override
        public boolean process() throws Exception {
            Log.d(TAG, "Restore call forwarding to " + mCallForwardingInfo);
            tm.createForSubscriptionId(subId).setCallForwarding(mCallForwardingInfo, executor,
                    this::updateStatusCallBack);
            return mResultFuture.get();
        }

        @Override
        void onRestore() {
        }

        private void updateStatusCallBack(int result) {
            Log.d(TAG, "updateStatusCallBack for CallForwarding: " + result);
            mResultFuture.set(true);
        }
    }

    class RestoreCallWaitingCommand extends UpdateCommand<Integer> {
        private SettableFuture<Boolean> mResultFuture = SettableFuture.create();
        private boolean mCallWaitingStatus;

        /* package */ RestoreCallWaitingCommand(TelephonyManager tm, Executor executor,
                boolean mCallWaitingStatus, int subId) {
            super(tm, executor, subId);
            this.mCallWaitingStatus = mCallWaitingStatus;
        }

        @Override
        public boolean process() throws Exception {
            Log.d(TAG, "Restore call waiting to " + mCallWaitingStatus);
            tm.createForSubscriptionId(subId).setCallWaitingEnabled(mCallWaitingStatus, executor,
                    this::updateStatusCallBack);
            return mResultFuture.get();
        }

        @Override
        void onRestore() {
        }

        private void updateStatusCallBack(int result) {
            Log.d(TAG, "updateStatusCallBack for CallWaiting: " + result);
            mResultFuture.set(true);
        }
    }

    private int getSubId(int slotIndex) {
        return SubscriptionManager.getSubscriptionId(slotIndex);
    }
}

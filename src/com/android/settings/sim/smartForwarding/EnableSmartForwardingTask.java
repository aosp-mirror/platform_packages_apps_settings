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

import static android.telephony.CallForwardingInfo.REASON_NOT_REACHABLE;

import static com.android.settings.sim.smartForwarding.SmartForwardingUtils.TAG;

import android.content.Context;
import android.telephony.CallForwardingInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EnableSmartForwardingTask
        implements Callable<EnableSmartForwardingTask.FeatureResult> {

    private static final int TIMEOUT = 20;

    private final SubscriptionManager sm;
    private final TelephonyManager tm;
    private final String[] mCallForwardingNumber;

    FeatureResult mResult = new FeatureResult(false, null);
    SettableFuture<FeatureResult> client = SettableFuture.create();

    public EnableSmartForwardingTask(Context context, String[] callForwardingNumber) {
        tm = context.getSystemService(TelephonyManager.class);
        sm = context.getSystemService(SubscriptionManager.class);
        mCallForwardingNumber = callForwardingNumber;
    }

    @Override
    public FeatureResult call() throws TimeoutException, InterruptedException, ExecutionException {
        FlowController controller = new FlowController();
        if (controller.init(mCallForwardingNumber)) {
            controller.startProcess();
        } else {
            client.set(mResult);
        }

        return client.get(TIMEOUT, TimeUnit.SECONDS);
    }

    class FlowController {
        private SlotUTData[] mSlotUTData;
        private final ArrayList<Command> mSteps = new ArrayList<>();

        public boolean init(String[] phoneNum) {
            if (!initObject(phoneNum)) return false;
            initSteps();
            return true;
        }

        private boolean initObject(String[] phoneNum) {
            Executor executor = Executors.newSingleThreadExecutor();
            if (tm == null || sm == null) {
                Log.e(TAG, "TelephonyManager or SubscriptionManager is null");
                return false;
            }

            if (phoneNum.length != tm.getActiveModemCount()) {
                Log.e(TAG, "The length of PhoneNum array should same as phone count.");
                return false;
            }

            mSlotUTData = new SlotUTData[tm.getActiveModemCount()];
            for (int i = 0; i < mSlotUTData.length; i++) {
                int subId = SubscriptionManager.getSubscriptionId(i);

                if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                    mResult.setReason(FeatureResult.FailedReason.SIM_NOT_ACTIVE);
                    return false;
                }

                QueryCallWaitingCommand queryCallWaitingCommand =
                        new QueryCallWaitingCommand(tm, executor, subId);
                QueryCallForwardingCommand queryCallForwardingCommand =
                        new QueryCallForwardingCommand(tm, executor, subId);
                UpdateCallWaitingCommand updateCallWaitingCommand =
                        new UpdateCallWaitingCommand(tm, executor, queryCallWaitingCommand, subId);
                UpdateCallForwardingCommand updateCallForwardingCommand =
                        new UpdateCallForwardingCommand(tm, executor, queryCallForwardingCommand,
                                subId, phoneNum[i]);

                mSlotUTData[i] = new SlotUTData(subId, phoneNum[i],
                        queryCallWaitingCommand,
                        queryCallForwardingCommand,
                        updateCallWaitingCommand,
                        updateCallForwardingCommand);
            }
            return true;
        }

        private void initSteps() {
            // 1. Query call waiting for each slots
            for (SlotUTData slotUTData : mSlotUTData) {
                mSteps.add(slotUTData.getQueryCallWaitingCommand());
            }

            // 2. Query call forwarding for each slots
            for (SlotUTData slotUTData : mSlotUTData) {
                mSteps.add(slotUTData.getQueryCallForwardingCommand());
            }

            // 3. Enable call waiting for each slots
            for (SlotUTData slotUTData : mSlotUTData) {
                mSteps.add(slotUTData.getUpdateCallWaitingCommand());
            }

            // 4. Set call forwarding for each slots
            for (SlotUTData slotUTData : mSlotUTData) {
                mSteps.add(slotUTData.getUpdateCallForwardingCommand());
            }
        }

        public void startProcess() {
            int index = 0;
            boolean result = true;

            // go through all steps
            while (index < mSteps.size() && result) {
                Command currentStep = mSteps.get(index);
                Log.d(TAG, "processing : " + currentStep);

                try {
                    result = currentStep.process();
                } catch (Exception e) {
                    Log.d(TAG, "Failed on : " + currentStep, e);
                    result = false;
                }

                if (result) {
                    index++;
                } else {
                    Log.d(TAG, "Failed on : " + currentStep);
                }
            }

            if (result) {
                // No more steps need to perform, return successful to UI.
                mResult.result = true;
                mResult.slotUTData = mSlotUTData;
                Log.d(TAG, "Smart forwarding successful");
                client.set(mResult);
            } else {
                restoreAllSteps(index);
                client.set(mResult);
            }
        }

        private void restoreAllSteps(int index) {
            List<Command> restoreCommands = mSteps.subList(0, index);
            Collections.reverse(restoreCommands);
            for (Command currentStep : restoreCommands) {
                Log.d(TAG, "restoreStep: " + currentStep);
                // Only restore update steps
                if (currentStep instanceof UpdateCommand) {
                    ((UpdateCommand) currentStep).onRestore();
                }
            }
        }
    }

    final class SlotUTData {
        int subId;
        String mCallForwardingNumber;

        QueryCallWaitingCommand mQueryCallWaiting;
        QueryCallForwardingCommand mQueryCallForwarding;
        UpdateCallWaitingCommand mUpdateCallWaiting;
        UpdateCallForwardingCommand mUpdateCallForwarding;

        public SlotUTData(int subId,
                String callForwardingNumber,
                QueryCallWaitingCommand queryCallWaiting,
                QueryCallForwardingCommand queryCallForwarding,
                UpdateCallWaitingCommand updateCallWaiting,
                UpdateCallForwardingCommand updateCallForwarding) {
            this.subId = subId;
            this.mCallForwardingNumber = callForwardingNumber;
            this.mQueryCallWaiting = queryCallWaiting;
            this.mQueryCallForwarding = queryCallForwarding;
            this.mUpdateCallWaiting = updateCallWaiting;
            this.mUpdateCallForwarding = updateCallForwarding;
        }

        public QueryCallWaitingCommand getQueryCallWaitingCommand() {
            return mQueryCallWaiting;
        }

        public QueryCallForwardingCommand getQueryCallForwardingCommand() {
            return mQueryCallForwarding;
        }

        public UpdateCallWaitingCommand getUpdateCallWaitingCommand() {
            return mUpdateCallWaiting;
        }

        public UpdateCallForwardingCommand getUpdateCallForwardingCommand() {
            return mUpdateCallForwarding;
        }
    }

    interface Command {
        boolean process() throws Exception;
    }

    abstract static class QueryCommand<T> implements Command {
        int subId;
        TelephonyManager tm;
        Executor executor;

        public QueryCommand(TelephonyManager tm, Executor executor, int subId) {
            this.subId = subId;
            this.tm = tm;
            this.executor = executor;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "[SubId " + subId + "]";
        }

        abstract T getResult();
    }

    abstract static class UpdateCommand<T> implements Command {
        int subId;
        TelephonyManager tm;
        Executor executor;

        public UpdateCommand(TelephonyManager tm, Executor executor, int subId) {
            this.subId = subId;
            this.tm = tm;
            this.executor = executor;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "[SubId " + subId + "] ";
        }

        abstract void onRestore();
    }

    static class QueryCallWaitingCommand extends QueryCommand<Integer> {
        int result;
        SettableFuture<Boolean> resultFuture = SettableFuture.create();

        public QueryCallWaitingCommand(TelephonyManager tm, Executor executor, int subId) {
            super(tm, executor, subId);
        }

        @Override
        public boolean process() throws Exception {
            tm.createForSubscriptionId(subId)
                    .getCallWaitingStatus(executor, this::queryStatusCallBack);
            return resultFuture.get();
        }

        @Override
        Integer getResult() {
            return result;
        }

        public void queryStatusCallBack(int result) {
            this.result = result;

            if (result == TelephonyManager.CALL_WAITING_STATUS_ENABLED
                    || result == TelephonyManager.CALL_WAITING_STATUS_DISABLED) {
                Log.d(TAG, "Call Waiting result: " + result);
                resultFuture.set(true);
            } else {
                resultFuture.set(false);
            }
        }
    }

    static class QueryCallForwardingCommand extends QueryCommand<CallForwardingInfo> {
        CallForwardingInfo result;
        SettableFuture<Boolean> resultFuture = SettableFuture.create();

        public QueryCallForwardingCommand(TelephonyManager tm, Executor executor, int subId) {
            super(tm, executor, subId);
        }

        @Override
        public boolean process() throws Exception{
            tm.createForSubscriptionId(subId)
                    .getCallForwarding(REASON_NOT_REACHABLE, executor,
                            new TelephonyManager.CallForwardingInfoCallback() {
                                @Override
                                public void onCallForwardingInfoAvailable(CallForwardingInfo info) {
                                    Log.d(TAG, "Call Forwarding result: " + info);
                                    result = info;
                                    resultFuture.set(true);
                                }

                                @Override
                                public void onError(int error) {
                                    Log.d(TAG, "Query Call Forwarding failed.");
                                    resultFuture.set(false);
                                }
                            });
            return resultFuture.get();
        }

        @Override
        CallForwardingInfo getResult() {
            return result;
        }
    }

    static class UpdateCallWaitingCommand extends UpdateCommand<Integer> {
        SettableFuture<Boolean> resultFuture = SettableFuture.create();
        QueryCallWaitingCommand queryResult;

        public UpdateCallWaitingCommand(TelephonyManager tm, Executor executor,
                QueryCallWaitingCommand queryCallWaitingCommand, int subId) {
            super(tm, executor, subId);
            this.queryResult = queryCallWaitingCommand;
        }

        @Override
        public boolean process() throws Exception {
            tm.createForSubscriptionId(subId)
                    .setCallWaitingEnabled(true, executor, this::updateStatusCallBack);
            return resultFuture.get();
        }

        public void updateStatusCallBack(int result) {
            Log.d(TAG, "UpdateCallWaitingCommand updateStatusCallBack result: " + result);
            if (result == TelephonyManager.CALL_WAITING_STATUS_ENABLED
                    || result == TelephonyManager.CALL_WAITING_STATUS_DISABLED) {
                resultFuture.set(true);
            } else {
                resultFuture.set(false);
            }
        }

        @Override
        void onRestore() {
            Log.d(TAG, "onRestore: " + this);
            if (queryResult.getResult() != TelephonyManager.CALL_WAITING_STATUS_ENABLED) {
                tm.createForSubscriptionId(subId)
                        .setCallWaitingEnabled(false, null, null);
            }
        }
    }

    static class UpdateCallForwardingCommand extends UpdateCommand<Integer> {
        String phoneNum;
        SettableFuture<Boolean> resultFuture = SettableFuture.create();
        QueryCallForwardingCommand queryResult;

        public UpdateCallForwardingCommand(TelephonyManager tm, Executor executor,
                QueryCallForwardingCommand queryCallForwardingCommand,
                int subId, String phoneNum) {
            super(tm, executor, subId);
            this.phoneNum = phoneNum;
            this.queryResult = queryCallForwardingCommand;
        }

        @Override
        public boolean process() throws Exception {
            CallForwardingInfo info = new CallForwardingInfo(
                    true, REASON_NOT_REACHABLE, phoneNum, 3);
            tm.createForSubscriptionId(subId)
                    .setCallForwarding(info, executor, this::updateStatusCallBack);
            return resultFuture.get();
        }

        public void updateStatusCallBack(int result) {
            Log.d(TAG, "UpdateCallForwardingCommand updateStatusCallBack : " + result);
            if (result == TelephonyManager.CallForwardingInfoCallback.RESULT_SUCCESS) {
                resultFuture.set(true);
            } else {
                resultFuture.set(false);
            }
        }

        @Override
        void onRestore() {
            Log.d(TAG, "onRestore: " + this);

            tm.createForSubscriptionId(subId)
                    .setCallForwarding(queryResult.getResult(), null, null);
        }
    }

    public static class FeatureResult {
        enum FailedReason {
            NETWORK_ERROR,
            SIM_NOT_ACTIVE
        }

        private boolean result;
        private FailedReason reason;
        private SlotUTData[] slotUTData;

        public FeatureResult(boolean result, SlotUTData[] slotUTData) {
            this.result = result;
            this.slotUTData = slotUTData;
        }

        public boolean getResult() {
            return result;
        }

        public SlotUTData[] getSlotUTData() {
            return slotUTData;
        }

        public void setReason(FailedReason reason) {
            this.reason = reason;
        }

        public FailedReason getReason() {
            return reason;
        }
    }
}

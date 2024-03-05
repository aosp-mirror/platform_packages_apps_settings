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

import android.annotation.IntDef;
import android.app.FragmentManager;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import com.android.settings.AsyncTaskSidecar;
import com.android.settings.SidecarFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nullable;

/** {@link SidecarFragment} to switch SIM slot. */
public class SwitchSlotSidecar
        extends AsyncTaskSidecar<SwitchSlotSidecar.Param, SwitchSlotSidecar.Result> {
    private static final String TAG = "SwitchSlotSidecar";

    /** Commands */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        Command.SWITCH_TO_REMOVABLE_SIM,
    })
    private @interface Command {
        int SWITCH_TO_REMOVABLE_SIM = 0;
        int SWITCH_TO_EUICC_SIM = 1;
    }

    static class Param {
        @Command int command;
        int slotId;
        int port;
        SubscriptionInfo removedSubInfo;
    }

    static class Result {
        Exception exception;
    }

    /** Returns a SwitchSlotSidecar sidecar instance. */
    public static SwitchSlotSidecar get(FragmentManager fm) {
        return SidecarFragment.get(fm, TAG, SwitchSlotSidecar.class, null /* args */);
    }

    @Nullable private Exception mException;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /** Starts switching to the removable slot. */
    public void runSwitchToRemovableSlot(int id, SubscriptionInfo removedSubInfo) {
        Param param = new Param();
        param.command = Command.SWITCH_TO_REMOVABLE_SIM;
        param.slotId = id;
        param.removedSubInfo = removedSubInfo;
        param.port = 0;
        super.run(param);
    }

    /**
     * Start the SimSlotMapping process if the euicc slot is not in SimSlotMapping list.
     * @param physicalSlotId The physical slot id.
     * @param port The port id.
     * @param removedSubInfo The subscriptionInfo which is selected by the user to disable when all
     *                      of sim slots are full in the device. If all of slots are not full in
     *                       the device, then this is null.
     */
    public void runSwitchToEuiccSlot(int physicalSlotId, int port,
            SubscriptionInfo removedSubInfo) {
        Param param = new Param();
        param.command = Command.SWITCH_TO_EUICC_SIM;
        param.slotId = physicalSlotId;
        param.removedSubInfo = removedSubInfo;
        param.port = port;
        super.run(param);
    }
    /**
     * Returns the exception thrown during the execution of a command. Will be null in any state
     * other than {@link State#SUCCESS}, and may be null in that state if there was not an error.
     */
    @Nullable
    public Exception getException() {
        return mException;
    }

    @Override
    protected Result doInBackground(@Nullable Param param) {
        Result result = new Result();
        if (param == null) {
            result.exception = new UiccSlotsException("Null param");
            return result;
        }
        try {
            switch (param.command) {
                case Command.SWITCH_TO_REMOVABLE_SIM:
                    Log.i(TAG, "Start to switch to removable slot.");
                    UiccSlotUtil.switchToRemovableSlot(getContext(), param.slotId,
                            param.removedSubInfo);
                    break;
                case Command.SWITCH_TO_EUICC_SIM:
                    Log.i(TAG, "Start to switch to euicc slot.");
                    UiccSlotUtil.switchToEuiccSlot(getContext(), param.slotId, param.port,
                            param.removedSubInfo);
                    break;
                default:
                    Log.e(TAG, "Wrong command.");
                    break;
            }
        } catch (UiccSlotsException e) {
            result.exception = e;
        }
        Log.i(TAG, "return command.");
        return result;
    }

    @Override
    protected void onPostExecute(Result result) {
        Log.i(TAG, "onPostExecute: get result");
        if (result.exception == null) {
            setState(State.SUCCESS, Substate.UNUSED);
        } else {
            mException = result.exception;
            setState(State.ERROR, Substate.UNUSED);
        }
    }
}

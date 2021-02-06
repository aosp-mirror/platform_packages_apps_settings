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

package com.android.settings.sim.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;

/** The receiver when SUW is finished. */
public class SuwFinishReceiver extends BroadcastReceiver {
    private static final String TAG = "SuwFinishReceiver";

    private final SimSlotChangeHandler mSlotChangeHandler = SimSlotChangeHandler.get();
    private final Object mLock = new Object();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!context.getResources().getBoolean(R.bool.config_handle_sim_slot_change)) {
            Log.i(TAG, "The flag is off. Ignore SUW finish event.");
            return;
        }

        final BroadcastReceiver.PendingResult pendingResult = goAsync();
        ThreadUtils.postOnBackgroundThread(
                () -> {
                    synchronized (mLock) {
                        Log.i(TAG, "Detected SUW finished. Checking slot events.");
                        mSlotChangeHandler.onSuwFinish(context.getApplicationContext());
                    }
                    ThreadUtils.postOnMainThread(pendingResult::finish);
                });
    }
}

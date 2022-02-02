/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.safetycenter;

import static android.safetycenter.SafetyCenterManager.EXTRA_REFRESH_SAFETY_SOURCE_IDS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.common.collect.ImmutableList;

/** Broadcast receiver for handling requests from Safety Center for fresh data. */
public class SafetySourceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SafetyCenterStatusHolder.get().isEnabled(context)) {
            return;
        }

        ImmutableList<String> sourceIds =
                ImmutableList.copyOf(intent.getStringArrayExtra(EXTRA_REFRESH_SAFETY_SOURCE_IDS));

        if (sourceIds.contains(LockScreenSafetySource.SAFETY_SOURCE_ID)) {
            LockScreenSafetySource.sendSafetyData(context);
        }

        if (sourceIds.contains(BiometricsSafetySource.SAFETY_SOURCE_ID)) {
            BiometricsSafetySource.sendSafetyData(context);
        }
    }
}

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

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.safetycenter.SafetyCenterManager.ACTION_REFRESH_SAFETY_SOURCES;
import static android.safetycenter.SafetyCenterManager.EXTRA_REFRESH_SAFETY_SOURCE_IDS;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.settings.security.ScreenLockPreferenceDetailsUtils;

import com.google.common.collect.ImmutableList;

import java.util.List;

/** Broadcast receiver for handling requests from Safety Center for fresh data. */
public class SafetySourceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return;
        }

        if (ACTION_REFRESH_SAFETY_SOURCES.equals(intent.getAction())) {
            String[] sourceIdsExtra =
                    intent.getStringArrayExtra(EXTRA_REFRESH_SAFETY_SOURCE_IDS);
            if (sourceIdsExtra != null && sourceIdsExtra.length > 0) {
                refreshSafetySources(context, ImmutableList.copyOf(sourceIdsExtra));
            }
            return;
        }


        if (ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            refreshAllSafetySources(context);
        }
    }

    private static void refreshSafetySources(Context context, List<String> sourceIds) {
        if (sourceIds.contains(LockScreenSafetySource.SAFETY_SOURCE_ID)) {
            LockScreenSafetySource.sendSafetyData(context,
                    new ScreenLockPreferenceDetailsUtils(context, SettingsEnums.SAFETY_CENTER));
        }

        if (sourceIds.contains(BiometricsSafetySource.SAFETY_SOURCE_ID)) {
            BiometricsSafetySource.sendSafetyData(context);
        }
    }

    private static void refreshAllSafetySources(Context context) {
        LockScreenSafetySource.sendSafetyData(context,
                new ScreenLockPreferenceDetailsUtils(context, SettingsEnums.SAFETY_CENTER));
        BiometricsSafetySource.sendSafetyData(context);
    }
}

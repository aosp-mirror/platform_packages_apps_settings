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
import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_DEVICE_REBOOTED;
import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_REFRESH_REQUESTED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.safetycenter.SafetyCenterManager;
import android.safetycenter.SafetyEvent;

import com.android.settings.privatespace.PrivateSpaceSafetySource;
import com.android.settings.security.ScreenLockPreferenceDetailsUtils;

import com.google.common.collect.ImmutableList;

import java.util.List;

/** Broadcast receiver for handling requests from Safety Center for fresh data. */
public class SafetySourceBroadcastReceiver extends BroadcastReceiver {

    private static final SafetyEvent EVENT_DEVICE_REBOOTED =
            new SafetyEvent.Builder(SAFETY_EVENT_TYPE_DEVICE_REBOOTED).build();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return;
        }

        if (ACTION_REFRESH_SAFETY_SOURCES.equals(intent.getAction())) {
            String[] sourceIdsExtra =
                    intent.getStringArrayExtra(EXTRA_REFRESH_SAFETY_SOURCE_IDS);
            final String refreshBroadcastId = intent.getStringExtra(
                    SafetyCenterManager.EXTRA_REFRESH_SAFETY_SOURCES_BROADCAST_ID);
            if (sourceIdsExtra != null && sourceIdsExtra.length > 0 && refreshBroadcastId != null) {
                final SafetyEvent safetyEvent = new SafetyEvent.Builder(
                        SAFETY_EVENT_TYPE_REFRESH_REQUESTED)
                        .setRefreshBroadcastId(refreshBroadcastId).build();
                refreshSafetySources(
                        context,
                        ImmutableList.copyOf(sourceIdsExtra),
                        safetyEvent);
            }
            return;
        }


        if (ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            refreshAllSafetySources(context, EVENT_DEVICE_REBOOTED);
        }
    }

    private static void refreshSafetySources(Context context, List<String> sourceIds,
            SafetyEvent safetyEvent) {
        if (sourceIds.contains(LockScreenSafetySource.SAFETY_SOURCE_ID)) {
            LockScreenSafetySource.setSafetySourceData(context,
                    new ScreenLockPreferenceDetailsUtils(context), safetyEvent);
        }

        if (sourceIds.contains(BiometricsSafetySource.SAFETY_SOURCE_ID)) {
            BiometricsSafetySource.setSafetySourceData(context, safetyEvent);
        }

        if (sourceIds.contains(PrivateSpaceSafetySource.SAFETY_SOURCE_ID)) {
            PrivateSpaceSafetySource.setSafetySourceData(context, safetyEvent);
        }
    }

    private static void refreshAllSafetySources(Context context, SafetyEvent safetyEvent) {
        LockScreenSafetySource.setSafetySourceData(context,
                new ScreenLockPreferenceDetailsUtils(context), safetyEvent);
        BiometricsSafetySource.setSafetySourceData(context, safetyEvent);
        PrivateSpaceSafetySource.setSafetySourceData(context, safetyEvent);
    }
}

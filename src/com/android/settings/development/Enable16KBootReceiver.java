/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.development;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import androidx.annotation.NonNull;

public class Enable16KBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || PageAgnosticNotificationService.INTENT_ACTION_DISMISSED.equals(action)) {
            // Do nothing if device is not in page-agnostic mode
            if (!Enable16kUtils.isPageAgnosticModeOn(context)) {
                return;
            }

            // start a service to post persistent notification
            Intent startServiceIntent = new Intent(context, PageAgnosticNotificationService.class);
            startServiceIntent.setAction(action);
            context.startServiceAsUser(startServiceIntent, UserHandle.SYSTEM);
        }
    }
}

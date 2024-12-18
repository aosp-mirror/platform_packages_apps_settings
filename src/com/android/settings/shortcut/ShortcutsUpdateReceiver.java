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

package com.android.settings.shortcut;

import android.app.Flags;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.settingslib.utils.ThreadUtils;

public class ShortcutsUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "ShortcutsUpdateReceiver";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (!Flags.modesApi() || !Flags.modesUi()) {
            return;
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            PendingResult pendingResult = goAsync();

            ThreadUtils.getBackgroundExecutor().execute(() -> {
                try {
                    ShortcutsUpdater.updatePinnedShortcuts(context);
                } catch (Exception e) {
                    Log.e(TAG, "Error trying to update Settings shortcuts", e);
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
}

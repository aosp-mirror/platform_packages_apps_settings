/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import static com.android.settings.deviceinfo.StorageSettings.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;

import com.android.settings.deviceinfo.StorageSettings.UnmountTask;

public class StorageUnmountReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final StorageManager storage = context.getSystemService(StorageManager.class);

        final String volId = intent.getStringExtra(VolumeInfo.EXTRA_VOLUME_ID);
        final VolumeInfo vol = storage.findVolumeById(volId);
        if (vol != null) {
            new UnmountTask(context, vol).execute();
        } else {
            Log.w(TAG, "Missing volume " + volId);
        }
    }
}

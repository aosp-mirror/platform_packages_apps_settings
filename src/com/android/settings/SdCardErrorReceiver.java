/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.widget.Toast;


/**
 * Glue class: connects AlarmAlert IntentReceiver to AlarmAlert
 * activity.  Passes through Alarm ID.
 */
public class SdCardErrorReceiver extends BroadcastReceiver {

    private static final String TAG = "SdCardErrorReceiver";

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        final int duration = 3500;
        if (action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
            Toast.makeText(context, R.string.sdcard_removal_alert_title, duration).show();
        } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTABLE)) {
            Toast.makeText(context, R.string.sdcard_unmountable_alert_title, duration).show();
        } else {
            Log.e(TAG, "unknown intent");
        }
    }
}

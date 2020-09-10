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

package com.android.settings.display;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Settings;

/*
 * This class lauches a dialog when users try to use twilight scheduling without
 * turning on location services
 */
public class TwilightLocationDialog {
    public static String TAG = "TwilightLocationDialog";

    public static void show(Context context) {
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.twilight_mode_launch_location, ((dialog1, which) -> {
                    Log.d(TAG, "clicked forget");
                    final Intent intent = new Intent();
                    intent.setClass(context, Settings.LocationSettingsActivity.class);
                    context.startActivity(intent);
                }))
                .setNegativeButton(R.string.cancel, null /* listener */)
                .setMessage(R.string.twilight_mode_location_off_dialog_message)
                .create();
        dialog.show();
    }
}

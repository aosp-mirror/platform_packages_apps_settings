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

package com.android.settings.fuelgauge;

import android.content.ContentResolver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.fuelgauge.batterysaver.BatterySaverScheduleRadioButtonsController;

/** Execute battery settings migration tasks in the device booting stage. */
public final class BatterySettingsMigrateChecker extends BroadcastReceiver {
    private static final String TAG = "BatterySettingsMigrateChecker";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            verifyConfiguration(context);
        }
    }

    static void verifyConfiguration(Context context) {
        context = context.getApplicationContext();
        verifySaverConfiguration(context);
    }

    private static void verifySaverConfiguration(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        final int threshold = Settings.Global.getInt(resolver,
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
        // Force refine the invalid scheduled battery level.
        if (threshold < BatterySaverScheduleRadioButtonsController.TRIGGER_LEVEL_MIN
                && threshold > 0) {
            Settings.Global.putInt(resolver, Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                    BatterySaverScheduleRadioButtonsController.TRIGGER_LEVEL_MIN);
            Log.w(TAG, "Reset invalid scheduled battery level from: " + threshold);
        }
    }
}

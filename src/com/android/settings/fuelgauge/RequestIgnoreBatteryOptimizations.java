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

package com.android.settings.fuelgauge;

import static com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_UNRESTRICTED;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;

public class RequestIgnoreBatteryOptimizations extends AlertActivity
        implements DialogInterface.OnClickListener {
    private static final String TAG = "RequestIgnoreBatteryOptimizations";
    private static final boolean DEBUG = false;

    private ApplicationInfo mApplicationInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow()
                .addSystemFlags(
                        android.view.WindowManager.LayoutParams
                                .SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);

        Uri data = getIntent().getData();
        if (data == null) {
            debugLog(
                    "No data supplied for IGNORE_BATTERY_OPTIMIZATION_SETTINGS in: " + getIntent());
            finish();
            return;
        }
        final String packageName = data.getSchemeSpecificPart();
        if (TextUtils.isEmpty(packageName)) {
            debugLog(
                    "No data supplied for IGNORE_BATTERY_OPTIMIZATION_SETTINGS in: " + getIntent());
            finish();
            return;
        }

        // Package in Unrestricted mode already ignoring the battery optimizations.
        PowerManager power = getSystemService(PowerManager.class);
        if (power.isIgnoringBatteryOptimizations(packageName)) {
            debugLog("Not should prompt, already ignoring optimizations: " + packageName);
            finish();
            return;
        }

        if (getPackageManager()
                        .checkPermission(
                                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                packageName)
                != PackageManager.PERMISSION_GRANTED) {
            debugLog(
                    "Requested package "
                            + packageName
                            + " does not hold permission "
                            + Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            finish();
            return;
        }

        try {
            mApplicationInfo = getPackageManager().getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            debugLog("Requested package doesn't exist: " + packageName);
            finish();
            return;
        }

        final AlertController.AlertParams p = mAlertParams;
        final CharSequence appLabel =
                mApplicationInfo.loadSafeLabel(
                        getPackageManager(),
                        PackageItemInfo.DEFAULT_MAX_LABEL_SIZE_PX,
                        PackageItemInfo.SAFE_LABEL_FLAG_TRIM
                                | PackageItemInfo.SAFE_LABEL_FLAG_FIRST_LINE);
        p.mTitle = getText(R.string.high_power_prompt_title);
        p.mMessage = getString(R.string.high_power_prompt_body, appLabel);
        p.mPositiveButtonText = getText(R.string.allow);
        p.mNegativeButtonText = getText(R.string.deny);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                BatteryOptimizeUtils batteryOptimizeUtils =
                        new BatteryOptimizeUtils(
                                getApplicationContext(),
                                mApplicationInfo.uid,
                                mApplicationInfo.packageName);
                batteryOptimizeUtils.setAppUsageState(MODE_UNRESTRICTED, Action.APPLY);
                break;
            case BUTTON_NEGATIVE:
                break;
        }
    }

    private static void debugLog(String debugContent) {
        if (DEBUG) Log.w(TAG, debugContent);
    }
}

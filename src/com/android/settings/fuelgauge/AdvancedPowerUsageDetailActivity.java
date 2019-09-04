/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.fuelgauge.AdvancedPowerUsageDetail.EXTRA_PACKAGE_NAME;
import static com.android.settings.fuelgauge.AdvancedPowerUsageDetail.EXTRA_POWER_USAGE_PERCENT;
import static com.android.settings.fuelgauge.AdvancedPowerUsageDetail.EXTRA_UID;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.android.settings.core.SubSettingLauncher;
import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.R;
import com.android.settings.Utils;

/**
 * Trampoline activity for launching the {@link AdvancedPowerUsageDetail} fragment.
 */
public class AdvancedPowerUsageDetailActivity extends AppCompatActivity {

    private static final String TAG = "AdvancedPowerDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        final Uri data = intent == null ? null : intent.getData();
        final String packageName = data == null ? null : data.getSchemeSpecificPart();
        if (packageName != null) {
            final Bundle args = new Bundle(4);
            final PackageManager packageManager = getPackageManager();
            args.putString(EXTRA_PACKAGE_NAME, packageName);
            args.putString(EXTRA_POWER_USAGE_PERCENT, Utils.formatPercentage(0));

            if (intent.getBooleanExtra("request_ignore_background_restriction", false)) {
                args.putString(":settings:fragment_args_key", "background_activity");
            }

            try {
                args.putInt(EXTRA_UID, packageManager.getPackageUid(packageName, 0 /* no flag */));
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Cannot find package: " + packageName, e);
            }

            new SubSettingLauncher(this)
                    .setDestination(AdvancedPowerUsageDetail.class.getName())
                    .setTitleRes(R.string.battery_details_title)
                    .setArguments(args)
                    .setSourceMetricsCategory(SettingsEnums.APPLICATIONS_INSTALLED_APP_DETAILS)
                    .launch();
        }

        finish();
    }
}

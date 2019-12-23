/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import static android.provider.Settings.System.ADAPTIVE_SLEEP;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;


public class AdaptiveSleepPreferenceController extends TogglePreferenceController {
    public static final String PREF_NAME = "adaptive_sleep";
    private static final String SYSTEM_KEY = ADAPTIVE_SLEEP;
    private static final int DEFAULT_VALUE = 0;

    public AdaptiveSleepPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return hasSufficientPermission(mContext.getPackageManager()) && Settings.System.getInt(
                mContext.getContentResolver(), SYSTEM_KEY, DEFAULT_VALUE) != DEFAULT_VALUE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.System.putInt(mContext.getContentResolver(), SYSTEM_KEY,
                isChecked ? 1 : DEFAULT_VALUE);
        return true;
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return isControllerAvailable(mContext);
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(isChecked()
                ? R.string.adaptive_sleep_summary_on
                : R.string.adaptive_sleep_summary_off);
    }

    public static int isControllerAvailable(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_adaptive_sleep_available)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    static boolean hasSufficientPermission(PackageManager packageManager) {
        final String attentionPackage = packageManager.getAttentionServicePackageName();
        return attentionPackage != null && packageManager.checkPermission(
                Manifest.permission.CAMERA, attentionPackage) == PackageManager.PERMISSION_GRANTED;
    }
}

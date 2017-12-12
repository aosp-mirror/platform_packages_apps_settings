/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class SafetyInfoPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final Intent INTENT_PROBE =
            new Intent("android.settings.SHOW_SAFETY_AND_REGULATORY_INFO");

    private final PackageManager mPackageManager;

    public SafetyInfoPreferenceController(Context context) {
        super(context);
        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public boolean isAvailable() {
        return !mPackageManager.queryIntentActivities(INTENT_PROBE, 0).isEmpty();
    }

    @Override
    public String getPreferenceKey() {
        return "safety_info";
    }
}

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
import android.content.pm.PackageManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.AbstractPreferenceController;

public class SecurityPatchPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_SECURITY_PATCH = "security_patch";
    private static final String TAG = "SecurityPatchPref";

    private final String mPatch;
    private final PackageManager mPackageManager;

    public SecurityPatchPreferenceController(Context context) {
        super(context);
        mPackageManager = mContext.getPackageManager();
        mPatch = DeviceInfoUtils.getSecurityPatch();
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(mPatch);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SECURITY_PATCH;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY_SECURITY_PATCH);
        if (pref != null) {
            pref.setSummary(mPatch);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_SECURITY_PATCH)) {
            return false;
        }
        if (mPackageManager.queryIntentActivities(preference.getIntent(), 0).isEmpty()) {
            // Don't send out the intent to stop crash
            Log.w(TAG, "Stop click action on " + KEY_SECURITY_PATCH + ": "
                    + "queryIntentActivities() returns empty");
            return true;
        }
        return false;
    }
}

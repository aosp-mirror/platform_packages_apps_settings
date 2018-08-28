/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class VrDisplayPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_VR_DISPLAY_PREF = "vr_display_pref";

    public VrDisplayPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        final PackageManager pm = mContext.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VR_DISPLAY_PREF;
    }

    @Override
    public void updateState(Preference preference) {
        int currentUser = ActivityManager.getCurrentUser();
        int current = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.VR_DISPLAY_MODE, Settings.Secure.VR_DISPLAY_MODE_LOW_PERSISTENCE,
                currentUser);
        if (current == 0) {
            preference.setSummary(R.string.display_vr_pref_low_persistence);
        } else {
            preference.setSummary(R.string.display_vr_pref_off);
        }
    }
}

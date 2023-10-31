/*
* Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development.widevine;

import android.content.Context;
import android.sysprop.WidevineProperties;
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settings.media_drm.Flags;

/**
 * The controller (in the Media Widevine settings) enforces L3 security level
* of Widevine CDM.
*/
public class ForceL3FallbackPreferenceController extends TogglePreferenceController {
    private static final String TAG = "ForceL3FallbackPreferenceController";

    public ForceL3FallbackPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public boolean isChecked() {
        return WidevineProperties.forcel3_enabled().orElse(false);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        WidevineProperties.forcel3_enabled(isChecked);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        if (Flags.forceL3Enabled()) {
            preference.setEnabled(true);
            Log.i(TAG, "forceL3Enabled is on");
        } else {
            preference.setEnabled(false);
            // In case of flag rollback, the controller should be unchecked.
            WidevineProperties.forcel3_enabled(false);
            Log.i(TAG, "forceL3Enabled is off");
        }
        super.updateState(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)) {
            return AVAILABLE;
        } else {
            return CONDITIONALLY_UNAVAILABLE;
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
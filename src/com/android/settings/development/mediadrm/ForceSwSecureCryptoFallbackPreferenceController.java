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

package com.android.settings.development.mediadrm;

import android.content.Context;
import android.media.MediaDrm;
import android.sysprop.WidevineProperties;
import android.util.Log;

import androidx.preference.Preference;

import java.util.UUID;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settings.media_drm.Flags;

/**
 * The controller (in the Media Drm settings) enforces software secure crypto.
*/
public class ForceSwSecureCryptoFallbackPreferenceController extends TogglePreferenceController {
    private static final String TAG = "ForceSwSecureCryptoFallbackPreferenceController";
    private static final UUID WIDEVINE_UUID =
        new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);

    public ForceSwSecureCryptoFallbackPreferenceController(Context context, String preferenceKey) {
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
        boolean isEnable = false;
        if (Flags.forceL3Enabled()) {
            try (MediaDrm drm = new MediaDrm(WIDEVINE_UUID)) {
                String version = drm.getPropertyString(MediaDrm.PROPERTY_VERSION);
                if (Integer.parseInt(version.split("\\.", 2)[0]) >= 19) {
                    isEnable = true;
                }
            } catch (Exception ex) {
                Log.e(TAG, "An exception occurred:", ex);
            }
        }

        preference.setEnabled(isEnable);
        if (!isEnable) {
            // In case of flag rollback, the controller should be unchecked.
            WidevineProperties.forcel3_enabled(false);
        }
        Log.i(TAG, "Force software crypto is " + isEnable);
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
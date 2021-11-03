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

package com.android.settings.emergency;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.emergencynumber.EmergencyNumberUtils;

/**
 * Preference controller for emergency sos gesture setting
 */
public class EmergencyGestureSoundPreferenceController extends TogglePreferenceController {

    @VisibleForTesting
    EmergencyNumberUtils mEmergencyNumberUtils;

    public EmergencyGestureSoundPreferenceController(Context context, String key) {
        super(context, key);
        mEmergencyNumberUtils = new EmergencyNumberUtils(context);
    }

    private static boolean isGestureAvailable(Context context) {
        return context.getResources()
                .getBoolean(R.bool.config_show_emergency_gesture_settings);
    }

    @Override
    public int getAvailabilityStatus() {
        return isGestureAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }

    @Override
    public boolean isChecked() {
        return mEmergencyNumberUtils.getEmergencyGestureSoundEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mEmergencyNumberUtils.setEmergencySoundEnabled(isChecked);
        return true;
    }
}

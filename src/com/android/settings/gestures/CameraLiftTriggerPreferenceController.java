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

package com.android.settings.gestures;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class CameraLiftTriggerPreferenceController extends GesturePreferenceController {

    private static final String PREF_KEY_VIDEO = "gesture_camera_lift_trigger_video";

    private final String mCameraLiftTriggerKey;

    public CameraLiftTriggerPreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, lifecycle);
        mCameraLiftTriggerKey = key;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                R.bool.config_cameraLiftTriggerAvailable);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return mCameraLiftTriggerKey;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.CAMERA_LIFT_TRIGGER_ENABLED, enabled ? 1 : 0);
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        final int triggerEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.CAMERA_LIFT_TRIGGER_ENABLED, 0);
        return triggerEnabled == 1;
    }
}

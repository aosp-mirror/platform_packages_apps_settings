/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.util.ArrayMap;
import com.android.settings.core.lifecycle.Lifecycle;
import com.android.settings.search2.InlineSwitchPayload;
import com.android.settings.search2.ResultPayload;

public class DoubleTapPowerPreferenceController extends GesturePreferenceController {

    private static final String PREF_KEY_VIDEO = "gesture_double_tap_power_video";
    private static final String PREF_KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power";

    public DoubleTapPowerPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled);
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_DOUBLE_TAP_POWER;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, enabled ? 0 : 1);
        return true;
    }

    @Override
    protected boolean isSwitchPrefEnabled() {
        final int cameraDisabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 0);
        return cameraDisabled == 0;
    }

    @Override
    public ResultPayload getResultPayload() {
        ArrayMap<Integer, Boolean> valueMap = new ArrayMap<>();
        valueMap.put(0, true);
        valueMap.put(1, false);

        return new InlineSwitchPayload(Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
                ResultPayload.SettingsSource.SECURE, valueMap);
    }
}

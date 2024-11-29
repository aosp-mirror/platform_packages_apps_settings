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

import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class DoubleTapPowerPreferenceController extends BasePreferenceController {

    public DoubleTapPowerPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    public static boolean isSuggestionComplete(Context context, SharedPreferences prefs) {
        return !isGestureAvailable(context)
                || prefs.getBoolean(DoubleTapPowerSettings.PREF_KEY_SUGGESTION_COMPLETE, false);
    }

    private static boolean isGestureAvailable(@NonNull Context context) {
        return context.getResources()
                .getBoolean(com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled);
    }

    @Override
    public int getAvailabilityStatus() {
        return isGestureAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    @NonNull
    public CharSequence getSummary() {
        final boolean isCameraDoubleTapPowerGestureEnabled =
                Settings.Secure.getInt(
                                mContext.getContentResolver(),
                                CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
                                DoubleTapPowerToOpenCameraPreferenceController.ON)
                        == DoubleTapPowerToOpenCameraPreferenceController.ON;
        return mContext.getText(
                isCameraDoubleTapPowerGestureEnabled
                        ? R.string.gesture_setting_on
                        : R.string.gesture_setting_off);
    }
}

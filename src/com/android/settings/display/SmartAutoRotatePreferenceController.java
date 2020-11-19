/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import static android.provider.Settings.Secure.CAMERA_AUTOROTATE;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * SmartAutoRotatePreferenceController provides auto rotate summary in display settings
 */
public class SmartAutoRotatePreferenceController extends BasePreferenceController {

    private static final String TAG = "SmartAutoRotatePreferenceController";

    public SmartAutoRotatePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return RotationPolicy.isRotationLockToggleVisible(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    protected void update(Preference preference) {
        refreshSummary(preference);
    }

    @Override
    public CharSequence getSummary() {
        int activeStringId = R.string.auto_rotate_option_off;
        if (!RotationPolicy.isRotationLocked(mContext)) {
            try {
                final int cameraRotate = Settings.Secure.getIntForUser(
                        mContext.getContentResolver(),
                        CAMERA_AUTOROTATE,
                        UserHandle.USER_CURRENT);
                activeStringId = cameraRotate == 1 ? R.string.auto_rotate_option_face_based
                        : R.string.auto_rotate_option_on;
            } catch (Settings.SettingNotFoundException e) {
                Log.w(TAG, "CAMERA_AUTOROTATE setting not found", e);
            }
        }
        return mContext.getString(activeStringId);
    }
}

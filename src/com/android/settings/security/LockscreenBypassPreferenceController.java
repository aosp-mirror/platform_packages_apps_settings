/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.security;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.provider.Settings;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.TogglePreferenceController;

public class LockscreenBypassPreferenceController extends TogglePreferenceController {

    @VisibleForTesting
    protected FaceManager mFaceManager;

    public LockscreenBypassPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mFaceManager = context.getSystemService(FaceManager.class);
    }

    @Override
    public boolean isChecked() {
        boolean defaultValue = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_faceAuthDismissesKeyguard);
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD, defaultValue ? 1 : 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD, isChecked ? 1 : 0);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        if (mFaceManager != null && mFaceManager.isHardwareDetected()) {
            return mFaceManager.hasEnrolledTemplates() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }
}

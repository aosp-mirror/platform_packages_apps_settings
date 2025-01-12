/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import static android.provider.Settings.Secure.FACE_APP_ENABLED;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.android.settings.Utils;
import com.android.settings.biometrics.activeunlock.ActiveUnlockStatusUtils;

public class FaceSettingsAppsPreferenceController extends
        FaceSettingsPreferenceController {
    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int DEFAULT = ON;

    private FaceManager mFaceManager;

    public FaceSettingsAppsPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
        mFaceManager = Utils.getFaceManagerOrNull(context);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(), FACE_APP_ENABLED,
                DEFAULT, getUserId()) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putIntForUser(mContext.getContentResolver(), FACE_APP_ENABLED,
                isChecked ? ON : OFF, getUserId());
    }

    @Override
    public int getAvailabilityStatus() {
        final ActiveUnlockStatusUtils activeUnlockStatusUtils =
                new ActiveUnlockStatusUtils(mContext);
        if (!Utils.hasFaceHardware(mContext)
                && !activeUnlockStatusUtils.isAvailable()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (mFaceManager == null) {
            return AVAILABLE_UNSEARCHABLE;
        }
        // This preference will be available only if the user has registered face.
        final boolean hasFaceEnrolledUser = mFaceManager.hasEnrolledTemplates(getUserId());
        if (hasFaceEnrolledUser) {
            return AVAILABLE;
        } else {
            return AVAILABLE_UNSEARCHABLE;
        }
    }
}

/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import static android.provider.Settings.Secure.FACE_UNLOCK_APP_ENABLED;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.Utils;

/**
 * Preference controller for Face settings page controlling the ability to use
 * Face authentication in apps (through BiometricPrompt).
 */
public class FaceSettingsAppPreferenceController extends FaceSettingsPreferenceController {

    static final String KEY = "security_settings_face_app";

    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int DEFAULT = ON;  // face unlock is enabled for BiometricPrompt by default

    private FaceManager mFaceManager;

    public FaceSettingsAppPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mFaceManager = Utils.getFaceManagerOrNull(context);
    }

    public FaceSettingsAppPreferenceController(Context context) {
        this(context, KEY);
    }

    @Override
    public boolean isChecked() {
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            return false;
        }
        return Settings.Secure.getIntForUser(
                mContext.getContentResolver(), FACE_UNLOCK_APP_ENABLED, DEFAULT, getUserId()) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putIntForUser(mContext.getContentResolver(), FACE_UNLOCK_APP_ENABLED,
                isChecked ? ON : OFF, getUserId());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            preference.setEnabled(false);
        } else if (!mFaceManager.hasEnrolledTemplates(getUserId())) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if(mFaceManager == null){
            return AVAILABLE_UNSEARCHABLE;
        }

        // By only allowing this preference controller to be searchable when the feature is turned
        // off, it will give preference to the face setup controller.
        final boolean hasEnrolledUser = mFaceManager.hasEnrolledTemplates(getUserId());
        final boolean appUnlockEnabled = Settings.Secure.getIntForUser(
                mContext.getContentResolver(), FACE_UNLOCK_APP_ENABLED, OFF, getUserId()) == ON;
        if (hasEnrolledUser && !appUnlockEnabled) {
            return AVAILABLE;
        } else {
            return AVAILABLE_UNSEARCHABLE;
        }
    }
}

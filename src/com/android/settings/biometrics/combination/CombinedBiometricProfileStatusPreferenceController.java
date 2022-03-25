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
package com.android.settings.biometrics.combination;

import android.content.Context;
import android.os.UserHandle;

import androidx.lifecycle.Lifecycle;

/**
 * Preference controller for biometrics settings page of work profile, controlling the ability to
 * unlock the phone with face and fingerprint.
 */
public class CombinedBiometricProfileStatusPreferenceController extends
        CombinedBiometricStatusPreferenceController {

    private static final String KEY_BIOMETRIC_SETTINGS = "biometric_settings_profile";

    public CombinedBiometricProfileStatusPreferenceController(Context context) {
        super(context, KEY_BIOMETRIC_SETTINGS);
    }

    public CombinedBiometricProfileStatusPreferenceController(Context context, String key) {
        super(context, key);
    }

    public CombinedBiometricProfileStatusPreferenceController(
            Context context, Lifecycle lifecycle) {
        super(context, KEY_BIOMETRIC_SETTINGS, lifecycle);
    }

    public CombinedBiometricProfileStatusPreferenceController(
            Context context, String key, Lifecycle lifecycle) {
        super(context, key, lifecycle);
    }

    @Override
    protected boolean isUserSupported() {
        return mProfileChallengeUserId != UserHandle.USER_NULL
                && mUm.isManagedProfile(mProfileChallengeUserId);
    }

    @Override
    protected int getUserId() {
        return mProfileChallengeUserId;
    }

    @Override
    protected String getSettingsClassName() {
        return mCombinedBiometricStatusUtils.getProfileSettingsClassName();
    }
}

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

package com.android.settings.privatespace.onelock;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.biometrics.combination.BiometricsSettingsBase;
import com.android.settings.privatespace.PrivateSpaceMaintainer;

public class PrivateSpaceBiometricSettings extends BiometricsSettingsBase {
    private static final String TAG = "PSBiometricSettings";
    private static final String KEY_FACE_SETTINGS = "private_space_face_unlock_settings";
    private static final String KEY_FINGERPRINT_SETTINGS =
            "private_space_fingerprint_unlock_settings";

    @Override
    public void onAttach(Context context) {
        if (android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enableBiometricsToUnlockPrivateSpace()) {
            super.onAttach(context);
            UserHandle privateProfileHandle =
                    PrivateSpaceMaintainer.getInstance(context).getPrivateProfileHandle();
            if (privateProfileHandle != null) {
                mUserId = privateProfileHandle.getIdentifier();
            } else {
                mUserId = -1;
                Log.e(TAG, "Private profile user handle is not expected to be null.");
            }
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.private_space_biometric_settings;
    }

    @Override
    public String getFacePreferenceKey() {
        return KEY_FACE_SETTINGS;
    }

    @Override
    public String getFingerprintPreferenceKey() {
        return KEY_FINGERPRINT_SETTINGS;
    }

    @Override
    public String getUnlockPhonePreferenceKey() {
        return "";
    }

    @Override
    public String getUseInAppsPreferenceKey() {
        return "";
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETTINGS;
    }
}

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
 * limitations under the License.
 */

package com.android.settings.biometrics.face;

import android.content.Context;
import android.hardware.face.FaceManager;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricStatusPreferenceController;

public class FaceStatusPreferenceController extends BiometricStatusPreferenceController {

    public static final String KEY_FACE_SETTINGS = "face_settings";

    protected final FaceManager mFaceManager;

    public FaceStatusPreferenceController(Context context) {
        this(context, KEY_FACE_SETTINGS);
    }

    public FaceStatusPreferenceController(Context context, String key) {
        super(context, key);
        mFaceManager = Utils.getFaceManagerOrNull(context);
    }

    @Override
    protected boolean isDeviceSupported() {
        return mFaceManager != null && mFaceManager.isHardwareDetected();
    }

    @Override
    protected boolean hasEnrolledBiometrics() {
        return mFaceManager.hasEnrolledTemplates(getUserId());
    }

    @Override
    protected String getSummaryTextEnrolled() {
        return mContext.getResources()
                .getString(R.string.security_settings_face_preference_summary);
    }

    @Override
    protected String getSummaryTextNoneEnrolled() {
        return mContext.getResources()
                .getString(R.string.security_settings_face_preference_summary_none);
    }

    @Override
    protected String getSettingsClassName() {
        return Settings.FaceSettingsActivity.class.getName();
    }

    @Override
    protected String getEnrollClassName() {
        return FaceEnrollIntroduction.class.getName();
    }

}

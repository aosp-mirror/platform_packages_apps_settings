/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.face.FaceManager;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.biometrics.ParentalControlsUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/**
 * Utilities for face details shared between Security Settings and Safety Center.
 */
public class FaceStatusUtils {

    private final int mUserId;
    private final Context mContext;
    private final FaceManager mFaceManager;

    public FaceStatusUtils(Context context, FaceManager faceManager, int userId) {
        mContext = context;
        mFaceManager = faceManager;
        mUserId = userId;
    }

    /**
     * Returns whether the face settings entity should be shown.
     */
    public boolean isAvailable() {
        return !Utils.isMultipleBiometricsSupported(mContext) && Utils.hasFaceHardware(mContext);
    }

    /**
     * Returns the {@link EnforcedAdmin} if parental consent is required to change face settings.
     *
     * @return null if face settings does not require a parental consent.
     */
    public EnforcedAdmin getDisablingAdmin() {
        return ParentalControlsUtils.parentConsentRequired(
                mContext, BiometricAuthenticator.TYPE_FACE);
    }

    /**
     * Returns the title of face settings entity.
     */
    public String getTitle() {
        UserManager userManager = mContext.getSystemService(UserManager.class);
        if (userManager != null && userManager.isProfile()) {
            return mContext.getString(R.string.security_settings_face_profile_preference_title);
        } else {
            return mContext.getString(R.string.security_settings_face_preference_title);
        }
    }

    /**
     * Returns the summary of face settings entity.
     */
    public String getSummary() {
        return mContext.getResources().getString(hasEnrolled()
                ? R.string.security_settings_face_preference_summary
                : R.string.security_settings_face_preference_summary_none);
    }

    /**
     * Returns the class name of the Settings page corresponding to face settings.
     */
    public String getSettingsClassName() {
        return hasEnrolled() ? Settings.FaceSettingsInternalActivity.class.getName()
                : FaceEnrollIntroductionInternal.class.getName();
    }

    /**
     * Returns whether at least one face template has been enrolled.
     */
    public boolean hasEnrolled() {
        return mFaceManager.hasEnrolledTemplates(mUserId);
    }
}

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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import static android.provider.Settings.Secure.FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION;

import android.content.Context;
import android.hardware.biometrics.SensorProperties;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorProperties;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;

import java.util.List;

/**
 * Preference controller giving the user an option to always require confirmation.
 */
public class FaceSettingsConfirmPreferenceController extends FaceSettingsPreferenceController {

    static final String KEY = "security_settings_face_require_confirmation";

    private static final int ON = 1;
    private static final int OFF = 0;
    private static final int DEFAULT = OFF;

    private FaceManager mFaceManager;

    public FaceSettingsConfirmPreferenceController(Context context) {
        this(context, KEY);
    }

    public FaceSettingsConfirmPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
        mFaceManager = Utils.getFaceManagerOrNull(context);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION, DEFAULT, getUserId()) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putIntForUser(mContext.getContentResolver(),
                FACE_UNLOCK_ALWAYS_REQUIRE_CONFIRMATION, isChecked ? ON : OFF, getUserId());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            preference.setEnabled(false);
        } else if (!mFaceManager.hasEnrolledTemplates(getUserId())) {
            preference.setEnabled(false);
        } else if (getRestrictingAdmin() != null) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
            // Update summary for private space face settings toggle
            if (Utils.isPrivateProfile(getUserId(), mContext)) {
                preference.setSummary(mContext.getString(
                        R.string.private_space_face_settings_require_confirmation_details));
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        List<FaceSensorProperties> properties = mFaceManager.getSensorProperties();
        // If a sensor is convenience, it is possible that it becomes weak or strong with
        // an update. For this reason, the sensor is conditionally unavailable.
        if (!properties.isEmpty()
                && properties.get(0).getSensorStrength() == SensorProperties.STRENGTH_CONVENIENCE) {
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }
}

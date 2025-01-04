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

import android.content.Context;
import android.hardware.face.FaceManager;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.flags.Flags;

/**
 * Preference controller that manages the ability to use face authentication with/without
 * user attention. See {@link FaceManager#setRequireAttention(boolean, byte[])}.
 */
public class FaceSettingsAttentionPreferenceController extends FaceSettingsPreferenceController {

    public static final String KEY = "security_settings_face_require_attention";

    private TwoStatePreference mPreference;
    private boolean mGazeEnabled;

    private FaceAttentionController mFaceAttentionController;

    private final FaceAttentionController.OnSetAttentionListener mSetAttentionListener =
            (success) -> {
                mPreference.setEnabled(true);
                if (!success) {
                    mPreference.setChecked(!mPreference.isChecked());
                } else {
                    Settings.Secure.putIntForUser(mContext.getContentResolver(),
                            Settings.Secure.FACE_UNLOCK_ATTENTION_REQUIRED,
                            mPreference.isChecked() ? 1 : 0, getUserId());
                }
            };

    private final FaceAttentionController.OnGetAttentionListener mOnGetAttentionListener =
            (success, requireAttentionEnabled) -> {
                mPreference.setChecked(requireAttentionEnabled);
                if (getRestrictingAdmin() != null) {
                    mPreference.setEnabled(false);
                } else {
                    mPreference.setEnabled(success);
                }
            };

    public FaceSettingsAttentionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mFaceAttentionController = new FaceAttentionController(context);
        mGazeEnabled = context.getResources().getBoolean(R.bool.config_gazeEnabled)
                && Flags.biometricsOnboardingEducation();
    }

    public FaceSettingsAttentionPreferenceController(Context context) {
        this(context, KEY);
    }

    public void setToken(byte[] token) {
        if (mFaceAttentionController != null) {
            mFaceAttentionController.setToken(token);
        }
    }

    /**
     * Displays preference in this controller.
     */
    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY);
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        super.updateState(preference);
        if (Utils.isPrivateProfile(getUserId(), mContext)) {
            preference.setSummary(mContext.getString(
                    R.string.private_space_face_settings_require_attention_details));
        } else if (mGazeEnabled) {
            preference.setTitle(mContext.getString(
                    R.string.security_settings_face_settings_gaze));
            preference.setSummary(mContext.getString(
                    R.string.security_settings_face_settings_gaze_details));
        }
    }

    @Override
    public boolean isChecked() {
        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
            return true;
        }
        // Set to disabled until we know the true value.
        mPreference.setEnabled(false);
        mFaceAttentionController.getAttentionStatus(getUserId(), mOnGetAttentionListener);

        // Ideally returns a cached value.
        return true;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        // Optimistically update state and set to disabled until we know it succeeded.
        mPreference.setEnabled(false);
        mPreference.setChecked(isChecked);
        mFaceAttentionController.setAttentionStatus(getUserId(), isChecked, mSetAttentionListener);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}

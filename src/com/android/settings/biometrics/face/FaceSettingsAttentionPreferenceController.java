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

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;

/**
 * Preference controller that manages the ability to use face authentication with/without
 * user attention. See {@link FaceManager#setRequireAttention(boolean, byte[])}.
 */
public class FaceSettingsAttentionPreferenceController extends TogglePreferenceController {

    public static final String KEY = "security_settings_face_require_attention";

    private byte[] mToken;
    private FaceManager mFaceManager;
    private SwitchPreference mPreference;

    public FaceSettingsAttentionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mFaceManager = Utils.getFaceManagerOrNull(context);
    }

    public FaceSettingsAttentionPreferenceController(Context context) {
        this(context, KEY);
    }

    public void setToken(byte[] token) {
        mToken = token;
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
    public boolean isChecked() {
        if (!FaceSettings.isAvailable(mContext)) {
            return true;
        }
        return mFaceManager.getFeature(FaceManager.FEATURE_REQUIRE_ATTENTION);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mFaceManager.setFeature(FaceManager.FEATURE_REQUIRE_ATTENTION, isChecked, mToken);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}

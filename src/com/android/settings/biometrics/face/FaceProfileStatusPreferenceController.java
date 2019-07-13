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

import android.content.Context;
import android.os.UserHandle;

import com.android.settings.R;

import androidx.preference.Preference;

public class FaceProfileStatusPreferenceController extends FaceStatusPreferenceController {

    private static final String KEY_FACE_SETTINGS = "face_settings_profile";

    public FaceProfileStatusPreferenceController(Context context) {
        super(context, KEY_FACE_SETTINGS);
    }

    @Override
    public int getAvailabilityStatus() {
        // Check if Face for Profile is available.
        final int isAvailable = super.getAvailabilityStatus();
        if (isAvailable != AVAILABLE) {
            return isAvailable;
        }
        // Make the profile unsearchable so the user preference controller gets highlighted
        // when searched for.
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    protected boolean isUserSupported() {
        return mProfileChallengeUserId != UserHandle.USER_NULL
                && mLockPatternUtils.isSeparateProfileChallengeAllowed(mProfileChallengeUserId);
    }

    @Override
    protected int getUserId() {
        return mProfileChallengeUserId;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setTitle(mContext.getResources().getString(
                R.string.security_settings_face_profile_preference_title));
    }
}

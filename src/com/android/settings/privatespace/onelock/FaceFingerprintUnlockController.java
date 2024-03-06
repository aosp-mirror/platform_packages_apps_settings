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

import android.content.Context;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.AbstractPreferenceController;

/** Represents the preference controller to enroll biometrics for private space lock. */
public class FaceFingerprintUnlockController extends AbstractPreferenceController {
    private static final String KEY_SET_UNSET_FACE_FINGERPRINT = "private_space_biometrics";

    public FaceFingerprintUnlockController(Context context, SettingsPreferenceFragment host) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return android.os.Flags.allowPrivateProfile();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SET_UNSET_FACE_FINGERPRINT;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return TextUtils.equals(preference.getKey(), getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        //TODO(b/308862923) : Add condition to check and enable when separate private lock is set.
        preference.setSummary(mContext.getString(R.string.lock_settings_profile_unified_summary));
        preference.setEnabled(false);
    }
}

/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.security;

import android.content.Context;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class EncryptionStatusPreferenceController extends BasePreferenceController {


    static final String PREF_KEY_ENCRYPTION_DETAIL_PAGE =
            "encryption_and_credentials_encryption_status";
    static final String PREF_KEY_ENCRYPTION_SECURITY_PAGE = "encryption_and_credential";

    private final UserManager mUserManager;

    public EncryptionStatusPreferenceController(Context context, String key) {
        super(context, key);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public int getAvailabilityStatus() {
        if (TextUtils.equals(getPreferenceKey(), PREF_KEY_ENCRYPTION_DETAIL_PAGE) &&
                !mContext.getResources().getBoolean(
                R.bool.config_show_encryption_and_credentials_encryption_status)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return mUserManager.isAdminUser() ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean encryptionEnabled = LockPatternUtils.isDeviceEncryptionEnabled();
        if (encryptionEnabled) {
            if (TextUtils.equals(getPreferenceKey(), PREF_KEY_ENCRYPTION_DETAIL_PAGE)) {
                preference.setFragment(null);
            }
            preference.setSummary(R.string.crypt_keeper_encrypted_summary);
        } else {
            if (TextUtils.equals(getPreferenceKey(), PREF_KEY_ENCRYPTION_DETAIL_PAGE)) {
                preference.setFragment(CryptKeeperSettings.class.getName());
            }
            preference.setSummary(R.string.decryption_settings_summary);
        }

    }
}

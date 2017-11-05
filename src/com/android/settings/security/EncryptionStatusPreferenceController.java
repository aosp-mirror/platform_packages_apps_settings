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
import android.support.v7.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class EncryptionStatusPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String PREF_KEY = "encryption_and_credentials_encryption_status";

    private final UserManager mUserManager;

    public EncryptionStatusPreferenceController(Context context) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean isAvailable() {
        return mUserManager.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean encryptionEnabled = LockPatternUtils.isDeviceEncryptionEnabled();
        if (encryptionEnabled) {
            preference.setFragment(null);
            preference.setSummary(R.string.crypt_keeper_encrypted_summary);
        } else {
            preference.setFragment(CryptKeeperSettings.class.getName());
            preference.setSummary(R.string.summary_placeholder);
        }
    }
}

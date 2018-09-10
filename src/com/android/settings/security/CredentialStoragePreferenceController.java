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
import android.security.KeyStore;

import androidx.preference.Preference;

import com.android.settings.R;

public class CredentialStoragePreferenceController extends
        RestrictedEncryptionPreferenceController {

    private static final String KEY_CREDENTIAL_STORAGE_TYPE = "credential_storage_type";
    private final KeyStore mKeyStore;

    public CredentialStoragePreferenceController(Context context) {
        super(context, UserManager.DISALLOW_CONFIG_CREDENTIALS);
        mKeyStore = KeyStore.getInstance();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_CREDENTIAL_STORAGE_TYPE;
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(mKeyStore.isHardwareBacked()
                ? R.string.credential_storage_type_hardware
                : R.string.credential_storage_type_software);
    }
}

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

import androidx.preference.PreferenceScreen;

import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class ResetCredentialsPreferenceController extends RestrictedEncryptionPreferenceController
        implements LifecycleObserver, OnResume {

    private static final String KEY_RESET_CREDENTIALS = "credentials_reset";

    private final KeyStore mKeyStore;

    private RestrictedPreference mPreference;

    public ResetCredentialsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, UserManager.DISALLOW_CONFIG_CREDENTIALS);
        mKeyStore = KeyStore.getInstance();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_RESET_CREDENTIALS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onResume() {
        if (mPreference != null && !mPreference.isDisabledByAdmin()) {
            mPreference.setEnabled(!mKeyStore.isEmpty());
        }
    }
}

/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.password;

import android.content.Context;
import android.content.Intent;

import com.android.internal.widget.LockscreenCredential;

/**
 * Helper for handling managed passwords in security settings UI.
 * It provides resources that should be shown in settings UI when lock password quality is set to
 * {@link android.app.admin.DevicePolicyManager#PASSWORD_QUALITY_MANAGED} and hooks for implementing
 * an option for setting the password quality to
 * {@link android.app.admin.DevicePolicyManager#PASSWORD_QUALITY_MANAGED}.
 */
public class ManagedLockPasswordProvider {
    /** Factory method to make it easier to inject extended ManagedLockPasswordProviders. */
    public static ManagedLockPasswordProvider get(Context context, int userId) {
        return new ManagedLockPasswordProvider();
    }

    protected ManagedLockPasswordProvider() {}

    /**
     * Whether choosing/setting a managed lock password is supported for the user.
     * Please update {@link #getPickerOptionTitle(boolean)} if overridden to return true.
     */
    boolean isSettingManagedPasswordSupported() { return false; }

    /**
     * Whether the user should be able to choose managed lock password.
     */
    boolean isManagedPasswordChoosable() { return false; }

    /**
     * Returns title for managed password preference in security (lock) setting picker.
     * Should be overridden if {@link #isManagedPasswordSupported()} returns true.
     * @param forFingerprint Whether fingerprint unlock is enabled.
     */
    CharSequence getPickerOptionTitle(boolean forFingerprint) { return ""; }

    /**
     * Creates intent that should be launched when user chooses managed password in the lock
     * settings picker.
     * @param requirePasswordToDecrypt Whether a password is needed to decrypt the user.
     * @param password Current lock password.
     * @return Intent that should update lock password to a managed password.
     */
    Intent createIntent(boolean requirePasswordToDecrypt, LockscreenCredential password) {
        return null;
    }
}

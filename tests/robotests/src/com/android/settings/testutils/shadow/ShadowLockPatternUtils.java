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

package com.android.settings.testutils.shadow;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.List;

@Implements(LockPatternUtils.class)
public class ShadowLockPatternUtils {

    private static boolean sDeviceEncryptionEnabled;

    @Implementation
    protected boolean hasSecureLockScreen() {
        return true;
    }

    @Implementation
    protected boolean isSecure(int id) {
        return true;
    }

    @Implementation
    protected int getActivePasswordQuality(int userId) {
        return DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    }

    @Implementation
    protected int getKeyguardStoredPasswordQuality(int userHandle) {
        return 1;
    }

    @Implementation
    protected static boolean isDeviceEncryptionEnabled() {
        return sDeviceEncryptionEnabled;
    }

    @Implementation
    protected List<ComponentName> getEnabledTrustAgents(int userId) {
        return null;
    }

    public static void setDeviceEncryptionEnabled(boolean deviceEncryptionEnabled) {
        sDeviceEncryptionEnabled = deviceEncryptionEnabled;
    }

    @Implementation
    protected byte[] getPasswordHistoryHashFactor(LockscreenCredential currentPassword,
            int userId) {
        return null;
    }

    @Implementation
    protected boolean checkPasswordHistory(byte[] passwordToCheck, byte[] hashFactor, int userId) {
        return false;
    }
}

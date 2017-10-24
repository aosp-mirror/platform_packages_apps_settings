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

import com.android.internal.widget.LockPatternUtils;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(LockPatternUtils.class)
public class ShadowLockPatternUtils {

    private int mPasswordQuality = 1;
    @Implementation
    public boolean isSecure(int id) {
        return true;
    }

    @Implementation
    public int getActivePasswordQuality(int userId) {
        return DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    }

    @Implementation
    public int getKeyguardStoredPasswordQuality(int userHandle) {
        return mPasswordQuality;
    }

    // Non-Android accessor.
    public int getPasswordQuality() {
        return mPasswordQuality;
    }

    // Non-Android accessor.
    public void setPasswordQuality(int passwordQuality) {
        mPasswordQuality = passwordQuality;
    }
}

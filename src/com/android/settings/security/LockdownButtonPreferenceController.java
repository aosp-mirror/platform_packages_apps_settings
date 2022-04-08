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
 * limitations under the License.
 */

package com.android.settings.security;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.TogglePreferenceController;

public class LockdownButtonPreferenceController extends TogglePreferenceController {

    private final LockPatternUtils mLockPatternUtils;

    public LockdownButtonPreferenceController(Context context, String key) {
        super(context, key);
        mLockPatternUtils = new LockPatternUtils(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mLockPatternUtils.isSecure(UserHandle.myUserId())) {
            return AVAILABLE;
        } else {
            return DISABLED_FOR_USER;
        }
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCKDOWN_IN_POWER_MENU, 0) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCKDOWN_IN_POWER_MENU, isChecked ? 1 : 0);
        return true;
    }
}

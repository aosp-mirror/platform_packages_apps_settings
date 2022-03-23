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
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;

public class ShowPasswordPreferenceController extends TogglePreferenceController {

    private static final String KEY_SHOW_PASSWORD = "show_password";
    private static final int MY_USER_ID = UserHandle.myUserId();
    private final LockPatternUtils mLockPatternUtils;

    public ShowPasswordPreferenceController(Context context) {
        super(context, KEY_SHOW_PASSWORD);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.TEXT_SHOW_PASSWORD, 1) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.TEXT_SHOW_PASSWORD,
                isChecked ? 1 : 0);
        mLockPatternUtils.setVisiblePasswordEnabled(isChecked, MY_USER_ID);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_show_password)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

}


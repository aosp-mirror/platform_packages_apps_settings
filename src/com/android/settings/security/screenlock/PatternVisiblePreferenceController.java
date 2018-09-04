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

package com.android.settings.security.screenlock;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class PatternVisiblePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String PREF_KEY = "visiblepattern";

    private final int mUserId;
    private final LockPatternUtils mLockPatternUtils;

    public PatternVisiblePreferenceController(Context context, int userId,
            LockPatternUtils lockPatternUtils) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
    }

    @Override
    public boolean isAvailable() {
        return isPatternLock();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(
                mLockPatternUtils.isVisiblePatternEnabled(mUserId));
    }

    private boolean isPatternLock() {
        return mLockPatternUtils.isSecure(mUserId)
                && mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)
                == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mLockPatternUtils.setVisiblePatternEnabled((Boolean) newValue, mUserId);
        return true;
    }
}

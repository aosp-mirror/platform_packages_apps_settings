/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * Preference controller for the pin_auto_confirm setting.
 */
public class AutoPinConfirmPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String PREF_KEY_PIN_AUTO_CONFIRM = "auto_pin_confirm";
    private static final long MIN_AUTO_PIN_REQUIREMENT_LENGTH = 6L;

    private final int mUserId;
    private final LockPatternUtils mLockPatternUtils;

    public AutoPinConfirmPreferenceController(Context context, int userId,
            LockPatternUtils lockPatternUtils) {
        super(context);
        mUserId = userId;
        mLockPatternUtils = lockPatternUtils;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        setPinAutoConfirmSettingState((boolean) newValue);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(getPinAutoConfirmSettingState());
    }

    @Override
    public boolean isAvailable() {
        return LockPatternUtils.isAutoPinConfirmFeatureAvailable() && isPinLock()
                && isPinLengthEligibleForAutoConfirmation();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY_PIN_AUTO_CONFIRM;
    }

    private boolean isPinLock() {
        return mLockPatternUtils.getCredentialTypeForUser(mUserId)
                == LockPatternUtils.CREDENTIAL_TYPE_PIN;
    }

    private boolean isPinLengthEligibleForAutoConfirmation() {
        return mLockPatternUtils.getPinLength(mUserId) >= MIN_AUTO_PIN_REQUIREMENT_LENGTH;
    }

    private boolean getPinAutoConfirmSettingState() {
        return mLockPatternUtils.isAutoPinConfirmEnabled(mUserId);
    }

    private void setPinAutoConfirmSettingState(boolean state) {
        mLockPatternUtils.setAutoPinConfirm(state, mUserId);
    }
}

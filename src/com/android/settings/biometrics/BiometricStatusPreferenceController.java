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
 * limitations under the License
 */

package com.android.settings.biometrics;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;

public abstract class BiometricStatusPreferenceController extends BasePreferenceController {

    protected final UserManager mUm;
    protected final LockPatternUtils mLockPatternUtils;

    private final int mUserId = UserHandle.myUserId();
    protected final int mProfileChallengeUserId;

    private final BiometricNavigationUtils mBiometricNavigationUtils;

    /**
     * @return true if the manager is not null and the hardware is detected.
     */
    protected abstract boolean isDeviceSupported();

    /**
     * @return the summary text.
     */
    protected abstract String getSummaryText();

    /**
     * @return the class name for the settings page.
     */
    protected abstract String getSettingsClassName();

    public BiometricStatusPreferenceController(Context context, String key) {
        super(context, key);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
        mBiometricNavigationUtils = new BiometricNavigationUtils(getUserId());
    }

    @Override
    public int getAvailabilityStatus() {
        if (!isDeviceSupported()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (isUserSupported()) {
            return AVAILABLE;
        } else {
            return DISABLED_FOR_USER;
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (!isAvailable()) {
            if (preference != null) {
                preference.setVisible(false);
            }
            return;
        } else {
            preference.setVisible(true);
        }
        preference.setSummary(getSummaryText());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        return mBiometricNavigationUtils.launchBiometricSettings(
                preference.getContext(), getSettingsClassName(), preference.getExtras());
    }

    protected int getUserId() {
        return mUserId;
    }

    protected boolean isUserSupported() {
        return true;
    }
}

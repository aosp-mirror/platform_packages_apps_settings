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

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_FROM_SETTINGS_SUMMARY;

import android.content.Context;
import android.content.Intent;
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

    /**
     * @return true if the manager is not null and the hardware is detected.
     */
    protected abstract boolean isDeviceSupported();

    /**
     * @return true if the user has enrolled biometrics of the subclassed type.
     */
    protected abstract boolean hasEnrolledBiometrics();

    /**
     * @return the summary text if biometrics are enrolled.
     */
    protected abstract String getSummaryTextEnrolled();

    /**
     * @return the summary text if no biometrics are enrolled.
     */
    protected abstract String getSummaryTextNoneEnrolled();

    /**
     * @return the class name for the settings page.
     */
    protected abstract String getSettingsClassName();

    /**
     * @return the class name for entry to enrollment.
     */
    protected abstract String getEnrollClassName();

    public BiometricStatusPreferenceController(Context context, String key) {
        super(context, key);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mLockPatternUtils = FeatureFactory.getFactory(context)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(context);
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, mUserId);
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
        preference.setSummary(hasEnrolledBiometrics() ? getSummaryTextEnrolled()
                : getSummaryTextNoneEnrolled());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        final Context context = preference.getContext();
        final UserManager userManager = UserManager.get(context);
        final int userId = getUserId();
        if (Utils.startQuietModeDialogIfNecessary(context, userManager, userId)) {
            return false;
        }

        final Intent intent = new Intent();
        final String clazz = hasEnrolledBiometrics() ? getSettingsClassName()
                : getEnrollClassName();
        intent.setClassName(SETTINGS_PACKAGE_NAME, clazz);
        intent.putExtra(Intent.EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_FROM_SETTINGS_SUMMARY, true);
        context.startActivity(intent);
        return true;
    }

    protected int getUserId() {
        return mUserId;
    }

    protected boolean isUserSupported() {
        return true;
    }
}

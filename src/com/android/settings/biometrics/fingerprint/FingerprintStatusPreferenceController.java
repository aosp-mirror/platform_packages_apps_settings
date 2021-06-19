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

package com.android.settings.biometrics.fingerprint;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.fingerprint.FingerprintManager;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricStatusPreferenceController;
import com.android.settings.biometrics.ParentalControlsUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

public class FingerprintStatusPreferenceController extends BiometricStatusPreferenceController {

    private static final String KEY_FINGERPRINT_SETTINGS = "fingerprint_settings";

    protected final FingerprintManager mFingerprintManager;
    @VisibleForTesting
    RestrictedPreference mPreference;

    public FingerprintStatusPreferenceController(Context context) {
        this(context, KEY_FINGERPRINT_SETTINGS);
    }

    public FingerprintStatusPreferenceController(Context context, String key) {
        super(context, key);
        mFingerprintManager = Utils.getFingerprintManagerOrNull(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_FINGERPRINT_SETTINGS);
    }

    @Override
    protected boolean isDeviceSupported() {
        return !Utils.isMultipleBiometricsSupported(mContext)
                && Utils.hasFingerprintHardware(mContext);
    }

    @Override
    protected boolean hasEnrolledBiometrics() {
        return mFingerprintManager.hasEnrolledFingerprints(getUserId());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final RestrictedLockUtils.EnforcedAdmin admin = ParentalControlsUtils
                .parentConsentRequired(mContext, BiometricAuthenticator.TYPE_FINGERPRINT);
        updateStateInternal(admin);
    }

    @VisibleForTesting
    void updateStateInternal(@Nullable RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        if (enforcedAdmin != null && mPreference != null) {
            mPreference.setDisabledByAdmin(enforcedAdmin);
        }
    }

    @Override
    protected String getSummaryTextEnrolled() {
        final int numEnrolled = mFingerprintManager.getEnrolledFingerprints(getUserId()).size();
        return mContext.getResources().getQuantityString(
                R.plurals.security_settings_fingerprint_preference_summary,
                numEnrolled, numEnrolled);
    }

    @Override
    protected String getSummaryTextNoneEnrolled() {
        return mContext.getString(R.string.security_settings_fingerprint_preference_summary_none);
    }

    @Override
    protected String getSettingsClassName() {
        return FingerprintSettings.class.getName();
    }

    @Override
    protected String getEnrollClassName() {
        return FingerprintEnrollIntroduction.class.getName();
    }

}

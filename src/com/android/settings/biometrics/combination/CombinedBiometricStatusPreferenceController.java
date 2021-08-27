/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.biometrics.combination;

import android.content.Context;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricStatusPreferenceController;
import com.android.settings.biometrics.ParentalControlsUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

/**
 * Preference controller for biometrics settings page controlling the ability to unlock the phone
 * with face and fingerprint.
 */
public class CombinedBiometricStatusPreferenceController extends
        BiometricStatusPreferenceController implements LifecycleObserver {
    private static final String KEY_BIOMETRIC_SETTINGS = "biometric_settings";

    @Nullable
    FingerprintManager mFingerprintManager;
    @Nullable
    FaceManager mFaceManager;
    @VisibleForTesting
    RestrictedPreference mPreference;

    public CombinedBiometricStatusPreferenceController(Context context) {
        this(context, KEY_BIOMETRIC_SETTINGS, null /* lifecycle */);
    }

    public CombinedBiometricStatusPreferenceController(Context context, String key) {
        this(context, key, null /* lifecycle */);
    }

    public CombinedBiometricStatusPreferenceController(Context context, Lifecycle lifecycle) {
        this(context, KEY_BIOMETRIC_SETTINGS, lifecycle);
    }

    public CombinedBiometricStatusPreferenceController(
            Context context, String key, Lifecycle lifecycle) {
        super(context, key);
        mFingerprintManager = Utils.getFingerprintManagerOrNull(context);
        mFaceManager = Utils.getFaceManagerOrNull(context);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        updateStateInternal();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(mPreferenceKey);
    }

    @Override
    protected boolean isDeviceSupported() {
        return Utils.hasFingerprintHardware(mContext) && Utils.hasFaceHardware(mContext);
    }

    @Override
    protected boolean hasEnrolledBiometrics() {
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateStateInternal();
    }

    private void updateStateInternal() {
        // This controller currently is shown if fingerprint&face exist on the device. If this
        // changes in the future, the modalities passed into the below will need to be updated.
        updateStateInternal(ParentalControlsUtils.parentConsentRequired(mContext,
                BiometricAuthenticator.TYPE_FACE | BiometricAuthenticator.TYPE_FINGERPRINT));
    }

    @VisibleForTesting
    void updateStateInternal(@Nullable RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        if (mPreference != null) {
            mPreference.setDisabledByAdmin(enforcedAdmin);
        }
    }

    @Override
    protected String getSummaryTextEnrolled() {
        // Note that this is currently never called (see the super class)
        return mContext.getString(
                R.string.security_settings_biometric_preference_summary_none_enrolled);
    }

    @Override
    protected String getSummaryTextNoneEnrolled() {
        final int numFingerprintsEnrolled = mFingerprintManager != null ?
                mFingerprintManager.getEnrolledFingerprints(getUserId()).size() : 0;
        final boolean faceEnrolled = mFaceManager != null
                && mFaceManager.hasEnrolledTemplates(getUserId());

        if (faceEnrolled && numFingerprintsEnrolled > 1) {
            return mContext.getString(
                    R.string.security_settings_biometric_preference_summary_both_fp_multiple);
        } else if (faceEnrolled && numFingerprintsEnrolled == 1) {
            return mContext.getString(
                    R.string.security_settings_biometric_preference_summary_both_fp_single);
        } else if (faceEnrolled) {
            return mContext.getString(R.string.security_settings_face_preference_summary);
        } else if (numFingerprintsEnrolled > 0) {
            return mContext.getResources().getQuantityString(
                    R.plurals.security_settings_fingerprint_preference_summary,
                    numFingerprintsEnrolled, numFingerprintsEnrolled);
        } else {
            return mContext.getString(
                    R.string.security_settings_biometric_preference_summary_none_enrolled);
        }
    }

    @Override
    protected String getSettingsClassName() {
        return Settings.CombinedBiometricSettingsActivity.class.getName();
    }

    @Override
    protected String getEnrollClassName() {
        return Settings.CombinedBiometricSettingsActivity.class.getName();
    }
}

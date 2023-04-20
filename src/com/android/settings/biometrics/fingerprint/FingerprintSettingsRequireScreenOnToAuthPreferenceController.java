/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;

/**
 * Preference controller that controls whether a SFPS device is required to be interactive for
 * fingerprint authentication to unlock the device.
 */
public class FingerprintSettingsRequireScreenOnToAuthPreferenceController
        extends FingerprintSettingsPreferenceController {
    private static final String TAG =
            "FingerprintSettingsRequireScreenOnToAuthPreferenceController";

    @VisibleForTesting
    protected FingerprintManager mFingerprintManager;

    public FingerprintSettingsRequireScreenOnToAuthPreferenceController(
            Context context, String prefKey) {
        super(context, prefKey);
        mFingerprintManager = Utils.getFingerprintManagerOrNull(context);
    }

    @Override
    public boolean isChecked() {
        if (!FingerprintSettings.isFingerprintHardwareDetected(mContext)) {
            return false;
        } else if (getRestrictingAdmin() != null) {
            return false;
        }
        int defaultValue = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_requireScreenOnToAuthEnabled) ? 1 : 0;

        return Settings.Secure.getIntForUser(
                mContext.getContentResolver(),
                Settings.Secure.SFPS_REQUIRE_SCREEN_ON_TO_AUTH_ENABLED,
                defaultValue,
                getUserHandle()) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putIntForUser(
                mContext.getContentResolver(),
                Settings.Secure.SFPS_REQUIRE_SCREEN_ON_TO_AUTH_ENABLED,
                isChecked ? 1 : 0,
                getUserHandle());
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!FingerprintSettings.isFingerprintHardwareDetected(mContext)) {
            preference.setEnabled(false);
        } else if (!mFingerprintManager.hasEnrolledTemplates(getUserId())) {
            preference.setEnabled(false);
        } else {
            preference.setEnabled(true);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (mFingerprintManager != null
                && mFingerprintManager.isHardwareDetected()
                && mFingerprintManager.isPowerbuttonFps()) {
            return mFingerprintManager.hasEnrolledTemplates(getUserId())
                    ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    private int getUserHandle() {
        return UserHandle.of(getUserId()).getIdentifier();
    }

}

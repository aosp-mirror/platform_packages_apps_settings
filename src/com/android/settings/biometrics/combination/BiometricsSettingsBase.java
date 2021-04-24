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

import static android.app.Activity.RESULT_OK;

import static com.android.settings.password.ChooseLockPattern.RESULT_FINISHED;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;

/**
 * Base fragment with the confirming credential functionality for combined biometrics settings.
 */
public abstract class BiometricsSettingsBase extends DashboardFragment {

    private static final int CONFIRM_REQUEST = 2001;
    private static final int CHOOSE_LOCK_REQUEST = 2002;

    private static final String SAVE_STATE_CONFIRM_CREDETIAL = "confirm_credential";

    protected int mUserId;
    protected long mFaceChallenge;
    protected long mFingerprintChallenge;
    protected int mFaceSensorId;
    protected int mFingerprintSensorId;
    protected long mGkPwHandle;
    private boolean mConfirmCredential;
    @Nullable private FaceManager mFaceManager;
    @Nullable private FingerprintManager mFingerprintManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mUserId = getActivity().getIntent().getIntExtra(Intent.EXTRA_USER_ID,
                UserHandle.myUserId());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFaceManager = Utils.getFaceManagerOrNull(getActivity());
        mFingerprintManager = Utils.getFingerprintManagerOrNull(getActivity());

        if (BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
            mGkPwHandle = BiometricUtils.getGatekeeperPasswordHandle(getIntent());
        }

        if (savedInstanceState != null) {
            mConfirmCredential = savedInstanceState.getBoolean(SAVE_STATE_CONFIRM_CREDETIAL);
            if (savedInstanceState.containsKey(
                    ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE)) {
                mGkPwHandle = savedInstanceState.getLong(
                        ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE);
            }
        }

        if (mGkPwHandle == 0L && !mConfirmCredential) {
            mConfirmCredential = true;
            launchChooseOrConfirmLock();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isFinishing()) {
            mFaceManager.revokeChallenge(mFaceSensorId, mUserId, mFaceChallenge);
            mFingerprintManager.revokeChallenge(mUserId, mFingerprintChallenge);
            BiometricUtils.removeGatekeeperPasswordHandle(getActivity(), mGkPwHandle);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();
        if (getFacePreferenceKey().equals(key)) {
            final byte[] token = BiometricUtils.requestGatekeeperHat(getActivity(), mGkPwHandle,
                    mUserId, mFaceChallenge);
            final Bundle extras = preference.getExtras();
            extras.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
            extras.putInt(BiometricEnrollBase.EXTRA_KEY_SENSOR_ID, mFaceSensorId);
            extras.putLong(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, mFaceChallenge);
        } else if (getFingerprintPreferenceKey().equals(key)) {
            final byte[] token = BiometricUtils.requestGatekeeperHat(getActivity(), mGkPwHandle,
                    mUserId, mFingerprintChallenge);
            final Bundle extras = preference.getExtras();
            extras.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
            extras.putLong(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, mFingerprintChallenge);
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVE_STATE_CONFIRM_CREDETIAL, mConfirmCredential);
        if (mGkPwHandle != 0L) {
            outState.putLong(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, mGkPwHandle);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_REQUEST || requestCode == CHOOSE_LOCK_REQUEST) {
            mConfirmCredential = false;
            if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                if (data != null && BiometricUtils.containsGatekeeperPasswordHandle(data)) {
                    mGkPwHandle = BiometricUtils.getGatekeeperPasswordHandle(data);
                    mFaceManager.generateChallenge((sensorId, challenge) -> {
                        mFaceSensorId = sensorId;
                        mFaceChallenge = challenge;
                    });
                    mFingerprintManager.generateChallenge(mUserId, (sensorId, challenge) -> {
                        mFingerprintSensorId = sensorId;
                        mFingerprintChallenge = challenge;
                    });
                } else {
                    Log.d(getLogTag(), "Data null or GK PW missing.");
                    finish();
                }
            } else {
                Log.d(getLogTag(), "Password not confirmed.");
                finish();
            }
        }
    }

    /**
     * Get the preference key of face for passing through credential data to face settings.
     */
    public abstract String getFacePreferenceKey();

    /**
     * Get the preference key of face for passing through credential data to face settings.
     */
    public abstract String getFingerprintPreferenceKey();

    private void launchChooseOrConfirmLock() {
        final ChooseLockSettingsHelper.Builder builder =
                new ChooseLockSettingsHelper.Builder(getActivity(), this)
                        .setRequestCode(CONFIRM_REQUEST)
                        .setTitle(getString(R.string.security_settings_biometric_preference_title))
                        .setRequestGatekeeperPasswordHandle(true)
                        .setForegroundOnly(true)
                        .setReturnCredentials(true);
        if (mUserId != UserHandle.USER_NULL) {
            builder.setUserId(mUserId);
        }
        final boolean launched = builder.show();

        if (!launched) {
            Intent intent = BiometricUtils.getChooseLockIntent(getActivity(), getIntent());
            intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
            intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS, true);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, true);

            if (mUserId != UserHandle.USER_NULL) {
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
            }
            startActivityForResult(intent, CHOOSE_LOCK_REQUEST);
        }
    }
}

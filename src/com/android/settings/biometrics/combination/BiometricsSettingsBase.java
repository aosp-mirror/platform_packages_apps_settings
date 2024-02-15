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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.biometrics.SensorProperties;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricStatusPreferenceController;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.transition.SettingsTransitionHelper;

import java.util.Collection;
import java.util.List;

/**
 * Base fragment with the confirming credential functionality for combined biometrics settings.
 */
public abstract class BiometricsSettingsBase extends DashboardFragment {

    @VisibleForTesting
    static final int CONFIRM_REQUEST = 2001;
    private static final int CHOOSE_LOCK_REQUEST = 2002;
    protected static final int ACTIVE_UNLOCK_REQUEST = 2003;

    private static final String SAVE_STATE_CONFIRM_CREDETIAL = "confirm_credential";
    private static final String DO_NOT_FINISH_ACTIVITY = "do_not_finish_activity";
    @VisibleForTesting
    static final String RETRY_PREFERENCE_KEY = "retry_preference_key";
    @VisibleForTesting
    static final String RETRY_PREFERENCE_BUNDLE = "retry_preference_bundle";

    protected int mUserId;
    protected long mGkPwHandle;
    private boolean mConfirmCredential;
    @Nullable private FaceManager mFaceManager;
    @Nullable private FingerprintManager mFingerprintManager;
    // Do not finish() if choosing/confirming credential, showing fp/face settings, or launching
    // active unlock
    protected boolean mDoNotFinishActivity;
    @Nullable private String mRetryPreferenceKey = null;
    @Nullable private Bundle mRetryPreferenceExtra = null;

    private final ActivityResultLauncher<Intent> mFaceOrFingerprintPreferenceLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::onFaceOrFingerprintPreferenceResult);

    private void onFaceOrFingerprintPreferenceResult(@Nullable ActivityResult result) {
        if (result != null && result.getResultCode() == BiometricEnrollBase.RESULT_TIMEOUT) {
            // When "Face Unlock" or "Fingerprint Unlock" is closed due to entering onStop(),
            // "Face & Fingerprint Unlock" shall also close itself and back to "Security" page.
            finish();
        }
    }

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
            mDoNotFinishActivity = savedInstanceState.getBoolean(DO_NOT_FINISH_ACTIVITY);
            mRetryPreferenceKey = savedInstanceState.getString(RETRY_PREFERENCE_KEY);
            mRetryPreferenceExtra = savedInstanceState.getBundle(RETRY_PREFERENCE_BUNDLE);
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

        updateUnlockPhonePreferenceSummary();

        final Preference useInAppsPreference = findPreference(getUseInAppsPreferenceKey());
        if (useInAppsPreference != null) {
            useInAppsPreference.setSummary(getUseClass2BiometricSummary());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mConfirmCredential) {
            mDoNotFinishActivity = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!getActivity().isChangingConfigurations() && !mDoNotFinishActivity) {
            BiometricUtils.removeGatekeeperPasswordHandle(getActivity(), mGkPwHandle);
            getActivity().finish();
        }
    }

    protected boolean onRetryPreferenceTreeClick(Preference preference, final boolean retry) {
        final String key = preference.getKey();
        final Context context = requireActivity().getApplicationContext();

        // Generate challenge (and request LSS to create a HAT) each time the preference is clicked,
        // since FingerprintSettings and FaceSettings revoke the challenge when finishing.
        if (getFacePreferenceKey().equals(key)) {
            mDoNotFinishActivity = true;
            mFaceManager.generateChallenge(mUserId, (sensorId, userId, challenge) -> {
                final Activity activity = getActivity();
                if (activity == null || activity.isFinishing()) {
                    Log.e(getLogTag(), "Stop during generating face unlock challenge"
                            + " because activity is null or finishing");
                    return;
                }
                try {
                    final byte[] token = requestGatekeeperHat(context, mGkPwHandle, mUserId,
                            challenge);
                    final Bundle extras = preference.getExtras();
                    extras.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
                    extras.putInt(BiometricEnrollBase.EXTRA_KEY_SENSOR_ID, sensorId);
                    extras.putLong(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, challenge);
                    onFaceOrFingerprintPreferenceTreeClick(preference);
                } catch (IllegalStateException e) {
                    if (retry) {
                        mRetryPreferenceKey = preference.getKey();
                        mRetryPreferenceExtra = preference.getExtras();
                        mConfirmCredential = true;
                        launchChooseOrConfirmLock();
                    } else {
                        Log.e(getLogTag(), "face generateChallenge fail", e);
                        mDoNotFinishActivity = false;
                    }
                }
            });
            return true;
        } else if (getFingerprintPreferenceKey().equals(key)) {
            mDoNotFinishActivity = true;
            mFingerprintManager.generateChallenge(mUserId, (sensorId, userId, challenge) -> {
                final Activity activity = getActivity();
                if (activity == null || activity.isFinishing()) {
                    Log.e(getLogTag(), "Stop during generating fingerprint challenge"
                            + " because activity is null or finishing");
                    return;
                }
                try {
                    final byte[] token = requestGatekeeperHat(context, mGkPwHandle, mUserId,
                            challenge);
                    final Bundle extras = preference.getExtras();
                    extras.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
                    extras.putLong(BiometricEnrollBase.EXTRA_KEY_CHALLENGE, challenge);
                    onFaceOrFingerprintPreferenceTreeClick(preference);
                } catch (IllegalStateException e) {
                    if (retry) {
                        mRetryPreferenceKey = preference.getKey();
                        mRetryPreferenceExtra = preference.getExtras();
                        mConfirmCredential = true;
                        launchChooseOrConfirmLock();
                    } else {
                        Log.e(getLogTag(), "fingerprint generateChallenge fail", e);
                        mDoNotFinishActivity = false;
                    }
                }
            });
            return true;
        }
        return false;
    }

    @VisibleForTesting
    protected byte[] requestGatekeeperHat(@NonNull Context context, long gkPwHandle, int userId,
            long challenge) {
        return BiometricUtils.requestGatekeeperHat(context, gkPwHandle, userId, challenge);
    }

    /**
     * Handle preference tree click action for "Face Unlock" or "Fingerprint Unlock" with a launcher
     * because "Face & Fingerprint Unlock" has to close itself when it gets a specific activity
     * error code.
     *
     * @param preference "Face Unlock" or "Fingerprint Unlock" preference.
     */
    private void onFaceOrFingerprintPreferenceTreeClick(@NonNull Preference preference) {
        Collection<List<AbstractPreferenceController>> controllers = getPreferenceControllers();
        for (List<AbstractPreferenceController> controllerList : controllers) {
            for (AbstractPreferenceController controller : controllerList) {
                if (controller instanceof BiometricStatusPreferenceController) {
                    final BiometricStatusPreferenceController biometricController =
                            (BiometricStatusPreferenceController) controller;
                    if (biometricController.setPreferenceTreeClickLauncher(preference,
                            mFaceOrFingerprintPreferenceLauncher)) {
                        if (biometricController.handlePreferenceTreeClick(preference)) {
                            writePreferenceClickMetric(preference);
                        }
                        biometricController.setPreferenceTreeClickLauncher(preference, null);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return onRetryPreferenceTreeClick(preference, true)
                || super.onPreferenceTreeClick(preference);
    }

    private void retryPreferenceKey(@NonNull String key, @Nullable Bundle extras) {
        final Preference preference = findPreference(key);
        if (preference == null) {
            Log.w(getLogTag(), ".retryPreferenceKey, fail to find " + key);
            return;
        }

        if (extras != null) {
            preference.getExtras().putAll(extras);
        }
        onRetryPreferenceTreeClick(preference, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVE_STATE_CONFIRM_CREDETIAL, mConfirmCredential);
        outState.putBoolean(DO_NOT_FINISH_ACTIVITY, mDoNotFinishActivity);
        if (mGkPwHandle != 0L) {
            outState.putLong(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, mGkPwHandle);
        }
        if (!TextUtils.isEmpty(mRetryPreferenceKey)) {
            outState.putString(RETRY_PREFERENCE_KEY, mRetryPreferenceKey);
            outState.putBundle(RETRY_PREFERENCE_BUNDLE, mRetryPreferenceExtra);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONFIRM_REQUEST || requestCode == CHOOSE_LOCK_REQUEST) {
            mConfirmCredential = false;
            mDoNotFinishActivity = false;
            if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                if (BiometricUtils.containsGatekeeperPasswordHandle(data)) {
                    mGkPwHandle = BiometricUtils.getGatekeeperPasswordHandle(data);
                    if (!TextUtils.isEmpty(mRetryPreferenceKey)) {
                        getActivity().overridePendingTransition(
                                com.google.android.setupdesign.R.anim.sud_slide_next_in,
                                com.google.android.setupdesign.R.anim.sud_slide_next_out);
                        retryPreferenceKey(mRetryPreferenceKey, mRetryPreferenceExtra);
                    }
                } else {
                    Log.d(getLogTag(), "Data null or GK PW missing.");
                    finish();
                }
            } else {
                Log.d(getLogTag(), "Password not confirmed.");
                finish();
            }
            mRetryPreferenceKey = null;
            mRetryPreferenceExtra = null;
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

    /**
     * @return The preference key of the "Unlock your phone" setting toggle.
     */
    public abstract String getUnlockPhonePreferenceKey();

    /**
     * @return The preference key of the "Verify it's you in apps" setting toggle.
     */
    public abstract String getUseInAppsPreferenceKey();

    @VisibleForTesting
    protected void launchChooseOrConfirmLock() {
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
        mDoNotFinishActivity = true;
        final boolean launched = builder.show();

        if (!launched) {
            Intent intent = BiometricUtils.getChooseLockIntent(getActivity(), getIntent());
            intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS,
                    true);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, true);
            intent.putExtra(SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
                    SettingsTransitionHelper.TransitionType.TRANSITION_SLIDE);

            if (mUserId != UserHandle.USER_NULL) {
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
            }
            startActivityForResult(intent, CHOOSE_LOCK_REQUEST);
        }
    }

    protected void updateUnlockPhonePreferenceSummary() {
        final Preference unlockPhonePreference = findPreference(getUnlockPhonePreferenceKey());
        if (unlockPhonePreference != null) {
            unlockPhonePreference.setSummary(getUseAnyBiometricSummary());
        }
    }

    @NonNull
    protected String getUseAnyBiometricSummary() {
        boolean isFaceAllowed = mFaceManager != null && mFaceManager.isHardwareDetected();
        boolean isFingerprintAllowed =
                mFingerprintManager != null && mFingerprintManager.isHardwareDetected();

        @StringRes final int resId = getUseBiometricSummaryRes(isFaceAllowed, isFingerprintAllowed);
        return resId == 0 ? "" : getString(resId);
    }

    protected int getUserId() {
        return mUserId;
    }

    protected long getGkPwHandle() {
        return mGkPwHandle;
    }

    @NonNull
    private String getUseClass2BiometricSummary() {
        boolean isFaceAllowed = false;
        if (mFaceManager != null) {
            for (final FaceSensorPropertiesInternal sensorProps
                    : mFaceManager.getSensorPropertiesInternal()) {
                if (sensorProps.sensorStrength == SensorProperties.STRENGTH_WEAK
                        || sensorProps.sensorStrength == SensorProperties.STRENGTH_STRONG) {
                    isFaceAllowed = true;
                    break;
                }
            }
        }

        boolean isFingerprintAllowed = false;
        if (mFingerprintManager != null) {
            for (final FingerprintSensorPropertiesInternal sensorProps
                    : mFingerprintManager.getSensorPropertiesInternal()) {
                if (sensorProps.sensorStrength == SensorProperties.STRENGTH_WEAK
                        || sensorProps.sensorStrength == SensorProperties.STRENGTH_STRONG) {
                    isFingerprintAllowed = true;
                    break;
                }
            }
        }

        @StringRes final int resId = getUseBiometricSummaryRes(isFaceAllowed, isFingerprintAllowed);
        return resId == 0 ? "" : getString(resId);
    }

    @StringRes
    private static int getUseBiometricSummaryRes(boolean isFaceAllowed,
            boolean isFingerprintAllowed) {

        if (isFaceAllowed && isFingerprintAllowed) {
            return R.string.biometric_settings_use_face_or_fingerprint_preference_summary;
        } else if (isFaceAllowed) {
            return R.string.biometric_settings_use_face_preference_summary;
        } else if (isFingerprintAllowed) {
            return R.string.biometric_settings_use_fingerprint_preference_summary;
        } else {
            return 0;
        }
    }
}

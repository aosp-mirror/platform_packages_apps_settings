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

import static android.provider.Settings.ACTION_BIOMETRIC_ENROLL;
import static android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED;

import android.annotation.NonNull;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricManager.Authenticators;
import android.hardware.biometrics.BiometricManager.BiometricError;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.core.InstrumentedActivity;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.password.ChooseLockSettingsHelper;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.List;

/**
 * Trampoline activity launched by the {@code android.settings.BIOMETRIC_ENROLL} action which
 * shows the user an appropriate enrollment flow depending on the device's biometric hardware.
 * This activity must only allow enrollment of biometrics that can be used by
 * {@link android.hardware.biometrics.BiometricPrompt}.
 */
public class BiometricEnrollActivity extends InstrumentedActivity {

    private static final String TAG = "BiometricEnrollActivity";

    private static final int REQUEST_CHOOSE_LOCK = 1;
    private static final int REQUEST_CONFIRM_LOCK = 2;

    public static final int RESULT_SKIP = BiometricEnrollBase.RESULT_SKIP;

    // Intent extra. If true, biometric enrollment should skip introductory screens. Currently
    // this only applies to fingerprint.
    public static final String EXTRA_SKIP_INTRO = "skip_intro";

    private static final String SAVED_STATE_CONFIRMING_CREDENTIALS = "confirming_credentials";
    private static final String SAVED_STATE_ENROLL_ACTION_LOGGED = "enroll_action_logged";
    private static final String SAVED_STATE_GK_PW_HANDLE = "gk_pw_handle";

    public static final class InternalActivity extends BiometricEnrollActivity {}

    private int mUserId = UserHandle.myUserId();
    private boolean mConfirmingCredentials;
    private boolean mIsEnrollActionLogged;
    private boolean mIsFaceEnrollable;
    private boolean mIsFingerprintEnrollable;
    @Nullable private Long mGkPwHandle;
    @Nullable private MultiBiometricEnrollHelper mMultiBiometricEnrollHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (this instanceof InternalActivity) {
            mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
            if (BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
                mGkPwHandle = BiometricUtils.getGatekeeperPasswordHandle(getIntent());
            }
        }

        if (savedInstanceState != null) {
            mConfirmingCredentials = savedInstanceState.getBoolean(
                    SAVED_STATE_CONFIRMING_CREDENTIALS, false);
            mIsEnrollActionLogged = savedInstanceState.getBoolean(
                    SAVED_STATE_ENROLL_ACTION_LOGGED, false);
            if (savedInstanceState.containsKey(SAVED_STATE_GK_PW_HANDLE)) {
                mGkPwHandle = savedInstanceState.getLong(SAVED_STATE_GK_PW_HANDLE);
            }
        }

        // Log a framework stats event if this activity was launched via intent action.
        final Intent intent = getIntent();
        if (!mIsEnrollActionLogged && ACTION_BIOMETRIC_ENROLL.equals(intent.getAction())) {
            mIsEnrollActionLogged = true;

            // Get the current status for each authenticator type.
            @BiometricError final int strongBiometricStatus;
            @BiometricError final int weakBiometricStatus;
            @BiometricError final int deviceCredentialStatus;
            final BiometricManager bm = getSystemService(BiometricManager.class);
            if (bm != null) {
                strongBiometricStatus = bm.canAuthenticate(Authenticators.BIOMETRIC_STRONG);
                weakBiometricStatus = bm.canAuthenticate(Authenticators.BIOMETRIC_WEAK);
                deviceCredentialStatus = bm.canAuthenticate(Authenticators.DEVICE_CREDENTIAL);
            } else {
                strongBiometricStatus = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE;
                weakBiometricStatus = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE;
                deviceCredentialStatus = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE;
            }

            FrameworkStatsLog.write(FrameworkStatsLog.AUTH_ENROLL_ACTION_INVOKED,
                    strongBiometricStatus == BiometricManager.BIOMETRIC_SUCCESS,
                    weakBiometricStatus == BiometricManager.BIOMETRIC_SUCCESS,
                    deviceCredentialStatus == BiometricManager.BIOMETRIC_SUCCESS,
                    intent.hasExtra(EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED),
                    intent.getIntExtra(EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, 0));
        }

        // Put the theme in the intent so it gets propagated to other activities in the flow
        if (intent.getStringExtra(WizardManagerHelper.EXTRA_THEME) == null) {
            intent.putExtra(
                    WizardManagerHelper.EXTRA_THEME,
                    SetupWizardUtils.getThemeString(intent));
        }

        // Default behavior is to enroll BIOMETRIC_WEAK or above. See ACTION_BIOMETRIC_ENROLL.
        final int authenticators = intent.getIntExtra(
                EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, Authenticators.BIOMETRIC_WEAK);

        Log.d(TAG, "Authenticators: " + authenticators);

        final PackageManager pm = getApplicationContext().getPackageManager();
        final boolean hasFeatureFingerprint =
                pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        final boolean hasFeatureFace = pm.hasSystemFeature(PackageManager.FEATURE_FACE);
        final boolean isSetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());

        if (isSetupWizard) {
            if (hasFeatureFace && hasFeatureFingerprint) {
                setupForMultiBiometricEnroll();
            } else if (hasFeatureFace) {
                launchFaceOnlyEnroll();
            } else if (hasFeatureFingerprint) {
                launchFingerprintOnlyEnroll();
            } else {
                Log.e(TAG, "No biometrics but started by SUW?");
                finish();
            }
        } else {
            // If the caller is not setup wizard, and the user has something enrolled, finish.
            final BiometricManager bm = getSystemService(BiometricManager.class);
            final @BiometricError int result = bm.canAuthenticate(authenticators);
            if (result != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                Log.e(TAG, "Unexpected result: " + result);
                finish();
                return;
            }

            // This will need to be updated if the device has sensors other than BIOMETRIC_STRONG
            if (authenticators == BiometricManager.Authenticators.DEVICE_CREDENTIAL) {
                launchCredentialOnlyEnroll();
            } else if (hasFeatureFace && hasFeatureFingerprint) {
                setupForMultiBiometricEnroll();
            } else if (hasFeatureFingerprint) {
                launchFingerprintOnlyEnroll();
            } else if (hasFeatureFace) {
                launchFaceOnlyEnroll();
            } else {
                Log.e(TAG, "Unknown state, finishing");
                finish();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_STATE_CONFIRMING_CREDENTIALS, mConfirmingCredentials);
        outState.putBoolean(SAVED_STATE_ENROLL_ACTION_LOGGED, mIsEnrollActionLogged);
        if (mGkPwHandle != null) {
            outState.putLong(SAVED_STATE_GK_PW_HANDLE, mGkPwHandle);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mMultiBiometricEnrollHelper == null) {
            overridePendingTransition(R.anim.sud_slide_next_in, R.anim.sud_slide_next_out);

            switch (requestCode) {
                case REQUEST_CHOOSE_LOCK:
                    mConfirmingCredentials = false;
                    if (resultCode == ChooseLockPattern.RESULT_FINISHED) {
                        startMultiBiometricEnroll(data);
                    } else {
                        Log.d(TAG, "Unknown result for chooseLock: " + resultCode);
                        setResult(resultCode);
                        finish();
                    }
                    break;
                case REQUEST_CONFIRM_LOCK:
                    mConfirmingCredentials = false;
                    if (resultCode == RESULT_OK) {
                        startMultiBiometricEnroll(data);
                    } else {
                        Log.d(TAG, "Unknown result for confirmLock: " + resultCode);
                        finish();
                    }
                    break;
                default:
                    Log.d(TAG, "Unknown requestCode: " + requestCode + ", finishing");
                    finish();
            }
        } else {
            mMultiBiometricEnrollHelper.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        final int new_resid = SetupWizardUtils.getTheme(this, getIntent());
        theme.applyStyle(R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, new_resid, first);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mConfirmingCredentials || mMultiBiometricEnrollHelper != null) {
            return;
        }

        if (!isChangingConfigurations()) {
            Log.d(TAG, "Finishing in onStop");
            finish();
        }
    }

    private void setupForMultiBiometricEnroll() {
        if (!mConfirmingCredentials) {
            mConfirmingCredentials = true;
            if (!userHasPassword(mUserId)) {
                launchChooseLock();
            } else {
                launchConfirmLock();
            }
        }
    }

    private void startMultiBiometricEnroll(Intent data) {
        final boolean isSetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
        final FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);
        final FaceManager faceManager = getSystemService(FaceManager.class);
        final List<FingerprintSensorPropertiesInternal> fpProperties =
                fingerprintManager.getSensorPropertiesInternal();
        final List<FaceSensorPropertiesInternal> faceProperties =
                faceManager.getSensorPropertiesInternal();

        mGkPwHandle = BiometricUtils.getGatekeeperPasswordHandle(data);

        if (isSetupWizard) {
            // This would need to be updated for devices with multiple sensors of the same modality
            mIsFaceEnrollable = !faceProperties.isEmpty()
                    && faceManager.getEnrolledFaces(mUserId).size() == 0;
            mIsFingerprintEnrollable = !fpProperties.isEmpty()
                    && fingerprintManager.getEnrolledFingerprints(mUserId).size() == 0;
        } else {
            // This would need to be updated for devices with multiple sensors of the same modality
            mIsFaceEnrollable = !faceProperties.isEmpty()
                    && faceManager.getEnrolledFaces(mUserId).size()
                    < faceProperties.get(0).maxEnrollmentsPerUser;
            mIsFingerprintEnrollable = !fpProperties.isEmpty()
                    && fingerprintManager.getEnrolledFingerprints(mUserId).size()
                    < fpProperties.get(0).maxEnrollmentsPerUser;

        }

        mMultiBiometricEnrollHelper = new MultiBiometricEnrollHelper(this, mUserId,
                mIsFaceEnrollable, mIsFingerprintEnrollable, mGkPwHandle);
        mMultiBiometricEnrollHelper.startNextStep();
    }

    private boolean userHasPassword(int userId) {
        final UserManager userManager = getSystemService(UserManager.class);
        final int passwordQuality = new LockPatternUtils(this)
                .getActivePasswordQuality(userManager.getCredentialOwnerProfile(userId));
        return passwordQuality != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    }

    private void launchChooseLock() {
        Log.d(TAG, "launchChooseLock");
        Intent intent = BiometricUtils.getChooseLockIntent(this, getIntent());
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, true);

        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        startActivityForResult(intent, REQUEST_CHOOSE_LOCK);
    }

    private void launchConfirmLock() {
        Log.d(TAG, "launchConfirmLock");
        final ChooseLockSettingsHelper.Builder builder = new ChooseLockSettingsHelper.Builder(this);
        builder.setRequestCode(REQUEST_CONFIRM_LOCK)
                .setRequestGatekeeperPasswordHandle(true)
                .setForegroundOnly(true)
                .setReturnCredentials(true);
        if (mUserId != UserHandle.USER_NULL) {
            builder.setUserId(mUserId);
        }
        final boolean launched = builder.show();
        if (!launched) {
            // This shouldn't happen, as we should only end up at this step if a lock thingy is
            // already set.
            finish();
        }
    }

    /**
     * This should only be used to launch enrollment for single-sensor devices, which use
     * FLAG_ACTIVITY_FORWARD_RESULT path.
     *
     * @param intent Enrollment activity that should be started (e.g. FaceEnrollIntroduction.class,
     *               etc).
     */
    private void launchEnrollActivity(@NonNull Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        byte[] hardwareAuthToken = null;
        if (this instanceof InternalActivity) {
            hardwareAuthToken = getIntent().getByteArrayExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        }
        BiometricUtils.launchEnrollForResult(this, intent, 0 /* requestCode */, hardwareAuthToken,
                mGkPwHandle, mUserId);
    }

    private void launchCredentialOnlyEnroll() {
        final Intent intent;
        // If only device credential was specified, ask the user to only set that up.
        intent = new Intent(this, ChooseLockGeneric.class);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS, true);
        launchEnrollActivity(intent);
    }

    private void launchFingerprintOnlyEnroll() {
        final Intent intent;
        // ChooseLockGeneric can request to start fingerprint enroll bypassing the intro screen.
        if (getIntent().getBooleanExtra(EXTRA_SKIP_INTRO, false)
                && this instanceof InternalActivity) {
            intent = BiometricUtils.getFingerprintFindSensorIntent(this, getIntent());
        } else {
            intent = BiometricUtils.getFingerprintIntroIntent(this, getIntent());
        }
        launchEnrollActivity(intent);
    }

    private void launchFaceOnlyEnroll() {
        final Intent intent = BiometricUtils.getFaceIntroIntent(this, getIntent());
        launchEnrollActivity(intent);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BIOMETRIC_ENROLL_ACTIVITY;
    }
}

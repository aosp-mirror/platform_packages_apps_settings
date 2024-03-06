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

import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_CONSENT_DENIED;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_CONSENT_GRANTED;

import static com.google.android.setupdesign.transition.TransitionHelper.TRANSITION_FADE_THROUGH;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricManager.Authenticators;
import android.hardware.biometrics.BiometricManager.BiometricError;
import android.hardware.biometrics.SensorProperties;
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
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.password.ChooseLockSettingsHelper;

import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.transition.TransitionHelper;

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
    // prompt for parental consent options
    private static final int REQUEST_CHOOSE_OPTIONS = 3;
    // prompt hand phone back to parent after enrollment
    private static final int REQUEST_HANDOFF_PARENT = 4;
    private static final int REQUEST_SINGLE_ENROLL_FINGERPRINT = 5;
    private static final int REQUEST_SINGLE_ENROLL_FACE = 6;

    public static final int RESULT_SKIP = BiometricEnrollBase.RESULT_SKIP;

    // Intent extra. If true, biometric enrollment should skip introductory screens. Currently
    // this only applies to fingerprint.
    public static final String EXTRA_SKIP_INTRO = "skip_intro";

    // Intent extra. If true, parental consent will be requested before user enrollment.
    public static final String EXTRA_REQUIRE_PARENTAL_CONSENT = "require_consent";

    // Intent extra. If true, the screen asking the user to return the device to their parent will
    // be skipped after enrollment.
    public static final String EXTRA_SKIP_RETURN_TO_PARENT = "skip_return_to_parent";

    // If EXTRA_REQUIRE_PARENTAL_CONSENT was used to start the activity then the result
    // intent will include this extra containing a bundle of the form:
    // "modality" -> consented (boolean).
    public static final String EXTRA_PARENTAL_CONSENT_STATUS = "consent_status";

    private static final String SAVED_STATE_CONFIRMING_CREDENTIALS = "confirming_credentials";
    private static final String SAVED_STATE_IS_SINGLE_ENROLLING =
            "is_single_enrolling";
    private static final String SAVED_STATE_PASS_THROUGH_EXTRAS_FROM_CHOSEN_LOCK_IN_SUW =
            "pass_through_extras_from_chosen_lock_in_suw";
    private static final String SAVED_STATE_ENROLL_ACTION_LOGGED = "enroll_action_logged";
    private static final String SAVED_STATE_PARENTAL_OPTIONS = "enroll_preferences";
    private static final String SAVED_STATE_GK_PW_HANDLE = "gk_pw_handle";

    public static final class InternalActivity extends BiometricEnrollActivity {}

    private int mUserId = UserHandle.myUserId();
    private boolean mConfirmingCredentials;
    private boolean mIsSingleEnrolling;
    private Bundle mPassThroughExtrasFromChosenLockInSuw = null;
    private boolean mIsEnrollActionLogged;
    private boolean mHasFeatureFace = false;
    private boolean mHasFeatureFingerprint = false;
    private boolean mIsFaceEnrollable = false;
    private boolean mIsFingerprintEnrollable = false;
    private boolean mParentalOptionsRequired = false;
    private boolean mSkipReturnToParent = false;
    private Bundle mParentalOptions;
    @Nullable private Long mGkPwHandle;
    @Nullable private ParentalConsentHelper mParentalConsentHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        if (this instanceof InternalActivity) {
            mUserId = intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
            if (BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
                mGkPwHandle = BiometricUtils.getGatekeeperPasswordHandle(getIntent());
            }
        } else if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            if (BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
                mGkPwHandle = BiometricUtils.getGatekeeperPasswordHandle(getIntent());
            }

        }

        if (savedInstanceState != null) {
            mConfirmingCredentials = savedInstanceState.getBoolean(
                    SAVED_STATE_CONFIRMING_CREDENTIALS, false);
            mIsSingleEnrolling = savedInstanceState.getBoolean(
                    SAVED_STATE_IS_SINGLE_ENROLLING, false);
            mPassThroughExtrasFromChosenLockInSuw = savedInstanceState.getBundle(
                    SAVED_STATE_PASS_THROUGH_EXTRAS_FROM_CHOSEN_LOCK_IN_SUW);
            mIsEnrollActionLogged = savedInstanceState.getBoolean(
                    SAVED_STATE_ENROLL_ACTION_LOGGED, false);
            mParentalOptions = savedInstanceState.getBundle(SAVED_STATE_PARENTAL_OPTIONS);
            if (savedInstanceState.containsKey(SAVED_STATE_GK_PW_HANDLE)) {
                mGkPwHandle = savedInstanceState.getLong(SAVED_STATE_GK_PW_HANDLE);
            }
        }

        // Log a framework stats event if this activity was launched via intent action.
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

        final PackageManager pm = getApplicationContext().getPackageManager();
        mHasFeatureFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        mHasFeatureFace = pm.hasSystemFeature(PackageManager.FEATURE_FACE);

        // Default behavior is to enroll BIOMETRIC_WEAK or above. See ACTION_BIOMETRIC_ENROLL.
        final int authenticators = getIntent().getIntExtra(
                EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, Authenticators.BIOMETRIC_WEAK);
        Log.d(TAG, "Authenticators: " + BiometricManager.authenticatorToStr(authenticators));

        mParentalOptionsRequired = intent.getBooleanExtra(EXTRA_REQUIRE_PARENTAL_CONSENT, false);
        mSkipReturnToParent = intent.getBooleanExtra(EXTRA_SKIP_RETURN_TO_PARENT, false);

        // determine what can be enrolled
        final boolean isSetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
        final boolean isMultiSensor = mHasFeatureFace && mHasFeatureFingerprint;

        Log.d(TAG, "parentalOptionsRequired: " + mParentalOptionsRequired
                + ", skipReturnToParent: " + mSkipReturnToParent
                + ", isSetupWizard: " + isSetupWizard
                + ", isMultiSensor: " + isMultiSensor);

        if (mHasFeatureFace) {
            final FaceManager faceManager = getSystemService(FaceManager.class);
            final List<FaceSensorPropertiesInternal> faceProperties =
                    faceManager.getSensorPropertiesInternal();
            final int maxFacesEnrollableIfSUW = getApplicationContext().getResources()
                    .getInteger(R.integer.suw_max_faces_enrollable);
            if (!faceProperties.isEmpty()) {
                final FaceSensorPropertiesInternal props = faceProperties.get(0);
                final int maxEnrolls =
                        isSetupWizard ? maxFacesEnrollableIfSUW : props.maxEnrollmentsPerUser;
                final boolean isFaceStrong =
                        props.sensorStrength == SensorProperties.STRENGTH_STRONG;
                mIsFaceEnrollable =
                        faceManager.getEnrolledFaces(mUserId).size() < maxEnrolls;

                // If we expect strong bio only, check if face is strong
                if (authenticators == Authenticators.BIOMETRIC_STRONG && !isFaceStrong) {
                    mIsFaceEnrollable = false;
                }

                final boolean parentalConsent = isSetupWizard || (mParentalOptionsRequired
                        && !WizardManagerHelper.isUserSetupComplete(this));
                if (parentalConsent && isMultiSensor && mIsFaceEnrollable) {
                    // Exclude face enrollment from setup wizard if feature config not supported
                    // in setup wizard flow, we still allow user enroll faces through settings.
                    mIsFaceEnrollable = FeatureFactory.getFeatureFactory()
                            .getFaceFeatureProvider()
                            .isSetupWizardSupported(getApplicationContext());
                    Log.d(TAG, "config_suw_support_face_enroll: " + mIsFaceEnrollable);
                }
            }
        }
        updateFingerprintEnrollable(isSetupWizard);

        // TODO(b/195128094): remove this restriction
        // Consent can only be recorded when this activity is launched directly from the kids
        // module. This can be removed when there is a way to notify consent status out of band.
        if (isSetupWizard && mParentalOptionsRequired) {
            Log.w(TAG, "Enrollment with parental consent is not supported when launched "
                    + " directly from SuW - skipping enrollment");
            setResult(RESULT_SKIP);
            finish();
            return;
        }

        // Only allow the consent flow to happen once when running from setup wizard.
        // This isn't common and should only happen if setup wizard is not completed normally
        // due to a restart, etc.
        // This check should probably remain even if b/195128094 is fixed to prevent SuW from
        // restarting the process once it has been fully completed at least one time.
        if (isSetupWizard && mParentalOptionsRequired) {
            final boolean consentAlreadyManaged = ParentalControlsUtils.parentConsentRequired(this,
                    BiometricAuthenticator.TYPE_FACE | BiometricAuthenticator.TYPE_FINGERPRINT)
                    != null;
            if (consentAlreadyManaged) {
                Log.w(TAG, "Consent was already setup - skipping enrollment");
                setResult(RESULT_SKIP);
                finish();
                return;
            }
        }

        if (mParentalOptionsRequired && mParentalOptions == null) {
            mParentalConsentHelper = new ParentalConsentHelper(mGkPwHandle);
            setOrConfirmCredentialsNow();
        } else {
            // Start enrollment process if we haven't bailed out yet
            startEnrollWith(authenticators, isSetupWizard);
        }
    }

    private void updateFingerprintEnrollable(boolean isSetupWizard) {
        if (mHasFeatureFingerprint) {
            final int authenticators = getIntent().getIntExtra(
                    EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, Authenticators.BIOMETRIC_WEAK);

            final FingerprintManager fpManager = getSystemService(FingerprintManager.class);
            final List<FingerprintSensorPropertiesInternal> fpProperties =
                    fpManager.getSensorPropertiesInternal();
            final int maxFingerprintsEnrollableIfSUW = getApplicationContext().getResources()
                    .getInteger(R.integer.suw_max_fingerprints_enrollable);
            if (!fpProperties.isEmpty()) {
                final int maxEnrolls =
                        isSetupWizard ? maxFingerprintsEnrollableIfSUW
                                : fpProperties.get(0).maxEnrollmentsPerUser;
                final boolean isFingerprintStrong =
                        fpProperties.get(0).sensorStrength == SensorProperties.STRENGTH_STRONG;
                mIsFingerprintEnrollable =
                        fpManager.getEnrolledFingerprints(mUserId).size() < maxEnrolls;

                // If we expect strong bio only, check if fingerprint is strong
                if (authenticators == Authenticators.BIOMETRIC_STRONG && !isFingerprintStrong) {
                    mIsFingerprintEnrollable = false;
                }
            }
        }
    }

    private void startEnrollWith(@Authenticators.Types int authenticators, boolean setupWizard) {
        // If the caller is not setup wizard, and the user has something enrolled, finish.
        // Allow parental consent flow to skip this check, since one modality could be consented
        // and another non-consented. This can also happen if the restriction is applied when
        // enrollments already exists.
        if (!setupWizard && !mParentalOptionsRequired) {
            final BiometricManager bm = getSystemService(BiometricManager.class);
            final @BiometricError int result = bm.canAuthenticate(authenticators);
            if (result != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                Log.e(TAG, "Unexpected result (has enrollments): " + result);
                finish();
                return;
            }
        }

        boolean canUseFace = mHasFeatureFace;
        boolean canUseFingerprint = mHasFeatureFingerprint;
        if (mParentalOptionsRequired) {
            if (mParentalOptions == null) {
                throw new IllegalStateException("consent options required, but not set");
            }
            canUseFace = canUseFace
                    && ParentalConsentHelper.hasFaceConsent(mParentalOptions);
            canUseFingerprint = canUseFingerprint
                    && ParentalConsentHelper.hasFingerprintConsent(mParentalOptions);
        }

        // This will need to be updated if the device has sensors other than BIOMETRIC_STRONG
        if (!setupWizard && authenticators == BiometricManager.Authenticators.DEVICE_CREDENTIAL) {
            launchCredentialOnlyEnroll();
            finish();
        } else if (canUseFace || canUseFingerprint) {
            if (mGkPwHandle == null) {
                setOrConfirmCredentialsNow();
            } else if (canUseFingerprint && mIsFingerprintEnrollable) {
                launchFingerprintOnlyEnroll();
            } else if (canUseFace && mIsFaceEnrollable) {
                launchFaceOnlyEnroll();
            } else {
                setOrConfirmCredentialsNow();
            }
        } else { // no modalities available
            if (mParentalOptionsRequired) {
                Log.d(TAG, "No consent for any modality: skipping enrollment");
                setResult(RESULT_OK, newResultIntent());
            } else {
                Log.e(TAG, "Unknown state, finishing (was SUW: " + setupWizard + ")");
            }
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_STATE_CONFIRMING_CREDENTIALS, mConfirmingCredentials);
        outState.putBoolean(SAVED_STATE_IS_SINGLE_ENROLLING, mIsSingleEnrolling);
        outState.putBundle(SAVED_STATE_PASS_THROUGH_EXTRAS_FROM_CHOSEN_LOCK_IN_SUW,
                mPassThroughExtrasFromChosenLockInSuw);
        outState.putBoolean(SAVED_STATE_ENROLL_ACTION_LOGGED, mIsEnrollActionLogged);
        if (mParentalOptions != null) {
            outState.putBundle(SAVED_STATE_PARENTAL_OPTIONS, mParentalOptions);
        }
        if (mGkPwHandle != null) {
            outState.putLong(SAVED_STATE_GK_PW_HANDLE, mGkPwHandle);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (isSuccessfulChooseCredential(requestCode, resultCode)
                && data != null && data.getExtras() != null && data.getExtras().size() > 0
                && WizardManagerHelper.isAnySetupWizard(getIntent())) {
            mPassThroughExtrasFromChosenLockInSuw = data.getExtras();
        }

        Log.d(TAG,
                "onActivityResult(requestCode=" + requestCode + ", resultCode=" + resultCode + ")");
        // single enrollment is handled entirely by the launched activity
        // this handles multi enroll or if parental consent is required
        if (mParentalConsentHelper != null) {
            // Lazily retrieve the values from ParentalControlUtils, since the value may not be
            // ready in onCreate.
            final boolean faceConsentRequired = ParentalControlsUtils
                    .parentConsentRequired(this, BiometricAuthenticator.TYPE_FACE) != null;
            final boolean fpConsentRequired = ParentalControlsUtils
                    .parentConsentRequired(this, BiometricAuthenticator.TYPE_FINGERPRINT) != null;

            final boolean requestFaceConsent = faceConsentRequired
                    && mHasFeatureFace
                    && mIsFaceEnrollable;
            final boolean requestFpConsent = fpConsentRequired && mHasFeatureFingerprint;

            Log.d(TAG, "faceConsentRequired: " + faceConsentRequired
                    + ", fpConsentRequired: " + fpConsentRequired
                    + ", hasFeatureFace: " + mHasFeatureFace
                    + ", hasFeatureFingerprint: " + mHasFeatureFingerprint
                    + ", faceEnrollable: " + mIsFaceEnrollable
                    + ", fpEnrollable: " + mIsFingerprintEnrollable);

            mParentalConsentHelper.setConsentRequirement(requestFaceConsent, requestFpConsent);

            handleOnActivityResultWhileConsenting(requestCode, resultCode, data);
        } else {
            handleOnActivityResultWhileEnrolling(requestCode, resultCode, data);
        }
    }

    // handles responses while parental consent is pending
    private void handleOnActivityResultWhileConsenting(
            int requestCode, int resultCode, Intent data) {
        overridePendingTransition(
                com.google.android.setupdesign.R.anim.sud_slide_next_in,
                com.google.android.setupdesign.R.anim.sud_slide_next_out);

        switch (requestCode) {
            case REQUEST_CHOOSE_LOCK:
            case REQUEST_CONFIRM_LOCK:
                mConfirmingCredentials = false;
                if (isSuccessfulConfirmOrChooseCredential(requestCode, resultCode)) {
                    updateGatekeeperPasswordHandle(data);
                    if (!mParentalConsentHelper.launchNext(this, REQUEST_CHOOSE_OPTIONS)) {
                        Log.e(TAG, "Nothing to prompt for consent (no modalities enabled)!");
                        finish();
                    }
                } else {
                    Log.d(TAG, "Unknown result for set/choose lock: " + resultCode);
                    setResult(resultCode);
                    finish();
                }
                break;
            case REQUEST_CHOOSE_OPTIONS:
                if (resultCode == RESULT_CONSENT_GRANTED || resultCode == RESULT_CONSENT_DENIED) {
                    final boolean isStillPrompting = mParentalConsentHelper.launchNext(
                            this, REQUEST_CHOOSE_OPTIONS, resultCode, data);
                    if (!isStillPrompting) {
                        mParentalOptions = mParentalConsentHelper.getConsentResult();
                        mParentalConsentHelper = null;
                        Log.d(TAG, "Enrollment consent options set, starting enrollment: "
                                + mParentalOptions);
                        // Note that we start enrollment with CONVENIENCE instead of the default
                        // of WEAK in startEnroll(), since we want to allow enrollment for any
                        // sensor as long as it has been consented for. We should eventually
                        // clean up this logic and do something like pass in the parental consent
                        // result, so that we can request enrollment for specific sensors, but
                        // that's quite a large and risky change to the startEnrollWith() logic.
                        startEnrollWith(Authenticators.BIOMETRIC_CONVENIENCE,
                                WizardManagerHelper.isAnySetupWizard(getIntent()));
                    }
                } else {
                    Log.d(TAG, "Unknown or cancelled parental consent");
                    setResult(RESULT_CANCELED, newResultIntent());
                    finish();
                }
                break;
            default:
                Log.w(TAG, "Unknown consenting requestCode: " + requestCode + ", finishing");
                finish();
        }
    }

    // handles responses while multi biometric enrollment is pending
    private void handleOnActivityResultWhileEnrolling(
            int requestCode, int resultCode, Intent data) {

        Log.d(TAG, "handleOnActivityResultWhileEnrolling, request = " + requestCode + ""
                + ", resultCode = " + resultCode);
        switch (requestCode) {
            case REQUEST_HANDOFF_PARENT:
                setResult(RESULT_OK, newResultIntent());
                finish();
                break;
            case REQUEST_CHOOSE_LOCK:
            case REQUEST_CONFIRM_LOCK:
                mConfirmingCredentials = false;
                final boolean isOk =
                        isSuccessfulConfirmOrChooseCredential(requestCode, resultCode);
                if (isOk && (mIsFaceEnrollable || mIsFingerprintEnrollable)) {
                    // Apply forward animation during the transition from ChooseLock/ConfirmLock to
                    // SetupFingerprintEnrollIntroduction/FingerprintEnrollmentActivity
                    TransitionHelper.applyForwardTransition(this, TRANSITION_FADE_THROUGH);
                    updateGatekeeperPasswordHandle(data);
                    if (mIsFingerprintEnrollable) {
                        launchFingerprintOnlyEnroll();
                    } else {
                        launchFaceOnlyEnroll();
                    }
                } else {
                    Log.d(TAG, "Unknown result for set/choose lock: " + resultCode);
                    setResult(resultCode, newResultIntent());
                    finish();
                }
                break;
            case REQUEST_SINGLE_ENROLL_FINGERPRINT:
                mIsSingleEnrolling = false;
                if (resultCode == BiometricEnrollBase.RESULT_FINISHED) {
                    // FingerprintEnrollIntroduction's visibility is determined by
                    // mIsFingerprintEnrollable. Keep this value up-to-date after a successful
                    // enrollment.
                    updateFingerprintEnrollable(WizardManagerHelper.isAnySetupWizard(getIntent()));
                }
                if ((resultCode == BiometricEnrollBase.RESULT_SKIP
                        || resultCode == BiometricEnrollBase.RESULT_FINISHED)
                        && mIsFaceEnrollable) {
                    // Apply forward animation during the transition from
                    // SetupFingerprintEnroll*/FingerprintEnrollmentActivity to
                    // SetupFaceEnrollIntroduction
                    TransitionHelper.applyForwardTransition(this, TRANSITION_FADE_THROUGH);
                    launchFaceOnlyEnroll();
                } else {
                    finishOrLaunchHandToParent(resultCode);
                }
                break;
            case REQUEST_SINGLE_ENROLL_FACE:
                mIsSingleEnrolling = false;
                if (resultCode == Activity.RESULT_CANCELED && mIsFingerprintEnrollable) {
                    launchFingerprintOnlyEnroll();
                } else {
                    finishOrLaunchHandToParent(resultCode);
                }
                break;
            default:
                Log.w(TAG, "Unknown enrolling requestCode: " + requestCode + ", finishing");
                finish();
        }
    }

    @Override
    public void finish() {
        if (mGkPwHandle != null) {
            // When launched as InternalActivity, the mGkPwHandle was gotten from intent extra
            // instead of requesting from the user. Do not remove the mGkPwHandle in service side
            // for this case because the caller activity may still need it and will be responsible
            // for removing it.
            if (!(this instanceof InternalActivity)) {
                BiometricUtils.removeGatekeeperPasswordHandle(this, mGkPwHandle);
            }
        }
        super.finish();
    }

    private void finishOrLaunchHandToParent(int resultCode) {
        if (mParentalOptionsRequired) {
            if (!mSkipReturnToParent) {
                launchHandoffToParent();
            } else {
                setResult(RESULT_OK, newResultIntent());
                finish();
            }
        } else {
            setResult(resultCode, newResultIntent());
            finish();
        }
    }

    @NonNull
    private Intent newResultIntent() {
        final Intent intent = new Intent();
        if (mParentalOptionsRequired && mParentalOptions != null) {
            final Bundle consentStatus = mParentalOptions.deepCopy();
            intent.putExtra(EXTRA_PARENTAL_CONSENT_STATUS, consentStatus);
            Log.v(TAG, "Result consent status: " + consentStatus);
        }
        if (mPassThroughExtrasFromChosenLockInSuw != null) {
            intent.putExtras(mPassThroughExtrasFromChosenLockInSuw);
        }
        return intent;
    }

    private static boolean isSuccessfulConfirmOrChooseCredential(int requestCode, int resultCode) {
        return isSuccessfulChooseCredential(requestCode, resultCode)
                || isSuccessfulConfirmCredential(requestCode, resultCode);
    }

    private static boolean isSuccessfulChooseCredential(int requestCode, int resultCode) {
        return requestCode == REQUEST_CHOOSE_LOCK
                && resultCode == ChooseLockPattern.RESULT_FINISHED;
    }

    private static boolean isSuccessfulConfirmCredential(int requestCode, int resultCode) {
        return requestCode == REQUEST_CONFIRM_LOCK && resultCode == RESULT_OK;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        final int newResid = SetupWizardUtils.getTheme(this, getIntent());
        theme.applyStyle(R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, newResid, first);
    }

    private void setOrConfirmCredentialsNow() {
        if (!mConfirmingCredentials) {
            mConfirmingCredentials = true;
            if (!userHasPassword(mUserId)) {
                launchChooseLock();
            } else {
                launchConfirmLock();
            }
        }
    }

    private void updateGatekeeperPasswordHandle(@NonNull Intent data) {
        mGkPwHandle = BiometricUtils.getGatekeeperPasswordHandle(data);
        if (mParentalConsentHelper != null) {
            mParentalConsentHelper.updateGatekeeperHandle(data);
        }
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
        if (mIsFingerprintEnrollable && mIsFaceEnrollable) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS, true);
        } else if (mIsFaceEnrollable) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE, true);
        } else if (mIsFingerprintEnrollable) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true);
        }

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

    // This should only be used to launch enrollment for single-sensor devices.
    private void launchSingleSensorEnrollActivity(@NonNull Intent intent, int requestCode) {
        byte[] hardwareAuthToken = null;
        if (this instanceof InternalActivity) {
            hardwareAuthToken = getIntent().getByteArrayExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        }
        BiometricUtils.launchEnrollForResult(this, intent, requestCode, hardwareAuthToken,
                mGkPwHandle, mUserId);
    }

    private void launchCredentialOnlyEnroll() {
        final Intent intent;
        // If only device credential was specified, ask the user to only set that up.
        intent = new Intent(this, ChooseLockGeneric.class);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS, true);
        launchSingleSensorEnrollActivity(intent, 0 /* requestCode */);
    }

    private void launchFingerprintOnlyEnroll() {
        if (!mIsSingleEnrolling) {
            mIsSingleEnrolling = true;
            final Intent intent;
            // ChooseLockGeneric can request to start fingerprint enroll bypassing the intro screen.
            if (getIntent().getBooleanExtra(EXTRA_SKIP_INTRO, false)
                    && this instanceof InternalActivity) {
                intent = BiometricUtils.getFingerprintFindSensorIntent(this, getIntent());
            } else {
                intent = BiometricUtils.getFingerprintIntroIntent(this, getIntent());
            }
            launchSingleSensorEnrollActivity(intent, REQUEST_SINGLE_ENROLL_FINGERPRINT);
        }
    }

    private void launchFaceOnlyEnroll() {
        if (!mIsSingleEnrolling) {
            mIsSingleEnrolling = true;
            final Intent intent = BiometricUtils.getFaceIntroIntent(this, getIntent());
            launchSingleSensorEnrollActivity(intent, REQUEST_SINGLE_ENROLL_FACE);
        }
    }

    private void launchHandoffToParent() {
        final Intent intent = BiometricUtils.getHandoffToParentIntent(this, getIntent());
        startActivityForResult(intent, REQUEST_HANDOFF_PARENT);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BIOMETRIC_ENROLL_ACTIVITY;
    }
}

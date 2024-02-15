/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.biometrics;

import static android.util.FeatureFlagUtils.SETTINGS_BIOMETRICS2_ENROLLMENT;

import android.annotation.IntDef;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.hardware.biometrics.SensorProperties;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.text.BidiFormatter;
import android.text.SpannableStringBuilder;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.settings.biometrics.face.FaceEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollFindSensor;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollFindSensor;
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollIntroduction;
import com.android.settings.biometrics2.ui.view.FingerprintEnrollmentActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.SetupChooseLockGeneric;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Common biometric utilities.
 */
public class BiometricUtils {
    private static final String TAG = "BiometricUtils";

    /** The character ' • ' to separate the setup choose options */
    public static final String SEPARATOR = " \u2022 ";

    // Note: Theis IntDef must align SystemUI DevicePostureInt
    @IntDef(prefix = {"DEVICE_POSTURE_"}, value = {
            DEVICE_POSTURE_UNKNOWN,
            DEVICE_POSTURE_CLOSED,
            DEVICE_POSTURE_HALF_OPENED,
            DEVICE_POSTURE_OPENED,
            DEVICE_POSTURE_FLIPPED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface DevicePostureInt {}

    // NOTE: These constants **must** match those defined for Jetpack Sidecar. This is because we
    // use the Device State -> Jetpack Posture map in DevicePostureControllerImpl to translate
    // between the two.
    public static final int DEVICE_POSTURE_UNKNOWN = 0;
    public static final int DEVICE_POSTURE_CLOSED = 1;
    public static final int DEVICE_POSTURE_HALF_OPENED = 2;
    public static final int DEVICE_POSTURE_OPENED = 3;
    public static final int DEVICE_POSTURE_FLIPPED = 4;

    public static int sAllowEnrollPosture = DEVICE_POSTURE_UNKNOWN;

    /**
     * Request was sent for starting another enrollment of a previously
     * enrolled biometric of the same type.
     */
    public static int REQUEST_ADD_ANOTHER = 7;

    /**
     * Gatekeeper credential not match exception, it throws if VerifyCredentialResponse is not
     * matched in requestGatekeeperHat().
     */
    public static class GatekeeperCredentialNotMatchException extends IllegalStateException {
        public GatekeeperCredentialNotMatchException(String s) {
            super(s);
        }
    };

    /**
     * @deprecated Use {@link com.android.settings.biometrics.GatekeeperPasswordProvider} instead.
     *
     * Given the result from confirming or choosing a credential, request Gatekeeper to generate
     * a HardwareAuthToken with the Gatekeeper Password together with a biometric challenge.
     *
     * @param context Caller's context
     * @param result The onActivityResult intent from ChooseLock* or ConfirmLock*
     * @param userId User ID that the credential/biometric operation applies to
     * @param challenge Unique biometric challenge from FingerprintManager/FaceManager
     * @return
     * @throws GatekeeperCredentialNotMatchException if Gatekeeper response is not match
     * @throws IllegalStateException if Gatekeeper Password is missing
     */
    @Deprecated
    public static byte[] requestGatekeeperHat(@NonNull Context context, @NonNull Intent result,
            int userId, long challenge) {
        if (!containsGatekeeperPasswordHandle(result)) {
            throw new IllegalStateException("Gatekeeper Password is missing!!");
        }
        final long gatekeeperPasswordHandle = result.getLongExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 0L);
        return requestGatekeeperHat(context, gatekeeperPasswordHandle, userId, challenge);
    }

    /**
     * @deprecated Use {@link com.android.settings.biometrics.GatekeeperPasswordProvider} instead.
     */
    @Deprecated
    public static byte[] requestGatekeeperHat(@NonNull Context context, long gkPwHandle, int userId,
            long challenge) {
        final LockPatternUtils utils = new LockPatternUtils(context);
        final VerifyCredentialResponse response = utils.verifyGatekeeperPasswordHandle(gkPwHandle,
                challenge, userId);
        if (!response.isMatched()) {
            throw new GatekeeperCredentialNotMatchException("Unable to request Gatekeeper HAT");
        }
        return response.getGatekeeperHAT();
    }

    /**
     * @deprecated Use {@link com.android.settings.biometrics.GatekeeperPasswordProvider} instead.
     */
    @Deprecated
    public static boolean containsGatekeeperPasswordHandle(@Nullable Intent data) {
        return data != null && data.hasExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE);
    }

    /**
     * @deprecated Use {@link com.android.settings.biometrics.GatekeeperPasswordProvider} instead.
     */
    @Deprecated
    public static long getGatekeeperPasswordHandle(@NonNull Intent data) {
        return data.getLongExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 0L);
    }

    /**
     * @deprecated Use {@link com.android.settings.biometrics.GatekeeperPasswordProvider} instead.
     *
     * Requests {@link com.android.server.locksettings.LockSettingsService} to remove the
     * gatekeeper password associated with a previous
     * {@link ChooseLockSettingsHelper.Builder#setRequestGatekeeperPasswordHandle(boolean)}
     *
     * @param context Caller's context
     * @param data The onActivityResult intent from ChooseLock* or ConfirmLock*
     */
    @Deprecated
    public static void removeGatekeeperPasswordHandle(@NonNull Context context,
            @Nullable Intent data) {
        if (data == null) {
            return;
        }
        if (!containsGatekeeperPasswordHandle(data)) {
            return;
        }
        removeGatekeeperPasswordHandle(context, getGatekeeperPasswordHandle(data));
    }

    /**
     * @deprecated Use {@link com.android.settings.biometrics.GatekeeperPasswordProvider} instead.
     */
    @Deprecated
    public static void removeGatekeeperPasswordHandle(@NonNull Context context, long handle) {
        final LockPatternUtils utils = new LockPatternUtils(context);
        utils.removeGatekeeperPasswordHandle(handle);
        Log.d(TAG, "Removed handle");
    }

    /**
     * @param context caller's context
     * @param activityIntent The intent that started the caller's activity
     * @return Intent for starting ChooseLock*
     */
    public static Intent getChooseLockIntent(@NonNull Context context,
            @NonNull Intent activityIntent) {
        if (WizardManagerHelper.isAnySetupWizard(activityIntent)) {
            // Default to PIN lock in setup wizard
            Intent intent = new Intent(context, SetupChooseLockGeneric.class);
            if (StorageManager.isFileEncrypted()) {
                intent.putExtra(
                        LockPatternUtils.PASSWORD_TYPE_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment
                        .EXTRA_SHOW_OPTIONS_BUTTON, true);
            }
            WizardManagerHelper.copyWizardManagerExtras(activityIntent, intent);
            return intent;
        } else {
            return new Intent(context, ChooseLockGeneric.class);
        }
    }

    /**
     * @param context caller's context
     * @param isSuw if it is running in setup wizard flows
     * @param suwExtras setup wizard extras for new intent
     * @return Intent for starting ChooseLock*
     */
    public static Intent getChooseLockIntent(@NonNull Context context,
            boolean isSuw, @NonNull Bundle suwExtras) {
        if (isSuw) {
            // Default to PIN lock in setup wizard
            Intent intent = new Intent(context, SetupChooseLockGeneric.class);
            if (StorageManager.isFileEncrypted()) {
                intent.putExtra(
                        LockPatternUtils.PASSWORD_TYPE_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment
                        .EXTRA_SHOW_OPTIONS_BUTTON, true);
            }
            intent.putExtras(suwExtras);
            return intent;
        } else {
            return new Intent(context, ChooseLockGeneric.class);
        }
    }

    /**
     * @param context caller's context
     * @param activityIntent The intent that started the caller's activity
     * @return Intent for starting FingerprintEnrollFindSensor
     */
    public static Intent getFingerprintFindSensorIntent(@NonNull Context context,
            @NonNull Intent activityIntent) {
        final boolean isSuw =  WizardManagerHelper.isAnySetupWizard(activityIntent);
        final Intent intent;
        if (FeatureFlagUtils.isEnabled(context, SETTINGS_BIOMETRICS2_ENROLLMENT)) {
            intent = new Intent(context, isSuw
                    ? FingerprintEnrollmentActivity.SetupActivity.class
                    : FingerprintEnrollmentActivity.class);
            intent.putExtra(BiometricEnrollActivity.EXTRA_SKIP_INTRO, true);
        } else {
            intent = new Intent(context, isSuw
                    ? SetupFingerprintEnrollFindSensor.class
                    : FingerprintEnrollFindSensor.class);
        }
        if (isSuw) {
            SetupWizardUtils.copySetupExtras(activityIntent, intent);
        }
        return intent;
    }

    /**
     * @param context caller's context
     * @param activityIntent The intent that started the caller's activity
     * @return Intent for starting FingerprintEnrollIntroduction
     */
    public static Intent getFingerprintIntroIntent(@NonNull Context context,
            @NonNull Intent activityIntent) {
        final boolean isSuw = WizardManagerHelper.isAnySetupWizard(activityIntent);
        final Intent intent;
        if (FeatureFlagUtils.isEnabled(context, SETTINGS_BIOMETRICS2_ENROLLMENT)) {
            intent = new Intent(context, isSuw
                    ? FingerprintEnrollmentActivity.SetupActivity.class
                    : FingerprintEnrollmentActivity.class);
        } else {
            intent = new Intent(context, isSuw
                    ? SetupFingerprintEnrollIntroduction.class
                    : FingerprintEnrollIntroduction.class);
        }
        if (isSuw) {
            WizardManagerHelper.copyWizardManagerExtras(activityIntent, intent);
        }
        return intent;
    }

    /**
     * @param context caller's context
     * @param activityIntent The intent that started the caller's activity
     * @return Intent for starting FaceEnrollIntroduction
     */
    public static Intent getFaceIntroIntent(@NonNull Context context,
            @NonNull Intent activityIntent) {
        final Intent intent = new Intent(context, FaceEnrollIntroduction.class);
        WizardManagerHelper.copyWizardManagerExtras(activityIntent, intent);
        return intent;
    }

    /**
     * Start an activity that prompts the user to hand the device to their parent or guardian.
     * @param context caller's context
     * @param activityIntent The intent that started the caller's activity
     * @return Intent for starting BiometricHandoffActivity
     */
    public static Intent getHandoffToParentIntent(@NonNull Context context,
            @NonNull Intent activityIntent) {
        final Intent intent = new Intent(context, BiometricHandoffActivity.class);
        WizardManagerHelper.copyWizardManagerExtras(activityIntent, intent);
        return intent;
    }

    /**
     * @param activity Reference to the calling activity, used to startActivity
     * @param intent Intent pointing to the enrollment activity
     * @param requestCode If non-zero, will invoke startActivityForResult instead of startActivity
     * @param hardwareAuthToken HardwareAuthToken from Gatekeeper
     * @param userId User to request enrollment for
     */
    public static void launchEnrollForResult(@NonNull FragmentActivity activity,
            @NonNull Intent intent, int requestCode,
            @Nullable byte[] hardwareAuthToken, @Nullable Long gkPwHandle, int userId) {
        if (hardwareAuthToken != null) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                    hardwareAuthToken);
        }
        if (gkPwHandle != null) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, (long) gkPwHandle);
        }

        if (activity instanceof BiometricEnrollActivity.InternalActivity) {
            intent.putExtra(Intent.EXTRA_USER_ID, userId);
        }

        if (requestCode != 0) {
            activity.startActivityForResult(intent, requestCode);
        } else {
            activity.startActivity(intent);
            activity.finish();
        }
    }

    /**
     * Used for checking if a multi-biometric enrollment flow starts with Face and
     * ends with Fingerprint.
     *
     * @param activity Activity that we want to check
     * @return True if the activity is going through a multi-biometric enrollment flow, that starts
     * with Face.
     */
    public static boolean isMultiBiometricFaceEnrollmentFlow(@NonNull Activity activity) {
        return activity.getIntent().hasExtra(MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FACE);
    }

    /**
     * Used for checking if a multi-biometric enrollment flowstarts with Fingerprint
     * and ends with Face.
     *
     * @param activity Activity that we want to check
     * @return True if the activity is going through a multi-biometric enrollment flow, that starts
     * with Fingerprint.
     */
    public static boolean isMultiBiometricFingerprintEnrollmentFlow(@NonNull Activity activity) {
        return activity.getIntent().hasExtra(
                MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FINGERPRINT);
    }

    /**
     * Used to check if the activity is a multi biometric flow activity.
     *
     * @param activity Activity to check
     * @return True if the activity is going through a multi-biometric enrollment flow, that starts
     * with Fingerprint.
     */
    public static boolean isAnyMultiBiometricFlow(@NonNull Activity activity) {
        return isMultiBiometricFaceEnrollmentFlow(activity)
                || isMultiBiometricFingerprintEnrollmentFlow(activity);
    }

    /**
     * Used to check if the activity is showing a posture guidance to user.
     *
     * @param devicePosture the device posture state
     * @param isLaunchedPostureGuidance True launching a posture guidance to user
     * @return True if the activity is showing posture guidance to user
     */
    public static boolean isPostureGuidanceShowing(@DevicePostureInt int devicePosture,
            boolean isLaunchedPostureGuidance) {
        return !isPostureAllowEnrollment(devicePosture) && isLaunchedPostureGuidance;
    }

    /**
     * Used to check if current device posture state is allow to enroll biometrics.
     * For compatibility, we don't restrict enrollment if device do not config.
     *
     * @param devicePosture True if current device posture allow enrollment
     * @return True if current device posture state allow enrollment
     */
    public static boolean isPostureAllowEnrollment(@DevicePostureInt int devicePosture) {
        return (sAllowEnrollPosture == DEVICE_POSTURE_UNKNOWN)
                || (devicePosture == sAllowEnrollPosture);
    }

    /**
     * Used to check if the activity should show a posture guidance to user.
     *
     * @param devicePosture the device posture state
     * @param isLaunchedPostureGuidance True launching a posture guidance to user
     * @return True if posture disallow enroll and posture guidance not showing, false otherwise.
     */
    public static boolean shouldShowPostureGuidance(@DevicePostureInt int devicePosture,
            boolean isLaunchedPostureGuidance) {
        return !isPostureAllowEnrollment(devicePosture) && !isLaunchedPostureGuidance;
    }

    /**
     * Sets allowed device posture for face enrollment.
     *
     * @param devicePosture the allowed posture state {@link DevicePostureInt} for enrollment
     */
    public static void setDevicePosturesAllowEnroll(@DevicePostureInt int devicePosture) {
        sAllowEnrollPosture = devicePosture;
    }

    public static void copyMultiBiometricExtras(@NonNull Intent fromIntent,
            @NonNull Intent toIntent) {
        PendingIntent pendingIntent = (PendingIntent) fromIntent.getExtra(
                MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FACE, null);
        if (pendingIntent != null) {
            toIntent.putExtra(MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FACE,
                    pendingIntent);
        }

        pendingIntent = (PendingIntent) fromIntent.getExtra(
                MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FINGERPRINT, null);
        if (pendingIntent != null) {
            toIntent.putExtra(MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FINGERPRINT,
                    pendingIntent);
        }
    }

    /**
     * If the current biometric enrollment (e.g. face/fingerprint) should be followed by another
     * one (e.g. fingerprint/face) retrieves the PendingIntent pointing to the next enrollment
     * and starts it. The caller will receive the result in onActivityResult.
     * @return true if the next enrollment was started
     */
    public static boolean tryStartingNextBiometricEnroll(@NonNull Activity activity,
            int requestCode, String debugReason) {

        PendingIntent pendingIntent = (PendingIntent) activity.getIntent()
                .getExtra(MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FACE);
        if (pendingIntent == null) {
            pendingIntent = (PendingIntent) activity.getIntent()
                .getExtra(MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FINGERPRINT);
        }

        if (pendingIntent != null) {
            try {
                IntentSender intentSender = pendingIntent.getIntentSender();
                activity.startIntentSenderForResult(intentSender, requestCode,
                        null /* fillInIntent */, 0 /* flagMask */, 0 /* flagValues */,
                        0 /* extraFlags */);
                return true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Pending intent canceled: " + e);
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the screen is going into a landscape mode and the angle is equal to
     * 270.
     * @param context Context that we use to get the display this context is associated with
     * @return True if the angle of the rotation is equal to 270.
     */
    public static boolean isReverseLandscape(@NonNull Context context) {
        return context.getDisplay().getRotation() == Surface.ROTATION_270;
    }

    /**
     * @param faceManager
     * @return True if at least one sensor is set as a convenience.
     */
    public static boolean isConvenience(@NonNull FaceManager faceManager) {
        for (FaceSensorPropertiesInternal props : faceManager.getSensorPropertiesInternal()) {
            if (props.sensorStrength == SensorProperties.STRENGTH_CONVENIENCE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the screen is going into a landscape mode and the angle is equal to
     * 90.
     * @param context Context that we use to get the display this context is associated with
     * @return True if the angle of the rotation is equal to 90.
     */
    public static boolean isLandscape(@NonNull Context context) {
        return context.getDisplay().getRotation() == Surface.ROTATION_90;
    }

    /**
     * Returns true if the device supports Face enrollment in SUW flow
     */
    public static boolean isFaceSupportedInSuw(Context context) {
        return FeatureFactory.getFeatureFactory().getFaceFeatureProvider().isSetupWizardSupported(
                context);
    }

    /**
     * Returns the combined screen lock options by device biometrics config
     * @param context the application context
     * @param screenLock the type of screen lock(PIN, Pattern, Password) in string
     * @param hasFingerprint device support fingerprint or not
     * @param isFaceSupported device support face or not
     * @return the options combined with screen lock, face, and fingerprint in String format.
     */
    public static String getCombinedScreenLockOptions(Context context,
            CharSequence screenLock, boolean hasFingerprint, boolean isFaceSupported) {
        final SpannableStringBuilder ssb = new SpannableStringBuilder();
        final BidiFormatter bidi = BidiFormatter.getInstance();
        // Assume the flow is "Screen Lock" + "Face" + "Fingerprint"
        ssb.append(bidi.unicodeWrap(screenLock));

        if (hasFingerprint) {
            ssb.append(bidi.unicodeWrap(SEPARATOR));
            ssb.append(bidi.unicodeWrap(
                    capitalize(context.getString(R.string.security_settings_fingerprint))));
        }

        if (isFaceSupported) {
            ssb.append(bidi.unicodeWrap(SEPARATOR));
            ssb.append(bidi.unicodeWrap(
                    capitalize(context.getString(R.string.keywords_face_settings))));
        }

        return ssb.toString();
    }

    private static String capitalize(final String input) {
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}

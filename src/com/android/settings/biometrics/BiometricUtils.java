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

import android.app.Activity;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.SetupWizardUtils;
import com.android.settings.biometrics.face.FaceEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollFindSensor;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollIntroduction;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.SetupChooseLockGeneric;

import com.google.android.setupcompat.util.WizardManagerHelper;

/**
 * Common biometric utilities.
 */
public class BiometricUtils {
    private static final String TAG = "BiometricUtils";
    /**
     * Given the result from confirming or choosing a credential, request Gatekeeper to generate
     * a HardwareAuthToken with the Gatekeeper Password together with a biometric challenge.
     *
     * @param context Caller's context
     * @param result The onActivityResult intent from ChooseLock* or ConfirmLock*
     * @param userId User ID that the credential/biometric operation applies to
     * @param challenge Unique biometric challenge from FingerprintManager/FaceManager
     * @return
     */
    public static byte[] requestGatekeeperHat(@NonNull Context context, @NonNull Intent result,
            int userId, long challenge) {
        if (!containsGatekeeperPasswordHandle(result)) {
            throw new IllegalStateException("Gatekeeper Password is missing!!");
        }
        final long gatekeeperPasswordHandle = result.getLongExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 0L);
        return requestGatekeeperHat(context, gatekeeperPasswordHandle, userId, challenge);
    }

    public static byte[] requestGatekeeperHat(@NonNull Context context, long gkPwHandle, int userId,
            long challenge) {
        final LockPatternUtils utils = new LockPatternUtils(context);
        final VerifyCredentialResponse response = utils.verifyGatekeeperPasswordHandle(gkPwHandle,
                challenge, userId);
        if (!response.isMatched()) {
            throw new IllegalStateException("Unable to request Gatekeeper HAT");
        }
        return response.getGatekeeperHAT();
    }

    public static boolean containsGatekeeperPasswordHandle(@Nullable Intent data) {
        return data != null && data.hasExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE);
    }

    public static long getGatekeeperPasswordHandle(@NonNull Intent data) {
        return data.getLongExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 0L);
    }

    /**
     * Requests {@link com.android.server.locksettings.LockSettingsService} to remove the
     * gatekeeper password associated with a previous
     * {@link ChooseLockSettingsHelper.Builder#setRequestGatekeeperPasswordHandle(boolean)}
     *
     * @param context Caller's context
     * @param data The onActivityResult intent from ChooseLock* or ConfirmLock*
     */
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
            if (StorageManager.isFileEncryptedNativeOrEmulated()) {
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
     * @param activityIntent The intent that started the caller's activity
     * @return Intent for starting FingerprintEnrollFindSensor
     */
    public static Intent getFingerprintFindSensorIntent(@NonNull Context context,
            @NonNull Intent activityIntent) {
        Intent intent = new Intent(context, FingerprintEnrollFindSensor.class);
        SetupWizardUtils.copySetupExtras(activityIntent, intent);
        return intent;
    }

    /**
     * @param context caller's context
     * @param activityIntent The intent that started the caller's activity
     * @return Intent for starting FingerprintEnrollIntroduction
     */
    public static Intent getFingerprintIntroIntent(@NonNull Context context,
            @NonNull Intent activityIntent) {
        if (WizardManagerHelper.isAnySetupWizard(activityIntent)) {
            Intent intent = new Intent(context, SetupFingerprintEnrollIntroduction.class);
            WizardManagerHelper.copyWizardManagerExtras(activityIntent, intent);
            return intent;
        } else {
            return new Intent(context, FingerprintEnrollIntroduction.class);
        }
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
     * @param activity Activity that we want to check
     * @return True if the activity is going through a multi-biometric enrollment flow.
     */
    public static boolean isMultiBiometricEnrollmentFlow(@NonNull Activity activity) {
        return activity.getIntent().hasExtra(MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FACE);
    }

    public static void copyMultiBiometricExtras(@NonNull Intent fromIntent,
            @NonNull Intent toIntent) {
        final PendingIntent pendingIntent = (PendingIntent) fromIntent.getExtra(
                MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FACE, null);
        if (pendingIntent != null) {
            toIntent.putExtra(MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FACE, pendingIntent);
        }
    }

    /**
     * If the current biometric enrollment (e.g. face) should be followed by another one (e.g.
     * fingerprint) (see {@link #isMultiBiometricEnrollmentFlow(Activity)}), retrieves the
     * PendingIntent pointing to the next enrollment and starts it. The caller will receive the
     * result in onActivityResult.
     * @return true if the next enrollment was started
     */
    public static boolean tryStartingNextBiometricEnroll(@NonNull Activity activity,
            int requestCode, String debugReason) {

        Log.d(TAG, "tryStartingNextBiometricEnroll, debugReason: " + debugReason);
        final PendingIntent pendingIntent = (PendingIntent) activity.getIntent()
                .getExtra(MultiBiometricEnrollHelper.EXTRA_ENROLL_AFTER_FACE);
        if (pendingIntent != null) {
            try {
                Log.d(TAG, "Starting pendingIntent: " + pendingIntent);
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
}

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

package com.android.settings.biometrics;

import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FACE;
import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FINGERPRINT;
import static android.hardware.biometrics.BiometricAuthenticator.TYPE_NONE;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_MODALITY;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_CONSENT_DENIED;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_CONSENT_GRANTED;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.biometrics.face.FaceEnrollParentalConsent;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollParentalConsent;
import com.android.settings.password.ChooseLockSettingsHelper;

import com.google.android.setupcompat.util.WizardManagerHelper;

/**
 * Helper for {@link BiometricEnrollActivity} to ask for parental consent prior to actual user
 * enrollment.
 */
public class ParentalConsentHelper {

    private static final String KEY_FACE_CONSENT = "face";
    private static final String KEY_FINGERPRINT_CONSENT = "fingerprint";
    private static final String KEY_IRIS_CONSENT = "iris";

    private static final String KEY_FACE_CONSENT_STRINGS = "face_strings";
    private static final String KEY_FINGERPRINT_CONSENT_STRINGS = "fingerprint_strings";
    private static final String KEY_IRIS_CONSENT_STRINGS = "iris_strings";

    private boolean mRequireFace;
    private boolean mRequireFingerprint;

    private long mGkPwHandle;
    @Nullable
    private Boolean mConsentFace;
    @Nullable
    private Boolean mConsentFingerprint;

    /**
     * Helper for aggregating user consent.
     *
     * @param gkPwHandle for launched intents
     */
    public ParentalConsentHelper(@Nullable Long gkPwHandle) {
        mGkPwHandle = gkPwHandle != null ? gkPwHandle : 0L;
    }

    /**
     * @param requireFace if face consent should be shown
     * @param requireFingerprint if fingerprint consent should be shown
     */
    public void setConsentRequirement(boolean requireFace, boolean requireFingerprint) {
        mRequireFace = requireFace;
        mRequireFingerprint = requireFingerprint;
    }

    /**
     * Updated the handle used for launching activities
     *
     * @param data result intent for credential verification
     */
    public void updateGatekeeperHandle(Intent data) {
        mGkPwHandle = BiometricUtils.getGatekeeperPasswordHandle(data);
    }

    /**
     * Launch the next consent screen.
     *
     * @param activity root activity
     * @param requestCode request code to launch new activity
     * @param resultCode result code of the last consent launch
     * @param data result data from the last consent launch
     * @return true if a consent activity was launched or false when complete
     */
    public boolean launchNext(@NonNull Activity activity, int requestCode, int resultCode,
            @Nullable Intent data) {
        if (data != null) {
            switch (data.getIntExtra(EXTRA_KEY_MODALITY, TYPE_NONE)) {
                case TYPE_FACE:
                    mConsentFace = isConsent(resultCode, mConsentFace);
                    break;
                case TYPE_FINGERPRINT:
                    mConsentFingerprint = isConsent(resultCode, mConsentFingerprint);
                    break;
            }
        }
        return launchNext(activity, requestCode);
    }

    @Nullable
    private static Boolean isConsent(int resultCode, @Nullable Boolean defaultValue) {
        switch (resultCode) {
            case RESULT_CONSENT_GRANTED:
                return true;
            case RESULT_CONSENT_DENIED:
                return false;
        }
        return defaultValue;
    }

    /** @see #launchNext(Activity, int, int, Intent)  */
    public boolean launchNext(@NonNull Activity activity, int requestCode) {
        final Intent intent = getNextConsentIntent(activity);
        if (intent != null) {
            WizardManagerHelper.copyWizardManagerExtras(activity.getIntent(), intent);
            if (mGkPwHandle != 0) {
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, mGkPwHandle);
            }
            activity.startActivityForResult(intent, requestCode);
            return true;
        }
        return false;
    }

    @Nullable
    private Intent getNextConsentIntent(@NonNull Context context) {
        if (mRequireFingerprint && mConsentFingerprint == null) {
            return new Intent(context, FingerprintEnrollParentalConsent.class);
        }
        if (mRequireFace && mConsentFace == null) {
            return new Intent(context, FaceEnrollParentalConsent.class);
        }
        return null;
    }

    /**
     * Get the result of all consent requests.
     *
     * This should be called when {@link #launchNext(Activity, int, int, Intent)} returns false
     * to indicate that all responses have been recorded.
     *
     * @return The aggregate consent status.
     */
    @NonNull
    public Bundle getConsentResult() {
        final Bundle result = new Bundle();
        result.putBoolean(KEY_FACE_CONSENT, mConsentFace != null ? mConsentFace : false);
        result.putIntArray(KEY_FACE_CONSENT_STRINGS,
                FaceEnrollParentalConsent.CONSENT_STRING_RESOURCES);
        result.putBoolean(KEY_FINGERPRINT_CONSENT,
                mConsentFingerprint != null ? mConsentFingerprint : false);
        result.putIntArray(KEY_FINGERPRINT_CONSENT_STRINGS,
                FingerprintEnrollParentalConsent.CONSENT_STRING_RESOURCES);
        result.putBoolean(KEY_IRIS_CONSENT, false);
        result.putIntArray(KEY_IRIS_CONSENT_STRINGS, new int[0]);
        return result;
    }

    /** @return If the result bundle contains consent for face authentication. */
    public static boolean hasFaceConsent(@NonNull Bundle bundle) {
        return bundle.getBoolean(KEY_FACE_CONSENT, false);
    }

    /** @return If the result bundle contains consent for fingerprint authentication. */
    public static boolean hasFingerprintConsent(@NonNull Bundle bundle) {
        return bundle.getBoolean(KEY_FINGERPRINT_CONSENT, false);
    }
}

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

import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.password.ChooseLockSettingsHelper;

import java.util.function.Function;

/**
 * Helper for {@link BiometricEnrollActivity} when multiple sensors exist on a device.
 */
public class MultiBiometricEnrollHelper {

    private static final String TAG = "MultiBiometricEnrollHelper";

    private static final int REQUEST_FACE_ENROLL = 3000;
    private static final int REQUEST_FINGERPRINT_ENROLL = 3001;

    public static final String EXTRA_ENROLL_AFTER_FACE = "enroll_after_face";
    public static final String EXTRA_ENROLL_AFTER_FINGERPRINT = "enroll_after_finger";
    public static final String EXTRA_SKIP_PENDING_ENROLL = "skip_pending_enroll";

    @NonNull private final FragmentActivity mActivity;
    private final long mGkPwHandle;
    private final int mUserId;
    private final boolean mRequestEnrollFace;
    private final boolean mRequestEnrollFingerprint;
    private final FingerprintManager mFingerprintManager;
    private final FaceManager mFaceManager;
    private final Intent mFingerprintEnrollIntroductionIntent;
    private final Intent mFaceEnrollIntroductionIntent;
    private Function<Long, byte[]> mGatekeeperHatSupplier;

    @VisibleForTesting
    MultiBiometricEnrollHelper(@NonNull FragmentActivity activity, int userId,
            boolean enrollFace, boolean enrollFingerprint, long gkPwHandle,
            FingerprintManager fingerprintManager,
            FaceManager faceManager, Intent fingerprintEnrollIntroductionIntent,
            Intent faceEnrollIntroductionIntent, Function<Long, byte[]> gatekeeperHatSupplier) {
        mActivity = activity;
        mUserId = userId;
        mGkPwHandle = gkPwHandle;
        mRequestEnrollFace = enrollFace;
        mRequestEnrollFingerprint = enrollFingerprint;
        mFingerprintManager = fingerprintManager;
        mFaceManager = faceManager;
        mFingerprintEnrollIntroductionIntent = fingerprintEnrollIntroductionIntent;
        mFaceEnrollIntroductionIntent = faceEnrollIntroductionIntent;
        mGatekeeperHatSupplier = gatekeeperHatSupplier;
    }

    MultiBiometricEnrollHelper(@NonNull FragmentActivity activity, int userId,
            boolean enrollFace, boolean enrollFingerprint, long gkPwHandle) {
        this(activity, userId, enrollFace, enrollFingerprint, gkPwHandle,
                activity.getSystemService(FingerprintManager.class),
                activity.getSystemService(FaceManager.class),
                BiometricUtils.getFingerprintIntroIntent(activity, activity.getIntent()),
                BiometricUtils.getFaceIntroIntent(activity, activity.getIntent()),
                (challenge) ->  BiometricUtils.requestGatekeeperHat(activity, gkPwHandle,
                        userId, challenge));
    }

    void startNextStep() {
        if (mRequestEnrollFingerprint) {
            launchFingerprintEnroll();
        } else if (mRequestEnrollFace) {
            launchFaceEnroll();
        } else {
            mActivity.setResult(BiometricEnrollIntroduction.RESULT_SKIP);
            mActivity.finish();
        }
    }

    private void launchFaceEnroll() {
        mFaceManager.generateChallenge(mUserId, (sensorId, userId, challenge) -> {
            final byte[] hardwareAuthToken = mGatekeeperHatSupplier.apply(challenge);
            mFaceEnrollIntroductionIntent.putExtra(
                    BiometricEnrollBase.EXTRA_KEY_SENSOR_ID, sensorId);
            mFaceEnrollIntroductionIntent.putExtra(
                    BiometricEnrollBase.EXTRA_KEY_CHALLENGE, challenge);
            BiometricUtils.launchEnrollForResult(mActivity, mFaceEnrollIntroductionIntent,
                    REQUEST_FACE_ENROLL, hardwareAuthToken, mGkPwHandle, mUserId);
        });
    }

    private void launchFingerprintEnroll() {
        mFingerprintManager.generateChallenge(mUserId, ((sensorId, userId, challenge) -> {
            final byte[] hardwareAuthToken = mGatekeeperHatSupplier.apply(challenge);
            mFingerprintEnrollIntroductionIntent.putExtra(
                    BiometricEnrollBase.EXTRA_KEY_SENSOR_ID, sensorId);
            mFingerprintEnrollIntroductionIntent.putExtra(
                    BiometricEnrollBase.EXTRA_KEY_CHALLENGE, challenge);
            if (mRequestEnrollFace) {
                // Give FingerprintEnroll a pendingIntent pointing to face enrollment, so that it
                // can be started when user skips or finishes fingerprint enrollment.
                // FLAG_UPDATE_CURRENT ensures it is launched with the most recent values.
                mFaceEnrollIntroductionIntent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                mFaceEnrollIntroductionIntent.putExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, mGkPwHandle);
                final PendingIntent faceAfterFp = PendingIntent.getActivity(mActivity,
                        0 /* requestCode */, mFaceEnrollIntroductionIntent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                mFingerprintEnrollIntroductionIntent.putExtra(EXTRA_ENROLL_AFTER_FINGERPRINT,
                        faceAfterFp);
            }
            BiometricUtils.launchEnrollForResult(mActivity, mFingerprintEnrollIntroductionIntent,
                    REQUEST_FINGERPRINT_ENROLL, hardwareAuthToken, mGkPwHandle, mUserId);
        }));
    }
}

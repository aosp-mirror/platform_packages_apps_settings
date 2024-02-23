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
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintEnrollOptions;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;

import androidx.annotation.Nullable;

import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.safetycenter.BiometricsSafetySource;

/**
 * Responsible for making {@link FingerprintManager#enroll} and {@link FingerprintManager#remove}
 * calls and thus updating the fingerprint setting.
 */
public class FingerprintUpdater {

    private final Context mContext;
    private final FingerprintManager mFingerprintManager;

    public FingerprintUpdater(Context context) {
        mContext = context;
        mFingerprintManager = Utils.getFingerprintManagerOrNull(context);
    }

    public FingerprintUpdater(Context context, FingerprintManager fingerprintManager) {
        mContext = context;
        mFingerprintManager = fingerprintManager;
    }

    /** Wrapper around the {@link FingerprintManager#enroll} method. */
    public void enroll(byte [] hardwareAuthToken, CancellationSignal cancel, int userId,
            FingerprintManager.EnrollmentCallback callback,
            @FingerprintManager.EnrollReason int enrollReason, Intent intent) {
        mFingerprintManager.enroll(hardwareAuthToken, cancel, userId,
                new NotifyingEnrollmentCallback(mContext, callback), enrollReason,
                toFingerprintEnrollOptions(intent));
    }

    /** Wrapper around the {@link FingerprintManager#remove} method. */
    public void remove(Fingerprint fp, int userId, FingerprintManager.RemovalCallback callback) {
        mFingerprintManager.remove(fp, userId, new NotifyingRemovalCallback(mContext, callback));
    }

    /**
     * Decorator of the {@link FingerprintManager.EnrollmentCallback} class that notifies other
     * interested parties that a fingerprint setting has changed.
     */
    private static class NotifyingEnrollmentCallback
            extends FingerprintManager.EnrollmentCallback {

        private final Context mContext;
        private final FingerprintManager.EnrollmentCallback mCallback;

        NotifyingEnrollmentCallback(Context context,
                FingerprintManager.EnrollmentCallback callback) {
            mContext = context;
            mCallback = callback;
        }

        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            mCallback.onEnrollmentError(errMsgId, errString);
        }

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            mCallback.onEnrollmentHelp(helpMsgId, helpString);
        }

        @Override
        public void onEnrollmentProgress(int remaining) {
            mCallback.onEnrollmentProgress(remaining);
            if (remaining == 0) {
                BiometricsSafetySource.onBiometricsChanged(mContext); // biometrics data changed
            }
        }

        @Override
        public void onAcquired(boolean isAcquiredGood) {
            mCallback.onAcquired(isAcquiredGood);
        }

        @Override
        public void onUdfpsPointerDown(int sensorId) {
            mCallback.onUdfpsPointerDown(sensorId);
        }

        @Override
        public void onUdfpsPointerUp(int sensorId) {
            mCallback.onUdfpsPointerUp(sensorId);
        }

        @Override
        public void onUdfpsOverlayShown() {
            mCallback.onUdfpsOverlayShown();
        }
    }

    /**
     * Decorator of the {@link FingerprintManager.RemovalCallback} class that notifies other
     * interested parties that a fingerprint setting has changed.
     */
    private static class NotifyingRemovalCallback extends FingerprintManager.RemovalCallback {

        private final Context mContext;
        private final FingerprintManager.RemovalCallback mCallback;

        NotifyingRemovalCallback(Context context, FingerprintManager.RemovalCallback callback) {
            mContext = context;
            mCallback = callback;
        }

        @Override
        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
            mCallback.onRemovalError(fp, errMsgId, errString);
        }

        @Override
        public void onRemovalSucceeded(@Nullable Fingerprint fp, int remaining) {
            mCallback.onRemovalSucceeded(fp, remaining);
            BiometricsSafetySource.onBiometricsChanged(mContext); // biometrics data changed
        }
    }

    private FingerprintEnrollOptions toFingerprintEnrollOptions(Intent intent) {
        final int reason = intent.getIntExtra(BiometricUtils.EXTRA_ENROLL_REASON, -1);
        final FingerprintEnrollOptions.Builder builder = new FingerprintEnrollOptions.Builder();
        builder.setEnrollReason(FingerprintEnrollOptions.ENROLL_REASON_UNKNOWN);
        if (reason != -1) {
            builder.setEnrollReason(reason);
        }
        return builder.build();
    }
}

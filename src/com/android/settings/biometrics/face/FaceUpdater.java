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

package com.android.settings.biometrics.face;

import android.content.Context;
import android.content.Intent;
import android.hardware.face.Face;
import android.hardware.face.FaceEnrollCell;
import android.hardware.face.FaceEnrollOptions;
import android.hardware.face.FaceManager;
import android.os.CancellationSignal;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.safetycenter.BiometricsSafetySource;

/**
 * Responsible for making {@link FaceManager#enroll} and {@link FaceManager#remove} calls and thus
 * updating the face setting.
 */
public class FaceUpdater {

    private final Context mContext;
    private final FaceManager mFaceManager;

    public FaceUpdater(Context context) {
        mContext = context;
        mFaceManager = Utils.getFaceManagerOrNull(context);
    }

    public FaceUpdater(Context context, FaceManager faceManager) {
        mContext = context;
        mFaceManager = faceManager;
    }

    /** Wrapper around the {@link FaceManager#enroll} method. */
    public void enroll(int userId, byte[] hardwareAuthToken, CancellationSignal cancel,
            FaceManager.EnrollmentCallback callback, int[] disabledFeatures, Intent intent) {
        this.enroll(userId, hardwareAuthToken, cancel,
                new NotifyingEnrollmentCallback(mContext, callback), disabledFeatures,
                null, false, intent);
    }

    /** Wrapper around the {@link FaceManager#enroll} method. */
    public void enroll(int userId, byte[] hardwareAuthToken, CancellationSignal cancel,
            FaceManager.EnrollmentCallback callback, int[] disabledFeatures,
            @Nullable Surface previewSurface, boolean debugConsent, Intent intent) {
        mFaceManager.enroll(userId, hardwareAuthToken, cancel,
                new NotifyingEnrollmentCallback(mContext, callback), disabledFeatures,
                previewSurface, debugConsent, toFaceEnrollOptions(intent));
    }

    /** Wrapper around the {@link FaceManager#remove} method. */
    public void remove(Face face, int userId, FaceManager.RemovalCallback callback) {
        mFaceManager.remove(face, userId, new NotifyingRemovalCallback(mContext, callback));
    }

    /**
     * Decorator of the {@link FaceManager.EnrollmentCallback} class that notifies other
     * interested parties that a face setting has changed.
     */
    private static class NotifyingEnrollmentCallback
            extends FaceManager.EnrollmentCallback {

        private final Context mContext;
        private final FaceManager.EnrollmentCallback mCallback;

        NotifyingEnrollmentCallback(Context context,
                FaceManager.EnrollmentCallback callback) {
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
        public void onEnrollmentFrame(int helpCode, @Nullable CharSequence helpMessage,
                @Nullable FaceEnrollCell cell, int stage, float pan, float tilt, float distance) {
            mCallback.onEnrollmentFrame(helpCode, helpMessage, cell, stage, pan, tilt, distance);
        }

        @Override
        public void onEnrollmentProgress(int remaining) {
            mCallback.onEnrollmentProgress(remaining);
            if (remaining == 0) {
                BiometricsSafetySource.onBiometricsChanged(mContext); // biometrics data changed
            }
        }
    }

    /**
     * Decorator of the {@link FaceManager.RemovalCallback} class that notifies other
     * interested parties that a face setting has changed.
     */
    private static class NotifyingRemovalCallback extends FaceManager.RemovalCallback {

        private final Context mContext;
        private final FaceManager.RemovalCallback mCallback;

        NotifyingRemovalCallback(Context context, FaceManager.RemovalCallback callback) {
            mContext = context;
            mCallback = callback;
        }

        @Override
        public void onRemovalError(Face fp, int errMsgId, CharSequence errString) {
            mCallback.onRemovalError(fp, errMsgId, errString);
        }

        @Override
        public void onRemovalSucceeded(@Nullable Face fp, int remaining) {
            mCallback.onRemovalSucceeded(fp, remaining);
            BiometricsSafetySource.onBiometricsChanged(mContext); // biometrics data changed
        }
    }

    private FaceEnrollOptions toFaceEnrollOptions(Intent intent) {
        final int reason = intent.getIntExtra(BiometricUtils.EXTRA_ENROLL_REASON, -1);
        final FaceEnrollOptions.Builder builder = new FaceEnrollOptions.Builder();
        builder.setEnrollReason(FaceEnrollOptions.ENROLL_REASON_UNKNOWN);
        if (reason != -1) {
            builder.setEnrollReason(reason);
        }
        return builder.build();
    }
}

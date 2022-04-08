/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import android.app.settings.SettingsEnums;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.os.CancellationSignal;

import com.android.settings.core.InstrumentedFragment;

/**
 * Sidecar fragment to handle the state around fingerprint authentication
 */
public class FingerprintAuthenticateSidecar extends InstrumentedFragment {

    private static final String TAG = "FingerprintAuthenticateSidecar";

    private FingerprintManager mFingerprintManager;
    private Listener mListener;
    private AuthenticationResult mAuthenticationResult;
    private CancellationSignal mCancellationSignal;
    private AuthenticationError mAuthenticationError;

    public interface Listener {
        void onAuthenticationSucceeded(AuthenticationResult result);
        void onAuthenticationFailed();
        void onAuthenticationError(int errMsgId, CharSequence errString);
        void onAuthenticationHelp(int helpMsgId, CharSequence helpString);
    }

    private class AuthenticationError {
        int error;
        CharSequence errorString;

        public AuthenticationError(int errMsgId, CharSequence errString) {
            error = errMsgId;
            errorString = errString;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT_AUTHENTICATE_SIDECAR;
    }

    private FingerprintManager.AuthenticationCallback mAuthenticationCallback =
            new FingerprintManager.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(AuthenticationResult result) {
                    mCancellationSignal = null;
                    if (mListener != null) {
                        mListener.onAuthenticationSucceeded(result);
                    } else {
                        mAuthenticationResult = result;
                        mAuthenticationError = null;
                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    if (mListener != null) {
                        mListener.onAuthenticationFailed();
                    }
                }

                @Override
                public void onAuthenticationError(int errMsgId, CharSequence errString) {
                    mCancellationSignal = null;
                    if (mListener != null) {
                        mListener.onAuthenticationError(errMsgId, errString);
                    } else {
                        mAuthenticationError = new AuthenticationError(errMsgId, errString);
                        mAuthenticationResult = null;
                    }
                }

                @Override
                public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                    if (mListener != null) {
                        mListener.onAuthenticationHelp(helpMsgId, helpString);
                    }
                }
    };

    public void setFingerprintManager(FingerprintManager fingerprintManager) {
        mFingerprintManager = fingerprintManager;
    }

    public void startAuthentication(int userId) {
        mCancellationSignal = new CancellationSignal();
        mFingerprintManager.authenticate(null, mCancellationSignal, 0 /* flags */,
                mAuthenticationCallback, null, userId);
    }

    public void stopAuthentication() {
        if (mCancellationSignal != null && !mCancellationSignal.isCanceled()) {
            mCancellationSignal.cancel();
        }
        mCancellationSignal = null;
    }

    public void setListener(Listener listener) {
        if (mListener == null && listener != null) {
            if (mAuthenticationResult != null) {
                listener.onAuthenticationSucceeded(mAuthenticationResult);
                mAuthenticationResult = null;
            }
            if (mAuthenticationError != null &&
                    mAuthenticationError.error != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                listener.onAuthenticationError(mAuthenticationError.error,
                        mAuthenticationError.errorString);
                mAuthenticationError = null;
            }
        }
        mListener = listener;
    }
}
/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.fingerprint;

import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

/**
 * Small helper class to manage text/icon around fingerprint authentication UI.
 */
public class FingerprintUiHelper extends FingerprintManager.AuthenticationCallback {

    private static final long ERROR_TIMEOUT = 1300;

    private ImageView mIcon;
    private TextView mErrorTextView;
    private CancellationSignal mCancellationSignal;
    private int mUserId;

    private Callback mCallback;
    private FingerprintManager mFingerprintManager;

    public FingerprintUiHelper(ImageView icon, TextView errorTextView, Callback callback,
            int userId) {
        mFingerprintManager = icon.getContext().getSystemService(FingerprintManager.class);
        mIcon = icon;
        mErrorTextView = errorTextView;
        mCallback = callback;
        mUserId = userId;
    }

    public void startListening() {
        if (mFingerprintManager != null && mFingerprintManager.isHardwareDetected()
                && mFingerprintManager.getEnrolledFingerprints(mUserId).size() > 0) {
            mCancellationSignal = new CancellationSignal();
            mFingerprintManager.setActiveUser(mUserId);
            mFingerprintManager.authenticate(
                    null, mCancellationSignal, 0 /* flags */, this, null, mUserId);
            setFingerprintIconVisibility(true);
            mIcon.setImageResource(R.drawable.ic_fingerprint);
        }
    }

    public void stopListening() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    public boolean isListening() {
        return mCancellationSignal != null && !mCancellationSignal.isCanceled();
    }

    private void setFingerprintIconVisibility(boolean visible) {
        mIcon.setVisibility(visible ? View.VISIBLE : View.GONE);
        mCallback.onFingerprintIconVisibilityChanged(visible);
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        showError(errString);
        setFingerprintIconVisibility(false);
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        showError(helpString);
    }

    @Override
    public void onAuthenticationFailed() {
        showError(mIcon.getResources().getString(
                R.string.fingerprint_not_recognized));
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        mIcon.setImageResource(R.drawable.ic_fingerprint_success);
        mCallback.onAuthenticated();
    }

    private void showError(CharSequence error) {
        if (!isListening()) {
            return;
        }

        mIcon.setImageResource(R.drawable.ic_fingerprint_error);
        mErrorTextView.setText(error);
        mErrorTextView.removeCallbacks(mResetErrorTextRunnable);
        mErrorTextView.postDelayed(mResetErrorTextRunnable, ERROR_TIMEOUT);
    }

    private Runnable mResetErrorTextRunnable = new Runnable() {
        @Override
        public void run() {
            mErrorTextView.setText("");
            mIcon.setImageResource(R.drawable.ic_fingerprint);
        }
    };

    public interface Callback {
        void onAuthenticated();
        void onFingerprintIconVisibilityChanged(boolean visible);
    }
}

/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.password;

import android.app.RemoteLockscreenValidationResult;
import android.os.Bundle;
import android.os.Handler;
import android.service.remotelockscreenvalidation.IRemoteLockscreenValidationCallback;
import android.service.remotelockscreenvalidation.RemoteLockscreenValidationClient;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.security.SecureBox;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

/**
 * A fragment used to hold state for remote lockscreen validation.
 * If the original listener is ever re-created, the new listener must be set again using
 * {@link #setListener} so that the validation result does not get handled by the old listener.
 */
public class RemoteLockscreenValidationFragment extends Fragment {

    private static final String TAG = RemoteLockscreenValidationFragment.class.getSimpleName();

    private Listener mListener;
    private Handler mHandler;
    private boolean mIsInProgress;
    private RemoteLockscreenValidationResult mResult;
    private String mErrorMessage;
    private LockscreenCredential mLockscreenCredential;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        clearLockscreenCredential();
        if (mResult != null && mErrorMessage != null) {
            Log.w(TAG, "Unprocessed remote lockscreen validation result");
        }
        super.onDestroy();
    }

    /**
     * @return {@code true} if remote lockscreen guess validation has started or
     * the validation result has not yet been handled.
     */
    public boolean isRemoteValidationInProgress() {
        return mIsInProgress;
    }

    /**
     * Sets the listener and handler that will handle the result of remote lockscreen validation.
     * Unprocessed results or failures will be handled after the listener is set.
     */
    public void setListener(Listener listener, Handler handler) {
        if (mListener == listener) {
            return;
        }

        mListener = listener;
        mHandler = handler;

        if (mResult != null) {
            handleResult();
        } else if (mErrorMessage != null) {
            handleFailure();
        }
    }

    /**
     * @return {@link LockscreenCredential} if it was cached in {@link #validateLockscreenGuess}.
     */
    public LockscreenCredential getLockscreenCredential() {
        return mLockscreenCredential;
    }

    /**
     * Clears the {@link LockscreenCredential} if it was cached in {@link #validateLockscreenGuess}.
     */
    public void clearLockscreenCredential() {
        if (mLockscreenCredential != null) {
            mLockscreenCredential.zeroize();
            mLockscreenCredential = null;
        }
    }

    /**
     * Validates the lockscreen guess on the remote device.
     * @param remoteLockscreenValidationClient the client that should be used to send the guess to
     *                                         for validation
     * @param guess the {@link LockscreenCredential} guess that the user entered
     * @param encryptionKey the key that should be used to encrypt the guess before validation
     * @param shouldCacheGuess whether to cache to guess so it can be used to set the current
     *                         device's lockscreen after validation succeeds.
     */
    public void validateLockscreenGuess(
            RemoteLockscreenValidationClient remoteLockscreenValidationClient,
            LockscreenCredential guess, byte[] encryptionKey, boolean shouldCacheGuess) {
        if (shouldCacheGuess) {
            mLockscreenCredential = guess;
        }

        remoteLockscreenValidationClient.validateLockscreenGuess(
                encryptDeviceCredentialGuess(guess.getCredential(), encryptionKey),
                new IRemoteLockscreenValidationCallback.Stub() {
                    @Override
                    public void onSuccess(RemoteLockscreenValidationResult result) {
                        mResult = result;
                        handleResult();
                    }

                    @Override
                    public void onFailure(String message) {
                        mErrorMessage = message;
                        handleFailure();
                    }
                });
        mIsInProgress = true;
    }

    private byte[] encryptDeviceCredentialGuess(byte[] guess, byte[] encryptionKey) {
        try {
            PublicKey publicKey = SecureBox.decodePublicKey(encryptionKey);
            return SecureBox.encrypt(
                    publicKey,
                    /* sharedSecret= */ null,
                    LockPatternUtils.ENCRYPTED_REMOTE_CREDENTIALS_HEADER,
                    guess);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Log.w(TAG, "Error encrypting device credential guess. Returning empty byte[].", e);
            return new byte[0];
        }
    }

    private void handleResult() {
        if (mHandler != null) {
            mHandler.post(()-> {
                if (mListener == null || mResult == null) {
                    return;
                }
                mIsInProgress = false;
                mListener.onRemoteLockscreenValidationResult(mResult);
                mResult = null;
            });
        }
    }

    private void handleFailure() {
        if (mHandler != null) {
            mHandler.post(()-> {
                if (mListener == null || mErrorMessage == null) {
                    return;
                }
                mIsInProgress = false;
                mListener.onRemoteLockscreenValidationFailure(
                        String.format("Remote lockscreen validation failed: %s", mErrorMessage));
                mErrorMessage = null;
            });
        }
    }

    interface Listener {
        void onRemoteLockscreenValidationResult(RemoteLockscreenValidationResult result);
        void onRemoteLockscreenValidationFailure(String message);
    }
}

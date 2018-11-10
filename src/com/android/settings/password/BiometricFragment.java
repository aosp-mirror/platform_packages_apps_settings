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
 * limitations under the License.
 */

package com.android.settings.password;

import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricConstants;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback;
import android.hardware.biometrics.BiometricPrompt.AuthenticationResult;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import com.android.settings.core.InstrumentedFragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Executor;

/**
 * A fragment that wraps the BiometricPrompt and manages its lifecycle.
 */
public class BiometricFragment extends InstrumentedFragment {

    private static final String KEY_TITLE = "title";
    private static final String KEY_SUBTITLE = "subtitle";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_NEGATIVE_TEXT = "negative_text";

    // Re-set by the application. Should be done upon orientation changes, etc
    private Executor mClientExecutor;
    private AuthenticationCallback mClientCallback;

    // Re-settable by the application.
    private int mUserId;

    // Created/Initialized once and retained
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private PromptInfo mPromptInfo;
    private BiometricPrompt mBiometricPrompt;
    private CancellationSignal mCancellationSignal;

    private AuthenticationCallback mAuthenticationCallback =
            new AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int error, @NonNull CharSequence message) {
            mClientExecutor.execute(() -> {
                mClientCallback.onAuthenticationError(error, message);
            });
            cleanup();
        }

        @Override
        public void onAuthenticationSucceeded(AuthenticationResult result) {
            mClientExecutor.execute(() -> {
                mClientCallback.onAuthenticationSucceeded(result);
            });
            cleanup();
        }
    };

    private final DialogInterface.OnClickListener mNegativeButtonListener =
            new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mAuthenticationCallback.onAuthenticationError(
                    BiometricConstants.BIOMETRIC_ERROR_NEGATIVE_BUTTON,
                    mPromptInfo.getNegativeButtonText());
        }
    };

    public static BiometricFragment newInstance(PromptInfo info) {
        BiometricFragment biometricFragment = new BiometricFragment();
        biometricFragment.setArguments(info.getBundle());
        return biometricFragment;
    }

    public void setCallbacks(Executor executor, AuthenticationCallback callback) {
        mClientExecutor = executor;
        mClientCallback = callback;
    }

    public void setUser(int userId) {
        mUserId = userId;
    }

    public void cancel() {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
        }
        cleanup();
    }

    private void cleanup() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mPromptInfo = new PromptInfo(getArguments());
        mBiometricPrompt = new BiometricPrompt.Builder(getContext())
            .setTitle(mPromptInfo.getTitle())
            .setUseDefaultTitle() // use default title if title is null/empty
            .setSubtitle(mPromptInfo.getSubtitle())
            .setDescription(mPromptInfo.getDescription())
            .setNegativeButton(mPromptInfo.getNegativeButtonText(), mClientExecutor,
                    mNegativeButtonListener)
            .build();
        mCancellationSignal = new CancellationSignal();

        // TODO: CC doesn't use crypto for now
        mBiometricPrompt.authenticateUser(mCancellationSignal, mClientExecutor,
                mAuthenticationCallback, mUserId);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BIOMETRIC_FRAGMENT;
    }

    /**
     * A simple wrapper for BiometricPrompt.PromptInfo. Since we want to manage the lifecycle
     * of BiometricPrompt correctly, the information needs to be stored in here.
     */
    static class PromptInfo {
        private final Bundle mBundle;

        private PromptInfo(Bundle bundle) {
            mBundle = bundle;
        }

        Bundle getBundle() {
            return mBundle;
        }

        public CharSequence getTitle() {
            return mBundle.getCharSequence(KEY_TITLE);
        }

        public CharSequence getSubtitle() {
            return mBundle.getCharSequence(KEY_SUBTITLE);
        }

        public CharSequence getDescription() {
            return mBundle.getCharSequence(KEY_DESCRIPTION);
        }

        public CharSequence getNegativeButtonText() {
            return mBundle.getCharSequence(KEY_NEGATIVE_TEXT);
        }

        public static class Builder {
            private final Bundle mBundle = new Bundle();

            public Builder setTitle(@NonNull CharSequence title) {
                mBundle.putCharSequence(KEY_TITLE, title);
                return this;
            }

            public Builder setSubtitle(@Nullable CharSequence subtitle) {
                mBundle.putCharSequence(KEY_SUBTITLE, subtitle);
                return this;
            }

            public Builder setDescription(@Nullable CharSequence description) {
                mBundle.putCharSequence(KEY_DESCRIPTION, description);
                return this;
            }

            public Builder setNegativeButtonText(@NonNull CharSequence text) {
                mBundle.putCharSequence(KEY_NEGATIVE_TEXT, text);
                return this;
            }

            public PromptInfo build() {
                return new PromptInfo(mBundle);
            }
        }
    }
}


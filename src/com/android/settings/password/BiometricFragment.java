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
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback;
import android.hardware.biometrics.BiometricPrompt.AuthenticationResult;
import android.hardware.biometrics.PromptInfo;
import android.os.Bundle;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;

import com.android.settings.core.InstrumentedFragment;

import java.util.concurrent.Executor;

/**
 * A fragment that wraps the BiometricPrompt and manages its lifecycle.
 */
public class BiometricFragment extends InstrumentedFragment {

    private static final String TAG = "ConfirmDeviceCredential/BiometricFragment";

    private static final String KEY_PROMPT_INFO = "prompt_info";

    // Re-set by the application. Should be done upon orientation changes, etc
    private Executor mClientExecutor;
    private AuthenticationCallback mClientCallback;

    // Re-settable by the application.
    private int mUserId;

    // Created/Initialized once and retained
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

        @Override
        public void onAuthenticationFailed() {
            mClientExecutor.execute(() -> {
                mClientCallback.onAuthenticationFailed();
            });
        }

        @Override
        public void onSystemEvent(int event) {
            mClientExecutor.execute(() -> {
                mClientCallback.onSystemEvent(event);
            });
        }
    };

    /**
     * @param promptInfo
     * @return
     */
    public static BiometricFragment newInstance(PromptInfo promptInfo) {
        BiometricFragment biometricFragment = new BiometricFragment();
        final Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_PROMPT_INFO, promptInfo);
        biometricFragment.setArguments(bundle);
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
            getActivity().getSupportFragmentManager().beginTransaction().remove(this)
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        final Bundle bundle = getArguments();
        final PromptInfo promptInfo = bundle.getParcelable(KEY_PROMPT_INFO);

        BiometricPrompt.Builder promptBuilder = new BiometricPrompt.Builder(getContext())
                .setTitle(promptInfo.getTitle())
                .setUseDefaultTitle() // use default title if title is null/empty
                .setDeviceCredentialAllowed(true)
                .setSubtitle(promptInfo.getSubtitle())
                .setDescription(promptInfo.getDescription())
                .setTextForDeviceCredential(
                        promptInfo.getDeviceCredentialTitle(),
                        promptInfo.getDeviceCredentialSubtitle(),
                        promptInfo.getDeviceCredentialDescription())
                .setConfirmationRequired(promptInfo.isConfirmationRequested())
                .setDisallowBiometricsIfPolicyExists(
                        promptInfo.isDisallowBiometricsIfPolicyExists())
                .setShowEmergencyCallButton(promptInfo.isShowEmergencyCallButton())
                .setReceiveSystemEvents(true)
                .setAllowBackgroundAuthentication(true);

        // Check if the default subtitle should be used if subtitle is null/empty
        if (promptInfo.isUseDefaultSubtitle()) {
            promptBuilder.setUseDefaultSubtitle();
        }
        mBiometricPrompt = promptBuilder.build();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mCancellationSignal == null) {
            mCancellationSignal = new CancellationSignal();
            mBiometricPrompt.authenticateUser(mCancellationSignal, mClientExecutor,
                    mAuthenticationCallback, mUserId);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BIOMETRIC_FRAGMENT;
    }
}

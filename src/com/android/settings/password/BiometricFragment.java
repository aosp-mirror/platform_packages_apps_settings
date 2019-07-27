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

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricConstants;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback;
import android.hardware.biometrics.BiometricPrompt.AuthenticationResult;
import android.hardware.biometrics.IBiometricConfirmDeviceCredentialCallback;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.overlay.FeatureFactory;

import java.util.concurrent.Executor;

/**
 * A fragment that wraps the BiometricPrompt and manages its lifecycle.
 */
public class BiometricFragment extends InstrumentedFragment {

    private static final String TAG = "ConfirmDeviceCredential/BiometricFragment";

    // Re-set by the application. Should be done upon orientation changes, etc
    private Executor mClientExecutor;
    private AuthenticationCallback mClientCallback;

    // Re-settable by the application.
    private int mUserId;

    // Created/Initialized once and retained
    private Bundle mBundle;
    private BiometricPrompt mBiometricPrompt;
    private CancellationSignal mCancellationSignal;
    private boolean mAuthenticating;

    private AuthenticationCallback mAuthenticationCallback =
            new AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int error, @NonNull CharSequence message) {
            mAuthenticating = false;
            mClientExecutor.execute(() -> {
                mClientCallback.onAuthenticationError(error, message);
            });
            cleanup();
        }

        @Override
        public void onAuthenticationSucceeded(AuthenticationResult result) {
            mAuthenticating = false;
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
                    mBundle.getString(BiometricPrompt.KEY_NEGATIVE_TEXT));
        }
    };

    // TODO(b/123378871): Remove when moved.
    private final IBiometricConfirmDeviceCredentialCallback mCancelCallback
        = new IBiometricConfirmDeviceCredentialCallback.Stub() {
        @Override
        public void cancel() {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            } else {
                Log.e(TAG, "Activity null!");
            }
        }
    };

    /**
     * @param bundle Bundle passed from {@link BiometricPrompt.Builder#buildIntent()}
     * @return
     */
    public static BiometricFragment newInstance(Bundle bundle) {
        BiometricFragment biometricFragment = new BiometricFragment();
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
            getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
        }
    }

    boolean isAuthenticating() {
        return mAuthenticating;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mBundle = getArguments();
        final BiometricPrompt.Builder builder = new BiometricPrompt.Builder(getContext())
                .setTitle(mBundle.getString(BiometricPrompt.KEY_TITLE))
                .setUseDefaultTitle() // use default title if title is null/empty
                .setFromConfirmDeviceCredential()
                .setSubtitle(mBundle.getString(BiometricPrompt.KEY_SUBTITLE))
                .setDescription(mBundle.getString(BiometricPrompt.KEY_DESCRIPTION))
                .setConfirmationRequired(
                        mBundle.getBoolean(BiometricPrompt.KEY_REQUIRE_CONFIRMATION, true));

        final LockPatternUtils lockPatternUtils = FeatureFactory.getFactory(
                getContext())
                .getSecurityFeatureProvider()
                .getLockPatternUtils(getContext());

        switch (lockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                builder.setNegativeButton(getResources().getString(
                        R.string.confirm_device_credential_pattern),
                        mClientExecutor, mNegativeButtonListener);
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                builder.setNegativeButton(getResources().getString(
                        R.string.confirm_device_credential_pin),
                        mClientExecutor, mNegativeButtonListener);
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                builder.setNegativeButton(getResources().getString(
                        R.string.confirm_device_credential_password),
                        mClientExecutor, mNegativeButtonListener);
                break;
        }

        mBiometricPrompt = builder.build();
        mCancellationSignal = new CancellationSignal();

        // TODO: CC doesn't use crypto for now
        mAuthenticating = true;
        mBiometricPrompt.authenticateUser(mCancellationSignal, mClientExecutor,
                mAuthenticationCallback, mUserId, mCancelCallback);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BIOMETRIC_FRAGMENT;
    }
}


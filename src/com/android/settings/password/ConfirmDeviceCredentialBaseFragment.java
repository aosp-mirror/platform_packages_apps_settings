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

// TODO (b/35202196): move this class out of the root of the package.
package com.android.settings.password;

import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LOCK_ATTEMPTS_FAILED;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.annotation.Nullable;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.RemoteLockscreenValidationResult;
import android.app.StartLockscreenValidationRequest;
import android.app.admin.DevicePolicyManager;
import android.app.admin.ManagedSubscriptionsPolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.hardware.biometrics.BiometricManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.remotelockscreenvalidation.IRemoteLockscreenValidationCallback;
import android.service.remotelockscreenvalidation.RemoteLockscreenValidationClient;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.security.SecureBox;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

/**
 * Base fragment to be shared for PIN/Pattern/Password confirmation fragments.
 */
public abstract class ConfirmDeviceCredentialBaseFragment extends InstrumentedFragment {
    public static final String TAG = ConfirmDeviceCredentialBaseFragment.class.getSimpleName();
    public static final String TITLE_TEXT = SETTINGS_PACKAGE_NAME + ".ConfirmCredentials.title";
    public static final String HEADER_TEXT = SETTINGS_PACKAGE_NAME + ".ConfirmCredentials.header";
    public static final String DETAILS_TEXT = SETTINGS_PACKAGE_NAME + ".ConfirmCredentials.details";
    public static final String DARK_THEME = SETTINGS_PACKAGE_NAME + ".ConfirmCredentials.darkTheme";
    public static final String SHOW_CANCEL_BUTTON =
            SETTINGS_PACKAGE_NAME + ".ConfirmCredentials.showCancelButton";
    public static final String SHOW_WHEN_LOCKED =
            SETTINGS_PACKAGE_NAME + ".ConfirmCredentials.showWhenLocked";
    public static final String USE_FADE_ANIMATION =
            SETTINGS_PACKAGE_NAME + ".ConfirmCredentials.useFadeAnimation";
    public static final String IS_REMOTE_LOCKSCREEN_VALIDATION =
            SETTINGS_PACKAGE_NAME + ".ConfirmCredentials.isRemoteLockscreenValidation";

    protected static final int USER_TYPE_PRIMARY = 1;
    protected static final int USER_TYPE_MANAGED_PROFILE = 2;
    protected static final int USER_TYPE_SECONDARY = 3;

    /** Time we wait before clearing a wrong input attempt (e.g. pattern) and the error message. */
    protected static final long CLEAR_WRONG_ATTEMPT_TIMEOUT_MS = 3000;

    protected boolean mReturnCredentials = false;
    protected boolean mReturnGatekeeperPassword = false;
    protected boolean mForceVerifyPath = false;
    protected CheckBox mCheckBox;
    protected Button mCancelButton;
    /** Button allowing managed profile password reset, null when is not shown. */
    @Nullable protected Button mForgotButton;
    protected int mEffectiveUserId;
    protected int mUserId;
    protected UserManager mUserManager;
    protected LockPatternUtils mLockPatternUtils;
    protected DevicePolicyManager mDevicePolicyManager;
    protected TextView mErrorTextView;
    protected final Handler mHandler = new Handler();
    protected boolean mFrp;
    protected boolean mRemoteValidation;
    protected CharSequence mAlternateButtonText;
    protected BiometricManager mBiometricManager;
    @Nullable protected StartLockscreenValidationRequest mStartLockscreenValidationRequest;
    /** Credential saved so the credential can be set for device if remote validation passes */
    @Nullable protected LockscreenCredential mDeviceCredentialGuess;
    @Nullable protected RemoteLockscreenValidationClient mRemoteLockscreenValidationClient;

    private boolean isInternalActivity() {
        return (getActivity() instanceof ConfirmLockPassword.InternalActivity)
                || (getActivity() instanceof ConfirmLockPattern.InternalActivity);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getActivity().getIntent();
        mAlternateButtonText = intent.getCharSequenceExtra(
                KeyguardManager.EXTRA_ALTERNATE_BUTTON_LABEL);
        mReturnCredentials = intent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_RETURN_CREDENTIALS, false);

        mReturnGatekeeperPassword = intent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, false);
        mForceVerifyPath = intent.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_FORCE_VERIFY, false);

        if (intent.getBooleanExtra(IS_REMOTE_LOCKSCREEN_VALIDATION, false)) {
            if (FeatureFlagUtils.isEnabled(getContext(),
                    FeatureFlagUtils.SETTINGS_REMOTE_DEVICE_CREDENTIAL_VALIDATION)) {
                mRemoteValidation = true;
            } else {
                Log.e(TAG, "Remote device credential validation not enabled.");
                getActivity().finish();
            }
        }
        if (mRemoteValidation) {
            mStartLockscreenValidationRequest = intent.getParcelableExtra(
                    KeyguardManager.EXTRA_START_LOCKSCREEN_VALIDATION_REQUEST,
                    StartLockscreenValidationRequest.class);
            if (mStartLockscreenValidationRequest == null
                    || mStartLockscreenValidationRequest.getRemainingAttempts() == 0) {
                Log.e(TAG, "StartLockscreenValidationRequest is null or "
                        + "no more attempts for remote lockscreen validation.");
                getActivity().finish();
            }

            ComponentName remoteLockscreenValidationServiceComponent =
                    intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName.class);
            if (remoteLockscreenValidationServiceComponent == null) {
                Log.e(TAG, "RemoteLockscreenValidationService ComponentName is null");
                getActivity().finish();
            }
            mRemoteLockscreenValidationClient = RemoteLockscreenValidationClient
                    .create(getContext(), remoteLockscreenValidationServiceComponent);
            if (!mRemoteLockscreenValidationClient.isServiceAvailable()) {
                Log.e(TAG, String.format("RemoteLockscreenValidationService at %s is not available",
                        remoteLockscreenValidationServiceComponent.getClassName()));
                getActivity().finish();
            }
        }

        // Only take this argument into account if it belongs to the current profile.
        mUserId = Utils.getUserIdFromBundle(getActivity(), intent.getExtras(),
                isInternalActivity());
        mFrp = (mUserId == LockPatternUtils.USER_FRP);
        mUserManager = UserManager.get(getActivity());
        mEffectiveUserId = mUserManager.getCredentialOwnerProfile(mUserId);
        mLockPatternUtils = new LockPatternUtils(getActivity());
        mDevicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mBiometricManager = getActivity().getSystemService(BiometricManager.class);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCancelButton = view.findViewById(R.id.cancelButton);
        boolean showCancelButton = mRemoteValidation || getActivity().getIntent().getBooleanExtra(
                SHOW_CANCEL_BUTTON, false);
        boolean hasAlternateButton = (mFrp || mRemoteValidation) && !TextUtils.isEmpty(
                mAlternateButtonText);
        mCancelButton.setVisibility(showCancelButton || hasAlternateButton
                ? View.VISIBLE : View.GONE);
        if (hasAlternateButton) {
            mCancelButton.setText(mAlternateButtonText);
        }
        mCancelButton.setOnClickListener(v -> {
            if (hasAlternateButton) {
                getActivity().setResult(KeyguardManager.RESULT_ALTERNATE);
            }
            getActivity().finish();
        });
        setupForgotButtonIfManagedProfile(view);

        mCheckBox = view.findViewById(R.id.checkbox);
        if (mCheckBox != null && mRemoteValidation) {
            mCheckBox.setVisibility(View.VISIBLE);
        }
        setupEmergencyCallButtonIfManagedSubscription(view);
    }

    private void setupEmergencyCallButtonIfManagedSubscription(View view) {
        int policyType = getContext().getSystemService(
                DevicePolicyManager.class).getManagedSubscriptionsPolicy().getPolicyType();

        if (policyType == ManagedSubscriptionsPolicy.TYPE_ALL_MANAGED_SUBSCRIPTIONS) {
            Button emergencyCallButton = view.findViewById(R.id.emergencyCallButton);
            if (emergencyCallButton == null) {
                Log.wtf(TAG,
                        "Emergency call button not found in managed profile credential dialog");
                return;
            }
            emergencyCallButton.setVisibility(View.VISIBLE);
            emergencyCallButton.setOnClickListener(v -> {
                final Intent intent = getActivity()
                        .getSystemService(TelecomManager.class)
                        .createLaunchEmergencyDialerIntent(null)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getActivity().startActivity(intent);
                getActivity().finish();
            });
        }
    }

    private void setupForgotButtonIfManagedProfile(View view) {
        if (mUserManager.isManagedProfile(mUserId)
                && mUserManager.isQuietModeEnabled(UserHandle.of(mUserId))
                && mDevicePolicyManager.canProfileOwnerResetPasswordWhenLocked(mUserId)) {
            mForgotButton = view.findViewById(R.id.forgotButton);
            if (mForgotButton == null) {
                Log.wtf(TAG, "Forgot button not found in managed profile credential dialog");
                return;
            }
            mForgotButton.setVisibility(View.VISIBLE);
            mForgotButton.setOnClickListener(v -> {
                final Intent intent = new Intent();
                intent.setClassName(SETTINGS_PACKAGE_NAME, ForgotPasswordActivity.class.getName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                getActivity().startActivity(intent);
                getActivity().finish();
            });
        }
    }

    // User could be locked while Effective user is unlocked even though the effective owns the
    // credential. Otherwise, fingerprint can't unlock fbe/keystore through
    // verifyTiedProfileChallenge. In such case, we also wanna show the user message that
    // fingerprint is disabled due to device restart.
    protected boolean isStrongAuthRequired() {
        return mFrp
                || !mLockPatternUtils.isBiometricAllowedForUser(mEffectiveUserId)
                || !mUserManager.isUserUnlocked(mUserId);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshLockScreen();
    }

    protected void refreshLockScreen() {
        updateErrorMessage(mLockPatternUtils.getCurrentFailedPasswordAttempts(mEffectiveUserId));
    }

    protected void setAccessibilityTitle(CharSequence supplementalText) {
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            CharSequence titleText = intent.getCharSequenceExtra(
                    ConfirmDeviceCredentialBaseFragment.TITLE_TEXT);
            if (supplementalText == null) {
                return;
            }
            if (titleText == null) {
                getActivity().setTitle(supplementalText);
            } else {
                String accessibilityTitle =
                        new StringBuilder(titleText).append(",").append(supplementalText).toString();
                getActivity().setTitle(Utils.createAccessibleSequence(titleText, accessibilityTitle));
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mRemoteLockscreenValidationClient != null) {
            mRemoteLockscreenValidationClient.disconnect();
        }
        if (mDeviceCredentialGuess != null) {
            mDeviceCredentialGuess.zeroize();
        }
        super.onDestroy();
    }

    protected abstract void authenticationSucceeded();

    protected abstract void onRemoteDeviceCredentialValidationResult(
            RemoteLockscreenValidationResult result);

    public void prepareEnterAnimation() {
    }

    public void startEnterAnimation() {
    }

    protected void reportFailedAttempt() {
        updateErrorMessage(
                mLockPatternUtils.getCurrentFailedPasswordAttempts(mEffectiveUserId) + 1);
        mLockPatternUtils.reportFailedPasswordAttempt(mEffectiveUserId);
    }

    protected void updateErrorMessage(int numAttempts) {
        final int maxAttempts =
                mLockPatternUtils.getMaximumFailedPasswordsForWipe(mEffectiveUserId);
        if (maxAttempts <= 0 || numAttempts <= 0) {
            return;
        }

        // Update the on-screen error string
        if (mErrorTextView != null) {
            final String message = getActivity().getString(
                    R.string.lock_failed_attempts_before_wipe, numAttempts, maxAttempts);
            showError(message, 0);
        }

        // Only show popup dialog before the last attempt and before wipe
        final int remainingAttempts = maxAttempts - numAttempts;
        if (remainingAttempts > 1) {
            return;
        }
        final FragmentManager fragmentManager = getChildFragmentManager();
        final int userType = getUserTypeForWipe();
        if (remainingAttempts == 1) {
            // Last try
            final String title = getActivity().getString(
                    R.string.lock_last_attempt_before_wipe_warning_title);
            final String overrideMessageId = getLastTryOverrideErrorMessageId(userType);
            final int defaultMessageId = getLastTryDefaultErrorMessage(userType);
            final String message = mDevicePolicyManager.getResources().getString(
                    overrideMessageId, () -> getString(defaultMessageId));
            LastTryDialog.show(fragmentManager, title, message,
                    android.R.string.ok, false /* dismiss */);
        } else {
            // Device, profile, or secondary user is wiped
            final String message = getWipeMessage(userType);
            LastTryDialog.show(fragmentManager, null /* title */, message,
                    com.android.settingslib.R.string.failed_attempts_now_wiping_dialog_dismiss,
                    true /* dismiss */);
        }
    }

    private int getUserTypeForWipe() {
        final UserInfo userToBeWiped = mUserManager.getUserInfo(
                mDevicePolicyManager.getProfileWithMinimumFailedPasswordsForWipe(mEffectiveUserId));
        if (userToBeWiped == null || userToBeWiped.isPrimary()) {
            return USER_TYPE_PRIMARY;
        } else if (userToBeWiped.isManagedProfile()) {
            return USER_TYPE_MANAGED_PROFILE;
        } else {
            return USER_TYPE_SECONDARY;
        }
    }

    protected abstract String getLastTryOverrideErrorMessageId(int userType);
    protected abstract int getLastTryDefaultErrorMessage(int userType);

    private String getWipeMessage(int userType) {
        switch (userType) {
            case USER_TYPE_PRIMARY:
                return getString(com.android.settingslib
                        .R.string.failed_attempts_now_wiping_device);
            case USER_TYPE_MANAGED_PROFILE:
                return mDevicePolicyManager.getResources().getString(
                        WORK_PROFILE_LOCK_ATTEMPTS_FAILED,
                        () -> getString(
                          com.android.settingslib.R.string.failed_attempts_now_wiping_profile));
            case USER_TYPE_SECONDARY:
                return getString(com.android.settingslib.R.string.failed_attempts_now_wiping_user);
            default:
                throw new IllegalArgumentException("Unrecognized user type:" + userType);
        }
    }

    private final Runnable mResetErrorRunnable = new Runnable() {
        @Override
        public void run() {
            mErrorTextView.setText("");
        }
    };

    protected void showError(CharSequence msg, long timeout) {
        mErrorTextView.setText(msg);
        onShowError();
        mHandler.removeCallbacks(mResetErrorRunnable);
        if (timeout != 0) {
            mHandler.postDelayed(mResetErrorRunnable, timeout);
        }
    }

    protected void validateGuess(LockscreenCredential credentialGuess) {
        if (mCheckBox.isChecked()) {
            // Keep credential in memory since user wants to set guess as screen lock.
            mDeviceCredentialGuess = credentialGuess;
        } else if (mDeviceCredentialGuess != null) {
            mDeviceCredentialGuess.zeroize();
        }

        mRemoteLockscreenValidationClient.validateLockscreenGuess(
                encryptDeviceCredentialGuess(credentialGuess.getCredential()),
                new IRemoteLockscreenValidationCallback.Stub() {
                    @Override
                    public void onSuccess(RemoteLockscreenValidationResult result) {
                        mHandler.post(()->onRemoteDeviceCredentialValidationResult(result));
                    }

                    @Override
                    public void onFailure(String message) {
                        Log.e(TAG, "A failure occurred while trying "
                                + "to validate lockscreen guess: " + message);
                        mHandler.post(()->getActivity().finish());
                    }
                });
    }

    private byte[] encryptDeviceCredentialGuess(byte[] guess) {
        try {
            byte[] encodedPublicKey = mStartLockscreenValidationRequest.getSourcePublicKey();
            PublicKey publicKey = SecureBox.decodePublicKey(encodedPublicKey);
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

    protected abstract void onShowError();

    protected void showError(int msg, long timeout) {
        showError(getText(msg), timeout);
    }

    public static class LastTryDialog extends DialogFragment {
        private static final String TAG = LastTryDialog.class.getSimpleName();

        private static final String ARG_TITLE = "title";
        private static final String ARG_MESSAGE = "message";
        private static final String ARG_BUTTON = "button";
        private static final String ARG_DISMISS = "dismiss";

        static boolean show(FragmentManager from, String title, String message, int button,
                boolean dismiss) {
            LastTryDialog existent = (LastTryDialog) from.findFragmentByTag(TAG);
            if (existent != null && !existent.isRemoving()) {
                return false;
            }
            Bundle args = new Bundle();
            args.putString(ARG_TITLE, title);
            args.putString(ARG_MESSAGE, message);
            args.putInt(ARG_BUTTON, button);
            args.putBoolean(ARG_DISMISS, dismiss);

            DialogFragment dialog = new LastTryDialog();
            dialog.setArguments(args);
            dialog.show(from, TAG);
            from.executePendingTransactions();
            return true;
        }

        static void hide(FragmentManager from) {
            LastTryDialog dialog = (LastTryDialog) from.findFragmentByTag(TAG);
            if (dialog != null) {
                dialog.dismissAllowingStateLoss();
                from.executePendingTransactions();
            }
        }

        /**
         * Dialog setup.
         * <p>
         * To make it less likely that the dialog is dismissed accidentally, for example if the
         * device is malfunctioning or if the device is in a pocket, we set
         * {@code setCanceledOnTouchOutside(false)}.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(getArguments().getString(ARG_TITLE))
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(getArguments().getInt(ARG_BUTTON), null)
                    .create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        @Override
        public void onDismiss(final DialogInterface dialog) {
            super.onDismiss(dialog);
            if (getActivity() != null && getArguments().getBoolean(ARG_DISMISS)) {
                getActivity().finish();
            }
        }
    }
}

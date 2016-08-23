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

package com.android.settings;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserManager;
import android.security.KeyStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.fingerprint.FingerprintUiHelper;

/**
 * Base fragment to be shared for PIN/Pattern/Password confirmation fragments.
 */
public abstract class ConfirmDeviceCredentialBaseFragment extends OptionsMenuFragment
        implements FingerprintUiHelper.Callback {

    public static final String PACKAGE = "com.android.settings";
    public static final String TITLE_TEXT = PACKAGE + ".ConfirmCredentials.title";
    public static final String HEADER_TEXT = PACKAGE + ".ConfirmCredentials.header";
    public static final String DETAILS_TEXT = PACKAGE + ".ConfirmCredentials.details";
    public static final String ALLOW_FP_AUTHENTICATION =
            PACKAGE + ".ConfirmCredentials.allowFpAuthentication";
    public static final String DARK_THEME = PACKAGE + ".ConfirmCredentials.darkTheme";
    public static final String SHOW_CANCEL_BUTTON =
            PACKAGE + ".ConfirmCredentials.showCancelButton";
    public static final String SHOW_WHEN_LOCKED =
            PACKAGE + ".ConfirmCredentials.showWhenLocked";

    private FingerprintUiHelper mFingerprintHelper;
    protected boolean mIsStrongAuthRequired;
    private boolean mAllowFpAuthentication;
    protected boolean mReturnCredentials = false;
    protected Button mCancelButton;
    protected ImageView mFingerprintIcon;
    protected int mEffectiveUserId;
    protected int mUserId;
    protected LockPatternUtils mLockPatternUtils;
    protected TextView mErrorTextView;
    protected final Handler mHandler = new Handler();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAllowFpAuthentication = getActivity().getIntent().getBooleanExtra(
                ALLOW_FP_AUTHENTICATION, false);
        mReturnCredentials = getActivity().getIntent().getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_RETURN_CREDENTIALS, false);
        // Only take this argument into account if it belongs to the current profile.
        Intent intent = getActivity().getIntent();
        mUserId = Utils.getUserIdFromBundle(getActivity(), intent.getExtras());
        final UserManager userManager = UserManager.get(getActivity());
        mEffectiveUserId = userManager.getCredentialOwnerProfile(mUserId);
        mLockPatternUtils = new LockPatternUtils(getActivity());
        mIsStrongAuthRequired = isFingerprintDisallowedByStrongAuth();
        mAllowFpAuthentication = mAllowFpAuthentication && !isFingerprintDisabledByAdmin()
                && !mReturnCredentials && !mIsStrongAuthRequired;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCancelButton = (Button) view.findViewById(R.id.cancelButton);
        mFingerprintIcon = (ImageView) view.findViewById(R.id.fingerprintIcon);
        mFingerprintHelper = new FingerprintUiHelper(
                mFingerprintIcon,
                (TextView) view.findViewById(R.id.errorText), this, mEffectiveUserId);
        boolean showCancelButton = getActivity().getIntent().getBooleanExtra(
                SHOW_CANCEL_BUTTON, false);
        mCancelButton.setVisibility(showCancelButton ? View.VISIBLE : View.GONE);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        int credentialOwnerUserId = Utils.getCredentialOwnerUserId(
                getActivity(),
                Utils.getUserIdFromBundle(
                        getActivity(),
                        getActivity().getIntent().getExtras()));
        if (Utils.isManagedProfile(UserManager.get(getActivity()), credentialOwnerUserId)) {
            setWorkChallengeBackground(view, credentialOwnerUserId);
        }
    }

    private boolean isFingerprintDisabledByAdmin() {
        DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final int disabledFeatures = dpm.getKeyguardDisabledFeatures(null, mEffectiveUserId);
        return (disabledFeatures & DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0;
    }

    // User could be locked while Effective user is unlocked even though the effective owns the
    // credential. Otherwise, fingerprint can't unlock fbe/keystore through
    // verifyTiedProfileChallenge. In such case, we also wanna show the user message that
    // fingerprint is disabled due to device restart.
    private boolean isFingerprintDisallowedByStrongAuth() {
        return !(mLockPatternUtils.isFingerprintAllowedForUser(mEffectiveUserId)
                && KeyStore.getInstance().state(mUserId) == KeyStore.State.UNLOCKED);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshLockScreen();
    }

    protected void refreshLockScreen() {
        if (mAllowFpAuthentication) {
            mFingerprintHelper.startListening();
        } else {
            if (mFingerprintHelper.isListening()) {
                mFingerprintHelper.stopListening();
            }
        }
        if (isProfileChallenge()) {
            updateErrorMessage(mLockPatternUtils.getCurrentFailedPasswordAttempts(
                    mEffectiveUserId));
        }
    }

    protected void setAccessibilityTitle(CharSequence supplementalText) {
        Intent intent = getActivity().getIntent();
        if (intent != null) {
            CharSequence titleText = intent.getCharSequenceExtra(
                    ConfirmDeviceCredentialBaseFragment.TITLE_TEXT);
            if (titleText == null || supplementalText == null) {
                return;
            }
            String accessibilityTitle =
                    new StringBuilder(titleText).append(",").append(supplementalText).toString();
            getActivity().setTitle(Utils.createAccessibleSequence(titleText, accessibilityTitle));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFingerprintHelper.isListening()) {
            mFingerprintHelper.stopListening();
        }
    }

    @Override
    public void onAuthenticated() {
        // Check whether we are still active.
        if (getActivity() != null && getActivity().isResumed()) {
            TrustManager trustManager =
                (TrustManager) getActivity().getSystemService(Context.TRUST_SERVICE);
            trustManager.setDeviceLockedForUser(mEffectiveUserId, false);
            authenticationSucceeded();
            checkForPendingIntent();
        }
    }

    protected abstract void authenticationSucceeded();

    @Override
    public void onFingerprintIconVisibilityChanged(boolean visible) {
    }

    public void prepareEnterAnimation() {
    }

    public void startEnterAnimation() {
    }

    protected void checkForPendingIntent() {
        int taskId = getActivity().getIntent().getIntExtra(Intent.EXTRA_TASK_ID, -1);
        if (taskId != -1) {
            try {
                IActivityManager activityManager = ActivityManagerNative.getDefault();
                final ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchStackId(ActivityManager.StackId.INVALID_STACK_ID);
                activityManager.startActivityFromRecents(taskId, options.toBundle());
                return;
            } catch (RemoteException e) {
                // Do nothing.
            }
        }
        IntentSender intentSender = getActivity().getIntent()
                .getParcelableExtra(Intent.EXTRA_INTENT);
        if (intentSender != null) {
            try {
                getActivity().startIntentSenderForResult(intentSender, -1, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                /* ignore */
            }
        }
    }

    private void setWorkChallengeBackground(View baseView, int userId) {
        View mainContent = getActivity().findViewById(com.android.settings.R.id.main_content);
        if (mainContent != null) {
            // Remove the main content padding so that the background image is full screen.
            mainContent.setPadding(0, 0, 0, 0);
        }

        DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        baseView.setBackground(new ColorDrawable(dpm.getOrganizationColorForUser(userId)));
        ImageView imageView = (ImageView) baseView.findViewById(R.id.background_image);
        if (imageView != null) {
            Drawable image = getResources().getDrawable(R.drawable.work_challenge_background);
            image.setColorFilter(
                    getResources().getColor(R.color.confirm_device_credential_transparent_black),
                    PorterDuff.Mode.DARKEN);
            imageView.setImageDrawable(image);
            Point screenSize = new Point();
            getActivity().getWindowManager().getDefaultDisplay().getSize(screenSize);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    screenSize.y));
        }
    }

    protected boolean isProfileChallenge() {
        return Utils.isManagedProfile(UserManager.get(getContext()), mEffectiveUserId);
    }

    protected void reportSuccessfullAttempt() {
        if (isProfileChallenge()) {
            mLockPatternUtils.reportSuccessfulPasswordAttempt(mEffectiveUserId);
            // Keyguard is responsible to disable StrongAuth for primary user. Disable StrongAuth
            // for work challenge only here.
            mLockPatternUtils.userPresent(mEffectiveUserId);
        }
    }

    protected void reportFailedAttempt() {
        if (isProfileChallenge()) {
            // + 1 for this attempt.
            updateErrorMessage(
                    mLockPatternUtils.getCurrentFailedPasswordAttempts(mEffectiveUserId) + 1);
            mLockPatternUtils.reportFailedPasswordAttempt(mEffectiveUserId);
        }
    }

    protected void updateErrorMessage(int numAttempts) {
        final int maxAttempts =
                mLockPatternUtils.getMaximumFailedPasswordsForWipe(mEffectiveUserId);
        if (maxAttempts > 0 && numAttempts > 0) {
            int remainingAttempts = maxAttempts - numAttempts;
            if (remainingAttempts == 1) {
                // Last try
                final String title = getActivity().getString(
                        R.string.lock_profile_wipe_warning_title);
                final String message = getActivity().getString(getLastTryErrorMessage());
                showDialog(title, message, android.R.string.ok, false /* dismiss */);
            } else if (remainingAttempts <= 0) {
                // Profile is wiped
                final String message = getActivity().getString(R.string.lock_profile_wipe_content);
                showDialog(null, message, R.string.lock_profile_wipe_dismiss, true /* dismiss */);
            }
            if (mErrorTextView != null) {
                final String message = getActivity().getString(R.string.lock_profile_wipe_attempts,
                        numAttempts, maxAttempts);
                showError(message, 0);
            }
        }
    }

    protected abstract int getLastTryErrorMessage();

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

    protected abstract void onShowError();

    protected void showError(int msg, long timeout) {
        showError(getText(msg), timeout);
    }

    private void showDialog(String title, String message, int buttonString, final boolean dismiss) {
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(buttonString, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dismiss) {
                        getActivity().finish();
                    }
                }
            })
            .create();
        dialog.show();
    }
}

/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static android.app.admin.DevicePolicyResources.Strings.Settings.CONFIRM_WORK_PROFILE_PASSWORD_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.CONFIRM_WORK_PROFILE_PIN_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_CONFIRM_PASSWORD;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_CONFIRM_PIN;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LAST_PASSWORD_ATTEMPT_BEFORE_WIPE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LAST_PIN_ATTEMPT_BEFORE_WIPE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_PASSWORD_REQUIRED;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_PIN_REQUIRED;
import static android.app.admin.DevicePolicyResources.UNDEFINED;

import android.annotation.Nullable;
import android.app.KeyguardManager;
import android.app.RemoteLockscreenValidationResult;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImeAwareEditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.fragment.app.Fragment;

import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settings.R;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import com.google.android.setupdesign.GlifLayout;

import java.util.ArrayList;

public class ConfirmLockPassword extends ConfirmDeviceCredentialBaseActivity {

    // The index of the array is isStrongAuth << 2 + isManagedProfile << 1 + isAlpha.
    private static final int[] DETAIL_TEXTS = new int[] {
        R.string.lockpassword_confirm_your_pin_generic,
        R.string.lockpassword_confirm_your_password_generic,
        R.string.lockpassword_confirm_your_pin_generic_profile,
        R.string.lockpassword_confirm_your_password_generic_profile,
        R.string.lockpassword_strong_auth_required_device_pin,
        R.string.lockpassword_strong_auth_required_device_password,
        R.string.lockpassword_strong_auth_required_work_pin,
        R.string.lockpassword_strong_auth_required_work_password
    };

    private static final String[] DETAIL_TEXT_OVERRIDES = new String[] {
            UNDEFINED,
            UNDEFINED,
            WORK_PROFILE_CONFIRM_PIN,
            WORK_PROFILE_CONFIRM_PASSWORD,
            UNDEFINED,
            UNDEFINED,
            WORK_PROFILE_PIN_REQUIRED,
            WORK_PROFILE_PASSWORD_REQUIRED
    };

    public static class InternalActivity extends ConfirmLockPassword {
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmLockPasswordFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmLockPasswordFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_content);
        if (fragment != null && fragment instanceof ConfirmLockPasswordFragment) {
            ((ConfirmLockPasswordFragment) fragment).onWindowFocusChanged(hasFocus);
        }
    }

    public static class ConfirmLockPasswordFragment extends ConfirmDeviceCredentialBaseFragment
            implements OnClickListener, OnEditorActionListener,
            CredentialCheckResultTracker.Listener, SaveChosenLockWorkerBase.Listener {
        private static final String FRAGMENT_TAG_CHECK_LOCK_RESULT = "check_lock_result";
        private ImeAwareEditText mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private CredentialCheckResultTracker mCredentialCheckResultTracker;
        private boolean mDisappearing = false;
        private CountDownTimer mCountdownTimer;
        private boolean mIsAlpha;
        private InputMethodManager mImm;
        private AppearAnimationUtils mAppearAnimationUtils;
        private DisappearAnimationUtils mDisappearAnimationUtils;
        private boolean mIsManagedProfile;
        private GlifLayout mGlifLayout;
        private CharSequence mCheckBoxLabel;

        // required constructor for fragments
        public ConfirmLockPasswordFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final int storedQuality = mLockPatternUtils.getKeyguardStoredPasswordQuality(
                    mEffectiveUserId);

            ConfirmLockPassword activity = (ConfirmLockPassword) getActivity();
            View view = inflater.inflate(
                    activity.getConfirmCredentialTheme() == ConfirmCredentialTheme.NORMAL
                            ? R.layout.confirm_lock_password_normal
                            : R.layout.confirm_lock_password,
                    container,
                    false);
            mGlifLayout = view.findViewById(R.id.setup_wizard_layout);
            mPasswordEntry = (ImeAwareEditText) view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            // EditText inside ScrollView doesn't automatically get focus.
            mPasswordEntry.requestFocus();
            mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);
            mErrorTextView = (TextView) view.findViewById(R.id.errorText);

            if (mRemoteValidation) {
                mIsAlpha = mRemoteLockscreenValidationSession.getLockType()
                        == KeyguardManager.PASSWORD;
                // ProgressBar visibility is set to GONE until interacted with.
                // Set progress bar to INVISIBLE, so the EditText does not get bumped down later.
                mGlifLayout.setProgressBarShown(false);
            } else {
                mIsAlpha = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
                        || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
                        || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality
                        || DevicePolicyManager.PASSWORD_QUALITY_MANAGED == storedQuality;
            }
            mImm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);

            mIsManagedProfile = UserManager.get(getActivity()).isManagedProfile(mEffectiveUserId);

            Intent intent = getActivity().getIntent();
            if (intent != null) {
                CharSequence headerMessage = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.HEADER_TEXT);
                CharSequence detailsMessage = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.DETAILS_TEXT);
                if (TextUtils.isEmpty(headerMessage) && mIsManagedProfile) {
                    headerMessage = mDevicePolicyManager.getOrganizationNameForUser(mUserId);
                }
                if (TextUtils.isEmpty(headerMessage)) {
                    headerMessage = getDefaultHeader();
                }
                if (TextUtils.isEmpty(detailsMessage)) {
                    detailsMessage = getDefaultDetails();
                }
                mGlifLayout.setHeaderText(headerMessage);
                mGlifLayout.setDescriptionText(detailsMessage);
                mCheckBoxLabel = intent.getCharSequenceExtra(KeyguardManager.EXTRA_CHECKBOX_LABEL);
            }
            int currentType = mPasswordEntry.getInputType();
            if (mIsAlpha) {
                mPasswordEntry.setInputType(currentType);
                mPasswordEntry.setContentDescription(
                        getContext().getString(R.string.unlock_set_unlock_password_title));
            } else {
                mPasswordEntry.setInputType(
                        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                mPasswordEntry.setContentDescription(
                        getContext().getString(R.string.unlock_set_unlock_pin_title));
            }
            // Can't set via XML since setInputType resets the fontFamily to null
            mPasswordEntry.setTypeface(Typeface.create(
                    getContext().getString(com.android.internal.R.string.config_headlineFontFamily),
                    Typeface.NORMAL));
            mAppearAnimationUtils = new AppearAnimationUtils(getContext(),
                    220, 2f /* translationScale */, 1f /* delayScale*/,
                    AnimationUtils.loadInterpolator(getContext(),
                            android.R.interpolator.linear_out_slow_in));
            mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(),
                    110, 1f /* translationScale */,
                    0.5f /* delayScale */, AnimationUtils.loadInterpolator(
                            getContext(), android.R.interpolator.fast_out_linear_in));
            setAccessibilityTitle(mGlifLayout.getHeaderText());

            mCredentialCheckResultTracker = (CredentialCheckResultTracker) getFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAG_CHECK_LOCK_RESULT);
            if (mCredentialCheckResultTracker == null) {
                mCredentialCheckResultTracker = new CredentialCheckResultTracker();
                getFragmentManager().beginTransaction().add(mCredentialCheckResultTracker,
                        FRAGMENT_TAG_CHECK_LOCK_RESULT).commit();
            }

            return view;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            if (mRemoteValidation) {
                if (mCheckBox != null) {
                    mCheckBox.setText(TextUtils.isEmpty(mCheckBoxLabel)
                            ? getDefaultCheckboxLabel()
                            : mCheckBoxLabel);
                }
                if (mCancelButton != null && TextUtils.isEmpty(mAlternateButtonText)) {
                    mCancelButton.setText(mIsAlpha
                            ? R.string.lockpassword_forgot_password
                            : R.string.lockpassword_forgot_pin);
                }
            }

            if (mForgotButton != null) {
                mForgotButton.setText(mIsAlpha
                        ? R.string.lockpassword_forgot_password
                        : R.string.lockpassword_forgot_pin);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mPasswordEntry != null) {
                mPasswordEntry.setText(null);
            }
            // Force a garbage collection to remove remnant of user password shards from memory.
            // Execute this with a slight delay to allow the activity lifecycle to complete and
            // the instance to become gc-able.
            new Handler(Looper.myLooper()).postDelayed(() -> {
                System.gc();
                System.runFinalization();
                System.gc();
            }, 5000);
        }

        private String getDefaultHeader() {
            if (mFrp) {
                return mIsAlpha ? getString(R.string.lockpassword_confirm_your_password_header_frp)
                        : getString(R.string.lockpassword_confirm_your_pin_header_frp);
            }
            if (mRemoteValidation) {
                return getString(R.string.lockpassword_remote_validation_header);
            }
            if (mIsManagedProfile) {
                if (mIsAlpha) {
                    return mDevicePolicyManager.getResources().getString(
                            CONFIRM_WORK_PROFILE_PASSWORD_HEADER,
                            () -> getString(
                                    R.string.lockpassword_confirm_your_work_password_header));
                }
                return mDevicePolicyManager.getResources().getString(
                        CONFIRM_WORK_PROFILE_PIN_HEADER,
                        () -> getString(R.string.lockpassword_confirm_your_work_pin_header));
            }
            return mIsAlpha ? getString(R.string.lockpassword_confirm_your_password_header)
                    : getString(R.string.lockpassword_confirm_your_pin_header);
        }

        private String getDefaultDetails() {
            if (mFrp) {
                return mIsAlpha ? getString(R.string.lockpassword_confirm_your_password_details_frp)
                        : getString(R.string.lockpassword_confirm_your_pin_details_frp);
            }
            if (mRemoteValidation) {
                return getContext().getString(mIsAlpha
                        ? R.string.lockpassword_remote_validation_password_details
                        : R.string.lockpassword_remote_validation_pin_details);
            }
            boolean isStrongAuthRequired = isStrongAuthRequired();
            // Map boolean flags to an index by isStrongAuth << 2 + isManagedProfile << 1 + isAlpha.
            int index = ((isStrongAuthRequired ? 1 : 0) << 2) + ((mIsManagedProfile ? 1 : 0) << 1)
                    + (mIsAlpha ? 1 : 0);
            return mDevicePolicyManager.getResources().getString(
                    DETAIL_TEXT_OVERRIDES[index], () -> getString(DETAIL_TEXTS[index]));
        }

        private String getDefaultCheckboxLabel() {
            if (mRemoteValidation) {
                return getString(mIsAlpha
                        ? R.string.lockpassword_remote_validation_set_password_as_screenlock
                        : R.string.lockpassword_remote_validation_set_pin_as_screenlock);
            }
            throw new IllegalStateException(
                    "Trying to get default checkbox label for illegal flow");
        }

        private int getErrorMessage() {
            return mIsAlpha ? R.string.lockpassword_invalid_password
                    : R.string.lockpassword_invalid_pin;
        }

        @Override
        protected String getLastTryOverrideErrorMessageId(int userType) {
            if (userType == USER_TYPE_MANAGED_PROFILE) {
                return mIsAlpha ?  WORK_PROFILE_LAST_PASSWORD_ATTEMPT_BEFORE_WIPE
                        : WORK_PROFILE_LAST_PIN_ATTEMPT_BEFORE_WIPE;
            }

            return UNDEFINED;
        }

        @Override
        protected int getLastTryDefaultErrorMessage(int userType) {
            switch (userType) {
                case USER_TYPE_PRIMARY:
                    return mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_device
                            : R.string.lock_last_pin_attempt_before_wipe_device;
                case USER_TYPE_MANAGED_PROFILE:
                    return mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_profile
                            : R.string.lock_last_pin_attempt_before_wipe_profile;
                case USER_TYPE_SECONDARY:
                    return mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_user
                            : R.string.lock_last_pin_attempt_before_wipe_user;
                default:
                    throw new IllegalArgumentException("Unrecognized user type:" + userType);
            }
        }

        @Override
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            mGlifLayout.getHeaderTextView().setAlpha(0f);
            mGlifLayout.getDescriptionTextView().setAlpha(0f);
            mCancelButton.setAlpha(0f);
            if (mForgotButton != null) {
                mForgotButton.setAlpha(0f);
            }
            mPasswordEntry.setAlpha(0f);
            mErrorTextView.setAlpha(0f);
        }

        private View[] getActiveViews() {
            ArrayList<View> result = new ArrayList<>();
            result.add(mGlifLayout.getHeaderTextView());
            result.add(mGlifLayout.getDescriptionTextView());
            if (mCancelButton.getVisibility() == View.VISIBLE) {
                result.add(mCancelButton);
            }
            if (mForgotButton != null) {
                result.add(mForgotButton);
            }
            result.add(mPasswordEntry);
            result.add(mErrorTextView);
            return result.toArray(new View[] {});
        }

        @Override
        public void startEnterAnimation() {
            super.startEnterAnimation();
            mAppearAnimationUtils.startAnimation(getActiveViews(), this::updatePasswordEntry);
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
                mCountdownTimer = null;
            }
            mCredentialCheckResultTracker.setListener(null);
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.CONFIRM_LOCK_PASSWORD;
        }

        @Override
        public void onResume() {
            super.onResume();
            long deadline = mLockPatternUtils.getLockoutAttemptDeadline(mEffectiveUserId);
            if (deadline != 0) {
                mCredentialCheckResultTracker.clearResult();
                handleAttemptLockout(deadline);
            } else {
                updatePasswordEntry();
                mErrorTextView.setText("");
                updateErrorMessage(
                        mLockPatternUtils.getCurrentFailedPasswordAttempts(mEffectiveUserId));
            }
            mCredentialCheckResultTracker.setListener(this);
        }

        @Override
        protected void authenticationSucceeded() {
            mCredentialCheckResultTracker.setResult(true, new Intent(), 0, mEffectiveUserId);
        }

        private void updatePasswordEntry() {
            final boolean isLockedOut =
                    mLockPatternUtils.getLockoutAttemptDeadline(mEffectiveUserId) != 0;
            mPasswordEntry.setEnabled(!isLockedOut);
            mPasswordEntryInputDisabler.setInputEnabled(!isLockedOut);
            if (isLockedOut) {
                mImm.hideSoftInputFromWindow(mPasswordEntry.getWindowToken(), 0 /*flags*/);
            } else {
                mPasswordEntry.scheduleShowSoftInput();
                mPasswordEntry.requestFocus();
            }
        }

        public void onWindowFocusChanged(boolean hasFocus) {
            if (!hasFocus) {
                return;
            }
            // Post to let window focus logic to finish to allow soft input show/hide properly.
            mPasswordEntry.post(this::updatePasswordEntry);
        }

        private void handleNext() {
            if (mPendingLockCheck != null || mDisappearing) {
                return;
            }

            // TODO(b/120484642): This is a point of entry for passwords from the UI
            final Editable passwordText = mPasswordEntry.getText();
            if (TextUtils.isEmpty(passwordText)) {
                return;
            }
            final LockscreenCredential credential = mIsAlpha
                    ? LockscreenCredential.createPassword(passwordText)
                    : LockscreenCredential.createPin(passwordText);

            mPasswordEntryInputDisabler.setInputEnabled(false);

            if (mRemoteValidation) {
                validateGuess(credential);
                mGlifLayout.setProgressBarShown(true);
                return;
            }

            Intent intent = new Intent();
            // TODO(b/161956762): Sanitize this
            if (mReturnGatekeeperPassword) {
                if (isInternalActivity()) {
                    startVerifyPassword(credential, intent,
                            LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE);
                    return;
                }
            } else if (mForceVerifyPath)  {
                if (isInternalActivity()) {
                    startVerifyPassword(credential, intent, 0 /* flags */);
                    return;
                }
            } else {
                startCheckPassword(credential, intent);
                return;
            }

            mCredentialCheckResultTracker.setResult(false, intent, 0, mEffectiveUserId);
        }

        private boolean isInternalActivity() {
            return getActivity() instanceof ConfirmLockPassword.InternalActivity;
        }

        private void startVerifyPassword(LockscreenCredential credential, final Intent intent,
                @LockPatternUtils.VerifyFlag int flags) {
            final int localEffectiveUserId = mEffectiveUserId;
            final int localUserId = mUserId;
            final LockPatternChecker.OnVerifyCallback onVerifyCallback = (response, timeoutMs) -> {
                mPendingLockCheck = null;
                final boolean matched = response.isMatched();
                if (matched && mReturnCredentials) {
                    if ((flags & LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE) != 0) {
                        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                                response.getGatekeeperPasswordHandle());
                    } else {
                        intent.putExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                                response.getGatekeeperHAT());
                    }
                }
                mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs,
                        localEffectiveUserId);
            };
            mPendingLockCheck = (localEffectiveUserId == localUserId)
                    ? LockPatternChecker.verifyCredential(mLockPatternUtils, credential,
                            localUserId, flags, onVerifyCallback)
                    : LockPatternChecker.verifyTiedProfileChallenge(mLockPatternUtils, credential,
                            localUserId, flags, onVerifyCallback);
        }

        private void startCheckPassword(final LockscreenCredential credential,
                final Intent intent) {
            final int localEffectiveUserId = mEffectiveUserId;
            mPendingLockCheck = LockPatternChecker.checkCredential(
                    mLockPatternUtils,
                    credential,
                    localEffectiveUserId,
                    new LockPatternChecker.OnCheckCallback() {
                        @Override
                        public void onChecked(boolean matched, int timeoutMs) {
                            mPendingLockCheck = null;
                            if (matched && isInternalActivity() && mReturnCredentials) {
                                intent.putExtra(
                                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, credential);
                            }
                            mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs,
                                    localEffectiveUserId);
                        }
                    });
        }

        private void startDisappearAnimation(final Intent intent) {
            ConfirmDeviceCredentialUtils.hideImeImmediately(
                    getActivity().getWindow().getDecorView());

            if (mDisappearing) {
                return;
            }
            mDisappearing = true;

            final ConfirmLockPassword activity = (ConfirmLockPassword) getActivity();
            // Bail if there is no active activity.
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (activity.getConfirmCredentialTheme() == ConfirmCredentialTheme.DARK) {
                mDisappearAnimationUtils.startAnimation(getActiveViews(), () -> {
                    activity.setResult(RESULT_OK, intent);
                    activity.finish();
                    activity.overridePendingTransition(
                            R.anim.confirm_credential_close_enter,
                            R.anim.confirm_credential_close_exit);
                });
            } else {
                activity.setResult(RESULT_OK, intent);
                activity.finish();
            }
        }

        private void onPasswordChecked(boolean matched, Intent intent, int timeoutMs,
                int effectiveUserId, boolean newResult) {
            mPasswordEntryInputDisabler.setInputEnabled(true);
            if (matched) {
                if (newResult) {
                    ConfirmDeviceCredentialUtils.reportSuccessfulAttempt(mLockPatternUtils,
                            mUserManager, mDevicePolicyManager, mEffectiveUserId,
                            /* isStrongAuth */ true);
                }
                startDisappearAnimation(intent);
                ConfirmDeviceCredentialUtils.checkForPendingIntent(getActivity());
            } else {
                if (timeoutMs > 0) {
                    refreshLockScreen();
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline(
                            effectiveUserId, timeoutMs);
                    handleAttemptLockout(deadline);
                } else {
                    showError(getErrorMessage(), CLEAR_WRONG_ATTEMPT_TIMEOUT_MS);
                }
                if (newResult) {
                    reportFailedAttempt();
                }
            }
        }

        @Override
        protected void onRemoteDeviceCredentialValidationResult(
                RemoteLockscreenValidationResult result) {
            switch (result.getResultCode()) {
                case RemoteLockscreenValidationResult.RESULT_GUESS_VALID:
                    if (mCheckBox.isChecked()) {
                        ChooseLockPassword.SaveAndFinishWorker saveAndFinishWorker =
                                new ChooseLockPassword.SaveAndFinishWorker();
                        Log.i(TAG, "Setting device screen lock to the other device's screen lock.");
                        getFragmentManager().beginTransaction().add(saveAndFinishWorker, null)
                                .commit();
                        getFragmentManager().executePendingTransactions();
                        saveAndFinishWorker.setListener(this);
                        saveAndFinishWorker.start(
                                mLockPatternUtils,
                                /* requestGatekeeperPassword= */ false,
                                mDeviceCredentialGuess,
                                /* currentCredential= */ null,
                                mEffectiveUserId);
                        return;
                    }
                    mCredentialCheckResultTracker.setResult(/* matched= */ true, new Intent(),
                            /* timeoutMs= */ 0, mEffectiveUserId);
                    break;
                case RemoteLockscreenValidationResult.RESULT_GUESS_INVALID:
                    mCredentialCheckResultTracker.setResult(/* matched= */ false, new Intent(),
                            /* timeoutMs= */ 0, mEffectiveUserId);
                    break;
                case RemoteLockscreenValidationResult.RESULT_LOCKOUT:
                    mCredentialCheckResultTracker.setResult(/* matched= */ false, new Intent(),
                            (int) result.getTimeoutMillis(), mEffectiveUserId);
                    break;
                case RemoteLockscreenValidationResult.RESULT_NO_REMAINING_ATTEMPTS:
                    getActivity().finish();
                    break;
                case RemoteLockscreenValidationResult.RESULT_SESSION_EXPIRED:
                    getActivity().finish();
            }
            mGlifLayout.setProgressBarShown(false);
        }

        @Override
        public void onCredentialChecked(boolean matched, Intent intent, int timeoutMs,
                int effectiveUserId, boolean newResult) {
            onPasswordChecked(matched, intent, timeoutMs, effectiveUserId, newResult);
        }

        @Override
        protected void onShowError() {
            mPasswordEntry.setText(null);
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            mCountdownTimer = new CountDownTimer(
                    elapsedRealtimeDeadline - SystemClock.elapsedRealtime(),
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    final int secondsCountdown = (int) (millisUntilFinished / 1000);
                    showError(getString(
                            R.string.lockpattern_too_many_failed_confirmation_attempts,
                            secondsCountdown), 0);
                }

                @Override
                public void onFinish() {
                    updatePasswordEntry();
                    mErrorTextView.setText("");
                    updateErrorMessage(
                            mLockPatternUtils.getCurrentFailedPasswordAttempts(mEffectiveUserId));
                }
            }.start();
            updatePasswordEntry();
        }

        public void onClick(View v) {
            if (v.getId() == R.id.next_button) {
                handleNext();
            } else if (v.getId() == R.id.cancel_button) {
                getActivity().setResult(RESULT_CANCELED);
                getActivity().finish();
            }
        }

        // {@link OnEditorActionListener} methods.
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Check if this was the result of hitting the enter or "done" key
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                handleNext();
                return true;
            }
            return false;
        }

        /**
         * Callback for when the device credential guess used for remote validation was set as the
         * current device's device credential.
         */
        @Override
        public void onChosenLockSaveFinished(boolean wasSecureBefore, Intent resultData) {
            if (mDeviceCredentialGuess != null) {
                mDeviceCredentialGuess.zeroize();
            }
            mGlifLayout.setProgressBarShown(false);
            mCredentialCheckResultTracker.setResult(/* matched= */ true, new Intent(),
                    /* timeoutMs= */ 0, mEffectiveUserId);
        }
    }
}

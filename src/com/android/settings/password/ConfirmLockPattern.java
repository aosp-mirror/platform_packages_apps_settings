/*
 * Copyright (C) 2008 The Android Open Source Project
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

import static android.app.admin.DevicePolicyResources.Strings.Settings.CONFIRM_WORK_PROFILE_PATTERN_HEADER;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_CONFIRM_PATTERN;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_LAST_PATTERN_ATTEMPT_BEFORE_WIPE;
import static android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_PATTERN_REQUIRED;
import static android.app.admin.DevicePolicyResources.UNDEFINED;

import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.RemoteLockscreenValidationResult;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;

import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.R;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import com.google.android.setupdesign.GlifLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Launch this when you want the user to confirm their lock pattern.
 *
 * Sets an activity result of {@link Activity#RESULT_OK} when the user
 * successfully confirmed their pattern.
 */
public class ConfirmLockPattern extends ConfirmDeviceCredentialBaseActivity {

    public static class InternalActivity extends ConfirmLockPattern {
    }

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmLockPatternFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmLockPatternFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    public static class ConfirmLockPatternFragment extends ConfirmDeviceCredentialBaseFragment
            implements AppearAnimationCreator<Object>, CredentialCheckResultTracker.Listener,
            SaveChosenLockWorkerBase.Listener {

        private static final String FRAGMENT_TAG_CHECK_LOCK_RESULT = "check_lock_result";

        private LockPatternView mLockPatternView;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private CredentialCheckResultTracker mCredentialCheckResultTracker;
        private boolean mDisappearing = false;
        private CountDownTimer mCountdownTimer;

        private GlifLayout mGlifLayout;
        private View mSudContent;

        // caller-supplied text for various prompts
        private CharSequence mHeaderText;
        private CharSequence mDetailsText;
        private CharSequence mCheckBoxLabel;

        private AppearAnimationUtils mAppearAnimationUtils;
        private DisappearAnimationUtils mDisappearAnimationUtils;

        private boolean mIsManagedProfile;

        // required constructor for fragments
        public ConfirmLockPatternFragment() {

        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ConfirmLockPattern activity = (ConfirmLockPattern) getActivity();
            View view = inflater.inflate(
                    activity.getConfirmCredentialTheme() == ConfirmCredentialTheme.NORMAL
                            ? R.layout.confirm_lock_pattern_normal
                            : R.layout.confirm_lock_pattern,
                    container,
                    false);
            mGlifLayout = view.findViewById(R.id.setup_wizard_layout);
            mLockPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
            mErrorTextView = (TextView) view.findViewById(R.id.errorText);
            // TODO(b/243008023) Workaround for Glif layout on 2 panel choose lock settings.
            mSudContent = mGlifLayout.findViewById(R.id.sud_layout_content);
            mSudContent.setPadding(mSudContent.getPaddingLeft(), 0, mSudContent.getPaddingRight(),
                    0);
            mIsManagedProfile = UserManager.get(getActivity()).isManagedProfile(mEffectiveUserId);

            // make it so unhandled touch events within the unlock screen go to the
            // lock pattern view.
            final LinearLayoutWithDefaultTouchRecepient topLayout
                    = (LinearLayoutWithDefaultTouchRecepient) view.findViewById(R.id.topLayout);
            topLayout.setDefaultTouchRecepient(mLockPatternView);

            Intent intent = getActivity().getIntent();
            if (intent != null) {
                mHeaderText = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.HEADER_TEXT);
                mDetailsText = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.DETAILS_TEXT);
                mCheckBoxLabel = intent.getCharSequenceExtra(KeyguardManager.EXTRA_CHECKBOX_LABEL);
            }
            if (TextUtils.isEmpty(mHeaderText) && mIsManagedProfile) {
                mHeaderText = mDevicePolicyManager.getOrganizationNameForUser(mUserId);
            }

            mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled(
                    mEffectiveUserId));
            mLockPatternView.setOnPatternListener(mConfirmExistingLockPatternListener);
            mLockPatternView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            });
            updateStage(Stage.NeedToUnlock);

            if (savedInstanceState == null) {
                // on first launch, if no lock pattern is set, then finish with
                // success (don't want user to get stuck confirming something that
                // doesn't exist).
                // Don't do this check for FRP though, because the pattern is not stored
                // in a way that isLockPatternEnabled is aware of for that case.
                // TODO(roosa): This block should no longer be needed since we removed the
                //              ability to disable the pattern in L. Remove this block after
                //              ensuring it's safe to do so. (Note that ConfirmLockPassword
                //              doesn't have this).
                if (!mFrp && !mRemoteValidation
                        && !mLockPatternUtils.isLockPatternEnabled(mEffectiveUserId)) {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                }
            }
            mAppearAnimationUtils = new AppearAnimationUtils(getContext(),
                    AppearAnimationUtils.DEFAULT_APPEAR_DURATION, 2f /* translationScale */,
                    1.3f /* delayScale */, AnimationUtils.loadInterpolator(
                    getContext(), android.R.interpolator.linear_out_slow_in));
            mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(),
                    125, 4f /* translationScale */,
                    0.3f /* delayScale */, AnimationUtils.loadInterpolator(
                    getContext(), android.R.interpolator.fast_out_linear_in),
                    new AppearAnimationUtils.RowTranslationScaler() {
                        @Override
                        public float getRowTranslationScale(int row, int numRows) {
                            return (float)(numRows - row) / numRows;
                        }
                    });
            setAccessibilityTitle(mGlifLayout.getHeaderText());

            mCredentialCheckResultTracker = (CredentialCheckResultTracker) getFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAG_CHECK_LOCK_RESULT);
            if (mCredentialCheckResultTracker == null) {
                mCredentialCheckResultTracker = new CredentialCheckResultTracker();
                getFragmentManager().beginTransaction().add(mCredentialCheckResultTracker,
                        FRAGMENT_TAG_CHECK_LOCK_RESULT).commit();
            }

            if (mRemoteValidation) {
                // ProgressBar visibility is set to GONE until interacted with.
                // Set progress bar to INVISIBLE, so the pattern does not get bumped down later.
                mGlifLayout.setProgressBarShown(false);
                // Lock pattern is generally not visible until the user has set a lockscreen for the
                // first time. For a new user, this means that the pattern will always be hidden.
                // Despite this prerequisite, we want to show the pattern anyway for this flow.
                mLockPatternView.setInStealthMode(false);
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
                    mCancelButton.setText(R.string.lockpassword_forgot_pattern);
                }
            }

            if (mForgotButton != null) {
                mForgotButton.setText(R.string.lockpassword_forgot_pattern);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            // deliberately not calling super since we are managing this in full
        }

        @Override
        public void onPause() {
            super.onPause();

            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
            }
            mCredentialCheckResultTracker.setListener(null);
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.CONFIRM_LOCK_PATTERN;
        }

        @Override
        public void onResume() {
            super.onResume();

            // if the user is currently locked out, enforce it.
            long deadline = mLockPatternUtils.getLockoutAttemptDeadline(mEffectiveUserId);
            if (deadline != 0) {
                mCredentialCheckResultTracker.clearResult();
                handleAttemptLockout(deadline);
            } else if (!mLockPatternView.isEnabled()) {
                // The deadline has passed, but the timer was cancelled. Or the pending lock
                // check was cancelled. Need to clean up.
                updateStage(Stage.NeedToUnlock);
            }
            mCredentialCheckResultTracker.setListener(this);
        }

        @Override
        protected void onShowError() {
        }

        @Override
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            mGlifLayout.getHeaderTextView().setAlpha(0f);
            mCancelButton.setAlpha(0f);
            if (mForgotButton != null) {
                mForgotButton.setAlpha(0f);
            }
            mLockPatternView.setAlpha(0f);
            mGlifLayout.getDescriptionTextView().setAlpha(0f);
        }

        private String getDefaultDetails() {
            if (mFrp) {
                return getString(R.string.lockpassword_confirm_your_pattern_details_frp);
            }
            if (mRemoteValidation) {
                return getString(
                        R.string.lockpassword_remote_validation_pattern_details);
            }
            final boolean isStrongAuthRequired = isStrongAuthRequired();
            if (mIsManagedProfile) {
                if (isStrongAuthRequired) {
                    return mDevicePolicyManager.getResources().getString(
                            WORK_PROFILE_PATTERN_REQUIRED,
                            () -> getString(
                                    R.string.lockpassword_strong_auth_required_work_pattern));
                } else {
                    return mDevicePolicyManager.getResources().getString(
                            WORK_PROFILE_CONFIRM_PATTERN,
                            () -> getString(
                                    R.string.lockpassword_confirm_your_pattern_generic_profile));
                }
            } else {
                return isStrongAuthRequired
                        ? getString(R.string.lockpassword_strong_auth_required_device_pattern)
                        : getString(R.string.lockpassword_confirm_your_pattern_generic);
            }
        }

        private Object[][] getActiveViews() {
            ArrayList<ArrayList<Object>> result = new ArrayList<>();
            result.add(new ArrayList<>(Collections.singletonList(mGlifLayout.getHeaderTextView())));
            result.add(new ArrayList<>(
                    Collections.singletonList(mGlifLayout.getDescriptionTextView())));
            if (mCancelButton.getVisibility() == View.VISIBLE) {
                result.add(new ArrayList<>(Collections.singletonList(mCancelButton)));
            }
            if (mForgotButton != null) {
                result.add(new ArrayList<>(Collections.singletonList(mForgotButton)));
            }
            LockPatternView.CellState[][] cellStates = mLockPatternView.getCellStates();
            for (int i = 0; i < cellStates.length; i++) {
                ArrayList<Object> row = new ArrayList<>();
                for (int j = 0; j < cellStates[i].length; j++) {
                    row.add(cellStates[i][j]);
                }
                result.add(row);
            }
            Object[][] resultArr = new Object[result.size()][cellStates[0].length];
            for (int i = 0; i < result.size(); i++) {
                ArrayList<Object> row = result.get(i);
                for (int j = 0; j < row.size(); j++) {
                    resultArr[i][j] = row.get(j);
                }
            }
            return resultArr;
        }

        @Override
        public void startEnterAnimation() {
            super.startEnterAnimation();
            mLockPatternView.setAlpha(1f);
            mAppearAnimationUtils.startAnimation2d(getActiveViews(), null, this);
        }

        private void updateStage(Stage stage) {
            switch (stage) {
                case NeedToUnlock:
                    if (mHeaderText != null) {
                        mGlifLayout.setHeaderText(mHeaderText);
                    } else {
                        mGlifLayout.setHeaderText(getDefaultHeader());
                    }

                    CharSequence detailsText =
                            mDetailsText == null ? getDefaultDetails() : mDetailsText;
                    mGlifLayout.setDescriptionText(detailsText);

                    mErrorTextView.setText("");
                    updateErrorMessage(
                            mLockPatternUtils.getCurrentFailedPasswordAttempts(mEffectiveUserId));

                    mLockPatternView.setEnabled(true);
                    mLockPatternView.enableInput();
                    mLockPatternView.clearPattern();
                    break;
                case NeedToUnlockWrong:
                    showError(R.string.lockpattern_need_to_unlock_wrong,
                            CLEAR_WRONG_ATTEMPT_TIMEOUT_MS);

                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    mLockPatternView.setEnabled(true);
                    mLockPatternView.enableInput();
                    break;
                case LockedOut:
                    mLockPatternView.clearPattern();
                    // enabled = false means: disable input, and have the
                    // appearance of being disabled.
                    mLockPatternView.setEnabled(false); // appearance of being disabled
                    break;
            }

            // Always announce the header for accessibility. This is a no-op
            // when accessibility is disabled.
            mGlifLayout.getHeaderTextView().announceForAccessibility(mGlifLayout.getHeaderText());
        }

        private String getDefaultHeader() {
            if (mFrp) return getString(R.string.lockpassword_confirm_your_pattern_header_frp);
            if (mRemoteValidation) {
                return getString(R.string.lockpassword_remote_validation_header);
            }
            if (mIsManagedProfile) {
                return mDevicePolicyManager.getResources().getString(
                        CONFIRM_WORK_PROFILE_PATTERN_HEADER,
                        () -> getString(R.string.lockpassword_confirm_your_work_pattern_header));
            }

            return getString(R.string.lockpassword_confirm_your_pattern_header);
        }

        private String getDefaultCheckboxLabel() {
            if (mRemoteValidation) {
                return getString(R.string.lockpassword_remote_validation_set_pattern_as_screenlock);
            }
            throw new IllegalStateException(
                    "Trying to get default checkbox label for illegal flow");
        }

        private Runnable mClearPatternRunnable = new Runnable() {
            public void run() {
                mLockPatternView.clearPattern();
            }
        };

        // clear the wrong pattern unless they have started a new one
        // already
        private void postClearPatternRunnable() {
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
            mLockPatternView.postDelayed(mClearPatternRunnable, CLEAR_WRONG_ATTEMPT_TIMEOUT_MS);
        }

        @Override
        protected void authenticationSucceeded() {
            mCredentialCheckResultTracker.setResult(true, new Intent(), 0, mEffectiveUserId);
        }

        private void startDisappearAnimation(final Intent intent) {
            if (mDisappearing) {
                return;
            }
            mDisappearing = true;

            final ConfirmLockPattern activity = (ConfirmLockPattern) getActivity();
            // Bail if there is no active activity.
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (activity.getConfirmCredentialTheme() == ConfirmCredentialTheme.DARK) {
                mLockPatternView.clearPattern();
                mDisappearAnimationUtils.startAnimation2d(getActiveViews(),
                        () -> {
                            activity.setResult(RESULT_OK, intent);
                            activity.finish();
                            activity.overridePendingTransition(
                                    R.anim.confirm_credential_close_enter,
                                    R.anim.confirm_credential_close_exit);
                        }, this);
            } else {
                activity.setResult(RESULT_OK, intent);
                activity.finish();
            }
        }

        /**
         * The pattern listener that responds according to a user confirming
         * an existing lock pattern.
         */
        private LockPatternView.OnPatternListener mConfirmExistingLockPatternListener
                = new LockPatternView.OnPatternListener() {

            public void onPatternStart() {
                mLockPatternView.removeCallbacks(mClearPatternRunnable);
            }

            public void onPatternCleared() {
                mLockPatternView.removeCallbacks(mClearPatternRunnable);
            }

            public void onPatternCellAdded(List<Cell> pattern) {

            }

            public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                if (mPendingLockCheck != null || mDisappearing) {
                    return;
                }

                mLockPatternView.setEnabled(false);

                final LockscreenCredential credential = LockscreenCredential.createPattern(pattern);

                if (mRemoteValidation) {
                    validateGuess(credential);
                    mGlifLayout.setProgressBarShown(true);
                    return;
                }

                // TODO(b/161956762): Sanitize this
                Intent intent = new Intent();
                if (mReturnGatekeeperPassword) {
                    if (isInternalActivity()) {
                        startVerifyPattern(credential, intent,
                                LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE);
                        return;
                    }
                } else if (mForceVerifyPath) {
                    if (isInternalActivity()) {
                        startVerifyPattern(credential, intent, 0 /* flags */);
                        return;
                    }
                } else {
                    startCheckPattern(credential, intent);
                    return;
                }

                mCredentialCheckResultTracker.setResult(false, intent, 0, mEffectiveUserId);
            }

            private boolean isInternalActivity() {
                return getActivity() instanceof ConfirmLockPattern.InternalActivity;
            }

            private void startVerifyPattern(final LockscreenCredential pattern,
                    final Intent intent, @LockPatternUtils.VerifyFlag int flags) {
                final int localEffectiveUserId = mEffectiveUserId;
                final int localUserId = mUserId;
                final LockPatternChecker.OnVerifyCallback onVerifyCallback =
                    (response, timeoutMs) -> {
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
                        ? LockPatternChecker.verifyCredential(
                                mLockPatternUtils, pattern, localUserId, flags,
                                onVerifyCallback)
                        : LockPatternChecker.verifyTiedProfileChallenge(
                                mLockPatternUtils, pattern, localUserId, flags,
                                onVerifyCallback);
            }

            private void startCheckPattern(final LockscreenCredential pattern,
                    final Intent intent) {
                if (pattern.size() < LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                    // Pattern size is less than the minimum, do not count it as an fail attempt.
                    onPatternChecked(false, intent, 0, mEffectiveUserId, false /* newResult */);
                    return;
                }

                final int localEffectiveUserId = mEffectiveUserId;
                mPendingLockCheck = LockPatternChecker.checkCredential(
                        mLockPatternUtils,
                        pattern,
                        localEffectiveUserId,
                        new LockPatternChecker.OnCheckCallback() {
                            @Override
                            public void onChecked(boolean matched, int timeoutMs) {
                                mPendingLockCheck = null;
                                if (matched && isInternalActivity() && mReturnCredentials) {
                                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD,
                                                    pattern);
                                }
                                mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs,
                                        localEffectiveUserId);
                            }
                        });
            }
        };

        private void onPatternChecked(boolean matched, Intent intent, int timeoutMs,
                int effectiveUserId, boolean newResult) {
            mLockPatternView.setEnabled(true);
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
                    updateStage(Stage.NeedToUnlockWrong);
                    postClearPatternRunnable();
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
                        Log.i(TAG, "Setting device screen lock to the other device's screen lock.");
                        ChooseLockPattern.SaveAndFinishWorker saveAndFinishWorker =
                                new ChooseLockPattern.SaveAndFinishWorker();
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
            }
            mGlifLayout.setProgressBarShown(false);
        }

        @Override
        public void onCredentialChecked(boolean matched, Intent intent, int timeoutMs,
                int effectiveUserId, boolean newResult) {
            onPatternChecked(matched, intent, timeoutMs, effectiveUserId, newResult);
        }

        @Override
        protected String getLastTryOverrideErrorMessageId(int userType) {
            if (userType == USER_TYPE_MANAGED_PROFILE) {
                return WORK_PROFILE_LAST_PATTERN_ATTEMPT_BEFORE_WIPE;
            }

            return UNDEFINED;
        }

        @Override
        protected int getLastTryDefaultErrorMessage(int userType) {
            switch (userType) {
                case USER_TYPE_PRIMARY:
                    return R.string.lock_last_pattern_attempt_before_wipe_device;
                case USER_TYPE_MANAGED_PROFILE:
                    return R.string.lock_last_pattern_attempt_before_wipe_profile;
                case USER_TYPE_SECONDARY:
                    return R.string.lock_last_pattern_attempt_before_wipe_user;
                default:
                    throw new IllegalArgumentException("Unrecognized user type:" + userType);
            }
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            updateStage(Stage.LockedOut);
            long elapsedRealtime = SystemClock.elapsedRealtime();
            mCountdownTimer = new CountDownTimer(
                    elapsedRealtimeDeadline - elapsedRealtime,
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    final int secondsCountdown = (int) (millisUntilFinished / 1000);
                    mErrorTextView.setText(getString(
                            R.string.lockpattern_too_many_failed_confirmation_attempts,
                            secondsCountdown));
                }

                @Override
                public void onFinish() {
                    updateStage(Stage.NeedToUnlock);
                }
            }.start();
        }

        @Override
        public void createAnimation(Object obj, long delay,
                long duration, float translationY, final boolean appearing,
                Interpolator interpolator,
                final Runnable finishListener) {
            if (obj instanceof LockPatternView.CellState) {
                final LockPatternView.CellState animatedCell = (LockPatternView.CellState) obj;
                mLockPatternView.startCellStateAnimation(animatedCell,
                        1f, appearing ? 1f : 0f, /* alpha */
                        appearing ? translationY : 0f, /* startTranslation */
                        appearing ? 0f : translationY, /* endTranslation */
                        appearing ? 0f : 1f, 1f /* scale */,
                        delay, duration, interpolator, finishListener);
            } else {
                mAppearAnimationUtils.createAnimation((View) obj, delay, duration, translationY,
                        appearing, interpolator, finishListener);
            }
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

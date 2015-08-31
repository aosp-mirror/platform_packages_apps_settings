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

package com.android.settings;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
            implements AppearAnimationCreator<Object>, CredentialCheckResultTracker.Listener {

        // how long we wait to clear a wrong pattern
        private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;

        private static final String FRAGMENT_TAG_CHECK_LOCK_RESULT = "check_lock_result";

        private LockPatternView mLockPatternView;
        private LockPatternUtils mLockPatternUtils;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private CredentialCheckResultTracker mCredentialCheckResultTracker;
        private boolean mDisappearing = false;
        private CountDownTimer mCountdownTimer;

        private TextView mHeaderTextView;
        private TextView mDetailsTextView;
        private TextView mErrorTextView;
        private View mLeftSpacerLandscape;
        private View mRightSpacerLandscape;

        // caller-supplied text for various prompts
        private CharSequence mHeaderText;
        private CharSequence mDetailsText;

        private AppearAnimationUtils mAppearAnimationUtils;
        private DisappearAnimationUtils mDisappearAnimationUtils;

        private int mEffectiveUserId;

        // required constructor for fragments
        public ConfirmLockPatternFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mLockPatternUtils = new LockPatternUtils(getActivity());
            mEffectiveUserId = Utils.getEffectiveUserId(getActivity());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.confirm_lock_pattern, null);
            mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            mLockPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
            mDetailsTextView = (TextView) view.findViewById(R.id.detailsText);
            mErrorTextView = (TextView) view.findViewById(R.id.errorText);
            mLeftSpacerLandscape = view.findViewById(R.id.leftSpacer);
            mRightSpacerLandscape = view.findViewById(R.id.rightSpacer);

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
            }

            mLockPatternView.setTactileFeedbackEnabled(
                    mLockPatternUtils.isTactileFeedbackEnabled());
            mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled(
                    mEffectiveUserId));
            mLockPatternView.setOnPatternListener(mConfirmExistingLockPatternListener);
            updateStage(Stage.NeedToUnlock);

            if (savedInstanceState == null) {
                // on first launch, if no lock pattern is set, then finish with
                // success (don't want user to get stuck confirming something that
                // doesn't exist).
                if (!mLockPatternUtils.isLockPatternEnabled(mEffectiveUserId)) {
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
            setAccessibilityTitle(mHeaderTextView.getText());

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
        protected int getMetricsCategory() {
            return MetricsLogger.CONFIRM_LOCK_PATTERN;
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
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            mHeaderTextView.setAlpha(0f);
            mCancelButton.setAlpha(0f);
            mLockPatternView.setAlpha(0f);
            mDetailsTextView.setAlpha(0f);
            mFingerprintIcon.setAlpha(0f);
        }

        private Object[][] getActiveViews() {
            ArrayList<ArrayList<Object>> result = new ArrayList<>();
            result.add(new ArrayList<Object>(Collections.singletonList(mHeaderTextView)));
            result.add(new ArrayList<Object>(Collections.singletonList(mDetailsTextView)));
            if (mCancelButton.getVisibility() == View.VISIBLE) {
                result.add(new ArrayList<Object>(Collections.singletonList(mCancelButton)));
            }
            LockPatternView.CellState[][] cellStates = mLockPatternView.getCellStates();
            for (int i = 0; i < cellStates.length; i++) {
                ArrayList<Object> row = new ArrayList<>();
                for (int j = 0; j < cellStates[i].length; j++) {
                    row.add(cellStates[i][j]);
                }
                result.add(row);
            }
            if (mFingerprintIcon.getVisibility() == View.VISIBLE) {
                result.add(new ArrayList<Object>(Collections.singletonList(mFingerprintIcon)));
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
                        mHeaderTextView.setText(mHeaderText);
                    } else {
                        mHeaderTextView.setText(R.string.lockpassword_confirm_your_pattern_header);
                    }
                    if (mDetailsText != null) {
                        mDetailsTextView.setText(mDetailsText);
                    } else {
                        mDetailsTextView.setText(
                                R.string.lockpassword_confirm_your_pattern_generic);
                    }
                    mErrorTextView.setText("");

                    mLockPatternView.setEnabled(true);
                    mLockPatternView.enableInput();
                    mLockPatternView.clearPattern();
                    break;
                case NeedToUnlockWrong:
                    mErrorTextView.setText(R.string.lockpattern_need_to_unlock_wrong);

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
            mHeaderTextView.announceForAccessibility(mHeaderTextView.getText());
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
            mLockPatternView.postDelayed(mClearPatternRunnable, WRONG_PATTERN_CLEAR_TIMEOUT_MS);
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

            if (getActivity().getThemeResId() == R.style.Theme_ConfirmDeviceCredentialsDark) {
                mLockPatternView.clearPattern();
                mDisappearAnimationUtils.startAnimation2d(getActiveViews(),
                        new Runnable() {
                            @Override
                            public void run() {
                                // Bail if there is no active activity.
                                if (getActivity() == null || getActivity().isFinishing()) {
                                    return;
                                }

                                getActivity().setResult(RESULT_OK, intent);
                                getActivity().finish();
                                getActivity().overridePendingTransition(
                                        R.anim.confirm_credential_close_enter,
                                        R.anim.confirm_credential_close_exit);
                            }
                        }, this);
            } else {
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            }
        }

        @Override
        public void onFingerprintIconVisibilityChanged(boolean visible) {
            if (mLeftSpacerLandscape != null && mRightSpacerLandscape != null) {

                // In landscape, adjust spacing depending on fingerprint icon visibility.
                mLeftSpacerLandscape.setVisibility(visible ? View.GONE : View.VISIBLE);
                mRightSpacerLandscape.setVisibility(visible ? View.GONE : View.VISIBLE);
            }
        }

        /**
         * The pattern listener that responds according to a user confirming
         * an existing lock pattern.
         */
        private LockPatternView.OnPatternListener mConfirmExistingLockPatternListener
                = new LockPatternView.OnPatternListener()  {

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

                final boolean verifyChallenge = getActivity().getIntent().getBooleanExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);
                Intent intent = new Intent();
                if (verifyChallenge) {
                    if (isInternalActivity()) {
                        startVerifyPattern(pattern, intent);
                        return;
                    }
                } else {
                    startCheckPattern(pattern, intent);
                    return;
                }

                mCredentialCheckResultTracker.setResult(false, intent, 0, mEffectiveUserId);
            }

            private boolean isInternalActivity() {
                return getActivity() instanceof ConfirmLockPattern.InternalActivity;
            }

            private void startVerifyPattern(final List<LockPatternView.Cell> pattern,
                    final Intent intent) {
                final int localEffectiveUserId = mEffectiveUserId;
                long challenge = getActivity().getIntent().getLongExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);
                mPendingLockCheck = LockPatternChecker.verifyPattern(
                        mLockPatternUtils,
                        pattern,
                        challenge,
                        localEffectiveUserId,
                        new LockPatternChecker.OnVerifyCallback() {
                            @Override
                            public void onVerified(byte[] token, int timeoutMs) {
                                mPendingLockCheck = null;
                                boolean matched = false;
                                if (token != null) {
                                    matched = true;
                                    intent.putExtra(
                                            ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                                            token);
                                }
                                mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs,
                                        localEffectiveUserId);
                            }
                        });
            }

            private void startCheckPattern(final List<LockPatternView.Cell> pattern,
                    final Intent intent) {
                if (pattern.size() < LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                    mCredentialCheckResultTracker.setResult(false, intent, 0, mEffectiveUserId);
                    return;
                }

                final int localEffectiveUserId = mEffectiveUserId;
                mPendingLockCheck = LockPatternChecker.checkPattern(
                        mLockPatternUtils,
                        pattern,
                        localEffectiveUserId,
                        new LockPatternChecker.OnCheckCallback() {
                            @Override
                            public void onChecked(boolean matched, int timeoutMs) {
                                mPendingLockCheck = null;
                                if (matched && isInternalActivity()) {
                                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_TYPE,
                                                    StorageManager.CRYPT_TYPE_PATTERN);
                                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD,
                                                    LockPatternUtils.patternToString(pattern));
                                }
                                mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs,
                                        localEffectiveUserId);
                            }
                        });
            }
        };

        private void onPatternChecked(boolean matched, Intent intent, int timeoutMs,
                int effectiveUserId) {
            mLockPatternView.setEnabled(true);
            if (matched) {
                startDisappearAnimation(intent);
            } else {
                if (timeoutMs > 0) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline(
                            effectiveUserId, timeoutMs);
                    handleAttemptLockout(deadline);
                } else {
                    updateStage(Stage.NeedToUnlockWrong);
                    postClearPatternRunnable();
                }
            }
        }

        @Override
        public void onCredentialChecked(boolean matched, Intent intent, int timeoutMs,
                int effectiveUserId) {
            onPatternChecked(matched, intent, timeoutMs, effectiveUserId);
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
    }
}

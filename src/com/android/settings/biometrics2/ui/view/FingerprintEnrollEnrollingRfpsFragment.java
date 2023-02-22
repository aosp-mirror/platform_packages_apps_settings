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

package com.android.settings.biometrics2.ui.view;

import static android.hardware.fingerprint.FingerprintManager.ENROLL_ENROLL;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.biometrics.fingerprint.FingerprintErrorDialog;
import com.android.settings.biometrics2.ui.model.EnrollmentProgress;
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/**
 * Fragment is used to handle enrolling process for rfps
 */
public class FingerprintEnrollEnrollingRfpsFragment extends Fragment {

    private static final String TAG = FingerprintEnrollEnrollingRfpsFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int PROGRESS_BAR_MAX = 10000;
    private static final long ANIMATION_DURATION = 250L;
    private static final long ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN = 500;
    private static final int ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3;

    /**
     * If we don't see progress during this time, we show an error message to remind the users that
     * they need to lift the finger and touch again.
     */
    private static final int HINT_TIMEOUT_DURATION = 2500;

    private FingerprintEnrollEnrollingViewModel mEnrollingViewModel;
    private FingerprintEnrollProgressViewModel mProgressViewModel;

    private Interpolator mFastOutSlowInInterpolator;
    private Interpolator mLinearOutSlowInInterpolator;
    private Interpolator mFastOutLinearInInterpolator;
    private boolean mAnimationCancelled;

    private GlifLayout mView;
    private ProgressBar mProgressBar;
    private ObjectAnimator mProgressAnim;
    private TextView mErrorText;
    private AnimatedVectorDrawable mIconAnimationDrawable;
    private AnimatedVectorDrawable mIconBackgroundBlinksDrawable;
    private int mIconTouchCount;

    private final View.OnClickListener mOnSkipClickListener = v -> {
        mEnrollingViewModel.setOnSkipPressed();
        cancelEnrollment();
    };

    private final Observer<EnrollmentProgress> mProgressObserver = progress -> {
        if (DEBUG) {
            Log.d(TAG, "mProgressObserver(" + progress + ")");
        }
        if (progress != null && progress.getSteps() >= 0) {
            onEnrollmentProgressChange(progress);
        }
    };

    private final Observer<EnrollmentStatusMessage> mHelpMessageObserver = helpMessage -> {
        if (DEBUG) {
            Log.d(TAG, "mHelpMessageObserver(" + helpMessage + ")");
        }
        if (helpMessage != null) {
            onEnrollmentHelp(helpMessage);
        }
    };

    private final Observer<EnrollmentStatusMessage> mErrorMessageObserver = errorMessage -> {
        if (DEBUG) {
            Log.d(TAG, "mErrorMessageObserver(" + errorMessage + ")");
        }
        if (errorMessage != null) {
            onEnrollmentError(errorMessage);
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        final FragmentActivity activity = getActivity();
        final ViewModelProvider provider = new ViewModelProvider(activity);
        mEnrollingViewModel = provider.get(FingerprintEnrollEnrollingViewModel.class);
        mProgressViewModel = provider.get(FingerprintEnrollProgressViewModel.class);
        super.onAttach(context);
        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setEnabled(false);
                mEnrollingViewModel.setOnBackPressed();
                cancelEnrollment();
            }
        });
    }

    @Nullable
    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (enter && nextAnim == R.anim.sud_slide_next_in) {
            final Animation animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
            if (animation != null) {
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mAnimationCancelled = false;
                        startIconAnimation();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                return animation;
            }
        }
        return super.onCreateAnimation(transit, enter, nextAnim);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEnrollingViewModel.restoreSavedState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        mEnrollingViewModel.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = initRfpsLayout(inflater, container);
        return mView;
    }

    private GlifLayout initRfpsLayout(LayoutInflater inflater, ViewGroup container) {
        final GlifLayout containView = (GlifLayout) inflater.inflate(
                R.layout.fingerprint_enroll_enrolling, container, false);

        final Activity activity = getActivity();
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(activity, containView);
        glifLayoutHelper.setDescriptionText(getString(
                R.string.security_settings_fingerprint_enroll_start_message));
        glifLayoutHelper.setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title);

        mErrorText = containView.findViewById(R.id.error_text);
        mProgressBar = containView.findViewById(R.id.fingerprint_progress_bar);
        containView.getMixin(FooterBarMixin.class).setSecondaryButton(
                new FooterButton.Builder(activity)
                        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
                        .setListener(mOnSkipClickListener)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        final LayerDrawable fingerprintDrawable = (LayerDrawable) mProgressBar.getBackground();
        mIconAnimationDrawable = (AnimatedVectorDrawable)
                fingerprintDrawable.findDrawableByLayerId(R.id.fingerprint_animation);
        mIconBackgroundBlinksDrawable = (AnimatedVectorDrawable)
                fingerprintDrawable.findDrawableByLayerId(R.id.fingerprint_background);
        mIconAnimationDrawable.registerAnimationCallback(mIconAnimationCallback);

        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                activity, android.R.interpolator.fast_out_slow_in);
        mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                activity, android.R.interpolator.linear_out_slow_in);
        mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(
                activity, android.R.interpolator.fast_out_linear_in);

        mProgressBar.setProgressBackgroundTintMode(PorterDuff.Mode.SRC);
        mProgressBar.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mIconTouchCount++;
                if (mIconTouchCount == ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN) {
                    showIconTouchDialog();
                } else {
                    mProgressBar.postDelayed(mShowDialogRunnable,
                            ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN);
                }
            } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL
                    || event.getActionMasked() == MotionEvent.ACTION_UP) {
                mProgressBar.removeCallbacks(mShowDialogRunnable);
            }
            return true;
        });

        return containView;
    }

    @Override
    public void onStart() {
        super.onStart();
        startEnrollment();
        updateProgress(false /* animate */, mProgressViewModel.getProgressLiveData().getValue());
        updateTitleAndDescription();
    }

    private void startIconAnimation() {
        if (mIconAnimationDrawable != null) {
            mIconAnimationDrawable.start();
        }
    }

    private void stopIconAnimation() {
        mAnimationCancelled = true;
        if (mIconAnimationDrawable != null) {
            mIconAnimationDrawable.stop();
        }
    }

    @Override
    public void onStop() {
        stopIconAnimation();
        removeEnrollmentObservers();
        if (!getActivity().isChangingConfigurations() && mProgressViewModel.isEnrolling()) {
            mProgressViewModel.cancelEnrollment();
        }
        super.onStop();
    }

    private void removeEnrollmentObservers() {
        preRemoveEnrollmentObservers();
        mProgressViewModel.getErrorMessageLiveData().removeObserver(mErrorMessageObserver);
    }

    private void preRemoveEnrollmentObservers() {
        mProgressViewModel.getProgressLiveData().removeObserver(mProgressObserver);
        mProgressViewModel.getHelpMessageLiveData().removeObserver(mHelpMessageObserver);
    }

    private void cancelEnrollment() {
        preRemoveEnrollmentObservers();
        mProgressViewModel.cancelEnrollment();
    }

    private void startEnrollment() {
        final boolean startResult = mProgressViewModel.startEnrollment(ENROLL_ENROLL);
        if (!startResult) {
            Log.e(TAG, "startEnrollment(), failed");
        }
        mProgressViewModel.getProgressLiveData().observe(this, mProgressObserver);
        mProgressViewModel.getHelpMessageLiveData().observe(this, mHelpMessageObserver);
        mProgressViewModel.getErrorMessageLiveData().observe(this, mErrorMessageObserver);
    }

    private void onEnrollmentHelp(@NonNull EnrollmentStatusMessage helpMessage) {
        final CharSequence helpStr = helpMessage.getStr();
        if (!TextUtils.isEmpty(helpStr)) {
            mErrorText.removeCallbacks(mTouchAgainRunnable);
            showError(helpStr);
        }
    }

    private void onEnrollmentError(@NonNull EnrollmentStatusMessage errorMessage) {
        stopIconAnimation();
        removeEnrollmentObservers();

        if (mEnrollingViewModel.getOnBackPressed()
                && errorMessage.getMsgId() == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
            mEnrollingViewModel.onCancelledDueToOnBackPressed();
        } else if (mEnrollingViewModel.getOnSkipPressed()
                && errorMessage.getMsgId() == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
            mEnrollingViewModel.onCancelledDueToOnSkipPressed();
        } else {
            final int errMsgId = errorMessage.getMsgId();
            mEnrollingViewModel.showErrorDialog(
                    new FingerprintEnrollEnrollingViewModel.ErrorDialogData(
                            mView.getContext().getString(
                                    FingerprintErrorDialog.getErrorMessage(errMsgId)),
                            mView.getContext().getString(
                                    FingerprintErrorDialog.getErrorTitle(errMsgId)),
                            errMsgId
                    ));
            mProgressViewModel.cancelEnrollment();
        }
    }

    private void onEnrollmentProgressChange(@NonNull EnrollmentProgress progress) {
        updateProgress(true /* animate */, progress);
        updateTitleAndDescription();
        animateFlash();
        mErrorText.removeCallbacks(mTouchAgainRunnable);
        mErrorText.postDelayed(mTouchAgainRunnable, HINT_TIMEOUT_DURATION);
    }

    private void updateProgress(boolean animate, @NonNull EnrollmentProgress enrollmentProgress) {
        if (!mProgressViewModel.isEnrolling()) {
            Log.d(TAG, "Enrollment not started yet");
            return;
        }

        final int progress = getProgress(enrollmentProgress);
        // Only clear the error when progress has been made.
        // TODO (b/234772728) Add tests.
        if (mProgressBar != null && mProgressBar.getProgress() < progress) {
            clearError();
        }

        if (animate) {
            animateProgress(progress);
        } else {
            if (mProgressBar != null) {
                mProgressBar.setProgress(progress);
            }
            if (progress >= PROGRESS_BAR_MAX) {
                mDelayedFinishRunnable.run();
            }
        }
    }

    private int getProgress(@NonNull EnrollmentProgress progress) {
        if (progress.getSteps() == -1) {
            return 0;
        }
        int displayProgress = Math.max(0, progress.getSteps() + 1 - progress.getRemaining());
        return PROGRESS_BAR_MAX * displayProgress / (progress.getSteps() + 1);
    }

    private void showError(CharSequence error) {
        mErrorText.setText(error);
        if (mErrorText.getVisibility() == View.INVISIBLE) {
            mErrorText.setVisibility(View.VISIBLE);
            mErrorText.setTranslationY(mView.getContext().getResources().getDimensionPixelSize(
                    R.dimen.fingerprint_error_text_appear_distance));
            mErrorText.setAlpha(0f);
            mErrorText.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .setInterpolator(mLinearOutSlowInInterpolator)
                    .start();
        } else {
            mErrorText.animate().cancel();
            mErrorText.setAlpha(1f);
            mErrorText.setTranslationY(0f);
        }
        if (isResumed() && mEnrollingViewModel.isAccessibilityEnabled()) {
            mEnrollingViewModel.vibrateError(getClass().getSimpleName() + "::showError");
        }
    }

    private void clearError() {
        if (mErrorText.getVisibility() == View.VISIBLE) {
            mErrorText.animate()
                    .alpha(0f)
                    .translationY(getResources().getDimensionPixelSize(
                            R.dimen.fingerprint_error_text_disappear_distance))
                    .setDuration(100)
                    .setInterpolator(mFastOutLinearInInterpolator)
                    .withEndAction(() -> mErrorText.setVisibility(View.INVISIBLE))
                    .start();
        }
    }

    private void animateProgress(int progress) {
        if (mProgressAnim != null) {
            mProgressAnim.cancel();
        }
        ObjectAnimator anim = ObjectAnimator.ofInt(mProgressBar, "progress",
                mProgressBar.getProgress(), progress);
        anim.addListener(mProgressAnimationListener);
        anim.setInterpolator(mFastOutSlowInInterpolator);
        anim.setDuration(ANIMATION_DURATION);
        anim.start();
        mProgressAnim = anim;
    }

    private final Runnable mTouchAgainRunnable = new Runnable() {
        @Override
        public void run() {
            // Use mView to getString to prevent activity is missing during rotation
            showError(mView.getContext().getString(
                    R.string.security_settings_fingerprint_enroll_lift_touch_again));
        }
    };

    private void animateFlash() {
        if (mIconBackgroundBlinksDrawable != null) {
            mIconBackgroundBlinksDrawable.start();
        }
    }

    private void updateTitleAndDescription() {
        final EnrollmentProgress progressLiveData =
                mProgressViewModel.getProgressLiveData().getValue();
        new GlifLayoutHelper(getActivity(), mView).setDescriptionText(mView.getContext().getString(
                progressLiveData == null || progressLiveData.getSteps() == -1
                ? R.string.security_settings_fingerprint_enroll_start_message
                : R.string.security_settings_fingerprint_enroll_repeat_message));
    }

    private void showIconTouchDialog() {
        mIconTouchCount = 0;
        mEnrollingViewModel.onIconTouchDialogShow();
    }

    private final Runnable mShowDialogRunnable = () -> showIconTouchDialog();

    private final Animator.AnimatorListener mProgressAnimationListener =
            new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    startIconAnimation();
                }

                @Override
                public void onAnimationRepeat(Animator animation) { }

                @Override
                public void onAnimationEnd(Animator animation) {
                    stopIconAnimation();
                    if (mProgressBar.getProgress() >= PROGRESS_BAR_MAX) {
                        mProgressBar.postDelayed(mDelayedFinishRunnable, ANIMATION_DURATION);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) { }
            };

    // Give the user a chance to see progress completed before jumping to the next stage.
    private final Runnable mDelayedFinishRunnable = () -> mEnrollingViewModel.onEnrollingDone();

    private final Animatable2.AnimationCallback mIconAnimationCallback =
            new Animatable2.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable d) {
                    if (mAnimationCancelled) {
                        return;
                    }

                    // Start animation after it has ended.
                    mProgressBar.post(() -> startIconAnimation());
                }
            };
}

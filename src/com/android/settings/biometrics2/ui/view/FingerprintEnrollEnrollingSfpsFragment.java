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
import android.annotation.RawRes;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
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

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.model.KeyPath;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.template.DescriptionMixin;
import com.google.android.setupdesign.template.HeaderMixin;

/**
 * Fragment is used to handle enrolling process for sfps
 */
public class FingerprintEnrollEnrollingSfpsFragment extends Fragment {

    private static final String TAG = FingerprintEnrollEnrollingSfpsFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final int PROGRESS_BAR_MAX = 10000;
    private static final long ANIMATION_DURATION = 250L;
    private static final long ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN = 500;
    private static final int ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3;

    private static final int STAGE_UNKNOWN = -1;
    private static final int SFPS_STAGE_NO_ANIMATION = 0;
    private static final int SFPS_STAGE_CENTER = 1;
    private static final int SFPS_STAGE_FINGERTIP = 2;
    private static final int SFPS_STAGE_LEFT_EDGE = 3;
    private static final int SFPS_STAGE_RIGHT_EDGE = 4;

    private FingerprintEnrollEnrollingViewModel mEnrollingViewModel;
    private FingerprintEnrollProgressViewModel mProgressViewModel;

    private Interpolator mFastOutSlowInInterpolator;

    private GlifLayout mView;
    private ProgressBar mProgressBar;
    private ObjectAnimator mProgressAnim;

    private LottieAnimationView mIllustrationLottie;

    private boolean mHaveShownSfpsNoAnimationLottie;
    private boolean mHaveShownSfpsCenterLottie;
    private boolean mHaveShownSfpsTipLottie;
    private boolean mHaveShownSfpsLeftEdgeLottie;
    private boolean mHaveShownSfpsRightEdgeLottie;
    private ObjectAnimator mHelpAnimation;
    private int mIconTouchCount;

    private final View.OnClickListener mOnSkipClickListener =
            (v) -> mEnrollingViewModel.onCancelledDueToOnSkipPressed();

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
        mView = initSfpsLayout(inflater, container);
        maybeHideSfpsText(getActivity().getResources().getConfiguration());
        return mView;
    }

    private GlifLayout initSfpsLayout(LayoutInflater inflater, ViewGroup container) {
        final GlifLayout containView = (GlifLayout) inflater.inflate(R.layout.sfps_enroll_enrolling,
                container, false);
        final Activity activity = getActivity();

        new GlifLayoutHelper(activity, containView).setDescriptionText(
                getString(R.string.security_settings_fingerprint_enroll_start_message));

        // setHelpAnimation()
        final float translationX = 40;
        final int duration = 550;
        final RelativeLayout progressLottieLayout = containView.findViewById(R.id.progress_lottie);
        mHelpAnimation = ObjectAnimator.ofFloat(progressLottieLayout,
                "translationX" /* propertyName */,
                0, translationX, -1 * translationX, translationX, 0f);
        mHelpAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        mHelpAnimation.setDuration(duration);
        mHelpAnimation.setAutoCancel(false);

        mIllustrationLottie = containView.findViewById(R.id.illustration_lottie);

        mProgressBar = containView.findViewById(R.id.fingerprint_progress_bar);
        final FooterBarMixin footerBarMixin = containView.getMixin(FooterBarMixin.class);
        footerBarMixin.setSecondaryButton(
                new FooterButton.Builder(activity)
                        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
                        .setListener(mOnSkipClickListener)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                activity, android.R.interpolator.fast_out_slow_in);

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

    @Override
    public void onStop() {
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

    private void configureEnrollmentStage(CharSequence description, @RawRes int lottie) {
        new GlifLayoutHelper(getActivity(), mView).setDescriptionText(description);
        LottieCompositionFactory.fromRawRes(getActivity(), lottie)
                .addListener((c) -> {
                    mIllustrationLottie.setComposition(c);
                    mIllustrationLottie.setVisibility(View.VISIBLE);
                    mIllustrationLottie.playAnimation();
                });
    }

    private int getCurrentSfpsStage() {
        EnrollmentProgress progressLiveData = mProgressViewModel.getProgressLiveData().getValue();

        if (progressLiveData == null) {
            return STAGE_UNKNOWN;
        }

        final int progressSteps = progressLiveData.getSteps() - progressLiveData.getRemaining();
        if (progressSteps < getStageThresholdSteps(0)) {
            return SFPS_STAGE_NO_ANIMATION;
        } else if (progressSteps < getStageThresholdSteps(1)) {
            return SFPS_STAGE_CENTER;
        } else if (progressSteps < getStageThresholdSteps(2)) {
            return SFPS_STAGE_FINGERTIP;
        } else if (progressSteps < getStageThresholdSteps(3)) {
            return SFPS_STAGE_LEFT_EDGE;
        } else {
            return SFPS_STAGE_RIGHT_EDGE;
        }
    }

    private void onEnrollmentHelp(@NonNull EnrollmentStatusMessage helpMessage) {
        final CharSequence helpStr = helpMessage.getStr();
        if (!TextUtils.isEmpty(helpStr)) {
            showError(helpStr);
        }
    }

    private void onEnrollmentError(@NonNull EnrollmentStatusMessage errorMessage) {
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
                            getString(FingerprintErrorDialog.getErrorMessage(errMsgId)),
                            getString(FingerprintErrorDialog.getErrorTitle(errMsgId)),
                            errMsgId
                    ));
            mProgressViewModel.cancelEnrollment();
        }
    }

    private void announceEnrollmentProgress(CharSequence announcement) {
        AccessibilityEvent event = new AccessibilityEvent();
        event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        event.setClassName(getClass().getName());
        event.setPackageName(getClass().getPackageName());
        event.getText().add(announcement);
        mEnrollingViewModel.sendAccessibilityEvent(event);
    }

    private void onEnrollmentProgressChange(@NonNull EnrollmentProgress progress) {
        updateProgress(true /* animate */, progress);
        if (mEnrollingViewModel.isAccessibilityEnabled()) {
            final int percent = (int) (((float) (progress.getSteps() - progress.getRemaining())
                    / (float) progress.getSteps()) * 100);

            CharSequence announcement = getString(
                    R.string.security_settings_sfps_enroll_progress_a11y_message, percent);
            announceEnrollmentProgress(announcement);

            mIllustrationLottie.setContentDescription(
                    getString(R.string.security_settings_sfps_animation_a11y_label, percent)
            );
        }
        updateTitleAndDescription();
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
        mView.setHeaderText(error);
        mView.getHeaderTextView().setContentDescription(error);
        new GlifLayoutHelper(getActivity(), mView).setDescriptionText("");
        if (!mHelpAnimation.isRunning()) {
            mHelpAnimation.start();
        }
        applySfpsErrorDynamicColors(true);
        if (isResumed() && mEnrollingViewModel.isAccessibilityEnabled()) {
            mEnrollingViewModel.vibrateError(getClass().getSimpleName() + "::showError");
        }
    }

    private void clearError() {
        applySfpsErrorDynamicColors(false);
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

    /**
     * Applies dynamic colors corresponding to showing or clearing errors on the progress bar
     * and finger lottie for SFPS
     */
    private void applySfpsErrorDynamicColors(boolean isError) {
        applyProgressBarDynamicColor(isError);
        applyLottieDynamicColor(isError);
    }

    private void applyProgressBarDynamicColor(boolean isError) {
        final Context context = getActivity().getApplicationContext();
        int error_color = context.getColor(R.color.sfps_enrollment_progress_bar_error_color);
        int progress_bar_fill_color = context.getColor(
                R.color.sfps_enrollment_progress_bar_fill_color);
        ColorStateList fillColor = ColorStateList.valueOf(
                isError ? error_color : progress_bar_fill_color);
        mProgressBar.setProgressTintList(fillColor);
        mProgressBar.setProgressTintMode(PorterDuff.Mode.SRC);
        mProgressBar.invalidate();
    }

    private void applyLottieDynamicColor(boolean isError) {
        final Context context = getActivity().getApplicationContext();
        int error_color = context.getColor(R.color.sfps_enrollment_fp_error_color);
        int fp_captured_color = context.getColor(R.color.sfps_enrollment_fp_captured_color);
        int color = isError ? error_color : fp_captured_color;
        mIllustrationLottie.addValueCallback(
                new KeyPath(".blue100", "**"),
                LottieProperty.COLOR_FILTER,
                frameInfo -> new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        );
        mIllustrationLottie.invalidate();
    }

    private int getStageThresholdSteps(int index) {
        final EnrollmentProgress progressLiveData =
                mProgressViewModel.getProgressLiveData().getValue();

        if (progressLiveData == null || progressLiveData.getSteps() == -1) {
            Log.w(TAG, "getStageThresholdSteps: Enrollment not started yet");
            return 1;
        }
        return Math.round(progressLiveData.getSteps()
                * mEnrollingViewModel.getEnrollStageThreshold(index));
    }

    private void updateTitleAndDescription() {
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(getActivity(), mView);
        if (mEnrollingViewModel.isAccessibilityEnabled()) {
            mEnrollingViewModel.clearTalkback();
            glifLayoutHelper.getGlifLayout().getDescriptionTextView().setAccessibilityLiveRegion(
                    View.ACCESSIBILITY_LIVE_REGION_POLITE);
        }
        final int stage = getCurrentSfpsStage();
        if (DEBUG) {
            Log.d(TAG, "updateTitleAndDescription, stage:" + stage
                    + ", noAnimation:" + mHaveShownSfpsNoAnimationLottie
                    + ", center:" + mHaveShownSfpsCenterLottie
                    + ", tip:" + mHaveShownSfpsTipLottie
                    + ", leftEdge:" + mHaveShownSfpsLeftEdgeLottie
                    + ", rightEdge:" + mHaveShownSfpsRightEdgeLottie);
        }
        switch (stage) {
            case SFPS_STAGE_NO_ANIMATION:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_fingerprint_enroll_repeat_title);
                if (!mHaveShownSfpsNoAnimationLottie) {
                    mHaveShownSfpsNoAnimationLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(
                                    R.string.security_settings_sfps_animation_a11y_label,
                                    0
                            )
                    );
                    configureEnrollmentStage(
                            getString(R.string.security_settings_sfps_enroll_start_message),
                            R.raw.sfps_lottie_no_animation
                    );
                }
                break;

            case SFPS_STAGE_CENTER:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_sfps_enroll_finger_center_title);
                if (!mHaveShownSfpsCenterLottie) {
                    mHaveShownSfpsCenterLottie = true;
                    configureEnrollmentStage(
                            getString(R.string.security_settings_sfps_enroll_start_message),
                            R.raw.sfps_lottie_pad_center
                    );
                }
                break;

            case SFPS_STAGE_FINGERTIP:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_sfps_enroll_fingertip_title);
                if (!mHaveShownSfpsTipLottie) {
                    mHaveShownSfpsTipLottie = true;
                    configureEnrollmentStage("", R.raw.sfps_lottie_tip);
                }
                break;

            case SFPS_STAGE_LEFT_EDGE:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_sfps_enroll_left_edge_title);
                if (!mHaveShownSfpsLeftEdgeLottie) {
                    mHaveShownSfpsLeftEdgeLottie = true;
                    configureEnrollmentStage("", R.raw.sfps_lottie_left_edge);
                }
                break;

            case SFPS_STAGE_RIGHT_EDGE:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_sfps_enroll_right_edge_title);
                if (!mHaveShownSfpsRightEdgeLottie) {
                    mHaveShownSfpsRightEdgeLottie = true;
                    configureEnrollmentStage("", R.raw.sfps_lottie_right_edge);
                }
                break;

            case STAGE_UNKNOWN:
            default:
                // Don't use BiometricEnrollBase#setHeaderText, since that invokes setTitle,
                // which gets announced for a11y upon entering the page. For SFPS, we want to
                // announce a different string for a11y upon entering the page.
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_sfps_enroll_find_sensor_title);
                glifLayoutHelper.setDescriptionText(getString(
                        R.string.security_settings_sfps_enroll_start_message));
                final CharSequence description = getString(
                        R.string.security_settings_sfps_enroll_find_sensor_message);
                glifLayoutHelper.getGlifLayout().getHeaderTextView().setContentDescription(
                        description);
                glifLayoutHelper.getActivity().setTitle(description);
                break;

        }
    }

    private void showIconTouchDialog() {
        mIconTouchCount = 0;
        //TODO EnrollingActivity should observe live data and add dialog fragment
        mEnrollingViewModel.onIconTouchDialogShow();
    }

    private final Runnable mShowDialogRunnable = () -> showIconTouchDialog();

    private final Animator.AnimatorListener mProgressAnimationListener =
            new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) { }

                @Override
                public void onAnimationRepeat(Animator animation) { }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mProgressBar.getProgress() >= PROGRESS_BAR_MAX) {
                        mProgressBar.postDelayed(mDelayedFinishRunnable, ANIMATION_DURATION);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) { }
            };

    // Give the user a chance to see progress completed before jumping to the next stage.
    private final Runnable mDelayedFinishRunnable = () -> mEnrollingViewModel.onEnrollingDone();

    private void maybeHideSfpsText(@NonNull Configuration newConfig) {
        final HeaderMixin headerMixin = ((GlifLayout) mView).getMixin(HeaderMixin.class);
        final DescriptionMixin descriptionMixin = ((GlifLayout) mView).getMixin(
                DescriptionMixin.class);
        final boolean isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (isLandscape) {
            headerMixin.setAutoTextSizeEnabled(true);
            headerMixin.getTextView().setMinLines(0);
            headerMixin.getTextView().setMaxLines(10);
            descriptionMixin.getTextView().setMinLines(0);
            descriptionMixin.getTextView().setMaxLines(10);
        } else {
            headerMixin.setAutoTextSizeEnabled(false);
            headerMixin.getTextView().setLines(4);
            // hide the description
            descriptionMixin.getTextView().setLines(0);
        }

    }
}

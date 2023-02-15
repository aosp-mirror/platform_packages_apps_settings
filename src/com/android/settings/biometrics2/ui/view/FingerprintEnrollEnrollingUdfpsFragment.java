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

import android.annotation.RawRes;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.Transition;
import androidx.transition.TransitionSet;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.biometrics2.ui.model.EnrollmentProgress;
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage;
import com.android.settings.biometrics2.ui.viewmodel.DeviceRotationViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel;
import com.android.settingslib.display.DisplayDensityUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieCompositionFactory;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import java.util.Locale;

/**
 * Fragment is used to handle enrolling process for udfps
 */
public class FingerprintEnrollEnrollingUdfpsFragment extends Fragment {

    private static final String TAG = FingerprintEnrollEnrollingUdfpsFragment.class.getSimpleName();

    private static final int PROGRESS_BAR_MAX = 10000;
    private static final long ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN = 500;
    private static final int ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3;
    private static final int HINT_TIMEOUT_DURATION = 2500;

    private static final int STAGE_UNKNOWN = -1;
    private static final int STAGE_CENTER = 0;
    private static final int STAGE_GUIDED = 1;
    private static final int STAGE_FINGERTIP = 2;
    private static final int STAGE_LEFT_EDGE = 3;
    private static final int STAGE_RIGHT_EDGE = 4;

    private FingerprintEnrollEnrollingViewModel mEnrollingViewModel;
    private DeviceRotationViewModel mRotationViewModel;
    private FingerprintEnrollProgressViewModel mProgressViewModel;

    private Interpolator mFastOutSlowInInterpolator;
    private Interpolator mLinearOutSlowInInterpolator;
    private Interpolator mFastOutLinearInInterpolator;
    private boolean mAnimationCancelled;

    private LottieAnimationView mIllustrationLottie;
    private boolean mHaveShownUdfpsTipLottie;
    private boolean mHaveShownUdfpsLeftEdgeLottie;
    private boolean mHaveShownUdfpsRightEdgeLottie;
    private boolean mHaveShownUdfpsCenterLottie;
    private boolean mHaveShownUdfpsGuideLottie;

    private GlifLayout mView;
    private TextView mErrorText;
    private FooterBarMixin mFooterBarMixin;
    private AnimatedVectorDrawable mIconAnimationDrawable;
    private AnimatedVectorDrawable mIconBackgroundBlinksDrawable;

    private boolean mShouldShowLottie;
    private boolean mIsAccessibilityEnabled;

    private final View.OnClickListener mOnSkipClickListener =
            (v) -> mEnrollingViewModel.onCancelledDueToOnSkipPressed();
    private final Observer<EnrollmentProgress> mProgressObserver = progress -> {
        // TODO
    };
    private final Observer<EnrollmentStatusMessage> mHelpMessageObserver = helpMessage -> {
        // TODO
    };
    private final Observer<EnrollmentStatusMessage> mErrorMessageObserver = errorMessage -> {
        // TODO
    };
    private final Observer<Boolean> mAcquireObserver = isAcquiredGood -> {
        // TODO
    };
    private final Observer<Integer> mPointerDownObserver = sensorId -> {
        // TODO
    };
    private final Observer<Integer> mPointerUpObserver = sensorId -> {
        // TODO
    };

    private int mIconTouchCount;

    @Override
    public void onAttach(@NonNull Context context) {
        final FragmentActivity activity = getActivity();
        final ViewModelProvider provider = new ViewModelProvider(activity);
        mEnrollingViewModel = provider.get(FingerprintEnrollEnrollingViewModel.class);
        mRotationViewModel = provider.get(DeviceRotationViewModel.class);
        mProgressViewModel = provider.get(FingerprintEnrollProgressViewModel.class);
        super.onAttach(context);
        final TransitionSet transitionSet = (TransitionSet) getSharedElementEnterTransition();
        if (transitionSet != null) {
            transitionSet.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(@NonNull Transition transition) {

                }

                @Override
                public void onTransitionEnd(@NonNull Transition transition) {
                    transition.removeListener(this);
                    startEnrollment();
                    mAnimationCancelled = false;
                    startIconAnimation();
                }

                @Override
                public void onTransitionCancel(@NonNull Transition transition) {

                }

                @Override
                public void onTransitionPause(@NonNull Transition transition) {

                }

                @Override
                public void onTransitionResume(@NonNull Transition transition) {

                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEnrollingViewModel.restoreSavedState(savedInstanceState);
        mIsAccessibilityEnabled = mEnrollingViewModel.isAccessibilityEnabled();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        mEnrollingViewModel.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = initUdfpsLayout(inflater, container);
        return mView;
    }

    private GlifLayout initUdfpsLayout(LayoutInflater inflater, ViewGroup container) {
        final GlifLayout containView = (GlifLayout) inflater.inflate(
                R.layout.udfps_enroll_enrolling, container, false);

        final int rotation = mRotationViewModel.getLiveData().getValue();
        if (rotation == Surface.ROTATION_90) {
            final boolean isLayoutRtl = (TextUtils.getLayoutDirectionFromLocale(
                    Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL);
            final LinearLayout layoutContainer = containView.findViewById(
                    R.id.layout_container);
            layoutContainer.setPaddingRelative(
                    (int) getResources().getDimension(R.dimen.rotation_90_enroll_padding_start),
                    0,
                    isLayoutRtl ? 0 : (int) getResources().getDimension(
                            R.dimen.rotation_90_enroll_padding_end),
                    0);
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            lp.setMarginEnd((int) getResources().getDimension(
                    R.dimen.rotation_90_enroll_margin_end));
            layoutContainer.setLayoutParams(lp);
            containView.setLayoutParams(lp);
        }

        final Activity activity = getActivity();
        new GlifLayoutHelper(activity, containView).setDescriptionText(
                getString(R.string.security_settings_udfps_enroll_start_message));
        updateTitleAndDescription();

        mShouldShowLottie = shouldShowLottie();
        boolean isLandscape = BiometricUtils.isReverseLandscape(activity)
                || BiometricUtils.isLandscape(activity);
        updateOrientation(containView, (isLandscape
                ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT));

        mErrorText = containView.findViewById(R.id.error_text);
        mFooterBarMixin = containView.getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(activity)
                        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
                        .setListener(mOnSkipClickListener)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                activity, android.R.interpolator.fast_out_slow_in);
        mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                activity, android.R.interpolator.linear_out_slow_in);
        mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(
                activity, android.R.interpolator.fast_out_linear_in);

        return containView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (true /* TODO mRestoring && !mIsCanceled */) {
            startEnrollment();
        }
        updateProgress(false /* animate */, mProgressViewModel.getProgressLiveData().getValue());
        updateTitleAndDescription();
        if (true /* TODO mRestoring */) {
            startIconAnimation();
        }
    }

    @Override
    public void onStop() {
        stopIconAnimation();
        removeEnrollmentObserver();
        if (!getActivity().isChangingConfigurations()) {
            mProgressViewModel.cancelEnrollment();
        }
        super.onStop();
    }

    private void removeEnrollmentObserver() {
        mProgressViewModel.getProgressLiveData().removeObserver(mProgressObserver);
        mProgressViewModel.getHelpMessageLiveData().removeObserver(mHelpMessageObserver);
        mProgressViewModel.getErrorMessageLiveData().removeObserver(mErrorMessageObserver);
        mProgressViewModel.getAcquireLiveData().removeObserver(mAcquireObserver);
        mProgressViewModel.getPointerDownLiveData().removeObserver(mPointerDownObserver);
        mProgressViewModel.getPointerUpLiveData().removeObserver(mPointerUpObserver);
    }

    private void startEnrollment() {
        mProgressViewModel.getProgressLiveData().observe(this, mProgressObserver);
        mProgressViewModel.getHelpMessageLiveData().observe(this, mHelpMessageObserver);
        mProgressViewModel.getErrorMessageLiveData().observe(this, mErrorMessageObserver);
        mProgressViewModel.getAcquireLiveData().observe(this, mAcquireObserver);
        mProgressViewModel.getPointerDownLiveData().observe(this, mPointerDownObserver);
        mProgressViewModel.getPointerUpLiveData().observe(this, mPointerUpObserver);
        mProgressViewModel.startEnrollment(ENROLL_ENROLL);
    }

    private void updateProgress(boolean animate, @NonNull EnrollmentProgress enrollmentProgress) {
        if (!mProgressViewModel.isEnrolling()) {
            Log.d(TAG, "Enrollment not started yet");
            return;
        }

        final int progress = getProgress(enrollmentProgress);

        if (animate) {
            animateProgress(progress);
        } else if (progress >= PROGRESS_BAR_MAX) {
            mDelayedFinishRunnable.run();
        }
    }

    private int getProgress(@NonNull EnrollmentProgress progress) {
        if (progress.getSteps() == -1) {
            return 0;
        }
        int displayProgress = Math.max(0, progress.getSteps() + 1 - progress.getRemaining());
        return PROGRESS_BAR_MAX * displayProgress / (progress.getSteps() + 1);
    }

    @Override
    public void onDestroy() {
        // TODO stopListenOrientationEvent();
        super.onDestroy();
    }

    private void animateProgress(int progress) {
        // UDFPS animations are owned by SystemUI
        if (progress >= PROGRESS_BAR_MAX) {
            // Wait for any animations in SysUI to finish, then proceed to next page
            getActivity().getMainThreadHandler().postDelayed(mDelayedFinishRunnable, 400L);
        }
    }

    private void updateTitleAndDescription() {

        final Activity activity = getActivity();
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(activity, mView);

        switch (getCurrentStage()) {
            case STAGE_CENTER:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_fingerprint_enroll_repeat_title);
                if (mIsAccessibilityEnabled || mIllustrationLottie == null) {
                    glifLayoutHelper.setDescriptionText(getString(
                            R.string.security_settings_udfps_enroll_start_message));
                } else if (!mHaveShownUdfpsCenterLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsCenterLottie = true;
                    // Note: Update string reference when differentiate in between udfps & sfps
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_sfps_enroll_finger_center_title)
                    );
                    configureEnrollmentStage("", R.raw.udfps_center_hint_lottie);
                }
                break;

            case STAGE_GUIDED:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_fingerprint_enroll_repeat_title);
                if (mIsAccessibilityEnabled || mIllustrationLottie == null) {
                    glifLayoutHelper.setDescriptionText(getString(
                            R.string.security_settings_udfps_enroll_repeat_a11y_message));
                } else if (!mHaveShownUdfpsGuideLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsGuideLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_fingerprint_enroll_repeat_message)
                    );
                    // TODO(b/228100413) Could customize guided lottie animation
                    configureEnrollmentStage("", R.raw.udfps_center_hint_lottie);
                }
                break;
            case STAGE_FINGERTIP:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_udfps_enroll_fingertip_title);
                if (!mHaveShownUdfpsTipLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsTipLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_udfps_tip_fingerprint_help)
                    );
                    configureEnrollmentStage("", R.raw.udfps_tip_hint_lottie);
                }
                break;
            case STAGE_LEFT_EDGE:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_udfps_enroll_left_edge_title);
                if (!mHaveShownUdfpsLeftEdgeLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsLeftEdgeLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_udfps_side_fingerprint_help)
                    );
                    configureEnrollmentStage("", R.raw.udfps_left_edge_hint_lottie);
                } else if (mIllustrationLottie == null) {
                    if (isStageHalfCompleted()) {
                        glifLayoutHelper.setDescriptionText(getString(
                                R.string.security_settings_fingerprint_enroll_repeat_message));
                    } else {
                        glifLayoutHelper.setDescriptionText(getString(
                                R.string.security_settings_udfps_enroll_edge_message));
                    }
                }
                break;
            case STAGE_RIGHT_EDGE:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_udfps_enroll_right_edge_title);
                if (!mHaveShownUdfpsRightEdgeLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsRightEdgeLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_udfps_side_fingerprint_help)
                    );
                    configureEnrollmentStage("", R.raw.udfps_right_edge_hint_lottie);

                } else if (mIllustrationLottie == null) {
                    if (isStageHalfCompleted()) {
                        glifLayoutHelper.setDescriptionText(getString(
                                R.string.security_settings_fingerprint_enroll_repeat_message));
                    } else {
                        glifLayoutHelper.setDescriptionText(getString(
                                R.string.security_settings_udfps_enroll_edge_message));
                    }
                }
                break;

            case STAGE_UNKNOWN:
            default:
                // setHeaderText(R.string.security_settings_fingerprint_enroll_udfps_title);
                // Don't use BiometricEnrollBase#setHeaderText, since that invokes setTitle,
                // which gets announced for a11y upon entering the page. For UDFPS, we want to
                // announce a different string for a11y upon entering the page.
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_fingerprint_enroll_udfps_title);
                glifLayoutHelper.setDescriptionText(getString(
                        R.string.security_settings_udfps_enroll_start_message));
                final CharSequence description = getString(
                        R.string.security_settings_udfps_enroll_a11y);
                mView.getHeaderTextView().setContentDescription(description);
                activity.setTitle(description);
                break;

        }
    }

    private boolean shouldShowLottie() {
        DisplayDensityUtils displayDensity = new DisplayDensityUtils(getContext());
        int currentDensityIndex = displayDensity.getCurrentIndexForDefaultDisplay();
        final int currentDensity = displayDensity.getDefaultDisplayDensityValues()
                [currentDensityIndex];
        final int defaultDensity = displayDensity.getDefaultDensityForDefaultDisplay();
        return defaultDensity == currentDensity;
    }

    private void updateOrientation(@NonNull GlifLayout glifLayout, int orientation) {
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE: {
                mIllustrationLottie = null;
                break;
            }
            case Configuration.ORIENTATION_PORTRAIT: {
                if (mShouldShowLottie) {
                    mIllustrationLottie = glifLayout.findViewById(R.id.illustration_lottie);
                }
                break;
            }
            default:
                Log.e(TAG, "Error unhandled configuration change");
                break;
        }
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

    private int getCurrentStage() {
        EnrollmentProgress progressLiveData = mProgressViewModel.getProgressLiveData().getValue();

        if (progressLiveData == null || progressLiveData.getSteps() == -1) {
            return STAGE_UNKNOWN;
        }

        final int progressSteps = progressLiveData.getSteps() - progressLiveData.getRemaining();
        if (progressSteps < getStageThresholdSteps(0)) {
            return STAGE_CENTER;
        } else if (progressSteps < getStageThresholdSteps(1)) {
            return STAGE_GUIDED;
        } else if (progressSteps < getStageThresholdSteps(2)) {
            return STAGE_FINGERTIP;
        } else if (progressSteps < getStageThresholdSteps(3)) {
            return STAGE_LEFT_EDGE;
        } else {
            return STAGE_RIGHT_EDGE;
        }
    }

    private boolean isStageHalfCompleted() {
        EnrollmentProgress progressLiveData = mProgressViewModel.getProgressLiveData().getValue();
        if (progressLiveData == null || progressLiveData.getSteps() == -1) {
            return false;
        }

        final int progressSteps = progressLiveData.getSteps() - progressLiveData.getRemaining();
        int prevThresholdSteps = 0;
        for (int i = 0; i < mEnrollingViewModel.getEnrollStageCount(); i++) {
            final int thresholdSteps = getStageThresholdSteps(i);
            if (progressSteps >= prevThresholdSteps && progressSteps < thresholdSteps) {
                final int adjustedProgress = progressSteps - prevThresholdSteps;
                final int adjustedThreshold = thresholdSteps - prevThresholdSteps;
                return adjustedProgress >= adjustedThreshold / 2;
            }
            prevThresholdSteps = thresholdSteps;
        }

        // After last enrollment step.
        return true;
    }

    private int getStageThresholdSteps(int index) {

        EnrollmentProgress progressLiveData = mProgressViewModel.getProgressLiveData().getValue();

        if (progressLiveData == null || progressLiveData.getSteps() == -1) {
            Log.w(TAG, "getStageThresholdSteps: Enrollment not started yet");
            return 1;
        }
        return Math.round(progressLiveData.getSteps()
                * mEnrollingViewModel.getEnrollStageThreshold(index));
    }

    private void showIconTouchDialog() {
        mIconTouchCount = 0;
        //TODO EnrollingActivity should observe live data and add dialog fragment
        mEnrollingViewModel.onIconTouchDialogShow();
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

    private final Runnable mShowDialogRunnable = new Runnable() {
        @Override
        public void run() {
            showIconTouchDialog();
        }
    };

    // Give the user a chance to see progress completed before jumping to the next stage.
    private final Runnable mDelayedFinishRunnable = new Runnable() {
        @Override
        public void run() {
            /* TODO launchFinish(); */
        }
    };

    private final Animatable2.AnimationCallback mIconAnimationCallback =
            new Animatable2.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable d) {
                    if (mAnimationCancelled) {
                        return;
                    }

                    // Start animation after it has ended.
                    /* TODO check mProgressBar?
                    mProgressBar.post(new Runnable() {
                        @Override
                        public void run() {
                            startIconAnimation();
                        }
                    });
                     */
                }
            };
}

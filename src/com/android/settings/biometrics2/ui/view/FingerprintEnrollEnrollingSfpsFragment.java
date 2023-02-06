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

import android.annotation.RawRes;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.biometrics2.ui.model.EnrollmentProgress;
import com.android.settings.biometrics2.ui.viewmodel.DeviceRotationViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollEnrollingViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel;
import com.android.settingslib.display.DisplayDensityUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieCompositionFactory;
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

    private static final long ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN = 500;
    private static final int ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3;
    private static final int HINT_TIMEOUT_DURATION = 2500;

    private static final int STAGE_UNKNOWN = -1;
    private static final int SFPS_STAGE_NO_ANIMATION = 0;
    private static final int SFPS_STAGE_CENTER = 1;
    private static final int SFPS_STAGE_FINGERTIP = 2;
    private static final int SFPS_STAGE_LEFT_EDGE = 3;
    private static final int SFPS_STAGE_RIGHT_EDGE = 4;

    private FingerprintEnrollEnrollingViewModel mEnrollingViewModel;
    private DeviceRotationViewModel mRotationViewModel;
    private FingerprintEnrollProgressViewModel mProgressViewModel;

    private Interpolator mFastOutSlowInInterpolator;
    private Interpolator mLinearOutSlowInInterpolator;
    private Interpolator mFastOutLinearInInterpolator;
    private boolean mAnimationCancelled;

    private View mView;
    private ProgressBar mProgressBar;
    private TextView mErrorText;
    private FooterBarMixin mFooterBarMixin;
    private AnimatedVectorDrawable mIconAnimationDrawable;
    private AnimatedVectorDrawable mIconBackgroundBlinksDrawable;

    private LottieAnimationView mIllustrationLottie;
    private boolean mShouldShowLottie;
    private boolean mIsAccessibilityEnabled;

    private boolean mHaveShownSfpsNoAnimationLottie;
    private boolean mHaveShownSfpsCenterLottie;
    private boolean mHaveShownSfpsTipLottie;
    private boolean mHaveShownSfpsLeftEdgeLottie;
    private boolean mHaveShownSfpsRightEdgeLottie;

    private final View.OnClickListener mOnSkipClickListener =
            (v) -> mEnrollingViewModel.onSkipButtonClick();

    private int mIconTouchCount;

    @Override
    public void onAttach(@NonNull Context context) {
        final FragmentActivity activity = getActivity();
        final ViewModelProvider provider = new ViewModelProvider(activity);
        mEnrollingViewModel = provider.get(FingerprintEnrollEnrollingViewModel.class);
        mRotationViewModel = provider.get(DeviceRotationViewModel.class);
        mProgressViewModel = provider.get(FingerprintEnrollProgressViewModel.class);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsAccessibilityEnabled = mEnrollingViewModel.isAccessibilityEnabled();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = initSfpsLayout(inflater, container);
        final Configuration config = getActivity().getResources().getConfiguration();
        maybeHideSfpsText(config);
        return mView;
    }

    private View initSfpsLayout(LayoutInflater inflater, ViewGroup container) {
        final View containView = inflater.inflate(R.layout.sfps_enroll_enrolling, container, false);
        final Activity activity = getActivity();
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(activity,
                (GlifLayout) containView);
        glifLayoutHelper.setDescriptionText(
                R.string.security_settings_fingerprint_enroll_start_message);
        updateTitleAndDescription();

        mShouldShowLottie = shouldShowLottie();
        boolean isLandscape = BiometricUtils.isReverseLandscape(activity)
                || BiometricUtils.isLandscape(activity);
        updateOrientation((isLandscape
                ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT));

        mErrorText = containView.findViewById(R.id.error_text);
        mProgressBar = containView.findViewById(R.id.fingerprint_progress_bar);
        mFooterBarMixin = ((GlifLayout) containView).getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(activity)
                        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
                        .setListener(mOnSkipClickListener)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        final LayerDrawable fingerprintDrawable = mProgressBar != null
                ? (LayerDrawable) mProgressBar.getBackground() : null;
        if (fingerprintDrawable != null) {
            mIconAnimationDrawable = (AnimatedVectorDrawable)
                    fingerprintDrawable.findDrawableByLayerId(R.id.fingerprint_animation);
            mIconBackgroundBlinksDrawable = (AnimatedVectorDrawable)
                    fingerprintDrawable.findDrawableByLayerId(R.id.fingerprint_background);
            mIconAnimationDrawable.registerAnimationCallback(mIconAnimationCallback);
        }

        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                activity, android.R.interpolator.fast_out_slow_in);
        mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                activity, android.R.interpolator.linear_out_slow_in);
        mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(
                activity, android.R.interpolator.fast_out_linear_in);

        if (mProgressBar != null) {
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
        }

        return containView;
    }

    private void updateTitleAndDescription() {

        final Activity activity = getActivity();
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(activity,
                (GlifLayout) mView);

        if (mIsAccessibilityEnabled) {
            mEnrollingViewModel.clearTalkback();
            ((GlifLayout) mView).getDescriptionTextView().setAccessibilityLiveRegion(
                    View.ACCESSIBILITY_LIVE_REGION_POLITE);
        }
        switch (getCurrentSfpsStage()) {
            case SFPS_STAGE_NO_ANIMATION:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_fingerprint_enroll_repeat_title);
                if (!mHaveShownSfpsNoAnimationLottie && mIllustrationLottie != null) {
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
                if (!mHaveShownSfpsCenterLottie && mIllustrationLottie != null) {
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
                if (!mHaveShownSfpsTipLottie && mIllustrationLottie != null) {
                    mHaveShownSfpsTipLottie = true;
                    configureEnrollmentStage("", R.raw.sfps_lottie_tip);
                }
                break;

            case SFPS_STAGE_LEFT_EDGE:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_sfps_enroll_left_edge_title);
                if (!mHaveShownSfpsLeftEdgeLottie && mIllustrationLottie != null) {
                    mHaveShownSfpsLeftEdgeLottie = true;
                    configureEnrollmentStage("", R.raw.sfps_lottie_left_edge);
                }
                break;

            case SFPS_STAGE_RIGHT_EDGE:
                glifLayoutHelper.setHeaderText(
                        R.string.security_settings_sfps_enroll_right_edge_title);
                if (!mHaveShownSfpsRightEdgeLottie && mIllustrationLottie != null) {
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
                glifLayoutHelper.setDescriptionText(
                        R.string.security_settings_sfps_enroll_start_message);
                final CharSequence description = getString(
                        R.string.security_settings_sfps_enroll_find_sensor_message);
                ((GlifLayout) mView).getHeaderTextView().setContentDescription(description);
                activity.setTitle(description);
                break;

        }
    }

    private void maybeHideSfpsText(@android.annotation.NonNull Configuration newConfig) {
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

    private int getCurrentSfpsStage() {
        EnrollmentProgress progressLiveData = mProgressViewModel.getProgressLiveData().getValue();

        if (progressLiveData == null || progressLiveData.getSteps() == -1) {
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

    private int getStageThresholdSteps(int index) {

        EnrollmentProgress progressLiveData = mProgressViewModel.getProgressLiveData().getValue();

        if (progressLiveData == null || progressLiveData.getSteps() == -1) {
            Log.w(TAG, "getStageThresholdSteps: Enrollment not started yet");
            return 1;
        }
        return Math.round(progressLiveData.getSteps()
                * mEnrollingViewModel.getEnrollStageThreshold(index));
    }

    private void updateOrientation(int orientation) {
        mIllustrationLottie = mView.findViewById(R.id.illustration_lottie);
    }

    private boolean shouldShowLottie() {
        DisplayDensityUtils displayDensity = new DisplayDensityUtils(getContext());
        int currentDensityIndex = displayDensity.getCurrentIndexForDefaultDisplay();
        final int currentDensity = displayDensity.getDefaultDisplayDensityValues()
                [currentDensityIndex];
        final int defaultDensity = displayDensity.getDefaultDensityForDefaultDisplay();
        return defaultDensity == currentDensity;
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

    private void showIconTouchDialog() {
        mIconTouchCount = 0;
        //TODO EnrollingActivity should observe live data and add dialog fragment
        mEnrollingViewModel.onIconTouchDialogShow();
    }

    private void configureEnrollmentStage(CharSequence description, @RawRes int lottie) {
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(getActivity(),
                (GlifLayout) mView);
        glifLayoutHelper.setDescriptionText(description);
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

    private final Animatable2.AnimationCallback mIconAnimationCallback =
            new Animatable2.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable d) {
                    if (mAnimationCancelled) {
                        return;
                    }

                    // Start animation after it has ended.
                    mProgressBar.post(new Runnable() {
                        @Override
                        public void run() {
                            startIconAnimation();
                        }
                    });
                }
            };
}

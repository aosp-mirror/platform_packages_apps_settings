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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import com.android.settings.biometrics2.ui.widget.UdfpsEnrollView;
import com.android.settingslib.display.DisplayDensityUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieCompositionFactory;

/**
 * Fragment is used to handle enrolling process for udfps
 */
public class FingerprintEnrollEnrollingUdfpsFragment extends Fragment {

    private static final String TAG = FingerprintEnrollEnrollingUdfpsFragment.class.getSimpleName();

    private static final int PROGRESS_BAR_MAX = 10000;

    private static final int STAGE_UNKNOWN = -1;
    private static final int STAGE_CENTER = 0;
    private static final int STAGE_GUIDED = 1;
    private static final int STAGE_FINGERTIP = 2;
    private static final int STAGE_LEFT_EDGE = 3;
    private static final int STAGE_RIGHT_EDGE = 4;

    private FingerprintEnrollEnrollingViewModel mEnrollingViewModel;
    private DeviceRotationViewModel mRotationViewModel;
    private FingerprintEnrollProgressViewModel mProgressViewModel;

    private boolean mAnimationCancelled;

    private LottieAnimationView mIllustrationLottie;
    private boolean mHaveShownUdfpsTipLottie;
    private boolean mHaveShownUdfpsLeftEdgeLottie;
    private boolean mHaveShownUdfpsRightEdgeLottie;
    private boolean mHaveShownUdfpsCenterLottie;
    private boolean mHaveShownUdfpsGuideLottie;

    private RelativeLayout mView;
    private ImageView mIcon;
    private TextView mErrorText;
    private TextView mTitleText;
    private TextView mSubTitleText;
    private Button mSkipBtn;
    private UdfpsEnrollView mUdfpsEnrollView;

    private boolean mShouldShowLottie;
    private boolean mIsAccessibilityEnabled;

    private final View.OnClickListener mOnSkipClickListener =
            (v) -> mEnrollingViewModel.onCancelledDueToOnSkipPressed();

    private Observer<EnrollmentProgress> mProgressObserver = progress -> {
        if (progress != null) {
            onEnrollmentProgressChange(progress);
        }
    };
    private Observer<EnrollmentStatusMessage> mHelpMessageObserver = helpMessage -> {
        if (helpMessage != null) {
            onEnrollmentHelp(helpMessage.getMsgId(), helpMessage.getStr());
        }
    };
    private Observer<EnrollmentStatusMessage> mErrorMessageObserver = errorMessage -> {
        // TODO
    };
    private Observer<Boolean> mAcquireObserver = isAcquiredGood -> {
        if (isAcquiredGood != null) {
            onAcquired(isAcquiredGood);
        }
    };
    private final Observer<Integer> mPointerDownObserver = sensorId -> {
        if (sensorId != null) {
            onPointerDown(sensorId);
        }
    };
    private final Observer<Integer> mPointerUpObserver = sensorId -> {
        if (sensorId != null) {
            onPointerUp(sensorId);
        }
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

    private RelativeLayout initUdfpsLayout(LayoutInflater inflater, ViewGroup container) {
        final RelativeLayout containView = (RelativeLayout) inflater.inflate(
                R.layout.udfps_enroll_enrolling_v2, container, false);

        final Activity activity = getActivity();
        mIcon = containView.findViewById(R.id.sud_layout_icon);
        mTitleText = containView.findViewById(R.id.suc_layout_title);
        mSubTitleText = containView.findViewById(R.id.sud_layout_subtitle);
        mErrorText = containView.findViewById(R.id.error_text);
        mSkipBtn = containView.findViewById(R.id.skip_btn);
        mSkipBtn.setOnClickListener(mOnSkipClickListener);
        mUdfpsEnrollView = containView.findViewById(R.id.udfps_animation_view);
        mUdfpsEnrollView.setSensorProperties(
                mEnrollingViewModel.getFirstFingerprintSensorPropertiesInternal());
        mShouldShowLottie = shouldShowLottie();
        boolean isLandscape = BiometricUtils.isReverseLandscape(activity)
                || BiometricUtils.isLandscape(activity);
        updateOrientation(containView, (isLandscape
                ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT));


        final int rotation = mRotationViewModel.getLiveData().getValue();
        if (rotation == Surface.ROTATION_270) {
            RelativeLayout.LayoutParams iconLP = new RelativeLayout.LayoutParams(-2, -2);
            iconLP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            iconLP.addRule(RelativeLayout.END_OF, R.id.udfps_animation_view);
            iconLP.topMargin = (int) convertDpToPixel(76.64f, activity);
            iconLP.leftMargin = (int) convertDpToPixel(151.54f, activity);
            mIcon.setLayoutParams(iconLP);

            RelativeLayout.LayoutParams titleLP = new RelativeLayout.LayoutParams(-1, -2);
            titleLP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            titleLP.addRule(RelativeLayout.END_OF, R.id.udfps_animation_view);
            titleLP.topMargin = (int) convertDpToPixel(138f, activity);
            titleLP.leftMargin = (int) convertDpToPixel(144f, activity);
            mTitleText.setLayoutParams(titleLP);

            RelativeLayout.LayoutParams subtitleLP = new RelativeLayout.LayoutParams(-1, -2);
            subtitleLP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            subtitleLP.addRule(RelativeLayout.END_OF, R.id.udfps_animation_view);
            subtitleLP.topMargin = (int) convertDpToPixel(198f, activity);
            subtitleLP.leftMargin = (int) convertDpToPixel(144f, activity);
            mSubTitleText.setLayoutParams(subtitleLP);
        } else if (rotation == Surface.ROTATION_90) {
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            RelativeLayout.LayoutParams iconLP = new RelativeLayout.LayoutParams(-2, -2);
            iconLP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            iconLP.addRule(RelativeLayout.ALIGN_PARENT_START);
            iconLP.topMargin = (int) convertDpToPixel(76.64f, activity);
            iconLP.leftMargin = (int) convertDpToPixel(71.99f, activity);
            mIcon.setLayoutParams(iconLP);

            RelativeLayout.LayoutParams titleLP = new RelativeLayout.LayoutParams(
                    metrics.widthPixels / 2, -2);
            titleLP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            titleLP.addRule(RelativeLayout.ALIGN_PARENT_START, R.id.udfps_animation_view);
            titleLP.topMargin = (int) convertDpToPixel(138f, activity);
            titleLP.leftMargin = (int) convertDpToPixel(66f, activity);
            mTitleText.setLayoutParams(titleLP);

            RelativeLayout.LayoutParams subtitleLP = new RelativeLayout.LayoutParams(
                    metrics.widthPixels / 2, -2);
            subtitleLP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            subtitleLP.addRule(RelativeLayout.ALIGN_PARENT_START);
            subtitleLP.topMargin = (int) convertDpToPixel(198f, activity);
            subtitleLP.leftMargin = (int) convertDpToPixel(66f, activity);
            mSubTitleText.setLayoutParams(subtitleLP);
        }

        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            RelativeLayout.LayoutParams skipBtnLP =
                    (RelativeLayout.LayoutParams) mIcon.getLayoutParams();
            skipBtnLP.topMargin = (int) convertDpToPixel(26f, activity);
            skipBtnLP.leftMargin = (int) convertDpToPixel(54f, activity);
            mSkipBtn.requestLayout();
        }
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


        mUdfpsEnrollView.onEnrollmentProgress(enrollmentProgress.getRemaining(),
                enrollmentProgress.getSteps());

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
        switch (getCurrentStage()) {
            case STAGE_CENTER:
                mTitleText.setText(R.string.security_settings_fingerprint_enroll_repeat_title);
                if (mIsAccessibilityEnabled || mIllustrationLottie == null) {
                    mSubTitleText.setText(R.string.security_settings_udfps_enroll_start_message);
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
                mTitleText.setText(R.string.security_settings_fingerprint_enroll_repeat_title);
                if (mIsAccessibilityEnabled || mIllustrationLottie == null) {
                    mSubTitleText.setText(
                            R.string.security_settings_udfps_enroll_repeat_a11y_message);
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
                mTitleText.setText(R.string.security_settings_udfps_enroll_fingertip_title);
                if (!mHaveShownUdfpsTipLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsTipLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_udfps_tip_fingerprint_help)
                    );
                    configureEnrollmentStage("", R.raw.udfps_tip_hint_lottie);
                }
                break;
            case STAGE_LEFT_EDGE:
                mTitleText.setText(R.string.security_settings_udfps_enroll_left_edge_title);
                if (!mHaveShownUdfpsLeftEdgeLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsLeftEdgeLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_udfps_side_fingerprint_help)
                    );
                    configureEnrollmentStage("", R.raw.udfps_left_edge_hint_lottie);
                } else if (mIllustrationLottie == null) {
                    if (isStageHalfCompleted()) {
                        mSubTitleText.setText(
                                R.string.security_settings_fingerprint_enroll_repeat_message);
                    } else {
                        mSubTitleText.setText(R.string.security_settings_udfps_enroll_edge_message);
                    }
                }
                break;
            case STAGE_RIGHT_EDGE:
                mTitleText.setText(R.string.security_settings_udfps_enroll_right_edge_title);
                if (!mHaveShownUdfpsRightEdgeLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsRightEdgeLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_udfps_side_fingerprint_help)
                    );
                    configureEnrollmentStage("", R.raw.udfps_right_edge_hint_lottie);

                } else if (mIllustrationLottie == null) {
                    if (isStageHalfCompleted()) {
                        mSubTitleText.setText(
                                R.string.security_settings_fingerprint_enroll_repeat_message);
                    } else {
                        mSubTitleText.setText(R.string.security_settings_udfps_enroll_edge_message);
                    }
                }
                break;

            case STAGE_UNKNOWN:
            default:
                mTitleText.setText(R.string.security_settings_fingerprint_enroll_udfps_title);
                mSubTitleText.setText(R.string.security_settings_udfps_enroll_start_message);
                final CharSequence description = getString(
                        R.string.security_settings_udfps_enroll_a11y);
                getActivity().setTitle(description);
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

    private void updateOrientation(@NonNull RelativeLayout content, int orientation) {
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE: {
                mIllustrationLottie = null;
                break;
            }
            case Configuration.ORIENTATION_PORTRAIT: {
                if (mShouldShowLottie) {
                    mIllustrationLottie = content.findViewById(R.id.illustration_lottie);
                }
                break;
            }
            default:
                Log.e(TAG, "Error unhandled configuration change");
                break;
        }
    }

    private void startIconAnimation() {
    }

    private void stopIconAnimation() {
        mAnimationCancelled = true;
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
        mSubTitleText.setText(description);
        LottieCompositionFactory.fromRawRes(getActivity(), lottie)
                .addListener((c) -> {
                    mIllustrationLottie.setComposition(c);
                    mIllustrationLottie.setVisibility(View.VISIBLE);
                    mIllustrationLottie.playAnimation();
                });
    }

    private void onEnrollmentProgressChange(@NonNull EnrollmentProgress progress) {
        updateProgress(true /* animate */, progress);

        updateTitleAndDescription();

        if (mIsAccessibilityEnabled) {
            final int steps = progress.getSteps();
            final int remaining = progress.getRemaining();
            final int percent = (int) (((float) (steps - remaining) / (float) steps) * 100);
            CharSequence announcement = getActivity().getString(
                    R.string.security_settings_udfps_enroll_progress_a11y_message, percent);
            mEnrollingViewModel.sendAccessibilityEvent(announcement);
        }

    }

    private void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        if (!TextUtils.isEmpty(helpString)) {
            showError(helpString);
            mUdfpsEnrollView.onEnrollmentHelp();
        }
    }

    private void onAcquired(boolean isAcquiredGood) {
        if (mUdfpsEnrollView != null) {
            mUdfpsEnrollView.onAcquired(isAcquiredGood);
        }
    }

    private void onPointerDown(int sensorId) {
        if (mUdfpsEnrollView != null) {
            mUdfpsEnrollView.onPointerDown(sensorId);
        }
    }

    private void onPointerUp(int sensorId) {
        if (mUdfpsEnrollView != null) {
            mUdfpsEnrollView.onPointerUp(sensorId);
        }
    }

    private void showError(CharSequence error) {
        mTitleText.setText(error);
        mTitleText.setContentDescription(error);
        mSubTitleText.setContentDescription("");
    }

    private float convertDpToPixel(float dp, Context context) {
        float px = dp * getDensity(context);
        return px;
    }

    private float getDensity(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.density;
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

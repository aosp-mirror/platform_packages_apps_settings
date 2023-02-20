/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.hardware.fingerprint.FingerprintManager.ENROLL_FIND_SENSOR;

import android.app.Activity;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.biometrics2.ui.model.EnrollmentProgress;
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage;
import com.android.settings.biometrics2.ui.viewmodel.DeviceFoldedViewModel;
import com.android.settings.biometrics2.ui.viewmodel.DeviceRotationViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel;
import com.android.settingslib.widget.LottieColorUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/**
 * Fragment explaining the side fingerprint sensor location for fingerprint enrollment.
 * It interacts with ProgressViewModel, FoldCallback (for different lottie), and
 * LottieAnimationView.
 * <pre>
 | Has                 | UDFPS | SFPS | Other (Rear FPS) |
 |---------------------|-------|------|------------------|
 | Primary button      | Yes   | No   | No               |
 | Illustration Lottie | Yes   | Yes  | No               |
 | Animation           | No    | No   | Depend on layout |
 | Progress ViewModel  | No    | Yes  | Yes              |
 | Orientation detect  | No    | Yes  | No               |
 | Foldable detect     | No    | Yes  | No               |
 </pre>
 */
public class FingerprintEnrollFindSfpsFragment extends Fragment {

    private static final boolean DEBUG = false;
    private static final String TAG = "FingerprintEnrollFindSfpsFragment";

    private FingerprintEnrollFindSensorViewModel mViewModel;
    private FingerprintEnrollProgressViewModel mProgressViewModel;
    private DeviceRotationViewModel mRotationViewModel;
    private DeviceFoldedViewModel mFoldedViewModel;

    private GlifLayout mView;
    private FooterBarMixin mFooterBarMixin;
    private final OnClickListener mOnSkipClickListener = (v) -> mViewModel.onSkipButtonClick();
    private LottieAnimationView mIllustrationLottie;
    @Surface.Rotation private int mAnimationRotation = -1;

    private final Observer<Integer> mRotationObserver = rotation -> {
        if (DEBUG) {
            Log.d(TAG, "rotationObserver " + rotation);
        }
        if (rotation != null) {
            onRotationChanged(rotation);
        }
    };

    private final Observer<EnrollmentProgress> mProgressObserver = progress -> {
        if (DEBUG) {
            Log.d(TAG, "mProgressObserver(" + progress + ")");
        }
        if (progress != null && !progress.isInitialStep()) {
            stopLookingForFingerprint(true);
        }
    };

    private final Observer<EnrollmentStatusMessage> mLastCancelMessageObserver = errorMessage -> {
        if (DEBUG) {
            Log.d(TAG, "mLastCancelMessageObserver(" + errorMessage + ")");
        }
        if (errorMessage != null) {
            onLastCancelMessage(errorMessage);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        final Context context = inflater.getContext();
        mView = (GlifLayout) inflater.inflate(R.layout.sfps_enroll_find_sensor_layout, container,
                false);
        mIllustrationLottie = mView.findViewById(R.id.illustration_lottie);
        mFooterBarMixin = mView.getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(context)
                        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Activity activity = getActivity();
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(activity, mView);
        glifLayoutHelper.setHeaderText(R.string.security_settings_sfps_enroll_find_sensor_title);
        glifLayoutHelper.setDescriptionText(
                getText(R.string.security_settings_sfps_enroll_find_sensor_message));
        mFooterBarMixin.getSecondaryButton().setOnClickListener(mOnSkipClickListener);
    }

    @Override
    public void onStart() {
        super.onStart();

        final boolean isEnrolling = mProgressViewModel.isEnrolling();
        if (DEBUG) {
            Log.d(TAG, "onStart(), isEnrolling:" + isEnrolling);
        }
        if (!isEnrolling) {
            startLookingForFingerprint();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final LiveData<Integer> rotationLiveData = mRotationViewModel.getLiveData();
        playLottieAnimation(rotationLiveData.getValue());
        rotationLiveData.observe(this, mRotationObserver);
    }

    @Override
    public void onPause() {
        mRotationViewModel.getLiveData().removeObserver(mRotationObserver);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        final boolean isEnrolling = mProgressViewModel.isEnrolling();
        if (DEBUG) {
            Log.d(TAG, "onStop(), isEnrolling:" + isEnrolling);
        }
        if (isEnrolling) {
            stopLookingForFingerprint(false);
        }
    }

    private void startLookingForFingerprint() {
        if (mProgressViewModel.isEnrolling()) {
            Log.d(TAG, "startLookingForFingerprint(), failed because isEnrolling is true before"
                    + " starting");
            return;
        }

        mProgressViewModel.clearProgressLiveData();
        mProgressViewModel.getProgressLiveData().observe(this, mProgressObserver);
        final boolean startResult = mProgressViewModel.startEnrollment(ENROLL_FIND_SENSOR);
        if (!startResult) {
            Log.e(TAG, "startLookingForFingerprint(), failed to start enrollment");
        }
    }

    private void stopLookingForFingerprint(boolean waitForLastCancelErrMsg) {
        if (!mProgressViewModel.isEnrolling()) {
            Log.d(TAG, "stopLookingForFingerprint(), failed because isEnrolling is false before"
                    + " stopping");
            return;
        }

        if (waitForLastCancelErrMsg) {
            mProgressViewModel.clearErrorMessageLiveData(); // Prevent got previous error message
            mProgressViewModel.getErrorMessageLiveData().observe(this,
                    mLastCancelMessageObserver);
        }

        mProgressViewModel.getProgressLiveData().removeObserver(mProgressObserver);
        final boolean cancelResult = mProgressViewModel.cancelEnrollment();
        if (!cancelResult) {
            Log.e(TAG, "stopLookingForFingerprint(), failed to cancel enrollment");
        }
    }

    private void onRotationChanged(@Surface.Rotation int newRotation) {
        if (DEBUG) {
            Log.d(TAG, "onRotationChanged() from " + mAnimationRotation + " to " + newRotation);
        }
        if ((newRotation + 2) % 4 == mAnimationRotation) {
            // Fragment not changed, we just need to play correct rotation animation
            playLottieAnimation(newRotation);
        } else if (newRotation % 2 != mAnimationRotation % 2) {
            // Fragment is going to be recreated, just stopLookingForFingerprint() here.
            stopLookingForFingerprint(true);
        }
    }

    private void onLastCancelMessage(@NonNull EnrollmentStatusMessage errorMessage) {
        if (errorMessage.getMsgId() == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
            final EnrollmentProgress progress = mProgressViewModel.getProgressLiveData().getValue();
            mProgressViewModel.clearProgressLiveData();
            mProgressViewModel.getErrorMessageLiveData().removeObserver(mLastCancelMessageObserver);
            if (progress != null && !progress.isInitialStep()) {
                mViewModel.onStartButtonClick();
            }
        } else {
            Log.e(TAG, "mErrorMessageObserver(" + errorMessage + ")");
        }
    }

    private void playLottieAnimation(@Surface.Rotation int rotation) {
        @RawRes final int animationRawRes = getSfpsLottieAnimationRawRes(rotation);
        if (DEBUG) {
            Log.d(TAG, "play lottie animation " + animationRawRes
                    + ", previous rotation:" + mAnimationRotation + ", new rotation:" + rotation);
        }

        mAnimationRotation = rotation;
        mIllustrationLottie.setAnimation(animationRawRes);
        LottieColorUtils.applyDynamicColors(getActivity(), mIllustrationLottie);
        mIllustrationLottie.setVisibility(View.VISIBLE);
        mIllustrationLottie.playAnimation();
    }

    @RawRes
    private int getSfpsLottieAnimationRawRes(@Surface.Rotation int rotation) {
        final boolean isFolded = !Boolean.FALSE.equals(mFoldedViewModel.getLiveData().getValue());
        switch (rotation) {
            case Surface.ROTATION_90:
                return isFolded ? R.raw.fingerprint_edu_lottie_folded_top_left
                        : R.raw.fingerprint_edu_lottie_portrait_top_left;
            case Surface.ROTATION_180 :
                return isFolded ? R.raw.fingerprint_edu_lottie_folded_bottom_left
                        : R.raw.fingerprint_edu_lottie_landscape_bottom_left;
            case Surface.ROTATION_270 :
                return isFolded ? R.raw.fingerprint_edu_lottie_folded_bottom_right
                        : R.raw.fingerprint_edu_lottie_portrait_bottom_right;
            default :
                return isFolded ? R.raw.fingerprint_edu_lottie_folded_top_right
                        : R.raw.fingerprint_edu_lottie_landscape_top_right;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        final FragmentActivity activity = getActivity();
        final ViewModelProvider provider = new ViewModelProvider(activity);
        mViewModel = provider.get(FingerprintEnrollFindSensorViewModel.class);
        mProgressViewModel = provider.get(FingerprintEnrollProgressViewModel.class);
        mRotationViewModel = provider.get(DeviceRotationViewModel.class);
        mFoldedViewModel = provider.get(DeviceFoldedViewModel.class);
        super.onAttach(context);
    }
}

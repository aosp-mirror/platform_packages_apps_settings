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
import static android.view.View.OnClickListener;

import android.app.Activity;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.android.settings.R;
import com.android.settings.biometrics.fingerprint.FingerprintFindSensorAnimation;
import com.android.settings.biometrics2.ui.model.EnrollmentProgress;
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage;
import com.android.settings.biometrics2.ui.viewmodel.DeviceRotationViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollFindSensorViewModel;
import com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollProgressViewModel;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/**
 * Fragment explaining the side fingerprint sensor location for fingerprint enrollment.
 * It interacts with ProgressViewModel, and FingerprintFindSensorAnimation.
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
public class FingerprintEnrollFindRfpsFragment extends Fragment {

    private static final boolean DEBUG = false;
    private static final String TAG = "FingerprintEnrollFindRfpsFragment";

    private FingerprintEnrollFindSensorViewModel mViewModel;
    private FingerprintEnrollProgressViewModel mProgressViewModel;
    private DeviceRotationViewModel mRotationViewModel;

    private View mView;
    private GlifLayout mGlifLayout;
    private FooterBarMixin mFooterBarMixin;
    private final OnClickListener mOnSkipClickListener = (v) -> mViewModel.onSkipButtonClick();
    @Nullable private FingerprintFindSensorAnimation mAnimation;
    @Surface.Rotation private int mLastRotation = -1;

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
        mView = inflater.inflate(R.layout.fingerprint_enroll_find_sensor, container, false);
        mGlifLayout = mView.findViewById(R.id.setup_wizard_layout);
        mFooterBarMixin = mGlifLayout.getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(context)
                        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );
        View animationView = mView.findViewById(R.id.fingerprint_sensor_location_animation);
        if (animationView instanceof FingerprintFindSensorAnimation) {
            mAnimation = (FingerprintFindSensorAnimation) animationView;
        }
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Activity activity = getActivity();
        final GlifLayoutHelper glifLayoutHelper = new GlifLayoutHelper(activity, mGlifLayout);
        glifLayoutHelper.setHeaderText(
                R.string.security_settings_fingerprint_enroll_find_sensor_title);
        glifLayoutHelper.setDescriptionText(
                getText(R.string.security_settings_fingerprint_enroll_find_sensor_message));
        mFooterBarMixin.getSecondaryButton().setOnClickListener(mOnSkipClickListener);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (DEBUG) {
            Log.d(TAG, "onStart(), start looking for fingerprint, animation exist:"
                    + (mAnimation != null));
        }
        startLookingForFingerprint();
    }

    @Override
    public void onResume() {
        final LiveData<Integer> rotationLiveData = mRotationViewModel.getLiveData();
        mLastRotation = rotationLiveData.getValue();
        rotationLiveData.observe(this, mRotationObserver);

        if (mAnimation != null) {
            if (DEBUG) {
                Log.d(TAG, "onResume(), start animation");
            }
            mAnimation.startAnimation();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mAnimation != null) {
            if (DEBUG) {
                Log.d(TAG, "onPause(), pause animation");
            }
            mAnimation.pauseAnimation();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        final boolean isEnrolling = mProgressViewModel.isEnrolling();
        if (DEBUG) {
            Log.d(TAG, "onStop(), current enrolling: " + isEnrolling + ", animation exist:"
                    + (mAnimation != null));
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

        final boolean startResult = mProgressViewModel.startEnrollment(ENROLL_FIND_SENSOR);
        if (!startResult) {
            Log.e(TAG, "startLookingForFingerprint(), failed to start enrollment");
        }
        mProgressViewModel.getProgressLiveData().observe(this, mProgressObserver);
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
            Log.d(TAG, "onRotationChanged() from " + mLastRotation + " to " + newRotation);
        }
        if (newRotation % 2 != mLastRotation % 2) {
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

    @Override
    public void onDestroy() {
        if (mAnimation != null) {
            if (DEBUG) {
                Log.d(TAG, "onDestroy(), stop animation");
            }
            mAnimation.stopAnimation();
        }
        super.onDestroy();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        final FragmentActivity activity = getActivity();
        final ViewModelProvider provider = new ViewModelProvider(activity);
        mViewModel = provider.get(FingerprintEnrollFindSensorViewModel.class);
        mProgressViewModel = provider.get(FingerprintEnrollProgressViewModel.class);
        mRotationViewModel = provider.get(DeviceRotationViewModel.class);
        super.onAttach(context);
    }
}

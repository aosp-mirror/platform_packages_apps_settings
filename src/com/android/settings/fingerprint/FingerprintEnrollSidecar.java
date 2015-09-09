/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.Fragment;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.InstrumentedFragment;

/**
 * Sidecar fragment to handle the state around fingerprint enrollment.
 */
public class FingerprintEnrollSidecar extends InstrumentedFragment {

    private int mEnrollmentSteps = -1;
    private int mEnrollmentRemaining = 0;
    private Listener mListener;
    private boolean mEnrolling;
    private CancellationSignal mEnrollmentCancel;
    private Handler mHandler = new Handler();
    private byte[] mToken;
    private boolean mDone;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mToken = activity.getIntent().getByteArrayExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mEnrolling) {
            startEnrollment();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!getActivity().isChangingConfigurations()) {
            cancelEnrollment();
        }
    }

    private void startEnrollment() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        mEnrollmentSteps = -1;
        mEnrollmentCancel = new CancellationSignal();
        getActivity().getSystemService(FingerprintManager.class).enroll(mToken, mEnrollmentCancel,
                0 /* flags */, mEnrollmentCallback);
        mEnrolling = true;
    }

    private void cancelEnrollment() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        if (mEnrolling) {
            mEnrollmentCancel.cancel();
            mEnrolling = false;
            mEnrollmentSteps = -1;
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public int getEnrollmentSteps() {
        return mEnrollmentSteps;
    }

    public int getEnrollmentRemaining() {
        return mEnrollmentRemaining;
    }

    public boolean isDone() {
        return mDone;
    }

    private FingerprintManager.EnrollmentCallback mEnrollmentCallback
            = new FingerprintManager.EnrollmentCallback() {

        @Override
        public void onEnrollmentProgress(int remaining) {
            if (mEnrollmentSteps == -1) {
                mEnrollmentSteps = remaining;
            }
            mEnrollmentRemaining = remaining;
            mDone = remaining == 0;
            if (mListener != null) {
                mListener.onEnrollmentProgressChange(mEnrollmentSteps, remaining);
            }
        }

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            if (mListener != null) {
                mListener.onEnrollmentHelp(helpString);
            }
        }

        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            if (mListener != null) {
                mListener.onEnrollmentError(errMsgId, errString);
            }
        }
    };

    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            cancelEnrollment();
        }
    };

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.FINGERPRINT_ENROLL_SIDECAR;
    }

    public interface Listener {
        void onEnrollmentHelp(CharSequence helpString);
        void onEnrollmentError(int errMsgId, CharSequence errString);
        void onEnrollmentProgressChange(int steps, int remaining);
    }
}

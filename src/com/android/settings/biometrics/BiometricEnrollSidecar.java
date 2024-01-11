/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.UserHandle;

import androidx.annotation.Nullable;

import com.android.settings.core.InstrumentedFragment;
import com.android.settings.password.ChooseLockSettingsHelper;

import java.util.ArrayList;

/**
 * Abstract sidecar fragment to handle the state around biometric enrollment. This sidecar manages
 * the state of enrollment throughout the activity lifecycle so the app can continue after an
 * event like rotation.
 */
public abstract class BiometricEnrollSidecar extends InstrumentedFragment {

    public interface Listener {
        void onEnrollmentHelp(int helpMsgId, CharSequence helpString);
        void onEnrollmentError(int errMsgId, CharSequence errString);
        void onEnrollmentProgressChange(int steps, int remaining);
        /**
         * Called when a fingerprint image has been acquired.
         * @param isAcquiredGood whether the fingerprint image was good.
         */
        default void onAcquired(boolean isAcquiredGood) { }
        /**
         * Called when a pointer down event has occurred.
         */
        default void onUdfpsPointerDown(int sensorId) { }
        /**
         * Called when a pointer up event has occurred.
         */
        default void onUdfpsPointerUp(int sensorId) { }

        /**
         * Called when udfps overlay is shown.
         */
        default void onUdfpsOverlayShown() { }
    }

    private int mEnrollmentSteps = -1;
    private int mEnrollmentRemaining = 0;
    private Listener mListener;
    private boolean mEnrolling;
    private Handler mHandler = new Handler();
    private boolean mDone;
    private ArrayList<QueuedEvent> mQueuedEvents;

    protected CancellationSignal mEnrollmentCancel;
    protected byte[] mToken;
    protected int mUserId;

    private abstract class QueuedEvent {
        public abstract void send(Listener listener);
    }

    private class QueuedEnrollmentProgress extends QueuedEvent {
        int enrollmentSteps;
        int remaining;
        public QueuedEnrollmentProgress(int enrollmentSteps, int remaining) {
            this.enrollmentSteps = enrollmentSteps;
            this.remaining = remaining;
        }

        @Override
        public void send(Listener listener) {
            listener.onEnrollmentProgressChange(enrollmentSteps, remaining);
        }
    }

    private class QueuedEnrollmentHelp extends QueuedEvent {
        int helpMsgId;
        CharSequence helpString;
        public QueuedEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            this.helpMsgId = helpMsgId;
            this.helpString = helpString;
        }

        @Override
        public void send(Listener listener) {
            listener.onEnrollmentHelp(helpMsgId, helpString);
        }
    }

    private class QueuedEnrollmentError extends QueuedEvent {
        int errMsgId;
        CharSequence errString;
        public QueuedEnrollmentError(int errMsgId, CharSequence errString) {
            this.errMsgId = errMsgId;
            this.errString = errString;
        }

        @Override
        public void send(Listener listener) {
            listener.onEnrollmentError(errMsgId, errString);
        }
    }

    private class QueuedAcquired extends QueuedEvent {
        private final boolean isAcquiredGood;

        public QueuedAcquired(boolean isAcquiredGood) {
            this.isAcquiredGood = isAcquiredGood;
        }

        @Override
        public void send(Listener listener) {
            listener.onAcquired(isAcquiredGood);
        }
    }

    private class QueuedUdfpsPointerDown extends QueuedEvent {
        private final int sensorId;

        QueuedUdfpsPointerDown(int sensorId) {
            this.sensorId = sensorId;
        }

        @Override
        public void send(Listener listener) {
            listener.onUdfpsPointerDown(sensorId);
        }
    }

    private class QueuedUdfpsPointerUp extends QueuedEvent {
        private final int sensorId;

        QueuedUdfpsPointerUp(int sensorId) {
            this.sensorId = sensorId;
        }

        @Override
        public void send(Listener listener) {
            listener.onUdfpsPointerUp(sensorId);
        }
    }

    private class QueuedUdfpsOverlayShown extends QueuedEvent {
        @Override
        public void send(Listener listener) {
            listener.onUdfpsOverlayShown();
        }
    }

    private final Runnable mTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            cancelEnrollment();
        }
    };

    public BiometricEnrollSidecar() {
        mQueuedEvents = new ArrayList<>();
    }

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
        mUserId = activity.getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.USER_NULL);
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

    protected void startEnrollment() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        mEnrollmentSteps = -1;
        mEnrollmentCancel = new CancellationSignal();
        mEnrolling = true;
    }

    public boolean cancelEnrollment() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        if (mEnrolling) {
            mEnrollmentCancel.cancel();
            mEnrolling = false;
            mEnrollmentSteps = -1;
            return true;
        }
        return false;
    }

    protected void onEnrollmentProgress(int remaining) {
        if (mEnrollmentSteps == -1) {
            mEnrollmentSteps = remaining;
        }
        mEnrollmentRemaining = remaining;
        mDone = remaining == 0;
        if (mListener != null) {
            mListener.onEnrollmentProgressChange(mEnrollmentSteps, remaining);
        } else {
            mQueuedEvents.add(new QueuedEnrollmentProgress(mEnrollmentSteps, remaining));
        }
    }

    protected void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        if (mListener != null) {
            mListener.onEnrollmentHelp(helpMsgId, helpString);
        } else {
            mQueuedEvents.add(new QueuedEnrollmentHelp(helpMsgId, helpString));
        }
    }

    protected void onEnrollmentError(int errMsgId, CharSequence errString) {
        if (mListener != null) {
            mListener.onEnrollmentError(errMsgId, errString);
        } else {
            mQueuedEvents.add(new QueuedEnrollmentError(errMsgId, errString));
        }
        mEnrolling = false;
    }

    protected void onAcquired(boolean isAcquiredGood) {
        if (mListener != null) {
            mListener.onAcquired(isAcquiredGood);
        } else {
            mQueuedEvents.add(new QueuedAcquired(isAcquiredGood));
        }
    }

    protected void onUdfpsPointerDown(int sensorId) {
        if (mListener != null) {
            mListener.onUdfpsPointerDown(sensorId);
        } else {
            mQueuedEvents.add(new QueuedUdfpsPointerDown(sensorId));
        }
    }

    protected void onUdfpsPointerUp(int sensorId) {
        if (mListener != null) {
            mListener.onUdfpsPointerUp(sensorId);
        } else {
            mQueuedEvents.add(new QueuedUdfpsPointerUp(sensorId));
        }
    }

    protected void onUdfpsOverlayShown() {
        if (mListener != null) {
            mListener.onUdfpsOverlayShown();
        } else {
            mQueuedEvents.add(new QueuedUdfpsOverlayShown());
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
        if (mListener != null) {
            for (int i=0; i<mQueuedEvents.size(); i++) {
                QueuedEvent event = mQueuedEvents.get(i);
                event.send(mListener);
            }
            mQueuedEvents.clear();
        }
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

    public boolean isEnrolling() {
        return mEnrolling;
    }
}

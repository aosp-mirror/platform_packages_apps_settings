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

package com.android.settings.network.helper;

import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * A {@link LiveData} as a mapping of voice call state reported from {@link TelephonyCallback}.
 * Only got update when Lifecycle.State is considered as STARTED or RESUMED.
 *
 * {@code null} when status unknown. Other values are TelephonyManager#CALL_STATE_IDLE,
 * TelephonyManager#CALL_STATE_RINGING and TelephonyManager#CALL_STATE_OFFHOOK.
 */
@VisibleForTesting
public class VoiceCallStatus extends LiveData<Integer> {
    private static final String TAG = "VoiceCallStatus";

    @VisibleForTesting
    protected CallStateProducer mCallStateProducer;

    @VisibleForTesting
    protected LifecycleCallbackTelephonyAdapter mAdapter;

    @VisibleForTesting
    protected Consumer<Integer> mLiveDataUpdater = status -> setValue(status);

    /**
     * Constructor
     * @param lifecycle {@link Lifecycle} to monitor
     * @param telephonyManager {@link TelephonyManager} to interact with
     * @param executor {@link Executor} for receiving the notify from telephony framework.
     */
    @VisibleForTesting
    public VoiceCallStatus(@NonNull Lifecycle lifecycle,
            @NonNull TelephonyManager telephonyManager, Executor executor) {
        super();

        mCallStateProducer = new CallStateProducer(this);

        mAdapter = new LifecycleCallbackTelephonyAdapter<Integer>(lifecycle,
                telephonyManager, mCallStateProducer, executor, mLiveDataUpdater) {
            @Override
            public void setCallbackActive(boolean isActive) {
                super.setCallbackActive(isActive);
                if (!isActive) {
                    /**
                     * Set to unknown status when no longer actively monitoring
                     * {@link TelephonyCallback}.
                     */
                    mLiveDataUpdater.accept(null);
                }
            }
        };
    }

    /**
     * An implementation of TelephonyCallback.
     *
     * Status of voice call will be forward to {@link LifecycleCallbackTelephonyAdapter}
     */
    @VisibleForTesting
    protected static class CallStateProducer extends TelephonyCallback
            implements TelephonyCallback.CallStateListener {
        private final VoiceCallStatus mStatus;

        /**
         * Constructor
         * @param status {@link VoiceCallStatus}
         */
        public CallStateProducer(VoiceCallStatus status) {
            mStatus = status;
        }

        @Override
        public void onCallStateChanged(int state) {
            mStatus.mAdapter.postResult(state);
        }
    }
}

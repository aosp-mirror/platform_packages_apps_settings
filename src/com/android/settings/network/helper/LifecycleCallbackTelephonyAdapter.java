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

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * A {@link LifecycleCallbackConverter} for supporting the register/unregister work for
 * {@link TelephonyCallback}.
 */
@VisibleForTesting
public class LifecycleCallbackTelephonyAdapter<T> extends LifecycleCallbackConverter<T> {
    private static final String TAG = "LifecycleCallbackTelephony";

    private final Runnable mRegisterCallback;
    private final Runnable mUnRegisterCallback;

    /**
     * Constructor
     * @param lifecycle {@link Lifecycle} to monitor
     * @param telephonyManager {@link TelephonyManager} to interact with
     * @param telephonyCallback {@link TelephonyCallback}
     * @param executor {@link Executor} for receiving the notify from telephony framework.
     * @param resultCallback for the result from {@link TelephonyCallback}
     */
    @VisibleForTesting
    public LifecycleCallbackTelephonyAdapter(@NonNull Lifecycle lifecycle,
            @NonNull TelephonyManager telephonyManager,
            @NonNull TelephonyCallback telephonyCallback,
            Executor executor, @NonNull Consumer<T> resultCallback) {
        super(lifecycle, resultCallback);

        // Register operation
        mRegisterCallback = () -> {
            telephonyManager.registerTelephonyCallback(executor, telephonyCallback);
        };

        // Un-Register operation
        mUnRegisterCallback = () -> {
            telephonyManager.unregisterTelephonyCallback(telephonyCallback);
        };
    }

    @Override
    public void setCallbackActive(boolean isActive) {
        super.setCallbackActive(isActive);
        Runnable op = (isActive) ? mRegisterCallback : mUnRegisterCallback;
        op.run();
    }
}

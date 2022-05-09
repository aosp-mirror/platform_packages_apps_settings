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

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;

import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * A {@link LifecycleCallbackAdapter} which support carrying a result from any threads back to UI
 * thread through {@link #postResult(T)}.
 *
 * A {@link Consumer<T>} would be invoked from UI thread for further processing on the result.
 *
 * Note: Result not in STARTED or RESUMED stage will be discarded silently.
 *       This is to align with the criteria set within
 *       {@link LifecycleCallbackAdapter#onStateChanged()}.
 */
@VisibleForTesting
public class LifecycleCallbackConverter<T> extends LifecycleCallbackAdapter {
    private static final String TAG = "LifecycleCallbackConverter";

    private final Thread mUiThread;
    private final Consumer<T> mResultCallback;

    /**
     * A record of number of active status change.
     * Even numbers (0, 2, 4, 6 ...) are inactive status.
     * Odd numbers (1, 3, 5, 7 ...) are active status.
     */
    private final AtomicLong mNumberOfActiveStatusChange = new AtomicLong();

    /**
     * Constructor
     *
     * @param lifecycle {@link Lifecycle} to monitor
     * @param resultCallback for further processing the result
     */
    @VisibleForTesting
    @UiThread
    public LifecycleCallbackConverter(
            @NonNull Lifecycle lifecycle, @NonNull Consumer<T> resultCallback) {
        super(lifecycle);
        mUiThread = Thread.currentThread();
        mResultCallback = resultCallback;
    }

    /**
     * Post a result (from any thread) back to UI thread.
     *
     * @param result the object ready to be passed back to {@link Consumer<T>}.
     */
    @AnyThread
    @VisibleForTesting
    public void postResult(T result) {
        /**
         * Since mNumberOfActiveStatusChange only increase, it is a concept of sequence number.
         * Carry it when sending data in between different threads allow to verify if the data
         * has arrived on time. And drop the data when expired.
         */
        long currentNumberOfChange = mNumberOfActiveStatusChange.get();
        if (Thread.currentThread() == mUiThread) {
            dispatchExtResult(currentNumberOfChange, result); // Dispatch directly
        } else {
            postResultToUiThread(currentNumberOfChange, result);
        }
    }

    @AnyThread
    protected void postResultToUiThread(long numberOfStatusChange, T result) {
        ThreadUtils.postOnMainThread(() -> dispatchExtResult(numberOfStatusChange, result));
    }

    @UiThread
    protected void dispatchExtResult(long numberOfStatusChange, T result) {
        /**
         * For a postResult() sending in between different threads, not only create a latency
         * but also enqueued into main UI thread for dispatch.
         *
         * To align behavior within {@link LifecycleCallbackAdapter#onStateChanged()},
         * some checking on both numberOfStatusChange and {@link Lifecycle} status are required.
         */
        if (isActiveStatus(numberOfStatusChange)
                && (numberOfStatusChange == mNumberOfActiveStatusChange.get())
                && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            mResultCallback.accept(result);
        }
    }

    private static final boolean isActiveStatus(long numberOfStatusChange) {
        return ((numberOfStatusChange & 1L) != 0L);
    }

    /* Implementation of LifecycleCallbackAdapter */
    @UiThread
    public boolean isCallbackActive() {
        return isActiveStatus(mNumberOfActiveStatusChange.get());
    }

    /* Implementation of LifecycleCallbackAdapter */
    @UiThread
    public void setCallbackActive(boolean updatedActiveStatus) {
        /**
         * Make sure only increase when active status got changed.
         * This is to implement the definition of mNumberOfActiveStatusChange.
         */
        if (isCallbackActive() != updatedActiveStatus) {
            mNumberOfActiveStatusChange.getAndIncrement();
        }
    }
}

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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link androidx.lifecycle.LifecycleObserver} implementation of adapter over callback.
 *
 * Which including:
 * 1. Request to active callback when Lifecycle.State.STARTED
 * 2. Request to inactive callback when Lifecycle.State.STOPPED
 * 3. Close (no further resume) when Lifecycle.State.DESTROYED
 */
@VisibleForTesting
abstract class LifecycleCallbackAdapter implements LifecycleEventObserver, AutoCloseable {
    private static final String TAG = "LifecycleCallbackAdapter";
    private AtomicReference<Lifecycle> mLifecycle = new AtomicReference<Lifecycle>();

    /**
     * Constructor
     * @param lifecycle {@link Lifecycle} to monitor
     */
    @VisibleForTesting
    protected LifecycleCallbackAdapter(@NonNull Lifecycle lifecycle) {
        mLifecycle.set(lifecycle);
        lifecycle.addObserver(this);
    }

    /**
     * Get {@link Lifecycle} under monitor.
     * @return {@link Lifecycle}. Return {@code null} when closed.
     */
    @VisibleForTesting
    public Lifecycle getLifecycle() {
        return mLifecycle.get();
    }

    /**
     * Check current callback status.
     * @return true when callback is active.
     */
    public abstract boolean isCallbackActive();

    /**
     * Change callback status.
     * @param isActive true to active callback, otherwise inactive.
     */
    public abstract void setCallbackActive(boolean isActive);

    /**
     * Implementation of LifecycleEventObserver.
     */
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        if (mLifecycle.get() == null) {
            return;
        }

        Lifecycle.State state = event.getTargetState();
        boolean expectCallbackActive = state.isAtLeast(Lifecycle.State.STARTED);
        if (expectCallbackActive != isCallbackActive()) {
            setCallbackActive(expectCallbackActive);
        }
        if (state == Lifecycle.State.DESTROYED) {
            close();
        }
    }

    /**
     * Implementation of AutoCloseable.
     */
    @MainThread
    public void close() {
        Lifecycle lifecycle = mLifecycle.getAndSet(null);
        if (lifecycle != null) {
            lifecycle.removeObserver(this);
        }
    }
}

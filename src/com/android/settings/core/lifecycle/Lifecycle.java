/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.core.lifecycle;

import android.annotation.UiThread;

import com.android.settings.core.lifecycle.events.OnDestroy;
import com.android.settings.core.lifecycle.events.OnPause;
import com.android.settings.core.lifecycle.events.OnResume;
import com.android.settings.core.lifecycle.events.OnStart;
import com.android.settings.core.lifecycle.events.OnStop;
import com.android.settings.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dispatcher for lifecycle events.
 */
public class Lifecycle {

    protected final List<LifecycleObserver> mObservers = new ArrayList<>();

    /**
     * Registers a new observer of lifecycle events.
     */
    @UiThread
    public <T extends LifecycleObserver> T addObserver(T observer) {
        ThreadUtils.ensureMainThread();
        mObservers.add(observer);
        return observer;
    }

    public void onStart() {
        for (LifecycleObserver observer : mObservers) {
            if (observer instanceof OnStart) {
                ((OnStart) observer).onStart();
            }
        }
    }

    public void onResume() {
        for (LifecycleObserver observer : mObservers) {
            if (observer instanceof OnResume) {
                ((OnResume) observer).onResume();
            }
        }
    }

    public void onPause() {
        for (LifecycleObserver observer : mObservers) {
            if (observer instanceof OnPause) {
                ((OnPause) observer).onPause();
            }
        }
    }

    public void onStop() {
        for (LifecycleObserver observer : mObservers) {
            if (observer instanceof OnStop) {
                ((OnStop) observer).onStop();
            }
        }
    }

    public void onDestroy() {
        for (LifecycleObserver observer : mObservers) {
            if (observer instanceof OnDestroy) {
                ((OnDestroy) observer).onDestroy();
            }
        }
    }
}

/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.settings.dashboard;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.CountDownLatch;

/**
 * Observer for updating injected dynamic data.
 */
public abstract class DynamicDataObserver extends ContentObserver {

    private Runnable mUpdateRunnable;
    private CountDownLatch mCountDownLatch;
    private boolean mUpdateDelegated;

    protected DynamicDataObserver() {
        super(new Handler(Looper.getMainLooper()));
        mCountDownLatch = new CountDownLatch(1);
        // Load data for the first time
        onDataChanged();
    }

    /** Returns the uri of the callback. */
    public abstract Uri getUri();

    /** Called when data changes. */
    public abstract void onDataChanged();

    /** Calls the runnable to update UI */
    public synchronized void updateUi() {
        mUpdateDelegated = true;
        if (mUpdateRunnable != null) {
            mUpdateRunnable.run();
        }
    }

    /** Returns the count-down latch */
    public CountDownLatch getCountDownLatch() {
        return mCountDownLatch;
    }

    @Override
    public void onChange(boolean selfChange) {
        onDataChanged();
    }

    protected synchronized void post(Runnable runnable) {
        if (mUpdateDelegated) {
            ThreadUtils.postOnMainThread(runnable);
        } else {
            mUpdateRunnable = runnable;
            mCountDownLatch.countDown();
        }
    }
}

/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.Future;

/** A {@link SidecarFragment} which uses an {@link AsyncTask} to perform background work. */
public abstract class AsyncTaskSidecar<Param, Result> extends SidecarFragment {

    private Future<Result> mAsyncTask;

    @Override
    public void onDestroy() {
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true /* mayInterruptIfRunning */);
        }

        super.onDestroy();
    }

    /**
     * Executes the background task.
     *
     * @param param parameters passed in from {@link #run}
     */
    protected abstract Result doInBackground(@Nullable Param param);

    /** Handles the background task's result. */
    protected void onPostExecute(Result result) {}

    /** Runs the sidecar and sets the state to RUNNING. */
    public void run(@Nullable final Param param) {
        setState(State.RUNNING, Substate.UNUSED);

        if (mAsyncTask != null) {
            mAsyncTask.cancel(true /* mayInterruptIfRunning */);
        }

        mAsyncTask =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            Result result = doInBackground(param);
                            ThreadUtils.postOnMainThread(() -> onPostExecute(result));
                        });
    }
}

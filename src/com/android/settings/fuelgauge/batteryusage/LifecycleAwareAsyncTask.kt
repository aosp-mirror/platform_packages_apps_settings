/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage

import android.os.AsyncTask
import androidx.annotation.CallSuper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.android.settingslib.datastore.HandlerExecutor.Companion.main as mainExecutor

/**
 * Lifecycle aware [AsyncTask] to cancel task automatically when [lifecycle] is stopped.
 *
 * Must call [start] instead of [execute] to run the task.
 */
abstract class LifecycleAwareAsyncTask<Result>(private val lifecycle: Lifecycle?) :
    AsyncTask<Void, Void, Result>(), DefaultLifecycleObserver {

    @CallSuper
    override fun onPostExecute(result: Result) {
        lifecycle?.removeObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        cancel(false)
        lifecycle?.removeObserver(this)
    }

    /**
     * Starts the task, which invokes [execute] (cannot override [execute] as it is final).
     *
     * This method is expected to be invoked from main thread but current usage might call from
     * background thread.
     */
    fun start() {
        execute() // expects main thread
        val lifecycle = lifecycle ?: return
        mainExecutor.execute {
            // Status is updated to FINISHED if onPoseExecute happened before. And task is cancelled
            // if lifecycle is stopped.
            if (status == Status.RUNNING && !isCancelled) {
                lifecycle.addObserver(this) // requires main thread
            }
        }
    }
}

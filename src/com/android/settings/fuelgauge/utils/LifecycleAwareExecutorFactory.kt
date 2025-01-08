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

package com.android.settings.fuelgauge.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 *  The factory class to create executors which could bind with an UI page lifecycle and shutdown
 *  automatically when onStop() method was invoked.
 *
 *  NOTE: Creating an executor from an UI page MUST set the lifecycle. Only non-UI jobs can set
 *  the lifecycle to null.
 */
object LifecycleAwareExecutorFactory {

    fun newSingleThreadExecutor(lifecycle: Lifecycle?): ExecutorService {
        return Executors.newSingleThreadExecutor().also { executor ->
            executor.autoShutdown(lifecycle)
        }
    }

    fun newFixedThreadPool(lifecycle: Lifecycle?, nThreads: Int): ExecutorService {
        return Executors.newFixedThreadPool(nThreads).also { executor ->
            executor.autoShutdown(lifecycle)
        }
    }

    fun newCachedThreadPool(lifecycle: Lifecycle?): ExecutorService {
        return Executors.newCachedThreadPool().also { executor ->
            executor.autoShutdown(lifecycle)
        }
    }

    private fun ExecutorService.autoShutdown(lifecycle: Lifecycle?) {
        if (lifecycle == null) {
            return
        }

        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                this@autoShutdown.shutdown()
                owner.lifecycle.removeObserver(this)
            }
        }
        lifecycle.addObserver(observer)
    }
}

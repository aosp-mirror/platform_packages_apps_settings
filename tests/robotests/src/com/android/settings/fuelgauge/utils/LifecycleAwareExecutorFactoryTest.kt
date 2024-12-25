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
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class LifecycleAwareExecutorFactoryTest {
    private val lifecycle = FakeLifecycle(State.CREATED)
    private val lifecycleOwner = FakeLifecycleOwner(lifecycle)

    @Test
    fun newSingleThreadExecutor_addObserver() {
        LifecycleAwareExecutorFactory.newSingleThreadExecutor(lifecycle)

        assertThat(lifecycle.observerList).hasSize(1)
    }

    @Test
    fun newSingleThreadExecutor_autoShutdown() {
        val executorRunningBlocker = CountDownLatch(1)
        val executor = LifecycleAwareExecutorFactory.newSingleThreadExecutor(lifecycle)

        executor.submit {
            executorRunningBlocker.await()
        }
        lifecycle.observerList[0].onStop(lifecycleOwner)

        assertThat(executor.isShutdown).isTrue()
        assertThat(lifecycle.observerList).isEmpty()
    }

    @Test
    fun newFixedThreadPool_addObserver() {
        LifecycleAwareExecutorFactory.newFixedThreadPool(lifecycle, nThreads = 6)

        assertThat(lifecycle.observerList).hasSize(1)
    }

    @Test
    fun newFixedThreadPool_autoShutdown() {
        val executorRunningBlocker = CountDownLatch(1)
        val executor = LifecycleAwareExecutorFactory.newFixedThreadPool(lifecycle, nThreads = 8)

        executor.submit {
            executorRunningBlocker.await()
        }
        lifecycle.observerList[0].onStop(lifecycleOwner)

        assertThat(executor.isShutdown).isTrue()
        assertThat(lifecycle.observerList).isEmpty()
    }

    @Test
    fun newCachedThreadPool_addObserver() {
        LifecycleAwareExecutorFactory.newCachedThreadPool(lifecycle)

        assertThat(lifecycle.observerList).hasSize(1)
    }

    @Test
    fun newCachedThreadPool_autoShutdown() {
        val executorRunningBlocker = CountDownLatch(1)
        val executor = LifecycleAwareExecutorFactory.newCachedThreadPool(lifecycle)

        executor.submit {
            executorRunningBlocker.await()
        }
        lifecycle.observerList[0].onStop(lifecycleOwner)

        assertThat(executor.isShutdown).isTrue()
        assertThat(lifecycle.observerList).isEmpty()
    }

    private class FakeLifecycle(override val currentState: State) : Lifecycle() {
        val observerList = mutableListOf<DefaultLifecycleObserver>()

        override fun addObserver(observer: LifecycleObserver) {
            if (observer is DefaultLifecycleObserver) {
                observerList.add(observer)
            }
        }

        override fun removeObserver(observer: LifecycleObserver) {
            if (observer is DefaultLifecycleObserver) {
                observerList.remove(observer)
            }
        }
    }

    private class FakeLifecycleOwner(override val lifecycle: Lifecycle) : LifecycleOwner
}

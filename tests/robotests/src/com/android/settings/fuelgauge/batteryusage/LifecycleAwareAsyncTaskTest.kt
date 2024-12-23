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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class LifecycleAwareAsyncTaskTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val lifecycle = mock<Lifecycle>()
    private val lifecycleOwner = mock<LifecycleOwner>()

    @Test
    fun addObserver_onPostExecute_onStop() {
        val taskBeginCountDownLatch = CountDownLatch(1)
        val taskEndCountDownLatch = CountDownLatch(1)
        val asyncTask =
            object : LifecycleAwareAsyncTask<Void?>(lifecycle) {
                override fun doInBackground(vararg params: Void): Void? {
                    taskBeginCountDownLatch.await()
                    taskEndCountDownLatch.countDown()
                    return null
                }
            }

        asyncTask.start()
        taskBeginCountDownLatch.countDown()
        verify(lifecycle).addObserver(asyncTask)

        taskEndCountDownLatch.await()
        asyncTask.onStop(lifecycleOwner)
        assertThat(asyncTask.isCancelled).isTrue()
        verify(lifecycle).removeObserver(asyncTask)
    }

    @Test
    fun addObserver_onStop() {
        val executorBlocker = CountDownLatch(1)
        object : LifecycleAwareAsyncTask<Void?>(null) {
                override fun doInBackground(vararg params: Void?): Void? {
                    executorBlocker.await()
                    return null
                }
            }
            .start()

        val asyncTask =
            object : LifecycleAwareAsyncTask<Void?>(lifecycle) {
                override fun doInBackground(vararg params: Void) = null
            }

        asyncTask.start()
        verify(lifecycle).addObserver(asyncTask)

        asyncTask.onStop(lifecycleOwner)
        executorBlocker.countDown()
        assertThat(asyncTask.isCancelled).isTrue()
        verify(lifecycle).removeObserver(asyncTask)
    }

    @Test
    fun onPostExecute_addObserver() {
        val countDownLatch = CountDownLatch(2)
        val observers = mutableListOf<LifecycleObserver>()
        val lifecycle =
            object : Lifecycle() {
                override val currentState: State
                    get() = State.RESUMED

                override fun addObserver(observer: LifecycleObserver) {
                    observers.add(observer)
                }

                override fun removeObserver(observer: LifecycleObserver) {
                    observers.remove(observer)
                    countDownLatch.countDown()
                }
            }
        val asyncTask =
            object : LifecycleAwareAsyncTask<Void?>(lifecycle) {
                override fun doInBackground(vararg params: Void) = null

                override fun maybeAddObserver(lifecycle: Lifecycle) {
                    super.maybeAddObserver(lifecycle)
                    countDownLatch.countDown()
                }
            }

        Thread { asyncTask.start() }.start()
        do {
            instrumentation.waitForIdleSync()
        } while (!countDownLatch.await(100, MILLISECONDS))

        assertThat(observers).isEmpty()
    }

    @Test
    fun onStop_addObserver() {
        val executorBlocker = CountDownLatch(1)
        object : LifecycleAwareAsyncTask<Void?>(null) {
                override fun doInBackground(vararg params: Void?): Void? {
                    executorBlocker.await()
                    return null
                }
            }
            .start()

        val asyncTask =
            object : LifecycleAwareAsyncTask<Void?>(lifecycle) {
                override fun doInBackground(vararg params: Void) = null
            }

        asyncTask.onStop(lifecycleOwner)
        assertThat(asyncTask.isCancelled).isTrue()
        verify(lifecycle).removeObserver(asyncTask)

        asyncTask.start()
        verify(lifecycle, never()).addObserver(asyncTask)
        executorBlocker.countDown()
    }
}

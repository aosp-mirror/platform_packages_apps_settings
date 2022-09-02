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

import static com.google.common.truth.Truth.assertThat;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@RunWith(AndroidJUnit4.class)
public class LifecycleCallbackConverterTest implements LifecycleOwner {

    private final LifecycleRegistry mRegistry = LifecycleRegistry.createUnsafe(this);

    private Object mTestData;
    private TestConsumer mConsumer;
    private LifecycleCallbackConverter<Object> mConverter;

    @Before
    public void setUp() {
        mTestData = new Object();
        mConsumer = new TestConsumer();
    }

    private void initEnvPerTestCase() {
        mConverter = new LifecycleCallbackConverter<Object>(getLifecycle(), mConsumer);
    }

    public Lifecycle getLifecycle() {
        return mRegistry;
    }

    @Test
    public void converter_dropResult_whenInActive() {
        initEnvPerTestCase();
        mConverter.postResult(mTestData);

        assertThat(mConsumer.getCallbackCount()).isEqualTo(0);
    }

    @Test
    public void converter_callbackResult_whenActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        mConverter.postResult(mTestData);
        assertThat(mConsumer.getCallbackCount()).isEqualTo(1);
        assertThat(mConsumer.getData()).isEqualTo(mTestData);
    }

    @Test
    public void converter_dropResult_whenBackToInActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        mConverter.postResult(mTestData);
        assertThat(mConsumer.getCallbackCount()).isEqualTo(0);
    }

    @Test
    public void converter_passResultToUiThread_whenActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        final Phaser phaser = new Phaser(1);
        Thread executionThread = new Thread(() -> {
            mConverter.postResult(phaser);
        });
        executionThread.start();
        phaser.awaitAdvance(0);

        assertThat(mConsumer.getData()).isEqualTo(phaser);
        assertThat(mConsumer.getCallbackCount()).isEqualTo(1);
    }

    public static class TestConsumer implements Consumer<Object> {
        long mNumberOfCallback;
        AtomicReference<Object> mLatestData;

        public TestConsumer() {
            mLatestData = new AtomicReference<Object>();
        }

        public void accept(Object data) {
            mLatestData.set(data);
            mNumberOfCallback ++;
            if ((data != null) && (data instanceof Phaser)) {
                ((Phaser)data).arrive();
            }
        }

        protected long getCallbackCount() {
            return mNumberOfCallback;
        }

        protected Object getData() {
            return mLatestData.get();
        }
    }
}

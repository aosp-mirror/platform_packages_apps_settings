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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.function.Consumer;

@RunWith(AndroidJUnit4.class)
public class LifecycleCallbackIntentReceiverTest implements LifecycleOwner {

    private final LifecycleRegistry mRegistry = LifecycleRegistry.createUnsafe(this);

    private static final String TEST_SCHEDULER_HANDLER = "testScheduler";
    private static final String TEST_INTENT_ACTION = "testAction";
    private static final String TEST_INTENT_PERMISSION = "testPermission";

    private Context mContext;
    private Intent mIntent;
    private IntentFilter mIntentFilter;
    private Handler mHandler;
    private TestConsumer mConsumer;

    private TestObj mTarget;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();

        mIntentFilter = new IntentFilter(TEST_INTENT_ACTION);
        mIntent = new Intent(TEST_INTENT_ACTION);

        HandlerThread thread = new HandlerThread(TEST_SCHEDULER_HANDLER);
        thread.start();

        mHandler = new Handler(thread.getLooper());
        mConsumer = new TestConsumer();
    }

    public Lifecycle getLifecycle() {
        return mRegistry;
    }

    private void initEnvPerTestCase() {
        mTarget = new TestObj(getLifecycle(), mContext,
                mIntentFilter, TEST_INTENT_PERMISSION,
                mHandler, mConsumer);
    }

    @Test
    public void receiver_register_whenActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mTarget.getCallbackActiveCount(true)
                + mTarget.getCallbackActiveCount(false)).isEqualTo(0);

        mTarget.mReceiver.onReceive(mContext, mIntent);

        assertThat(mConsumer.getCallbackCount()).isEqualTo(0);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(mTarget.getCallbackActiveCount(true)).isEqualTo(1);
        assertThat(mConsumer.getCallbackCount()).isEqualTo(0);

        mTarget.mReceiver.onReceive(mContext, mIntent);

        assertThat(mConsumer.getCallbackCount()).isEqualTo(1);
        assertThat(mConsumer.getData()).isEqualTo(mIntent);
    }

    @Test
    public void receiver_unregister_whenInActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        assertThat(mTarget.getCallbackActiveCount(false)).isEqualTo(1);

        mTarget.mReceiver.onReceive(mContext, mIntent);

        assertThat(mConsumer.getCallbackCount()).isEqualTo(0);
    }

    @Test
    public void receiver_register_whenReActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(mTarget.getCallbackActiveCount(true)).isEqualTo(2);

        mTarget.mReceiver.onReceive(mContext, mIntent);

        assertThat(mConsumer.getCallbackCount()).isEqualTo(1);
        assertThat(mConsumer.getData()).isEqualTo(mIntent);
    }

    @Test
    public void receiver_close_whenDestroy() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        assertThat(mTarget.getCallbackActiveCount(false)).isEqualTo(1);

        mTarget.mReceiver.onReceive(mContext, mIntent);

        assertThat(mConsumer.getCallbackCount()).isEqualTo(0);
    }

    public static class TestConsumer implements Consumer<Intent> {
        long mNumberOfCallback;
        Intent mLatestData;

        public TestConsumer() {}

        public void accept(Intent data) {
            mLatestData = data;
            mNumberOfCallback ++;
        }

        protected long getCallbackCount() {
            return mNumberOfCallback;
        }

        protected Intent getData() {
            return mLatestData;
        }
    }

    public static class TestObj extends LifecycleCallbackIntentReceiver {
        long mCallbackActiveCount;
        long mCallbackInActiveCount;

        public TestObj(Lifecycle lifecycle, Context context, IntentFilter filter,
                String broadcastPermission, Handler scheduler, Consumer<Intent> resultCallback) {
            super(lifecycle, context, filter, broadcastPermission, scheduler, resultCallback);
        }

        @Override
        public void setCallbackActive(boolean isActive) {
            if (isActive) {
                mCallbackActiveCount ++;
            } else {
                mCallbackInActiveCount ++;
            }
            super.setCallbackActive(isActive);
        }

        protected long getCallbackActiveCount(boolean forActive) {
            return forActive ? mCallbackActiveCount : mCallbackInActiveCount;
        }
    }
}

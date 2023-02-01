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

@RunWith(AndroidJUnit4.class)
public class LifecycleCallbackAdapterTest implements LifecycleOwner {

    private final LifecycleRegistry mRegistry = LifecycleRegistry.createUnsafe(this);

    private TestObj mTarget;

    @Before
    public void setUp() {
        mTarget = new TestObj(getLifecycle());
    }

    public Lifecycle getLifecycle() {
        return mRegistry;
    }

    @Test
    public void lifecycle_get_lifecycleToMonitor() {
        assertThat(mTarget.getLifecycle()).isEqualTo(mRegistry);
    }

    @Test
    public void lifecycle_stateChangeToStart_callbackActive() {
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mTarget.getCallbackCount()).isEqualTo(0);
        assertThat(mTarget.isCallbackActive()).isEqualTo(Boolean.FALSE);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(mTarget.getCallbackCount()).isEqualTo(1);
        assertThat(mTarget.isCallbackActive()).isEqualTo(Boolean.TRUE);
    }

    @Test
    public void lifecycle_stateChangeToStop_callbackInActive() {
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        assertThat(mTarget.getCallbackCount()).isEqualTo(2);
        assertThat(mTarget.isCallbackActive()).isEqualTo(Boolean.FALSE);
    }

    @Test
    public void lifecycle_stateChangeToDestroy_noFurtherActive() {
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        assertThat(mTarget.getCallbackCount()).isEqualTo(2);
        assertThat(mTarget.isCallbackActive()).isEqualTo(Boolean.FALSE);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        assertThat(mTarget.getCallbackCount()).isEqualTo(2);
        assertThat(mTarget.isCallbackActive()).isEqualTo(Boolean.FALSE);
    }

    public static class TestObj extends LifecycleCallbackAdapter {
        boolean mIsActive;
        int mNumberOfCallback;

        public TestObj(Lifecycle lifecycle) {
            super(lifecycle);
        }

        public boolean isCallbackActive() {
            return mIsActive;
        }

        public void setCallbackActive(boolean isActive) {
            mIsActive = isActive;
            mNumberOfCallback ++;
        }

        protected int getCallbackCount() {
            return mNumberOfCallback;
        }
    }
}

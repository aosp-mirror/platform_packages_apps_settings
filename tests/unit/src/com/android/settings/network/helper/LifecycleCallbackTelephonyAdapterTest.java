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

import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class LifecycleCallbackTelephonyAdapterTest implements LifecycleOwner {

    private final LifecycleRegistry mRegistry = LifecycleRegistry.createUnsafe(this);

    @Mock
    private TelephonyManager mTelMgr;

    private TestCallback mTestCallback;
    private AtomicReference<Object> mResult;
    private LifecycleCallbackTelephonyAdapter<Object> mAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mResult = new AtomicReference<Object>();
        mTestCallback = new TestCallback();

        doNothing().when(mTelMgr).registerTelephonyCallback(null, mTestCallback);
        doNothing().when(mTelMgr).unregisterTelephonyCallback(mTestCallback);
    }

    public Lifecycle getLifecycle() {
        return mRegistry;
    }

    private void initEnvPerTestCase() {
        mAdapter = new LifecycleCallbackTelephonyAdapter<Object>(getLifecycle(), mTelMgr,
                mTestCallback, null, result -> mResult.set(result));
    }

    @Test
    public void telephonyCallback_register_whenActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);

        verify(mTelMgr, never()).registerTelephonyCallback(anyObject(), anyObject());

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        verify(mTelMgr).registerTelephonyCallback(anyObject(), anyObject());
    }

    @Test
    public void telephonyCallback_unregister_whenInActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);

        verify(mTelMgr, never()).unregisterTelephonyCallback(anyObject());

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        verify(mTelMgr, never()).unregisterTelephonyCallback(anyObject());

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        verify(mTelMgr).unregisterTelephonyCallback(anyObject());

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

        verify(mTelMgr, times(1)).unregisterTelephonyCallback(anyObject());
    }

    protected static class TestCallback extends TelephonyCallback
            implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {}
    }
}

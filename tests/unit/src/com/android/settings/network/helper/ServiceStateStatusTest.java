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

import android.telephony.ServiceState;
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
public class ServiceStateStatusTest implements LifecycleOwner {

    private final LifecycleRegistry mRegistry = LifecycleRegistry.createUnsafe(this);

    @Mock
    private TelephonyManager mTelMgr;
    @Mock
    private ServiceState mTestData;

    private AtomicReference<ServiceState> mStatusStorage;
    private ServiceStateStatus mServiceStateStatus;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mStatusStorage = new AtomicReference<ServiceState>();
    }

    private void initEnvPerTestCase() {
        mServiceStateStatus = new ServiceStateStatus(getLifecycle(), mTelMgr, null) {
            @Override
            protected void setValue(ServiceState status) {
                mStatusStorage.set(status);
            }
        };
    }

    public Lifecycle getLifecycle() {
        return mRegistry;
    }

    @Test
    public void telephonyCallback_updateStatus_whenActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);

        mServiceStateStatus.mServiceStateProducer.onServiceStateChanged(mTestData);

        assertThat(mStatusStorage.get()).isEqualTo(null);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        mServiceStateStatus.mServiceStateProducer.onServiceStateChanged(mTestData);

        assertThat(mStatusStorage.get()).isEqualTo(mTestData);
    }

    @Test
    public void telephonyCallback_updateStatusToNull_whenInActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        mServiceStateStatus.mServiceStateProducer.onServiceStateChanged(mTestData);

        assertThat(mStatusStorage.get()).isEqualTo(mTestData);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        assertThat(mStatusStorage.get()).isEqualTo(null);
    }
}

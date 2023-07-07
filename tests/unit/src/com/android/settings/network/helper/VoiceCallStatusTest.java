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
public class VoiceCallStatusTest implements LifecycleOwner {

    private final LifecycleRegistry mRegistry = LifecycleRegistry.createUnsafe(this);

    @Mock
    private TelephonyManager mTelMgr;

    private AtomicReference<Integer> mStatusStorage;
    private VoiceCallStatus mVoiceCallStatus;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mStatusStorage = new AtomicReference<Integer>();
    }

    private void initEnvPerTestCase() {
        mVoiceCallStatus = new VoiceCallStatus(getLifecycle(), mTelMgr, null) {
                //ArchTaskExecutor.getMainThreadExecutor()) {
            @Override
            protected void setValue(Integer status) {
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

        mVoiceCallStatus.mCallStateProducer.onCallStateChanged(
                TelephonyManager.CALL_STATE_RINGING);

        assertThat(mStatusStorage.get()).isEqualTo(null);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        mVoiceCallStatus.mCallStateProducer.onCallStateChanged(
                TelephonyManager.CALL_STATE_OFFHOOK);

        assertThat(mStatusStorage.get()).isEqualTo(TelephonyManager.CALL_STATE_OFFHOOK);
    }

    @Test
    public void telephonyCallback_updateStatusToNull_whenInActive() {
        initEnvPerTestCase();
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);

        mVoiceCallStatus.mCallStateProducer.onCallStateChanged(
                TelephonyManager.CALL_STATE_OFFHOOK);

        assertThat(mStatusStorage.get()).isEqualTo(TelephonyManager.CALL_STATE_OFFHOOK);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

        assertThat(mStatusStorage.get()).isEqualTo(null);
    }
}

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

package com.android.settings.wifi.helper;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.os.HandlerThread;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class WifiTrackerBaseTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    Lifecycle mLifecycle;
    @Mock
    HandlerThread mWorkerThread;

    WifiTrackerBase mWifiTrackerBase;

    @Before
    public void setUp() {
        mWifiTrackerBase = new WifiTrackerBase(mLifecycle, mWorkerThread);
    }

    @Test
    public void constructor_createWorkerThread() {
        mWifiTrackerBase = new WifiTrackerBase(mLifecycle);

        assertThat(mWifiTrackerBase.mWorkerThread).isNotNull();
    }

    @Test
    public void constructor_startWorkerThread() {
        verify(mWorkerThread).start();
    }

    @Test
    public void onDestroy_quitWorkerThread() {
        mWifiTrackerBase.onDestroy(mock(LifecycleOwner.class));

        verify(mWorkerThread).quit();
    }

    @Test
    public void getWorkerThreadHandler_isNotNull() {
        mWifiTrackerBase = new WifiTrackerBase(mLifecycle);

        assertThat(mWifiTrackerBase.getWorkerThreadHandler()).isNotNull();
    }
}

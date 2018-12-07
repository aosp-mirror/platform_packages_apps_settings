/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.datetime;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;

import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TimeChangeListenerMixinTest {

    @Mock
    private UpdateTimeAndDateCallback mCallback;

    private Context mContext;
    private TimeChangeListenerMixin mMixin;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mMixin = new TimeChangeListenerMixin(mContext, mCallback);
    }

    @Test
    public void shouldImplementMixinInterfaces() {
        assertThat(mMixin instanceof LifecycleObserver).isTrue();
        assertThat(mMixin instanceof OnPause).isTrue();
        assertThat(mMixin instanceof OnResume).isTrue();
    }

    @Test
    public void onResume_shouldRegisterIntentFilter() {
        mMixin.onResume();
        mContext.sendBroadcast(new Intent(Intent.ACTION_TIME_TICK));
        mContext.sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED)
                .addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND));
        mContext.sendBroadcast(new Intent(Intent.ACTION_TIMEZONE_CHANGED));

        verify(mCallback, times(3)).updateTimeAndDateDisplay(mContext);
    }

    @Test
    public void onPause_shouldUnregisterIntentFilter() {
        mMixin.onResume();
        mMixin.onPause();
        mContext.sendBroadcast(new Intent(Intent.ACTION_TIME_TICK));
        mContext.sendBroadcast(new Intent(Intent.ACTION_TIME_CHANGED)
                .addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND));
        mContext.sendBroadcast(new Intent(Intent.ACTION_TIMEZONE_CHANGED));

        verify(mCallback, never()).updateTimeAndDateDisplay(mContext);
    }
}

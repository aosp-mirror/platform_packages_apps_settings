/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Looper;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GlobalSettingsChangeListenerTest {

    @Mock
    private Lifecycle mLifecycle;

    private Context mContext;
    private GlobalSettingsChangeListener mListener;

    private static final String SETTINGS_FIELD = Settings.Global.AIRPLANE_MODE_ON;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mListener = spy(new GlobalSettingsChangeListener(Looper.getMainLooper(),
                mContext, SETTINGS_FIELD) {
            public void onChanged(String field) {}
        });

        doNothing().when(mLifecycle).addObserver(mListener);
        doNothing().when(mLifecycle).removeObserver(mListener);
    }

    @Test
    public void whenChanged_onChangedBeenCalled() {
        mListener.onChange(false);
        verify(mListener, times(1)).onChanged(SETTINGS_FIELD);
    }

    @Test
    public void whenNotifyChangeBasedOnLifecycle_onStopEvent_onChangedNotCalled() {
        mListener.notifyChangeBasedOn(mLifecycle);
        mListener.onStart();

        mListener.onChange(false);
        verify(mListener, times(1)).onChanged(SETTINGS_FIELD);

        mListener.onStop();

        mListener.onChange(false);
        verify(mListener, times(1)).onChanged(SETTINGS_FIELD);

        mListener.onStart();

        mListener.onChange(false);
        verify(mListener, times(2)).onChanged(SETTINGS_FIELD);
    }
}

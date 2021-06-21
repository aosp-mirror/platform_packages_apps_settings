/*
 * Copyright (C) 2020 The Android Open Source Project
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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class GlobalSettingsChangeListenerTest {

    @Mock
    private Lifecycle mLifecycle;

    private Context mContext;
    private TestListener mTestListener;

    private static final String SETTINGS_FIELD = Settings.Global.AIRPLANE_MODE_ON;

    public class TestListener extends GlobalSettingsChangeListener {
        TestListener(Looper looper, Context context, String field) {
            super(looper, context, field);
        }
        @Override
        public void onChanged(String field) {}
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mTestListener = spy(new TestListener(Looper.getMainLooper(), mContext, SETTINGS_FIELD));

        doNothing().when(mLifecycle).addObserver(mTestListener);
        doNothing().when(mLifecycle).removeObserver(mTestListener);
    }

    /*
     * The method onChange is override from {@link ContentObserver}, and when the content change
     * occurs will be called. And the method onChanged is subclass from
     * {@link GlobalSettingsChangeListener}, it will be called when the onChange be called.
     */
    @Test
    public void whenChanged_onChangedBeenCalled() {
        mTestListener.onChange(false);

        verify(mTestListener, times(1)).onChanged(SETTINGS_FIELD);
    }

    @Test
    public void whenNotifyChangeBasedOnLifecycle_onStopEvent_onChangedNotCalled() {
        mTestListener.notifyChangeBasedOn(mLifecycle);
        mTestListener.onStart();

        mTestListener.onChange(false);
        verify(mTestListener, times(1)).onChanged(SETTINGS_FIELD);

        mTestListener.onStop();

        mTestListener.onChange(false);
        verify(mTestListener, times(1)).onChanged(SETTINGS_FIELD);

        mTestListener.onStart();

        mTestListener.onChange(false);
        verify(mTestListener, times(2)).onChanged(SETTINGS_FIELD);
    }
}

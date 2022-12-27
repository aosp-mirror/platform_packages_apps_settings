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

package com.android.settings.network.tether;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.net.TetheringManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class TetheringHelperTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    Lifecycle mLifecycle;
    @Mock
    LifecycleOwner mLifecycleOwner;
    @Mock
    TetheringManager mTetheringManager;
    @Mock
    TetheringManager.TetheringEventCallback mTetheringEventCallback;
    @Mock
    List<String> mInterfaces;

    TetheringHelper mHelper;

    @Before
    public void setUp() {
        mHelper = TetheringHelper.getInstance(mContext, mTetheringEventCallback, mLifecycle);
        mHelper.sTetheringManager = mTetheringManager;
    }

    @Test
    public void constructor_propertiesShouldBeReady() {
        assertThat(mHelper.sAppContext).isNotNull();
        assertThat(mHelper.sTetheringManager).isNotNull();
        assertThat(mHelper.mCallback).isNotNull();
        verify(mLifecycle).addObserver(any());
    }

    @Test
    public void getTetheringManager_isNotNull() {
        assertThat(mHelper.getTetheringManager()).isNotNull();
    }

    @Test
    public void onStart_eventCallbacksClear_addAndRegisterCallback() {
        mHelper.sEventCallbacks.clear();

        mHelper.onStart(mLifecycleOwner);

        assertThat(mHelper.sEventCallbacks.contains(mTetheringEventCallback)).isTrue();
        assertThat(mHelper.sEventCallbacks.size()).isEqualTo(1);
        verify(mTetheringManager).registerTetheringEventCallback(any(Executor.class),
                eq(TetheringHelper.sTetheringEventCallback));
    }

    @Test
    public void onStart_eventCallbacksContains_doNotRegisterCallback() {
        mHelper.sEventCallbacks.clear();
        mHelper.sEventCallbacks.add(mTetheringEventCallback);

        mHelper.onStart(mLifecycleOwner);

        verify(mTetheringManager, never()).registerTetheringEventCallback(any(Executor.class),
                eq(TetheringHelper.sTetheringEventCallback));
    }

    @Test
    public void onStop_eventCallbacksContains_removeAndUnregisterCallback() {
        mHelper.sEventCallbacks.clear();
        mHelper.sEventCallbacks.add(mTetheringEventCallback);

        mHelper.onStop(mLifecycleOwner);

        assertThat(mHelper.sEventCallbacks.contains(mTetheringEventCallback)).isFalse();
        assertThat(mHelper.sEventCallbacks.size()).isEqualTo(0);
        verify(mTetheringManager).unregisterTetheringEventCallback(
                eq(TetheringHelper.sTetheringEventCallback));
    }

    @Test
    public void onStop_eventCallbacksClear_doNotUnregisterCallback() {
        mHelper.sEventCallbacks.clear();

        mHelper.onStop(mLifecycleOwner);

        assertThat(mHelper.sEventCallbacks.contains(mTetheringEventCallback)).isFalse();
        assertThat(mHelper.sEventCallbacks.size()).isEqualTo(0);
        verify(mTetheringManager, never()).unregisterTetheringEventCallback(
                eq(TetheringHelper.sTetheringEventCallback));
    }

    @Test
    public void onTetheredInterfacesChanged_eventCallbacksContains_doCallback() {
        mHelper.sEventCallbacks.clear();
        mHelper.sEventCallbacks.add(mTetheringEventCallback);

        mHelper.sTetheringEventCallback.onTetheredInterfacesChanged(mInterfaces);

        verify(mTetheringEventCallback).onTetheredInterfacesChanged(eq(mInterfaces));
    }

    @Test
    public void onTetheredInterfacesChanged_eventCallbacksClear_doNotCallback() {
        mHelper.sEventCallbacks.clear();

        mHelper.sTetheringEventCallback.onTetheredInterfacesChanged(mInterfaces);

        verify(mTetheringEventCallback, never()).onTetheredInterfacesChanged(eq(mInterfaces));
    }
}

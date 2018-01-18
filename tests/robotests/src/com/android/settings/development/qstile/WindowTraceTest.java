/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.development.qstile;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.os.RemoteException;
import android.widget.Toast;

import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowParcel;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.IWindowManagerWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WindowTraceTest {
    @Mock
    private IWindowManagerWrapper mWindowManager;
    @Mock
    private Toast mToast;

    private DevelopmentTiles.WindowTrace mWindowTrace;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mWindowTrace = spy(new DevelopmentTiles.WindowTrace());
        ReflectionHelpers.setField(mWindowTrace, "mWindowManager", mWindowManager);
        ReflectionHelpers.setField(mWindowTrace, "mToast", mToast);
    }

    @After
    public void teardown() {
        verifyNoMoreInteractions(mToast);
    }

    @Test
    public void wmReturnsTraceEnabled_shouldReturnEnabled() throws RemoteException {
        doReturn(true).when(mWindowManager).isWindowTraceEnabled();
        assertThat(mWindowTrace.isEnabled()).isTrue();
    }

    @Test
    public void wmReturnsTraceDisabled_shouldReturnDisabled() throws RemoteException {
        doReturn(false).when(mWindowManager).isWindowTraceEnabled();
        assertThat(mWindowTrace.isEnabled()).isFalse();
    }

    @Test
    public void wmThrowsRemoteException_shouldReturnDisabled() throws RemoteException {
        doThrow(new RemoteException("Unknown"))
                .when(mWindowManager).isWindowTraceEnabled();
        assertThat(mWindowTrace.isEnabled()).isFalse();
    }

    @Test
    public void setIsEnableTrue_shouldEnableWindowTrace() throws RemoteException {
        mWindowTrace.setIsEnabled(true);
        verify(mWindowManager).startWindowTrace();
        verifyNoMoreInteractions(mWindowManager);
    }

    @Test
    @Config(shadows = {ShadowParcel.class})
    public void setIsEnableFalse_shouldDisableWindowTraceAndShowToast() throws RemoteException {
        mWindowTrace.setIsEnabled(false);
        verify(mWindowManager).stopWindowTrace();
        verify(mToast).show();
        verifyNoMoreInteractions(mWindowManager);
    }

    @Test
    public void setIsEnableAndWmThrowsRemoteException_shouldDoNothing() throws RemoteException {
        doThrow(new RemoteException("Unknown")).when(mWindowManager).isWindowTraceEnabled();
        mWindowTrace.setIsEnabled(true);
    }
}

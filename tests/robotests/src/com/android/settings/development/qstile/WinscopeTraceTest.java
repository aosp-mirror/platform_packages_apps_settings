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

import static com.android.settings.development.qstile.DevelopmentTiles.WinscopeTrace
        .SURFACE_FLINGER_LAYER_TRACE_CONTROL_CODE;
import static com.android.settings.development.qstile.DevelopmentTiles.WinscopeTrace
        .SURFACE_FLINGER_LAYER_TRACE_STATUS_CODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.os.IBinder;
import android.os.RemoteException;
import android.view.IWindowManager;
import android.widget.Toast;

import com.android.internal.inputmethod.ImeTracing;
import com.android.settings.testutils.shadow.ShadowParcel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class WinscopeTraceTest {

    @Mock
    private IWindowManager mWindowManager;
    @Mock
    private ImeTracing mImeTracing;
    @Mock
    private IBinder mSurfaceFlinger;
    @Mock
    private Toast mToast;

    private DevelopmentTiles.WinscopeTrace mWinscopeTrace;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mWinscopeTrace = spy(new DevelopmentTiles.WinscopeTrace());
        ReflectionHelpers.setField(mWinscopeTrace, "mWindowManager", mWindowManager);
        ReflectionHelpers.setField(mWinscopeTrace, "mImeTracing", mImeTracing);
        ReflectionHelpers.setField(mWinscopeTrace, "mSurfaceFlinger", mSurfaceFlinger);
        ReflectionHelpers.setField(mWinscopeTrace, "mToast", mToast);
    }

    @After
    public void teardown() {
        verifyNoMoreInteractions(mToast);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void wmReturnsTraceEnabled_shouldReturnEnabled() throws RemoteException {
        // Assume Surface Trace and Input Method Manager are disabled.
        ShadowParcel.sReadBoolResult = false;
        doReturn(true).when(mWindowManager).isWindowTraceEnabled();
        assertThat(mWinscopeTrace.isEnabled()).isTrue();
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void sfReturnsTraceEnabled_shouldReturnEnabled() throws RemoteException {
        // Assume Window Trace and Input Method Manager are disabled.
        doReturn(false).when(mWindowManager).isWindowTraceEnabled();
        doReturn(false).when(mImeTracing).isEnabled();
        ShadowParcel.sReadBoolResult = true;
        assertThat(mWinscopeTrace.isEnabled()).isTrue();
        verify(mSurfaceFlinger)
                .transact(eq(SURFACE_FLINGER_LAYER_TRACE_STATUS_CODE), any(), any(),
                        eq(0 /* flags */));
        verifyNoMoreInteractions(mSurfaceFlinger);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void sfAndWmReturnsTraceEnabled_shouldReturnEnabled() throws RemoteException {
        ShadowParcel.sReadBoolResult = true;
        doReturn(true).when(mWindowManager).isWindowTraceEnabled();
        assertThat(mWinscopeTrace.isEnabled()).isTrue();
    }

    @Test
    public void wmAndSfAndImmReturnTraceDisabled_shouldReturnDisabled() throws RemoteException {
        ShadowParcel.sReadBoolResult = false;
        doReturn(false).when(mWindowManager).isWindowTraceEnabled();
        doReturn(false).when(mImeTracing).isEnabled();
        assertThat(mWinscopeTrace.isEnabled()).isFalse();
        verify(mSurfaceFlinger)
                .transact(eq(SURFACE_FLINGER_LAYER_TRACE_STATUS_CODE), any(), any(),
                eq(0 /* flags */));
        verifyNoMoreInteractions(mSurfaceFlinger);
    }

    @Test
    public void wmAndSfReturnTraceDisabled_immReturnsTraceEnabled_shouldReturnEnabled()
            throws RemoteException {
        ShadowParcel.sReadBoolResult = false;
        doReturn(false).when(mWindowManager).isWindowTraceEnabled();
        doReturn(true).when(mImeTracing).isEnabled();
        assertThat(mWinscopeTrace.isEnabled()).isTrue();
        verify(mSurfaceFlinger)
                .transact(eq(SURFACE_FLINGER_LAYER_TRACE_STATUS_CODE), any(), any(),
                eq(0 /* flags */));
        verifyNoMoreInteractions(mSurfaceFlinger);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void immReturnsTraceEnabled_shouldReturnEnabled() throws RemoteException {
        // Assume Window Manager and Surface Trace are disabled.
        ShadowParcel.sReadBoolResult = false;
        doReturn(true).when(mImeTracing).isEnabled();
        assertThat(mWinscopeTrace.isEnabled()).isTrue();
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void immReturnsTraceDisabled_shouldReturnDisabled() throws RemoteException {
        // Assume Window Manager and Surface Trace are disabled.
        ShadowParcel.sReadBoolResult = false;
        doReturn(false).when(mImeTracing).isEnabled();
        assertThat(mWinscopeTrace.isEnabled()).isFalse();
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void wmThrowsRemoteExAndSfReturnsTraceDisabled_shouldReturnDisabled()
            throws RemoteException {
        ShadowParcel.sReadBoolResult = false;
        doThrow(new RemoteException("Unknown"))
                .when(mWindowManager).isWindowTraceEnabled();
        assertThat(mWinscopeTrace.isEnabled()).isFalse();
    }

    @Test
    public void sfUnavailableAndWmAndImmReturnTraceDisabled_shouldReturnDisabled()
            throws RemoteException {
        doReturn(false).when(mWindowManager).isWindowTraceEnabled();
        doReturn(false).when(mImeTracing).isEnabled();
        ReflectionHelpers.setField(mWinscopeTrace, "mSurfaceFlinger", null);
        assertThat(mWinscopeTrace.isEnabled()).isFalse();
    }

    @Test
    public void setIsEnableTrue_shouldEnableWindowTrace() throws RemoteException {
        mWinscopeTrace.setIsEnabled(true);
        verify(mWindowManager).startWindowTrace();
        verifyNoMoreInteractions(mWindowManager);
    }

    @Test
    public void setIsEnableTrue_shouldEnableImeTrace() throws RemoteException {
        mWinscopeTrace.setIsEnabled(true);
        verify(mImeTracing).startImeTrace();
        verifyNoMoreInteractions(mImeTracing);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void setIsEnableTrue_shouldEnableLayerTrace() throws RemoteException {
        mWinscopeTrace.setIsEnabled(true);
        assertThat(ShadowParcel.sWriteIntResult).isEqualTo(1);
        verify(mSurfaceFlinger)
                .transact(eq(SURFACE_FLINGER_LAYER_TRACE_CONTROL_CODE), any(), isNull(),
                        eq(0 /* flags */));
        verifyNoMoreInteractions(mSurfaceFlinger);
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void setIsEnableFalse_shouldDisableWindowTrace() throws RemoteException {
        mWinscopeTrace.setIsEnabled(false);
        verify(mWindowManager).stopWindowTrace();
        verifyNoMoreInteractions(mWindowManager);
        verify(mToast).show();
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void setIsEnableFalse_shouldDisableImeTrace() throws RemoteException {
        mWinscopeTrace.setIsEnabled(false);
        verify(mImeTracing).stopImeTrace();
        verifyNoMoreInteractions(mImeTracing);
        verify(mToast).show();
    }

    @Test
    @Config(shadows = ShadowParcel.class)
    public void setIsEnableFalse_shouldDisableLayerTrace() throws RemoteException {
        mWinscopeTrace.setIsEnabled(false);
        assertThat(ShadowParcel.sWriteIntResult).isEqualTo(0);
        verify(mSurfaceFlinger)
                .transact(eq(SURFACE_FLINGER_LAYER_TRACE_CONTROL_CODE), any(), isNull(),
                        eq(0 /* flags */));
        verifyNoMoreInteractions(mSurfaceFlinger);
        verify(mToast).show();
    }

    @Test
    public void setIsEnableFalse_shouldShowToast() {
        mWinscopeTrace.setIsEnabled(false);
        verify(mToast).show();
    }

    /**
     * Verify when window manager call throws a remote exception, it is handled without
     * re-throwing the exception.
     */
    @Test
    public void setIsEnableAndWmThrowsRemoteException_shouldFailGracefully()
            throws RemoteException {
        doThrow(new RemoteException("Unknown")).when(mWindowManager).isWindowTraceEnabled();
        mWinscopeTrace.setIsEnabled(true);
    }

    /**
     * Verify is surface flinger is not available not calls are made to it.
     */
    @Test
    public void setIsEnableAndSfUnavailable_shouldFailGracefully() {
        ReflectionHelpers.setField(mWinscopeTrace, "mSurfaceFlinger", null);
        mWinscopeTrace.setIsEnabled(true);
        verifyNoMoreInteractions(mSurfaceFlinger);
    }
}

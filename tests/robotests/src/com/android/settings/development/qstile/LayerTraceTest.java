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

import static com.android.settings.development.qstile.DevelopmentTiles.LayerTrace
        .SURFACE_FLINGER_LAYER_TRACE_CONTROL_CODE;
import static com.android.settings.development.qstile.DevelopmentTiles.LayerTrace
        .SURFACE_FLINGER_LAYER_TRACE_STATUS_CODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.os.IBinder;
import android.os.RemoteException;

import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowParcel;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

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
public class LayerTraceTest {
    @Mock
    private IBinder mSurfaceFlinger;

    private DevelopmentTiles.LayerTrace mLayerTraceTile;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLayerTraceTile = spy(new DevelopmentTiles.LayerTrace());
        mLayerTraceTile.onCreate();
        ReflectionHelpers.setField(mLayerTraceTile, "mSurfaceFlinger", mSurfaceFlinger);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(mSurfaceFlinger);
    }

    @Test
    @Config(shadows = {ShadowParcel.class})
    public void sfReturnsTraceEnabled_shouldReturnEnabled() throws RemoteException {
        ShadowParcel.sReadBoolResult = true;
        assertThat(mLayerTraceTile.isEnabled()).isTrue();
        verify(mSurfaceFlinger)
                .transact(eq(SURFACE_FLINGER_LAYER_TRACE_STATUS_CODE), any(), any(),
                        eq(0 /* flags */));
    }

    @Test
    @Config(shadows = {ShadowParcel.class})
    public void sfReturnsTraceDisabled_shouldReturnDisabled() throws RemoteException {
        ShadowParcel.sReadBoolResult = false;
        assertThat(mLayerTraceTile.isEnabled()).isFalse();
        verify(mSurfaceFlinger)
                .transact(eq(SURFACE_FLINGER_LAYER_TRACE_STATUS_CODE), any(), any(),
                        eq(0 /* flags */));
    }

    @Test
    public void sfUnavailable_shouldReturnDisabled() throws RemoteException {
        ReflectionHelpers.setField(mLayerTraceTile, "mSurfaceFlinger", null);
        assertThat(mLayerTraceTile.isEnabled()).isFalse();
    }

    @Test
    @Config(shadows = {ShadowParcel.class})
    public void setIsEnableTrue_shouldEnableLayerTrace() throws RemoteException {
        mLayerTraceTile.setIsEnabled(true);
        assertThat(ShadowParcel.sWriteIntResult).isEqualTo(1);
        verify(mSurfaceFlinger)
                .transact(eq(SURFACE_FLINGER_LAYER_TRACE_CONTROL_CODE), any(), isNull(),
                        eq(0 /* flags */));
    }

    @Test
    @Config(shadows = {ShadowParcel.class})
    public void setIsEnableFalse_shouldDisableLayerTrace() throws RemoteException {
        mLayerTraceTile.setIsEnabled(false);
        assertThat(ShadowParcel.sWriteIntResult).isEqualTo(0);
        verify(mSurfaceFlinger)
                .transact(eq(SURFACE_FLINGER_LAYER_TRACE_CONTROL_CODE), any(), isNull(),
                        eq(0 /* flags */));
    }

    @Test
    public void setIsEnableAndSfUnavailable_shouldDoNothing() throws RemoteException {
        ReflectionHelpers.setField(mLayerTraceTile, "mSurfaceFlinger", null);
        mLayerTraceTile.setIsEnabled(true);
        mLayerTraceTile.setIsEnabled(false);
    }
}

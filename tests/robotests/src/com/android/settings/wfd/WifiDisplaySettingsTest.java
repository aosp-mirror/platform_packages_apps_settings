/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.wfd;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.media.MediaRouter;
import android.net.wifi.p2p.WifiP2pManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WifiDisplaySettingsTest {

    @Mock
    private Activity mActivity;
    @Mock
    private MediaRouter mMediaRouter;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mActivity.getSystemService(Context.MEDIA_ROUTER_SERVICE)).thenReturn(mMediaRouter);
        when(mActivity.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)).thenReturn(true);
    }

    @Test
    public void isAvailable_nullService_shouldReturnFalse() {
        assertThat(WifiDisplaySettings.isAvailable(mActivity)).isFalse();
    }

    @Test
    public void isAvailable_noWifiDirectFeature_shouldReturnFalse() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT))
            .thenReturn(false);

        assertThat(WifiDisplaySettings.isAvailable(mActivity)).isFalse();
    }

    @Test
    public void isAvailable_hasService_shouldReturnTrue() {
        when(mActivity.getSystemService(Context.DISPLAY_SERVICE))
            .thenReturn(mock(DisplayManager.class));
        when(mActivity.getSystemService(Context.WIFI_P2P_SERVICE))
            .thenReturn(mock(WifiP2pManager.class));

        assertThat(WifiDisplaySettings.isAvailable(mActivity)).isTrue();
    }
}

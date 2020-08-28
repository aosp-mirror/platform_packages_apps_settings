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
 * limitations under the License
 */

package com.android.settings.wfd;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRouter;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiDisplayPreferenceControllerTest {
    private Context mContext;
    private WifiDisplayPreferenceController mWifiDisplayPreferenceController;
    @Mock
    private MediaRouter mMediaRouter;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.MEDIA_ROUTER_SERVICE)).thenReturn(mMediaRouter);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)).thenReturn(true);
        mWifiDisplayPreferenceController = new WifiDisplayPreferenceController(mContext, "key");
    }

    @Test
    public void getSummary_disconnected_shouldProvideDisconnectedSummary() {
        assertThat(mWifiDisplayPreferenceController.getSummary().toString()).contains(
                mContext.getString(R.string.disconnected));
    }

    @Test
    public void getSummary_connected_shouldProvideConnectedSummary() {
        final MediaRouter.RouteInfo route = mock(MediaRouter.RouteInfo.class);
        when(mMediaRouter.getRouteCount()).thenReturn(1);
        when(mMediaRouter.getRouteAt(0)).thenReturn(route);
        when(route.matchesTypes(anyInt())).thenReturn(true);
        when(route.isSelected()).thenReturn(true);
        when(route.isConnecting()).thenReturn(false);

        assertThat(mWifiDisplayPreferenceController.getSummary().toString()).contains(
                mContext.getString(R.string.wifi_display_status_connected));
    }
}

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

package com.android.settings.enterprise;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ProxyInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GlobalHttpProxyPreferenceControllerTest {

    private static final String KEY_GLOBAL_HTTP_PROXY = "global_http_proxy";

    @Mock
    private Context mContext;
    @Mock
    private ConnectivityManager mCm;

    private GlobalHttpProxyPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mCm);
        mController = new GlobalHttpProxyPreferenceController(mContext);
    }

    @Test
    public void testIsAvailable() {
        when(mCm.getGlobalProxy()).thenReturn(null);
        assertThat(mController.isAvailable()).isFalse();

        when(mCm.getGlobalProxy()).thenReturn(ProxyInfo.buildDirectProxy("localhost", 123));
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testGetPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(KEY_GLOBAL_HTTP_PROXY);
    }
}

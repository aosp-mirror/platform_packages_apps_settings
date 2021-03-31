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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowRestrictedLockUtilsInternal.class)
public class WifiEnablerTest {

    @Mock
    private Context mContext;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private ConnectivityManager mConnectivityManager;

    private WifiEnabler mEnabler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        mEnabler = new WifiEnabler(mContext, mock(SwitchWidgetController.class),
                mock(MetricsFeatureProvider.class), mConnectivityManager);
    }

    @Test
    public void onSwitchToggled_avoidBadWifiConfigIsFalse_shouldReturnTrue() {
        when(mWifiManager.setWifiEnabled(true)).thenReturn(true);
        when(mWifiManager.getWifiApState()).thenReturn(WifiManager.WIFI_AP_STATE_ENABLED);

        assertThat(mEnabler.onSwitchToggled(true)).isTrue();
    }
}

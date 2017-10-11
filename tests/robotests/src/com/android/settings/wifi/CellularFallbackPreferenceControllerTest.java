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

import android.content.Context;
import android.net.wifi.WifiManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class CellularFallbackPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private WifiManager mWifiManager;

    private CellularFallbackPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new CellularFallbackPreferenceController(mContext);
    }

    @Test
    public void isAvailable_avoidBadWifiConfigIsFalse_shouldReturnTrue() {
        when(mContext.getResources().getInteger(
                com.android.internal.R.integer.config_networkAvoidBadWifi))
                .thenReturn(0);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_avoidBadWifiConfigIsTrue_shouldReturnFalse() {
        when(mContext.getResources().getInteger(
                com.android.internal.R.integer.config_networkAvoidBadWifi))
                .thenReturn(1);

        assertThat(mController.isAvailable()).isFalse();
    }
}

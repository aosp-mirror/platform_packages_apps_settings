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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.WifiManagerWrapper;
import com.android.settingslib.wifi.AccessPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SavedAccessPointsWifiSettingsTest {

    @Mock
    private Handler mHandler;

    private SavedAccessPointsWifiSettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSettings = new SavedAccessPointsWifiSettings();
        ReflectionHelpers.setField(mSettings, "mHandler", mHandler);
    }

    @Test
    public void onForget_isPasspointConfig_shouldSendMessageToHandler() {
        final AccessPoint accessPoint = mock(AccessPoint.class);
        when(accessPoint.isPasspointConfig()).thenReturn(true);
        ReflectionHelpers.setField(mSettings, "mSelectedAccessPoint", accessPoint);
        ReflectionHelpers.setField(mSettings, "mWifiManager", mock(WifiManagerWrapper.class));

        mSettings.onForget(null);

        verify(mHandler).sendEmptyMessage(mSettings.MSG_UPDATE_PREFERENCES);
    }

    @Test
    public void onForget_onSuccess_shouldSendMessageToHandler() {
        mSettings.mForgetListener.onSuccess();

        verify(mHandler).sendEmptyMessage(mSettings.MSG_UPDATE_PREFERENCES);
    }

    @Test
    public void onForget_onFailure_shouldSendMessageToHandler() {
        mSettings.mForgetListener.onFailure(0);

        verify(mHandler).sendEmptyMessage(mSettings.MSG_UPDATE_PREFERENCES);
    }
}

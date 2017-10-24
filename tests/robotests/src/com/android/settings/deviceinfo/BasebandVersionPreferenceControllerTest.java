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
package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.robolectric.shadow.api.Shadow.extract;

import android.net.ConnectivityManager;
import android.support.v7.preference.Preference;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;
import com.android.settings.testutils.shadow.ShadowConnectivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = ShadowConnectivityManager.class)
public class BasebandVersionPreferenceControllerTest {


    @Mock
    private Preference mPreference;

    private BasebandVersionPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new BasebandVersionPreferenceController(RuntimeEnvironment.application);
    }

    @Test
    public void isAvailable_wifiOnly_shouldReturnFalse() {
        ShadowConnectivityManager connectivityManager =
                extract(RuntimeEnvironment.application.getSystemService(ConnectivityManager.class));
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_hasMobile_shouldReturnTrue() {
        ShadowConnectivityManager connectivityManager =
                extract(RuntimeEnvironment.application.getSystemService(ConnectivityManager.class));
        connectivityManager.setNetworkSupported(ConnectivityManager.TYPE_MOBILE, true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Config(shadows = {SettingsShadowSystemProperties.class})
    @Test
    public void updateState_shouldLoadFromSysProperty() {
        SettingsShadowSystemProperties.set("gsm.version.baseband", "test");

        mController.updateState(mPreference);

        verify(mPreference).setSummary("test");
    }
}

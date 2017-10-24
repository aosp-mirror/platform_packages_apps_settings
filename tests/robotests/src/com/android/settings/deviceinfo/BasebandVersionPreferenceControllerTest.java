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

import android.content.Context;
import android.net.ConnectivityManager;
import android.support.v7.preference.Preference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BasebandVersionPreferenceControllerTest {


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private ConnectivityManager mCm;
    @Mock
    private Preference mPreference;

    private BasebandVersionPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new BasebandVersionPreferenceController(mContext);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mCm);
    }

    @Test
    public void isAvailable_wifiOnly_shouldReturnFalse() {
        when(mCm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_hasMobile_shouldReturnTrue() {
        when(mCm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)).thenReturn(true);
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

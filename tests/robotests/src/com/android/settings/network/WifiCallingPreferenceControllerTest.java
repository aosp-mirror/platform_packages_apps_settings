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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.ims.ImsManager;
import com.android.settings.TestConfig;
import com.android.settings.network.WifiCallingPreferenceControllerTest.ShadowImsManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION_O,
        shadows = {ShadowImsManager.class})
public class WifiCallingPreferenceControllerTest {

    @Mock
    private Preference mPreference;

    private Context mContext;
    private WifiCallingPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        MockitoAnnotations.initMocks(this);

        mController = new WifiCallingPreferenceController(mContext);
    }

    @After
    public void teardown() {
        ShadowImsManager.reset();
    }

    @Test
    public void isAvailable_platformEnabledAndProvisioned_shouldReturnTrue() {
        ShadowImsManager.wfcProvisioned = true;
        ShadowImsManager.wfcEnabledByPlatform = true;

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_shouldUpdatePreferenceSummary() {
        mController.updateState(mPreference);

        verify(mPreference).setSummary(anyInt());
    }

    @Implements(ImsManager.class)
    public static class ShadowImsManager {

        public static boolean wfcEnabledByPlatform;
        public static boolean wfcProvisioned;

        public static void reset() {
            wfcEnabledByPlatform = false;
            wfcProvisioned = false;
        }

        @Implementation
        public static boolean isWfcEnabledByPlatform(Context context) {
            return wfcEnabledByPlatform;
        }

        @Implementation
        public static boolean isWfcProvisionedOnDevice(Context context) {
            return wfcProvisioned;
        }
    }

}

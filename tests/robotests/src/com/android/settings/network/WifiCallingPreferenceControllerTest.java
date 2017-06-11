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

import android.content.Context;
import android.support.v7.preference.Preference;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;

import com.android.ims.ImsManager;
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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WifiCallingPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private Preference mPreference;
    private WifiCallingPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE))
                .thenReturn(mCarrierConfigManager);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE))
                .thenReturn(mTelephonyManager);
        mController = new WifiCallingPreferenceController(mContext);
    }

    @Test
    public void isAvailable_platformEnabledAndProvisioned_shouldReturnTrue() {
        ImsManager.wfcEnabledByPlatform = true;
        ImsManager.wfcProvisioned = true;

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_shouldUpdatePreferenceSummary() {
        mController.updateState(mPreference);

        verify(mPreference).setSummary(anyInt());
    }
}

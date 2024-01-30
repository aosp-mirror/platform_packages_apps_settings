/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link HearingDeviceCallRoutingPreferenceController}. */
@Config(shadows = {ShadowBluetoothAdapter.class})
@RunWith(RobolectricTestRunner.class)
public class HearingDeviceCallRoutingPreferenceControllerTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private static final String FAKE_KEY = "fake_key";

    @Mock
    private TelephonyManager mTelephonyManager;
    private HearingDeviceCallRoutingPreferenceController mController;

    @Before
    public void setUp() {
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        mController = new HearingDeviceCallRoutingPreferenceController(mContext, FAKE_KEY);
    }

    @Test
    public void getAvailabilityStatus_hasTelephonyCalling_available() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noTelephonyCalling_unsupported() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);


        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}

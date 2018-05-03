/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.connecteddevice;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.FeatureFlagUtils;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.FeatureFlags;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class BluetoothOnWhileDrivingPreferenceControllerTest {

    private BluetoothOnWhileDrivingPreferenceController mController;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new BluetoothOnWhileDrivingPreferenceController(mContext);
    }

    @Test
    public void getAvailabilityStatus_onWhenEnabled() {
        FeatureFlagUtils.setEnabled(mContext, FeatureFlags.BLUETOOTH_WHILE_DRIVING, true);

        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_offWhenDisabled() {
        assertThat(mController.getAvailabilityStatus())
            .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void setChecked_togglesSettingSecure() {
        mController.setChecked(true);

        final String name = Secure.BLUETOOTH_ON_WHILE_DRIVING;
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(), name, 0)).isEqualTo(1);
    }
}

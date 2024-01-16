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
 * limitations under the License.
 */

package com.android.settings.fuelgauge;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.BatteryManager;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.BatteryTestUtils;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TopLevelBatteryPreferenceControllerTest {
    private Context mContext;
    private TopLevelBatteryPreferenceController mController;
    private BatterySettingsFeatureProvider mBatterySettingsFeatureProvider;

    @Mock private UsbPort mUsbPort;
    @Mock private UsbManager mUsbManager;
    @Mock private UsbPortStatus mUsbPortStatus;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new TopLevelBatteryPreferenceController(mContext, "test_key");
        when(mContext.getSystemService(UsbManager.class)).thenReturn(mUsbManager);
    }

    @Test
    public void getAvailibilityStatus_availableByDefault() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void convertClassPathToComponentName_nullInput_returnsNull() {
        assertThat(mController.convertClassPathToComponentName(null)).isNull();
    }

    @Test
    @Ignore
    public void convertClassPathToComponentName_emptyStringInput_returnsNull() {
        assertThat(mController.convertClassPathToComponentName("")).isNull();
    }

    @Test
    public void convertClassPathToComponentName_singleClassName_returnsCorrectComponentName() {
        ComponentName output = mController.convertClassPathToComponentName("ClassName");

        assertThat(output.getPackageName()).isEqualTo("");
        assertThat(output.getClassName()).isEqualTo("ClassName");
    }

    @Test
    public void convertClassPathToComponentName_validAddress_returnsCorrectComponentName() {
        ComponentName output = mController.convertClassPathToComponentName("my.fragment.ClassName");

        assertThat(output.getPackageName()).isEqualTo("my.fragment");
        assertThat(output.getClassName()).isEqualTo("ClassName");
    }

    @Test
    public void getDashboardLabel_returnsBatterPercentString() {
        mController.mPreference = new Preference(mContext);
        BatteryInfo info = new BatteryInfo();
        info.batteryPercentString = "3%";

        assertThat(mController.getDashboardLabel(mContext, info, true))
                .isEqualTo(info.batteryPercentString);
    }

    @Test
    public void getDashboardLabel_returnsRemainingLabel() {
        mController.mPreference = new Preference(mContext);
        BatteryInfo info = new BatteryInfo();
        info.batteryPercentString = "3%";
        info.remainingLabel = "Phone will shut down soon";

        assertThat(mController.getDashboardLabel(mContext, info, true))
                .isEqualTo("3% - Phone will shut down soon");
    }

    @Test
    public void getDashboardLabel_returnsChargeLabel() {
        mController.mPreference = new Preference(mContext);
        BatteryInfo info = new BatteryInfo();
        info.discharging = false;
        info.chargeLabel = "5% - charging";

        assertThat(mController.getDashboardLabel(mContext, info, true)).isEqualTo(info.chargeLabel);
    }

    @Test
    public void getDashboardLabel_incompatibleCharger_returnsCorrectLabel() {
        BatteryTestUtils.setupIncompatibleEvent(mUsbPort, mUsbManager, mUsbPortStatus);
        mController.mPreference = new Preference(mContext);
        BatteryInfo info = new BatteryInfo();
        info.batteryPercentString = "66%";

        assertThat(mController.getDashboardLabel(mContext, info, true))
                .isEqualTo(
                        mContext.getString(
                                com.android.settingslib.R.string
                                        .power_incompatible_charging_settings_home_page,
                                info.batteryPercentString));
    }

    @Test
    public void getDashboardLabel_notChargingState_returnsCorrectLabel() {
        mController.mPreference = new Preference(mContext);
        BatteryInfo info = new BatteryInfo();
        info.batteryStatus = BatteryManager.BATTERY_STATUS_NOT_CHARGING;
        info.statusLabel = "expected returned label";

        assertThat(mController.getDashboardLabel(mContext, info, true)).isEqualTo(info.statusLabel);
    }

    @Test
    public void getSummary_batteryNotPresent_shouldShowWarningMessage() {
        mController.mIsBatteryPresent = false;
        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(R.string.battery_missing_message));
    }
}

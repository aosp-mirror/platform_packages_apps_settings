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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;

import android.content.ContentResolver;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SmartBatteryPreferenceControllerTest {

    private static final int ON = 1;
    private static final int OFF = 0;

    private SmartBatteryPreferenceController mController;
    private SwitchPreference mPreference;
    private ContentResolver mContentResolver;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mController = new SmartBatteryPreferenceController(RuntimeEnvironment.application);
        mPreference = new SwitchPreference(RuntimeEnvironment.application);
    }

    @Test
    public void testUpdateState_smartBatteryOn_preferenceChecked() {
        putSmartBatteryValue(ON);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_smartBatteryOff_preferenceUnchecked() {
        putSmartBatteryValue(OFF);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_checkPreference_smartBatteryOn() {
        mController.onPreferenceChange(mPreference, true);

        assertThat(getSmartBatteryValue()).isEqualTo(ON);
    }

    @Test
    public void testUpdateState_unCheckPreference_smartBatteryOff() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(getSmartBatteryValue()).isEqualTo(OFF);
    }

    @Test
    public void testGetAvailabilityStatus_smartBatterySupported_returnAvailable() {
        doReturn(true).when(mFeatureFactory.powerUsageFeatureProvider).isSmartBatterySupported();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void testGetAvailabilityStatus_smartBatteryUnSupported_returnDisabled() {
        doReturn(false).when(mFeatureFactory.powerUsageFeatureProvider).isSmartBatterySupported();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    private void putSmartBatteryValue(int value) {
        Settings.Global.putInt(mContentResolver,
                Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, value);
    }

    private int getSmartBatteryValue() {
        return Settings.Global.getInt(mContentResolver,
                Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED, ON);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final SmartBatteryPreferenceController controller =
                new SmartBatteryPreferenceController(null);
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnsTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}

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

import android.content.Context;
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
public class AutoRestrictionPreferenceControllerTest {
    private static final int ON = 1;
    private static final int OFF = 0;

    private AutoRestrictionPreferenceController mController;
    private SwitchPreference mPreference;
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mContext = RuntimeEnvironment.application;
        mController = new AutoRestrictionPreferenceController(mContext);
        mPreference = new SwitchPreference(mContext);
    }

    @Test
    public void testUpdateState_AutoRestrictionOn_preferenceChecked() {
        putAutoRestrictionValue(ON);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void testUpdateState_AutoRestrictionOff_preferenceUnchecked() {
        putAutoRestrictionValue(OFF);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void testUpdateState_checkPreference_autoRestrictionOn() {
        mController.onPreferenceChange(mPreference, true);

        assertThat(getAutoRestrictionValue()).isEqualTo(ON);
    }

    @Test
    public void testUpdateState_unCheckPreference_autoRestrictionOff() {
        mController.onPreferenceChange(mPreference, false);

        assertThat(getAutoRestrictionValue()).isEqualTo(OFF);
    }

    @Test
    public void testGetAvailabilityStatus_smartBatterySupported_returnDisabled() {
        doReturn(true).when(mFeatureFactory.powerUsageFeatureProvider).isSmartBatterySupported();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void testGetAvailabilityStatus_smartBatteryUnSupported_returnAvailable() {
        doReturn(false).when(mFeatureFactory.powerUsageFeatureProvider).isSmartBatterySupported();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    private void putAutoRestrictionValue(int value) {
        Settings.Global.putInt(
                mContext.getContentResolver(), Settings.Global.APP_AUTO_RESTRICTION_ENABLED, value);
    }

    private int getAutoRestrictionValue() {
        return Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.APP_AUTO_RESTRICTION_ENABLED, ON);
    }
}

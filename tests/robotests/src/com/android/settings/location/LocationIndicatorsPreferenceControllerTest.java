/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.location;


import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.DeviceConfig;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.Utils;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link LocationIndicatorsPreferenceController}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class LocationIndicatorsPreferenceControllerTest {
    @Mock
    PackageManager mPackageManager;
    private Context mContext;
    private LocationIndicatorsPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new LocationIndicatorsPreferenceController(mContext, "key");

        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
    }

    /**
     * Verify the location indicator settings are visible when location feature is supported
     * on the device.
     */
    @Test
    public void getAvailabilityStatus_locationSupported_shouldReturnAVAILABLE() {
        // Enable the settings flags.
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                Utils.PROPERTY_LOCATION_INDICATOR_SETTINGS_ENABLED, Boolean.toString(true),
                true);
        when(mPackageManager.hasSystemFeature(eq(PackageManager.FEATURE_LOCATION))).thenReturn(
                true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    /**
     * Verify the location indicator settings are not visible when location feature is not supported
     * on the device.
     */
    @Test
    public void getAvailabilityStatus_locationNotSupported_shouldReturnUNSUPPORTED() {
        // Enable the settings flags.
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                Utils.PROPERTY_LOCATION_INDICATOR_SETTINGS_ENABLED, Boolean.toString(true),
                true);
        when(mPackageManager.hasSystemFeature(eq(PackageManager.FEATURE_LOCATION))).thenReturn(
                false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    /**
     * Verify the location indicator settings are not visible when location indicator settings
     * are disabled on the device.
     */
    @Test
    public void getAvailabilityStatus_flagDisabled_shouldReturnUNSUPPORTED() {
        // Disable the settings flags.
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                Utils.PROPERTY_LOCATION_INDICATOR_SETTINGS_ENABLED, Boolean.toString(false),
                false);
        when(mPackageManager.hasSystemFeature(eq(PackageManager.FEATURE_LOCATION))).thenReturn(
                true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    /**
     * Verify the location indicator preference is checked when the feature is enabled.
     */
    @Test
    public void isChecked_featureEnabled_shouldReturnTrue() {
        final boolean enabled = true;
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                Utils.PROPERTY_LOCATION_INDICATORS_ENABLED, Boolean.toString(enabled), true);
        assertThat(mController.isChecked()).isTrue();
    }

    /**
     * Verify the location indicator preference is unchecked when the feature is not enabled.
     */
    @Test
    public void isChecked_featureNotEnabled_shouldReturnFalse() {
        final boolean enabled = false;
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_PRIVACY,
                Utils.PROPERTY_LOCATION_INDICATORS_ENABLED, Boolean.toString(enabled), true);
        assertThat(mController.isChecked()).isFalse();
    }
}

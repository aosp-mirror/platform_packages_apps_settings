/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.location.LocationManager;

import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class NightDisplayAutoModePreferenceControllerTest {

    private Context mContext;
    private NightDisplayAutoModePreferenceController mController;
    private LocationManager mLocationManager;

    @Before
    public void setUp() {
        mContext = Mockito.spy(RuntimeEnvironment.application);
        mLocationManager = Mockito.mock(LocationManager.class);
        when(mLocationManager.isLocationEnabled()).thenReturn(true);
        when(mContext.getSystemService(eq(LocationManager.class))).thenReturn(mLocationManager);
        mController = new NightDisplayAutoModePreferenceController(mContext,
            "night_display_auto_mode");
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void isAvailable_configuredAvailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_nightDisplayAvailable, true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configuredUnavailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_nightDisplayAvailable, false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onPreferenceChange_changesAutoMode() {
        mController.onPreferenceChange(null,
                String.valueOf(ColorDisplayManager.AUTO_MODE_TWILIGHT));
        assertThat(mContext.getSystemService(ColorDisplayManager.class).getNightDisplayAutoMode())
                .isEqualTo(ColorDisplayManager.AUTO_MODE_TWILIGHT);
    }
}

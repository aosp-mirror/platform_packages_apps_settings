/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.display;


import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import com.android.server.display.feature.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class EvenDimmerPreferenceControllerTest {

    private EvenDimmerPreferenceController mController;
    @Mock
    private Context mContext;
    @Mock
    private Resources mResources;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getResources()).thenReturn(mResources);
        mController = new EvenDimmerPreferenceController(mContext, "key");
    }

    @RequiresFlagsDisabled(Flags.FLAG_EVEN_DIMMER)
    @Test
    public void testGetAvailabilityStatus_flagOffconfigTrue() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled)).thenReturn(true);
        // setup
        mController = new EvenDimmerPreferenceController(mContext, "key");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @RequiresFlagsDisabled(Flags.FLAG_EVEN_DIMMER)
    @Test
    public void testGetCheckedStatus_setTrue() throws Settings.SettingNotFoundException {
        // setup
        mController = new EvenDimmerPreferenceController(mContext, "key");
        mController.setChecked(true);

        assertThat(Settings.Secure.getFloat(mContext.getContentResolver(),
                Settings.Secure.EVEN_DIMMER_ACTIVATED)).isEqualTo(0.0f); // false
    }

    @RequiresFlagsEnabled(Flags.FLAG_EVEN_DIMMER)
    @Test
    public void testGetAvailabilityStatus_flagOnConfigTrue() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled)).thenReturn(true);
        // setup
        mController = new EvenDimmerPreferenceController(mContext, "key");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_EVEN_DIMMER)
    public void testSetChecked_enable() throws Settings.SettingNotFoundException {
        mController.setChecked(true);
        assertThat(Settings.Secure.getFloat(mContext.getContentResolver(),
                Settings.Secure.EVEN_DIMMER_ACTIVATED)).isEqualTo(1.0f); // true
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_EVEN_DIMMER)
    public void testSetChecked_disable() throws Settings.SettingNotFoundException {
        mController.setChecked(false);
        assertThat(Settings.Secure.getFloat(mContext.getContentResolver(),
                Settings.Secure.EVEN_DIMMER_ACTIVATED)).isEqualTo(0.0f); // false
    }
}

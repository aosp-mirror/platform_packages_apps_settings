/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.hardwareinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class DeviceModelPreferenceControllerTest {

    private Context mContext;
    private DeviceModelPreferenceController mController;

    @Mock
    private Resources mResources;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getResources()).thenReturn(mResources);
        mController = new DeviceModelPreferenceController(mContext, "test_key");
    }

    @Test
    public void getAvailabilityStatus_configAllowed_available() {
        final int boolId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "bool", "config_show_device_model");
        when(mResources.getBoolean(boolId)).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_configDisallowed_unavailable() {
        final int boolId = ResourcesUtils.getResourcesId(
                ApplicationProvider.getApplicationContext(), "bool", "config_show_device_model");
        when(mResources.getBoolean(boolId)).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isAlwaysSliceable() {
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}

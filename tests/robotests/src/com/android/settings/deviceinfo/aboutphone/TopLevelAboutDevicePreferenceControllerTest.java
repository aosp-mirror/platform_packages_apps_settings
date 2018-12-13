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

package com.android.settings.deviceinfo.aboutphone;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Build;
import android.provider.Settings.Global;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TopLevelAboutDevicePreferenceControllerTest {

    private Context mContext;
    private TopLevelAboutDevicePreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new TopLevelAboutDevicePreferenceController(mContext, "test_key");
    }

    @Test
    public void getAvailabilityState_shouldBeAvailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getSummary_deviceNameNotSet_shouldReturnDeviceModel() {
        assertThat(mController.getSummary().toString()).isEqualTo(Build.MODEL);
    }

    @Test
    public void getSummary_deviceNameSet_shouldReturnDeviceName() {
        Global.putString(mContext.getContentResolver(), Global.DEVICE_NAME, "Test");
        assertThat(mController.getSummary().toString()).isEqualTo("Test");
    }
}

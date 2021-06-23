/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.emergency;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.emergencynumber.EmergencyNumberUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class EmergencyGesturePreferenceControllerTest {

    @Mock
    private EmergencyNumberUtils mEmergencyNumberUtils;
    private Context mContext;
    private EmergencyGesturePreferenceController mController;
    private static final String PREF_KEY = "gesture_emergency_button";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mController = new EmergencyGesturePreferenceController(mContext, PREF_KEY);
        mController.mEmergencyNumberUtils = mEmergencyNumberUtils;
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void getAvailabilityStatus_configIsTrue_shouldReturnAvailable() {
        SettingsShadowResources.overrideResource(
                R.bool.config_show_emergency_gesture_settings,
                Boolean.TRUE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_configIsFalse_shouldReturnUnsupported() {
        SettingsShadowResources.overrideResource(
                R.bool.config_show_emergency_gesture_settings,
                Boolean.FALSE);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isChecked_configIsNotSet_shouldReturnTrue() {
        // Set the setting to be enabled.
        when(mEmergencyNumberUtils.getEmergencyGestureEnabled()).thenReturn(true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_configIsSet_shouldReturnFalse() {
        // Set the setting to be disabled.
        when(mEmergencyNumberUtils.getEmergencyGestureEnabled()).thenReturn(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isSliceable_returnsFalse() {
        assertThat(mController.isSliceable()).isFalse();
    }
}

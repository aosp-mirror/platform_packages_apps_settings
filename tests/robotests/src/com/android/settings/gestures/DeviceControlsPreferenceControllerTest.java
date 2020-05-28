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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class DeviceControlsPreferenceControllerTest {

    private Context mContext;
    private DeviceControlsPreferenceController mController;
    private ShadowPackageManager mShadowPackageManager;

    private static final String KEY_GESTURE_PANEL = "gesture_device_controls";
    private static final String ENABLED_SETTING =
            DeviceControlsPreferenceController.ENABLED_SETTING;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mController = new DeviceControlsPreferenceController(mContext, KEY_GESTURE_PANEL);
    }

    @Test
    public void testIsChecked_panelEnabled() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), ENABLED_SETTING, 1);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_panelDisabled() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), ENABLED_SETTING, 0);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_hasSystemFeature_panelAvailable() {
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_CONTROLS, true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasntSystemFeature_panelUnsupported() {
        mShadowPackageManager.setSystemFeature(PackageManager.FEATURE_CONTROLS, false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isSliceable_correctKey() {
        final DeviceControlsPreferenceController controller =
                new DeviceControlsPreferenceController(mContext,
                        DeviceControlsPreferenceController.TOGGLE_KEY);
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceable_incorrectKey() {
        final DeviceControlsPreferenceController controller =
                new DeviceControlsPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}

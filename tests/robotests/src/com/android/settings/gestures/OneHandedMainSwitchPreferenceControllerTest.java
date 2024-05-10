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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.SystemProperties;
import android.os.UserHandle;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OneHandedMainSwitchPreferenceControllerTest {

    private static final String KEY = "gesture_one_handed_mode_enabled_main_switch";

    private Context mContext;
    private OneHandedSettingsUtils mUtils;
    private OneHandedMainSwitchPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mUtils = new OneHandedSettingsUtils(mContext);
        mController = new OneHandedMainSwitchPreferenceController(mContext, KEY);
        OneHandedSettingsUtils.setUserId(UserHandle.myUserId());
    }

    @Test
    public void setChecked_setBoolean_checkIsTrueOrFalse() {
        mController.setChecked(false);
        assertThat(OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)).isFalse();

        mController.setChecked(true);
        assertThat(OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)).isTrue();
    }

    @Test
    public void isChecked_setOneHandedModeEnabled_shouldReturnTrue() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
        mUtils.setNavigationBarMode(mContext, "2" /* fully gestural */);
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_setSupportOneHandedModeProperty_shouldAvailable() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
        mUtils.setNavigationBarMode(mContext, "2" /* fully gestural */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_unsetSupportOneHandedModeProperty_shouldDisabled() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "false");
        mUtils.setNavigationBarMode(mContext, "2" /* fully gestural */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Ignore("b/313541907")
    @Test
    public void getAvailabilityStatus_set3ButtonMode_shouldDisabled() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
        mUtils.setNavigationBarMode(mContext, "0" /* 3-button mode */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }
}

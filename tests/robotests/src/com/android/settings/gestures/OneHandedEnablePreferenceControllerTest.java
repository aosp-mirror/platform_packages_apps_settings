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
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class OneHandedEnablePreferenceControllerTest {

    private static final String KEY = "gesture_one_handed_mode_enabled";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private OneHandedEnablePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new OneHandedEnablePreferenceController(mContext, KEY);
    }

    @Test
    public void setChecked_setBoolean_checkIsTrueOrFalse() {
        mController.setChecked(false);
        assertThat(OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)).isFalse();

        mController.setChecked(true);
        assertThat(OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)).isTrue();
    }

    @Test
    public void getAvailabilityStatus_setSupportOneHandedModeProperty_shouldAvailable() {
        SystemProperties.set(OneHandedEnablePreferenceController.SUPPORT_ONE_HANDED_MODE, "true");

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_unsetSupportOneHandedModeProperty_shouldUnsupported() {
        SystemProperties.set(OneHandedEnablePreferenceController.SUPPORT_ONE_HANDED_MODE, "false");

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getSummary_enabledOneHanded_shouldDisplayOnSummary() {
        OneHandedSettingsUtils.setSettingsOneHandedModeEnabled(mContext, true);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.switch_on_text));
    }

    @Test
    public void getSummary_disabledOneHanded_shouldDisplayOffSummary() {
        OneHandedSettingsUtils.setSettingsOneHandedModeEnabled(mContext, false);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.switch_off_text));
    }
}

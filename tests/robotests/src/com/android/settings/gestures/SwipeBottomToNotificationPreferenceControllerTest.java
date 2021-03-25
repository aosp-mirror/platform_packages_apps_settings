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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;

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
public class SwipeBottomToNotificationPreferenceControllerTest {

    private static final String KEY = "gesture_swipe_bottom_to_notification";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private SwipeBottomToNotificationPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new SwipeBottomToNotificationPreferenceController(mContext, KEY);
    }

    @Test
    public void setChecked_toggledOn_enablesSwipeBottomToNotification() {
        mController.setChecked(true);

        assertThat(OneHandedSettingsUtils.isSwipeDownNotificationEnabled(mContext)).isTrue();
        assertThat(OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)).isFalse();
    }

    @Test
    public void setChecked_toggledOff_disablesSwipeBottomToNotification() {
        mController.setChecked(false);

        assertThat(OneHandedSettingsUtils.isSwipeDownNotificationEnabled(mContext)).isFalse();
    }

    @Test
    public void getAvailabilityStatus_oneHandedUnsupported_returnsUnsupport() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "false");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_oneHandedSupported_returnsAvailable() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getSummary_gestureEnabled_returnsOnSummary() {
        mController.setChecked(true);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.gesture_setting_on));
    }

    @Test
    public void getSummary_gestureDisabled_returnsOffSummary() {
        mController.setChecked(false);

        assertThat(mController.getSummary()).isEqualTo(
                mContext.getText(R.string.gesture_setting_off));
    }

    @Test
    public void isChecked_getDefaultConfig_returnFalse() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "false");
        Settings.Secure.resetToDefaults(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED);

        assertThat(mController.isChecked()).isFalse();
    }
}

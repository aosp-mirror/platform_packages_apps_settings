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
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OneHandedEnablePreferenceControllerTest {

    private static final String KEY = "gesture_one_handed_mode_enabled";

    private Context mContext;
    private OneHandedEnablePreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new OneHandedEnablePreferenceController(mContext, KEY);
        OneHandedSettingsUtils.setUserId(UserHandle.myUserId());
    }

    @Test
    public void setChecked_setBoolean_checkIsTrueOrFalse() {
        mController.setChecked(false);
        assertThat(OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)).isFalse();
        assertThat(OneHandedSettingsUtils.isSwipeDownNotificationEnabled(mContext)).isTrue();

        mController.setChecked(true);
        assertThat(OneHandedSettingsUtils.isOneHandedModeEnabled(mContext)).isTrue();
        assertThat(OneHandedSettingsUtils.isSwipeDownNotificationEnabled(mContext)).isFalse();
    }

    @Test
    public void getAvailabilityStatus_setSupportOneHandedModeProperty_shouldAvailable() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
        setNavigationBarMode(mContext, "2" /* fully gestural */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_unsetSupportOneHandedModeProperty_shouldUnsupported() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "false");
        setNavigationBarMode(mContext, "2" /* fully gestural */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_set3ButtonModeProperty_shouldUnsupported() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
        setNavigationBarMode(mContext, "0" /* 3-button */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getSummary_enabledOneHanded_shouldDisplayOnSummary() {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, true);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.switch_on_text));
    }

    @Test
    public void getSummary_disabledOneHanded_shouldDisplayOffSummary() {
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, false);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.switch_off_text));
    }

    /**
     * Set NavigationBar mode flag to Settings provider.
     * @param context App context
     * @param value Navigation bar mode:
     *  0 = 3 button
     *  1 = 2 button
     *  2 = fully gestural
     * @return true if the value was set, false on database errors.
     */
    private boolean setNavigationBarMode(Context context, String value) {
        return Settings.Secure.putStringForUser(context.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, value, UserHandle.myUserId());
    }
}

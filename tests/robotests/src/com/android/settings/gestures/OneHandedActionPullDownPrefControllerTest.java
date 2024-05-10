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
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OneHandedActionPullDownPrefControllerTest {

    private static final String KEY = "gesture_one_handed_action_pull_screen_down";

    private Context mContext;
    private OneHandedSettingsUtils mUtils;
    private OneHandedActionPullDownPrefController mController;
    private SelectorWithWidgetPreference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mUtils = new OneHandedSettingsUtils(mContext);
        mController = new OneHandedActionPullDownPrefController(mContext, KEY);
        mPreference = new SelectorWithWidgetPreference(mContext);
        OneHandedSettingsUtils.setUserId(UserHandle.myUserId());
    }

    @Test
    public void updateState_showNotificationEnabled_shouldUnchecked() {
        OneHandedSettingsUtils.setSwipeDownNotificationEnabled(mContext, true);

        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_showNotificationDisabled_shouldChecked() {
        OneHandedSettingsUtils.setSwipeDownNotificationEnabled(mContext, false);

        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_setOneHandedModeDisabled_shouldDisabled() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, false);
        mUtils.setNavigationBarMode(mContext, "2" /* fully gestural */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Ignore("b/313541907")
    @Test
    public void getAvailabilityStatus_setNavi3ButtonMode_shouldDisabled() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, true);
        mUtils.setNavigationBarMode(mContext, "0" /* 3 button */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_setNaviGesturalMode_shouldEnabled() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, true);
        mUtils.setNavigationBarMode(mContext, "2" /* fully gestural */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_unsetSupportOneHandedModeProperty_shouldDisabled() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "false");
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, true);
        mUtils.setNavigationBarMode(mContext, "2" /* fully gestural */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_setShortcutEnabled_shouldEnabled() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, false);
        mUtils.setNavigationBarMode(mContext, "0" /* 3-button mode */);
        mUtils.setShortcutEnabled(mContext, true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_setShortcutDisabled_shouldDisabled() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
        OneHandedSettingsUtils.setOneHandedModeEnabled(mContext, false);
        mUtils.setNavigationBarMode(mContext, "0" /* 3-button mode */);
        mUtils.setShortcutEnabled(mContext, false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }
}

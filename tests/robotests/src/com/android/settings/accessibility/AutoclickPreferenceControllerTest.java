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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AutoclickPreferenceControllerTest {

    private Context mContext;
    private AutoclickPreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new AutoclickPreferenceController(mContext, "auto_click");
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailableUnsearchable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_disabledAutoclick_shouldReturnOffSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, 0);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.accessibility_feature_state_off));
    }

    @Test
    public void getSummary_enabledAutoclick_shouldReturnOnSummary() {
        final int autoclickDelayDefault = AccessibilityManager.AUTOCLICK_DELAY_DEFAULT;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY, autoclickDelayDefault);

        assertThat(mController.getSummary())
                .isEqualTo(ToggleAutoclickPreferenceFragment.getAutoclickPreferenceSummary(
                        mContext.getResources(), autoclickDelayDefault));
    }
}

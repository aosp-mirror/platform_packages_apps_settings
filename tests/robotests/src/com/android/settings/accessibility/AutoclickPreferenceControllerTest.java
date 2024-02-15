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

import static android.view.accessibility.AccessibilityManager.AUTOCLICK_DELAY_DEFAULT;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AutoclickPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class AutoclickPreferenceControllerTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private AutoclickPreferenceController mController;

    @Before
    public void setUp() {
        mController = new AutoclickPreferenceController(mContext, "auto_click");
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAvailableUnsearchable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_disabledAutoclick_shouldReturnOffSummary() {
        setAutoClickEnabled(false);

        assertThat(mController.getSummary().toString())
                .isEqualTo(mContext.getText(R.string.autoclick_disabled));
    }

    @Test
    public void getSummary_enabledAutoclick_shouldReturnOnSummary() {
        setAutoClickEnabled(true);
        setAutoClickDelayed(AUTOCLICK_DELAY_DEFAULT);


        assertThat(mController.getSummary().toString())
                .isEqualTo(AutoclickUtils.getAutoclickDelaySummary(
                        mContext,
                        R.string.accessibilty_autoclick_preference_subtitle_medium_delay,
                        AUTOCLICK_DELAY_DEFAULT).toString());
    }

    private void setAutoClickEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, enabled ? ON : OFF);
    }

    private void setAutoClickDelayed(int delayedInMs) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY, delayedInMs);
    }
}

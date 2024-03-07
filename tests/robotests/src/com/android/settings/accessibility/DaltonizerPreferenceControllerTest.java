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

import static com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DaltonizerPreferenceControllerTest {
    private static final String PREF_KEY = "daltonizer_preference";
    private static final int ON = 1;
    private static final int OFF = 0;

    private Context mContext;
    private DaltonizerPreferenceController mController;
    private String mDaltonizerSummary;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new DaltonizerPreferenceController(mContext, PREF_KEY);
        mDaltonizerSummary = mContext.getString(R.string.daltonizer_feature_summary);
    }

    @Test
    public void getSummary_enabledColorCorrectionShortcutOff_shouldReturnOnSummary() {
        setColorCorrectionEnabled(true);
        setColorCorrectionShortcutEnabled(false);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.daltonizer_state_on));
    }

    @Test
    public void getSummary_enabledColorCorrectionShortcutOn_shouldReturnOnSummary() {
        setColorCorrectionEnabled(true);
        setColorCorrectionShortcutEnabled(true);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.daltonizer_state_on));
    }

    @Test
    public void getSummary_disabledColorCorrectionShortcutOff_shouldReturnOffSummary() {
        setColorCorrectionEnabled(false);
        setColorCorrectionShortcutEnabled(false);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.daltonizer_state_off));
    }

    @Test
    public void getSummary_disabledColorCorrectionShortcutOn_shouldReturnOffSummary() {
        setColorCorrectionEnabled(false);
        setColorCorrectionShortcutEnabled(true);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.daltonizer_state_off));
    }

    private void setColorCorrectionEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, enabled ? ON : OFF);
    }

    private void setColorCorrectionShortcutEnabled(boolean enabled) {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS,
                enabled ? DALTONIZER_COMPONENT_NAME.flattenToString() : "");
    }
}

/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RunWith(RobolectricTestRunner.class)
public class ColorInversionPreferenceControllerTest {
    private static final String PREF_KEY = "toggle_inversion_preference";
    private static final String DISPLAY_INVERSION_ENABLED =
            Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;
    private Context mContext;
    private ColorInversionPreferenceController mController;
    private String mColorInversionSummary;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new ColorInversionPreferenceController(mContext, PREF_KEY);
        mColorInversionSummary = mContext.getString(R.string.color_inversion_feature_summary);
    }

    @Test
    public void getSummary_enabledColorInversionShortcutOff_shouldReturnOnSummary() {
        setColorInversionEnabled(true);
        setColorInversionShortcutEnabled(false);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.color_inversion_state_on));
    }

    @Test
    public void getSummary_enabledColorInversionShortcutOn_shouldReturnOnSummary() {
        setColorInversionEnabled(true);
        setColorInversionShortcutEnabled(true);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.color_inversion_state_on));
    }

    @Test
    public void getSummary_disabledColorInversionShortcutOff_shouldReturnOffSummary() {
        setColorInversionEnabled(false);
        setColorInversionShortcutEnabled(false);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.color_inversion_state_off));
    }

    @Test
    public void getSummary_disabledColorInversionShortcutOn_shouldReturnOffSummary() {
        setColorInversionEnabled(false);
        setColorInversionShortcutEnabled(true);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.color_inversion_state_off));
    }

    private void setColorInversionShortcutEnabled(boolean enabled) {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS,
                enabled ? COLOR_INVERSION_COMPONENT_NAME.flattenToString() : "");
    }

    private void setColorInversionEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                DISPLAY_INVERSION_ENABLED, enabled ? State.ON : State.OFF);
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
        int OFF = 0;
        int ON = 1;
    }
}

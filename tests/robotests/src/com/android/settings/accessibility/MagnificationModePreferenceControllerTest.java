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

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class MagnificationModePreferenceControllerTest {
    private static final String PREF_KEY = "screen_magnification_mode";
    // TODO(b/146019459): Use magnification_capability.
    private static final String KEY_CAPABILITY = Settings.System.MASTER_MONO;
    private static final int WINDOW_SCREEN_VALUE = 2;
    private static final int ALL_VALUE = 3;

    private Context mContext;
    private MagnificationModePreferenceController mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new MagnificationModePreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void getSummary_saveWindowScreen_shouldReturnWindowScreenSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                KEY_CAPABILITY, WINDOW_SCREEN_VALUE);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(
                        R.string.accessibility_magnification_area_settings_window_screen_summary));
    }

    @Test
    public void getSummary_saveAll_shouldReturnAllSummary() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                KEY_CAPABILITY, ALL_VALUE);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(
                        R.string.accessibility_magnification_area_settings_all_summary));
    }
}

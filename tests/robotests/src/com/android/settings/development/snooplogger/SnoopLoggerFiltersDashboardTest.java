/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.development.snooplogger;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SnoopLoggerFiltersDashboardTest {

    private SnoopLoggerFiltersDashboard mDashboard;

    @Before
    public void setUp() {
        mDashboard = new SnoopLoggerFiltersDashboard();
    }

    @Test
    public void shouldNotHaveHelpResource() {
        assertThat(mDashboard.getHelpResource()).isEqualTo(0);
    }

    @Test
    public void shouldLogAsSnoopLoggerFiltersPage() {
        assertThat(mDashboard.getMetricsCategory())
                .isEqualTo(SettingsEnums.SETTINGS_SNOOP_LOGGER_DASHBOARD);
    }

    @Test
    public void shouldUseSnoopLoggerFiltersPreferenceLayout() {
        assertThat(mDashboard.getPreferenceScreenResId())
                .isEqualTo(R.xml.snoop_logger_filters_settings);
    }
}

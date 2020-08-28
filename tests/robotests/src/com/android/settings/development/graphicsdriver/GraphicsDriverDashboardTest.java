/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.graphicsdriver;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GraphicsDriverDashboardTest {

    private GraphicsDriverDashboard mDashboard;

    @Before
    public void setUp() {
        mDashboard = new GraphicsDriverDashboard();
    }

    @Test
    public void getHelpResource_shouldReturn0() {
        assertThat(mDashboard.getHelpResource()).isEqualTo(0);
    }

    @Test
    public void getMetricesCategory_shouldReturnGraphicsDriverDashboard() {
        assertThat(mDashboard.getMetricsCategory())
                .isEqualTo(SettingsEnums.SETTINGS_GAME_DRIVER_DASHBOARD);
    }

    @Test
    public void getPreferenceScreen_shouldReturnGraphicsDriverSettings() {
        assertThat(mDashboard.getPreferenceScreenResId()).isEqualTo(R.xml.graphics_driver_settings);
    }
}

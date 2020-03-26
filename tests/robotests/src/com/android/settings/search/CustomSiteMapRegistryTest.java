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

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.DisplaySettings;
import com.android.settings.network.NetworkDashboardFragment;
import com.android.settings.security.SecuritySettings;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settings.wallpaper.WallpaperSuggestionActivity;
import com.android.settings.wifi.WifiSettings2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CustomSiteMapRegistryTest {

    @Test
    public void shouldContainScreenLockSettingsPairs() {
        assertThat(CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(ScreenLockSettings.class.getName()))
                .isEqualTo(SecuritySettings.class.getName());
    }

    @Test
    public void shouldContainWallpaperSuggestionActivityPairs() {
        assertThat(CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(
                WallpaperSuggestionActivity.class.getName()))
                .isEqualTo(DisplaySettings.class.getName());
    }

    @Test
    public void shouldContainWifiSettings2Pairs() {
        assertThat(CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(WifiSettings2.class.getName()))
                .isEqualTo(NetworkDashboardFragment.class.getName());
    }
}

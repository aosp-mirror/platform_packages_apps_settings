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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.backup.UserBackupSettingsActivity;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.connecteddevice.usb.UsbDetailsFragment;
import com.android.settings.fuelgauge.batteryusage.PowerUsageAdvanced;
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummary;
import com.android.settings.gestures.GestureNavigationSettingsFragment;
import com.android.settings.gestures.SystemNavigationGestureSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.RecentLocationAccessSeeAllFragment;
import com.android.settings.notification.zen.ZenModeBlockedEffectsSettings;
import com.android.settings.notification.zen.ZenModeRestrictNotificationsSettings;
import com.android.settings.security.SecuritySettings;
import com.android.settings.security.screenlock.ScreenLockSettings;
import com.android.settings.system.SystemDashboardFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CustomSiteMapRegistryTest {

    @Test
    public void shouldContainScreenLockSettingsPairs() {
        assertThat(CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(ScreenLockSettings.class.getName()))
                .isEqualTo(SecuritySettings.class.getName());
    }

    @Test
    public void shouldContainPowerUsageAdvancedPairs() {
        assertThat(CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(PowerUsageAdvanced.class.getName()))
                .isEqualTo(PowerUsageSummary.class.getName());
    }

    @Test
    public void shouldContainRecentLocationAccessSeeAllFragmentPairs() {
        assertThat(CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(
                RecentLocationAccessSeeAllFragment.class.getName())).isEqualTo(
                LocationSettings.class.getName());
    }

    @Test
    public void shouldContainUsbDetailsFragmentPairs() {
        assertThat(CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(
                UsbDetailsFragment.class.getName())).isEqualTo(
                ConnectedDeviceDashboardFragment.class.getName());
    }

    @Test
    public void shouldContainUserBackupSettingsActivityPairs() {
        assertThat(CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(
                UserBackupSettingsActivity.class.getName())).isEqualTo(
                SystemDashboardFragment.class.getName());
    }

    @Test
    public void shouldContainZenModeBlockedEffectsSettingsPairs() {
        assertThat(CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(
                ZenModeBlockedEffectsSettings.class.getName())).isEqualTo(
                ZenModeRestrictNotificationsSettings.class.getName());
    }

    @Test
    public void shouldContainGestureNavigationSettingsFragmentPairs() {
        assertThat(CustomSiteMapRegistry.CUSTOM_SITE_MAP.get(
                GestureNavigationSettingsFragment.class.getName())).isEqualTo(
                SystemNavigationGestureSettings.class.getName());
    }
}

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

package com.android.settings.homepage;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class HomepageComponentTest {
    public final String TAG = this.getClass().getSimpleName();

    private final Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();

    @Test
    public void test_launch_all_settings_in_home()
            throws ClassNotFoundException {

        List<Intent> launchIntents = ImmutableList.of(

                // Wifi
                // Implemented in WifiSettings2ActivityTest

                // Connected devices
                new Intent(Settings.ACTION_BLUETOOTH_SETTINGS),

                // Applications
                new Intent(Settings.ACTION_AUTO_ROTATE_SETTINGS),

                // Notifications
                new Intent(Settings.ACTION_NOTIFICATION_SETTINGS),

                // Display
                new Intent(Settings.ACTION_DISPLAY_SETTINGS),

                // Battery
                // Implemented in fuelgauge.batterysaver

                // Storage
                new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),

                // Sound
                new Intent(Settings.ACTION_SOUND_SETTINGS),

                // Display
                new Intent(Settings.ACTION_DISPLAY_SETTINGS),

                // Wallpaper
                new Intent(mInstrumentation.getTargetContext(), Class.forName(
                        "com.android.settings.wallpaper.WallpaperSuggestionActivity")),

                // A11y
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),

                // Security
                new Intent(Settings.ACTION_SECURITY_SETTINGS),

                // Privacy
                new Intent(Settings.ACTION_PRIVACY_SETTINGS),

                // Location
                new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),

                // Emergency ? EmergencyDashboardFragment
                // TODO: find out launch method

                // Password & Account
                new Intent(Settings.ACTION_SYNC_SETTINGS),

                // Digital wellbeing
                // Use IA link
                new Intent().setComponent(
                        new ComponentName(
                                "com.google.android.apps.wellbeing",
                                "com.google.android.apps.wellbeing.settings"
                                        + ".TopLevelSettingsActivity")),

                // Google
                // Use IA link
                new Intent().setComponent(
                        new ComponentName(
                                "com.google.android.gms",
                                "com.google.android.gms.app.settings.GoogleSettingsIALink")),

                // System ?
                // TODO: find out launch method.

                // About
                new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)

        );

        for (Intent intent : launchIntents) {
            Log.d(TAG, "Start to launch intent " + intent.getAction());
            try {
                mInstrumentation.getTargetContext()
                        .startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Log.e(TAG, "Launch with exception. " + e.toString());
                assert_().fail();
            }
            // Launch success without exception.
            assertThat(Boolean.TRUE).isTrue();
        }
    }
}

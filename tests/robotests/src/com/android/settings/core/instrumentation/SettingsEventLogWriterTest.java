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

package com.android.settings.core.instrumentation;


import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.metrics.LogMaker;
import android.provider.DeviceConfig;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class, SettingsEventLogWriterTest.ShadowMetricsLogger.class})
public class SettingsEventLogWriterTest {

    private SettingsEventLogWriter mWriter;

    @Before
    public void setUp() {
        mWriter = new SettingsEventLogWriter();
    }

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
        ShadowMetricsLogger.reset();
    }

    @Test
    public void visible_eventLogEnabled_shouldLog() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.GENERIC_EVENT_LOGGING_ENABLED, "true", true);

        mWriter.visible(RuntimeEnvironment.application, SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.SETTINGS_HOMEPAGE, 0);

        assertThat(ShadowMetricsLogger.sActionLoggedCount).isEqualTo(1);
    }

    @Test
    public void hidden_eventLogEnabled_shouldLog() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.GENERIC_EVENT_LOGGING_ENABLED, "true", true);

        mWriter.hidden(RuntimeEnvironment.application, SettingsEnums.SETTINGS_HOMEPAGE, 0);

        assertThat(ShadowMetricsLogger.sActionLoggedCount).isEqualTo(1);
    }

    @Test
    public void visible_eventLogDisabled_shouldNotLog() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.GENERIC_EVENT_LOGGING_ENABLED, "false", true);

        mWriter.visible(RuntimeEnvironment.application, SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.SETTINGS_HOMEPAGE, 0);

        assertThat(ShadowMetricsLogger.sActionLoggedCount).isEqualTo(0);
    }

    @Test
    public void hidden_eventLogDisabled_shouldNotLog() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.GENERIC_EVENT_LOGGING_ENABLED, "false", true);

        mWriter.hidden(RuntimeEnvironment.application, SettingsEnums.SETTINGS_HOMEPAGE, 0);

        assertThat(ShadowMetricsLogger.sActionLoggedCount).isEqualTo(0);
    }

    @Implements(MetricsLogger.class)
    public static class ShadowMetricsLogger {

        public static int sActionLoggedCount = 0;

        @Resetter
        public static void reset() {
            sActionLoggedCount = 0;
        }

        @Implementation
        protected static void action(LogMaker content) {
            sActionLoggedCount++;
        }

        @Implementation
        public static void hidden(Context context, int category) throws IllegalArgumentException {
            sActionLoggedCount++;
        }
    }

}

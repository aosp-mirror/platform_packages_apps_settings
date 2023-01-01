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

package com.android.settings.regionalpreferences;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static org.junit.Assert.assertEquals;

import android.app.UiAutomation;
import android.content.Context;
import android.os.SystemProperties;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RegionalPreferencesControllerTest {
    private boolean mCacheProperty = false;
    private Context mApplicationContext;
    private RegionalPreferencesController mController;

    @Before
    public void setUp() throws Exception {
        mApplicationContext = ApplicationProvider.getApplicationContext();
        mCacheProperty =
                SystemProperties.getBoolean(RegionalPreferencesController.FEATURE_PROPERTY, false);
        mController = new RegionalPreferencesController(mApplicationContext, "key");
    }

    @After
    public void tearDown() throws Exception {
        setProp(mCacheProperty);
    }

    @Test
    public void getAvailabilityStatus_systemPropertyIstrue_available() throws Exception {
        setProp(true);

        int result = mController.getAvailabilityStatus();

        assertEquals(AVAILABLE, result);
    }

    @Test
    public void getAvailabilityStatus_systemPropertyIstrue_unavailable() throws Exception {
        setProp(false);

        int result = mController.getAvailabilityStatus();

        assertEquals(CONDITIONALLY_UNAVAILABLE, result);
    }

    private static void setProp(boolean isEnabled) throws Exception {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.executeShellCommand(
                "setprop " + RegionalPreferencesController.FEATURE_PROPERTY + " " + isEnabled);

        for (int i = 0; i < 3; i++) {
            Thread.sleep(500);
            if (SystemProperties.getBoolean(
                    RegionalPreferencesController.FEATURE_PROPERTY, false) == isEnabled) {
                break;
            }
        }
    }
}

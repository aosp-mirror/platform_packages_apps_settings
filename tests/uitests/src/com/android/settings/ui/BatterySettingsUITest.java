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

package com.android.settings.ui;

import android.content.Intent;
import android.os.RemoteException;
import android.support.test.uiautomator.UiDevice;
import android.system.helpers.SettingsHelper;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.ui.testutils.SettingsTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class BatterySettingsUITest {
    // Items we really want to always show
    private static final String[] CATEGORIES = new String[] {
            "Battery Saver",
            "Battery percentage",
            "Battery usage data is approximate and can change based on usage",
    };

    private UiDevice mDevice;
    private SettingsHelper mHelper;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mHelper = SettingsHelper.getInstance();
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientaion", e);
        }
    }

    @After
    public void tearDown() throws Exception {
        // Go back to home for next test.
        mDevice.pressHome();
    }

    @Test
    public void launchSecuritySettings() throws Exception {
        // Launch Settings
        SettingsHelper.launchSettingsPage(
                InstrumentationRegistry.getTargetContext(), Intent.ACTION_POWER_USAGE_SUMMARY);
        mHelper.scrollVert(false);
        for (String category : CATEGORIES) {
            SettingsTestUtils.assertTitleMatch(mDevice, category);
        }
    }
}

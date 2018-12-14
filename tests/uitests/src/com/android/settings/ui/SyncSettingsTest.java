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

import static junit.framework.Assert.assertTrue;

import android.os.RemoteException;
import android.provider.Settings;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.system.helpers.SettingsHelper;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class SyncSettingsTest {
    private static final int TIMEOUT = 2000;

    private UiDevice mDevice;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientaion", e);
        }
    }

    @After
    public void tearDown() throws Exception {
        // Need to finish settings activity
        mDevice.pressHome();
    }

    @Test
    public void syncPageShouldHaveAddAccountButton() throws Exception {
        // Launch Settings
        SettingsHelper.launchSettingsPage(
                InstrumentationRegistry.getContext(), Settings.ACTION_SYNC_SETTINGS);
        UiObject2 addAccount = mDevice.wait(
                Until.findObject(By.text("Add account")), TIMEOUT);
        assertTrue(addAccount != null);
    }
}

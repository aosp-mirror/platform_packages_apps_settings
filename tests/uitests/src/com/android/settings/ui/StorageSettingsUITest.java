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

import android.os.RemoteException;
import android.provider.Settings;
import android.support.test.uiautomator.UiDevice;
import android.system.helpers.SettingsHelper;
import android.test.suitebuilder.annotation.MediumTest;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.ui.testutils.SettingsTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class StorageSettingsUITest {

    private static final String[] TITLES = {
            "Photos & videos",
            "Music & audio",
            "Games",
            "Movie & TV apps",
            "Other apps",
            "Files",
            "System",
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
            throw new RuntimeException("failed to freeze device orientation", e);
        }
    }

    @After
    public void tearDown() {
        mDevice.pressHome();
    }

    @Test
    public void launchStorageSettings() throws Exception {
        // Launch Settings
        SettingsHelper.launchSettingsPage(
                InstrumentationRegistry.getTargetContext(),
                Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        mHelper.scrollVert(false);
        for (String category : TITLES) {
            SettingsTestUtils.assertTitleMatch(mDevice, category);
        }
    }
}

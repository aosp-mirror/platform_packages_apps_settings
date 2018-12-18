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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.RemoteException;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ConnectedDeviceTests {

    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final int TIMEOUT = 2000;
    private UiDevice mDevice;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientation", e);
        }
    }

    @After
    public void tearDown() throws Exception {
        mDevice.pressBack();
        mDevice.pressHome();
    }

    // This NFC toggle test is set up this way since there's no way to set
    // the NFC flag to enabled or disabled without touching UI.
    // This way, we get coverage for whether or not the toggle button works.
    @Test
    public void testNFCToggle() throws Exception {
        NfcManager manager = (NfcManager) InstrumentationRegistry.getTargetContext()
                .getSystemService(Context.NFC_SERVICE);
        NfcAdapter nfcAdapter = manager.getDefaultAdapter();
        boolean nfcInitiallyEnabled = nfcAdapter.isEnabled();
        InstrumentationRegistry.getContext().startActivity(new Intent()
                .setClassName(
                        SETTINGS_PACKAGE,
                        "com.android.settings.Settings$ConnectedDeviceDashboardActivity"));
        UiObject2 nfcSetting = mDevice.wait(Until.findObject(By.text("NFC")), TIMEOUT);
        nfcSetting.click();
        Thread.sleep(TIMEOUT * 2);
        if (nfcInitiallyEnabled) {
            assertFalse("NFC wasn't disabled on toggle", nfcAdapter.isEnabled());
            nfcSetting.click();
            Thread.sleep(TIMEOUT * 2);
            assertTrue("NFC wasn't enabled on toggle", nfcAdapter.isEnabled());
        } else {
            assertTrue("NFC wasn't enabled on toggle", nfcAdapter.isEnabled());
            nfcSetting.click();
            Thread.sleep(TIMEOUT * 2);
            assertFalse("NFC wasn't disabled on toggle", nfcAdapter.isEnabled());
        }
    }
}

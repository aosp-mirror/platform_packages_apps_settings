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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.metrics.MetricsReader;
import android.os.RemoteException;
import android.platform.test.annotations.Presubmit;
import android.provider.Settings;
import android.support.test.metricshelper.MetricsAsserts;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.Until;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class BluetoothNetworkSettingsTests extends InstrumentationTestCase {

    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final int TIMEOUT = 2000;
    private static final int LONG_TIMEOUT = 40000;
    private UiDevice mDevice;
    private MetricsReader mMetricsReader;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientaion", e);
        }
        mMetricsReader = new MetricsReader();
        // Clear out old logs
        mMetricsReader.checkpoint();
    }

    @Override
    protected void tearDown() throws Exception {
        mDevice.pressBack();
        mDevice.pressHome();
        mDevice.waitForIdle();
        super.tearDown();
    }

    @Presubmit
    @MediumTest
    public void testBluetoothEnabled() throws Exception {
        verifyBluetoothOnOrOff(true);
        MetricsAsserts.assertHasActionLog("missing bluetooth toggle log",
                mMetricsReader, MetricsEvent.ACTION_BLUETOOTH_TOGGLE);
    }

    @Presubmit
    @MediumTest
    public void testBluetoothDisabled() throws Exception {
        verifyBluetoothOnOrOff(false);
        MetricsAsserts.assertHasActionLog("missing bluetooth toggle log",
                mMetricsReader, MetricsEvent.ACTION_BLUETOOTH_TOGGLE);
    }

    @MediumTest
    public void testRenameOption() throws Exception {
        launchBluetoothSettings();
        verifyUiObjectClicked(By.text("Device name"), "Rename preference");
        verifyUiObjectClicked(By.text("CANCEL"), "CANCEL button");

        MetricsAsserts.assertHasActionLog("missing bluetooth rename device log",
                mMetricsReader, MetricsEvent.ACTION_BLUETOOTH_RENAME);
        MetricsAsserts.assertHasVisibilityLog("missing bluetooth rename dialog log",
                mMetricsReader, MetricsEvent.DIALOG_BLUETOOTH_RENAME, true);
    }

    @MediumTest
    public void testReceivedFilesOption() throws Exception {
        launchBluetoothSettings();
        verifyUiObjectClicked(By.text("Received files"), "Received files preference");

        MetricsAsserts.assertHasActionLog("missing bluetooth received files log",
                mMetricsReader, MetricsEvent.ACTION_BLUETOOTH_FILES);
    }

    @MediumTest
    public void testHelpFeedbackOverflowOption() throws Exception {
        launchBluetoothSettings();

        // Verify help & feedback
        assertNotNull("Help & feedback item not found under Bluetooth Settings",
                mDevice.wait(Until.findObject(By.desc("Help & feedback")), TIMEOUT));
    }

    public void launchBluetoothSettings() throws Exception {
        Intent btIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        btIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getInstrumentation().getContext().startActivity(btIntent);
        Thread.sleep(TIMEOUT * 2);
    }

    /**
     * Find the {@link UiObject2} by {@code itemSelector} and try to click it if possible.
     *
     * If not find, throw assertion error
     * @param itemSelector used to find the {@link UiObject2}
     * @param text the description of the {@link UiObject2}
     */
    private void verifyUiObjectClicked(BySelector itemSelector, String text) throws Exception {
        UiObject2 uiObject2 = mDevice.wait(Until.findObject(itemSelector), TIMEOUT);
        assertNotNull(text + "is not present in bluetooth settings page", uiObject2);
        uiObject2.click();
    }

    /**
     * Toggles the Bluetooth switch and verifies that the change is reflected in Settings
     *
     * @param verifyOn set to whether you want the setting turned On or Off
     */
    private void verifyBluetoothOnOrOff(boolean verifyOn) throws Exception {
        String switchText = "ON";
        BluetoothAdapter bluetoothAdapter = ((BluetoothManager) getInstrumentation().getContext()
                .getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (verifyOn) {
            switchText = "OFF";
            bluetoothAdapter.disable();
        } else {
            bluetoothAdapter.enable();
        }
        launchBluetoothSettings();
        mDevice.wait(Until
                .findObject(By.res(SETTINGS_PACKAGE, "switch_widget").text(switchText)), TIMEOUT)
                .click();
        Thread.sleep(TIMEOUT);
        String bluetoothValue =
                Settings.Global.getString(getInstrumentation().getContext().getContentResolver(),
                        Settings.Global.BLUETOOTH_ON);
        if (verifyOn) {
            assertEquals("1", bluetoothValue);
        } else {
            assertEquals("0", bluetoothValue);
        }
    }
}

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
import android.os.SystemClock;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.Until;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Verify storage wizard flows. Temporarily enables a virtual disk which enables
 * testing on all devices, regardless of physical SD card support.
 */
@RunWith(AndroidJUnit4.class)
public class StorageWizardTest {
    private static final String ANDROID_PACKAGE = "android";
    private static final String PACKAGE = "com.android.settings";
    private static final int TIMEOUT = 5000;
    private static final int TIMEOUT_LONG = 30000;

    private UiDevice mDevice;

    private String mDisk;
    private String mVolume;

    @Before
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.executeShellCommand("setprop sys.debug.storage_slow 1");
        mDevice.executeShellCommand("sm set-virtual-disk true");

        mDisk = getAdoptableDisk();
        mDevice.executeShellCommand("sm partition " + mDisk + " public");
        mVolume = getPublicVolume();
    }

    @After
    public void tearDown() throws Exception {
        // Go back to home for next test.
        mDevice.pressBack();
        mDevice.pressBack();
        mDevice.pressHome();
        mDevice.waitForIdle(TIMEOUT);

        mDevice.executeShellCommand("setprop sys.debug.storage_slow 0");
        mDevice.executeShellCommand("sm set-virtual-disk false");
        mDevice.executeShellCommand("sm forget all");
    }

    /**
     * Test flow for adopting a storage device as internal/adopted.
     */
    @Test
    public void testInternal() throws Exception {
        InstrumentationRegistry.getContext().startActivity(buildInitIntent());

        // Activity: pick option to use as internal
        waitFor(By.res(PACKAGE, "suc_layout_title").text(containsIgnoringCase("How will you use")));
        waitFor(By.res(PACKAGE, "storage_wizard_init_internal")).click();

        // Dialog: acknowledge that we're formatting the card
        waitFor(By.res(ANDROID_PACKAGE, "alertTitle").textContains("Format"));
        waitFor(By.clickable(true).text(containsIgnoringCase("Format"))).click();

        // Activity: ack storage device is slow
        waitForLong(By.res(PACKAGE, "suc_layout_title").textContains("Slow"));
        waitFor(By.res(PACKAGE, "storage_next_button")).click();

        // Activity: choose to move content
        waitForLong(By.res(PACKAGE, "suc_layout_title").textContains("Move content"));
        waitFor(By.res(PACKAGE, "storage_next_button")).click();

        // Activity: yay, we're done!
        waitForLong(By.res(PACKAGE, "suc_layout_title").textContains("ready to use"));
        waitFor(By.res(PACKAGE, "storage_next_button")).click();
    }

    /**
     * Test flow for adopting a storage device as external/portable.
     */
    @Test
    public void testExternal() throws Exception {
        InstrumentationRegistry.getContext().startActivity(buildInitIntent());

        // Activity: pick option to use as external
        waitFor(By.res(PACKAGE, "suc_layout_title").textContains("How will you use"));
        waitFor(By.res(PACKAGE, "storage_wizard_init_external")).click();

        // Activity: yay, we're done!
        waitFor(By.res(PACKAGE, "suc_layout_title").textContains("ready to use"));
        waitFor(By.res(PACKAGE, "storage_next_button")).click();
    }

    private UiObject2 waitFor(BySelector selector) throws UiObjectNotFoundException {
        return waitFor(selector, TIMEOUT);
    }

    private UiObject2 waitForLong(BySelector selector) throws UiObjectNotFoundException {
        return waitFor(selector, TIMEOUT_LONG);
    }

    private UiObject2 waitFor(BySelector selector, long timeout) throws UiObjectNotFoundException {
        final UiObject2 item = mDevice.wait(Until.findObject(selector), timeout);
        if (item != null) {
            return item;
        } else {
            throw new UiObjectNotFoundException(selector.toString());
        }
    }

    /**
     * Shamelessly borrowed from AdoptableHostTest in CTS.
     */
    private String getAdoptableDisk() throws IOException {
        // In the case where we run multiple test we cleanup the state of the device. This
        // results in the execution of sm forget all which causes the MountService to "reset"
        // all its knowledge about available drives. This can cause the adoptable drive to
        // become temporarily unavailable.
        int attempt = 0;
        String disks = mDevice.executeShellCommand("sm list-disks adoptable");
        while ((disks == null || disks.isEmpty()) && attempt++ < 15) {
            SystemClock.sleep(1000);
            disks = mDevice.executeShellCommand("sm list-disks adoptable");
        }

        if (disks == null || disks.isEmpty()) {
            throw new AssertionError("Devices that claim to support adoptable storage must have "
                    + "adoptable media inserted during CTS to verify correct behavior");
        }
        return disks.split("\n")[0].trim();
    }

    private String getPublicVolume() throws IOException {
        int attempt = 0;
        String volumes = mDevice.executeShellCommand("sm list-volumes public");
        while ((volumes == null || volumes.isEmpty() || !volumes.contains("mounted"))
                && attempt++ < 15) {
            SystemClock.sleep(1000);
            volumes = mDevice.executeShellCommand("sm list-volumes public");
        }

        if (volumes == null || volumes.isEmpty()) {
            throw new AssertionError("Devices that claim to support adoptable storage must have "
                    + "adoptable media inserted during CTS to verify correct behavior");
        }
        return volumes.split("[\n ]")[0].trim();
    }

    private Intent buildInitIntent() {
        final Intent intent = new Intent().setClassName(PACKAGE,
                PACKAGE + ".deviceinfo.StorageWizardInit");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk);
        intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mVolume);
        return intent;
    }

    private Pattern containsIgnoringCase(String text) {
        return Pattern.compile("(?i)^.*" + text + ".*$");
    }
}

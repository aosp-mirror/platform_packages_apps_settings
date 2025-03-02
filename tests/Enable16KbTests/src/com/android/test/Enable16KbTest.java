/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.platform.test.annotations.AppModeFull;

import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.testtype.junit4.DeviceTestRunOptions;
import com.android.tradefed.util.RunUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.StringReader;

@RunWith(DeviceJUnit4ClassRunner.class)
public class Enable16KbTest extends BaseHostJUnit4Test {
    private static final String TEST_APP_NAME = "test_16kb_app.apk";

    private static final String APP_PACKAGE = "com.android.settings.development.test";

    private static final String TEST_NAME = "Enable16KbDeviceTest";

    private static final String SWITCH_TO_EXT4 = "enable16k_switchToExt4";

    private static final String SWITCH_TO_16KB = "enable16k_switchTo16Kb";

    private static final String SWITCH_TO_4KB = "enable16k_switchTo4Kb";
    private static final String DISABLE_DEV_OPTION = "enable16k_disableDeveloperOption";

    private static final int DEVICE_WAIT_TIMEOUT = 120000;
    private static final int DEVICE_UPDATE_TIMEOUT = 180000;

    @Test
    @AppModeFull
    public void enable16KbToggle() throws Exception {
        // Wait for 2 minutes for device to be online
        prepareDevice();
        if (!isPackageInstalled(APP_PACKAGE)) {
            //If test app has failed for some reason, retry installation
            installTestApp();
        }

        // Check if developer option is enabled otherwise exit
        prepareDevice();
        String result = getDevice().getProperty("ro.product.build.16k_page.enabled");
        assumeTrue("true".equals(result));

        // This test can be run on OEM unlocked device only as unlocking bootloader requires
        // manual intervention.
        result = getDevice().getProperty("ro.boot.flash.locked");
        assumeTrue("0".equals(result));

        getDevice().executeShellCommand("am start -a com.android.setupwizard.FOUR_CORNER_EXIT");

        // Enables developer option and switch to ext4
        runTestAndWait(SWITCH_TO_EXT4);
        getDevice().executeShellCommand("am start -a com.android.setupwizard.FOUR_CORNER_EXIT");
        assertTrue(verifyExt4());

        // Device will wiped. need to install test package again.
        installTestApp();

        // Enable developer option and switch to 16kb kernel and Check page size
        runTestAndWait(SWITCH_TO_16KB);
        result = getDevice().executeShellCommand("getconf PAGE_SIZE");
        assertEquals("16384", result.strip());

        // switch back to 4kb kernel and check page size
        runTestAndWait(SWITCH_TO_4KB);
        result = getDevice().executeShellCommand("getconf PAGE_SIZE");
        assertEquals("4096", result.strip());

        // Verify that developer options can't be turned off
        runDeviceTests(APP_PACKAGE, APP_PACKAGE + "." + TEST_NAME, DISABLE_DEV_OPTION);
    }

    private void installTestApp() throws Exception {
        DeviceTestRunOptions options = new DeviceTestRunOptions(null /* unused */);
        options.setApkFileName(TEST_APP_NAME);
        options.setInstallArgs("-r");
        installPackage(options);
        assertTrue(isPackageInstalled(APP_PACKAGE));
    }

    private void runTestAndWait(String testMethodName) throws Exception {
        prepareDevice();
        runDeviceTests(APP_PACKAGE, APP_PACKAGE + "." + TEST_NAME, testMethodName);
        // Device is either formatting or applying update. It usually takes 3 minutes to boot.
        RunUtil.getDefault().sleep(DEVICE_UPDATE_TIMEOUT);

        // make sure it is available again after the test
        prepareDevice();
    }

    private void prepareDevice() throws Exception {
        // Verify that device is online before running test and enable root
        getDevice().waitForDeviceOnline(DEVICE_WAIT_TIMEOUT);
        getDevice().enableAdbRoot();
        getDevice().waitForDeviceOnline(DEVICE_WAIT_TIMEOUT);

        getDevice().executeShellCommand("input keyevent KEYCODE_WAKEUP");
        getDevice().executeShellCommand("wm dismiss-keyguard");
    }

    private boolean verifyExt4() throws Exception {
        String result = getDevice().executeShellCommand("cat /proc/mounts");
        BufferedReader br = new BufferedReader(new StringReader(result));
        String line;
        while ((line = br.readLine()) != null) {
            final String[] fields = line.split(" ");
            final String partition = fields[1];
            final String fsType = fields[2];
            if (partition.equals("/data") && fsType.equals("ext4")) {
                return true;
            }
        }
        return false;
    }
}

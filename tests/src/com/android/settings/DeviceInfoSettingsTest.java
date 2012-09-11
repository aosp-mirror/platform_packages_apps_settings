/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings;

import android.test.AndroidTestCase;

import com.android.settings.DeviceInfoSettings;

public class DeviceInfoSettingsTest extends AndroidTestCase {

    public void testGetFormattedKernelVersion() throws Exception {
        if ("Unavailable".equals(DeviceInfoSettings.getFormattedKernelVersion())) {
            fail("formatKernelVersion can't cope with this device's /proc/version");
        }
    }

    public void testFormatKernelVersion() throws Exception {
        assertEquals("Unavailable", DeviceInfoSettings.formatKernelVersion(""));
        assertEquals("2.6.38.8-gg784\n" +
                     "root@hpao4.eem.corp.google.com #2\n" +
                     "Fri Feb 24 03:31:23 PST 2012",
                     DeviceInfoSettings.formatKernelVersion("Linux version 2.6.38.8-gg784 " +
                         "(root@hpao4.eem.corp.google.com) " +
                         "(gcc version 4.4.3 (Ubuntu 4.4.3-4ubuntu5) ) #2 SMP " +
                         "Fri Feb 24 03:31:23 PST 2012"));
        assertEquals("3.0.31-g6fb96c9\n" +
                     "android-build@vpbs1.mtv.corp.google.com #1\n" +
                     "Thu Jun 28 11:02:39 PDT 2012",
                     DeviceInfoSettings.formatKernelVersion("Linux version 3.0.31-g6fb96c9 " +
                         "(android-build@vpbs1.mtv.corp.google.com) " +
                         "(gcc version 4.6.x-google 20120106 (prerelease) (GCC) ) #1 " +
                         "SMP PREEMPT Thu Jun 28 11:02:39 PDT 2012"));
        assertEquals("2.6.38.8-a-b-jellybean+\n" +
                     "x@y #1\n" +
                     "Tue Aug 28 22:10:46 CDT 2012",
                     DeviceInfoSettings.formatKernelVersion("Linux version " +
                         "2.6.38.8-a-b-jellybean+ (x@y) " +
                         "(gcc version 4.4.3 (GCC) ) #1 PREEMPT Tue Aug 28 22:10:46 CDT 2012"));
    }
}

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

package com.android.settings.development;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.oemlock.OemLockManager;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Enable16kUtils {
    private static final long PAGE_SIZE = Os.sysconf(OsConstants._SC_PAGESIZE);
    private static final int PAGE_SIZE_16KB = 16 * 1024;

    @VisibleForTesting
    static final String DEV_OPTION_PROPERTY = "ro.product.build.16k_page.enabled";

    private static final String TAG = "Enable16kUtils";

    /**
     * @param context uses context to retrieve OEM unlock info
     * @return true if device is OEM unlocked and factory reset is allowed for user.
     */
    public static boolean isDeviceOEMUnlocked(@NonNull Context context) {
        // OEM unlock is checked for bootloader, carrier and user. Check all three to ensure
        // that device is unlocked and it is also allowed by user as well as carrier
        final OemLockManager oemLockManager = context.getSystemService(OemLockManager.class);
        final UserManager userManager = context.getSystemService(UserManager.class);
        if (oemLockManager == null || userManager == null) {
            Log.e(TAG, "Required services not found on device to check for OEM unlock state.");
            return false;
        }

        // If either of device or carrier is not allowed to unlock, return false
        if (!oemLockManager.isDeviceOemUnlocked()) {
            Log.e(TAG, "Device is not OEM unlocked");
            return false;
        }

        final UserHandle userHandle = UserHandle.of(UserHandle.myUserId());
        if (userManager.hasBaseUserRestriction(UserManager.DISALLOW_FACTORY_RESET, userHandle)) {
            Log.e(TAG, "Factory reset is not allowed for user.");
            return false;
        }

        return true;
    }

    /**
     * @return true if /data partition is ext4
     */
    public static boolean isDataExt4() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/mounts"))) {
            String line;
            while ((line = br.readLine()) != null) {
                final String[] fields = line.split(" ");
                final String partition = fields[1];
                final String fsType = fields[2];
                if (partition.equals("/data") && fsType.equals("ext4")) {
                    return true;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read /proc/mounts");
        }

        return false;
    }

    /**
     * @return returns true if 16KB developer option is available for the device.
     */
    public static boolean is16KbToggleAvailable() {
        return SystemProperties.getBoolean(DEV_OPTION_PROPERTY, false);
    }

    /**
     * 16kB page-agnostic mode requires /data to be ext4, ro.product.build.16k_page.enabled for
     * device and Device OEM unlocked.
     *
     * @param context is needed to query OEM unlock state
     * @return true if device is in page-agnostic mode.
     */
    public static boolean isPageAgnosticModeOn(@NonNull Context context) {
        return is16KbToggleAvailable() && isDeviceOEMUnlocked(context) && isDataExt4();
    }

    /**
     * @return returns true if current page size is 16KB
     */
    public static boolean isUsing16kbPages() {
        return PAGE_SIZE == PAGE_SIZE_16KB;
    }

    /**
     * show page-agnostic mode warning dialog to user
     * @param context to start activity
     */
    public static void showPageAgnosticWarning(@NonNull Context context) {
        Intent intent = new Intent(context, PageAgnosticWarningActivity.class);
        context.startActivityAsUser(intent, UserHandle.SYSTEM);
    }
}

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

package com.android.settings.security;

import android.os.SystemProperties;

import com.android.internal.os.Zygote;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.Arrays;

public class MemtagHelper {
    private static boolean isForcedOff() {
        return "force_off"
                .equals(
                        SystemProperties.get(
                                "persist.device_config.memory_safety_native_boot.bootloader_override"));
    }

    private static boolean isForcedOn() {
        return "force_on"
                .equals(
                        SystemProperties.get(
                                "persist.device_config.memory_safety_native_boot.bootloader_override"));
    }

    public static boolean isChecked() {
        String modes[] = SystemProperties.get("arm64.memtag.bootctl", "").split(",");
        return Arrays.asList(modes).contains("memtag");
    }

    public static void setChecked(boolean isChecked) {
        String newString = isChecked ? "memtag" : "none";
        SystemProperties.set("arm64.memtag.bootctl", newString);
    }

    public static int getAvailabilityStatus() {
        if (MemtagHelper.isForcedOff() || MemtagHelper.isForcedOn()) {
            return BasePreferenceController.DISABLED_DEPENDENT_SETTING;
        }
        return SystemProperties.getBoolean("ro.arm64.memtag.bootctl_settings_toggle", false)
                ? BasePreferenceController.AVAILABLE
                : BasePreferenceController.UNSUPPORTED_ON_DEVICE;
    }

    /**
     * Returns whether MTE is currently active on this device. We use this to determine whether we
     * need to reboot the device to apply the user choice.
     *
     * @return boolean whether MTE is currently active
     */
    public static boolean isOn() {
        return Zygote.nativeSupportsMemoryTagging();
    }

    public static int getSummary() {
        if (isForcedOff()) {
            return R.string.memtag_force_off;
        }
        if (isForcedOn()) {
            return R.string.memtag_force_on;
        }
        if (isOn()) {
            if (isChecked()) {
                return R.string.memtag_on;
            }
            return R.string.memtag_off_pending;
        }
        if (isChecked()) {
            return R.string.memtag_on_pending;
        }
        return R.string.memtag_off;
    }
}

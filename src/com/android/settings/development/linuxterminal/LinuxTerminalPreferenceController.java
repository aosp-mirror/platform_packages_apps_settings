/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.development.linuxterminal;

import static android.system.virtualmachine.VirtualMachineManager.CAPABILITY_NON_PROTECTED_VM;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.storage.StorageManager;
import android.system.virtualmachine.VirtualMachineManager;
import android.text.TextUtils;
import android.util.DataUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/** Preference controller for Linux terminal option in developers option */
public class LinuxTerminalPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {
    @VisibleForTesting
    static final int TERMINAL_PACKAGE_NAME_RESID = R.string.config_linux_terminal_app_package_name;

    @VisibleForTesting
    static final long MEMORY_MIN_BYTES = DataUnit.GIGABYTES.toBytes(4); // 4_000_000_000

    @VisibleForTesting
    static final long STORAGE_MIN_BYTES = DataUnit.GIGABYTES.toBytes(16); // 16_000_000_000

    private static final String LINUX_TERMINAL_KEY = "linux_terminal";

    @Nullable private final String mTerminalPackageName;
    private final boolean mIsDeviceCapable;

    public LinuxTerminalPreferenceController(@NonNull Context context) {
        super(context);
        String packageName = context.getString(TERMINAL_PACKAGE_NAME_RESID);
        mTerminalPackageName =
                isPackageInstalled(context.getPackageManager(), packageName) ? packageName : null;

        StorageManager storageManager = context.getSystemService(StorageManager.class);
        VirtualMachineManager virtualMachineManager =
                context.getSystemService(VirtualMachineManager.class);

        mIsDeviceCapable =
                getTotalMemory() >= MEMORY_MIN_BYTES
                        && storageManager != null
                        && storageManager.getPrimaryStorageSize() >= STORAGE_MIN_BYTES
                        && virtualMachineManager != null
                        && ((virtualMachineManager.getCapabilities() & CAPABILITY_NON_PROTECTED_VM)
                                != 0);
    }

    // Avoid lazy initialization because this may be called before displayPreference().
    @Override
    public boolean isAvailable() {
        // Check build flag RELEASE_AVF_SUPPORT_CUSTOM_VM_WITH_PARAVIRTUALIZED_DEVICES indirectly
        // by checking whether the terminal app is installed.
        // TODO(b/343795511): Add explicitly check for the flag when it's accessible from Java code.
        return mTerminalPackageName != null && mIsDeviceCapable;
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return LINUX_TERMINAL_KEY;
    }

    private static boolean isPackageInstalled(PackageManager manager, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            return manager.getPackageInfo(
                            packageName,
                            PackageManager.MATCH_ALL | PackageManager.MATCH_DISABLED_COMPONENTS)
                    != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // Can be overridden for test
    @VisibleForTesting
    long getTotalMemory() {
        return Process.getTotalMemory();
    }
}

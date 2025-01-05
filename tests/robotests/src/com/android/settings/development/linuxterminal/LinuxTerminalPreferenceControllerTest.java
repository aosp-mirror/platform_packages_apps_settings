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

import static com.android.settings.development.linuxterminal.LinuxTerminalPreferenceController.MEMORY_MIN_BYTES;
import static com.android.settings.development.linuxterminal.LinuxTerminalPreferenceController.STORAGE_MIN_BYTES;
import static com.android.settings.development.linuxterminal.LinuxTerminalPreferenceController.TERMINAL_PACKAGE_NAME_RESID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.storage.StorageManager;
import android.system.virtualmachine.VirtualMachineManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests {@link LinuxTerminalPreferenceController} */
@RunWith(RobolectricTestRunner.class)
public class LinuxTerminalPreferenceControllerTest {
    private static final String TERMINAL_PACKAGE_NAME = "com.android.virtualization.terminal";

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;
    @Mock private StorageManager mStorageManager;
    @Mock private VirtualMachineManager mVirtualMachineManager;
    @Mock private PackageInfo mPackageInfo;

    private LinuxTerminalPreferenceController mController;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(TERMINAL_PACKAGE_NAME).when(mContext).getString(TERMINAL_PACKAGE_NAME_RESID);

        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mPackageInfo)
                .when(mPackageManager)
                .getPackageInfo(eq(TERMINAL_PACKAGE_NAME), anyInt());

        doReturn(mStorageManager).when(mContext).getSystemService(StorageManager.class);
        doReturn(STORAGE_MIN_BYTES).when(mStorageManager).getPrimaryStorageSize();

        doReturn(mVirtualMachineManager)
                .when(mContext)
                .getSystemService(VirtualMachineManager.class);
        doReturn(CAPABILITY_NON_PROTECTED_VM).when(mVirtualMachineManager).getCapabilities();
    }

    @Test
    public void isAvailable_whenMemoryInsufficient_returnFalse() {
        mController = createController(mContext, MEMORY_MIN_BYTES / 2);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenDeviceStorageInsufficient_returnFalse() {
        doReturn(STORAGE_MIN_BYTES / 2).when(mStorageManager).getPrimaryStorageSize();

        mController = createController(mContext);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenVmNotSupported_returnFalse() {
        doReturn(0).when(mVirtualMachineManager).getCapabilities();

        mController = createController(mContext);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenPackageExists_returnsTrue() {
        mController = createController(mContext);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_whenPackageNameIsNull_returnsFalse() {
        doReturn(null).when(mContext).getString(TERMINAL_PACKAGE_NAME_RESID);

        mController = createController(mContext);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenAppDoesNotExist_returnsFalse() throws Exception {
        doThrow(new NameNotFoundException())
                .when(mPackageManager)
                .getPackageInfo(eq(TERMINAL_PACKAGE_NAME), anyInt());

        mController = createController(mContext);

        assertThat(mController.isAvailable()).isFalse();
    }

    private LinuxTerminalPreferenceController createController(Context context) {
        return createController(context, MEMORY_MIN_BYTES);
    }

    private LinuxTerminalPreferenceController createController(Context context, long totalMemory) {
        return new LinuxTerminalPreferenceController(context) {
            @Override
            public long getTotalMemory() {
                return totalMemory;
            }
        };
    }
}

/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;

import com.android.settings.testutils.ResolveInfoBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SmartAutoRotateBatterySaverControllerTest {

    private static final String PACKAGE_NAME = "package_name";

    private SmartAutoRotateBatterySaverController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = Mockito.spy(RuntimeEnvironment.application);
        final ContentResolver contentResolver = RuntimeEnvironment.application.getContentResolver();
        when(context.getContentResolver()).thenReturn(contentResolver);
        final PackageManager packageManager = Mockito.mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        doReturn(PACKAGE_NAME).when(packageManager).getRotationResolverPackageName();
        mController = Mockito.spy(
                new SmartAutoRotateBatterySaverController(context, "smart_auto_rotate"));
        when(mController.isPowerSaveMode()).thenReturn(false);

        final ResolveInfo resolveInfo = new ResolveInfoBuilder(PACKAGE_NAME).build();
        resolveInfo.serviceInfo = new ServiceInfo();
        when(packageManager.resolveService(any(), anyInt())).thenReturn(resolveInfo);
    }

    @Test
    public void getAvailabilityStatus_returnUnsupportedOnDevice() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_powerSaveModeEnabled_returnAvailableUnSearchAble() {
        when(mController.isPowerSaveMode()).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }
}

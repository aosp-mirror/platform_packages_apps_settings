/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.deviceinfo.legal;

import static com.android.settings.deviceinfo.legal.ModuleLicenseProvider.GZIPPED_LICENSE_FILE_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.android.settings.core.BasePreferenceController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class ModuleLicensesListPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "key";
    private static final String PACKAGE_NAME = "com.android.test_package";

    @Test
    public void getAvailabilityStatus_validLicenses_returnsAvailable()
            throws PackageManager.NameNotFoundException, IOException {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        ModuleInfo moduleInfo = new ModuleInfo();
        moduleInfo.setPackageName(PACKAGE_NAME);
        when(packageManager.getInstalledModules(0))
                .thenReturn(Collections.singletonList(moduleInfo));
        PackageInfo packageInfo = new PackageInfo();
        ApplicationInfo applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo = applicationInfo;
        when(packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.MATCH_APEX)).thenReturn(
                packageInfo);
        Resources resources = mock(Resources.class);
        when(packageManager.getResourcesForApplication(applicationInfo)).thenReturn(resources);
        AssetManager manager = mock(AssetManager.class);
        when(resources.getAssets()).thenReturn(manager);
        when(manager.list("")).thenReturn(new String[]{GZIPPED_LICENSE_FILE_NAME});

        ModuleLicensesListPreferenceController controller =
                new ModuleLicensesListPreferenceController(context, PREFERENCE_KEY);
        assertThat(controller.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noModules_returnsConditionallyUnavailable() {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getInstalledModules(0))
                .thenReturn(Collections.emptyList());

        ModuleLicensesListPreferenceController controller =
                new ModuleLicensesListPreferenceController(context, PREFERENCE_KEY);
        assertThat(controller.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noLicenses_returnsConditionallyUnavailable()
            throws PackageManager.NameNotFoundException, IOException {
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        ModuleInfo moduleInfo = new ModuleInfo();
        moduleInfo.setPackageName(PACKAGE_NAME);
        when(packageManager.getInstalledModules(0))
                .thenReturn(Collections.singletonList(moduleInfo));
        PackageInfo packageInfo = new PackageInfo();
        ApplicationInfo applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo = applicationInfo;
        when(packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.MATCH_APEX)).thenReturn(
                packageInfo);
        Resources resources = mock(Resources.class);
        when(packageManager.getResourcesForApplication(applicationInfo)).thenReturn(resources);
        AssetManager manager = mock(AssetManager.class);
        when(resources.getAssets()).thenReturn(manager);
        when(manager.list("")).thenReturn(new String[]{});

        ModuleLicensesListPreferenceController controller =
                new ModuleLicensesListPreferenceController(context, PREFERENCE_KEY);
        assertThat(controller.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }
}

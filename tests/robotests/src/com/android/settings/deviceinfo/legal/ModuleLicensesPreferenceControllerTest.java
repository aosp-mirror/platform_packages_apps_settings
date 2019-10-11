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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ModuleLicensesPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "key";
    private static final String MODULE_1_NAME = "Module 1";
    private static final String MODULE_1_PACKAGE_NAME = "com.android.module_one";
    private static final String MODULE_2_NAME = "Module 2";
    private static final String MODULE_2_PACKAGE_NAME = "com.android.module_two";
    private ModuleInfo mModuleOne;
    private ModuleInfo mModuleTwo;

    @Before
    public void setUp() {
        mModuleOne = new ModuleInfo();
        mModuleOne.setName(MODULE_1_NAME);
        mModuleOne.setPackageName(MODULE_1_PACKAGE_NAME);
        mModuleTwo = new ModuleInfo();
        mModuleTwo.setName(MODULE_2_NAME);
        mModuleTwo.setPackageName(MODULE_2_PACKAGE_NAME);
    }

    @Test
    public void displayPreference_alphabeticalOrder()
            throws PackageManager.NameNotFoundException, IOException {
        Context context = mock(Context.class);
        ModuleLicensesPreferenceController controller =
                new ModuleLicensesPreferenceController(context, PREFERENCE_KEY);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        PreferenceGroup group = spy(new MockPreferenceGroup(RuntimeEnvironment.application, null));
        when(screen.findPreference(PREFERENCE_KEY)).thenReturn(group);
        when(group.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(packageManager.getInstalledModules(0))
                .thenReturn(Arrays.asList(mModuleTwo, mModuleOne));
        PackageInfo packageInfo = new PackageInfo();
        ApplicationInfo applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo = applicationInfo;
        when(packageManager.getPackageInfo(MODULE_1_PACKAGE_NAME, PackageManager.MATCH_APEX))
                .thenReturn(packageInfo);
        when(packageManager.getPackageInfo(MODULE_2_PACKAGE_NAME, PackageManager.MATCH_APEX))
                .thenReturn(packageInfo);
        Resources resources = mock(Resources.class);
        when(packageManager.getResourcesForApplication(applicationInfo)).thenReturn(resources);
        AssetManager manager = mock(AssetManager.class);
        when(resources.getAssets()).thenReturn(manager);
        when(manager.list("")).thenReturn(new String[]{GZIPPED_LICENSE_FILE_NAME});

        controller.displayPreference(screen);

        assertThat(group.getPreferenceCount()).isEqualTo(2);
        assertThat(group.getPreference(0).getTitle()).isEqualTo(MODULE_1_NAME);
        assertThat(group.getPreference(1).getTitle()).isEqualTo(MODULE_2_NAME);
    }

    @Test
    public void displayPreference_includeOnlyModulesWithLicenseFile()
            throws PackageManager.NameNotFoundException, IOException {
        Context context = mock(Context.class);
        ModuleLicensesPreferenceController controller =
                new ModuleLicensesPreferenceController(context, PREFERENCE_KEY);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        PreferenceGroup group = spy(new MockPreferenceGroup(RuntimeEnvironment.application, null));
        when(screen.findPreference(PREFERENCE_KEY)).thenReturn(group);
        when(group.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(packageManager.getInstalledModules(0))
                .thenReturn(Arrays.asList(mModuleTwo, mModuleOne));
        PackageInfo packageInfo = new PackageInfo();
        ApplicationInfo applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo = applicationInfo;
        when(packageManager.getPackageInfo(MODULE_1_PACKAGE_NAME, PackageManager.MATCH_APEX))
                .thenReturn(packageInfo);
        Resources resources = mock(Resources.class);
        when(packageManager.getResourcesForApplication(applicationInfo)).thenReturn(resources);
        AssetManager manager = mock(AssetManager.class);
        when(resources.getAssets()).thenReturn(manager);
        when(manager.list("")).thenReturn(new String[]{GZIPPED_LICENSE_FILE_NAME});
        PackageInfo packageInfo2 = new PackageInfo();
        ApplicationInfo applicationInfo2 = new ApplicationInfo();
        packageInfo2.applicationInfo = applicationInfo2;
        when(packageManager.getPackageInfo(MODULE_2_PACKAGE_NAME, PackageManager.MATCH_APEX))
                .thenReturn(packageInfo2);
        Resources resources2 = mock(Resources.class);
        when(packageManager.getResourcesForApplication(applicationInfo2)).thenReturn(resources2);
        AssetManager manager2 = mock(AssetManager.class);
        when(resources2.getAssets()).thenReturn(manager2);
        when(manager2.list("")).thenReturn(new String[]{});

        controller.displayPreference(screen);

        assertThat(group.getPreferenceCount()).isEqualTo(1);
        assertThat(group.getPreference(0).getTitle()).isEqualTo(MODULE_1_NAME);
    }

    private static class MockPreferenceGroup extends PreferenceGroup {
        List<Preference> mList = new ArrayList<>();

        public MockPreferenceGroup(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean addPreference(Preference preference) {
            mList.add(preference);
            return true;
        }

        @Override
        public int getPreferenceCount() {
            return mList.size();
        }

        @Override
        public Preference getPreference(int index) {
            return mList.get(index);
        }
    }
}

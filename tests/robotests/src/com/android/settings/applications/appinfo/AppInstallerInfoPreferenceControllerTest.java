/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppInstallerInfoPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mAppInfo;
    @Mock
    private InstallSourceInfo mInstallSourceInfo;
    @Mock
    private AppInfoDashboardFragment mFragment;
    @Mock
    private Preference mPreference;

    private Context mContext;
    private AppInstallerInfoPreferenceController mController;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        final String installerPackage = "Installer1";
        when(mPackageManager.getInstallSourceInfo(anyString())).thenReturn(mInstallSourceInfo);
        when(mInstallSourceInfo.getInstallingPackageName()).thenReturn(installerPackage);
        when(mPackageManager.getApplicationInfo(eq(installerPackage), anyInt()))
                .thenReturn(mAppInfo);
        mController = new AppInstallerInfoPreferenceController(mContext, "test_key");
        mController.setPackageName("Package1");
        mController.setParentFragment(mFragment);
    }

    @Test
    public void getAvailabilityStatus_noAppLabel_shouldReturnDisabled() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_FOR_USER);
    }

    @Test
    public void getAvailabilityStatus_hasAppLabel_shouldReturnAvailable()
            throws PackageManager.NameNotFoundException {
        final String packageName = "Package1";
        when(mAppInfo.loadLabel(mPackageManager)).thenReturn("Label1");
        mController = new AppInstallerInfoPreferenceController(mContext, "test_key");
        mController.setPackageName(packageName);
        mController.setParentFragment(mFragment);
        mockMainlineModule(packageName, false /* isMainlineModule */);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_shouldSetSummary() {
        final PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.applicationInfo = mAppInfo;
        when(mFragment.getPackageInfo()).thenReturn(packageInfo);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(any());
    }

    @Test
    public void updateState_noAppStoreLink_shouldDisablePreference() {
        final PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.applicationInfo = mAppInfo;
        when(mFragment.getPackageInfo()).thenReturn(packageInfo);
        when(mPackageManager.resolveActivity(any(), anyInt())).thenReturn(null);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(false);
    }

    @Test
    public void updateState_hasAppStoreLink_shouldSetPreferenceIntent() {
        final PackageInfo packageInfo = mock(PackageInfo.class);
        packageInfo.applicationInfo = mAppInfo;
        when(mFragment.getPackageInfo()).thenReturn(packageInfo);
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = "Pkg1";
        resolveInfo.activityInfo.name = "Name1";
        when(mPackageManager.resolveActivity(any(), anyInt())).thenReturn(resolveInfo);

        mController.updateState(mPreference);

        verify(mPreference, never()).setEnabled(false);
        verify(mPreference).setIntent(any(Intent.class));
    }

    @Test
    public void getAvailabilityStatus_isMainlineModule_shouldReturnDisabled()
            throws PackageManager.NameNotFoundException {
        final String packageName = "Package";
        when(mAppInfo.loadLabel(mPackageManager)).thenReturn("Label");
        mController.setPackageName(packageName);
        mockMainlineModule(packageName, true /* isMainlineModule */);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.DISABLED_FOR_USER);
    }

    private void mockMainlineModule(String packageName, boolean isMainlineModule)
            throws PackageManager.NameNotFoundException {
        final PackageInfo packageInfo = new PackageInfo();
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.sourceDir = "apex";
        packageInfo.applicationInfo = applicationInfo;

        if (isMainlineModule) {
            when(mPackageManager.getModuleInfo(packageName, 0 /* flags */)).thenReturn(
                    new ModuleInfo());
        } else {
            when(mPackageManager.getPackageInfo(packageName, 0 /* flags */)).thenReturn(
                    packageInfo);
            when(mPackageManager.getModuleInfo(packageName, 0 /* flags */)).thenThrow(
                    new PackageManager.NameNotFoundException());
        }
    }
}

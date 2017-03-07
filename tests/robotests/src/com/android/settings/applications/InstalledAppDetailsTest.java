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

package com.android.settings.applications;


import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.StorageStatsSource.AppStorageStats;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class InstalledAppDetailsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock
    private Activity mActivity;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private InstalledAppDetails mAppDetail;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAppDetail = new InstalledAppDetails();
    }

    @Test
    public void getInstallationStatus_notInstalled_shouldReturnUninstalled() {

        assertThat(mAppDetail.getInstallationStatus(new ApplicationInfo()))
                .isEqualTo(R.string.not_installed);
    }

    @Test
    public void getInstallationStatus_enabled_shouldReturnInstalled() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;

        assertThat(mAppDetail.getInstallationStatus(info)).isEqualTo(R.string.installed);
    }

    @Test
    public void getInstallationStatus_disabled_shouldReturnDisabled() {
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = false;

        assertThat(mAppDetail.getInstallationStatus(info)).isEqualTo(R.string.disabled);
    }

    @Test
    public void shouldShowUninstallForAll_installForOneOtherUserOnly_shouldReturnTrue() {
        when(mDevicePolicyManager.packageHasActiveAdmins(anyString())).thenReturn(false);
        when(mUserManager.getUsers().size()).thenReturn(2);
        ReflectionHelpers.setField(mAppDetail, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mAppDetail, "mUserManager", mUserManager);
        final ApplicationInfo info = new ApplicationInfo();
        info.enabled = true;
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        final PackageInfo packageInfo = mock(PackageInfo.class);
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", packageInfo);

        assertThat(mAppDetail.shouldShowUninstallForAll(appEntry)).isTrue();
    }

    @Test
    public void shouldShowUninstallForAll_installForSelfOnly_shouldReturnFalse() {
        when(mDevicePolicyManager.packageHasActiveAdmins(anyString())).thenReturn(false);
        when(mUserManager.getUsers().size()).thenReturn(2);
        ReflectionHelpers.setField(mAppDetail, "mDpm", mDevicePolicyManager);
        ReflectionHelpers.setField(mAppDetail, "mUserManager", mUserManager);
        final ApplicationInfo info = new ApplicationInfo();
        info.flags = ApplicationInfo.FLAG_INSTALLED;
        info.enabled = true;
        final AppEntry appEntry = mock(AppEntry.class);
        appEntry.info = info;
        final PackageInfo packageInfo = mock(PackageInfo.class);
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", packageInfo);

        assertThat(mAppDetail.shouldShowUninstallForAll(appEntry)).isFalse();
    }

    @Test
    public void getStorageSummary_shouldWorkForExternal() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        AppStorageStats stats = mock(AppStorageStats.class);
        when(stats.getTotalBytes()).thenReturn(1L);

        assertThat(InstalledAppDetails.getStorageSummary(context, stats, true))
                .isEqualTo("1.00B used in External storage");
    }

    @Test
    public void getStorageSummary_shouldWorkForInternal() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        AppStorageStats stats = mock(AppStorageStats.class);
        when(stats.getTotalBytes()).thenReturn(1L);

        assertThat(InstalledAppDetails.getStorageSummary(context, stats, false))
                .isEqualTo("1.00B used in Internal storage");
    }

    @Test
    public void launchFragment_hasNoPackageInfo_shouldFinish() {
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", null);

        assertThat(mAppDetail.ensurePackageInfoAvailable(mActivity)).isFalse();
        verify(mActivity).finishAndRemoveTask();
    }

    @Test
    public void launchFragment_hasPackageInfo_shouldReturnTrue() {
        final PackageInfo packageInfo = mock(PackageInfo.class);
        ReflectionHelpers.setField(mAppDetail, "mPackageInfo", packageInfo);

        assertThat(mAppDetail.ensurePackageInfoAvailable(mActivity)).isTrue();
        verify(mActivity, never()).finishAndRemoveTask();
    }
}

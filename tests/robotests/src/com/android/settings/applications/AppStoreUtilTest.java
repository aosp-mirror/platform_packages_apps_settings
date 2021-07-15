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

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppStoreUtilTest {

    private static final String PACKAGE_NAME = "com.android.app";
    private static final String INSTALLING_PACKAGE_NAME = "com.android.installing";
    private static final String INITIATING_PACKAGE_NAME = "com.android.initiating";
    private static final String ORIGINATING_PACKAGE_NAME = "com.android.originating";

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mInitiatingAppInfo;
    @Mock
    private InstallSourceInfo mInstallSourceInfo;

    private Context mContext;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getInstallSourceInfo(anyString())).thenReturn(mInstallSourceInfo);
        when(mInstallSourceInfo.getInstallingPackageName()).thenReturn(INSTALLING_PACKAGE_NAME);
        when(mInstallSourceInfo.getInitiatingPackageName()).thenReturn(INITIATING_PACKAGE_NAME);
        when(mInstallSourceInfo.getOriginatingPackageName()).thenReturn(ORIGINATING_PACKAGE_NAME);
        when(mPackageManager.getApplicationInfo(eq(INITIATING_PACKAGE_NAME), anyInt()))
                .thenReturn(mInitiatingAppInfo);
    }

    @Test
    public void getInstallerPackageName_hasOriginatingByNonSystem_shouldReturnInstalling() {
        assertThat(AppStoreUtil.getInstallerPackageName(mContext, PACKAGE_NAME))
                .isEqualTo(INSTALLING_PACKAGE_NAME);
    }

    @Test
    public void getInstallerPackageName_hasOriginatingBySystem_shouldReturnOriginating() {
        mInitiatingAppInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        assertThat(AppStoreUtil.getInstallerPackageName(mContext, PACKAGE_NAME))
                .isEqualTo(ORIGINATING_PACKAGE_NAME);
    }

    @Test
    public void getInstallerPackageName_noInitiating_shouldReturnInstalling() {
        when(mInstallSourceInfo.getInitiatingPackageName()).thenReturn(null);
        assertThat(AppStoreUtil.getInstallerPackageName(mContext, PACKAGE_NAME))
                .isEqualTo(INSTALLING_PACKAGE_NAME);
    }

    @Test
    public void getInstallerPackageName_noOriginating_shouldReturnInstalling() {
        when(mInstallSourceInfo.getOriginatingPackageName()).thenReturn(null);
        assertThat(AppStoreUtil.getInstallerPackageName(mContext, PACKAGE_NAME))
                .isEqualTo(INSTALLING_PACKAGE_NAME);
    }
}

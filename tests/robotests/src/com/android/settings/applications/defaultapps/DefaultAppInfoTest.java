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

package com.android.settings.applications.defaultapps;


import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DefaultAppInfoTest {

    @Mock
    private ActivityInfo mActivityInfo;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private ComponentName mComponentName;
    @Mock
    private PackageManager mPackageManager;

    private DefaultAppInfo mInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void initInfoWithActivityInfo_shouldLoadInfo() {
        mActivityInfo.packageName = "test";
        mInfo = new DefaultAppInfo(mActivityInfo);
        mInfo.loadLabel(mPackageManager);
        mInfo.loadIcon(mPackageManager);

        assertThat(mInfo.getKey()).isEqualTo(mActivityInfo.packageName);
        verify(mActivityInfo).loadLabel(mPackageManager);
        verify(mActivityInfo).loadIcon(mPackageManager);
    }

    @Test
    public void initInfoWithApplicationInfo_shouldLoadInfo() {
        mApplicationInfo.packageName = "test";

        mInfo = new DefaultAppInfo(mApplicationInfo);
        mInfo.loadLabel(mPackageManager);
        mInfo.loadIcon(mPackageManager);

        assertThat(mInfo.getKey()).isEqualTo(mApplicationInfo.packageName);
        verify(mApplicationInfo).loadLabel(mPackageManager);
        verify(mApplicationInfo).loadIcon(mPackageManager);
    }

    @Test
    public void initInfoWithComponent_shouldLoadInfo() {
        when(mComponentName.getPackageName()).thenReturn("com.android.settings");

        mInfo = new DefaultAppInfo(0 /* uid */, mComponentName);
        mInfo.getKey();

        verify(mComponentName).flattenToString();
    }
}

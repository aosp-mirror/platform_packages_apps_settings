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


import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.PackageManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DefaultAppInfoTest {

    @Mock
    private PackageItemInfo mPackageItemInfo;
    @Mock
    private ComponentName mComponentName;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PackageManagerWrapper mPackageManagerWrapper;

    private DefaultAppInfo mInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mPackageManagerWrapper.getPackageManager()).thenReturn(mPackageManager);
    }

    @Test
    public void initInfoWithActivityInfo_shouldLoadInfo() {
        mPackageItemInfo.packageName = "test";
        mInfo = new DefaultAppInfo(mPackageManagerWrapper, mPackageItemInfo);
        mInfo.loadLabel();
        mInfo.loadIcon();

        assertThat(mInfo.getKey()).isEqualTo(mPackageItemInfo.packageName);
        verify(mPackageItemInfo).loadLabel(mPackageManager);
        verify(mPackageItemInfo).loadIcon(mPackageManager);
    }

    @Test
    public void initInfoWithComponent_shouldLoadInfo() {
        when(mComponentName.getPackageName()).thenReturn("com.android.settings");

        mInfo = new DefaultAppInfo(mPackageManagerWrapper, 0 /* uid */, mComponentName);
        mInfo.getKey();

        verify(mComponentName).flattenToString();
    }
}

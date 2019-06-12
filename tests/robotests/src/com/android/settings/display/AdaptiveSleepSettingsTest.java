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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AdaptiveSleepSettingsTest {
    private AdaptiveSleepSettings mSettings;
    private static final String PACKAGE_NAME = "package_name";
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Context context = RuntimeEnvironment.application;
        mSettings = spy(new AdaptiveSleepSettings());

        doReturn(PACKAGE_NAME).when(mPackageManager).getAttentionServicePackageName();
        doReturn(PackageManager.PERMISSION_GRANTED).when(mPackageManager).checkPermission(
                Manifest.permission.CAMERA, PACKAGE_NAME);
        doReturn(mScreen).when(mSettings).getPreferenceScreen();

        mSettings.setupForTesting(mPackageManager, context);
        mSettings.onAttach(context);

    }

    @Test
    public void onResume_hasPermission_preferenceInvisible() {
        mSettings.onResume();

        assertThat(mSettings.mPermissionRequiredPreference.isVisible()).isFalse();
    }

    @Test
    public void onResume_noPermission_preferenceVisible() {
        doReturn(PackageManager.PERMISSION_DENIED).when(mPackageManager).checkPermission(
                Manifest.permission.CAMERA, PACKAGE_NAME);

        mSettings.onResume();

        assertThat(mSettings.mPermissionRequiredPreference.isVisible()).isTrue();
    }
}

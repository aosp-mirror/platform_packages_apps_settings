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

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AdaptiveSleepPermissionPreferenceControllerTest {
    private final static String PACKAGE_NAME = "package_name";
    private AdaptiveSleepPermissionPreferenceController mController;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = Mockito.spy(RuntimeEnvironment.application);
        doReturn(mPackageManager).when(context).getPackageManager();
        doReturn(PACKAGE_NAME).when(mPackageManager).getAttentionServicePackageName();
        doReturn(PackageManager.PERMISSION_GRANTED).when(mPackageManager).checkPermission(
                Manifest.permission.CAMERA, PACKAGE_NAME);
        mController = new AdaptiveSleepPermissionPreferenceController(context, "test_key");
        doReturn(mController.getPreferenceKey()).when(mPreference).getKey();
    }

    @Test
    public void getAvailabilityStatus_returnAvailableUnsearchable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void updateStates_permissionGranted_preferenceInvisible() {
        mController.updateState(mPreference);

        verify(mPreference).setVisible(false);
    }

    @Test
    public void updateStates_permissionRevoked_preferenceVisible() {
        doReturn(PackageManager.PERMISSION_DENIED).when(mPackageManager).checkPermission(
                Manifest.permission.CAMERA, PACKAGE_NAME);

        mController.updateState(mPreference);

        verify(mPreference).setVisible(true);
    }
}

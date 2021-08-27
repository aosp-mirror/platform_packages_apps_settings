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
 * limitations under the License.
 */

package com.android.settings.display;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AdaptiveSleepPermissionPreferenceControllerTest {
    private Context mContext;
    private AdaptiveSleepPermissionPreferenceController mController;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(getApplicationContext());

        doReturn(mPackageManager).when(mContext).getPackageManager();
        when(mPackageManager.getAttentionServicePackageName()).thenReturn("some.package");
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_GRANTED);

        mController = new AdaptiveSleepPermissionPreferenceController(mContext);
    }

    @Test
    public void addToScreen_normalCase_hidePreference() {
        mController.addToScreen(mScreen);

        verify(mScreen, never()).addPreference(mController.mPreference);
    }

    @Test
    public void addToScreen_permissionNotGranted_showPreference() {
        when(mPackageManager.checkPermission(any(), any())).thenReturn(
                PackageManager.PERMISSION_DENIED);

        mController.addToScreen(mScreen);

        verify(mScreen).addPreference(mController.mPreference);
    }
}

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

package com.android.settings.vpn2;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unittest for AppPreference */
@RunWith(AndroidJUnit4.class)
public class AppPreferenceTest {
    // Additional mocking of the underying classes is necsesary if another user id is used.
    private static final int USER_ID = UserHandle.USER_NULL;
    private static final String PACKAGE_NAME = "test_package";
    private static final String DIFFERENT_PACKAGE_NAME = "not_test_package";

    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private Context mContext;
    private AppPreference mAppPreference;

    @Before
    public void setUp() throws NameNotFoundException {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mContext).when(mContext).createContextAsUser(any(), anyInt());
        doReturn(mContext).when(mContext).createPackageContextAsUser(any(), anyInt(), any());
        when(mContext.getSystemService(DevicePolicyManager.class)).thenReturn(mDevicePolicyManager);
    }

    @Test
    public void getPackageName_returnsAccuratePackageName() {
        doReturn(DIFFERENT_PACKAGE_NAME).when(mDevicePolicyManager).getAlwaysOnVpnPackage();

        mAppPreference = spy(new AppPreference(mContext, USER_ID, PACKAGE_NAME));
        assertThat(mAppPreference.getPackageName()).isEqualTo(PACKAGE_NAME);
    }

    @Test
    public void disableIfConfiguredByAdmin_packageNameNotEqualsAlwaysOn_shouldEnable() {
        doReturn(DIFFERENT_PACKAGE_NAME).when(mDevicePolicyManager).getAlwaysOnVpnPackage();

        mAppPreference = spy(new AppPreference(mContext, USER_ID, PACKAGE_NAME));
        assertFalse(mAppPreference.isDisabledByAdmin());
    }
}

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

package com.android.settings.password;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static com.android.settings.password.PasswordUtils.getCallingAppLabel;
import static com.android.settings.password.PasswordUtils.getCallingAppPackageName;
import static com.android.settings.password.PasswordUtils.isCallingAppPermitted;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.IActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.settings.testutils.shadow.ShadowActivityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowActivityManager.class})
public class PasswordUtilsTest {

    private static final String PACKAGE_NAME = "com.android.app";
    private static final String PERMISSION = "com.testing.permission";
    private static final int UID = 1234;

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private IActivityManager mActivityService;
    @Mock
    private IBinder mActivityToken;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        ShadowActivityManager.setService(mActivityService);
    }

    @Test
    public void getCallingAppLabel_getCallingAppPackageNameReturnsNull_returnsNull()
            throws Exception {
        when(mActivityService.getLaunchedFromPackage(mActivityToken))
                .thenThrow(new RemoteException());

        assertThat(getCallingAppLabel(mContext, mActivityToken)).isNull();
    }

    @Test
    public void getCallingAppLabel_getCallingAppPackageNameReturnsSettingsApp_returnsNull()
            throws Exception {
        when(mActivityService.getLaunchedFromPackage(mActivityToken))
                .thenReturn("com.android.settings");

        assertThat(getCallingAppLabel(mContext, mActivityToken)).isNull();
    }

    @Test
    public void getCallingAppLabel_packageManagerThrowsNameNotFound_returnsNull() throws Exception {
        when(mActivityService.getLaunchedFromPackage(mActivityToken))
                .thenReturn(PACKAGE_NAME);
        when(mPackageManager.getApplicationInfo(eq(PACKAGE_NAME), anyInt()))
                .thenThrow(new NameNotFoundException());

        assertThat(getCallingAppLabel(mContext, mActivityToken)).isNull();
    }

    @Test
    public void getCallingAppLabel_returnsLabel() throws Exception {
        when(mActivityService.getLaunchedFromPackage(mActivityToken))
                .thenReturn(PACKAGE_NAME);
        when(mPackageManager.getApplicationInfo(eq(PACKAGE_NAME), anyInt()))
                .thenReturn(mApplicationInfo);
        when(mApplicationInfo.loadLabel(mPackageManager)).thenReturn("label");

        assertThat(getCallingAppLabel(mContext, mActivityToken)).isEqualTo("label");
    }

    @Test
    public void getCallingAppPackageName_activityServiceThrowsRemoteException_returnsNull()
            throws Exception {
        when(mActivityService.getLaunchedFromPackage(mActivityToken))
                .thenThrow(new RemoteException());

        assertThat(getCallingAppPackageName(mActivityToken)).isNull();
    }

    @Test
    public void getCallingAppPackageName_returnsPackageName() throws Exception {
        when(mActivityService.getLaunchedFromPackage(mActivityToken))
                .thenReturn(PACKAGE_NAME);

        assertThat(getCallingAppPackageName(mActivityToken)).isEqualTo(PACKAGE_NAME);
    }

    @Test
    public void isCallingAppPermitted_permissionGranted_returnsTrue() throws Exception {
        when(mActivityService.getLaunchedFromUid(mActivityToken)).thenReturn(UID);
        when(mContext.checkPermission(PERMISSION, -1, UID)).thenReturn(PERMISSION_GRANTED);

        assertThat(isCallingAppPermitted(mContext, mActivityToken, PERMISSION)).isTrue();
    }

    @Test
    public void isCallingAppPermitted_permissionDenied_returnsFalse() throws Exception {
        when(mActivityService.getLaunchedFromUid(mActivityToken)).thenReturn(UID);
        when(mContext.checkPermission(PERMISSION, -1, UID)).thenReturn(PERMISSION_DENIED);

        assertThat(isCallingAppPermitted(mContext, mActivityToken, PERMISSION)).isFalse();
    }

    @Test
    public void isCallingAppPermitted_throwsRemoteException_returnsFalse() throws Exception {
        when(mActivityService.getLaunchedFromUid(mActivityToken)).thenThrow(new RemoteException());

        assertThat(isCallingAppPermitted(mContext, mActivityToken, PERMISSION)).isFalse();
    }
}

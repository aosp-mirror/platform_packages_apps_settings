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

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class DefaultAppsPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "DefaultApps";

    private static final String BROWSER_PACKAGE_NAME = "com.example.browser1";
    private static final String DIALER_PACKAGE_NAME = "com.example.dialer1";
    private static final String SMS_PACKAGE_NAME = "com.example.sms1";

    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private RoleManager mRoleManager;
    @Mock
    private ApplicationInfo mBrowserApplicationInfo;
    @Mock
    private ApplicationInfo mDialerApplicationInfo;
    @Mock
    private ApplicationInfo mSmsApplicationInfo;

    private DefaultAppsPreferenceController mPreferenceController;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        MockitoAnnotations.initMocks(this);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(RoleManager.class)).thenReturn(mRoleManager);

        when(mBrowserApplicationInfo.loadLabel(mPackageManager)).thenReturn("Browser1");
        when(mPackageManager.getApplicationInfo(eq(BROWSER_PACKAGE_NAME), anyInt())).thenReturn(
                mBrowserApplicationInfo);
        when(mDialerApplicationInfo.loadLabel(mPackageManager)).thenReturn("Phone1");
        when(mPackageManager.getApplicationInfo(eq(DIALER_PACKAGE_NAME), anyInt())).thenReturn(
                mDialerApplicationInfo);
        when(mSmsApplicationInfo.loadLabel(mPackageManager)).thenReturn("Sms1");
        when(mPackageManager.getApplicationInfo(eq(SMS_PACKAGE_NAME), anyInt())).thenReturn(
                mSmsApplicationInfo);

        mPreferenceController = new DefaultAppsPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void isAvailable_shouldReturnTrue() {
        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    public void getSummary_allAvailable_shouldReturnAll() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.singletonList(BROWSER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.singletonList(DIALER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(
                Collections.singletonList(SMS_PACKAGE_NAME));

        assertThat(mPreferenceController.getSummary()).isEqualTo("Browser1, Phone1, and Sms1");
    }

    @Test
    public void getSummary_browserAndDialerAvailable_shouldReturnBrowserAndDialer() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.singletonList(BROWSER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.singletonList(DIALER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(Collections.emptyList());

        assertThat(mPreferenceController.getSummary()).isEqualTo("Browser1 and Phone1");
    }

    @Test
    public void getSummary_browserAndSmsAvailable_shouldReturnBrowserAndSms() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.singletonList(BROWSER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(
                Collections.singletonList(SMS_PACKAGE_NAME));

        assertThat(mPreferenceController.getSummary()).isEqualTo("Browser1 and Sms1");
    }

    @Test
    public void getSummary_dialerAndSmsAvailable_shouldReturnDialerAndSms() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.singletonList(DIALER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(
                Collections.singletonList(SMS_PACKAGE_NAME));

        assertThat(mPreferenceController.getSummary()).isEqualTo("Phone1 and Sms1");
    }

    @Test
    public void getSummary_browserAvailable_shouldReturnBrowser() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.singletonList(BROWSER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(Collections.emptyList());

        assertThat(mPreferenceController.getSummary()).isEqualTo("Browser1");
    }

    @Test
    public void getSummary_dialerAvailable_shouldReturnDialer() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.singletonList(DIALER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(Collections.emptyList());

        assertThat(mPreferenceController.getSummary()).isEqualTo("Phone1");
    }

    @Test
    public void getSummary_smsAvailable_shouldReturnSms() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(
                Collections.singletonList(SMS_PACKAGE_NAME));

        assertThat(mPreferenceController.getSummary()).isEqualTo("Sms1");
    }

    @Test
    public void getSummary_noneAvailable_shouldReturnNull() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(Collections.emptyList());

        assertThat(mPreferenceController.getSummary()).isNull();
    }
}

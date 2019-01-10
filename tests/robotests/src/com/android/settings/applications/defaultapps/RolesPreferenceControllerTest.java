/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class RolesPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "roles";
    private static final String DIFFERENT_PREFERENCE_KEY = "different";

    private static final String PERMISSION_CONTROLLER_PACKAGE_NAME =
            "com.android.permissioncontroller";

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
    }

    @Test
    public void getAvailabilityStatus_noPermissionController_shouldReturnUnsupportedOnDevice() {
        when(mPackageManager.getPermissionControllerPackageName()).thenReturn(null);
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);

        assertThat(preferenceController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_hasPermissionController_shouldReturnAvailableUnsearchable() {
        when(mPackageManager.getPermissionControllerPackageName())
                .thenReturn(PERMISSION_CONTROLLER_PACKAGE_NAME);
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);

        assertThat(preferenceController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void handlePreferenceTreeClick_differentKey_shouldReturnFalse() {
        when(mPackageManager.getPermissionControllerPackageName())
                .thenReturn(PERMISSION_CONTROLLER_PACKAGE_NAME);
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(DIFFERENT_PREFERENCE_KEY);

        assertThat(preferenceController.handlePreferenceTreeClick(preference)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_sameKey_shouldReturnTrue() {
        when(mPackageManager.getPermissionControllerPackageName())
                .thenReturn(PERMISSION_CONTROLLER_PACKAGE_NAME);
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(PREFERENCE_KEY);

        assertThat(preferenceController.handlePreferenceTreeClick(preference)).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_noPermissionController_shouldNotStartActivity() {
        when(mPackageManager.getPermissionControllerPackageName()).thenReturn(null);
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(PREFERENCE_KEY);
        preferenceController.handlePreferenceTreeClick(preference);

        verify(mContext, never()).startActivity(any(Intent.class));
    }

    @Test
    public void handlePreferenceTreeClick_hasPermissionController_shouldStartActivityWithIntent() {
        when(mPackageManager.getPermissionControllerPackageName())
                .thenReturn(PERMISSION_CONTROLLER_PACKAGE_NAME);
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(PREFERENCE_KEY);
        preferenceController.handlePreferenceTreeClick(preference);
        ArgumentCaptor<Intent> intent = ArgumentCaptor.forClass(Intent.class);

        verify(mContext).startActivity(intent.capture());
        assertThat(intent.getValue().getAction())
                .isEqualTo(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
        assertThat(intent.getValue().getPackage()).isEqualTo(PERMISSION_CONTROLLER_PACKAGE_NAME);
    }

    @Test
    public void getSummary_allAvailable_shouldReturnAll() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.singletonList(BROWSER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.singletonList(DIALER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(
                Collections.singletonList(SMS_PACKAGE_NAME));
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);

        assertThat(preferenceController.getSummary()).isEqualTo("Browser1, Phone1, and Sms1");
    }

    @Test
    public void getSummary_browserAndDialerAvailable_shouldReturnBrowserAndDialer() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.singletonList(BROWSER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.singletonList(DIALER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(Collections.emptyList());
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);

        assertThat(preferenceController.getSummary()).isEqualTo("Browser1 and Phone1");
    }

    @Test
    public void getSummary_browserAndSmsAvailable_shouldReturnBrowserAndSms() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.singletonList(BROWSER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(
                Collections.singletonList(SMS_PACKAGE_NAME));
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);

        assertThat(preferenceController.getSummary()).isEqualTo("Browser1 and Sms1");
    }

    @Test
    public void getSummary_dialerAndSmsAvailable_shouldReturnDialerAndSms() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.singletonList(DIALER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(
                Collections.singletonList(SMS_PACKAGE_NAME));
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);

        assertThat(preferenceController.getSummary()).isEqualTo("Phone1 and Sms1");
    }

    @Test
    public void getSummary_browserAvailable_shouldReturnBrowser() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.singletonList(BROWSER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(Collections.emptyList());
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);

        assertThat(preferenceController.getSummary()).isEqualTo("Browser1");
    }

    @Test
    public void getSummary_dialerAvailable_shouldReturnDialer() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.singletonList(DIALER_PACKAGE_NAME));
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(Collections.emptyList());
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);

        assertThat(preferenceController.getSummary()).isEqualTo("Phone1");
    }

    @Test
    public void getSummary_smsAvailable_shouldReturnSms() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(
                Collections.singletonList(SMS_PACKAGE_NAME));
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);

        assertThat(preferenceController.getSummary()).isEqualTo("Sms1");
    }

    @Test
    public void getSummary_noneAvailable_shouldReturnNull() {
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_BROWSER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_DIALER)).thenReturn(
                Collections.emptyList());
        when(mRoleManager.getRoleHolders(RoleManager.ROLE_SMS)).thenReturn(Collections.emptyList());
        RolesPreferenceController preferenceController = new RolesPreferenceController(mContext,
                PREFERENCE_KEY);

        assertThat(preferenceController.getSummary()).isNull();
    }
}

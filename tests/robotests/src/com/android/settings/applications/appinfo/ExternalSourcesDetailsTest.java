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
 * limitations under the License
 */

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.applications.AppStateInstallAppsBridge;
import com.android.settings.applications.AppStateInstallAppsBridge.InstallAppsState;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedPreferenceHelper;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class ExternalSourcesDetailsTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private ActivityManager mActivityManager;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private RestrictedSwitchPreference mSwitchPref;
    @Mock
    private RestrictedPreferenceHelper mHelper;
    @Mock
    private PackageInfo mPackageInfo;

    private ExternalSourcesDetails mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = new ExternalSourcesDetails();
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mActivityManager", mActivityManager);
        ReflectionHelpers.setField(mFragment, "mAppOpsManager", mAppOpsManager);
        ReflectionHelpers.setField(mFragment, "mSwitchPref", mSwitchPref);
    }

    @Test
    public void setCanInstallApps_false_shouldKillNonCoreUid() {
        int mockUid = 23456;
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);

        mPackageInfo.applicationInfo = new ApplicationInfo();
        mPackageInfo.applicationInfo.uid = mockUid;
        assertThat(UserHandle.isCore(mockUid)).isFalse();
        mFragment.setCanInstallApps(false);
        verify(mActivityManager).killUid(eq(mockUid), anyString());
    }

    @Test
    public void setCanInstallApps_false_shouldNotKillCoreUid() {
        int mockUid = 1234;
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);

        mPackageInfo.applicationInfo = new ApplicationInfo();
        mPackageInfo.applicationInfo.uid = mockUid;
        assertThat(UserHandle.isCore(mockUid)).isTrue();
        mFragment.setCanInstallApps(false);
        verify(mActivityManager, never()).killUid(eq(mockUid), anyString());
    }

    @Test
    public void setCanInstallApps_true_shouldNotKillUid() {
        int mockUid = 23456;
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);

        mPackageInfo.applicationInfo = new ApplicationInfo();
        mPackageInfo.applicationInfo.uid = mockUid;
        mFragment.setCanInstallApps(true);
        verify(mActivityManager, never()).killUid(eq(mockUid), anyString());
    }

    @Test
    public void refreshUi_noPackageInfo_shouldReturnFalseAndNoCrash() {
        mFragment.refreshUi();

        assertThat(mFragment.refreshUi()).isFalse();
        // should not crash
    }

    @Test
    public void refreshUi_noApplicationInfo_shouldReturnFalseAndNoCrash() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);

        mFragment.refreshUi();

        assertThat(mFragment.refreshUi()).isFalse();
        // should not crash
    }

    @Test
    public void refreshUi_hasApplicationInfo_shouldReturnTrue() {
        ReflectionHelpers.setField(mFragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        final AppStateInstallAppsBridge appBridge = mock(AppStateInstallAppsBridge.class);
        ReflectionHelpers.setField(mFragment, "mAppBridge", appBridge);
        when(appBridge.createInstallAppsStateFor(nullable(String.class), anyInt()))
                .thenReturn(mock(InstallAppsState.class));

        mFragment.refreshUi();

        assertThat(mFragment.refreshUi()).isTrue();
        assertThat(mSwitchPref.isDisabledByAdmin()).isFalse();
    }

    @Test
    public void refreshUi_userRestrictionsUnknownSources_disablesSwitchPreference() {
        // Mocks set up
        final ExternalSourcesDetails fragment = new ExternalSourcesDetails();
        final ContextWrapper context = RuntimeEnvironment.application;
        final UserManager userManager = (UserManager) context.getSystemService(
                Context.USER_SERVICE);
        final ShadowUserManager shadowUserManager = Shadow.extract(userManager);

        ReflectionHelpers.setField(fragment, "mSwitchPref", mSwitchPref);
        ReflectionHelpers.setField(fragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        ReflectionHelpers.setField(fragment, "mUserManager", userManager);
        ReflectionHelpers.setField(mSwitchPref, "mHelper", mHelper);

        final AppStateInstallAppsBridge appBridge = mock(AppStateInstallAppsBridge.class);
        ReflectionHelpers.setField(fragment, "mAppBridge", appBridge);
        when(appBridge.createInstallAppsStateFor(nullable(String.class), anyInt()))
                .thenReturn(mock(InstallAppsState.class));

        // Test restriction set up
        shadowUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, true);
        doAnswer((answer) -> {
            when(mSwitchPref.isDisabledByAdmin()).thenReturn(true);
            return null;
        }).when(mSwitchPref).checkRestrictionAndSetDisabled(
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);

        // Code execution
        assertThat(fragment.refreshUi()).isTrue();

        // Assertions
        assertThat(userManager.hasUserRestriction(
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
                UserHandle.of(UserHandle.myUserId()))).isTrue();
        assertThat(mSwitchPref.isDisabledByAdmin()).isTrue();
    }

    @Test
    public void refreshUi_userRestrictionsUnknownSourcesGlobally_disablesSwitchPreference() {
        // Mocks set up
        final ExternalSourcesDetails fragment = new ExternalSourcesDetails();
        final ContextWrapper context = RuntimeEnvironment.application;
        final UserManager userManager = (UserManager) context.getSystemService(
                Context.USER_SERVICE);
        final ShadowUserManager shadowUserManager = Shadow.extract(userManager);

        ReflectionHelpers.setField(fragment, "mSwitchPref", mSwitchPref);
        ReflectionHelpers.setField(fragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        ReflectionHelpers.setField(fragment, "mUserManager", userManager);
        ReflectionHelpers.setField(mSwitchPref, "mHelper", mHelper);

        final AppStateInstallAppsBridge appBridge = mock(AppStateInstallAppsBridge.class);
        ReflectionHelpers.setField(fragment, "mAppBridge", appBridge);
        when(appBridge.createInstallAppsStateFor(nullable(String.class), anyInt()))
                .thenReturn(mock(InstallAppsState.class));

        // Test restriction set up
        shadowUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY, true);
        doAnswer((answer) -> {
            when(mSwitchPref.isDisabledByAdmin()).thenReturn(true);
            return null;
        }).when(mSwitchPref).checkRestrictionAndSetDisabled(
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY);

        // Code execution
        assertThat(fragment.refreshUi()).isTrue();

        // Assertions
        assertThat(userManager.hasUserRestriction(
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,
                UserHandle.of(UserHandle.myUserId()))).isTrue();
        assertThat(mSwitchPref.isDisabledByAdmin()).isTrue();
    }

    @Test
    public void refreshUi_bothUnknownSourcesUserRestrictions_disableSwitchPreference() {
        // Mocks set up
        final ExternalSourcesDetails fragment = new ExternalSourcesDetails();
        final ContextWrapper context = RuntimeEnvironment.application;
        final UserManager userManager = (UserManager) context.getSystemService(
                Context.USER_SERVICE);
        final ShadowUserManager shadowUserManager = Shadow.extract(userManager);

        ReflectionHelpers.setField(fragment, "mSwitchPref", mSwitchPref);
        ReflectionHelpers.setField(fragment, "mPackageInfo", mPackageInfo);
        mPackageInfo.applicationInfo = new ApplicationInfo();
        ReflectionHelpers.setField(fragment, "mUserManager", userManager);
        ReflectionHelpers.setField(mSwitchPref, "mHelper", mHelper);

        final AppStateInstallAppsBridge appBridge = mock(AppStateInstallAppsBridge.class);
        ReflectionHelpers.setField(fragment, "mAppBridge", appBridge);
        when(appBridge.createInstallAppsStateFor(nullable(String.class), anyInt()))
                .thenReturn(mock(InstallAppsState.class));

        // Test restriction set up
        shadowUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY, true);
        shadowUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, true);
        doAnswer((answer) -> {
            when(mSwitchPref.isDisabledByAdmin()).thenReturn(true);
            return null;
        }).when(mSwitchPref).checkRestrictionAndSetDisabled(
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY);

        // Code execution
        assertThat(fragment.refreshUi()).isTrue();

        // Assertions
        assertThat(userManager.hasUserRestriction(
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,
                UserHandle.of(UserHandle.myUserId()))).isTrue();
        assertThat(userManager.hasUserRestriction(
                UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
                UserHandle.of(UserHandle.myUserId()))).isTrue();
        assertThat(mSwitchPref.isDisabledByAdmin()).isTrue();
    }
}

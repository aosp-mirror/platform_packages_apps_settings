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

import static android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES;
import static android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY;
import static android.os.UserManager.RESTRICTION_SOURCE_DEVICE_OWNER;
import static android.os.UserManager.RESTRICTION_SOURCE_PROFILE_OWNER;
import static android.os.UserManager.RESTRICTION_SOURCE_SYSTEM;
import static android.security.advancedprotection.AdvancedProtectionManager.ADVANCED_PROTECTION_SYSTEM_ENTITY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.UnknownAuthority;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.security.Flags;

import com.android.settings.applications.AppStateInstallAppsBridge;
import com.android.settings.applications.AppStateInstallAppsBridge.InstallAppsState;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedPreferenceHelper;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Rule;
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
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private RestrictedSwitchPreference mSwitchPref;
    @Mock
    private RestrictedPreferenceHelper mHelper;
    @Mock
    private PackageInfo mPackageInfo;
    @Mock
    private AppEntry mAppEntry;

    private final int mAppUid = 10123;
    private final String mPackageName = "test.pkg";
    private final UserHandle mUserHandle = UserHandle.getUserHandleForUid(mAppUid);

    private ExternalSourcesDetails mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(mDevicePolicyManager).when(mContext).getSystemService(DevicePolicyManager.class);
        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);

        mFragment = new ExternalSourcesDetails();
        ReflectionHelpers.setField(mFragment, "mUserManager", mUserManager);
        ReflectionHelpers.setField(mFragment, "mSwitchPref", mSwitchPref);

        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.uid = mAppUid;
        applicationInfo.packageName = mPackageName;
        mAppEntry.info = applicationInfo;
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

    @RequiresFlagsDisabled(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSTALL_UNKNOWN_SOURCES)
    @Test
    public void getPreferenceSummary_restrictedBySystem_adminString() {
        when(mUserManager.getUserRestrictionSource(DISALLOW_INSTALL_UNKNOWN_SOURCES, mUserHandle))
                .thenReturn(RESTRICTION_SOURCE_SYSTEM);
        when(mContext
                .getString(com.android.settingslib.widget.restricted.R.string.disabled_by_admin))
                .thenReturn("disabled_by_admin");

        CharSequence summary = ExternalSourcesDetails.getPreferenceSummary(mContext, mAppEntry);

        assertEquals("disabled_by_admin", summary.toString());
    }

    @RequiresFlagsDisabled(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSTALL_UNKNOWN_SOURCES)
    @Test
    public void getPreferenceSummary_restrictedByProfileOwner_disabledString() {
        when(mUserManager.getUserRestrictionSource(DISALLOW_INSTALL_UNKNOWN_SOURCES, mUserHandle))
                .thenReturn(RESTRICTION_SOURCE_PROFILE_OWNER);
        when(mContext.getString(com.android.settingslib.R.string.disabled)).thenReturn("disabled");

        CharSequence summary = ExternalSourcesDetails.getPreferenceSummary(mContext, mAppEntry);

        assertEquals("disabled", summary.toString());
    }

    @RequiresFlagsDisabled(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSTALL_UNKNOWN_SOURCES)
    @Test
    public void getPreferenceSummary_restrictedByDeviceOwner_disabledString() {
        when(mUserManager.getUserRestrictionSource(DISALLOW_INSTALL_UNKNOWN_SOURCES, mUserHandle))
                .thenReturn(RESTRICTION_SOURCE_DEVICE_OWNER);
        when(mContext.getString(com.android.settingslib.R.string.disabled)).thenReturn("disabled");

        CharSequence summary = ExternalSourcesDetails.getPreferenceSummary(mContext, mAppEntry);

        assertEquals("disabled", summary.toString());
    }

    @RequiresFlagsEnabled(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSTALL_UNKNOWN_SOURCES)
    @Test
    public void getPreferenceSummary_baseRestricted_disabledString() {
        when(mUserManager.hasBaseUserRestriction(DISALLOW_INSTALL_UNKNOWN_SOURCES, mUserHandle))
                .thenReturn(true);
        when(mContext.getString(com.android.settingslib.R.string.disabled)).thenReturn("disabled");

        CharSequence summary = ExternalSourcesDetails.getPreferenceSummary(mContext, mAppEntry);

        assertEquals("disabled", summary.toString());
    }

    @RequiresFlagsEnabled(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSTALL_UNKNOWN_SOURCES)
    @Test
    public void getPreferenceSummary_restrictedOnUser_adminString() {
        when(mUserManager.hasUserRestrictionForUser(DISALLOW_INSTALL_UNKNOWN_SOURCES, mUserHandle))
                .thenReturn(true);
        when(mContext
                .getString(com.android.settingslib.widget.restricted.R.string.disabled_by_admin))
                .thenReturn("disabled_by_admin");

        CharSequence summary = ExternalSourcesDetails.getPreferenceSummary(mContext, mAppEntry);

        assertEquals("disabled_by_admin", summary.toString());
    }

    @RequiresFlagsEnabled(android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSTALL_UNKNOWN_SOURCES)
    @Test
    public void getPreferenceSummary_restrictedGlobally_adminString() {
        final EnforcingAdmin nonAdvancedProtectionEnforcingAdmin = new EnforcingAdmin("test.pkg",
                UnknownAuthority.UNKNOWN_AUTHORITY, mUserHandle, new ComponentName("", ""));

        when(mDevicePolicyManager.getEnforcingAdmin(mUserHandle.getIdentifier(),
                DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)).thenReturn(
                        nonAdvancedProtectionEnforcingAdmin);
        when(mUserManager.hasUserRestrictionForUser(DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,
                mUserHandle)).thenReturn(true);
        when(mContext
                .getString(com.android.settingslib.widget.restricted.R.string.disabled_by_admin))
                .thenReturn("disabled_by_admin");

        CharSequence summary = ExternalSourcesDetails.getPreferenceSummary(mContext, mAppEntry);

        assertEquals("disabled_by_admin", summary.toString());
    }

    @RequiresFlagsEnabled(Flags.FLAG_AAPM_FEATURE_DISABLE_INSTALL_UNKNOWN_SOURCES)
    @Test
    public void getPreferenceSummary_restrictedGlobally_advancedProtectionString() {
        final EnforcingAdmin advancedProtectionEnforcingAdmin = new EnforcingAdmin("test.pkg",
                new UnknownAuthority(ADVANCED_PROTECTION_SYSTEM_ENTITY), mUserHandle,
                new ComponentName("", ""));

        when(mDevicePolicyManager.getEnforcingAdmin(mUserHandle.getIdentifier(),
                DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)).thenReturn(
                        advancedProtectionEnforcingAdmin);
        when(mUserManager.hasUserRestrictionForUser(DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY,
                mUserHandle)).thenReturn(true);
        when(mContext.getString(
                com.android.settingslib.widget.restricted.R.string.disabled_by_advanced_protection))
                .thenReturn("disabled_by_advanced_protection");

        CharSequence summary = ExternalSourcesDetails.getPreferenceSummary(mContext, mAppEntry);

        assertEquals("disabled_by_advanced_protection", summary.toString());
    }
}

/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.location.LocationManager;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.testutils.ApplicationTestUtils;
import com.android.settingslib.testutils.shadow.ShadowDefaultDialerManager;
import com.android.settingslib.testutils.shadow.ShadowSmsApplication;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link ApplicationFeatureProviderImpl}.
 */
@RunWith(RobolectricTestRunner.class)
public final class ApplicationFeatureProviderImplTest {

    private final int MAIN_USER_ID = 0;
    private final int MANAGED_PROFILE_ID = 10;

    private final int PER_USER_UID_RANGE = 100000;
    private final int APP_1_UID = MAIN_USER_ID * PER_USER_UID_RANGE + 1;
    private final int APP_2_UID = MANAGED_PROFILE_ID * PER_USER_UID_RANGE + 1;

    private final String APP_1 = "app1";
    private final String APP_2 = "app2";

    private final String PERMISSION = "some.permission";

    @Mock
    private UserManager mUserManager;
    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private IPackageManager mPackageManagerService;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private LocationManager mLocationManager;

    private ApplicationFeatureProvider mProvider;

    private int mAppCount = -1;
    private List<UserAppInfo> mAppList = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mLocationManager);

        mProvider = new ApplicationFeatureProviderImpl(mContext, mPackageManager,
                mPackageManagerService, mDevicePolicyManager);
    }

    private void verifyCalculateNumberOfPolicyInstalledApps(boolean async) {
        setUpUsersAndInstalledApps();

        when(mPackageManager.getInstallReason(APP_1, new UserHandle(MAIN_USER_ID)))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);
        when(mPackageManager.getInstallReason(APP_2, new UserHandle(MANAGED_PROFILE_ID)))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);

        mAppCount = -1;
        mProvider.calculateNumberOfPolicyInstalledApps(async, (num) -> mAppCount = num);
        if (async) {
            ShadowApplication.runBackgroundTasks();
        }
        assertThat(mAppCount).isEqualTo(1);
    }

    @Test
    public void testListPolicyInstalledApps() {
        setUpUsersAndInstalledApps();

        when(mPackageManager.getInstallReason(APP_1, new UserHandle(MAIN_USER_ID)))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);
        when(mPackageManager.getInstallReason(APP_2, new UserHandle(MANAGED_PROFILE_ID)))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);

        mAppList = null;
        mProvider.listPolicyInstalledApps((list) -> mAppList = list);
        assertThat(mAppList).isNotNull();
        assertThat(mAppList.size()).isEqualTo(1);
        assertThat(mAppList.get(0).appInfo.packageName).isEqualTo(APP_2);
    }

    @Test
    public void testCalculateNumberOfInstalledAppsSync() {
        verifyCalculateNumberOfPolicyInstalledApps(false /* async */);
    }

    @Test
    public void testCalculateNumberOfInstalledAppsAsync() {
        verifyCalculateNumberOfPolicyInstalledApps(true /* async */);
    }

    private void verifyCalculateNumberOfAppsWithAdminGrantedPermissions(boolean async)
            throws Exception {
        setUpUsersAndInstalledApps();

        when(mDevicePolicyManager.getPermissionGrantState(null, APP_1, PERMISSION))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        when(mDevicePolicyManager.getPermissionGrantState(null, APP_2, PERMISSION))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mPackageManagerService.checkUidPermission(PERMISSION, APP_1_UID))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(PERMISSION, APP_2_UID))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mPackageManager.getInstallReason(APP_1, new UserHandle(MAIN_USER_ID)))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);
        when(mPackageManager.getInstallReason(APP_2, new UserHandle(MANAGED_PROFILE_ID)))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);

        mAppCount = -1;
        mProvider.calculateNumberOfAppsWithAdminGrantedPermissions(new String[]{PERMISSION}, async,
                (num) -> mAppCount = num);
        if (async) {
            ShadowApplication.runBackgroundTasks();
        }
        assertThat(mAppCount).isEqualTo(2);
    }

    @Test
    public void testCalculateNumberOfAppsWithAdminGrantedPermissionsSync() throws Exception {
        verifyCalculateNumberOfAppsWithAdminGrantedPermissions(false /* async */);
    }

    @Test
    public void testCalculateNumberOfAppsWithAdminGrantedPermissionsAsync() throws Exception {
        verifyCalculateNumberOfAppsWithAdminGrantedPermissions(true /* async */);
    }

    @Test
    public void testListAppsWithAdminGrantedPermissions()
            throws Exception {
        setUpUsersAndInstalledApps();

        when(mDevicePolicyManager.getPermissionGrantState(null, APP_1, PERMISSION))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        when(mDevicePolicyManager.getPermissionGrantState(null, APP_2, PERMISSION))
                .thenReturn(DevicePolicyManager.PERMISSION_GRANT_STATE_DENIED);
        when(mPackageManagerService.checkUidPermission(PERMISSION, APP_1_UID))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        when(mPackageManagerService.checkUidPermission(PERMISSION, APP_2_UID))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mPackageManager.getInstallReason(APP_1, new UserHandle(MAIN_USER_ID)))
                .thenReturn(PackageManager.INSTALL_REASON_UNKNOWN);
        when(mPackageManager.getInstallReason(APP_2, new UserHandle(MANAGED_PROFILE_ID)))
                .thenReturn(PackageManager.INSTALL_REASON_POLICY);

        mAppList = null;
        mProvider.listAppsWithAdminGrantedPermissions(new String[]{PERMISSION},
                (list) -> mAppList = list);
        assertThat(mAppList).isNotNull();
        assertThat(mAppList.size()).isEqualTo(2);
        assertThat(Arrays.asList(mAppList.get(0).appInfo.packageName,
                mAppList.get(1).appInfo.packageName).containsAll(Arrays.asList(APP_1, APP_2)))
                .isTrue();
    }

    @Test
    public void testFindPersistentPreferredActivities() throws Exception {
        final UserInfo mainUser = new UserInfo(MAIN_USER_ID, "main", UserInfo.FLAG_ADMIN);
        final UserInfo managedUser = new UserInfo(MANAGED_PROFILE_ID, "managed",
                UserInfo.FLAG_MANAGED_PROFILE);

        when(mUserManager.getUserProfiles()).thenReturn(Arrays.asList(new UserHandle(MAIN_USER_ID),
                new UserHandle(MANAGED_PROFILE_ID)));
        when(mUserManager.getUserInfo(MAIN_USER_ID)).thenReturn(mainUser);
        when(mUserManager.getUserInfo(MANAGED_PROFILE_ID)).thenReturn(managedUser);

        final Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        final Intent editIntent = new Intent(Intent.ACTION_EDIT);
        final Intent sendIntent = new Intent(Intent.ACTION_SEND);

        final ResolveInfo app1 = createResolveInfo(APP_1);
        final ResolveInfo app2 = createResolveInfo(APP_2);
        when(mPackageManagerService.findPersistentPreferredActivity(viewIntent, MAIN_USER_ID))
                .thenReturn(app1);
        when(mPackageManagerService.findPersistentPreferredActivity(viewIntent, MANAGED_PROFILE_ID))
                .thenReturn(app1);
        when(mPackageManagerService.findPersistentPreferredActivity(editIntent, MAIN_USER_ID))
                .thenReturn(null);
        when(mPackageManagerService.findPersistentPreferredActivity(editIntent, MANAGED_PROFILE_ID))
                .thenReturn(app2);
        when(mPackageManagerService.findPersistentPreferredActivity(sendIntent, MAIN_USER_ID))
                .thenReturn(app1);
        when(mPackageManagerService.findPersistentPreferredActivity(sendIntent, MANAGED_PROFILE_ID))
                .thenReturn(null);

        final List<UserAppInfo> expectedMainUserActivities = new ArrayList<>();
        expectedMainUserActivities.add(new UserAppInfo(mainUser,
                new ApplicationInfo(app1.activityInfo.applicationInfo)));
        final List<UserAppInfo> expectedManagedUserActivities = new ArrayList<>();
        expectedManagedUserActivities.add(new UserAppInfo(managedUser,
                new ApplicationInfo(app1.activityInfo.applicationInfo)));
        expectedManagedUserActivities.add(new UserAppInfo(managedUser,
                new ApplicationInfo(app2.activityInfo.applicationInfo)));

        assertThat(mProvider.findPersistentPreferredActivities(MAIN_USER_ID,
                new Intent[]{viewIntent, editIntent, sendIntent}))
                .isEqualTo(expectedMainUserActivities);
        assertThat(mProvider.findPersistentPreferredActivities(MANAGED_PROFILE_ID,
                new Intent[]{viewIntent, editIntent, sendIntent}))
                .isEqualTo(expectedManagedUserActivities);
    }

    @Test
    @Config(shadows = {ShadowSmsApplication.class, ShadowDefaultDialerManager.class})
    public void getKeepEnabledPackages_shouldContainDefaultPhoneAndSmsAndLocationHistory() {
        final String testDialer = "com.android.test.defaultdialer";
        final String testSms = "com.android.test.defaultsms";
        final String testLocationHistory = "com.android.test.location.history";

        ShadowSmsApplication.setDefaultSmsApplication(new ComponentName(testSms, "receiver"));
        ShadowDefaultDialerManager.setDefaultDialerApplication(testDialer);

        // Spy the real context to mock LocationManager.
        Context spyContext = spy(RuntimeEnvironment.application);
        when(mLocationManager.getExtraLocationControllerPackage()).thenReturn(testLocationHistory);
        when(spyContext.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mLocationManager);

        ReflectionHelpers.setField(mProvider, "mContext", spyContext);

        final Set<String> keepEnabledPackages = mProvider.getKeepEnabledPackages();

        final List<String> expectedPackages = Arrays.asList(testDialer, testSms,
                testLocationHistory);
        assertThat(keepEnabledPackages).containsAllIn(expectedPackages);
    }

    @Test
    @Config(shadows = {ShadowSmsApplication.class, ShadowDefaultDialerManager.class})
    public void getKeepEnabledPackages_hasEuiccComponent_shouldContainEuiccPackage() {
        final String testDialer = "com.android.test.defaultdialer";
        final String testSms = "com.android.test.defaultsms";
        final String testLocationHistory = "com.android.test.location.history";
        final String testEuicc = "com.android.test.euicc";

        ShadowSmsApplication.setDefaultSmsApplication(new ComponentName(testSms, "receiver"));
        ShadowDefaultDialerManager.setDefaultDialerApplication(testDialer);
        final ComponentInfo componentInfo = new ComponentInfo();
        componentInfo.packageName = testEuicc;

        ApplicationFeatureProviderImpl spyProvider = spy(new ApplicationFeatureProviderImpl(
                mContext, mPackageManager, mPackageManagerService, mDevicePolicyManager));
        doReturn(componentInfo).when(spyProvider).findEuiccService(mPackageManager);

        // Spy the real context to mock LocationManager.
        Context spyContext = spy(RuntimeEnvironment.application);
        when(mLocationManager.getExtraLocationControllerPackage()).thenReturn(testLocationHistory);
        when(spyContext.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mLocationManager);

        ReflectionHelpers.setField(mProvider, "mContext", spyContext);

        final Set<String> keepEnabledPackages = spyProvider.getKeepEnabledPackages();

        assertThat(keepEnabledPackages).contains(testEuicc);
    }

    @Test
    @Config(shadows = {ShadowSmsApplication.class, ShadowDefaultDialerManager.class})
    public void getKeepEnabledPackages_shouldContainSettingsIntelligence() {
        final String testDialer = "com.android.test.defaultdialer";
        final String testSms = "com.android.test.defaultsms";
        final String testLocationHistory = "com.android.test.location.history";

        ShadowSmsApplication.setDefaultSmsApplication(new ComponentName(testSms, "receiver"));
        ShadowDefaultDialerManager.setDefaultDialerApplication(testDialer);

        // Spy the real context to mock LocationManager.
        Context spyContext = spy(RuntimeEnvironment.application);
        when(mLocationManager.getExtraLocationControllerPackage()).thenReturn(testLocationHistory);
        when(spyContext.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mLocationManager);

        ReflectionHelpers.setField(mProvider, "mContext", spyContext);

        final Set<String> whitelist = mProvider.getKeepEnabledPackages();

        assertThat(whitelist).contains("com.android.settings.intelligence");
    }

    @Test
    @Config(shadows = {ShadowSmsApplication.class, ShadowDefaultDialerManager.class})
    public void getKeepEnabledPackages_shouldContainPackageInstaller() {
        final String testDialer = "com.android.test.defaultdialer";
        final String testSms = "com.android.test.defaultsms";
        final String testLocationHistory = "com.android.test.location.history";

        ShadowSmsApplication.setDefaultSmsApplication(new ComponentName(testSms, "receiver"));
        ShadowDefaultDialerManager.setDefaultDialerApplication(testDialer);

        // Spy the real context to mock LocationManager.
        Context spyContext = spy(RuntimeEnvironment.application);
        when(mLocationManager.getExtraLocationControllerPackage()).thenReturn(testLocationHistory);
        when(spyContext.getSystemService(Context.LOCATION_SERVICE)).thenReturn(mLocationManager);

        ReflectionHelpers.setField(mProvider, "mContext", spyContext);

        final Set<String> whitelist = mProvider.getKeepEnabledPackages();

        assertThat(whitelist).contains("com.android.packageinstaller");
    }

    private void setUpUsersAndInstalledApps() {
        when(mUserManager.getProfiles(UserHandle.myUserId())).thenReturn(Arrays.asList(
                new UserInfo(MAIN_USER_ID, "main", UserInfo.FLAG_ADMIN),
                new UserInfo(MANAGED_PROFILE_ID, "managed profile", 0)));

        when(mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                        | PackageManager.MATCH_ANY_USER,
                MAIN_USER_ID)).thenReturn(Arrays.asList(
                ApplicationTestUtils.buildInfo(APP_1_UID, APP_1, 0 /* flags */,
                        Build.VERSION_CODES.M)));
        when(mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                        | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS,
                MANAGED_PROFILE_ID)).thenReturn(Arrays.asList(
                ApplicationTestUtils.buildInfo(APP_2_UID, APP_2, 0 /* flags */,
                        Build.VERSION_CODES.LOLLIPOP)));
    }

    private ResolveInfo createResolveInfo(String packageName) {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = packageName;
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = packageName;
        activityInfo.applicationInfo = applicationInfo;
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        return resolveInfo;
    }
}

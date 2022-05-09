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

package com.android.settings.enterprise;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.VpnManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.SpannableStringBuilder;

import com.android.settings.R;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@RunWith(RobolectricTestRunner.class)
public class EnterprisePrivacyFeatureProviderImplTest {

    private static final String OWNER_ORGANIZATION = "ACME";
    private static final String VPN_PACKAGE_ID = "com.example.vpn";
    private static final String IME_PACKAGE_ID = "com.example.ime";
    private static final String IME_PACKAGE_LABEL = "Test IME";

    private final ComponentName mOwner = new ComponentName("mock", "component");
    private final ComponentName mAdmin1 = new ComponentName("mock", "admin1");
    private final ComponentName mAdmin2 = new ComponentName("mock", "admin2");
    private final Date mDate = new Date(2011, 11, 11);
    private final int mUserId = UserHandle.myUserId();
    private final int mManagedProfileUserId = mUserId + 1;

    private List<UserInfo> mProfiles = new ArrayList<>();

    @Mock
    private Context mContext;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private ConnectivityManager mConnectivityManger;
    @Mock
    private VpnManager mVpnManager;
    private Resources mResources;

    private EnterprisePrivacyFeatureProvider mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        resetAndInitializePackageManager();
        when(mUserManager.getProfiles(mUserId)).thenReturn(mProfiles);
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mProfiles.add(new UserInfo(mUserId, "", "", 0 /* flags */));
        mResources = RuntimeEnvironment.application.getResources();

        mProvider = new EnterprisePrivacyFeatureProviderImpl(mContext, mDevicePolicyManager,
                mPackageManager, mUserManager, mConnectivityManger, mVpnManager, mResources);
    }

    @Test
    public void testHasDeviceOwner() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        assertThat(mProvider.hasDeviceOwner()).isFalse();

        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(mOwner);
        assertThat(mProvider.hasDeviceOwner()).isTrue();
    }

    @Test
    public void testIsInCompMode() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(mOwner);
        assertThat(mProvider.isInCompMode()).isFalse();

        mProfiles.add(new UserInfo(mManagedProfileUserId, "", "", UserInfo.FLAG_MANAGED_PROFILE));
        assertThat(mProvider.isInCompMode()).isTrue();
    }

    @Test
    public void testGetDeviceOwnerOrganizationName() {
        when(mDevicePolicyManager.getDeviceOwnerOrganizationName()).thenReturn(null);
        assertThat(mProvider.getDeviceOwnerOrganizationName()).isNull();

        when(mDevicePolicyManager.getDeviceOwnerOrganizationName()).thenReturn(OWNER_ORGANIZATION);
        assertThat(mProvider.getDeviceOwnerOrganizationName()).isEqualTo(OWNER_ORGANIZATION);
    }

    @Test
    @Ignore
    public void testGetDeviceOwnerDisclosure() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        assertThat(mProvider.getDeviceOwnerDisclosure()).isNull();

        SpannableStringBuilder disclosure = new SpannableStringBuilder();
        disclosure.append(mResources.getString(R.string.do_disclosure_generic));
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(mOwner);
        when(mDevicePolicyManager.getDeviceOwnerOrganizationName()).thenReturn(null);
        assertThat(mProvider.getDeviceOwnerDisclosure()).isEqualTo(disclosure);

        disclosure = new SpannableStringBuilder();
        disclosure.append(mResources.getString(R.string.do_disclosure_with_name,
                OWNER_ORGANIZATION));
        when(mDevicePolicyManager.getDeviceOwnerOrganizationName()).thenReturn(OWNER_ORGANIZATION);
        assertThat(mProvider.getDeviceOwnerDisclosure()).isEqualTo(disclosure);
    }

    @Test
    public void testGetLastSecurityLogRetrievalTime() {
        when(mDevicePolicyManager.getLastSecurityLogRetrievalTime()).thenReturn(-1L);
        assertThat(mProvider.getLastSecurityLogRetrievalTime()).isNull();

        when(mDevicePolicyManager.getLastSecurityLogRetrievalTime())
                .thenReturn(mDate.getTime());
        assertThat(mProvider.getLastSecurityLogRetrievalTime()).isEqualTo(mDate);
    }

    @Test
    public void testGetLastBugReportRequestTime() {
        when(mDevicePolicyManager.getLastBugReportRequestTime()).thenReturn(-1L);
        assertThat(mProvider.getLastBugReportRequestTime()).isNull();

        when(mDevicePolicyManager.getLastBugReportRequestTime()).thenReturn(mDate.getTime());
        assertThat(mProvider.getLastBugReportRequestTime()).isEqualTo(mDate);
    }

    @Test
    public void testGetLastNetworkLogRetrievalTime() {
        when(mDevicePolicyManager.getLastNetworkLogRetrievalTime()).thenReturn(-1L);
        assertThat(mProvider.getLastNetworkLogRetrievalTime()).isNull();

        when(mDevicePolicyManager.getLastNetworkLogRetrievalTime()).thenReturn(mDate.getTime());
        assertThat(mProvider.getLastNetworkLogRetrievalTime()).isEqualTo(mDate);
    }

    @Test
    public void testIsSecurityLoggingEnabled() {
        when(mDevicePolicyManager.isSecurityLoggingEnabled(null)).thenReturn(false);
        assertThat(mProvider.isSecurityLoggingEnabled()).isFalse();

        when(mDevicePolicyManager.isSecurityLoggingEnabled(null)).thenReturn(true);
        assertThat(mProvider.isSecurityLoggingEnabled()).isTrue();
    }

    @Test
    public void testIsNetworkLoggingEnabled() {
        when(mDevicePolicyManager.isNetworkLoggingEnabled(null)).thenReturn(false);
        assertThat(mProvider.isNetworkLoggingEnabled()).isFalse();

        when(mDevicePolicyManager.isNetworkLoggingEnabled(null)).thenReturn(true);
        assertThat(mProvider.isNetworkLoggingEnabled()).isTrue();
    }

    @Test
    public void testIsAlwaysOnVpnSetInCurrentUser() {
        when(mVpnManager.getAlwaysOnVpnPackageForUser(mUserId)).thenReturn(null);
        assertThat(mProvider.isAlwaysOnVpnSetInCurrentUser()).isFalse();

        when(mVpnManager.getAlwaysOnVpnPackageForUser(mUserId)).thenReturn(VPN_PACKAGE_ID);
        assertThat(mProvider.isAlwaysOnVpnSetInCurrentUser()).isTrue();
    }

    @Test
    public void testIsAlwaysOnVpnSetInManagedProfileProfile() {
        assertThat(mProvider.isAlwaysOnVpnSetInManagedProfile()).isFalse();

        mProfiles.add(new UserInfo(mManagedProfileUserId, "", "", UserInfo.FLAG_MANAGED_PROFILE));

        when(mVpnManager.getAlwaysOnVpnPackageForUser(mManagedProfileUserId)).thenReturn(null);
        assertThat(mProvider.isAlwaysOnVpnSetInManagedProfile()).isFalse();

        when(mVpnManager.getAlwaysOnVpnPackageForUser(mManagedProfileUserId))
                .thenReturn(VPN_PACKAGE_ID);
        assertThat(mProvider.isAlwaysOnVpnSetInManagedProfile()).isTrue();
    }

    @Test
    public void testGetMaximumFailedPasswordsForWipeInCurrentUser() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnCallingUser()).thenReturn(null);
        when(mDevicePolicyManager.getProfileOwnerAsUser(mUserId)).thenReturn(null);
        when(mDevicePolicyManager.getMaximumFailedPasswordsForWipe(mOwner, mUserId))
                .thenReturn(10);
        assertThat(mProvider.getMaximumFailedPasswordsBeforeWipeInCurrentUser()).isEqualTo(0);

        when(mDevicePolicyManager.getProfileOwnerAsUser(mUserId)).thenReturn(mOwner);
        assertThat(mProvider.getMaximumFailedPasswordsBeforeWipeInCurrentUser()).isEqualTo(10);

        when(mDevicePolicyManager.getDeviceOwnerComponentOnCallingUser()).thenReturn(mOwner);
        when(mDevicePolicyManager.getProfileOwnerAsUser(mUserId)).thenReturn(null);
        assertThat(mProvider.getMaximumFailedPasswordsBeforeWipeInCurrentUser()).isEqualTo(10);
    }

    @Test
    public void testGetMaximumFailedPasswordsForWipeInManagedProfile() {
        when(mDevicePolicyManager.getProfileOwnerAsUser(mManagedProfileUserId)).thenReturn(mOwner);
        when(mDevicePolicyManager.getMaximumFailedPasswordsForWipe(mOwner, mManagedProfileUserId))
                .thenReturn(10);
        assertThat(mProvider.getMaximumFailedPasswordsBeforeWipeInManagedProfile()).isEqualTo(0);

        mProfiles.add(new UserInfo(mManagedProfileUserId, "", "", UserInfo.FLAG_MANAGED_PROFILE));
        assertThat(mProvider.getMaximumFailedPasswordsBeforeWipeInManagedProfile()).isEqualTo(10);
    }

    @Test
    public void testGetImeLabelIfOwnerSet() throws Exception {
        final ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        when(applicationInfo.loadLabel(mPackageManager)).thenReturn(IME_PACKAGE_LABEL);

        Settings.Secure.putString(null, Settings.Secure.DEFAULT_INPUT_METHOD, IME_PACKAGE_ID);
        when(mPackageManager.getApplicationInfoAsUser(IME_PACKAGE_ID, 0, mUserId))
                .thenReturn(applicationInfo);

        // IME not set by Device Owner.
        when(mDevicePolicyManager.isCurrentInputMethodSetByOwner()).thenReturn(false);
        assertThat(mProvider.getImeLabelIfOwnerSet()).isNull();

        // Device Owner set IME to empty string.
        when(mDevicePolicyManager.isCurrentInputMethodSetByOwner()).thenReturn(true);
        Settings.Secure.putString(null, Settings.Secure.DEFAULT_INPUT_METHOD, null);
        assertThat(mProvider.getImeLabelIfOwnerSet()).isNull();

        // Device Owner set IME to nonexistent package.
        Settings.Secure.putString(null, Settings.Secure.DEFAULT_INPUT_METHOD, IME_PACKAGE_ID);
        when(mPackageManager.getApplicationInfoAsUser(IME_PACKAGE_ID, 0, mUserId))
                .thenThrow(new PackageManager.NameNotFoundException());
        assertThat(mProvider.getImeLabelIfOwnerSet()).isNull();

        // Device Owner set IME to existent package.
        resetAndInitializePackageManager();
        when(mPackageManager.getApplicationInfoAsUser(IME_PACKAGE_ID, 0, mUserId))
                .thenReturn(applicationInfo);
        assertThat(mProvider.getImeLabelIfOwnerSet()).isEqualTo(IME_PACKAGE_LABEL);
    }

    @Test
    public void testGetNumberOfOwnerInstalledCaCertsForCurrent() {
        final UserHandle userHandle = new UserHandle(UserHandle.USER_SYSTEM);
        final UserHandle managedProfileUserHandle = new UserHandle(mManagedProfileUserId);

        when(mDevicePolicyManager.getOwnerInstalledCaCerts(managedProfileUserHandle))
                .thenReturn(Arrays.asList("ca1", "ca2"));

        when(mDevicePolicyManager.getOwnerInstalledCaCerts(userHandle))
                .thenReturn(null);
        assertThat(mProvider.getNumberOfOwnerInstalledCaCertsForCurrentUser())
                .isEqualTo(0);
        when(mDevicePolicyManager.getOwnerInstalledCaCerts(userHandle))
                .thenReturn(new ArrayList<>());
        assertThat(mProvider.getNumberOfOwnerInstalledCaCertsForCurrentUser())
                .isEqualTo(0);
        when(mDevicePolicyManager.getOwnerInstalledCaCerts(userHandle))
                .thenReturn(Arrays.asList("ca1", "ca2"));
        assertThat(mProvider.getNumberOfOwnerInstalledCaCertsForCurrentUser())
                .isEqualTo(2);
    }

    @Test
    public void testGetNumberOfOwnerInstalledCaCertsForManagedProfile() {
        final UserHandle userHandle = new UserHandle(UserHandle.USER_SYSTEM);
        final UserHandle managedProfileUserHandle = new UserHandle(mManagedProfileUserId);
        final UserInfo managedProfile =
                new UserInfo(mManagedProfileUserId, "", "", UserInfo.FLAG_MANAGED_PROFILE);

        // Without a profile
        when(mDevicePolicyManager.getOwnerInstalledCaCerts(managedProfileUserHandle))
                .thenReturn(Arrays.asList("ca1", "ca2"));
        assertThat(mProvider.getNumberOfOwnerInstalledCaCertsForManagedProfile())
                .isEqualTo(0);

        // With a profile
        mProfiles.add(managedProfile);
        when(mDevicePolicyManager.getOwnerInstalledCaCerts(managedProfileUserHandle))
                .thenReturn(null);
        assertThat(mProvider.getNumberOfOwnerInstalledCaCertsForManagedProfile())
                .isEqualTo(0);
        when(mDevicePolicyManager.getOwnerInstalledCaCerts(userHandle))
                .thenReturn(new ArrayList<>());
        assertThat(mProvider.getNumberOfOwnerInstalledCaCertsForManagedProfile())
                .isEqualTo(0);
        when(mDevicePolicyManager.getOwnerInstalledCaCerts(managedProfileUserHandle))
                .thenReturn(Arrays.asList("ca1", "ca2"));
        assertThat(mProvider.getNumberOfOwnerInstalledCaCertsForManagedProfile())
                .isEqualTo(2);
    }

    @Test
    public void testGetNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile() {
        when(mDevicePolicyManager.getActiveAdminsAsUser(mUserId))
                .thenReturn(Arrays.asList(mAdmin1, mAdmin2));
        when(mDevicePolicyManager.getActiveAdminsAsUser(mManagedProfileUserId))
                .thenReturn(Arrays.asList(mAdmin1));

        assertThat(mProvider.getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile())
                .isEqualTo(2);

        mProfiles.add(new UserInfo(mManagedProfileUserId, "", "", UserInfo.FLAG_MANAGED_PROFILE));
        assertThat(mProvider.getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile())
                .isEqualTo(3);
    }

    @Test
    public void workPolicyInfo_unmanagedDevice_shouldDoNothing() {
        // Even if we have the intent resolved, don't show it if there's no DO or PO
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        addWorkPolicyInfoIntent(mOwner.getPackageName(), true, false);
        assertThat(mProvider.hasWorkPolicyInfo()).isFalse();

        assertThat(mProvider.showWorkPolicyInfo(mContext)).isFalse();
        verify(mContext, never()).startActivity(any());
    }

    @Test
    public void workPolicyInfo_deviceOwner_shouldResolveIntent() {
        // If the intent is not resolved, then there's no info to show for DO
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(mOwner);
        assertThat(mProvider.hasWorkPolicyInfo()).isFalse();
        assertThat(mProvider.showWorkPolicyInfo(mContext)).isFalse();

        // If the intent is resolved, then we can use it to launch the activity
        Intent intent = addWorkPolicyInfoIntent(mOwner.getPackageName(), true, false);
        assertThat(mProvider.hasWorkPolicyInfo()).isTrue();
        assertThat(mProvider.showWorkPolicyInfo(mContext)).isTrue();
        verify(mContext).startActivity(intentEquals(intent));
    }

    @Test
    public void workPolicyInfo_profileOwner_shouldResolveIntent()
            throws PackageManager.NameNotFoundException {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        List<UserHandle> mAllProfiles = new ArrayList<>();
        mAllProfiles.add(new UserHandle(mManagedProfileUserId));
        when(mUserManager.getAllProfiles()).thenReturn(mAllProfiles);
        when(mUserManager.isManagedProfile(mManagedProfileUserId)).thenReturn(true);
        when(mContext.getPackageName()).thenReturn("somePackageName");
        when(mContext.createPackageContextAsUser(
                eq(mContext.getPackageName()),
                anyInt(),
                any(UserHandle.class))
        ).thenReturn(mContext);
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(mOwner);

        // If the intent is not resolved, then there's no info to show for PO
        assertThat(mProvider.hasWorkPolicyInfo()).isFalse();
        assertThat(mProvider.showWorkPolicyInfo(mContext)).isFalse();

        // If the intent is resolved, then we can use it to launch the activity in managed profile
        Intent intent = addWorkPolicyInfoIntent(mOwner.getPackageName(), false, true);
        assertThat(mProvider.hasWorkPolicyInfo()).isTrue();
        assertThat(mProvider.showWorkPolicyInfo(mContext)).isTrue();
        verify(mContext)
                .startActivityAsUser(
                        intentEquals(intent),
                        argThat(handle -> handle.getIdentifier() == mManagedProfileUserId));
    }

    @Test
    public void workPolicyInfo_comp_shouldUseDeviceOwnerIntent() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(mOwner);
        mProfiles.add(new UserInfo(mManagedProfileUserId, "", "", UserInfo.FLAG_MANAGED_PROFILE));
        when(mDevicePolicyManager.getProfileOwnerAsUser(mUserId)).thenReturn(mOwner);

        // If the intent is not resolved, then there's no info to show for COMP
        assertThat(mProvider.hasWorkPolicyInfo()).isFalse();
        assertThat(mProvider.showWorkPolicyInfo(mContext)).isFalse();

        // If the intent is resolved, then we can use it to launch the activity for device owner
        Intent intent = addWorkPolicyInfoIntent(mOwner.getPackageName(), true, true);
        assertThat(mProvider.hasWorkPolicyInfo()).isTrue();
        assertThat(mProvider.showWorkPolicyInfo(mContext)).isTrue();
        verify(mContext).startActivity(intentEquals(intent));
    }

    @Test
    public void testShowParentalControls() {
        when(mDevicePolicyManager.getProfileOwnerOrDeviceOwnerSupervisionComponent(any()))
                .thenReturn(mOwner);

        // If the intent is resolved, then we can use it to launch the activity
        Intent intent = addParentalControlsIntent(mOwner.getPackageName());
        assertThat(mProvider.showParentalControls()).isTrue();
        verify(mContext).startActivity(intentEquals(intent));
    }

    private Intent addWorkPolicyInfoIntent(
            String packageName, boolean deviceOwner, boolean profileOwner) {
        Intent intent = new Intent(Settings.ACTION_SHOW_WORK_POLICY_INFO);
        intent.setPackage(packageName);
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.resolvePackageName = packageName;
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.name = "activityName";
        resolveInfo.activityInfo.packageName = packageName;

        List<ResolveInfo> activities = ImmutableList.of(resolveInfo);
        if (deviceOwner) {
            when(mPackageManager.queryIntentActivities(intentEquals(intent), anyInt()))
                    .thenReturn(activities);
        }
        if (profileOwner) {
            when(mPackageManager.queryIntentActivitiesAsUser(
                    intentEquals(intent), anyInt(), eq(UserHandle.of(mManagedProfileUserId))))
                    .thenReturn(activities);
        }

        return intent;
    }

    private Intent addParentalControlsIntent(String packageName) {
        Intent intent = new Intent(EnterprisePrivacyFeatureProviderImpl.ACTION_PARENTAL_CONTROLS);
        intent.setPackage(packageName);
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.resolvePackageName = packageName;
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.name = "activityName";
        resolveInfo.activityInfo.packageName = packageName;

        List<ResolveInfo> activities = ImmutableList.of(resolveInfo);
        when(mPackageManager.queryIntentActivities(intentEquals(intent), anyInt()))
                .thenReturn(activities);
        when(mPackageManager.queryIntentActivitiesAsUser(intentEquals(intent), anyInt(), anyInt()))
                .thenReturn(activities);
        return intent;
    }

    private static class IntentMatcher implements ArgumentMatcher<Intent> {
        private final Intent mExpectedIntent;

        public IntentMatcher(Intent expectedIntent) {
            mExpectedIntent = expectedIntent;
        }

        @Override
        public boolean matches(Intent actualIntent) {
            // filterEquals() compares only the action, data, type, class, and categories.
            return actualIntent != null && mExpectedIntent.filterEquals(actualIntent);
        }
    }

    private static Intent intentEquals(Intent intent) {
        return argThat(new IntentMatcher(intent));
    }

    private void resetAndInitializePackageManager() {
        reset(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN))
                .thenReturn(true);
    }
}

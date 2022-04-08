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
import android.net.ProxyInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.SpannableStringBuilder;

import com.android.settings.R;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
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

    private final ComponentName OWNER = new ComponentName("dummy", "component");
    private final ComponentName ADMIN_1 = new ComponentName("dummy", "admin1");
    private final ComponentName ADMIN_2 = new ComponentName("dummy", "admin2");
    private final String OWNER_ORGANIZATION = new String("ACME");
    private final Date TIMESTAMP = new Date(2011, 11, 11);
    private final int MY_USER_ID = UserHandle.myUserId();
    private final int MANAGED_PROFILE_USER_ID = MY_USER_ID + 1;
    private final String VPN_PACKAGE_ID = "com.example.vpn";
    private final String IME_PACKAGE_ID = "com.example.ime";
    private final String IME_PACKAGE_LABEL = "Test IME";

    private List<UserInfo> mProfiles = new ArrayList<>();

    private @Mock Context mContext;
    private @Mock DevicePolicyManager mDevicePolicyManager;
    private @Mock PackageManager mPackageManager;
    private @Mock UserManager mUserManager;
    private @Mock ConnectivityManager mConnectivityManger;
    private Resources mResources;

    private EnterprisePrivacyFeatureProvider mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        resetAndInitializePackageManager();
        when(mUserManager.getProfiles(MY_USER_ID)).thenReturn(mProfiles);
        mProfiles.add(new UserInfo(MY_USER_ID, "", "", 0 /* flags */));
        mResources = RuntimeEnvironment.application.getResources();

        mProvider = new EnterprisePrivacyFeatureProviderImpl(mContext, mDevicePolicyManager,
                mPackageManager, mUserManager, mConnectivityManger, mResources);
    }

    @Test
    public void testHasDeviceOwner() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        assertThat(mProvider.hasDeviceOwner()).isFalse();

        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(OWNER);
        assertThat(mProvider.hasDeviceOwner()).isTrue();
    }

    @Test
    public void testIsInCompMode() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(OWNER);
        assertThat(mProvider.isInCompMode()).isFalse();

        mProfiles.add(new UserInfo(MANAGED_PROFILE_USER_ID, "", "", UserInfo.FLAG_MANAGED_PROFILE));
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
    public void testGetDeviceOwnerDisclosure() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        assertThat(mProvider.getDeviceOwnerDisclosure()).isNull();

        SpannableStringBuilder disclosure = new SpannableStringBuilder();
        disclosure.append(mResources.getString(R.string.do_disclosure_generic));
        disclosure.append(mResources.getString(R.string.do_disclosure_learn_more_separator));
        disclosure.append(mResources.getString(R.string.learn_more),
                new EnterprisePrivacyFeatureProviderImpl.EnterprisePrivacySpan(mContext), 0);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(OWNER);
        when(mDevicePolicyManager.getDeviceOwnerOrganizationName()).thenReturn(null);
        assertThat(mProvider.getDeviceOwnerDisclosure()).isEqualTo(disclosure);

        disclosure = new SpannableStringBuilder();
        disclosure.append(mResources.getString(R.string.do_disclosure_with_name,
                OWNER_ORGANIZATION));
        disclosure.append(mResources.getString(R.string.do_disclosure_learn_more_separator));
        disclosure.append(mResources.getString(R.string.learn_more),
                new EnterprisePrivacyFeatureProviderImpl.EnterprisePrivacySpan(mContext), 0);
        when(mDevicePolicyManager.getDeviceOwnerOrganizationName()).thenReturn(OWNER_ORGANIZATION);
        assertThat(mProvider.getDeviceOwnerDisclosure()).isEqualTo(disclosure);
    }

    @Test
    public void testGetLastSecurityLogRetrievalTime() {
        when(mDevicePolicyManager.getLastSecurityLogRetrievalTime()).thenReturn(-1L);
        assertThat(mProvider.getLastSecurityLogRetrievalTime()).isNull();

        when(mDevicePolicyManager.getLastSecurityLogRetrievalTime())
                .thenReturn(TIMESTAMP.getTime());
        assertThat(mProvider.getLastSecurityLogRetrievalTime()).isEqualTo(TIMESTAMP);
    }

    @Test
    public void testGetLastBugReportRequestTime() {
        when(mDevicePolicyManager.getLastBugReportRequestTime()).thenReturn(-1L);
        assertThat(mProvider.getLastBugReportRequestTime()).isNull();

        when(mDevicePolicyManager.getLastBugReportRequestTime()).thenReturn(TIMESTAMP.getTime());
        assertThat(mProvider.getLastBugReportRequestTime()).isEqualTo(TIMESTAMP);
    }

    @Test
    public void testGetLastNetworkLogRetrievalTime() {
        when(mDevicePolicyManager.getLastNetworkLogRetrievalTime()).thenReturn(-1L);
        assertThat(mProvider.getLastNetworkLogRetrievalTime()).isNull();

        when(mDevicePolicyManager.getLastNetworkLogRetrievalTime()).thenReturn(TIMESTAMP.getTime());
        assertThat(mProvider.getLastNetworkLogRetrievalTime()).isEqualTo(TIMESTAMP);
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
        when(mConnectivityManger.getAlwaysOnVpnPackageForUser(MY_USER_ID)).thenReturn(null);
        assertThat(mProvider.isAlwaysOnVpnSetInCurrentUser()).isFalse();

        when(mConnectivityManger.getAlwaysOnVpnPackageForUser(MY_USER_ID))
                .thenReturn(VPN_PACKAGE_ID);
        assertThat(mProvider.isAlwaysOnVpnSetInCurrentUser()).isTrue();
    }

    @Test
    public void testIsAlwaysOnVpnSetInManagedProfileProfile() {
        assertThat(mProvider.isAlwaysOnVpnSetInManagedProfile()).isFalse();

        mProfiles.add(new UserInfo(MANAGED_PROFILE_USER_ID, "", "", UserInfo.FLAG_MANAGED_PROFILE));

        when(mConnectivityManger.getAlwaysOnVpnPackageForUser(MANAGED_PROFILE_USER_ID))
                .thenReturn(null);
        assertThat(mProvider.isAlwaysOnVpnSetInManagedProfile()).isFalse();

        when(mConnectivityManger.getAlwaysOnVpnPackageForUser(MANAGED_PROFILE_USER_ID))
                .thenReturn(VPN_PACKAGE_ID);
        assertThat(mProvider.isAlwaysOnVpnSetInManagedProfile()).isTrue();
    }

    @Test
    public void testIsGlobalHttpProxySet() {
        when(mConnectivityManger.getGlobalProxy()).thenReturn(null);
        assertThat(mProvider.isGlobalHttpProxySet()).isFalse();

        when(mConnectivityManger.getGlobalProxy())
            .thenReturn(ProxyInfo.buildDirectProxy("localhost", 123));
        assertThat(mProvider.isGlobalHttpProxySet()).isTrue();
    }

    @Test
    public void testGetMaximumFailedPasswordsForWipeInCurrentUser() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnCallingUser()).thenReturn(null);
        when(mDevicePolicyManager.getProfileOwnerAsUser(MY_USER_ID)).thenReturn(null);
        when(mDevicePolicyManager.getMaximumFailedPasswordsForWipe(OWNER, MY_USER_ID))
                .thenReturn(10);
        assertThat(mProvider.getMaximumFailedPasswordsBeforeWipeInCurrentUser()).isEqualTo(0);

        when(mDevicePolicyManager.getProfileOwnerAsUser(MY_USER_ID)).thenReturn(OWNER);
        assertThat(mProvider.getMaximumFailedPasswordsBeforeWipeInCurrentUser()).isEqualTo(10);

        when(mDevicePolicyManager.getDeviceOwnerComponentOnCallingUser()).thenReturn(OWNER);
        when(mDevicePolicyManager.getProfileOwnerAsUser(MY_USER_ID)).thenReturn(null);
        assertThat(mProvider.getMaximumFailedPasswordsBeforeWipeInCurrentUser()).isEqualTo(10);
    }

    @Test
    public void testGetMaximumFailedPasswordsForWipeInManagedProfile() {
        when(mDevicePolicyManager.getProfileOwnerAsUser(MANAGED_PROFILE_USER_ID)).thenReturn(OWNER);
        when(mDevicePolicyManager.getMaximumFailedPasswordsForWipe(OWNER, MANAGED_PROFILE_USER_ID))
                .thenReturn(10);
        assertThat(mProvider.getMaximumFailedPasswordsBeforeWipeInManagedProfile()).isEqualTo(0);

        mProfiles.add(new UserInfo(MANAGED_PROFILE_USER_ID, "", "", UserInfo.FLAG_MANAGED_PROFILE));
        assertThat(mProvider.getMaximumFailedPasswordsBeforeWipeInManagedProfile()).isEqualTo(10);
    }

    @Test
    public void testGetImeLabelIfOwnerSet() throws Exception {
        final ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        when(applicationInfo.loadLabel(mPackageManager)).thenReturn(IME_PACKAGE_LABEL);

        Settings.Secure.putString(null, Settings.Secure.DEFAULT_INPUT_METHOD, IME_PACKAGE_ID);
        when(mPackageManager.getApplicationInfoAsUser(IME_PACKAGE_ID, 0, MY_USER_ID))
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
        when(mPackageManager.getApplicationInfoAsUser(IME_PACKAGE_ID, 0, MY_USER_ID))
                .thenThrow(new PackageManager.NameNotFoundException());
        assertThat(mProvider.getImeLabelIfOwnerSet()).isNull();

        // Device Owner set IME to existent package.
        resetAndInitializePackageManager();
        when(mPackageManager.getApplicationInfoAsUser(IME_PACKAGE_ID, 0, MY_USER_ID))
                .thenReturn(applicationInfo);
        assertThat(mProvider.getImeLabelIfOwnerSet()).isEqualTo(IME_PACKAGE_LABEL);
    }

    @Test
    public void testGetNumberOfOwnerInstalledCaCertsForCurrent() {
        final UserHandle userHandle = new UserHandle(UserHandle.USER_SYSTEM);
        final UserHandle managedProfileUserHandle = new UserHandle(MANAGED_PROFILE_USER_ID);

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
        final UserHandle managedProfileUserHandle = new UserHandle(MANAGED_PROFILE_USER_ID);
        final UserInfo managedProfile =
                new UserInfo(MANAGED_PROFILE_USER_ID, "", "", UserInfo.FLAG_MANAGED_PROFILE);

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
        when(mDevicePolicyManager.getActiveAdminsAsUser(MY_USER_ID))
                .thenReturn(Arrays.asList(ADMIN_1, ADMIN_2));
        when(mDevicePolicyManager.getActiveAdminsAsUser(MANAGED_PROFILE_USER_ID))
                .thenReturn(Arrays.asList(ADMIN_1));

        assertThat(mProvider.getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile())
                .isEqualTo(2);

        mProfiles.add(new UserInfo(MANAGED_PROFILE_USER_ID, "", "", UserInfo.FLAG_MANAGED_PROFILE));
        assertThat(mProvider.getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile())
                .isEqualTo(3);
    }

    @Test
    public void workPolicyInfo_unmanagedDevice_shouldDoNothing() {
        // Even if we have the intent resolved, don't show it if there's no DO or PO
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        addWorkPolicyInfoIntent(OWNER.getPackageName(), true, false);
        assertThat(mProvider.hasWorkPolicyInfo()).isFalse();

        assertThat(mProvider.showWorkPolicyInfo()).isFalse();
        verify(mContext, never()).startActivity(any());
    }

    @Test
    public void workPolicyInfo_deviceOwner_shouldResolveIntent() {
        // If the intent is not resolved, then there's no info to show for DO
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(OWNER);
        assertThat(mProvider.hasWorkPolicyInfo()).isFalse();
        assertThat(mProvider.showWorkPolicyInfo()).isFalse();

        // If the intent is resolved, then we can use it to launch the activity
        Intent intent = addWorkPolicyInfoIntent(OWNER.getPackageName(), true, false);
        assertThat(mProvider.hasWorkPolicyInfo()).isTrue();
        assertThat(mProvider.showWorkPolicyInfo()).isTrue();
        verify(mContext).startActivity(intentEquals(intent));
    }

    @Test
    public void workPolicyInfo_profileOwner_shouldResolveIntent() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        mProfiles.add(new UserInfo(MANAGED_PROFILE_USER_ID, "", "", UserInfo.FLAG_MANAGED_PROFILE));
        when(mDevicePolicyManager.getProfileOwnerAsUser(MANAGED_PROFILE_USER_ID)).thenReturn(OWNER);

        // If the intent is not resolved, then there's no info to show for PO
        assertThat(mProvider.hasWorkPolicyInfo()).isFalse();
        assertThat(mProvider.showWorkPolicyInfo()).isFalse();

        // If the intent is resolved, then we can use it to launch the activity in managed profile
        Intent intent = addWorkPolicyInfoIntent(OWNER.getPackageName(), false, true);
        assertThat(mProvider.hasWorkPolicyInfo()).isTrue();
        assertThat(mProvider.showWorkPolicyInfo()).isTrue();
        verify(mContext)
                .startActivityAsUser(
                        intentEquals(intent),
                        argThat(handle -> handle.getIdentifier() == MANAGED_PROFILE_USER_ID));
    }

    @Test
    public void workPolicyInfo_comp_shouldUseDeviceOwnerIntent() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(OWNER);
        mProfiles.add(new UserInfo(MANAGED_PROFILE_USER_ID, "", "", UserInfo.FLAG_MANAGED_PROFILE));
        when(mDevicePolicyManager.getProfileOwnerAsUser(MY_USER_ID)).thenReturn(OWNER);

        // If the intent is not resolved, then there's no info to show for COMP
        assertThat(mProvider.hasWorkPolicyInfo()).isFalse();
        assertThat(mProvider.showWorkPolicyInfo()).isFalse();

        // If the intent is resolved, then we can use it to launch the activity for device owner
        Intent intent = addWorkPolicyInfoIntent(OWNER.getPackageName(), true, true);
        assertThat(mProvider.hasWorkPolicyInfo()).isTrue();
        assertThat(mProvider.showWorkPolicyInfo()).isTrue();
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
                            intentEquals(intent), anyInt(), eq(MANAGED_PROFILE_USER_ID)))
                    .thenReturn(activities);
        }

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

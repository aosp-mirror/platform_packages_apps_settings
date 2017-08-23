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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.net.ProxyInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.SpannableStringBuilder;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.vpn2.ConnectivityManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EnterprisePrivacyFeatureProviderImpl}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class EnterprisePrivacyFeatureProviderImplTest {

    private final ComponentName OWNER = new ComponentName("dummy", "component");
    private final ComponentName ADMIN_1 = new ComponentName("dummy", "admin1");
    private final ComponentName ADMIN_2 = new ComponentName("dummy", "admin2");
    private final String OWNER_ORGANIZATION = new String("ACME");
    private final Date TIMESTAMP = new Date(2011, 11, 11);
    private final int MY_USER_ID = UserHandle.myUserId();
    private final int MANAGED_PROFILE_USER_ID = MY_USER_ID + 1;
    private final String VPN_PACKAGE_ID = "com.example.vpn";
    private final String IME_PACKAGE_ID = "com.example.ime";
    private final String OTHER_PACKAGE_ID = "com.example.other";
    private final String IME_PACKAGE_LABEL = "Test IME";

    private List<UserInfo> mProfiles = new ArrayList();

    private @Mock Context mContext;
    private @Mock DevicePolicyManagerWrapper mDevicePolicyManager;
    private @Mock PackageManagerWrapper mPackageManagerWrapper;
    private @Mock PackageManager mPackageManager;
    private @Mock UserManager mUserManager;
    private @Mock ConnectivityManagerWrapper mConnectivityManger;
    private Resources mResources;

    private EnterprisePrivacyFeatureProvider mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        resetAndInitializePackageManagerWrapper();
        when(mUserManager.getProfiles(MY_USER_ID)).thenReturn(mProfiles);
        mProfiles.add(new UserInfo(MY_USER_ID, "", "", 0 /* flags */));
        mResources = ShadowApplication.getInstance().getApplicationContext().getResources();

        mProvider = new EnterprisePrivacyFeatureProviderImpl(mContext, mDevicePolicyManager,
                mPackageManagerWrapper, mUserManager, mConnectivityManger, mResources);
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
        disclosure.append(mResources.getString(R.string.do_disclosure_learn_more),
                new EnterprisePrivacyFeatureProviderImpl.EnterprisePrivacySpan(mContext), 0);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(OWNER);
        when(mDevicePolicyManager.getDeviceOwnerOrganizationName()).thenReturn(null);
        assertThat(mProvider.getDeviceOwnerDisclosure()).isEqualTo(disclosure);

        disclosure = new SpannableStringBuilder();
        disclosure.append(mResources.getString(R.string.do_disclosure_with_name,
                OWNER_ORGANIZATION));
        disclosure.append(mResources.getString(R.string.do_disclosure_learn_more_separator));
        disclosure.append(mResources.getString(R.string.do_disclosure_learn_more),
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

        when(mConnectivityManger.getGlobalProxy()).thenReturn(
                ProxyInfo.buildDirectProxy("localhost", 123));
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
        when(mPackageManagerWrapper.getApplicationInfoAsUser(IME_PACKAGE_ID, 0, MY_USER_ID))
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
        when(mPackageManagerWrapper.getApplicationInfoAsUser(IME_PACKAGE_ID, 0, MY_USER_ID))
                .thenThrow(new PackageManager.NameNotFoundException());
        assertThat(mProvider.getImeLabelIfOwnerSet()).isNull();

        // Device Owner set IME to existent package.
        resetAndInitializePackageManagerWrapper();
        when(mPackageManagerWrapper.getApplicationInfoAsUser(IME_PACKAGE_ID, 0, MY_USER_ID))
                .thenReturn(applicationInfo);
        assertThat(mProvider.getImeLabelIfOwnerSet()).isEqualTo(IME_PACKAGE_LABEL);
    }

    @Test
    public void testGetNumberOfOwnerInstalledCaCertsForCurrent() {
        final UserHandle userHandle = new UserHandle(UserHandle.USER_SYSTEM);
        final UserHandle managedProfileUserHandle = new UserHandle(MANAGED_PROFILE_USER_ID);
        final UserInfo managedProfile =
                new UserInfo(MANAGED_PROFILE_USER_ID, "", "", UserInfo.FLAG_MANAGED_PROFILE);

        when(mDevicePolicyManager.getOwnerInstalledCaCerts(managedProfileUserHandle))
                .thenReturn(Arrays.asList(new String[] {"ca1", "ca2"}));

        when(mDevicePolicyManager.getOwnerInstalledCaCerts(userHandle))
                .thenReturn(null);
        assertThat(mProvider.getNumberOfOwnerInstalledCaCertsForCurrentUser())
                .isEqualTo(0);
        when(mDevicePolicyManager.getOwnerInstalledCaCerts(userHandle))
                .thenReturn(new ArrayList<>());
        assertThat(mProvider.getNumberOfOwnerInstalledCaCertsForCurrentUser())
                .isEqualTo(0);
        when(mDevicePolicyManager.getOwnerInstalledCaCerts(userHandle))
                .thenReturn(Arrays.asList(new String[] {"ca1", "ca2"}));
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
                .thenReturn(Arrays.asList(new String[] {"ca1", "ca2"}));
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
                .thenReturn(Arrays.asList(new String[] {"ca1", "ca2"}));
        assertThat(mProvider.getNumberOfOwnerInstalledCaCertsForManagedProfile())
                .isEqualTo(2);
    }

    @Test
    public void testGetNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile() {
        when(mDevicePolicyManager.getActiveAdminsAsUser(MY_USER_ID))
                .thenReturn(Arrays.asList(new ComponentName[] {ADMIN_1, ADMIN_2}));
        when(mDevicePolicyManager.getActiveAdminsAsUser(MANAGED_PROFILE_USER_ID))
                .thenReturn(Arrays.asList(new ComponentName[] {ADMIN_1}));

        assertThat(mProvider.getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile())
                .isEqualTo(2);

        mProfiles.add(new UserInfo(MANAGED_PROFILE_USER_ID, "", "", UserInfo.FLAG_MANAGED_PROFILE));
        assertThat(mProvider.getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile())
                .isEqualTo(3);
    }

    private void resetAndInitializePackageManagerWrapper() {
        reset(mPackageManagerWrapper);
        when(mPackageManagerWrapper.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN))
                .thenReturn(true);
        when(mPackageManagerWrapper.getPackageManager()).thenReturn(mPackageManager);
    }
}

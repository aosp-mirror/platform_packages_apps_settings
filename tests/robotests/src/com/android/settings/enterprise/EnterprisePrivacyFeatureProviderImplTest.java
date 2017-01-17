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
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.net.ProxyInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.SpannableStringBuilder;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
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
import java.util.Date;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EnterprisePrivacyFeatureProviderImpl}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class EnterprisePrivacyFeatureProviderImplTest {

    private final ComponentName DEVICE_OWNER = new ComponentName("dummy", "component");
    private final String DEVICE_OWNER_ORGANIZATION = new String("ACME");
    private final Date TIMESTAMP = new Date(2011, 11, 11);
    private final int MY_USER_ID = UserHandle.myUserId();
    private final int MANAGED_PROFILE_USER_ID = MY_USER_ID + 1;
    private final String VPN_PACKAGE_ID = "com.example.vpn";

    private List<UserInfo> mProfiles = new ArrayList();

    private @Mock DevicePolicyManagerWrapper mDevicePolicyManager;
    private @Mock PackageManagerWrapper mPackageManager;
    private @Mock UserManager mUserManager;
    private @Mock ConnectivityManagerWrapper mConnectivityManger;
    private Resources mResources;

    private EnterprisePrivacyFeatureProvider mProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN))
                .thenReturn(true);
        when(mUserManager.getProfiles(MY_USER_ID)).thenReturn(mProfiles);
        mProfiles.add(new UserInfo(MY_USER_ID, "", "", 0 /* flags */));
        mResources = ShadowApplication.getInstance().getApplicationContext().getResources();

        mProvider = new EnterprisePrivacyFeatureProviderImpl(mDevicePolicyManager, mPackageManager,
                mUserManager, mConnectivityManger, mResources);
    }

    @Test
    public void testHasDeviceOwner() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        assertThat(mProvider.hasDeviceOwner()).isFalse();

        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(DEVICE_OWNER);
        assertThat(mProvider.hasDeviceOwner()).isTrue();
    }

    @Test
    public void testIsInCompMode() {
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(DEVICE_OWNER);
        assertThat(mProvider.isInCompMode()).isFalse();

        mProfiles.add(new UserInfo(MANAGED_PROFILE_USER_ID, "", "", UserInfo.FLAG_MANAGED_PROFILE));
        assertThat(mProvider.isInCompMode()).isTrue();
    }

    @Test
    public void testGetDeviceOwnerDisclosure() {
        final Context context = mock(Context.class);

        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(null);
        assertThat(mProvider.getDeviceOwnerDisclosure(context)).isNull();

        SpannableStringBuilder disclosure = new SpannableStringBuilder();
        disclosure.append(mResources.getString(R.string.do_disclosure_generic));
        disclosure.append(mResources.getString(R.string.do_disclosure_learn_more_separator));
        disclosure.append(mResources.getString(R.string.do_disclosure_learn_more),
                new EnterprisePrivacyFeatureProviderImpl.EnterprisePrivacySpan(context), 0);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(DEVICE_OWNER);
        when(mDevicePolicyManager.getDeviceOwnerOrganizationName()).thenReturn(null);
        assertThat(mProvider.getDeviceOwnerDisclosure(context)).isEqualTo(disclosure);

        disclosure = new SpannableStringBuilder();
        disclosure.append(mResources.getString(R.string.do_disclosure_with_name,
                DEVICE_OWNER_ORGANIZATION));
        disclosure.append(mResources.getString(R.string.do_disclosure_learn_more_separator));
        disclosure.append(mResources.getString(R.string.do_disclosure_learn_more),
                new EnterprisePrivacyFeatureProviderImpl.EnterprisePrivacySpan(context), 0);
        when(mDevicePolicyManager.getDeviceOwnerOrganizationName())
                .thenReturn(DEVICE_OWNER_ORGANIZATION);
        assertThat(mProvider.getDeviceOwnerDisclosure(context)).isEqualTo(disclosure);
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
    public void testIsAlwaysOnVpnSetInPrimaryUser() {
        when(mConnectivityManger.getAlwaysOnVpnPackageForUser(MY_USER_ID)).thenReturn(null);
        assertThat(mProvider.isAlwaysOnVpnSetInPrimaryUser()).isFalse();

        when(mConnectivityManger.getAlwaysOnVpnPackageForUser(MY_USER_ID))
                .thenReturn(VPN_PACKAGE_ID);
        assertThat(mProvider.isAlwaysOnVpnSetInPrimaryUser()).isTrue();
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
}

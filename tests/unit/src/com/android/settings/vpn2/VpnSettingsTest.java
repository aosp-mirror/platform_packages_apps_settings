/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.vpn2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class VpnSettingsTest {
    private static final int USER_ID_1 = UserHandle.USER_NULL;
    private static final String VPN_GROUP_KEY = "vpn_group";
    private static final String VPN_GROUP_TITLE = "vpn_group_title";
    private static final String VPN_PACKAGE_NAME = "vpn.package.name";
    private static final String VPN_LAUNCH_INTENT = "vpn.action";
    private static final String ADVANCED_VPN_GROUP_KEY = "advanced_vpn_group";
    private static final String ADVANCED_VPN_GROUP_TITLE = "advanced_vpn_group_title";
    private static final String ADVANCED_VPN_PACKAGE_NAME = "advanced.vpn.package.name";
    private static final String ADVANCED_VPN_LAUNCH_INTENT = "advanced.vpn.action";

    private final Intent mVpnIntent = new Intent().setAction(VPN_LAUNCH_INTENT);
    private final Intent mAdvancedVpnIntent = new Intent().setAction(ADVANCED_VPN_LAUNCH_INTENT);

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private PackageManager mPackageManager;

    private VpnSettings mVpnSettings;
    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private PreferenceGroup mAdvancedVpnGroup;
    private PreferenceGroup mVpnGroup;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    @UiThreadTest
    public void setUp() throws PackageManager.NameNotFoundException {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mVpnSettings = spy(new VpnSettings());
        mContext = spy(ApplicationProvider.getApplicationContext());
        mAdvancedVpnGroup = spy(new PreferenceCategory(mContext));
        mVpnGroup = spy(new PreferenceCategory(mContext));
        mAdvancedVpnGroup.setKey(ADVANCED_VPN_GROUP_KEY);
        mVpnGroup.setKey(VPN_GROUP_KEY);
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mAdvancedVpnGroup);
        mPreferenceScreen.addPreference(mVpnGroup);
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mVpnSettings.init(mPreferenceScreen, mFakeFeatureFactory.getAdvancedVpnFeatureProvider());

        when(mVpnSettings.getContext()).thenReturn(mContext);
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider
                .getAdvancedVpnPreferenceGroupTitle(mContext)).thenReturn(ADVANCED_VPN_GROUP_TITLE);
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.getVpnPreferenceGroupTitle(mContext))
                .thenReturn(VPN_GROUP_TITLE);
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.getAdvancedVpnPackageName())
                .thenReturn(ADVANCED_VPN_PACKAGE_NAME);
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.isAdvancedVpnSupported(any()))
                .thenReturn(true);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        doReturn(mContext).when(mContext).createContextAsUser(any(), anyInt());
        doReturn(mContext).when(mContext).createPackageContextAsUser(any(), anyInt(), any());
        doReturn(mPreferenceManager).when(mVpnGroup).getPreferenceManager();
        doReturn(mPreferenceManager).when(mAdvancedVpnGroup).getPreferenceManager();
    }

    @Test
    public void setShownAdvancedPreferences_hasGeneralVpn_returnsVpnCountAs1() {
        Set<Preference> updates = new ArraySet<>();
        AppPreference pref =
                spy(new AppPreference(mContext, USER_ID_1, VPN_PACKAGE_NAME));
        updates.add(pref);

        mVpnSettings.setShownAdvancedPreferences(updates);

        assertThat(mVpnGroup.getPreferenceCount()).isEqualTo(1);
        assertThat(mVpnGroup.isVisible()).isTrue();
        assertThat(mAdvancedVpnGroup.isVisible()).isFalse();
    }

    @Test
    public void setShownAdvancedPreferences_hasAdvancedVpn_returnsAdvancedVpnCountAs1() {
        Set<Preference> updates = new ArraySet<>();
        AppPreference pref =
                spy(new AppPreference(mContext, USER_ID_1, ADVANCED_VPN_PACKAGE_NAME));
        updates.add(pref);

        mVpnSettings.setShownAdvancedPreferences(updates);

        assertThat(mAdvancedVpnGroup.getPreferenceCount()).isEqualTo(1);
        assertThat(mAdvancedVpnGroup.isVisible()).isTrue();
        assertThat(mVpnGroup.isVisible()).isFalse();
    }

    @Test
    public void setShownAdvancedPreferences_noVpn_returnsEmpty() {
        Set<Preference> updates = new ArraySet<>();

        mVpnSettings.setShownAdvancedPreferences(updates);

        assertThat(mAdvancedVpnGroup.getPreferenceCount()).isEqualTo(0);
        assertThat(mVpnGroup.getPreferenceCount()).isEqualTo(0);
        assertThat(mAdvancedVpnGroup.isVisible()).isFalse();
        assertThat(mVpnGroup.isVisible()).isFalse();
    }

    @Test
    public void getVpnApps_isAdvancedVpn_returnsOne() throws Exception {
        ApplicationInfo info = new ApplicationInfo();
        info.uid = 1111;
        when(mPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(info);

        assertThat(VpnSettings.getVpnApps(mContext, /* includeProfiles= */ false,
                mFakeFeatureFactory.getAdvancedVpnFeatureProvider(),
                mAppOpsManager).size()).isEqualTo(1);
    }

    @Test
    public void getVpnApps_isNotAdvancedVpn_returnsEmpty() {
        int uid = 1111;
        List<AppOpsManager.OpEntry> opEntries = new ArrayList<>();
        List<AppOpsManager.PackageOps> apps = new ArrayList<>();
        AppOpsManager.PackageOps packageOps =
                new AppOpsManager.PackageOps(VPN_PACKAGE_NAME, uid, opEntries);
        apps.add(packageOps);
        when(mAppOpsManager.getPackagesForOps((int[]) any())).thenReturn(apps);
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.isAdvancedVpnSupported(any()))
                .thenReturn(false);

        assertThat(VpnSettings.getVpnApps(mContext, /* includeProfiles= */ false,
                mFakeFeatureFactory.getAdvancedVpnFeatureProvider(),
                mAppOpsManager)).isEmpty();
    }

    @Test
    public void clickVpn_VpnConnected_doesNotStartVpnLaunchIntent()
            throws PackageManager.NameNotFoundException {
        Set<Preference> updates = new ArraySet<>();
        AppPreference pref = spy(new AppPreference(mContext, USER_ID_1, VPN_PACKAGE_NAME));
        pref.setState(AppPreference.STATE_CONNECTED);
        updates.add(pref);
        when(mContext.createPackageContextAsUser(any(), anyInt(), any())).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getLaunchIntentForPackage(any())).thenReturn(mVpnIntent);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivityAsUser(captor.capture(), any());
        mVpnSettings.setShownPreferences(updates);

        mVpnSettings.onPreferenceClick(pref);

        verify(mContext, never()).startActivityAsUser(any(), any());
    }

    @Test
    public void clickVpn_VpnDisconnected_startsVpnLaunchIntent()
            throws PackageManager.NameNotFoundException {
        Set<Preference> updates = new ArraySet<>();
        AppPreference pref = spy(new AppPreference(mContext, USER_ID_1, VPN_PACKAGE_NAME));
        pref.setState(AppPreference.STATE_DISCONNECTED);
        updates.add(pref);
        when(mContext.createPackageContextAsUser(any(), anyInt(), any())).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getLaunchIntentForPackage(any())).thenReturn(mVpnIntent);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivityAsUser(captor.capture(), any());
        mVpnSettings.setShownPreferences(updates);

        mVpnSettings.onPreferenceClick(pref);

        verify(mContext).startActivityAsUser(captor.capture(), any());
        assertThat(TextUtils.equals(captor.getValue().getAction(),
                VPN_LAUNCH_INTENT)).isTrue();
    }

    @Test
    public void clickAdvancedVpn_VpnConnectedDisconnectDialogDisabled_startsAppLaunchIntent()
            throws PackageManager.NameNotFoundException {
        Set<Preference> updates = new ArraySet<>();
        AppPreference pref =
                spy(new AppPreference(mContext, USER_ID_1, ADVANCED_VPN_PACKAGE_NAME));
        pref.setState(AppPreference.STATE_CONNECTED);
        updates.add(pref);
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.isDisconnectDialogEnabled())
                .thenReturn(false);
        when(mContext.createPackageContextAsUser(any(), anyInt(), any())).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getLaunchIntentForPackage(any())).thenReturn(mAdvancedVpnIntent);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivityAsUser(captor.capture(), any());
        mVpnSettings.setShownAdvancedPreferences(updates);

        mVpnSettings.onPreferenceClick(pref);

        verify(mContext).startActivityAsUser(captor.capture(), any());
        assertThat(TextUtils.equals(captor.getValue().getAction(),
                ADVANCED_VPN_LAUNCH_INTENT)).isTrue();
    }

    @Test
    public void clickAdvancedVpn_VpnConnectedDisconnectDialogEnabled_doesNotStartAppLaunchIntent()
            throws PackageManager.NameNotFoundException {
        Set<Preference> updates = new ArraySet<>();
        AppPreference pref =
                spy(new AppPreference(mContext, USER_ID_1, ADVANCED_VPN_PACKAGE_NAME));
        pref.setState(AppPreference.STATE_CONNECTED);
        updates.add(pref);
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.isDisconnectDialogEnabled())
                .thenReturn(true);
        when(mContext.createPackageContextAsUser(any(), anyInt(), any())).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.getLaunchIntentForPackage(any())).thenReturn(mAdvancedVpnIntent);
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivityAsUser(captor.capture(), any());
        mVpnSettings.setShownAdvancedPreferences(updates);

        mVpnSettings.onPreferenceClick(pref);

        verify(mContext, never()).startActivityAsUser(any(), any());
    }
}

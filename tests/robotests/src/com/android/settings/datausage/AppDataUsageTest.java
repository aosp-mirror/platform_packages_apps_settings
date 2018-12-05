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
 * limitations under the License.
 */

package com.android.settings.datausage;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.NetworkPolicyManager;
import android.os.Bundle;
import android.util.ArraySet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.AppItem;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.net.NetworkCycleDataForUid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowEntityHeaderController.class, ShadowRestrictedLockUtilsInternal.class})
public class AppDataUsageTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private EntityHeaderController mHeaderController;
    @Mock
    private PackageManager mPackageManager;

    private AppDataUsage mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() {
        ShadowEntityHeaderController.reset();
    }

    @Test
    public void bindAppHeader_allWorkApps_shouldNotShowAppInfoLink() {
        ShadowEntityHeaderController.setUseMock(mHeaderController);
        when(mHeaderController.setRecyclerView(any(), any())).thenReturn(mHeaderController);
        when(mHeaderController.setUid(anyInt())).thenReturn(mHeaderController);

        mFragment = spy(new AppDataUsage());

        when(mFragment.getPreferenceManager())
            .thenReturn(mock(PreferenceManager.class, RETURNS_DEEP_STUBS));
        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();
        ReflectionHelpers.setField(mFragment, "mAppItem", mock(AppItem.class));

        mFragment.onViewCreated(new View(RuntimeEnvironment.application), new Bundle());

        verify(mHeaderController).setHasAppInfoLink(false);
    }

    @Test
    public void bindAppHeader_workApp_shouldSetWorkAppUid() throws
            PackageManager.NameNotFoundException {
        final int fakeUserId = 100;

        mFragment = spy(new AppDataUsage());
        final ArraySet<String> packages = new ArraySet<>();
        packages.add("pkg");
        final AppItem appItem = new AppItem(123456789);

        ReflectionHelpers.setField(mFragment, "mPackageManager", mPackageManager);
        ReflectionHelpers.setField(mFragment, "mAppItem", appItem);
        ReflectionHelpers.setField(mFragment, "mPackages", packages);

        when(mPackageManager.getPackageUidAsUser(anyString(), anyInt()))
                .thenReturn(fakeUserId);

        ShadowEntityHeaderController.setUseMock(mHeaderController);
        when(mHeaderController.setRecyclerView(any(), any())).thenReturn(mHeaderController);
        when(mHeaderController.setUid(fakeUserId)).thenReturn(mHeaderController);
        when(mHeaderController.setHasAppInfoLink(anyBoolean())).thenReturn(mHeaderController);

        when(mFragment.getPreferenceManager())
            .thenReturn(mock(PreferenceManager.class, RETURNS_DEEP_STUBS));
        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();

        mFragment.onViewCreated(new View(RuntimeEnvironment.application), new Bundle());

        verify(mHeaderController).setHasAppInfoLink(true);
        verify(mHeaderController).setUid(fakeUserId);
    }

    @Test
    public void changePreference_backgroundData_shouldUpdateUI() {
        mFragment = spy(new AppDataUsage());
        final AppItem appItem = new AppItem(123456789);
        final RestrictedSwitchPreference pref = mock(RestrictedSwitchPreference.class);
        final DataSaverBackend dataSaverBackend = mock(DataSaverBackend.class);
        ReflectionHelpers.setField(mFragment, "mAppItem", appItem);
        ReflectionHelpers.setField(mFragment, "mRestrictBackground", pref);
        ReflectionHelpers.setField(mFragment, "mDataSaverBackend", dataSaverBackend);

        doNothing().when(mFragment).updatePrefs();

        mFragment.onPreferenceChange(pref, true /* value */);

        verify(mFragment).updatePrefs();
    }

    @Test
    public void updatePrefs_restrictedByAdmin_shouldDisablePreference() {
        mFragment = spy(new AppDataUsage());
        final int testUid = 123123;
        final AppItem appItem = new AppItem(testUid);
        final RestrictedSwitchPreference restrictBackgroundPref
                = mock(RestrictedSwitchPreference.class);
        final RestrictedSwitchPreference unrestrictedDataPref
                = mock(RestrictedSwitchPreference.class);
        final DataSaverBackend dataSaverBackend = mock(DataSaverBackend.class);
        final NetworkPolicyManager networkPolicyManager = mock(NetworkPolicyManager.class);
        ReflectionHelpers.setField(mFragment, "mAppItem", appItem);
        ReflectionHelpers.setField(mFragment, "mRestrictBackground", restrictBackgroundPref);
        ReflectionHelpers.setField(mFragment, "mUnrestrictedData", unrestrictedDataPref);
        ReflectionHelpers.setField(mFragment, "mDataSaverBackend", dataSaverBackend);
        ReflectionHelpers.setField(mFragment.services, "mPolicyManager", networkPolicyManager);

        ShadowRestrictedLockUtilsInternal.setRestricted(true);
        doReturn(NetworkPolicyManager.POLICY_NONE).when(networkPolicyManager)
                .getUidPolicy(testUid);

        mFragment.updatePrefs();

        verify(restrictBackgroundPref).setDisabledByAdmin(any(EnforcedAdmin.class));
        verify(unrestrictedDataPref).setDisabledByAdmin(any(EnforcedAdmin.class));
    }

    @Test
    public void bindData_noAppUsageData_shouldHideCycleSpinner() {
        mFragment = spy(new AppDataUsage());
        final SpinnerPreference cycle = mock(SpinnerPreference.class);
        ReflectionHelpers.setField(mFragment, "mCycle", cycle);
        final Preference preference = mock(Preference.class);
        ReflectionHelpers.setField(mFragment, "mBackgroundUsage", preference);
        ReflectionHelpers.setField(mFragment, "mForegroundUsage", preference);
        ReflectionHelpers.setField(mFragment, "mTotalUsage", preference);
        doReturn(RuntimeEnvironment.application).when(mFragment).getContext();

        mFragment.bindData(0 /* position */);

        verify(cycle).setVisible(false);
    }

    @Test
    public void bindData_hasAppUsageData_shouldShowCycleSpinnerAndUpdateUsageSummary() {
        mFragment = spy(new AppDataUsage());
        final Context context = RuntimeEnvironment.application;
        doReturn(context).when(mFragment).getContext();
        final long backgroundBytes = 1234L;
        final long foregroundBytes = 5678L;
        final List<NetworkCycleDataForUid> appUsage = new ArrayList<>();
        appUsage.add(new NetworkCycleDataForUid.Builder()
            .setBackgroundUsage(backgroundBytes).setForegroundUsage(foregroundBytes).build());
        ReflectionHelpers.setField(mFragment, "mUsageData", appUsage);
        final Preference backgroundPref = mock(Preference.class);
        ReflectionHelpers.setField(mFragment, "mBackgroundUsage", backgroundPref);
        final Preference foregroundPref = mock(Preference.class);
        ReflectionHelpers.setField(mFragment, "mForegroundUsage", foregroundPref);
        final Preference totalPref = mock(Preference.class);
        ReflectionHelpers.setField(mFragment, "mTotalUsage", totalPref);
        final SpinnerPreference cycle = mock(SpinnerPreference.class);
        ReflectionHelpers.setField(mFragment, "mCycle", cycle);

        mFragment.bindData(0 /* position */);

        verify(cycle).setVisible(true);
        verify(totalPref).setSummary(
            DataUsageUtils.formatDataUsage(context, backgroundBytes + foregroundBytes));
        verify(backgroundPref).setSummary(DataUsageUtils.formatDataUsage(context, backgroundBytes));
        verify(foregroundPref).setSummary(DataUsageUtils.formatDataUsage(context, foregroundBytes));
    }
}

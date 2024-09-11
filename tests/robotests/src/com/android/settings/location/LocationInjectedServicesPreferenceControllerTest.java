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
package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.widget.RestrictedAppPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowDevicePolicyManager.class})
public class LocationInjectedServicesPreferenceControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String KEY_LOCATION_SERVICES = "location_service";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocationSettings mFragment;
    @Mock
    private PreferenceCategory mCategoryPrimary;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private AppSettingsInjector mSettingsInjector;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private Context mContext;
    private LocationInjectedServicesPreferenceController mController;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mController = spy(
                new LocationInjectedServicesPreferenceController(mContext, KEY_LOCATION_SERVICES));
        when(mFragment.getSettingsLifecycle()).thenReturn(mLifecycle);
        mController.init(mFragment);
        mController.mInjector = mSettingsInjector;
        final String key = mController.getPreferenceKey();
        when(mScreen.findPreference(key)).thenReturn(mCategoryPrimary);
        when(mCategoryPrimary.getKey()).thenReturn(key);
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
    }

    @Test
    public void onResume_shouldRegisterListener() {
        mController.onResume();

        verify(mContext).registerReceiver(eq(mController.mInjectedSettingsReceiver),
                eq(mController.INTENT_FILTER_INJECTED_SETTING_CHANGED),
                anyInt());
    }

    @Test
    public void onPause_shouldUnregisterListener() {
        mController.onResume();
        mController.onPause();

        verify(mContext).unregisterReceiver(mController.mInjectedSettingsReceiver);
    }

    @Test
    public void workProfileDisallowShareLocationOn_getParentUserLocationServicesOnly() {
        final int fakeWorkProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakeWorkProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().addProfile(new UserInfo(fakeWorkProfileId, "",
                UserInfo.FLAG_MANAGED_PROFILE | UserInfo.FLAG_PROFILE));

        // Mock RestrictedLockUtils.checkIfRestrictionEnforced and let it return non-null.
        final List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new UserManager.EnforcingUser(fakeWorkProfileId,
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        final ComponentName componentName = new ComponentName("test", "test");
        // Ensure that RestrictedLockUtils.checkIfRestrictionEnforced doesn't return null.
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_SHARE_LOCATION,
                UserHandle.of(fakeWorkProfileId),
                enforcingUsers);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(componentName);

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue())
                .doesNotContain(UserHandle.of(fakeWorkProfileId));
    }

    @Test
    public void workProfileDisallowShareLocationOff_getAllUserLocationServices() {
        final int fakeWorkProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakeWorkProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().addProfile(new UserInfo(fakeWorkProfileId, "",
                UserInfo.FLAG_MANAGED_PROFILE | UserInfo.FLAG_PROFILE));

        // Mock RestrictedLockUtils.checkIfRestrictionEnforced and let it return null.
        // Empty enforcing users.
        final List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_SHARE_LOCATION,
                UserHandle.of(fakeWorkProfileId),
                enforcingUsers);

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue()).contains(UserHandle.of(fakeWorkProfileId));
    }

    @Test
    public void privateProfileDisallowShareLocationOn_getParentUserLocationServicesOnly() {
        mSetFlagsRule.enableFlags(
                android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES,
                android.multiuser.Flags.FLAG_HANDLE_INTERLEAVED_SETTINGS_FOR_PRIVATE_SPACE);
        final int fakePrivateProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakePrivateProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().setPrivateProfile(fakePrivateProfileId, "private", 0);
        ShadowUserManager.getShadow().addUserProfile(UserHandle.of(fakePrivateProfileId));

        // Mock RestrictedLockUtils.checkIfRestrictionEnforced and let it return non-null.
        final List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new UserManager.EnforcingUser(fakePrivateProfileId,
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        final ComponentName componentName = new ComponentName("test", "test");
        // Ensure that RestrictedLockUtils.checkIfRestrictionEnforced doesn't return null.
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_SHARE_LOCATION,
                UserHandle.of(fakePrivateProfileId),
                enforcingUsers);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(componentName);

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue())
                .doesNotContain(UserHandle.of(fakePrivateProfileId));
    }

    @Test
    public void privateProfileDisallowShareLocationOff_getAllUserLocationServices() {
        mSetFlagsRule.enableFlags(
                android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES,
                android.multiuser.Flags.FLAG_HANDLE_INTERLEAVED_SETTINGS_FOR_PRIVATE_SPACE);
        final int fakePrivateProfileId = 123;
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(
                new int[]{UserHandle.myUserId(), fakePrivateProfileId});
        ShadowUserManager.getShadow().addProfile(new UserInfo(UserHandle.myUserId(), "", 0));
        ShadowUserManager.getShadow().setPrivateProfile(fakePrivateProfileId, "private", 0);
        ShadowUserManager.getShadow().addUserProfile(UserHandle.of(fakePrivateProfileId));

        // Mock RestrictedLockUtils.checkIfRestrictionEnforced and let it return null.
        // Empty enforcing users.
        final List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_SHARE_LOCATION,
                UserHandle.of(fakePrivateProfileId),
                enforcingUsers);

        mController.displayPreference(mScreen);

        ArgumentCaptor<ArraySet<UserHandle>> profilesArgumentCaptor =
                ArgumentCaptor.forClass(ArraySet.class);
        verify(mSettingsInjector).getInjectedSettings(
                any(Context.class), profilesArgumentCaptor.capture());
        assertThat(profilesArgumentCaptor.getValue()).contains(UserHandle.of(fakePrivateProfileId));
    }

    @Test
    public void onLocationModeChanged_shouldRequestReloadInjectedSettigns() {
        mController.onLocationModeChanged(Settings.Secure.LOCATION_MODE_BATTERY_SAVING, false);

        verify(mSettingsInjector).reloadStatusMessages();
    }

    @Test
    public void withUserRestriction_shouldDisableLocationAccuracy() {
        final List<Preference> preferences = new ArrayList<>();
        final RestrictedAppPreference pref = new RestrictedAppPreference(mContext,
                UserManager.DISALLOW_CONFIG_LOCATION);
        pref.setTitle("Location Accuracy");
        preferences.add(pref);
        final Map<Integer, List<Preference>> map = new ArrayMap<>();
        map.put(UserHandle.myUserId(), preferences);
        doReturn(map).when(mSettingsInjector)
                .getInjectedSettings(any(Context.class), any(ArraySet.class));
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(new int[]{UserHandle.myUserId()});

        final int userId = UserHandle.myUserId();
        List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new UserManager.EnforcingUser(userId,
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        ComponentName componentName = new ComponentName("test", "test");
        // Ensure that RestrictedLockUtils.checkIfRestrictionEnforced doesn't return null.
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_CONFIG_LOCATION,
                UserHandle.of(userId),
                enforcingUsers);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(componentName);

        mController.displayPreference(mScreen);

        assertThat(pref.isEnabled()).isFalse();
        assertThat(pref.isDisabledByAdmin()).isTrue();
    }
}

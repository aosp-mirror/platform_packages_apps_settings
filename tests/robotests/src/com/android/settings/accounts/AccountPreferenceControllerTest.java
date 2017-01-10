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
package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.PreferenceFragment;
import android.util.SparseArray;

import com.android.settings.AccessiblePreferenceCategory;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAccountManager;
import com.android.settings.testutils.shadow.ShadowContentResolver;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AccountPreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceFragment mFragment;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private AccountManager mAccountManager;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private AccountRestrictionHelper mAccountHelper;

    private FakeFeatureFactory mFactory;
    private Context mContext;
    private AccountPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.USER_SERVICE, mUserManager);
        shadowContext.setSystemService(Context.ACCOUNT_SERVICE, mAccountManager);
        mContext = spy(shadowContext.getApplicationContext());
        FakeFeatureFactory.setupForTest(mContext);
        mFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        when(mFactory.dashboardFeatureProvider.isEnabled()).thenReturn(true);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(
            new AuthenticatorDescription[0]);
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(new Account[0]);
        mController = new AccountPreferenceController(mContext, mFragment, null, mAccountHelper);
    }

    @Test
    public void onResume_managedProfile_shouldNotAddAccountCategory() {
        when(mUserManager.isManagedProfile()).thenReturn(true);
        mController.onResume();

        verify(mScreen, never()).addPreference(any(Preference.class));
    }

    @Test
    @Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
    public void onResume_linkedUser_shouldAddOneAccountCategory() {
        final UserInfo info = new UserInfo(1, "user 1", 0);
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(true);
        when(mUserManager.getUserInfo(anyInt())).thenReturn(info);

        mController.onResume();

        verify(mScreen, times(1)).addPreference(any(PreferenceGroup.class));
    }

    @Test
    @Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
    public void onResume_oneProfile_shouldAddOneAccountCategory() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        mController.onResume();

        verify(mScreen, times(1)).addPreference(any(PreferenceGroup.class));
    }

    @Test
    @Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
    public void onResume_twoProfiles_shouldAddTwoAccountCategory() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        infos.add(new UserInfo(2, "user 2", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        mController.onResume();

        verify(mScreen, times(2)).addPreference(any(PreferenceGroup.class));
    }

    @Test
    @Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
    public void onResume_oneProfiles_shouldRemoveOneAccountCategory() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
            preferenceGroup);

        // First time resume will build the UI, 2nd time will refresh the UI
        mController.onResume();
        mController.onResume();

        verify(mScreen, times(1)).removePreference(any(PreferenceGroup.class));
        verify(mScreen).removePreference(preferenceGroup);
    }

    @Test
    @Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
    public void onResume_twoProfiles_shouldRemoveTwoAccountCategory() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        infos.add(new UserInfo(2, "user 2", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
            preferenceGroup);

        // First time resume will build the UI, 2nd time will refresh the UI
        mController.onResume();
        mController.onResume();

        verify(mScreen, times(2)).removePreference(any(PreferenceGroup.class));
        verify(mScreen, times(2)).removePreference(preferenceGroup);
    }

    @Test
    @Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
    public void onResume_noPreferenceScreen_shouldNotCrash() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
                preferenceGroup);

        mController.onResume();

        // Should not crash
    }

    @Test
    public void updateRawDataToIndex_ManagedProfile_shouldNotUpdate() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        when(mUserManager.isManagedProfile()).thenReturn(true);

        mController.updateRawDataToIndex(data);

        assertThat(data).isEmpty();
    }

    @Test
    public void updateRawDataToIndex_DisabledUser_shouldNotUpdate() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", UserInfo.FLAG_DISABLED));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        mController.updateRawDataToIndex(data);

        assertThat(data).isEmpty();
    }

    @Test
    public void updateRawDataToIndex_EnabledUser_shouldAddOne() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        mController.updateRawDataToIndex(data);

        assertThat(data.size()).isEqualTo(1);
    }

    @Test
    public void updateRawDataToIndex_ManagedUser_shouldAddThree() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        mController.updateRawDataToIndex(data);

        assertThat(data.size()).isEqualTo(3);
    }

    @Test
    public void updateRawDataToIndex_DisallowRemove_shouldAddTwo() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        when(mAccountHelper.hasBaseUserRestriction(
                eq(UserManager.DISALLOW_REMOVE_MANAGED_PROFILE), anyInt()))
            .thenReturn(true);

        mController.updateRawDataToIndex(data);

        assertThat(data.size()).isEqualTo(2);
    }

    @Test
    public void updateRawDataToIndex_DisallowModify_shouldAddTwo() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        when(mAccountHelper.hasBaseUserRestriction(
            eq(UserManager.DISALLOW_MODIFY_ACCOUNTS), anyInt())).thenReturn(true);

        mController.updateRawDataToIndex(data);

        assertThat(data.size()).isEqualTo(2);
    }

    @Test
    @Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
    public void onResume_twoAccountsOfSameType_shouldAddThreePreferences() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isLinkedUser()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        Account[] accounts = {new Account("Account1", "com.acct1")};
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(accounts);

        Account[] accountType1 = new Account[2];
        accountType1[0] = new Account("Account11", "com.acct1");
        accountType1[1] = new Account("Account12", "com.acct1");
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct1"), any(UserHandle.class)))
            .thenReturn(accountType1);

        AuthenticatorDescription[] authDescs = {
            new AuthenticatorDescription("com.acct1", "com.android.settings",
                R.string.account_settings_title, 0, 0, 0, false)
        };
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(authDescs);

        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
            preferenceGroup);

        mController.onResume();

        // should add 2 individual account and the Add account preference
        verify(preferenceGroup, times(3)).addPreference(any(Preference.class));
    }

}

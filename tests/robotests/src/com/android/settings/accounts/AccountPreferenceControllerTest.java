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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.AccessiblePreferenceCategory;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.testutils.shadow.ShadowAccountManager;
import com.android.settings.testutils.shadow.ShadowContentResolver;
import com.android.settings.testutils.shadow.ShadowSettingsLibUtils;
import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class,
        ShadowSettingsLibUtils.class})
public class AccountPreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private UserManager mUserManager;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private DashboardFragment mFragment;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private AccountManager mAccountManager;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private AccountRestrictionHelper mAccountHelper;

    private Context mContext;
    private AccountPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final ShadowApplication shadowApp = ShadowApplication.getInstance();
        shadowApp.setSystemService(Context.USER_SERVICE, mUserManager);
        shadowApp.setSystemService(Context.ACCOUNT_SERVICE, mAccountManager);

        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt()))
                .thenReturn(new AuthenticatorDescription[0]);
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(new Account[0]);
        mController = new AccountPreferenceController(mContext, mFragment, null, mAccountHelper,
                ProfileSelectFragment.ProfileType.ALL);
    }

    @After
    public void tearDown() {
        ShadowContentResolver.reset();
    }

    @Test
    public void onResume_managedProfile_shouldNotAddAccountCategory() {
        when(mUserManager.isManagedProfile()).thenReturn(true);
        mController.onResume();

        verify(mScreen, never()).addPreference(any(Preference.class));
    }

    @Test
    public void onResume_linkedUser_shouldAddOneAccountCategory() {
        final UserInfo info = new UserInfo(1, "user 1", 0);
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(true);
        when(mUserManager.getUserInfo(anyInt())).thenReturn(info);

        mController.onResume();

        verify(mScreen, times(1)).addPreference(any(PreferenceGroup.class));
    }

    @Test
    public void onResume_oneProfile_shouldAddOneAccountCategory() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        mController.onResume();

        verify(mScreen, times(1)).addPreference(any(PreferenceGroup.class));
    }

    @Test
    public void onResume_twoProfiles_shouldAddTwoAccountCategory() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        infos.add(new UserInfo(2, "user 2", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        mController.onResume();

        verify(mScreen, times(2)).addPreference(any(PreferenceGroup.class));
    }

    @Test
    public void onResume_noProfileChange_shouldNotAddOrRemoveAccountCategory() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        infos.add(new UserInfo(2, "user 2", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        // First time resume will build the UI
        mController.onResume();
        reset(mScreen);

        mController.onResume();
        verify(mScreen, never()).addPreference(any(PreferenceGroup.class));
        verify(mScreen, never()).removePreference(any(PreferenceGroup.class));
    }

    @Test
    public void onResume_oneNewProfile_shouldAddOneAccountCategory() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        // First time resume will build the UI
        mController.onResume();
        // add a new profile
        infos.add(new UserInfo(2, "user 2", UserInfo.FLAG_MANAGED_PROFILE));
        reset(mScreen);

        mController.onResume();
        verify(mScreen, times(1)).addPreference(any(PreferenceGroup.class));
    }

    @Test
    public void onResume_oneProfileRemoved_shouldRemoveOneAccountCategory() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        infos.add(new UserInfo(2, "user 2", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        // First time resume will build the UI
        mController.onResume();
        // remove a profile
        infos.remove(1);

        mController.onResume();
        verify(mScreen, times(1)).removePreference(any(PreferenceGroup.class));
    }

    @Test
    public void onResume_oneProfile_shouldSetAccountTitleWithUserName() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
                preferenceGroup);

        mController.onResume();

        verify(preferenceGroup).setTitle(
                mContext.getString(R.string.account_for_section_header, "user 1"));
    }

    @Test
    public void onResume_noPreferenceScreen_shouldNotCrash() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
                preferenceGroup);

        mController.onResume();

        // Should not crash
    }

    @Test
    public void onResume_noPreferenceManager_shouldNotCrash() {
        when(mFragment.getPreferenceManager()).thenReturn(null);
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        mController.onResume();

        // Should not crash
    }

    @Test
    public void updateRawDataToIndex_noManagedProfile_shouldContainAddAccount() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        when(mUserManager.isManagedProfile()).thenReturn(false);

        mController.updateRawDataToIndex(data);

        assertThat(data).hasSize(1);
        assertThat(data.get(0).key).isEqualTo("add_account");
    }


    @Test
    public void updateRawDataToIndex_ManagedProfile_shouldContainAddAccount() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        when(mUserManager.isManagedProfile()).thenReturn(true);

        mController.updateRawDataToIndex(data);

        assertThat(data).hasSize(1);
        assertThat(data.get(0).key).isEqualTo("add_account");
    }

    @Test
    public void updateDynamicRawDataToIndex_enabledUser_notManagedUser_shouldNotUpdate() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        mController.updateDynamicRawDataToIndex(data);

        assertThat(data.size()).isEqualTo(0);
    }

    @Test
    public void updateDynamicRawDataToIndex_managedUser_shouldAddTwo() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        mController.updateDynamicRawDataToIndex(data);

        assertThat(data.size()).isEqualTo(2);
    }

    @Test
    public void updateDynamicRawDataToIndex_disallowRemove_shouldAddOne() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        when(mAccountHelper.hasBaseUserRestriction(
                eq(UserManager.DISALLOW_REMOVE_MANAGED_PROFILE), anyInt()))
                .thenReturn(true);

        mController.updateDynamicRawDataToIndex(data);

        assertThat(data.size()).isEqualTo(1);
    }

    @Test
    public void updateDynamicRawDataToIndex_disallowModify_shouldAddTwo() {
        final List<SearchIndexableRaw> data = new ArrayList<>();
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        when(mAccountHelper.hasBaseUserRestriction(
                eq(UserManager.DISALLOW_MODIFY_ACCOUNTS), anyInt())).thenReturn(true);

        mController.updateDynamicRawDataToIndex(data);

        assertThat(data.size()).isEqualTo(2);
    }

    @Test
    public void onResume_twoAccountsOfSameType_shouldAddThreePreferences() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        Account[] accounts = {new Account("Account1", "com.acct1")};
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(accounts);

        Account[] accountType1 = {
                new Account("Account11", "com.acct1"),
                new Account("Account12", "com.acct1")
        };
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct1"), any(UserHandle.class)))
                .thenReturn(accountType1);

        AuthenticatorDescription[] authDescs = {
                new AuthenticatorDescription("com.acct1", "com.android.settings",
                        R.string.account_settings_title, 0, 0, 0, false)
        };
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(authDescs);

        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(preferenceGroup.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
                preferenceGroup);

        mController.onResume();

        // should add 2 individual account and the Add account preference
        verify(preferenceGroup, times(3)).addPreference(any(Preference.class));
    }

    @Test
    @Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
    public void onResume_twoAccountsOfSameName_shouldAddFivePreferences() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        final Account[] accountType1 = new Account[2];
        accountType1[0] = new Account("Account1", "com.acct1");
        accountType1[1] = new Account("Account2", "com.acct1");
        final Account[] accountType2 = new Account[2];
        accountType2[0] = new Account("Account1", "com.acct2");
        accountType2[1] = new Account("Account2", "com.acct2");
        final Account[] allAccounts = new Account[4];
        allAccounts[0] = accountType1[0];
        allAccounts[1] = accountType1[1];
        allAccounts[2] = accountType2[0];
        allAccounts[3] = accountType2[1];
        final AuthenticatorDescription[] authDescs = {
                new AuthenticatorDescription("com.acct1", "com.android.settings",
                        R.string.account_settings_title, 0, 0, 0, false),
                new AuthenticatorDescription("com.acct2", "com.android.settings",
                        R.string.account_settings_title, 0, 0, 0, false)
        };

        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(allAccounts);
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct1"), any(UserHandle.class)))
                .thenReturn(accountType1);
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct2"), any(UserHandle.class)))
                .thenReturn(accountType2);
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(authDescs);

        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(preferenceGroup.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
                preferenceGroup);

        mController.onResume();

        // should add 4 individual account and the Add account preference
        verify(preferenceGroup, times(5)).addPreference(any(Preference.class));
    }

    @Test
    public void onResume_noAccountChange_shouldNotAddAccountPreference() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        Account[] accounts = {new Account("Acct1", "com.acct1")};
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(accounts);

        Account[] accountType1 = new Account[2];
        accountType1[0] = new Account("Acct11", "com.acct1");
        accountType1[1] = new Account("Acct12", "com.acct1");
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct1"), any(UserHandle.class)))
                .thenReturn(accountType1);

        AuthenticatorDescription[] authDescs = {
                new AuthenticatorDescription("com.acct1", "com.android.settings",
                        R.string.account_settings_title, 0, 0, 0, false)
        };
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(authDescs);

        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(preferenceGroup.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
                preferenceGroup);
        mController.onResume();

        mController.onResume();

        // each account should be added only once
        verify(preferenceGroup).addPreference(argThat(titleMatches("Acct11")));
        verify(preferenceGroup).addPreference(argThat(titleMatches("Acct12")));
    }

    @Test
    public void onResume_oneNewAccount_shouldAddOneAccountPreference() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        Account[] accounts = {new Account("Acct1", "com.acct1")};
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(accounts);

        Account[] accountType1 = new Account[2];
        accountType1[0] = new Account("Acct11", "com.acct1");
        accountType1[1] = new Account("Acct12", "com.acct1");
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct1"), any(UserHandle.class)))
                .thenReturn(accountType1);

        AuthenticatorDescription[] authDescs = {
                new AuthenticatorDescription("com.acct1", "com.android.settings",
                        R.string.account_settings_title, 0, 0, 0, false)
        };
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(authDescs);

        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(preferenceGroup.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
                preferenceGroup);

        mController.onResume();

        // add a new account
        accountType1 = new Account[3];
        accountType1[0] = new Account("Acct11", "com.acct1");
        accountType1[1] = new Account("Acct12", "com.acct1");
        accountType1[2] = new Account("Acct13", "com.acct1");
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct1"), any(UserHandle.class)))
                .thenReturn(accountType1);

        mController.onResume();

        // each account should be added only once
        verify(preferenceGroup, times(1)).addPreference(argThat(titleMatches("Acct11")));
        verify(preferenceGroup, times(1)).addPreference(argThat(titleMatches("Acct12")));
        verify(preferenceGroup, times(1)).addPreference(argThat(titleMatches("Acct13")));
    }

    @Test
    public void onResume_oneNewAccountType_shouldAddOneAccountPreference() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        infos.add(new UserInfo(2, "user 2", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(preferenceGroup.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
                preferenceGroup);

        // First time resume will build the UI with no account
        mController.onResume();

        // Add new account
        Account[] accounts = {new Account("Acct1", "com.acct1")};
        when(mAccountManager.getAccountsAsUser(2)).thenReturn(accounts);
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct1"), any(UserHandle.class)))
                .thenReturn(accounts);

        AuthenticatorDescription[] authDescs = {
                new AuthenticatorDescription("com.acct1", "com.android.settings",
                        R.string.account_settings_title, 0, 0, 0, false)
        };
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(authDescs);

        // Resume should show the newly added account
        mController.onResume();

        verify(preferenceGroup).addPreference(argThat(titleMatches("Acct1")));
    }

    @Test
    public void onResume_oneAccountRemoved_shouldRemoveOneAccountPreference() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", 0));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);
        Account[] accounts = {new Account("Acct1", "com.acct1")};
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(accounts);

        Account[] accountType1 = {
                new Account("Acct11", "com.acct1"),
                new Account("Acct12", "com.acct1")
        };
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct1"), any(UserHandle.class)))
                .thenReturn(accountType1);

        AuthenticatorDescription[] authDescs = {
                new AuthenticatorDescription("com.acct1", "com.android.settings",
                        R.string.account_settings_title, 0, 0, 0, false)
        };
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(authDescs);

        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(preferenceGroup.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class))).thenReturn(
                preferenceGroup);

        mController.onResume();

        // remove an account
        accountType1 = new Account[]{new Account("Acct11", "com.acct1")};
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct1"), any(UserHandle.class)))
                .thenReturn(accountType1);

        mController.onResume();

        verify(preferenceGroup, times(1)).addPreference(argThat(titleMatches("Acct11")));
        verify(preferenceGroup, times(1)).addPreference(argThat(titleMatches("Acct12")));
        verify(preferenceGroup, times(1)).removePreference(argThat(titleMatches("Acct12")));
    }

    @Test
    public void onResume_userReEnabled_shouldAddOneAccountPreference() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(1, "user 1", UserInfo.FLAG_DISABLED));
        when(mUserManager.isManagedProfile()).thenReturn(false);
        when(mUserManager.isRestrictedProfile()).thenReturn(false);
        when(mUserManager.getProfiles(anyInt())).thenReturn(infos);

        Account[] accounts = {new Account("Acct1", "com.acct1")};
        when(mAccountManager.getAccountsAsUser(1)).thenReturn(accounts);
        when(mAccountManager.getAccountsByTypeAsUser(eq("com.acct1"), any(UserHandle.class)))
                .thenReturn(accounts);

        AuthenticatorDescription[] authDescs = {
                new AuthenticatorDescription("com.acct1", "com.android.settings",
                        R.string.account_settings_title, 0 /* iconId */, 0 /* smallIconId */,
                        0 /* prefId */, false /* customTokens */)
        };
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(authDescs);

        AccessiblePreferenceCategory preferenceGroup = mock(AccessiblePreferenceCategory.class);
        when(preferenceGroup.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mAccountHelper.createAccessiblePreferenceCategory(any(Context.class)))
                .thenReturn(preferenceGroup);

        // First time resume will build the UI with no account
        mController.onResume();
        verify(preferenceGroup, never()).addPreference(argThat(titleMatches("Acct1")));

        // Enable the user
        infos.remove(0 /* index */);
        infos.add(new UserInfo(1, "user 1", 0 /* flags */));

        // Resume should show the account for the user
        mController.onResume();

        verify(preferenceGroup).addPreference(argThat(titleMatches("Acct1")));
    }

    private static ArgumentMatcher<Preference> titleMatches(String expected) {
        return preference -> TextUtils.equals(expected, preference.getTitle());
    }
}

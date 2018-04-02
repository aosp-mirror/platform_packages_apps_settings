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
package com.android.settings.accounts;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowAccountManager;
import com.android.settings.testutils.shadow.ShadowContentResolver;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        ShadowDevicePolicyManager.class
})
public class RemoveAccountPreferenceControllerTest {

    private static final String KEY_REMOVE_ACCOUNT = "remove_account";
    private static final String TAG_REMOVE_ACCOUNT_DIALOG = "confirmRemoveAccount";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private AccountManager mAccountManager;
    @Mock
    private PreferenceFragment mFragment;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private LayoutPreference mPreference;

    private RemoveAccountPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication.getInstance().setSystemService(Context.ACCOUNT_SERVICE, mAccountManager);

        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mPreferenceManager.getContext()).thenReturn(RuntimeEnvironment.application);
        when(mFragment.getFragmentManager()).thenReturn(mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt()))
                .thenReturn(new AuthenticatorDescription[0]);
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(new Account[0]);
        mController = new RemoveAccountPreferenceController(RuntimeEnvironment.application,
                mFragment);
    }

    @Test
    public void displayPreference_shouldAddClickListener() {
        when(mScreen.findPreference(KEY_REMOVE_ACCOUNT)).thenReturn(mPreference);
        final Button button = mock(Button.class);
        when(mPreference.findViewById(R.id.button)).thenReturn(button);

        mController.displayPreference(mScreen);

        verify(button).setOnClickListener(mController);
    }

    @Test
    public void onClick_shouldStartConfirmDialog() {
        when(mFragment.isAdded()).thenReturn(true);
        mController.onClick(null);

        verify(mFragmentTransaction).add(
                any(RemoveAccountPreferenceController.ConfirmRemoveAccountDialog.class),
                eq(TAG_REMOVE_ACCOUNT_DIALOG));
    }

    @Test
    public void onClick_shouldNotStartConfirmDialogWhenModifyAccountsIsDisallowed() {
        when(mFragment.isAdded()).thenReturn(true);

        final int userId = UserHandle.myUserId();
        mController.init(new Account("test", "test"), UserHandle.of(userId));

        List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new UserManager.EnforcingUser(userId,
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        ComponentName componentName = new ComponentName("test", "test");
        // Ensure that RestrictedLockUtils.checkIfRestrictionEnforced doesn't return null.
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_MODIFY_ACCOUNTS,
                UserHandle.of(userId),
                enforcingUsers);
        ShadowDevicePolicyManager.getShadow().setDeviceOwnerComponentOnAnyUser(componentName);

        mController.onClick(null);

        verify(mFragmentTransaction, never()).add(
                any(RemoveAccountPreferenceController.ConfirmRemoveAccountDialog.class),
                eq(TAG_REMOVE_ACCOUNT_DIALOG));
    }

    @Test
    @Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
    public void confirmRemove_shouldRemoveAccount() {
        when(mFragment.isAdded()).thenReturn(true);
        Activity activity = mock(Activity.class);
        when(activity.getSystemService(Context.ACCOUNT_SERVICE)).thenReturn(mAccountManager);
        when(mFragment.getActivity()).thenReturn(activity);

        Account account = new Account("Account11", "com.acct1");
        UserHandle userHandle = new UserHandle(10);
        RemoveAccountPreferenceController.ConfirmRemoveAccountDialog dialog =
                RemoveAccountPreferenceController.ConfirmRemoveAccountDialog.show(
                        mFragment, account, userHandle);
        dialog.onCreate(new Bundle());
        dialog.onClick(null, 0);
        verify(mAccountManager).removeAccountAsUser(eq(account), nullable(Activity.class),
                nullable(AccountManagerCallback.class), nullable(Handler.class), eq(userHandle));
    }
}

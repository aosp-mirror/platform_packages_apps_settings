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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import androidx.collection.ArraySet;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowAccountManager;
import com.android.settings.testutils.shadow.ShadowContentResolver;
import com.android.settingslib.accounts.AuthenticatorHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
        ShadowAccountManager.class,
        ShadowContentResolver.class,
})
public class AccountTypePreferenceLoaderTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private AccountManager mAccountManager;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceFragmentCompat mPreferenceFragment;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceManager mManager;

    private Context mContext;
    private Account mAccount;
    private AccountTypePreferenceLoader mPrefLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.ACCOUNT_SERVICE, mAccountManager);
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(
            new AuthenticatorDescription[0]);
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(new Account[0]);
        when(mPreferenceFragment.getActivity().getPackageManager()).thenReturn(mPackageManager);
        mContext = RuntimeEnvironment.application;
        mAccount = new Account("name", "type");
        final AuthenticatorHelper helper = new AuthenticatorHelper(mContext, UserHandle.CURRENT,
            null /* OnAccountsUpdateListener */);
        mPrefLoader = spy(new AccountTypePreferenceLoader(mPreferenceFragment, helper,
            UserHandle.CURRENT));
    }

    @After
    public void tearDown() {
        ShadowContentResolver.reset();
    }

    @Test
    public void updatePreferenceIntents_shouldRunRecursively() {
        // Top level
        PreferenceGroup prefRoot = spy(new PreferenceScreen(mContext, null));
        when(prefRoot.getPreferenceManager()).thenReturn(mManager);
        Preference pref1 = mock(Preference.class);
        PreferenceGroup prefGroup2 = spy(new PreferenceScreen(mContext, null));
        when(prefGroup2.getPreferenceManager()).thenReturn(mManager);
        Preference pref3 = mock(Preference.class);
        PreferenceGroup prefGroup4 = spy(new PreferenceScreen(mContext, null));
        when(prefGroup4.getPreferenceManager()).thenReturn(mManager);
        prefRoot.addPreference(pref1);
        prefRoot.addPreference(prefGroup2);
        prefRoot.addPreference(pref3);
        prefRoot.addPreference(prefGroup4);

        // 2nd level
        Preference pref21 = mock(Preference.class);
        Preference pref22 = mock(Preference.class);
        prefGroup2.addPreference(pref21);
        prefGroup2.addPreference(pref22);
        PreferenceGroup prefGroup41 = spy(new PreferenceScreen(mContext, null));
        when(prefGroup41.getPreferenceManager()).thenReturn(mManager);
        Preference pref42 = mock(Preference.class);
        prefGroup4.addPreference(prefGroup41);
        prefGroup4.addPreference(pref42);

        // 3rd level
        Preference pref411 = mock(Preference.class);
        Preference pref412 = mock(Preference.class);
        prefGroup41.addPreference(pref411);
        prefGroup41.addPreference(pref412);

        final String acctType = "testType";
        mPrefLoader.updatePreferenceIntents(prefRoot, acctType, mAccount);

        verify(mPrefLoader).updatePreferenceIntents(prefGroup2, acctType, mAccount);
        verify(mPrefLoader).updatePreferenceIntents(prefGroup4, acctType, mAccount);
        verify(mPrefLoader).updatePreferenceIntents(prefGroup41, acctType, mAccount);
    }

    @Test
    public void generateFragmentAllowlist_nullPrefGroup_emptyList() {
        Set<String> allowed = mPrefLoader.generateFragmentAllowlist(null);

        assertThat(allowed).isEmpty();
    }

    @Test
    public void generateFragmentAllowlist_simpleGroupNoFragment_emptyList() {
        Preference pref = new Preference(mContext);
        PreferenceScreen screen = spy(new PreferenceScreen(mContext, null));
        when(screen.getPreferenceManager()).thenReturn(mManager);
        screen.addPreference(pref);

        Set<String> allowed = mPrefLoader.generateFragmentAllowlist(screen);

        assertThat(allowed).isEmpty();
    }

    @Test
    public void generateFragmentAllowlist_simpleGroupOneFragment_populatedList() {
        Preference pref = new Preference(mContext);
        pref.setFragment("test");
        PreferenceScreen screen = spy(new PreferenceScreen(mContext, null));
        when(screen.getPreferenceManager()).thenReturn(mManager);
        screen.addPreference(pref);

        Set<String> allowed = mPrefLoader.generateFragmentAllowlist(screen);

        assertThat(allowed).isNotEmpty();
    }

    @Test
    public void generateFragmentAllowlist_nestedGroupWithFragments_populatedList() {
        Preference pref = new Preference(mContext);
        pref.setFragment("test");
        PreferenceScreen nested = spy(new PreferenceScreen(mContext, null));
        PreferenceScreen parent = spy(new PreferenceScreen(mContext, null));
        when(nested.getPreferenceManager()).thenReturn(mManager);
        when(parent.getPreferenceManager()).thenReturn(mManager);
        parent.addPreference(nested);
        nested.addPreference(pref);

        Set<String> allowed = mPrefLoader.generateFragmentAllowlist(parent);

        assertThat(allowed).isNotEmpty();
    }

    @Test
    public void filterBlockedFragments_nullPrefGroup_noop() {
        // verify no NPE
        mPrefLoader.filterBlockedFragments(null, new ArraySet<>());
    }

    @Test
    public void filterBlockedFragments_simplePrefGroupNoFragment_noop() {
        Preference pref = spy(new Preference(mContext));
        PreferenceScreen screen = spy(new PreferenceScreen(mContext, null));
        when(screen.getPreferenceManager()).thenReturn(mManager);
        screen.addPreference(pref);

        mPrefLoader.filterBlockedFragments(screen, new ArraySet<>());

        verify(screen, never()).setOnPreferenceClickListener(any());
        verify(pref, never()).setOnPreferenceClickListener(any());
    }

    @Test
    public void filterBlockedFragments_simplePrefGroupWithAllowedFragment_noop() {
        Preference pref = spy(new Preference(mContext));
        pref.setFragment("test");
        PreferenceScreen screen = spy(new PreferenceScreen(mContext, null));
        when(screen.getPreferenceManager()).thenReturn(mManager);
        screen.addPreference(pref);

        mPrefLoader.filterBlockedFragments(screen, Set.of("test"));

        verify(screen, never()).setOnPreferenceClickListener(any());
        verify(pref, never()).setOnPreferenceClickListener(any());
    }

    @Test
    public void filterBlockedFragments_simplePrefGroupNoMatchFragment_overrideClick() {
        Preference pref = spy(new Preference(mContext));
        pref.setFragment("test");
        PreferenceScreen screen = spy(new PreferenceScreen(mContext, null));
        when(screen.getPreferenceManager()).thenReturn(mManager);
        screen.addPreference(pref);

        mPrefLoader.filterBlockedFragments(screen, new ArraySet<>());

        verify(pref).setOnPreferenceClickListener(any());
    }

    @Test
    public void filterBlockedFragments_nestedPrefGroupWithNoMatchFragment_overrideClick() {
        Preference pref = spy(new Preference(mContext));
        pref.setFragment("test");
        PreferenceScreen nested = spy(new PreferenceScreen(mContext, null));
        PreferenceScreen parent = spy(new PreferenceScreen(mContext, null));
        when(nested.getPreferenceManager()).thenReturn(mManager);
        when(parent.getPreferenceManager()).thenReturn(mManager);
        parent.addPreference(nested);
        nested.addPreference(pref);

        mPrefLoader.filterBlockedFragments(parent, Set.of("nomatch", "other"));
        verify(pref).setOnPreferenceClickListener(any());
    }
}

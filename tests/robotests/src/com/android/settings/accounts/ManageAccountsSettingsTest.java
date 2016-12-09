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
import android.content.SyncInfo;
import android.content.SyncStatusInfo;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceScreen;
import android.util.ArraySet;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ManageAccountsSettingsTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AccountPreference mAccountPref;
    private Account mAccount;
    private ArrayList<String> mAuthorities;
    private TestFragment mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAuthorities = new ArrayList<>();
        mAuthorities.add("authority");
        mAccount = new Account("name", "type");
        when(mAccountPref.getAccount()).thenReturn(mAccount);
        when(mAccountPref.getAuthorities()).thenReturn(mAuthorities);
        mSettings = new TestFragment();
    }

    @Test
    public void showSyncState_noAccountPrefs_shouldUpdateNothing() {
        when(mAccountPref.getAuthorities()).thenReturn(null);
        mSettings.showSyncState();
        verify(mSettings.getPreferenceScreen(), never()).getPreference(anyInt());
    }

    @Test
    public void showSyncState_syncInProgress_shouldUpdateInProgress() {
        mSettings.mUserFacingSyncAuthorities.add(mAuthorities.get(0));
        mSettings.mSyncInfos.add(new SyncInfo(0, mAccount, mAuthorities.get(0), 0));
        mSettings.mSyncStatusInfo = new SyncStatusInfo(0);
        when(mSettings.getPreferenceScreen().getPreferenceCount()).thenReturn(1);
        when(mSettings.getPreferenceScreen().getPreference(0)).thenReturn(mAccountPref);

        mSettings.showSyncState();

        verify(mSettings.getPreferenceScreen()).getPreference(anyInt());
        verify(mAccountPref).setSyncStatus(AccountPreference.SYNC_IN_PROGRESS, true);
    }

    @Test
    public void showSyncState_noUserFacingSynclets_shouldUpdateToDisabled() {
        mSettings.mSyncInfos.add(new SyncInfo(0, mAccount, mAuthorities.get(0), 0));
        mSettings.mSyncStatusInfo = new SyncStatusInfo(0);
        when(mSettings.getPreferenceScreen().getPreferenceCount()).thenReturn(1);
        when(mSettings.getPreferenceScreen().getPreference(0)).thenReturn(mAccountPref);

        mSettings.showSyncState();

        verify(mSettings.getPreferenceScreen()).getPreference(anyInt());
        verify(mAccountPref).setSyncStatus(AccountPreference.SYNC_DISABLED, true);
    }

    public static class TestFragment extends ManageAccountsSettings {

        private PreferenceScreen mScreen;
        private List<SyncInfo> mSyncInfos;
        private SyncStatusInfo mSyncStatusInfo;

        public TestFragment() {
            mUserHandle = mock(UserHandle.class);
            mScreen = mock(PreferenceScreen.class);
            mUserFacingSyncAuthorities = new ArraySet<>();
            mSyncInfos = new ArrayList<>();
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mScreen;
        }

        @Override
        protected boolean isSyncEnabled(int userId, Account account, String authority) {
            return true;
        }

        @Override
        protected List<SyncInfo> getCurrentSyncs(int userId) {
            return mSyncInfos;
        }

        @Override
        protected SyncStatusInfo getSyncStatusInfo(Account account, String authority, int userId) {
            return mSyncStatusInfo;
        }
    }

}
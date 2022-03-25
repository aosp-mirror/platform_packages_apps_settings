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
 * limitations under the License
 */
package com.android.settings.accounts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.provider.SearchIndexableResource;

import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AccountDashboardFragmentTest {

    private static final int PROFILE_ID = 10;
    private static final String PROFILE_NAME = "User";
    private static final String ACCOUNT_TYPE = "com.android.settings";
    private static final String ACCOUNT_NAME = "test account";

    @Mock
    private UserManager mUserManager;
    @Mock
    private AccountManager mAccountManager;

    private Context mContext;
    private AccountDashboardFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFragment = new AccountDashboardFragment();

        doReturn(mUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
    }

    @Test
    public void testCategory_isAccount() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_ACCOUNT);
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                AccountDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(RuntimeEnvironment.application, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    public void searchIndexProvider_hasManagedProfile_shouldNotIndex() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(PROFILE_ID, PROFILE_NAME, UserInfo.FLAG_MANAGED_PROFILE));
        doReturn(infos).when(mUserManager).getProfiles(anyInt());

        final List<SearchIndexableRaw> indexRaws =
                AccountDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getDynamicRawDataToIndex(mContext, true /* enabled */);

        assertThat(indexRaws).isEmpty();
    }

    @Test
    public void searchIndexProvider_hasAccounts_shouldIndex() {
        final List<UserInfo> infos = new ArrayList<>();
        infos.add(new UserInfo(PROFILE_ID, PROFILE_NAME, UserInfo.FLAG_PRIMARY));
        doReturn(infos).when(mUserManager).getProfiles(anyInt());

        final Account[] accounts = {
                new Account(ACCOUNT_NAME, ACCOUNT_TYPE)
        };
        when(AccountManager.get(mContext)).thenReturn(mAccountManager);
        doReturn(accounts).when(mAccountManager).getAccounts();

        final List<SearchIndexableRaw> indexRaws =
                AccountDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getDynamicRawDataToIndex(mContext, true /* enabled */);

        assertThat(indexRaws).isNotEmpty();
    }

    @Test
    public void shouldSkipForInitialSUW_returnTrue() {
        assertThat(mFragment.shouldSkipForInitialSUW()).isTrue();
    }
}

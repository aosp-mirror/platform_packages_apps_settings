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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowAccountManager;
import com.android.settings.testutils.shadow.ShadowContentResolver;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AccountDetailDashboardFragmentTest {

    private static final String METADATA_CATEGORY = "com.android.settings.category";
    private static final String METADATA_ACCOUNT_TYPE = "com.android.settings.ia.account";
    private static final String METADATA_USER_HANDLE = "user_handle";
    private static final String PREF_ACCOUNT_HEADER = "account_header";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private AccountManager mAccountManager;
    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    private AccountDetailDashboardFragment mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.ACCOUNT_SERVICE, mAccountManager);
        mContext = spy(shadowContext.getApplicationContext());

        mFragment = spy(new AccountDetailDashboardFragment());
        final Bundle args = new Bundle();
        args.putParcelable(METADATA_USER_HANDLE, UserHandle.CURRENT);
        mFragment.setArguments(args);
        mFragment.mAccountType = "com.abc";
        mFragment.mAccount = new Account("name1@abc.com", "com.abc");
    }

    @Test
    public void testCategory_isAccount() {
        assertThat(new AccountDetailDashboardFragment().getCategoryKey())
                .isEqualTo(CategoryKey.CATEGORY_ACCOUNT);
    }

    @Test
    public void refreshDashboardTiles_HasAccountType_shouldDisplay() {
        final Tile tile = new Tile();
        final Bundle metaData = new Bundle();
        metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT);
        metaData.putString(METADATA_ACCOUNT_TYPE, "com.abc");
        tile.metaData = metaData;

        assertThat(mFragment.displayTile(tile)).isTrue();
    }

    @Test
    public void refreshDashboardTiles_NoAccountType_shouldNotDisplay() {
        final Tile tile = new Tile();
        final Bundle metaData = new Bundle();
        metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT);
        tile.metaData = metaData;

        assertThat(mFragment.displayTile(tile)).isFalse();
    }

    @Test
    public void refreshDashboardTiles_OtherAccountType_shouldNotDisplay() {
        final Tile tile = new Tile();
        final Bundle metaData = new Bundle();
        metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT);
        metaData.putString(METADATA_ACCOUNT_TYPE, "com.other");
        tile.metaData = metaData;

        assertThat(mFragment.displayTile(tile)).isFalse();
    }

    @Test
    @Config(shadows = {ShadowAccountManager.class, ShadowContentResolver.class})
    public void updateAccountHeader_shouldShowAccountName() throws Exception {
        when(mAccountManager.getAuthenticatorTypesAsUser(anyInt())).thenReturn(
            new AuthenticatorDescription[0]);
        when(mAccountManager.getAccountsAsUser(anyInt())).thenReturn(new Account[0]);
        when(mFragment.getContext()).thenReturn(mContext);
        doReturn(mScreen).when(mFragment).getPreferenceScreen();
        doReturn(mPreference).when(mFragment).findPreference(PREF_ACCOUNT_HEADER);

        mFragment.updateUi();

        verify(mPreference).setTitle("name1@abc.com");
    }

}

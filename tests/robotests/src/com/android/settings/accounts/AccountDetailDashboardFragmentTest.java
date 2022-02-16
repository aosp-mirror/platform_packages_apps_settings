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

import static android.content.Intent.EXTRA_USER;

import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_KEYHINT;
import static com.android.settingslib.drawer.TileUtils.META_DATA_PREFERENCE_TITLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.dashboard.DashboardFeatureProviderImpl;
import com.android.settings.testutils.shadow.ShadowAccountManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.drawer.ActivityTile;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.Tile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAccountManager.class, ShadowUserManager.class})
public class AccountDetailDashboardFragmentTest {

    private static final String METADATA_CATEGORY = "com.android.settings.category";
    private static final String METADATA_ACCOUNT_TYPE = "com.android.settings.ia.account";
    private static final String METADATA_USER_HANDLE = "user_handle";

    private AccountDetailDashboardFragment mFragment;
    private Context mContext;
    private ActivityInfo mActivityInfo;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mActivityInfo = new ActivityInfo();
        mActivityInfo.packageName = mContext.getPackageName();
        mActivityInfo.name = "clazz";
        mActivityInfo.metaData = new Bundle();

        final Bundle args = new Bundle();
        args.putParcelable(METADATA_USER_HANDLE, UserHandle.CURRENT);

        mFragment = spy(new AccountDetailDashboardFragment());
        mFragment.setArguments(args);
        mFragment.mAccountType = "com.abc";
        mFragment.mAccount = new Account("name1@abc.com", "com.abc");
        when(mFragment.getContext()).thenReturn(mContext);
    }

    @After
    public void tearDown() {
        ShadowAccountManager.reset();
    }

    @Test
    public void testCategory_isAccountDetail() {
        assertThat(new AccountDetailDashboardFragment().getCategoryKey())
                .isEqualTo(CategoryKey.CATEGORY_ACCOUNT_DETAIL);
    }

    @Test
    public void refreshDashboardTiles_HasAccountType_shouldDisplay() {
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        mActivityInfo.metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        mActivityInfo.metaData.putString(METADATA_ACCOUNT_TYPE, "com.abc");

        assertThat(mFragment.displayTile(tile)).isTrue();
    }

    @Test
    public void refreshDashboardTiles_NoAccountType_shouldNotDisplay() {
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        mActivityInfo.metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT_DETAIL);

        assertThat(mFragment.displayTile(tile)).isFalse();
    }

    @Test
    public void refreshDashboardTiles_OtherAccountType_shouldNotDisplay() {
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        mActivityInfo.metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        mActivityInfo.metaData.putString(METADATA_ACCOUNT_TYPE, "com.other");

        assertThat(mFragment.displayTile(tile)).isFalse();
    }

    @Test
    public void refreshDashboardTiles_HasAccountType_shouldAddAccountNameToIntent() {
        final DashboardFeatureProviderImpl dashboardFeatureProvider =
                new DashboardFeatureProviderImpl(mContext);
        final PackageManager packageManager = mock(PackageManager.class);
        ReflectionHelpers.setField(dashboardFeatureProvider, "mPackageManager", packageManager);
        when(packageManager.resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(mock(ResolveInfo.class));

        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_KEYHINT, "key");
        mActivityInfo.metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT);
        mActivityInfo.metaData.putString(METADATA_ACCOUNT_TYPE, "com.abc");
        mActivityInfo.metaData.putString(META_DATA_PREFERENCE_TITLE, "summary");
        mActivityInfo.metaData.putString("com.android.settings.intent.action",
                Intent.ACTION_ASSIST);
        tile.userHandle = null;
        mFragment.displayTile(tile);

        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        final Preference preference = new Preference(mContext);
        dashboardFeatureProvider.bindPreferenceToTileAndGetObservers(activity,
                false /* forceRoundedIcon */, MetricsProto.MetricsEvent.DASHBOARD_SUMMARY,
                preference, tile, null /* key */, Preference.DEFAULT_ORDER);

        assertThat(preference.getKey()).isEqualTo(tile.getKey(mContext));
        preference.performClick();

        final Intent intent = Shadows.shadowOf(activity).getNextStartedActivityForResult().intent;

        assertThat(intent.getStringExtra("extra.accountName")).isEqualTo("name1@abc.com");
    }

    @Test
    public void displayTile_shouldAddUserHandleToTileIntent() {
        mFragment.mUserHandle = new UserHandle(1);

        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        mActivityInfo.metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT);
        mActivityInfo.metaData.putString(METADATA_ACCOUNT_TYPE, "com.abc");

        mFragment.displayTile(tile);

        final UserHandle userHandle = tile.getIntent().getParcelableExtra(EXTRA_USER);
        assertThat(userHandle.getIdentifier()).isEqualTo(1);
    }

    @Test
    public void onResume_accountMissing_shouldFinish() {
        ShadowUserManager userManager =
            Shadow.extract(mContext.getSystemService(UserManager.class));

        userManager.addUserProfile(new UserHandle(1));
        ShadowAccountManager.addAccountForUser(1, new Account("test@test.com", "com.test"));

        mFragment.finishIfAccountMissing();
        verify(mFragment).finish();
    }

    @Test
    public void onResume_accountPresentOneProfile_shouldNotFinish() {
        ShadowUserManager userManager =
            Shadow.extract(mContext.getSystemService(UserManager.class));

        userManager.addUserProfile(new UserHandle(1));
        ShadowAccountManager.addAccountForUser(1, mFragment.mAccount);

        mFragment.finishIfAccountMissing();
        verify(mFragment, never()).finish();
    }

    @Test
    public void onResume_accountPresentTwoProfiles_shouldNotFinish() {
        ShadowUserManager userManager =
            Shadow.extract(mContext.getSystemService(UserManager.class));

        userManager.addUserProfile(new UserHandle(1));
        userManager.addUserProfile(new UserHandle(2));
        ShadowAccountManager.addAccountForUser(1, new Account("test@test.com", "com.test"));
        ShadowAccountManager.addAccountForUser(2, mFragment.mAccount);

        mFragment.finishIfAccountMissing();
        verify(mFragment, never()).finish();
    }
}

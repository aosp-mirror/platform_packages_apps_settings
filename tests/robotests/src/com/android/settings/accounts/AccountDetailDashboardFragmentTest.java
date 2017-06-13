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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.DashboardFeatureProviderImpl;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AccountDetailDashboardFragmentTest {

    private static final String METADATA_CATEGORY = "com.android.settings.category";
    private static final String METADATA_ACCOUNT_TYPE = "com.android.settings.ia.account";
    private static final String METADATA_USER_HANDLE = "user_handle";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Activity mActivity;
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
    public void testCategory_isAccountDetail() {
        assertThat(new AccountDetailDashboardFragment().getCategoryKey())
                .isEqualTo(CategoryKey.CATEGORY_ACCOUNT_DETAIL);
    }

    @Test
    public void refreshDashboardTiles_HasAccountType_shouldDisplay() {
        final Tile tile = new Tile();
        final Bundle metaData = new Bundle();
        metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        metaData.putString(METADATA_ACCOUNT_TYPE, "com.abc");
        tile.metaData = metaData;

        assertThat(mFragment.displayTile(tile)).isTrue();
    }

    @Test
    public void refreshDashboardTiles_NoAccountType_shouldNotDisplay() {
        final Tile tile = new Tile();
        final Bundle metaData = new Bundle();
        metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        tile.metaData = metaData;

        assertThat(mFragment.displayTile(tile)).isFalse();
    }

    @Test
    public void refreshDashboardTiles_OtherAccountType_shouldNotDisplay() {
        final Tile tile = new Tile();
        final Bundle metaData = new Bundle();
        metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT_DETAIL);
        metaData.putString(METADATA_ACCOUNT_TYPE, "com.other");
        tile.metaData = metaData;

        assertThat(mFragment.displayTile(tile)).isFalse();
    }

    @Test
    public void refreshDashboardTiles_HasAccountType_shouldAddAccountNameToIntent() {
        FakeFeatureFactory.setupForTest(mActivity);
        final FakeFeatureFactory featureFactory =
                (FakeFeatureFactory) FakeFeatureFactory.getFactory(mActivity);
        final DashboardFeatureProviderImpl dashboardFeatureProvider =
                new DashboardFeatureProviderImpl(mActivity);
        final PackageManager packageManager = mock(PackageManager.class);
        ReflectionHelpers.setField(dashboardFeatureProvider, "mPackageManager", packageManager);
        when(packageManager.resolveActivity(any(Intent.class), anyInt()))
            .thenReturn(mock(ResolveInfo.class));

        final Tile tile = new Tile();
        tile.key = "key";
        tile.metaData = new Bundle();
        tile.metaData.putString(METADATA_CATEGORY, CategoryKey.CATEGORY_ACCOUNT);
        tile.metaData.putString(METADATA_ACCOUNT_TYPE, "com.abc");
        tile.metaData.putString("com.android.settings.intent.action", Intent.ACTION_ASSIST);
        tile.intent = new Intent();
        tile.userHandle = null;
        mFragment.displayTile(tile);

        final Activity activity = Robolectric.buildActivity(Activity.class).get();
        final Preference preference = new Preference(mContext);
        dashboardFeatureProvider.bindPreferenceToTile(activity,
                MetricsProto.MetricsEvent.DASHBOARD_SUMMARY, preference, tile, "key",
                Preference.DEFAULT_ORDER);

        preference.performClick();

        final Intent intent = shadowOf(activity).getNextStartedActivityForResult().intent;

        assertThat(intent.getStringExtra("extra.accountName"))
                .isEqualTo("name1@abc.com");
    }
}

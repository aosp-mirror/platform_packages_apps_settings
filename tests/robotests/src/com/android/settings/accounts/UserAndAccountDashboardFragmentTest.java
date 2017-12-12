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

import android.app.Activity;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UserAndAccountDashboardFragmentTest {

    private static final String METADATA_CATEGORY = "com.android.settings.category";
    private static final String METADATA_ACCOUNT_TYPE = "com.android.settings.ia.account";

    @Mock
    private UserManager mUserManager;
    private UserAndAccountDashboardFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new UserAndAccountDashboardFragment();
    }

    @Test
    public void testCategory_isAccount() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_ACCOUNT);
    }

    @Test
    public void updateSummary_shouldDisplaySignedInUser() {
        final Activity activity = mock(Activity.class);
        final SummaryLoader loader = mock(SummaryLoader.class);
        final UserInfo userInfo = new UserInfo();
        userInfo.name = "test_name";

        when(activity.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.getUserInfo(anyInt())).thenReturn(userInfo);

        final SummaryLoader.SummaryProvider provider = mFragment.SUMMARY_PROVIDER_FACTORY
                .createSummaryProvider(activity, loader);
        provider.setListening(true);

        verify(activity).getString(R.string.users_and_accounts_summary,
                userInfo.name);
    }

    @Test
    public void testSearchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                UserAndAccountDashboardFragment.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        ShadowApplication.getInstance().getApplicationContext(),
                        true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }
}

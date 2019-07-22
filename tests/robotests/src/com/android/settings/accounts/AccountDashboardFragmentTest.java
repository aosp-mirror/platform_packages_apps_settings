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

import static android.app.ActivityManager.LOCK_TASK_MODE_NONE;
import static android.app.ActivityManager.LOCK_TASK_MODE_PINNED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivityManager;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AccountDashboardFragmentTest {

    private AccountDashboardFragment mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        mFragment = new AccountDashboardFragment();
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testCategory_isAccount() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_ACCOUNT);
    }

    @Test
    public void testSearchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                AccountDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(RuntimeEnvironment.application, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    public void isLockTaskModePinned_disableLockTaskMode_shouldReturnFalse() {
        final AccountDashboardFragment fragment = spy(mFragment);
        doReturn(mContext).when(fragment).getContext();
        final ShadowActivityManager activityManager =
                Shadow.extract(mContext.getSystemService(ActivityManager.class));
        activityManager.setLockTaskModeState(LOCK_TASK_MODE_NONE);

        assertThat(fragment.isLockTaskModePinned()).isFalse();
    }

    @Test
    public void isLockTaskModePinned_hasTaskPinned_shouldReturnTrue() {
        final AccountDashboardFragment fragment = spy(mFragment);
        doReturn(mContext).when(fragment).getContext();
        final ShadowActivityManager activityManager =
                Shadow.extract(mContext.getSystemService(ActivityManager.class));
        activityManager.setLockTaskModeState(LOCK_TASK_MODE_PINNED);

        assertThat(fragment.isLockTaskModePinned()).isTrue();
    }
}

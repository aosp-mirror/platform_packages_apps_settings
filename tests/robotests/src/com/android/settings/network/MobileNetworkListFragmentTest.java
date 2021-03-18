/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import com.android.settings.search.BaseSearchIndexProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;


@RunWith(RobolectricTestRunner.class)
public class MobileNetworkListFragmentTest {
    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;

    private MobileNetworkListFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new MobileNetworkListFragment();
    }

    @Test
    public void isPageSearchEnabled_adminUser_shouldReturnTrue() {
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.isAdminUser()).thenReturn(true);
        final BaseSearchIndexProvider provider =
                (BaseSearchIndexProvider) mFragment.SEARCH_INDEX_DATA_PROVIDER;

        final Object obj = ReflectionHelpers.callInstanceMethod(provider, "isPageSearchEnabled",
                ReflectionHelpers.ClassParameter.from(Context.class, mContext));
        final boolean isEnabled = (Boolean) obj;

        assertThat(isEnabled).isTrue();
    }

    @Test
    public void isPageSearchEnabled_nonAdminUser_shouldReturnFalse() {
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.isAdminUser()).thenReturn(false);
        final BaseSearchIndexProvider provider =
                (BaseSearchIndexProvider) mFragment.SEARCH_INDEX_DATA_PROVIDER;

        final Object obj = ReflectionHelpers.callInstanceMethod(provider, "isPageSearchEnabled",
                ReflectionHelpers.ClassParameter.from(Context.class, mContext));
        final boolean isEnabled = (Boolean) obj;

        assertThat(isEnabled).isFalse();
    }
}

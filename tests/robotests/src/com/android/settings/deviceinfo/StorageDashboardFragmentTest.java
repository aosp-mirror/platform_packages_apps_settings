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

package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.provider.SearchIndexableResource;
import android.util.SparseArray;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.deviceinfo.storage.CachedStorageValuesHelper;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class StorageDashboardFragmentTest {

    private StorageDashboardFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new StorageDashboardFragment();
    }

    @Test
    public void testCategory_isConnectedDevice() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_STORAGE);
    }

    @Test
    public void test_initializeOptionsMenuInvalidatesExistingMenu() {
        Activity activity = mock(Activity.class);

        mFragment.initializeOptionsMenu(activity);

        verify(activity).invalidateOptionsMenu();
    }

    @Test
    public void test_cacheProviderProvidesValuesIfBothCached() {
        CachedStorageValuesHelper helper = mock(CachedStorageValuesHelper.class);
        PrivateStorageInfo info = new PrivateStorageInfo(0, 0);
        when(helper.getCachedPrivateStorageInfo()).thenReturn(info);
        SparseArray<StorageAsyncLoader.AppsStorageResult> result = new SparseArray<>();
        when(helper.getCachedAppsStorageResult()).thenReturn(result);

        mFragment.setCachedStorageValuesHelper(helper);
        mFragment.initializeCachedValues();

        assertThat(mFragment.getPrivateStorageInfo()).isEqualTo(info);
        assertThat(mFragment.getAppsStorageResult()).isEqualTo(result);
    }

    @Test
    public void test_cacheProviderDoesntProvideValuesIfAppsMissing() {
        CachedStorageValuesHelper helper = mock(CachedStorageValuesHelper.class);
        PrivateStorageInfo info = new PrivateStorageInfo(0, 0);
        when(helper.getCachedPrivateStorageInfo()).thenReturn(info);

        mFragment.setCachedStorageValuesHelper(helper);
        mFragment.initializeCachedValues();

        assertThat(mFragment.getPrivateStorageInfo()).isNull();
        assertThat(mFragment.getAppsStorageResult()).isNull();
    }

    @Test
    public void test_cacheProviderDoesntProvideValuesIfVolumeInfoMissing() {
        CachedStorageValuesHelper helper = mock(CachedStorageValuesHelper.class);
        SparseArray<StorageAsyncLoader.AppsStorageResult> result = new SparseArray<>();
        when(helper.getCachedAppsStorageResult()).thenReturn(result);

        mFragment.setCachedStorageValuesHelper(helper);
        mFragment.initializeCachedValues();

        assertThat(mFragment.getPrivateStorageInfo()).isNull();
        assertThat(mFragment.getAppsStorageResult()).isNull();
    }

    @Test
    public void test_loadWhenQuotaOffIfVolumeInfoNotLoaded() {
        View fakeView = mock(View.class, RETURNS_DEEP_STUBS);
        RecyclerView fakeRecyclerView = mock(RecyclerView.class, RETURNS_DEEP_STUBS);
        when(fakeView.findViewById(anyInt())).thenReturn(fakeView);
        mFragment = spy(mFragment);
        when(mFragment.getView()).thenReturn(fakeView);
        when(mFragment.getListView()).thenReturn(fakeRecyclerView);

        mFragment.maybeSetLoading(false);

        verify(mFragment).setLoading(true, false);
    }

    @Test
    public void test_dontLoadWhenQuotaOffIfVolumeInfoNotLoaded() {
        View fakeView = mock(View.class, RETURNS_DEEP_STUBS);
        RecyclerView fakeRecyclerView = mock(RecyclerView.class, RETURNS_DEEP_STUBS);
        when(fakeView.findViewById(anyInt())).thenReturn(fakeView);
        mFragment = spy(mFragment);
        when(mFragment.getView()).thenReturn(fakeView);
        when(mFragment.getListView()).thenReturn(fakeRecyclerView);

        PrivateStorageInfo info = new PrivateStorageInfo(0, 0);
        mFragment.setPrivateStorageInfo(info);

        mFragment.maybeSetLoading(false);

        verify(mFragment, never()).setLoading(true, false);
    }

    @Test
    public void test_loadWhenQuotaOnAndVolumeInfoLoadedButAppsMissing() {
        View fakeView = mock(View.class, RETURNS_DEEP_STUBS);
        RecyclerView fakeRecyclerView = mock(RecyclerView.class, RETURNS_DEEP_STUBS);
        when(fakeView.findViewById(anyInt())).thenReturn(fakeView);
        mFragment = spy(mFragment);
        when(mFragment.getView()).thenReturn(fakeView);
        when(mFragment.getListView()).thenReturn(fakeRecyclerView);

        PrivateStorageInfo info = new PrivateStorageInfo(0, 0);
        mFragment.setPrivateStorageInfo(info);

        mFragment.maybeSetLoading(true);

        verify(mFragment).setLoading(true, false);
    }

    @Test
    public void test_loadWhenQuotaOnAndAppsLoadedButVolumeInfoMissing() {
        View fakeView = mock(View.class, RETURNS_DEEP_STUBS);
        RecyclerView fakeRecyclerView = mock(RecyclerView.class, RETURNS_DEEP_STUBS);
        when(fakeView.findViewById(anyInt())).thenReturn(fakeView);
        mFragment = spy(mFragment);
        when(mFragment.getView()).thenReturn(fakeView);
        when(mFragment.getListView()).thenReturn(fakeRecyclerView);
        mFragment.setAppsStorageResult(new SparseArray<>());

        mFragment.maybeSetLoading(true);

        verify(mFragment).setLoading(true, false);
    }

    @Test
    public void test_dontLoadWhenQuotaOnAndAllLoaded() {
        View fakeView = mock(View.class, RETURNS_DEEP_STUBS);
        RecyclerView fakeRecyclerView = mock(RecyclerView.class, RETURNS_DEEP_STUBS);
        when(fakeView.findViewById(anyInt())).thenReturn(fakeView);
        mFragment = spy(mFragment);
        when(mFragment.getView()).thenReturn(fakeView);
        when(mFragment.getListView()).thenReturn(fakeRecyclerView);

        mFragment.setAppsStorageResult(new SparseArray<>());
        PrivateStorageInfo storageInfo = new PrivateStorageInfo(0, 0);
        mFragment.setPrivateStorageInfo(storageInfo);

        mFragment.maybeSetLoading(true);

        verify(mFragment, never()).setLoading(true, false);
    }

    @Test
    public void testSearchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                StorageDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                    .getXmlResourcesToIndex(RuntimeEnvironment.application, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }
}
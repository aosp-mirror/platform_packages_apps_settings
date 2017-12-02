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

package com.android.settings.search;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE;
import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.fail;

import static org.mockito.Mockito.spy;

import android.database.Cursor;
import android.text.TextUtils;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wifi.WifiSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class SearchIndexableResourcesTest {

    Set<Class> sProviderClassCopy;

    @Before
    public void setUp() {
        sProviderClassCopy = new HashSet<>(SearchIndexableResources.sProviders);
    }

    @After
    public void cleanUp() {
        SearchIndexableResources.sProviders.clear();
        SearchIndexableResources.sProviders.addAll(sProviderClassCopy);
    }

    @Test
    public void testAddIndex() {
        final Class stringClass = java.lang.String.class;
        // Confirms that String.class isn't contained in SearchIndexableResources.
        assertThat(SearchIndexableResources.sProviders).doesNotContain(stringClass);
        final int beforeCount = SearchIndexableResources.providerValues().size();

        SearchIndexableResources.addIndex(java.lang.String.class);

        assertThat(SearchIndexableResources.sProviders).contains(stringClass);
        final int afterCount = SearchIndexableResources.providerValues().size();
        assertThat(afterCount).isEqualTo(beforeCount + 1);
    }

    @Test
    public void testIndexHasWifiSettings() {
        assertThat(sProviderClassCopy).contains(WifiSettings.class);
    }

    @Test
    public void testNonIndexableKeys_GetsKeyFromProvider() {
        SearchIndexableResources.sProviders.clear();
        SearchIndexableResources.addIndex(FakeIndexProvider.class);

        SettingsSearchIndexablesProvider provider = spy(new SettingsSearchIndexablesProvider());

        Cursor cursor = provider.queryNonIndexableKeys(null);
        boolean hasTestKey = false;
        while (cursor.moveToNext()) {
            String key = cursor.getString(COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE);
            if (TextUtils.equals(key, FakeIndexProvider.KEY)) {
                hasTestKey = true;
                break;
            }
        }

        assertThat(hasTestKey).isTrue();
    }

    @Test
    public void testAllClassNamesHaveProviders() {
        for (Class clazz: sProviderClassCopy) {
            if(DatabaseIndexingUtils.getSearchIndexProvider(clazz) == null) {
                fail(clazz.getName() + "is not an index provider");
            }
        }
    }
}

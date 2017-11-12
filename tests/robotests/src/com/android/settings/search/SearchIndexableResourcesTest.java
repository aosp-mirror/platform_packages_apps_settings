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

    private Set<Class> mProviderClassCopy;

    @Before
    public void setUp() {
        mProviderClassCopy = new HashSet<>(SettingsSearchIndexablesProvider.INDEXABLES);
    }

    @After
    public void cleanUp() {
        SettingsSearchIndexablesProvider.INDEXABLES.clear();
        SettingsSearchIndexablesProvider.INDEXABLES.addAll(mProviderClassCopy);
    }

    @Test
    public void testAddIndex() {
        final Class stringClass = java.lang.String.class;
        // Confirms that String.class isn't contained in SearchIndexableResources.
        assertThat(SettingsSearchIndexablesProvider.INDEXABLES).doesNotContain(stringClass);
        final int beforeCount = SettingsSearchIndexablesProvider.INDEXABLES.size();

        SettingsSearchIndexablesProvider.addIndex(java.lang.String.class);

        assertThat(SettingsSearchIndexablesProvider.INDEXABLES).contains(stringClass);
        final int afterCount = SettingsSearchIndexablesProvider.INDEXABLES.size();
        assertThat(afterCount).isEqualTo(beforeCount + 1);
    }

    @Test
    public void testIndexHasWifiSettings() {
        assertThat(mProviderClassCopy).contains(WifiSettings.class);
    }

    @Test
    public void testNonIndexableKeys_GetsKeyFromProvider() {
        SettingsSearchIndexablesProvider.INDEXABLES.clear();
        SettingsSearchIndexablesProvider.addIndex(FakeIndexProvider.class);

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
        for (Class clazz : mProviderClassCopy) {
            if (DatabaseIndexingUtils.getSearchIndexProvider(clazz) == null) {
                fail(clazz.getName() + "is not an index provider");
            }
        }
    }
}

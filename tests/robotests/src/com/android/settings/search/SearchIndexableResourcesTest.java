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
import static com.android.settings.search.SearchIndexableResources.NO_DATA_RES_ID;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;

import android.annotation.DrawableRes;
import android.annotation.XmlRes;
import android.database.Cursor;
import android.provider.SearchIndexableResource;

import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.wifi.WifiSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchIndexableResourcesTest {

    @XmlRes
    private static final int XML_RES_ID = R.xml.physical_keyboard_settings;
    @DrawableRes
    private static final int ICON_RES_ID = R.drawable.ic_settings_language;

    Map<String, SearchIndexableResource> sResMapCopy;

    @Before
    public void setUp() {
        sResMapCopy = new HashMap<>(SearchIndexableResources.sResMap);
    }

    @After
    public void cleanUp() {
        SearchIndexableResources.sResMap.clear();
        for (String key : sResMapCopy.keySet()) {
            SearchIndexableResources.sResMap.put(key, sResMapCopy.get(key));
        }
    }

    @Test
    public void testAddIndex() {
        // Confirms that String.class isn't contained in SearchIndexableResources.
        assertThat(SearchIndexableResources.getResourceByName("java.lang.String")).isNull();
        final int beforeCount = SearchIndexableResources.values().size();

        SearchIndexableResources.addIndex(java.lang.String.class, XML_RES_ID, ICON_RES_ID);
        final SearchIndexableResource index = SearchIndexableResources
                .getResourceByName("java.lang.String");

        assertThat(index).isNotNull();
        assertThat(index.className).isEqualTo("java.lang.String");
        assertThat(index.xmlResId).isEqualTo(XML_RES_ID);
        assertThat(index.iconResId).isEqualTo(ICON_RES_ID);
        final int afterCount = SearchIndexableResources.values().size();
        assertThat(afterCount).isEqualTo(beforeCount + 1);
    }

    @Test
    public void testIndexHasWifiSettings() {
        final SearchIndexableResource index = SearchIndexableResources
                .getResourceByName(WifiSettings.class.getName());

        assertThat(index).isNotNull();
        assertThat(index.className).isEqualTo(WifiSettings.class.getName());
        assertThat(index.xmlResId).isEqualTo(NO_DATA_RES_ID);
        assertThat(index.iconResId).isEqualTo(R.drawable.ic_settings_wireless);
    }

    @Test
    public void testNonIndexableKeys_GetsKeyFromProvider() {
        SearchIndexableResources.sResMap.clear();
        SearchIndexableResources.addIndex(FakeIndexProvider.class, 0, 0);

        SettingsSearchIndexablesProvider provider = spy(new SettingsSearchIndexablesProvider());

        Cursor cursor = provider.queryNonIndexableKeys(null);
        boolean hasTestKey = false;
        while(cursor.moveToNext()) {
            String key = cursor.getString(COLUMN_INDEX_NON_INDEXABLE_KEYS_KEY_VALUE);
            if (TextUtils.equals(key, FakeIndexProvider.KEY)) {
                hasTestKey = true;
                break;
            }
        }

        assertThat(hasTestKey).isTrue();
    }
}

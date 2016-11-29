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

import static com.android.settings.search.SearchIndexableResources.NO_DATA_RES_ID;

import static com.google.common.truth.Truth.assertThat;

import android.annotation.DrawableRes;
import android.annotation.XmlRes;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.wifi.WifiSettings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SearchIndexableResourcesTest {

    @XmlRes
    private static final int XML_RES_ID = R.xml.physical_keyboard_settings;
    @DrawableRes
    private static final int ICON_RES_ID = R.drawable.ic_settings_language;

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
        assertThat(index.rank).isEqualTo(Ranking.RANK_OTHERS);
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
        assertThat(index.rank).isEqualTo(Ranking.RANK_WIFI);
        assertThat(index.xmlResId).isEqualTo(NO_DATA_RES_ID);
        assertThat(index.iconResId).isEqualTo(R.drawable.ic_settings_wireless);
    }
}

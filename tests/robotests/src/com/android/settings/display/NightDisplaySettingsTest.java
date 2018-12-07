/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class NightDisplaySettingsTest {

    @Test
    public void testNightDisplayIndexing_containsResource() {
        List<SearchIndexableResource> resources =
                NightDisplaySettings.SEARCH_INDEX_DATA_PROVIDER
                    .getXmlResourcesToIndex(RuntimeEnvironment.application, true /* enabled */);

        List<Integer> indexedXml = new ArrayList<>();
        for (SearchIndexableResource resource : resources) {
            indexedXml.add(resource.xmlResId);
        }

        assertThat(indexedXml).contains(R.xml.night_display_settings);
    }

    @Test
    public void getCategoryKey_isCategoryNightDisplay() {
        NightDisplaySettings settings = new NightDisplaySettings();
        assertThat(settings.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_NIGHT_DISPLAY);
    }
}

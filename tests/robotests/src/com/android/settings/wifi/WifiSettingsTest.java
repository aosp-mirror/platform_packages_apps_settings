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
 * limitations under the License
 */
package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;

import android.content.Context;

import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class WifiSettingsTest {

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
    }

    @Test
    public void testSearchIndexProvider_shouldIndexFragmentTitle() {
        final List<SearchIndexableRaw> indexRes =
            WifiSettings.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(mContext, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).key).isEqualTo(WifiSettings.DATA_KEY_REFERENCE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testSearchIndexProvider_ifWifiSettingsNotVisible_shouldNotIndexFragmentTitle() {
        final List<SearchIndexableRaw> indexRes =
            WifiSettings.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(mContext, true /* enabled */);

        assertThat(indexRes).isEmpty();
    }
}
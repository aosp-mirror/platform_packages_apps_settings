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

package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ScanningSettingsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void searchProvider_shouldIndexDefaultXml() {
        final List<SearchIndexableResource> sir = ScanningSettings.SEARCH_INDEX_DATA_PROVIDER
                .getXmlResourcesToIndex(mContext, true /* enabled */);

        assertThat(sir).hasSize(1);
        assertThat(sir.get(0).xmlResId).isEqualTo(R.xml.location_scanning);
    }
}

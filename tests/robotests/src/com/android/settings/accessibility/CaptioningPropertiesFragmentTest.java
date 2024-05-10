/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Tests for {@link CaptioningPropertiesFragment}. */
@RunWith(RobolectricTestRunner.class)
public class CaptioningPropertiesFragmentTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CaptioningPropertiesFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new CaptioningPropertiesFragment();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_CAPTION_PROPERTIES);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.captioning_settings);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("CaptioningPropertiesFragment");
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = CaptioningPropertiesFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.captioning_settings);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }
}

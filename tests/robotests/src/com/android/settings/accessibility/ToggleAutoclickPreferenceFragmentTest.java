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

/** Tests for {@link ToggleAutoclickPreferenceFragment}. */
@RunWith(RobolectricTestRunner.class)
public class ToggleAutoclickPreferenceFragmentTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ToggleAutoclickPreferenceFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new ToggleAutoclickPreferenceFragment();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_autoclick_settings);
    }

    @Test
    public void getHelpResource_returnsCorrectHelpResource() {
        assertThat(mFragment.getHelpResource()).isEqualTo(R.string.help_url_autoclick);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("AutoclickPrefFragment");
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = ToggleAutoclickPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_autoclick_settings);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }
}

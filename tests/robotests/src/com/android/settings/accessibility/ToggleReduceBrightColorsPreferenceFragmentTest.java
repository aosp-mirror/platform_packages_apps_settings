/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.settings.accessibility.ToggleReduceBrightColorsPreferenceFragment.KEY_SHORTCUT;
import static com.android.settings.accessibility.ToggleReduceBrightColorsPreferenceFragment.KEY_SWITCH;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link ToggleReduceBrightColorsPreferenceFragment} */
@RunWith(RobolectricTestRunner.class)
public class ToggleReduceBrightColorsPreferenceFragmentTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getRawDataToIndex_flagOn_returnPreferencesCreatedInCodes() {
        String[] expectedKeys = {KEY_SHORTCUT, KEY_SWITCH};
        String[] expectedTitles = {
                mContext.getString(R.string.reduce_bright_colors_shortcut_title),
                mContext.getString(R.string.reduce_bright_colors_switch_title)};
        List<String> keysResultList = new ArrayList<>();
        List<String> titlesResultList = new ArrayList<>();
        List<SearchIndexableRaw> rawData = ToggleReduceBrightColorsPreferenceFragment
                .SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(mContext, /* enabled= */ true);

        for (SearchIndexableRaw rawDataItem : rawData) {
            keysResultList.add(rawDataItem.key);
            titlesResultList.add(rawDataItem.title);
        }

        // Verify that `getRawDataToIndex` includes the preferences created in codes
        assertThat(keysResultList).containsAtLeastElementsIn(expectedKeys);
        assertThat(titlesResultList).containsAtLeastElementsIn(expectedTitles);
    }
}

/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.view.accessibility.Flags.FLAG_FORCE_INVERT_COLOR;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Tests for {@link ColorAndMotionFragment}. */
@RunWith(RobolectricTestRunner.class)
public class ColorAndMotionFragmentTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ColorAndMotionFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new ColorAndMotionFragment();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_COLOR_AND_MOTION);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_color_and_motion);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("ColorAndMotionFragment");
    }

    @Test
    @RequiresFlagsEnabled(FLAG_FORCE_INVERT_COLOR)
    public void forceInvertEnabled_getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = ColorAndMotionFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_color_and_motion);

        assertThat(niks).doesNotContain(ColorAndMotionFragment.TOGGLE_FORCE_INVERT);
        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_FORCE_INVERT_COLOR)
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = ColorAndMotionFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_color_and_motion);

        assertThat(niks).contains(ColorAndMotionFragment.TOGGLE_FORCE_INVERT);
        assertThat(keys).containsAtLeastElementsIn(niks);
    }
}

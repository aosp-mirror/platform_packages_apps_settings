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
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Tests for {@link CaptioningMoreOptionsFragment}. */
@RunWith(RobolectricTestRunner.class)
public class CaptioningMoreOptionsFragmentTest {
    // Language/locale preference key, from captioning_more_options.xml
    private static final String CAPTIONING_LOCALE_KEY = "captioning_locale";

    @Rule
    public final SetFlagsRule mSetFlagRule = new SetFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CaptioningMoreOptionsFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new CaptioningMoreOptionsFragment();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_CAPTION_MORE_OPTIONS);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.captioning_more_options);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("CaptioningMoreOptionsFragment");
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = CaptioningMoreOptionsFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext)
                .stream().filter(Objects::nonNull).collect(Collectors.toList());
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext, R.xml.captioning_more_options);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getNonIndexableKeys_captioningEnabled_localeIsSearchable() {
        setCaptioningEnabled(true);

        final List<String> niks = CaptioningMoreOptionsFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        // Not in NonIndexableKeys == searchable
        assertThat(niks).doesNotContain(CAPTIONING_LOCALE_KEY);
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getNonIndexableKeys_captioningDisabled_localeIsNotSearchable() {
        setCaptioningEnabled(false);

        final List<String> niks = CaptioningMoreOptionsFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        // In NonIndexableKeys == not searchable
        assertThat(niks).contains(CAPTIONING_LOCALE_KEY);
    }

    private void setCaptioningEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, enabled ? 1 : 0);
    }
}

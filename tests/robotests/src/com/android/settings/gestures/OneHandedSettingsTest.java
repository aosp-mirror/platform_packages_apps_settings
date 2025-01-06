/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.android.settings.gestures.OneHandedSettings.ONE_HANDED_SHORTCUT_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.SystemProperties;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.SearchIndexableResource;

import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

/** Tests for {@link OneHandedSettings}. */
@RunWith(RobolectricTestRunner.class)
public class OneHandedSettingsTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private OneHandedSettings mSettings;

    @Before
    public void setUp() {
        mSettings = spy(new OneHandedSettings());
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mSettings.getLogTag()).isEqualTo("OneHandedSettings");
    }

    @Test
    public void testSearchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                OneHandedSettings.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        RuntimeEnvironment.application, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mSettings.getPreferenceScreenResId());
    }

    @Test
    public void isPageSearchEnabled_setSupportOneHandedModeProperty_shouldReturnTrue() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "true");

        final Object obj = ReflectionHelpers.callInstanceMethod(
                OneHandedSettings.SEARCH_INDEX_DATA_PROVIDER, "isPageSearchEnabled",
                ReflectionHelpers.ClassParameter.from(Context.class, mContext));
        final boolean isEnabled = (Boolean) obj;
        assertThat(isEnabled).isTrue();
    }

    @Test
    public void isPageSearchEnabled_unsetSupportOneHandedModeProperty_shouldReturnFalse() {
        SystemProperties.set(OneHandedSettingsUtils.SUPPORT_ONE_HANDED_MODE, "false");

        final Object obj = ReflectionHelpers.callInstanceMethod(
                OneHandedSettings.SEARCH_INDEX_DATA_PROVIDER, "isPageSearchEnabled",
                ReflectionHelpers.ClassParameter.from(Context.class, mContext));
        final boolean isEnabled = (Boolean) obj;
        assertThat(isEnabled).isFalse();
    }

    @Test
    @DisableFlags(com.android.settings.accessibility.Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getRawDataToIndex_flagDisabled_isEmpty() {
        final List<SearchIndexableRaw> rawData = OneHandedSettings
                .SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(mContext, true);
        final List<String> actualSearchKeys = rawData.stream().map(raw -> raw.key).toList();

        assertThat(actualSearchKeys).isEmpty();
    }

    @Test
    @EnableFlags(com.android.settings.accessibility.Flags.FLAG_FIX_A11Y_SETTINGS_SEARCH)
    public void getRawDataToIndex_returnsOnlyShortcutKey() {
        final List<SearchIndexableRaw> rawData = OneHandedSettings
                .SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(mContext, true);
        final List<String> actualSearchKeys = rawData.stream().map(raw -> raw.key).toList();

        assertThat(actualSearchKeys).containsExactly(ONE_HANDED_SHORTCUT_KEY);
    }

    @Test
    public void getNonIndexableKeys_containsNonSearchableElements() {
        final List<String> niks = OneHandedSettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        assertThat(niks).containsExactly(
                "gesture_one_handed_mode_intro",
                "one_handed_header",
                "one_handed_mode_footer");
    }
}

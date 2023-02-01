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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.QuickSettingsTooltipType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

/** Tests for {@link OneHandedSettings}. */
@RunWith(RobolectricTestRunner.class)
public class OneHandedSettingsTest {

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private OneHandedSettings mSettings;

    @Before
    public void setUp() {
        mSettings = spy(new OneHandedSettings());
    }

    @Test
    public void getTileTooltipContent_returnsExpectedValues() {
        // Simulate to call getTileTooltipContent after onDetach
        assertThat(mSettings.getTileTooltipContent(QuickSettingsTooltipType.GUIDE_TO_EDIT))
                .isNull();
        // Simulate to call getTileTooltipContent after onAttach
        when(mSettings.getContext()).thenReturn(mContext);
        assertThat(mSettings.getTileTooltipContent(QuickSettingsTooltipType.GUIDE_TO_EDIT))
                .isEqualTo(mContext.getText(
                        R.string.accessibility_one_handed_mode_qs_tooltip_content));
        assertThat(mSettings.getTileTooltipContent(QuickSettingsTooltipType.GUIDE_TO_DIRECT_USE))
                .isEqualTo(mContext.getText(
                        R.string.accessibility_one_handed_mode_auto_added_qs_tooltip_content));
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
}

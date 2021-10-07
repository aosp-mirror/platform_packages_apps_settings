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

import android.content.Context;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class OneHandedSettingsTest {

    private OneHandedSettings mSettings;
    private Context mContext;

    @Before
    public void setUp() {
        mSettings = new OneHandedSettings();
        mContext = RuntimeEnvironment.application;
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

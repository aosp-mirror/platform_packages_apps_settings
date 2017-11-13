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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AssistGestureSettingsTest {
    @Mock
    private Context mContext;
    private FakeFeatureFactory mFakeFeatureFactory;
    private AssistGestureSettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFakeFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mSettings = new AssistGestureSettings();
    }

    @Test
    public void testGetPreferenceScreenResId() {
        assertThat(mSettings.getPreferenceScreenResId())
                .isEqualTo(R.xml.assist_gesture_settings);
    }

    @Test
    public void testSearchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                AssistGestureSettings.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        ShadowApplication.getInstance().getApplicationContext(),
                        true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mSettings.getPreferenceScreenResId());
    }

    @Test
    public void testSearchIndexProvider_noSensor_shouldDisablePageSearch() {
        when(mFakeFeatureFactory.assistGestureFeatureProvider.isSensorAvailable(any(Context.class)))
                .thenReturn(false);

        assertThat(AssistGestureSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                RuntimeEnvironment.application))
                .contains("gesture_assist_settings_page");
    }
}


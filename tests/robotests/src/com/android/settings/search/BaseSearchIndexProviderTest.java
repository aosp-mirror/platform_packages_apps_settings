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

package com.android.settings.search;


import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.PreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BaseSearchIndexProviderTest {

    private static final String TEST_PREF_KEY = "test_pref_key";

    @Mock
    private Context mContext;
    private BaseSearchIndexProvider mIndexProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mIndexProvider = spy(BaseSearchIndexProvider.class);
    }

    @Test
    public void getNonIndexableKeys_noPreferenceController_shouldReturnEmptyList() {
        assertThat(mIndexProvider.getNonIndexableKeys(mContext)).isEqualTo(Collections.EMPTY_LIST);
    }

    @Test
    public void getNonIndexableKeys_preferenceIsAvailable_shouldReturnEmptyList() {
        List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new PreferenceController(mContext) {
            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public String getPreferenceKey() {
                return TEST_PREF_KEY;
            }
        });
        doReturn(controllers).when(mIndexProvider).getPreferenceControllers(mContext);

        assertThat(mIndexProvider.getNonIndexableKeys(mContext)).isEqualTo(Collections.EMPTY_LIST);
    }

    @Test
    public void getNonIndexableKeys_preferenceIsNotAvailable_shouldReturnKey() {
        List<PreferenceController> controllers = new ArrayList<>();
        controllers.add(new PreferenceController(mContext) {
            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public String getPreferenceKey() {
                return TEST_PREF_KEY;
            }
        });
        doReturn(controllers).when(mIndexProvider).getPreferenceControllers(mContext);

        assertThat(mIndexProvider.getNonIndexableKeys(mContext)).contains(TEST_PREF_KEY);
    }

    @Test
    public void getNonIndexableKeys_pageSearchIsDisabled_shouldSuppressEverything() {
        final BaseSearchIndexProvider provider = new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                    boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.data_usage;
                return Arrays.asList(sir);
            }

            @Override
            protected boolean isPageSearchEnabled(Context context) {
                return false;
            }
        };

        final List<String> nonIndexableKeys = provider
                .getNonIndexableKeys(RuntimeEnvironment.application);

        assertThat(nonIndexableKeys).containsAllOf("status_header", "limit_summary",
                "restrict_background");
    }
}

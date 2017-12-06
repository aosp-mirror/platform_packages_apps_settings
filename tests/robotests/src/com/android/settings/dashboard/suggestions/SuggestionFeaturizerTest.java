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
 * limitations under the License
 */

package com.android.settings.dashboard.suggestions;

import java.util.Arrays;
import java.util.Map;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SuggestionFeaturizerTest {

    private EventStore mEventStore;
    private SuggestionFeaturizer mSuggestionFeaturizer;

    @Before
    public void setUp() {
        mEventStore = new EventStore(RuntimeEnvironment.application);
        mSuggestionFeaturizer = new SuggestionFeaturizer(mEventStore);
    }

    @Test
    public void testFeaturize_singlePackage() {
        mEventStore.writeEvent("pkg", EventStore.EVENT_DISMISSED);
        mEventStore.writeEvent("pkg", EventStore.EVENT_SHOWN);
        mEventStore.writeEvent("pkg", EventStore.EVENT_SHOWN);
        Map<String, Double> features = mSuggestionFeaturizer.featurize(Arrays.asList("pkg"))
                .get("pkg");
        assertThat(features.get(SuggestionFeaturizer.FEATURE_IS_SHOWN)).isEqualTo(1.0);
        assertThat(features.get(SuggestionFeaturizer.FEATURE_IS_DISMISSED)).isEqualTo(1.0);
        assertThat(features.get(SuggestionFeaturizer.FEATURE_IS_CLICKED)).isEqualTo(0.0);

        assertThat(features.get(SuggestionFeaturizer.FEATURE_TIME_FROM_LAST_SHOWN)).isLessThan(1.0);
        assertThat(features.get(SuggestionFeaturizer.FEATURE_TIME_FROM_LAST_DISMISSED))
                .isLessThan(1.0);
        assertThat(features.get(SuggestionFeaturizer.FEATURE_TIME_FROM_LAST_CLICKED))
                .isEqualTo(1.0);
        assertThat(features.get(SuggestionFeaturizer.FEATURE_SHOWN_COUNT))
                .isEqualTo(2.0 / SuggestionFeaturizer.COUNT_NORMALIZATION_FACTOR);
        assertThat(features.get(SuggestionFeaturizer.FEATURE_DISMISSED_COUNT))
                .isEqualTo(1.0 / SuggestionFeaturizer.COUNT_NORMALIZATION_FACTOR);
        assertThat(features.get(SuggestionFeaturizer.FEATURE_CLICKED_COUNT)).isEqualTo(0.0);
    }

    @Test
    public void testFeaturize_multiplePackages() {
        mEventStore.writeEvent("pkg1", EventStore.EVENT_DISMISSED);
        mEventStore.writeEvent("pkg2", EventStore.EVENT_SHOWN);
        mEventStore.writeEvent("pkg1", EventStore.EVENT_SHOWN);
        Map<String, Map<String, Double>> features = mSuggestionFeaturizer
                .featurize(Arrays.asList("pkg1", "pkg2"));
        Map<String, Double> features1 = features.get("pkg1");
        Map<String, Double> features2 = features.get("pkg2");

        assertThat(features1.get(SuggestionFeaturizer.FEATURE_IS_SHOWN)).isEqualTo(1.0);
        assertThat(features1.get(SuggestionFeaturizer.FEATURE_IS_DISMISSED)).isEqualTo(1.0);
        assertThat(features1.get(SuggestionFeaturizer.FEATURE_IS_CLICKED)).isEqualTo(0.0);
        assertThat(features1.get(SuggestionFeaturizer.FEATURE_TIME_FROM_LAST_SHOWN))
                .isLessThan(1.0);
        assertThat(features1.get(SuggestionFeaturizer.FEATURE_TIME_FROM_LAST_DISMISSED))
                .isLessThan(1.0);
        assertThat(features1.get(SuggestionFeaturizer.FEATURE_TIME_FROM_LAST_CLICKED))
                .isEqualTo(1.0);
        assertThat(features1.get(SuggestionFeaturizer.FEATURE_SHOWN_COUNT))
                .isEqualTo(1.0 / SuggestionFeaturizer.COUNT_NORMALIZATION_FACTOR);
        assertThat(features1.get(SuggestionFeaturizer.FEATURE_DISMISSED_COUNT))
                .isEqualTo(1.0 / SuggestionFeaturizer.COUNT_NORMALIZATION_FACTOR);
        assertThat(features1.get(SuggestionFeaturizer.FEATURE_CLICKED_COUNT)).isEqualTo(0.0);

        assertThat(features2.get(SuggestionFeaturizer.FEATURE_IS_SHOWN)).isEqualTo(1.0);
        assertThat(features2.get(SuggestionFeaturizer.FEATURE_IS_DISMISSED)).isEqualTo(0.0);
        assertThat(features2.get(SuggestionFeaturizer.FEATURE_IS_CLICKED)).isEqualTo(0.0);
        assertThat(features2.get(SuggestionFeaturizer.FEATURE_TIME_FROM_LAST_SHOWN))
                .isLessThan(1.0);
        assertThat(features2.get(SuggestionFeaturizer.FEATURE_TIME_FROM_LAST_DISMISSED))
                .isEqualTo(1.0);
        assertThat(features2.get(SuggestionFeaturizer.FEATURE_TIME_FROM_LAST_CLICKED))
                .isEqualTo(1.0);
        assertThat(features2.get(SuggestionFeaturizer.FEATURE_SHOWN_COUNT))
                .isEqualTo(1.0 / SuggestionFeaturizer.COUNT_NORMALIZATION_FACTOR);
        assertThat(features2.get(SuggestionFeaturizer.FEATURE_DISMISSED_COUNT)).isEqualTo(0.0);
        assertThat(features2.get(SuggestionFeaturizer.FEATURE_CLICKED_COUNT)).isEqualTo(0.0);
    }
}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.spy;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SuggestionRankerTest {

    @Mock
    private SuggestionRanker mSuggestionRanker;
    @Mock
    private SuggestionFeaturizer mSuggestionFeaturizer;
    private Map<String, Map<String, Double>> mFeatures;
    private List<String> mPkgNames;
    private List<Tile> mSuggestions;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPkgNames = Arrays.asList("pkg1", "pkg2", "pkg3");
        mFeatures = new HashMap<String, Map<String, Double>>();
        mFeatures.put("pkg1", new HashMap<String, Double>());
        mFeatures.put("pkg2", new HashMap<String, Double>());
        mFeatures.put("pkg3", new HashMap<String, Double>());
        mSuggestions = new ArrayList<Tile>() {
            {
                add(new Tile());
                add(new Tile());
                add(new Tile());
            }
        };
        mSuggestionFeaturizer = mock(SuggestionFeaturizer.class);
        mSuggestionRanker = new SuggestionRanker(mSuggestionFeaturizer);
        when(mSuggestionFeaturizer.featurize(mPkgNames)).thenReturn(mFeatures);
        mSuggestionRanker = spy(mSuggestionRanker);
        when(mSuggestionRanker.getRelevanceMetric(same(mFeatures.get("pkg1")))).thenReturn(0.9);
        when(mSuggestionRanker.getRelevanceMetric(same(mFeatures.get("pkg2")))).thenReturn(0.1);
        when(mSuggestionRanker.getRelevanceMetric(same(mFeatures.get("pkg3")))).thenReturn(0.5);
    }

    @Test
    public void testRank() {
        List<Tile> expectedOrderdList = new ArrayList<Tile>() {
            {
                add(mSuggestions.get(0)); // relevance = 0.9
                add(mSuggestions.get(2)); // relevance = 0.5
                add(mSuggestions.get(1)); // relevance = 0.1
            }
        };
        mSuggestionRanker.rankSuggestions(mSuggestions, mPkgNames);
        assertThat(mSuggestions).isEqualTo(expectedOrderdList);
    }
}


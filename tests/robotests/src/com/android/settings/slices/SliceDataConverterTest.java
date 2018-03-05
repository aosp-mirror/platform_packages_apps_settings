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

package com.android.settings.slices;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.android.settings.search.FakeIndexProvider;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class SliceDataConverterTest {

    private final String fakeKey = "key";
    private final String fakeTitle = "title";
    private final String fakeSummary = "summary";
    private final String fakeScreenTitle = "screen_title";
    private final String fakeFragmentClassName = FakeIndexProvider.class.getName();
    private final String fakeControllerName = FakePreferenceController.class.getName();

    private SliceDataConverter mSliceDataConverter;
    private SearchFeatureProvider mSearchFeatureProvider;
    private FakeFeatureFactory mFakeFeatureFactory;

    @Before
    public void setUp() {
        mSliceDataConverter = new SliceDataConverter(RuntimeEnvironment.application);
        mSearchFeatureProvider = new SearchFeatureProviderImpl();
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFakeFeatureFactory.searchFeatureProvider = mSearchFeatureProvider;
    }

    @After
    public void cleanUp() {
        mFakeFeatureFactory.searchFeatureProvider = mock(SearchFeatureProvider.class);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testFakeProvider_convertsFakeData() {
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues().clear();
        mSearchFeatureProvider.getSearchIndexableResources().getProviderValues()
                .add(FakeIndexProvider.class);

        List<SliceData> sliceDataList = mSliceDataConverter.getSliceData();

        assertThat(sliceDataList).hasSize(1);
        SliceData fakeSlice = sliceDataList.get(0);

        assertThat(fakeSlice.getKey()).isEqualTo(fakeKey);
        assertThat(fakeSlice.getTitle()).isEqualTo(fakeTitle);
        assertThat(fakeSlice.getSummary()).isEqualTo(fakeSummary);
        assertThat(fakeSlice.getScreenTitle()).isEqualTo(fakeScreenTitle);
        assertThat(fakeSlice.getIconResource()).isNotNull();
        assertThat(fakeSlice.getUri()).isNull();
        assertThat(fakeSlice.getFragmentClassName()).isEqualTo(fakeFragmentClassName);
        assertThat(fakeSlice.getPreferenceController()).isEqualTo(fakeControllerName);
        assertThat(fakeSlice.getSliceType()).isEqualTo(SliceData.SliceType.SLIDER);
    }
}
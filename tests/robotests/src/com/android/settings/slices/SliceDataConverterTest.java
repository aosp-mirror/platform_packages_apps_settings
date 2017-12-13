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

import android.content.Context;

import com.android.settings.TestConfig;
import com.android.settings.search.FakeIndexProvider;
import com.android.settings.search.SearchIndexableResources;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SliceDataConverterTest {

    private final String fakeKey = "key";
    private final String fakeTitle = "title";
    private final String fakeSummary = "summary";
    private final String fakeScreenTitle = "screen_title";
    private final String fakeFragmentClassName = FakeIndexProvider.class.getName();
    private final String fakeControllerName = FakePreferenceController.class.getName();

    Context mContext;

    private Set<Class> mProviderClassesCopy;

    SliceDataConverter mSliceDataConverter;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mProviderClassesCopy = new HashSet<>(SearchIndexableResources.providerValues());
        mSliceDataConverter = new SliceDataConverter(mContext);
    }

    @After
    public void cleanUp() {
        SearchIndexableResources.providerValues().clear();
        SearchIndexableResources.providerValues().addAll(mProviderClassesCopy);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testFakeProvider_convertsFakeData() {
        SearchIndexableResources.providerValues().clear();
        SearchIndexableResources.providerValues().add(FakeIndexProvider.class);

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
    }
}
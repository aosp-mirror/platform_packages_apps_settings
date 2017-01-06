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

package com.android.settings.dashboard;

import android.app.Activity;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DashboardSummaryTest {

    @Mock
    private DashboardAdapter mAdapter;
    @Mock
    private DashboardFeatureProvider mDashboardFeatureProvider;

    private DashboardSummary mSummary;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mSummary = spy(new DashboardSummary());
        ReflectionHelpers.setField(mSummary, "mAdapter", mAdapter);
        ReflectionHelpers.setField(mSummary, "mDashboardFeatureProvider",
                mDashboardFeatureProvider);
    }

    @Test
    public void updateCategoryAndSuggestion_shouldGetCategoryFromFeatureProvider() {
        doReturn(mock(Activity.class)).when(mSummary).getActivity();
        when(mDashboardFeatureProvider.isEnabled()).thenReturn(true);
        mSummary.updateCategoryAndSuggestion(null);
        verify(mDashboardFeatureProvider).getTilesForCategory(CategoryKey.CATEGORY_HOMEPAGE);
    }
}

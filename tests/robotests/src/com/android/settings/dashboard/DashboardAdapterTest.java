/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.view.View;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.conditional.Condition;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DashboardAdapterTest {
    @Mock
    private Context mContext;
    @Mock
    private View mView;
    @Mock
    private Condition mCondition;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    @Mock
    private TypedArray mTypedArray;
    @Captor
    private ArgumentCaptor<Integer> mActionCategoryCaptor = ArgumentCaptor.forClass(Integer.class);
    @Captor
    private ArgumentCaptor<String> mActionPackageCaptor = ArgumentCaptor.forClass(String.class);
    private DashboardAdapter mDashboardAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mDashboardAdapter = new DashboardAdapter(mContext, null, mMetricsFeatureProvider,
                null, null);
        when(mView.getTag()).thenReturn(mCondition);
    }

    @Test
    public void testSetConditions_AfterSetConditions_ExpandedConditionNull() {
        mDashboardAdapter.onExpandClick(mView);
        assertThat(mDashboardAdapter.mDashboardData.getExpandedCondition()).isEqualTo(mCondition);
        mDashboardAdapter.setConditions(null);
        assertThat(mDashboardAdapter.mDashboardData.getExpandedCondition()).isNull();
    }

    @Test
    public void testSuggestionsLogs() {
        when(mTypedArray.getColor(any(int.class), any(int.class))).thenReturn(0);
        when(mContext.obtainStyledAttributes(any(int[].class))).thenReturn(mTypedArray);
        List<Tile> suggestions = new ArrayList<Tile>();
        suggestions.add(makeSuggestion("pkg1", "cls1"));
        suggestions.add(makeSuggestion("pkg2", "cls2"));
        suggestions.add(makeSuggestion("pkg3", "cls3"));
        mDashboardAdapter.setCategoriesAndSuggestions(
                new ArrayList<DashboardCategory>(), suggestions);
        mDashboardAdapter.onPause();
        verify(mMetricsFeatureProvider, times(4)).action(
             any(Context.class), mActionCategoryCaptor.capture(), mActionPackageCaptor.capture());
        String[] expectedPackages = new String[] {"pkg1", "pkg2", "pkg1", "pkg2"};
        Integer[] expectedActions = new Integer[] {
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION,
                MetricsEvent.ACTION_HIDE_SETTINGS_SUGGESTION};
        assertThat(mActionPackageCaptor.getAllValues().toArray()).isEqualTo(expectedPackages);
        assertThat(mActionCategoryCaptor.getAllValues().toArray()).isEqualTo(expectedActions);
    }

    private Tile makeSuggestion(String pkgName, String className) {
        Tile suggestion = new Tile();
        suggestion.intent = new Intent("action");
        suggestion.intent.setComponent(new ComponentName(pkgName, className));
        return suggestion;
    }

}

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


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.testutils.FakeFeatureFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DashboardDividerDecorationTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private Drawable mDrawable;
    @Mock
    private Canvas mCanvas;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RecyclerView mRecyclerView;
    @Mock
    private PreferenceGroupAdapter mAdapter;
    @Mock
    private Preference pref1;
    @Mock
    private Preference pref2;
    private DashboardDividerDecoration mDecoration;
    private FakeFeatureFactory mFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFactory = (FakeFeatureFactory) FeatureFactory.getFactory(mContext);
        mDecoration = new DashboardDividerDecoration(mContext);
        mDecoration.setDivider(mDrawable);
        mDecoration.setDividerHeight(3);
    }

    @Test
    public void drawOver_differentPriorityGroup_shouldDrawDivider() {
        when(mRecyclerView.getAdapter()).thenReturn(mAdapter);
        when(mRecyclerView.getChildCount()).thenReturn(2);
        when(mRecyclerView.getChildAdapterPosition(any(View.class)))
                .thenReturn(0)
                .thenReturn(1);
        when(mAdapter.getItem(0)).thenReturn(pref1);
        when(mAdapter.getItem(1)).thenReturn(pref2);
        when(mFactory.dashboardFeatureProvider.getPriorityGroup(pref1)).thenReturn(1);
        when(mFactory.dashboardFeatureProvider.getPriorityGroup(pref2)).thenReturn(2);

        mDecoration.onDrawOver(mCanvas, mRecyclerView, null /* state */);

        verify(mDrawable).draw(mCanvas);
    }


    @Test
    public void drawOver_samePriorityGroup_doNotDrawDivider() {
        when(mRecyclerView.getAdapter()).thenReturn(mAdapter);
        when(mRecyclerView.getChildCount()).thenReturn(2);
        when(mRecyclerView.getChildAdapterPosition(any(View.class)))
                .thenReturn(0)
                .thenReturn(1);
        when(mAdapter.getItem(0)).thenReturn(pref1);
        when(mAdapter.getItem(1)).thenReturn(pref2);

        mDecoration.onDrawOver(mCanvas, mRecyclerView, null /* state */);

        verify(mDrawable, never()).draw(mCanvas);
    }
}

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

package com.android.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroupAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PreferenceDividerDecorationTest {

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
    private PreferenceCategory mPrefCategory;
    @Mock
    private Preference mNormalPref;
    @Mock
    private FooterPreference mFooterPref;
    private PreferenceDividerDecoration mDecoration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mDecoration = new PreferenceDividerDecoration();
        mDecoration.setDivider(mDrawable);
        mDecoration.setDividerHeight(3);
    }

    @Test
    public void drawOver_footerPreference_shouldDrawDivider() {
        when(mRecyclerView.getAdapter()).thenReturn(mAdapter);
        when(mRecyclerView.getChildCount()).thenReturn(1);
        when(mAdapter.getItem(anyInt())).thenReturn(mFooterPref);

        mDecoration.onDrawOver(mCanvas, mRecyclerView, null /* state */);

        verify(mDrawable).draw(mCanvas);
    }

    @Test
    public void drawOver_preferenceCategory_shouldSkipFirst() {
        when(mRecyclerView.getAdapter()).thenReturn(mAdapter);
        when(mRecyclerView.getChildCount()).thenReturn(3);
        when(mAdapter.getItem(anyInt())).thenReturn(mPrefCategory);
        when(mRecyclerView.getChildAdapterPosition(any(View.class)))
                .thenReturn(0)
                .thenReturn(1)
                .thenReturn(2);

        mDecoration.onDrawOver(mCanvas, mRecyclerView, null /* state */);

        // 3 prefCategory, should skip first draw
        verify(mDrawable, times(2)).draw(mCanvas);
    }

    @Test
    public void drawOver_preference_doNotDrawDivider() {
        when(mRecyclerView.getAdapter()).thenReturn(mAdapter);
        when(mRecyclerView.getChildCount()).thenReturn(1);
        when(mAdapter.getItem(anyInt())).thenReturn(mNormalPref);

        mDecoration.onDrawOver(mCanvas, mRecyclerView, null /* state */);

        verify(mDrawable, never()).draw(mCanvas);
    }
}

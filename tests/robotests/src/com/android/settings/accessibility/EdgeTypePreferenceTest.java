/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.widget.SubtitleView;
import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link EdgeTypePreference}. */
@RunWith(RobolectricTestRunner.class)
public class EdgeTypePreferenceTest {

    private Context mContext;
    private View mRootView;
    private TextView mSummaryView;
    private SubtitleView mSubtitleView;
    private EdgeTypePreference mEdgeTypePreference;

    @Before
    public void init() {
        mContext = ApplicationProvider.getApplicationContext();
        mRootView = spy(new View(mContext));
        mSummaryView = spy(new TextView(mContext));
        mSubtitleView = spy(new SubtitleView(mContext));

        final AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        mEdgeTypePreference = spy(new EdgeTypePreference(mContext, attributeSet));
        doReturn(mSubtitleView).when(mRootView).findViewById(R.id.preview);
        doReturn(mSummaryView).when(mRootView).findViewById(R.id.summary);
    }

    @Test
    public void shouldDisableDependents_edgeTypeNone_returnTrue() {
        mEdgeTypePreference.setValue(CaptionStyle.EDGE_TYPE_NONE);
        final boolean shouldDisableDependents =
                mEdgeTypePreference.shouldDisableDependents();

        assertThat(shouldDisableDependents).isTrue();
    }

    @Test
    public void onBindListItem_initSubtitleView() {
        final int testIndex = 0;
        mEdgeTypePreference.onBindListItem(mRootView, testIndex);
        final float density = mContext.getResources().getDisplayMetrics().density;
        final int value = mEdgeTypePreference.getValueAt(testIndex);

        verify(mSubtitleView).setForegroundColor(Color.WHITE);
        verify(mSubtitleView).setBackgroundColor(Color.TRANSPARENT);
        verify(mSubtitleView).setTextSize(32f * density);
        verify(mSubtitleView).setEdgeType(value);
        verify(mSubtitleView).setEdgeColor(Color.BLACK);
    }

    @Test
    public void onBindListItem_setSummary() {
        final int testIndex = 0;
        mEdgeTypePreference.onBindListItem(mRootView, testIndex);
        final CharSequence title = mEdgeTypePreference.getTitleAt(testIndex);

        verify(mSummaryView).setText(title);
    }
}

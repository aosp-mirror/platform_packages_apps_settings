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
package com.android.settings.widget;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FixedLineSummaryPreferenceTest {

    @Mock
    private TextView mSummary;

    private Context mContext;
    private PreferenceViewHolder mHolder;
    private FixedLineSummaryPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new FixedLineSummaryPreference(mContext, null);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view =
            inflater.inflate(mPreference.getLayoutResource(), new LinearLayout(mContext), false);
        mHolder = spy(PreferenceViewHolder.createInstanceForTests(view));
        when(mHolder.findViewById(android.R.id.summary)).thenReturn(mSummary);
    }

    @Test
    public void onBindViewHolder_shouldSetSingleLine() {
        mPreference.onBindViewHolder(mHolder);

        verify(mSummary).setMinLines(1);
        verify(mSummary).setMaxLines(1);
    }

    @Test
    public void onBindViewHolder_TwoLineSummary_shouldSetTwoLines() {
        mPreference.setSummaryLineCount(2);
        mPreference.onBindViewHolder(mHolder);

        verify(mSummary).setMinLines(2);
        verify(mSummary).setMaxLines(2);
    }
}

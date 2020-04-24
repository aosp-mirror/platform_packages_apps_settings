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

package com.android.settings.widget;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LinkifySummaryPreferenceTest {
    @Spy
    private PreferenceViewHolder mViewHolder;
    @Mock
    private TextView mSummaryTextView;
    private LinkifySummaryPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final Context context = RuntimeEnvironment.application;
        mPreference = new LinkifySummaryPreference(context, null /* attrs */);

        final View view = spy(View.inflate(context, mPreference.getLayoutResource(),
                null /* root */));
        mViewHolder = spy(PreferenceViewHolder.createInstanceForTests(view));
        doReturn(mSummaryTextView).when(mViewHolder).findViewById(android.R.id.summary);
    }

    @Test
    public void onBindViewHolder_summaryTextViewGone_shouldNotSetMovementMethod() {
        when(mSummaryTextView.getVisibility()).thenReturn(View.GONE);

        mPreference.onBindViewHolder(mViewHolder);

        verify(mSummaryTextView, never()).setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Test
    public void onBindViewHolder_noLinkSummary_shouldNotSetMovementMethod() {
        when(mSummaryTextView.getVisibility()).thenReturn(View.VISIBLE);
        final CharSequence seondSummary = "secondSummary";
        mPreference.setSummary(seondSummary);

        mPreference.onBindViewHolder(mViewHolder);

        verify(mSummaryTextView, never()).setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Test
    public void onBindViewHolder_linkedSummary_shouldSetMovementMethod() {
        when(mSummaryTextView.getVisibility()).thenReturn(View.VISIBLE);
        final CharSequence seondSummary = "secondSummary";
        final SpannableStringBuilder summaryBuilder = new SpannableStringBuilder();
        summaryBuilder.append(seondSummary, new URLSpan("" /* url */),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        mPreference.setSummary(summaryBuilder);

        mPreference.onBindViewHolder(mViewHolder);

        verify(mSummaryTextView).setMovementMethod(any(LinkMovementMethod.class));
    }
}

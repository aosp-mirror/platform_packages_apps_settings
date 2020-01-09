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
package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LinkifyUtilsTest {
    private static final String TEST_STRING = "to LINK_BEGINscanning settingsLINK_END.";
    private static final String WRONG_STRING = "to scanning settingsLINK_END.";
    private final LinkifyUtils.OnClickListener mClickListener = () -> { /* Do nothing */ };

    private StringBuilder mSpanStringBuilder;
    private StringBuilder mWrongSpanStringBuilder;
    TextView mTextView;

    @Before
    public void setUp() throws Exception {
        mSpanStringBuilder = new StringBuilder(TEST_STRING);
        mWrongSpanStringBuilder = new StringBuilder(WRONG_STRING);
        mTextView = new TextView(RuntimeEnvironment.application);
    }

    @Test
    public void linkify_whenSpanStringCorrect_shouldReturnTrue() {
        final boolean linkifyResult = LinkifyUtils.linkify(mTextView, mSpanStringBuilder,
                mClickListener);

        assertThat(linkifyResult).isTrue();
    }

    @Test
    public void linkify_whenSpanStringWrong_shouldReturnFalse() {
        final boolean linkifyResult = LinkifyUtils.linkify(mTextView, mWrongSpanStringBuilder,
                mClickListener);

        assertThat(linkifyResult).isFalse();
    }

    @Test
    public void linkify_whenSpanStringCorrect_shouldContainClickableSpan() {
        LinkifyUtils.linkify(mTextView, mSpanStringBuilder, mClickListener);
        final Spannable spannableContent = (Spannable) mTextView.getText();
        final int len = spannableContent.length();
        final Object[] spans = spannableContent.getSpans(0, len, Object.class);

        assertThat(spans[1] instanceof ClickableSpan).isTrue();
    }
}

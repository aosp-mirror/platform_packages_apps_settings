/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class ExpandDividerPreferenceTest {

    private Context mContext;
    private ExpandDividerPreference mExpandDividerPreference;

    private ImageView mImageView;
    private TextView mTextView;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mImageView = spy(new ImageView(mContext));
        mTextView = spy(new TextView(mContext));
        mExpandDividerPreference = new ExpandDividerPreference(mContext);
        doReturn(R.id.expand_title).when(mTextView).getId();
        doReturn(R.id.expand_icon).when(mImageView).getId();
    }

    @Test
    public void testConstructor_returnExpectedResult() {
        assertThat(mExpandDividerPreference.getKey())
                .isEqualTo(ExpandDividerPreference.PREFERENCE_KEY);
        assertThat(mExpandDividerPreference.getLayoutResource())
                .isEqualTo(R.layout.preference_expand_divider);
    }

    @Test
    public void testSetTitle_setTitleContentIntoTextView() {
        final String titleContent = "title content";
        mExpandDividerPreference.mTextView = mTextView;

        mExpandDividerPreference.setTitle(titleContent);

        verify(mTextView).setText(titleContent);
    }

    @Test
    public void testOnClick_switchExpandStateAndInvokeCallback() {
        final boolean[] isExpandedArray = new boolean[]{false};
        mExpandDividerPreference.mImageView = mImageView;
        mExpandDividerPreference.setOnExpandListener(
                isExpanded -> isExpandedArray[0] = isExpanded);

        // Click the item first time from false -> true.
        mExpandDividerPreference.onClick();
        // Verifies the first time click result.
        verify(mImageView).setImageResource(R.drawable.ic_settings_expand_less);
        assertThat(isExpandedArray[0]).isTrue();

        // Clicks the item second time from true -> false.
        mExpandDividerPreference.onClick();
        // Verifies the second time click result.
        verify(mImageView).setImageResource(R.drawable.ic_settings_expand_more);
        assertThat(isExpandedArray[0]).isFalse();
    }

    @Test
    public void testSetIsExpanded_updateStateButNotInvokeCallback() {
        final boolean[] isExpandedArray = new boolean[]{false};
        mExpandDividerPreference.mImageView = mImageView;
        mExpandDividerPreference.setOnExpandListener(
                isExpanded -> isExpandedArray[0] = isExpanded);

        mExpandDividerPreference.setIsExpanded(true);

        verify(mImageView).setImageResource(R.drawable.ic_settings_expand_less);
        assertThat(isExpandedArray[0]).isFalse();
    }
}

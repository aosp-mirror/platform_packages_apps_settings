/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.widget.RadioButtonPreferenceWithExtraWidget.EXTRA_WIDGET_VISIBILITY_GONE;
import static com.android.settings.widget.RadioButtonPreferenceWithExtraWidget.EXTRA_WIDGET_VISIBILITY_INFO;
import static com.android.settings.widget.RadioButtonPreferenceWithExtraWidget.EXTRA_WIDGET_VISIBILITY_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;

import android.app.Application;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RadioButtonPreferenceWithExtraWidgetTest {

    private Application mContext;
    private RadioButtonPreferenceWithExtraWidget mPreference;

    private TextView mSummary;
    private ImageView mExtraWidget;
    private View mExtraWidgetDivider;

    private boolean mIsClickListenerCalled = false;
    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mIsClickListenerCalled = true;
        }
    };

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new RadioButtonPreferenceWithExtraWidget(mContext);
        mPreference.setSummary("test summary");

        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.preference_radio_with_extra_widget, null);
        PreferenceViewHolder preferenceViewHolder =
                PreferenceViewHolder.createInstanceForTests(view);
        mPreference.onBindViewHolder(preferenceViewHolder);

        mSummary = view.findViewById(android.R.id.summary);
        mExtraWidget = view.findViewById(R.id.radio_extra_widget);
        mExtraWidgetDivider = view.findViewById(R.id.radio_extra_widget_divider);
    }

    @Test
    public void shouldHaveRadioPreferenceWithExtraWidgetLayout() {
        assertThat(mPreference.getLayoutResource())
                .isEqualTo(R.layout.preference_radio_with_extra_widget);
    }

    @Test
    public void iconSpaceReservedShouldBeFalse() {
        assertThat(mPreference.isIconSpaceReserved()).isFalse();
    }

    @Test
    public void summaryShouldBeVisible() {
        assertEquals(View.VISIBLE, mSummary.getVisibility());
    }

    @Test
    public void testSetExtraWidgetVisibility_gone() {
        mPreference.setExtraWidgetVisibility(EXTRA_WIDGET_VISIBILITY_GONE);
        assertEquals(View.GONE, mExtraWidget.getVisibility());
        assertEquals(View.GONE, mExtraWidgetDivider.getVisibility());
        assertThat(mExtraWidget.isClickable()).isFalse();
    }

    @Test
    public void testSetExtraWidgetVisibility_info() {
        mPreference.setExtraWidgetVisibility(EXTRA_WIDGET_VISIBILITY_INFO);
        assertEquals(View.VISIBLE, mExtraWidget.getVisibility());
        assertEquals(View.VISIBLE, mExtraWidgetDivider.getVisibility());
        assertThat(mExtraWidget.isClickable()).isTrue();
        assertEquals(mContext.getResources().getText(R.string.information_label),
                mExtraWidget.getContentDescription());
    }

    @Test
    public void testSetExtraWidgetVisibility_setting() {
        mPreference.setExtraWidgetVisibility(EXTRA_WIDGET_VISIBILITY_SETTING);
        assertEquals(View.VISIBLE, mExtraWidget.getVisibility());
        assertEquals(View.VISIBLE, mExtraWidgetDivider.getVisibility());
        assertThat(mExtraWidget.isClickable()).isTrue();
        assertEquals(mContext.getResources().getText(R.string.settings_label),
                mExtraWidget.getContentDescription());
    }

    @Test
    public void testSetExtraWidgetOnClickListener() {
        mPreference.setExtraWidgetOnClickListener(mClickListener);

        assertThat(mIsClickListenerCalled).isFalse();
        mExtraWidget.callOnClick();
        assertThat(mIsClickListenerCalled).isTrue();
    }

    @Test
    public void extraWidgetStaysEnabledWhenPreferenceIsDisabled() {
        mPreference.setEnabled(false);
        mExtraWidget.setEnabled(false);

        assertThat(mExtraWidget.isEnabled()).isFalse();
        mPreference.setExtraWidgetOnClickListener(mClickListener);
        assertThat(mExtraWidget.isEnabled()).isTrue();
    }
}

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
 * limitations under the License.
 */

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.preference.PreferenceViewHolder;

@RunWith(RobolectricTestRunner.class)
public class AddPreferenceTest {

    private Context mContext;
    private PreferenceViewHolder mViewHolder;
    private View mWidgetFrame;
    private View mAddWidget;
    private AddPreference mPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new AddPreference(mContext, null);

        final View view = spy(View.inflate(mContext, mPreference.getLayoutResource(), null));
        mViewHolder = PreferenceViewHolder.createInstanceForTests(view);
        mWidgetFrame = view.findViewById(android.R.id.widget_frame);
        mAddWidget = spy(View.inflate(mContext, mPreference.getSecondTargetResId(), null));
        when(mViewHolder.findViewById(mPreference.getAddWidgetResId())).thenReturn(mAddWidget);
    }

    @Test
    public void onBindViewHolder_noListener_addButtonNotVisible() {
        mPreference.onBindViewHolder(mViewHolder);
        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onBindViewHolder_hasListener_addButtonVisible() {
        mPreference.setOnAddClickListener(p -> {});
        mPreference.onBindViewHolder(mViewHolder);
        assertThat(mPreference.shouldHideSecondTarget()).isFalse();
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void setOnAddClickListener_listenerAddedAfterBinding_addButtonBecomesVisible() {
        mPreference.onBindViewHolder(mViewHolder);
        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.GONE);

        mPreference.setOnAddClickListener(p -> {});
        assertThat(mPreference.shouldHideSecondTarget()).isFalse();
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void setOnAddClickListener_listenerRemovedAfterBinding_addButtonNotVisible() {
        mPreference.setOnAddClickListener(p -> {});

        mPreference.onBindViewHolder(mViewHolder);
        assertThat(mPreference.shouldHideSecondTarget()).isFalse();
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.VISIBLE);

        mPreference.setOnAddClickListener(null);
        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void setOnAddClickListener_listenerAddedAndRemovedAfterBinding_addButtonNotVisible() {
        mPreference.onBindViewHolder(mViewHolder);
        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.GONE);

        mPreference.setOnAddClickListener(p -> {});
        assertThat(mPreference.shouldHideSecondTarget()).isFalse();
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.VISIBLE);

        mPreference.setOnAddClickListener(null);
        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onClick_noListener_noCrash() {
        mPreference.onBindViewHolder(mViewHolder);
        // should be no crash here
        mPreference.onClick(mAddWidget);
    }

    @Test
    public void onClick_hasListenerBeforeBind_firesCorrectly() {
        final AddPreference.OnAddClickListener listener = mock(
                AddPreference.OnAddClickListener.class);
        mPreference.setOnAddClickListener(listener);

        mPreference.onBindViewHolder(mViewHolder);
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.VISIBLE);

        mPreference.onClick(mAddWidget);
        verify(listener).onAddClick(eq(mPreference));
    }

    @Test
    public void onClick_listenerAddedAfterBind_firesCorrectly() {
        mPreference.onBindViewHolder(mViewHolder);
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.GONE);

        final AddPreference.OnAddClickListener listener = mock(
                AddPreference.OnAddClickListener.class);
        mPreference.setOnAddClickListener(listener);
        assertThat(mWidgetFrame.getVisibility()).isEqualTo(View.VISIBLE);

        mPreference.onClick(mAddWidget);
        verify(listener).onAddClick(eq(mPreference));
    }
}

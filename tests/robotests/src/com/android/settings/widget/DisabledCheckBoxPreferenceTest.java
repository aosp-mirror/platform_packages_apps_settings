/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DisabledCheckBoxPreferenceTest {

    private Context mContext;
    private View mRootView;
    private DisabledCheckBoxPreference mPref;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mRootView = View.inflate(mContext, R.layout.preference, null /* parent */);
        mHolder = PreferenceViewHolder.createInstanceForTests(mRootView);
        mPref = new DisabledCheckBoxPreference(mContext);
    }

    private void inflatePreference() {
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final LinearLayout widgetView = mHolder.itemView.findViewById(android.R.id.widget_frame);
        assertThat(widgetView).isNotNull();
        inflater.inflate(R.layout.preference_widget_checkbox, widgetView, true);
        mPref.onBindViewHolder(mHolder);
    }

    @Test
    public void onBindViewHolder_checkboxDisabled() {
        inflatePreference();
        assertThat(mRootView.findViewById(android.R.id.checkbox).isEnabled()).isFalse();
    }

    @Test
    public void checkboxOnClick_checkboxDisabled() {
        Preference.OnPreferenceClickListener onClick =
                mock(Preference.OnPreferenceClickListener.class);
        mPref.setOnPreferenceClickListener(onClick);
        inflatePreference();

        mPref.enableCheckbox(false);
        mPref.performClick(mRootView);

        verify(onClick, never()).onPreferenceClick(any());
    }

    @Test
    public void checkboxOnClick_checkboxEnabled() {
        Preference.OnPreferenceClickListener onClick =
                mock(Preference.OnPreferenceClickListener.class);
        mPref.setOnPreferenceClickListener(onClick);
        inflatePreference();

        mPref.enableCheckbox(true);
        mPref.performClick(mRootView);

        verify(onClick, times(1)).onPreferenceClick(any());
    }
}

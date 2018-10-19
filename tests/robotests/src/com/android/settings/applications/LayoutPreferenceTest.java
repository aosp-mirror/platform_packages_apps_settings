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
 * limitations under the License.
 */

package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceViewHolder;
import android.view.LayoutInflater;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class LayoutPreferenceTest {

    private Context mContext;
    private LayoutPreference mPreference;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new LayoutPreference(mContext, R.layout.two_action_buttons);
        mHolder = PreferenceViewHolder.createInstanceForTests(LayoutInflater.from(mContext)
                .inflate(R.layout.layout_preference_frame, null, false));
    }

    @Test
    public void setOnClickListener_shouldAttachToRootView() {
        final OnPreferenceClickListener listener = mock(OnPreferenceClickListener.class);

        mPreference.setOnPreferenceClickListener(listener);
        mPreference.onBindViewHolder(mHolder);

        mHolder.itemView.callOnClick();

        verify(listener).onPreferenceClick(mPreference);
        assertThat(mHolder.itemView.isFocusable()).isTrue();
        assertThat(mHolder.itemView.isClickable()).isTrue();
    }

    @Test
    public void setNonSelectable_viewShouldNotBeSelectable() {
        mPreference.setSelectable(false);
        mPreference.onBindViewHolder(mHolder);

        assertThat(mHolder.itemView.isFocusable()).isFalse();
        assertThat(mHolder.itemView.isClickable()).isFalse();
    }

    @Test
    public void disableSomeView_shouldMaintainStateAfterBind() {
        mPreference.findViewById(R.id.button1_positive).setEnabled(false);
        mPreference.findViewById(R.id.button2_positive).setEnabled(true);

        mPreference.onBindViewHolder(mHolder);

        assertThat(mPreference.findViewById(R.id.button1_positive).isEnabled()).isFalse();
        assertThat(mPreference.findViewById(R.id.button2_positive).isEnabled()).isTrue();
    }
}

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
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class LayoutPreferenceTest {

    private Context mContext;
    private LayoutPreference mPreference;
    private View mRootView;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new LayoutPreference(mContext, R.layout.app_action_buttons);
        mRootView = mPreference.mRootView;
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
        mPreference.findViewById(R.id.left_button).setEnabled(false);
        mPreference.findViewById(R.id.right_button).setEnabled(true);

        mPreference.onBindViewHolder(mHolder);

        assertThat(mPreference.findViewById(R.id.left_button).isEnabled()).isFalse();
        assertThat(mPreference.findViewById(R.id.right_button).isEnabled()).isTrue();
    }
}

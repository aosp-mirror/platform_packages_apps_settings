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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AppCheckBoxPreferenceTest {

    private Context mContext;
    private AppCheckBoxPreference mPreference;
    private AppCheckBoxPreference mAttrPreference;
    private PreferenceViewHolder mPreferenceViewHolder;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new AppCheckBoxPreference(mContext);
        mAttrPreference = new AppCheckBoxPreference(mContext, null /* attrs */);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(mContext)
                        .inflate(com.android.settingslib.widget.preference.app.R.layout.preference_app, null));
    }

    @Test
    public void testGetLayoutResource() {
        assertThat(mPreference.getLayoutResource())
                .isEqualTo(com.android.settingslib.widget.preference.app.R.layout.preference_app);
        assertThat(mAttrPreference.getLayoutResource())
                .isEqualTo(com.android.settingslib.widget.preference.app.R.layout.preference_app);
    }

    @Test
    public void onBindViewHolder_appendixGone() {
        mPreference.onBindViewHolder(mPreferenceViewHolder);

        View appendix =
                mPreferenceViewHolder.findViewById(com.android.settingslib.widget.preference.app.R.id.appendix);
        assertThat(appendix.getVisibility()).isEqualTo(View.GONE);
    }
}

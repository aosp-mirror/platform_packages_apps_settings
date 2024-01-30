/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.textservice.SpellCheckerInfo;

import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SpellCheckerPreferenceTest {

    private Context mContext;
    private PreferenceViewHolder mViewHolder;
    private View mDivider;
    private SpellCheckerPreference mPreference;
    private final SpellCheckerInfo[] mScis = new SpellCheckerInfo[]{};

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreference = new SpellCheckerPreference(mContext, mScis);
    }

    @Test
    public void onBindViewHolder_withIntent_DividerIsVisible() {
        final View view = spy(View.inflate(mContext, mPreference.getLayoutResource(), null));
        mViewHolder = PreferenceViewHolder.createInstanceForTests(view);
        mDivider = view.findViewById(
                com.android.settingslib.widget.preference.twotarget.R.id.two_target_divider);
        mPreference.mIntent = new Intent(Intent.ACTION_MAIN);

        mPreference.onBindViewHolder(mViewHolder);

        assertThat(mDivider.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void onBindViewHolder_withoutIntent_DividerIsNotExist() {
        final View view = spy(View.inflate(mContext, mPreference.getLayoutResource(), null));
        mViewHolder = PreferenceViewHolder.createInstanceForTests(view);
        mDivider = view.findViewById(
                com.android.settingslib.widget.preference.twotarget.R.id.two_target_divider);

        mPreference.onBindViewHolder(mViewHolder);

        assertThat(mDivider.getVisibility()).isEqualTo(View.GONE);
    }
}

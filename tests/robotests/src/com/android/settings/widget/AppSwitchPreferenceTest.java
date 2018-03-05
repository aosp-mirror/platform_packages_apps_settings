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

package com.android.settings.widget;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class AppSwitchPreferenceTest {

    private Context mContext;
    private View mRootView;
    private AppSwitchPreference mPref;
    private PreferenceViewHolder mHolder;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mRootView = View.inflate(mContext, R.layout.preference_app, null /* parent */);
        mHolder = PreferenceViewHolder.createInstanceForTests(mRootView);
        mPref = new AppSwitchPreference(mContext);
    }

    @Test
    public void setSummary_showSummaryContainer() {
        mPref.setSummary("test");
        mPref.onBindViewHolder(mHolder);

        assertThat(mHolder.findViewById(R.id.summary_container).getVisibility())
                .isEqualTo(View.VISIBLE);
    }

    @Test
    public void noSummary_hideSummaryContainer() {
        mPref.setSummary(null);
        mPref.onBindViewHolder(mHolder);

        assertThat(mHolder.findViewById(R.id.summary_container).getVisibility())
                .isEqualTo(View.GONE);
    }
}

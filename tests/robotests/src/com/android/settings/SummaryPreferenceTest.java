/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SummaryPreferenceTest {

    private PreferenceViewHolder mHolder;
    private SummaryPreference mPreference;

    @Before
    public void setUp() {
        final Context context = RuntimeEnvironment.application;
        mPreference = new SummaryPreference(context, null);

        LayoutInflater inflater = LayoutInflater.from(context);
        final View view =
            inflater.inflate(mPreference.getLayoutResource(), new LinearLayout(context), false);

        mHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @Test
    public void disableChart_shouldNotRender() {
        mPreference.setChartEnabled(false);
        mPreference.onBindViewHolder(mHolder);

        final TextView textView1 = (TextView) mHolder.findViewById(android.R.id.text1);
        assertThat(textView1.getText()).isEqualTo("");
        
        final TextView textView2 = (TextView) mHolder.findViewById(android.R.id.text2);
        assertThat(textView2.getText()).isEqualTo("");
    }

    @Test
    public void enableChart_shouldRender() {
        final String testLabel1 = "label1";
        final String testLabel2 = "label2";
        mPreference.setChartEnabled(true);
        mPreference.setLabels(testLabel1, testLabel2);
        mPreference.onBindViewHolder(mHolder);

        final TextView textView1 = (TextView) mHolder.findViewById(android.R.id.text1);
        assertThat(textView1.getText()).isEqualTo(testLabel1);

        final TextView textView2 = (TextView) mHolder.findViewById(android.R.id.text2);
        assertThat(textView2.getText()).isEqualTo(testLabel2);
    }
}

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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.R;
import com.android.settings.widget.LabeledSeekBarPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LabeledSeekBarPreferenceTest {

    private Context mContext;
    private PreferenceViewHolder mViewHolder;
    private SeekBar mSeekBar;
    private TextView mSummary;
    private LabeledSeekBarPreference mSeekBarPreference;

    @Mock
    private Preference.OnPreferenceChangeListener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mSeekBarPreference = new LabeledSeekBarPreference(mContext, null);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view =
                inflater.inflate(mSeekBarPreference.getLayoutResource(),
                        new LinearLayout(mContext), false);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(view);
        mSeekBar = (SeekBar) mViewHolder.findViewById(R.id.seekbar);
        mSummary = (TextView) mViewHolder.findViewById(R.id.summary);
    }

    @Test
    public void seekBarPreferenceOnStopTrackingTouch_callsListener() {
        mSeekBar.setProgress(2);

        mSeekBarPreference.setOnPreferenceChangeStopListener(mListener);
        mSeekBarPreference.onStopTrackingTouch(mSeekBar);

        verify(mListener, times(1)).onPreferenceChange(mSeekBarPreference, 2);
    }

    @Test
    public void seekBarPreferenceSummarySet_returnsValue() {
        final String summary = "this is a summary";
        mSeekBarPreference.setSummary(summary);
        mSeekBarPreference.onBindViewHolder(mViewHolder);

        assertThat(mSeekBarPreference.getSummary()).isEqualTo(summary);
        assertThat(mSummary.getText()).isEqualTo(summary);
    }

    @Test
    public void seekBarPreferenceSummaryNull_hidesView() {
        mSeekBarPreference.setSummary(null);
        mSeekBarPreference.onBindViewHolder(mViewHolder);

        assertThat(mSummary.getText()).isEqualTo("");
        assertThat(mSummary.getVisibility()).isEqualTo(View.GONE);
    }
}

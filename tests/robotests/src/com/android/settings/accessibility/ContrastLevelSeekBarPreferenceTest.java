/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.ContrastLevelSeekBarPreference.CONTRAST_SLIDER_TICKS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowInteractionJankMonitor.class})
public class ContrastLevelSeekBarPreferenceTest {

    private Context mContext;
    private AttributeSet mAttrs;
    private PreferenceViewHolder mHolder;
    private ContrastLevelSeekBar mSeekBar;
    private ContrastLevelSeekBarPreference mSeekBarPreference;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSeekBarPreference = new ContrastLevelSeekBarPreference(mContext, mAttrs);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view =
                inflater.inflate(mSeekBarPreference.getLayoutResource(),
                        new LinearLayout(mContext), false);
        mHolder = PreferenceViewHolder.createInstanceForTests(view);
        mSeekBar = view.findViewById(com.android.internal.R.id.seekbar);
    }

    @Test
    public void seekBarPreferenceOnBindViewHolder_shouldInitSeekBarValue() {
        mSeekBarPreference.onBindViewHolder(mHolder);

        assertThat(mSeekBar.getMax()).isEqualTo(CONTRAST_SLIDER_TICKS);
        assertThat(mSeekBar.getProgress()).isEqualTo(0);
    }
}

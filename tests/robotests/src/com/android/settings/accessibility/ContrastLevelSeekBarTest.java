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

import static android.view.HapticFeedbackConstants.CLOCK_TICK;
import static android.view.HapticFeedbackConstants.CONTEXT_CLICK;

import static com.android.settings.accessibility.ContrastLevelSeekBarPreference.CONTRAST_SLIDER_TICKS;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.SeekBar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ContrastLevelSeekBarTest {

    private Context mContext;
    private ContrastLevelSeekBar mSeekBar;
    private SeekBar.OnSeekBarChangeListener mProxySeekBarListener;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSeekBar = new ContrastLevelSeekBar(mContext, null);
        mProxySeekBarListener = shadowOf(mSeekBar).getOnSeekBarChangeListener();
    }

    @Test
    public void onProgressChanged_minimumValue_shouldModifyContrast() {
        // Assign the test value of SeekBar progress
        mProxySeekBarListener.onProgressChanged(mSeekBar, 0, true);

        assertThat(Settings.Secure.getFloatForUser(
                mContext.getContentResolver(), Settings.Secure.CONTRAST_LEVEL,
                -10f, UserHandle.USER_CURRENT)).isEqualTo(0);
    }

    @Test
    public void onProgressChanged_centerValue_shouldModifyContrast() {
        // Assign the test value of SeekBar progress
        mProxySeekBarListener.onProgressChanged(mSeekBar, CONTRAST_SLIDER_TICKS / 2, true);

        assertThat(Settings.Secure.getFloatForUser(
                mContext.getContentResolver(), Settings.Secure.CONTRAST_LEVEL,
                -10f, UserHandle.USER_CURRENT)).isWithin(1e-8f).of(0.5f);
    }

    @Test
    public void onProgressChanged_maximumValue_shouldModifyContrast() {
        // Assign the test value of SeekBar progress
        mProxySeekBarListener.onProgressChanged(mSeekBar, CONTRAST_SLIDER_TICKS, true);

        assertThat(Settings.Secure.getFloatForUser(
                mContext.getContentResolver(), Settings.Secure.CONTRAST_LEVEL,
                -10f, UserHandle.USER_CURRENT)).isEqualTo(1);
    }

    @Test
    public void onProgressChanged_minimumValue_clockTickFeedbackPerformed() {
        mSeekBar.performHapticFeedback(CONTEXT_CLICK);
        mProxySeekBarListener.onProgressChanged(mSeekBar, 0, true);

        assertThat(shadowOf(mSeekBar).lastHapticFeedbackPerformed()).isEqualTo(CLOCK_TICK);
    }

    @Test
    public void onProgressChanged_centerValue_clockTickFeedbackPerformed() {
        mSeekBar.performHapticFeedback(CONTEXT_CLICK);
        mProxySeekBarListener.onProgressChanged(mSeekBar, CONTRAST_SLIDER_TICKS / 2, true);

        assertThat(shadowOf(mSeekBar).lastHapticFeedbackPerformed()).isEqualTo(CLOCK_TICK);
    }

    @Test
    public void onProgressChanged_maximumValue_clockTickFeedbackPerformed() {
        mSeekBar.performHapticFeedback(CONTEXT_CLICK);
        mProxySeekBarListener.onProgressChanged(mSeekBar, CONTRAST_SLIDER_TICKS, true);

        assertThat(shadowOf(mSeekBar).lastHapticFeedbackPerformed()).isEqualTo(CLOCK_TICK);
    }
}

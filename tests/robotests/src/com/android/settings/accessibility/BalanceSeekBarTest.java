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

package com.android.settings.accessibility;

import static android.view.HapticFeedbackConstants.CLOCK_TICK;
import static android.view.HapticFeedbackConstants.CONTEXT_CLICK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.android.settings.testutils.shadow.ShadowSystemSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSystemSettings.class,
})
public class BalanceSeekBarTest {
    // Fix the maximum process value to 200 for testing the BalanceSeekBar.
    // It affects the SeekBar value of center(100) and snapThreshold(200 * SNAP_TO_PERCENTAGE).
    private static final int MAX_PROGRESS_VALUE = 200;

    private Context mContext;
    private AttributeSet mAttrs;
    private BalanceSeekBar mSeekBar;
    private BalanceSeekBar.OnSeekBarChangeListener mProxySeekBarListener;
    private SeekBar.OnSeekBarChangeListener mockSeekBarChangeListener;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mSeekBar = new BalanceSeekBar(mContext, mAttrs);
        mProxySeekBarListener = shadowOf(mSeekBar).getOnSeekBarChangeListener();
        mockSeekBarChangeListener = mock(SeekBar.OnSeekBarChangeListener.class);
        mSeekBar.setOnSeekBarChangeListener(mockSeekBarChangeListener);
    }

    @Test
    public void onStartTrackingTouch_shouldInvokeMethod() {
        mProxySeekBarListener.onStartTrackingTouch(mSeekBar);

        verify(mockSeekBarChangeListener, times(1)).onStartTrackingTouch(mSeekBar);
    }

    @Test
    public void onStopTrackingTouch_shouldInvokeMethod() {
        mProxySeekBarListener.onStopTrackingTouch(mSeekBar);

        verify(mockSeekBarChangeListener, times(1)).onStopTrackingTouch(mSeekBar);
    }

    @Test
    public void onProgressChanged_shouldInvokeMethod() {
        // Assign the test value of SeekBar progress
        mProxySeekBarListener.onProgressChanged(mSeekBar, MAX_PROGRESS_VALUE, true);

        verify(mockSeekBarChangeListener, times(1)).onProgressChanged(eq(mSeekBar),
                eq(MAX_PROGRESS_VALUE), eq(true));
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
        mProxySeekBarListener.onProgressChanged(mSeekBar, MAX_PROGRESS_VALUE / 2, true);

        assertThat(shadowOf(mSeekBar).lastHapticFeedbackPerformed()).isEqualTo(CLOCK_TICK);
    }

    @Test
    public void onProgressChanged_maximumValue_clockTickFeedbackPerformed() {
        mSeekBar.setMax(MAX_PROGRESS_VALUE);
        mSeekBar.performHapticFeedback(CONTEXT_CLICK);
        mProxySeekBarListener.onProgressChanged(mSeekBar, MAX_PROGRESS_VALUE, true);

        assertThat(shadowOf(mSeekBar).lastHapticFeedbackPerformed()).isEqualTo(CLOCK_TICK);
    }

    @Test
    public void setMaxTest_shouldSetValue() {
        mSeekBar.setMax(MAX_PROGRESS_VALUE);

        assertThat(getBalanceSeekBarCenter(mSeekBar)).isEqualTo(MAX_PROGRESS_VALUE / 2);
        assertThat(getBalanceSeekBarSnapThreshold(mSeekBar)).isEqualTo(
                MAX_PROGRESS_VALUE * BalanceSeekBar.SNAP_TO_PERCENTAGE);
    }

    @Test
    public void setProgressTest_shouldSnapToCenter() {
        // Assign the test value of SeekBar progress within the threshold (94-106 in this case).
        final int progressWithinThreshold = 102;
        mSeekBar.setMax(MAX_PROGRESS_VALUE);
        mSeekBar.setProgress(progressWithinThreshold + 10); //set progress which is over threshold.
        mProxySeekBarListener.onProgressChanged(mSeekBar, progressWithinThreshold, true);

        assertThat(mSeekBar.getProgress()).isEqualTo(getBalanceSeekBarCenter(mSeekBar));
    }

    @Test
    public void setProgressTest_shouldMaintainInputValue() {
        // Assign the test value of SeekBar progress without the threshold.
        final int progressWithoutThreshold = 107;
        mSeekBar.setMax(MAX_PROGRESS_VALUE);
        mSeekBar.setProgress(progressWithoutThreshold);
        mProxySeekBarListener.onProgressChanged(mSeekBar, progressWithoutThreshold, true);

        assertThat(mSeekBar.getProgress()).isEqualTo(progressWithoutThreshold);
    }

    // method to get the center from BalanceSeekBar for testing setMax().
    private int getBalanceSeekBarCenter(BalanceSeekBar seekBar) {
        return seekBar.getMax() / 2;
    }

    // method to get the snapThreshold from BalanceSeekBar for testing setMax().
    private float getBalanceSeekBarSnapThreshold(BalanceSeekBar seekBar) {
        return seekBar.getMax() * BalanceSeekBar.SNAP_TO_PERCENTAGE;
    }
}

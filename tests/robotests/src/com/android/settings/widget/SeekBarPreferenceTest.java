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

import static android.view.HapticFeedbackConstants.CLOCK_TICK;
import static android.view.HapticFeedbackConstants.CONTEXT_CLICK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.SeekBar;

import androidx.preference.PreferenceFragmentCompat;

import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRestrictedLockUtilsInternal.class, ShadowInteractionJankMonitor.class})
public class SeekBarPreferenceTest {

    private static final int MAX = 75;
    private static final int MIN = 5;
    private static final int PROGRESS = 16;
    private static final int NEW_PROGRESS = 17;

    private Context mContext;
    private SeekBarPreference mSeekBarPreference;
    private SeekBar mSeekBar;

    @Mock
    SeekBar.OnSeekBarChangeListener mMockOnSeekBarChangeListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mSeekBarPreference = spy(new SeekBarPreference(mContext));
        mSeekBarPreference.setMax(MAX);
        mSeekBarPreference.setMin(MIN);
        mSeekBarPreference.setProgress(PROGRESS);
        mSeekBarPreference.setPersistent(false);
        mSeekBarPreference.setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_NONE);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setMax(MAX);
        mSeekBar.setMin(MIN);
    }

    @Test
    public void testSaveAndRestoreInstanceState() {
        final Parcelable parcelable = mSeekBarPreference.onSaveInstanceState();

        final SeekBarPreference preference = new SeekBarPreference(mContext);
        preference.onRestoreInstanceState(parcelable);

        assertThat(preference.getMax()).isEqualTo(MAX);
        assertThat(preference.getMin()).isEqualTo(MIN);
        assertThat(preference.getProgress()).isEqualTo(PROGRESS);
    }

    @Test
    public void isSelectable_disabledByAdmin_returnTrue() {
        when(mSeekBarPreference.isDisabledByAdmin()).thenReturn(true);

        assertThat(mSeekBarPreference.isSelectable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc998")
    public void isSelectable_default_returnFalse() {
        final PreferenceFragmentCompat fragment = FragmentController.of(new TestFragment(),
                new Bundle())
                .create()
                .start()
                .resume()
                .get();

        final SeekBarPreference seekBarPreference = fragment.findPreference("seek_bar");

        assertThat(seekBarPreference.isSelectable()).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isSelectable_selectableInXml_returnFalse() {
        final PreferenceFragmentCompat fragment = FragmentController.of(new TestFragment(),
                new Bundle())
                .create()
                .start()
                .resume()
                .get();

        final SeekBarPreference seekBarPreference = fragment.findPreference("seek_bar");

        assertThat(seekBarPreference.isSelectable()).isFalse();
    }

    @Test
    public void testSetSeekBarStateDescription() {
        mSeekBarPreference.setSeekBarStateDescription("test");

        verify(mSeekBarPreference).setSeekBarStateDescription("test");
    }

    @Test
    public void onProgressChanged_hapticFeedbackModeNone_clockTickFeedbackNotPerformed() {
        mSeekBar.setProgress(NEW_PROGRESS);
        when(mSeekBarPreference.callChangeListener(anyInt())).thenReturn(true);
        mSeekBar.performHapticFeedback(CONTEXT_CLICK);

        mSeekBarPreference.onProgressChanged(mSeekBar, NEW_PROGRESS, true);

        assertThat(shadowOf(mSeekBar).lastHapticFeedbackPerformed()).isNotEqualTo(CLOCK_TICK);
    }

    @Test
    public void onProgressChanged_hapticFeedbackModeOnTicks_clockTickFeedbackPerformed() {
        mSeekBarPreference.setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
        mSeekBar.setProgress(NEW_PROGRESS);
        when(mSeekBarPreference.callChangeListener(anyInt())).thenReturn(true);
        mSeekBar.performHapticFeedback(CONTEXT_CLICK);

        mSeekBarPreference.onProgressChanged(mSeekBar, NEW_PROGRESS, true);

        assertThat(shadowOf(mSeekBar).lastHapticFeedbackPerformed()).isEqualTo(CLOCK_TICK);
    }

    @Test
    public void onProgressChanged_hapticFeedbackModeOnEnds_clockTickFeedbackNotPerformed() {
        mSeekBarPreference.setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS);
        mSeekBar.setProgress(NEW_PROGRESS);
        when(mSeekBarPreference.callChangeListener(anyInt())).thenReturn(true);
        mSeekBar.performHapticFeedback(CONTEXT_CLICK);

        mSeekBarPreference.onProgressChanged(mSeekBar, NEW_PROGRESS, true);

        assertThat(shadowOf(mSeekBar).lastHapticFeedbackPerformed()).isNotEqualTo(CLOCK_TICK);
    }

    @Test
    public void onProgressChanged_hapticFeedbackModeOnEndsAndMinValue_clockTickFeedbackPerformed() {
        mSeekBarPreference.setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS);
        mSeekBar.setProgress(MIN);
        when(mSeekBarPreference.callChangeListener(anyInt())).thenReturn(true);
        mSeekBar.performHapticFeedback(CONTEXT_CLICK);

        mSeekBarPreference.onProgressChanged(mSeekBar, MIN, true);

        assertThat(shadowOf(mSeekBar).lastHapticFeedbackPerformed()).isEqualTo(CLOCK_TICK);
    }

    @Test
    public void onProgressChanged_hasSeekBarChangeListener_receiveCallBack() {
        mSeekBarPreference.setOnSeekBarChangeListener(mMockOnSeekBarChangeListener);

        mSeekBarPreference.onProgressChanged(mSeekBar, PROGRESS, true);

        verify(mMockOnSeekBarChangeListener).onProgressChanged(mSeekBar, PROGRESS, true);
    }

    @Test
    public void onProgressChanged_noSeekBarChangeListener_noAction() {
        mSeekBarPreference.onProgressChanged(mSeekBar, PROGRESS, true);

        verifyNoInteractions(mMockOnSeekBarChangeListener);
    }

    @Test
    public void onStartTrackingTouch_hasSeekBarChangeListener_receiveCallBack() {
        mSeekBarPreference.setOnSeekBarChangeListener(mMockOnSeekBarChangeListener);

        mSeekBarPreference.onStartTrackingTouch(mSeekBar);

        verify(mMockOnSeekBarChangeListener).onStartTrackingTouch(mSeekBar);
    }

    @Test
    public void onStartTrackingTouch_noSeekBarChangeListener_noAction() {
        mSeekBarPreference.onStartTrackingTouch(mSeekBar);

        verifyNoInteractions(mMockOnSeekBarChangeListener);
    }

    @Test
    public void onStopTrackingTouch_hasSeekBarChangeListener_receiveCallBack() {
        mSeekBarPreference.setOnSeekBarChangeListener(mMockOnSeekBarChangeListener);

        mSeekBarPreference.onStopTrackingTouch(mSeekBar);

        verify(mMockOnSeekBarChangeListener).onStopTrackingTouch(mSeekBar);
    }

    @Test
    public void onStopTrackingTouch_noSeekBarChangeListener_noAction() {
        mSeekBarPreference.onStopTrackingTouch(mSeekBar);

        verifyNoInteractions(mMockOnSeekBarChangeListener);
    }

    public static class TestFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(com.android.settings.R.xml.seekbar_preference);
        }
    }
}

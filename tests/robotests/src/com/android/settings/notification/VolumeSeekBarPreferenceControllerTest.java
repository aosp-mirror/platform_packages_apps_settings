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
 * limitations under the License.
 */

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VolumeSeekBarPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private VolumeSeekBarPreference mPreference;
    @Mock
    private VolumeSeekBarPreference.Callback mCallback;
    @Mock
    private AudioHelper mHelper;

    private VolumeSeekBarPreferenceControllerTestable mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mScreen.findPreference(nullable(String.class))).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn("key");
        mController = new VolumeSeekBarPreferenceControllerTestable(mContext, mCallback, true,
                mPreference.getKey());
        mController.setAudioHelper(mHelper);
    }

    @Test
    public void displayPreference_available_shouldUpdatePreference() {
        mController.displayPreference(mScreen);

        verify(mPreference).setCallback(mCallback);
        verify(mPreference).setStream(VolumeSeekBarPreferenceControllerTestable.AUDIO_STREAM);
        verify(mPreference).setMuteIcon(VolumeSeekBarPreferenceControllerTestable.MUTE_ICON);
    }

    @Test
    public void displayPreference_notAvailable_shouldNotUpdatePreference() {
        mController = new VolumeSeekBarPreferenceControllerTestable(mContext, mCallback, false,
                mPreference.getKey());

        mController.displayPreference(mScreen);

        verify(mPreference, never()).setCallback(any(VolumeSeekBarPreference.Callback.class));
        verify(mPreference, never()).setStream(anyInt());
        verify(mPreference, never()).setMuteIcon(anyInt());
    }

    @Test
    public void onResume_shouldResumePreference() {
        mController.displayPreference(mScreen);

        mController.onResume();

        verify(mPreference).onActivityResume();
    }

    @Test
    public void onPause_shouldPausePreference() {
        mController.displayPreference(mScreen);

        mController.onPause();

        verify(mPreference).onActivityPause();
    }

    @Test
    public void sliderMethods_handleNullPreference() {
        when(mHelper.getStreamVolume(mController.getAudioStream())).thenReturn(4);
        when(mHelper.getMaxVolume(mController.getAudioStream())).thenReturn(10);
        when(mHelper.getMinVolume(mController.getAudioStream())).thenReturn(1);

        assertThat(mController.getMax()).isEqualTo(10);
        assertThat(mController.getMin()).isEqualTo(1);
        assertThat(mController.getSliderPosition()).isEqualTo(4);

        mController.setSliderPosition(9);
        verify(mHelper).setStreamVolume(mController.getAudioStream(), 9);
    }

    @Test
    public void setSliderPosition_passesAlongValue() {
        mController.displayPreference(mScreen);

        mController.setSliderPosition(2);
        verify(mPreference).setProgress(2);
    }

    @Test
    public void getMaxValue_passesAlongValue() {
        when(mPreference.getMax()).thenReturn(6);
        mController.displayPreference(mScreen);

        assertThat(mController.getMax()).isEqualTo(6);
    }

    @Test
    public void getMinValue_passesAlongValue() {
        when(mPreference.getMin()).thenReturn(1);
        mController.displayPreference(mScreen);

        assertThat(mController.getMin()).isEqualTo(1);
    }

    @Test
    public void getSliderPosition_passesAlongValue() {
        when(mPreference.getProgress()).thenReturn(7);
        mController.displayPreference(mScreen);

        assertThat(mController.getSliderPosition()).isEqualTo(7);
    }

    private class VolumeSeekBarPreferenceControllerTestable
        extends VolumeSeekBarPreferenceController {

        private final static int AUDIO_STREAM = 1;
        private final static int MUTE_ICON = 2;

        private boolean mAvailable;

        VolumeSeekBarPreferenceControllerTestable(Context context,
            VolumeSeekBarPreference.Callback callback, boolean available, String key) {
            super(context, key);
            setCallback(callback);
            mAvailable = available;
        }

        @Override
        public String getPreferenceKey() {
            return "key";
        }

        @Override
        public boolean handlePreferenceTreeClick(Preference preference) {
            return false;
        }

        @Override
        public int getAvailabilityStatus() {
            return mAvailable ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        }

        @Override
        public int getAudioStream() {
            return AUDIO_STREAM;
        }

        @Override
        public int getMuteIcon() {
            return MUTE_ICON;
        }
    }
}

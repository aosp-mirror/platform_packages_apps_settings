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

package com.android.settings.notification;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.AudioManager;
import android.preference.SeekBarVolumizer;
import android.widget.SeekBar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VolumeSeekBarPreferenceTest {

    private static final CharSequence CONTENT_DESCRIPTION = "TEST";
    @Mock
    private AudioManager mAudioManager;
    @Mock
    private VolumeSeekBarPreference mPreference;
    @Mock
    private Context mContext;
    @Mock
    private SeekBar mSeekBar;
    @Mock
    private SeekBarVolumizer mVolumizer;
    private VolumeSeekBarPreference.Listener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mAudioManager);
        doCallRealMethod().when(mPreference).updateContentDescription(CONTENT_DESCRIPTION);
        mPreference.mSeekBar = mSeekBar;
        mPreference.mAudioManager = mAudioManager;
        mPreference.mVolumizer = mVolumizer;
        mListener = () -> mPreference.updateContentDescription(CONTENT_DESCRIPTION);
    }

    @Test
    public void setStream_shouldSetMinMaxAndProgress() {
        final int stream = 5;
        final int max = 17;
        final int min = 1;
        final int progress = 4;
        when(mAudioManager.getStreamMaxVolume(stream)).thenReturn(max);
        when(mAudioManager.getStreamMinVolumeInt(stream)).thenReturn(min);
        when(mAudioManager.getStreamVolume(stream)).thenReturn(progress);
        doCallRealMethod().when(mPreference).setStream(anyInt());

        mPreference.setStream(stream);

        verify(mPreference).setMax(max);
        verify(mPreference).setMin(min);
        verify(mPreference).setProgress(progress);
    }

    @Test
    public void init_listenerIsCalled() {
        when(mPreference.isEnabled()).thenReturn(true);
        doCallRealMethod().when(mPreference).setListener(mListener);
        doCallRealMethod().when(mPreference).init();

        mPreference.setListener(mListener);
        mPreference.init();

        verify(mPreference).updateContentDescription(CONTENT_DESCRIPTION);
    }

    @Test
    public void init_listenerNotSet_noException() {
        when(mPreference.isEnabled()).thenReturn(true);
        doCallRealMethod().when(mPreference).init();

        mPreference.init();

        verify(mPreference, never()).updateContentDescription(CONTENT_DESCRIPTION);
    }

    @Test
    public void init_preferenceIsDisabled_shouldNotInvokeListener() {
        when(mPreference.isEnabled()).thenReturn(false);
        doCallRealMethod().when(mPreference).setListener(mListener);
        doCallRealMethod().when(mPreference).init();

        mPreference.init();

        verify(mPreference, never()).updateContentDescription(CONTENT_DESCRIPTION);
    }
}

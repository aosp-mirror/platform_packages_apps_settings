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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.LocaleList;
import android.preference.SeekBarVolumizer;
import android.widget.SeekBar;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class VolumeSeekBarPreferenceTest {

    private static final CharSequence CONTENT_DESCRIPTION = "TEST";
    private static final int STREAM = 5;
    @Mock
    private AudioManager mAudioManager;
    @Mock
    private VolumeSeekBarPreference mPreference;
    @Mock
    private Context mContext;

    @Mock
    private Resources mRes;
    @Mock
    private Configuration mConfig;
    @Mock
    private SeekBar mSeekBar;
    @Captor
    private ArgumentCaptor<SeekBarVolumizer.Callback> mSbvc;
    @Mock
    private SeekBarVolumizer mVolumizer;
    @Mock
    private SeekBarVolumizerFactory mSeekBarVolumizerFactory;
    private VolumeSeekBarPreference.Listener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mAudioManager);
        when(mSeekBarVolumizerFactory.create(eq(STREAM), eq(null), mSbvc.capture()))
                .thenReturn(mVolumizer);
        doCallRealMethod().when(mPreference).setStream(anyInt());
        doCallRealMethod().when(mPreference).updateContentDescription(CONTENT_DESCRIPTION);
        mPreference.mSeekBar = mSeekBar;
        mPreference.mAudioManager = mAudioManager;
        mPreference.mSeekBarVolumizerFactory = mSeekBarVolumizerFactory;
        mListener = () -> mPreference.updateContentDescription(CONTENT_DESCRIPTION);
    }

    @Test
    public void setStream_shouldSetMinMaxAndProgress() {
        final int max = 17;
        final int min = 1;
        final int progress = 4;
        when(mAudioManager.getStreamMaxVolume(STREAM)).thenReturn(max);
        when(mAudioManager.getStreamMinVolumeInt(STREAM)).thenReturn(min);
        when(mAudioManager.getStreamVolume(STREAM)).thenReturn(progress);

        mPreference.setStream(STREAM);

        verify(mPreference).setMax(max);
        verify(mPreference).setMin(min);
        verify(mPreference).setProgress(progress);
    }

    @Test
    public void init_listenerIsCalled() {
        when(mPreference.isEnabled()).thenReturn(true);
        doCallRealMethod().when(mPreference).setListener(mListener);
        doCallRealMethod().when(mPreference).init();

        mPreference.setStream(STREAM);
        mPreference.setListener(mListener);
        mPreference.init();

        verify(mPreference).updateContentDescription(CONTENT_DESCRIPTION);
    }

    @Test
    public void init_listenerNotSet_noException() {
        when(mPreference.isEnabled()).thenReturn(true);
        doCallRealMethod().when(mPreference).init();

        mPreference.setStream(STREAM);
        mPreference.init();

        verify(mPreference, never()).updateContentDescription(CONTENT_DESCRIPTION);
    }

    @Test
    public void init_preferenceIsDisabled_shouldNotInvokeListener() {
        when(mPreference.isEnabled()).thenReturn(false);
        doCallRealMethod().when(mPreference).setListener(mListener);
        doCallRealMethod().when(mPreference).init();

        mPreference.setStream(STREAM);
        mPreference.init();

        verify(mPreference, never()).updateContentDescription(CONTENT_DESCRIPTION);
    }

    @Test
    public void init_changeProgress_overrideStateDescriptionCalled() {
        final int progress = 4;
        when(mPreference.isEnabled()).thenReturn(true);
        when(mPreference.formatStateDescription(progress)).thenReturn(CONTENT_DESCRIPTION);
        doCallRealMethod().when(mPreference).init();

        mPreference.setStream(STREAM);
        mPreference.init();

        verify(mSeekBarVolumizerFactory).create(eq(STREAM), eq(null), mSbvc.capture());

        mSbvc.getValue().onProgressChanged(mSeekBar, 4, true);

        verify(mPreference).overrideSeekBarStateDescription(CONTENT_DESCRIPTION);
    }

    @Test
    public void init_changeProgress_stateDescriptionValueUpdated() {
        final int max = 17;
        final int min = 1;
        int progress = 4;
        when(mAudioManager.getStreamMaxVolume(STREAM)).thenReturn(max);
        when(mAudioManager.getStreamMinVolumeInt(STREAM)).thenReturn(min);
        when(mAudioManager.getStreamVolume(STREAM)).thenReturn(progress);
        when(mPreference.isEnabled()).thenReturn(true);
        when(mPreference.getMin()).thenReturn(min);
        when(mPreference.getMax()).thenReturn(max);
        when(mPreference.getContext()).thenReturn(mContext);
        when(mContext.getResources()).thenReturn(mRes);
        when(mRes.getConfiguration()).thenReturn(mConfig);
        when(mConfig.getLocales()).thenReturn(new LocaleList(Locale.US));
        doCallRealMethod().when(mPreference).init();

        mPreference.setStream(STREAM);
        mPreference.init();

        verify(mSeekBarVolumizerFactory).create(eq(STREAM), eq(null), mSbvc.capture());

        // On progress change, Round down the percent to match it with what the talkback says.
        // (b/285458191)
        // when progress is 4, the percent is 0.187. The state description should be set to 18%.
        testFormatStateDescription(progress, "18%");

        progress = 6;

        // when progress is 6, the percent is 0.3125. The state description should be set to 31%.
        testFormatStateDescription(progress, "31%");

        progress = 7;

        // when progress is 7, the percent is 0.375. The state description should be set to 37%.
        testFormatStateDescription(progress, "37%");
    }

    private void testFormatStateDescription(int progress, String expected) {
        doCallRealMethod().when(mPreference).formatStateDescription(progress);
        doCallRealMethod().when(mPreference).getPercent(progress);

        mSbvc.getValue().onProgressChanged(mSeekBar, progress, true);

        verify(mPreference).overrideSeekBarStateDescription(expected);
    }
}

/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settingslib.flags.Flags.FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.text.SpannableString;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioStreamStateHandlerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final int SUMMARY_RES = 1;
    private static final String SUMMARY = "summary";
    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    @Mock private AudioStreamsProgressCategoryController mController;
    @Mock private AudioStreamsHelper mHelper;
    @Mock private AudioStreamPreference mPreference;
    private AudioStreamStateHandler mHandler;

    @Before
    public void setUp() {
        mSetFlagsRule.disableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);
        mHandler = spy(new AudioStreamStateHandler());
    }

    @Test
    public void testHandleStateChange_noChange_doNothing() {
        when(mHandler.getStateEnum())
                .thenReturn(
                        AudioStreamsProgressCategoryController.AudioStreamState
                                .ADD_SOURCE_BAD_CODE);
        when(mPreference.getAudioStreamState())
                .thenReturn(
                        AudioStreamsProgressCategoryController.AudioStreamState
                                .ADD_SOURCE_BAD_CODE);

        mHandler.handleStateChange(mPreference, mController, mHelper);

        verify(mPreference, never()).setAudioStreamState(any());
        verify(mHandler, never()).performAction(any(), any(), any());
        verify(mPreference, never()).setIsConnected(anyBoolean());
        verify(mPreference, never()).setSummary(any());
        verify(mPreference, never()).setOnPreferenceClickListener(any());
    }

    @Test
    public void testHandleStateChange_setNewState() {
        when(mHandler.getStateEnum())
                .thenReturn(AudioStreamsProgressCategoryController.AudioStreamState.SOURCE_ADDED);
        when(mPreference.getAudioStreamState())
                .thenReturn(
                        AudioStreamsProgressCategoryController.AudioStreamState
                                .ADD_SOURCE_BAD_CODE);

        mHandler.handleStateChange(mPreference, mController, mHelper);

        verify(mPreference)
                .setAudioStreamState(
                        AudioStreamsProgressCategoryController.AudioStreamState.SOURCE_ADDED);
        verify(mHandler).performAction(any(), any(), any());
        verify(mPreference).setIsConnected(eq(true));
        verify(mPreference).setSummary(eq(""));
        verify(mPreference).setOnPreferenceClickListener(eq(null));
    }

    @Test
    public void testHandleStateChange_setNewState_sourcePresent() {
        mSetFlagsRule.enableFlags(FLAG_AUDIO_SHARING_HYSTERESIS_MODE_FIX);

        when(mHandler.getStateEnum())
                .thenReturn(AudioStreamsProgressCategoryController.AudioStreamState.SOURCE_PRESENT);
        when(mPreference.getAudioStreamState())
                .thenReturn(
                        AudioStreamsProgressCategoryController.AudioStreamState
                                .ADD_SOURCE_BAD_CODE);

        mHandler.handleStateChange(mPreference, mController, mHelper);

        verify(mPreference)
                .setAudioStreamState(
                        AudioStreamsProgressCategoryController.AudioStreamState.SOURCE_PRESENT);
        verify(mHandler).performAction(any(), any(), any());
        verify(mPreference).setIsConnected(eq(true));
        verify(mPreference).setSummary(eq(""));
        verify(mPreference).setOnPreferenceClickListener(eq(null));
    }

    @Test
    public void testHandleStateChange_setNewState_newSummary_newListener() {
        Preference.OnPreferenceClickListener listener =
                mock(Preference.OnPreferenceClickListener.class);
        when(mHandler.getStateEnum())
                .thenReturn(
                        AudioStreamsProgressCategoryController.AudioStreamState
                                .ADD_SOURCE_BAD_CODE);
        when(mHandler.getSummary()).thenReturn(SUMMARY_RES);
        when(mHandler.getOnClickListener(any())).thenReturn(listener);
        when(mPreference.getAudioStreamState())
                .thenReturn(
                        AudioStreamsProgressCategoryController.AudioStreamState.ADD_SOURCE_FAILED);
        when(mPreference.getContext()).thenReturn(mContext);
        doReturn(SUMMARY).when(mContext).getString(anyInt());

        mHandler.handleStateChange(mPreference, mController, mHelper);

        verify(mPreference)
                .setAudioStreamState(
                        AudioStreamsProgressCategoryController.AudioStreamState
                                .ADD_SOURCE_BAD_CODE);
        verify(mHandler).performAction(any(), any(), any());
        verify(mPreference).setIsConnected(eq(false));
        ArgumentCaptor<SpannableString> argumentCaptor =
                ArgumentCaptor.forClass(SpannableString.class);
        verify(mPreference).setSummary(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isNotNull();
        assertThat(argumentCaptor.getValue().toString()).isEqualTo(SUMMARY);
        verify(mPreference).setOnPreferenceClickListener(eq(listener));
    }

    @Test
    public void testGetSummary() {
        int res = mHandler.getSummary();
        assertThat(res).isEqualTo(AudioStreamStateHandler.EMPTY_STRING_RES);
    }

    @Test
    public void testGetOnClickListener() {
        Preference.OnPreferenceClickListener listener = mHandler.getOnClickListener(mController);
        assertThat(listener).isNull();
    }

    @Test
    public void testGetStateEnum() {
        var state = mHandler.getStateEnum();
        assertThat(state)
                .isEqualTo(AudioStreamsProgressCategoryController.AudioStreamState.UNKNOWN);
    }
}

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

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamHeaderController.AUDIO_STREAM_HEADER_LISTENING_NOW_SUMMARY;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamHeaderController.AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowEntityHeaderController;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowEntityHeaderController.class,
            ShadowThreadUtils.class,
            ShadowAudioStreamsHelper.class,
        })
public class AudioStreamHeaderControllerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String KEY = "audio_stream_header";
    private static final int BROADCAST_ID = 1;
    private static final String BROADCAST_NAME = "broadcast name";
    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private AudioStreamsHelper mAudioStreamsHelper;
    @Mock private PreferenceScreen mScreen;
    @Mock private BluetoothLeBroadcastReceiveState mBroadcastReceiveState;
    @Mock private AudioStreamDetailsFragment mFragment;
    @Mock private LayoutPreference mPreference;
    @Mock private EntityHeaderController mHeaderController;
    private AudioStreamHeaderController mController;

    @Before
    public void setUp() {
        ShadowEntityHeaderController.setUseMock(mHeaderController);
        ShadowAudioStreamsHelper.setUseMock(mAudioStreamsHelper);
        mController = new AudioStreamHeaderController(mContext, KEY);
        mController.init(mFragment, BROADCAST_NAME, BROADCAST_ID);
        when(mScreen.findPreference(KEY)).thenReturn(mPreference);
        when(mScreen.getContext()).thenReturn(mContext);
        when(mPreference.getContext()).thenReturn(mContext);
    }

    @Test
    public void testDisplayPreference_sourceConnected_setSummary() {
        when(mAudioStreamsHelper.getAllConnectedSources())
                .thenReturn(List.of(mBroadcastReceiveState));
        when(mBroadcastReceiveState.getBroadcastId()).thenReturn(BROADCAST_ID);

        mController.displayPreference(mScreen);

        verify(mHeaderController).setLabel(BROADCAST_NAME);
        verify(mHeaderController)
                .setSummary(mContext.getString(AUDIO_STREAM_HEADER_LISTENING_NOW_SUMMARY));
        verify(mHeaderController).done(true);
    }

    @Test
    public void testDisplayPreference_sourceNotConnected_setSummary() {
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(Collections.emptyList());

        mController.displayPreference(mScreen);

        verify(mHeaderController).setLabel(BROADCAST_NAME);
        verify(mHeaderController).setSummary(AUDIO_STREAM_HEADER_NOT_LISTENING_SUMMARY);
        verify(mHeaderController).done(true);
    }
}

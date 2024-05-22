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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.view.View;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.widget.ActionButtonsPreference;

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
            ShadowThreadUtils.class,
            ShadowAudioStreamsHelper.class,
        })
public class AudioStreamButtonControllerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String KEY = "audio_stream_button";
    private static final int BROADCAST_ID = 1;
    @Spy Context mContext = ApplicationProvider.getApplicationContext();
    @Mock private AudioStreamsHelper mAudioStreamsHelper;
    @Mock private PreferenceScreen mScreen;
    @Mock private BluetoothLeBroadcastReceiveState mBroadcastReceiveState;
    @Mock private ActionButtonsPreference mPreference;
    private AudioStreamButtonController mController;

    @Before
    public void setUp() {
        ShadowAudioStreamsHelper.setUseMock(mAudioStreamsHelper);
        mController = new AudioStreamButtonController(mContext, KEY);
        mController.init(BROADCAST_ID);
        when(mScreen.findPreference(KEY)).thenReturn(mPreference);
        when(mPreference.getContext()).thenReturn(mContext);
        when(mPreference.setButton1Text(anyInt())).thenReturn(mPreference);
        when(mPreference.setButton1Icon(anyInt())).thenReturn(mPreference);
        when(mPreference.setButton1Enabled(anyBoolean())).thenReturn(mPreference);
        when(mPreference.setButton1OnClickListener(any(View.OnClickListener.class)))
                .thenReturn(mPreference);
    }

    @Test
    public void testDisplayPreference_sourceConnected_setDisconnectButton() {
        when(mAudioStreamsHelper.getAllConnectedSources())
                .thenReturn(List.of(mBroadcastReceiveState));
        when(mBroadcastReceiveState.getBroadcastId()).thenReturn(BROADCAST_ID);

        mController.displayPreference(mScreen);

        verify(mPreference).setButton1Enabled(true);
        verify(mPreference).setButton1Text(R.string.audio_streams_disconnect);
        verify(mPreference).setButton1Icon(com.android.settings.R.drawable.ic_settings_close);
        verify(mPreference).setButton1OnClickListener(any(View.OnClickListener.class));
    }

    @Test
    public void testDisplayPreference_sourceNotConnected_setConnectButton() {
        when(mAudioStreamsHelper.getAllConnectedSources()).thenReturn(Collections.emptyList());

        mController.displayPreference(mScreen);

        verify(mPreference).setButton1Enabled(true);
        verify(mPreference).setButton1Text(R.string.audio_streams_connect);
        verify(mPreference).setButton1Icon(com.android.settings.R.drawable.ic_add_24dp);
        verify(mPreference).setButton1OnClickListener(any(View.OnClickListener.class));
    }
}

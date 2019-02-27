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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class RemoteVolumePreferenceControllerTest {

    @Mock
    private MediaSessionManager mMediaSessionManager;
    @Mock
    private MediaController mMediaController;
    private RemoteVolumePreferenceController mController;
    private Context mContext;
    private List<MediaController> mActiveSessions;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(MediaSessionManager.class)).thenReturn(mMediaSessionManager);
        mActiveSessions = new ArrayList<>();
        mActiveSessions.add(mMediaController);
        when(mMediaSessionManager.getActiveSessions(null)).thenReturn(
                mActiveSessions);

        mController = new RemoteVolumePreferenceController(mContext);
    }

    @Test
    public void isAvailable_containRemoteMedia_returnTrue() {
        when(mMediaController.getPlaybackInfo()).thenReturn(
                new MediaController.PlaybackInfo(MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
                        0, 0, 0, null));
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noRemoteMedia_returnFalse() {
        when(mMediaController.getPlaybackInfo()).thenReturn(
                new MediaController.PlaybackInfo(MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL,
                        0, 0, 0, null));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getMuteIcon_returnMuteIcon() {
        assertThat(mController.getMuteIcon()).isEqualTo(R.drawable.ic_volume_remote_mute);
    }

    @Test
    public void getAudioStream_returnRemoteVolume() {
        assertThat(mController.getAudioStream()).isEqualTo(
                RemoteVolumePreferenceController.REMOTE_VOLUME);
    }
}

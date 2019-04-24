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

package com.android.settings.panel;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.session.ISessionController;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.Uri;

import com.android.settings.notification.RemoteVolumePreferenceController;
import com.android.settings.slices.CustomSliceRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VolumePanelTest {

    @Mock
    private MediaSessionManager mMediaSessionManager;
    @Mock
    private MediaController mMediaController;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ISessionController mStub;


    private VolumePanel mPanel;
    private Context mContext;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(MediaSessionManager.class)).thenReturn(mMediaSessionManager);

        mPanel = VolumePanel.create(mContext);
    }

    @Test
    public void getSlices_hasActiveRemoteToken_containsRemoteMediaUri() {
        List<MediaController> activeSessions = new ArrayList<>();
        MediaSession.Token token = new MediaSession.Token(mStub);
        activeSessions.add(mMediaController);

        when(mMediaSessionManager.getActiveSessions(null)).thenReturn(
                activeSessions);
        when(mMediaController.getPlaybackInfo()).thenReturn(new MediaController.PlaybackInfo(
                MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE, 0, 10, 5, null));
        when(mMediaController.getSessionToken()).thenReturn(new MediaSession.Token(mStub));
        when(RemoteVolumePreferenceController.getActiveRemoteToken(mContext)).thenReturn(token);

        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(
                CustomSliceRegistry.VOLUME_REMOTE_MEDIA_URI,
                CustomSliceRegistry.VOLUME_CALL_URI,
                CustomSliceRegistry.VOLUME_MEDIA_URI,
                CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI,
                CustomSliceRegistry.VOLUME_RINGER_URI,
                CustomSliceRegistry.VOLUME_ALARM_URI);
    }

    @Test
    public void getSlices_doesNotHaveActiveRemoteToken_doesNotcontainRemoteMediaUri() {
        final List<Uri> uris = mPanel.getSlices();

        when(RemoteVolumePreferenceController.getActiveRemoteToken(mContext))
            .thenReturn(null);

        assertThat(uris).doesNotContain(CustomSliceRegistry.VOLUME_REMOTE_MEDIA_URI);
        assertThat(uris).containsExactly(
            CustomSliceRegistry.VOLUME_CALL_URI,
            CustomSliceRegistry.VOLUME_MEDIA_URI,
            CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI,
            CustomSliceRegistry.VOLUME_RINGER_URI,
            CustomSliceRegistry.VOLUME_ALARM_URI);
    }

    @Test
    public void getSeeMoreIntent_notNull() {
        assertThat(mPanel.getSeeMoreIntent()).isNotNull();
    }
}

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
 *
 */
package com.android.settings.panel;

import static com.android.settings.media.MediaOutputSlice.MEDIA_PACKAGE_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settingslib.media.InfoMediaDevice;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.PhoneMediaDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MediaOutputPanelTest {

    private static final String TEST_PACKAGENAME = "com.test.packagename";
    private static final String TEST_PACKAGENAME2 = "com.test.packagename2";
    private static final String TEST_ARTIST = "test_artist";
    private static final String TEST_SONG = "test_song";

    @Mock
    private MediaSessionManager mMediaSessionManager;
    @Mock
    private MediaController mMediaController;
    @Mock
    private MediaMetadata mMediaMetadata;
    @Mock
    private LocalMediaManager mLocalMediaManager;
    @Mock
    private PanelContentCallback mCallback;
    @Mock
    private PlaybackState mPlaybackState;

    private MediaOutputPanel mPanel;
    private Context mContext;
    private List<MediaController> mMediaControllers = new ArrayList<>();
    private ArgumentCaptor<MediaController.Callback> mControllerCbs =
            ArgumentCaptor.forClass(MediaController.Callback.class);
    private MediaDescription mMediaDescription;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);

        mMediaControllers.add(mMediaController);
        when(mMediaController.getPackageName()).thenReturn(TEST_PACKAGENAME);
        when(mMediaSessionManager.getActiveSessions(any())).thenReturn(mMediaControllers);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(MediaSessionManager.class)).thenReturn(mMediaSessionManager);
        MediaDescription.Builder builder = new MediaDescription.Builder();
        builder.setTitle(TEST_SONG);
        builder.setSubtitle(TEST_ARTIST);
        mMediaDescription = builder.build();
        when(mMediaMetadata.getDescription()).thenReturn(mMediaDescription);

        mPanel = MediaOutputPanel.create(mContext, TEST_PACKAGENAME);
        mPanel.mLocalMediaManager = mLocalMediaManager;
        mPanel.registerCallback(mCallback);
    }

    @Test
    public void getSlices_containsNecessarySlices() {
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI);
    }

    @Test
    public void getSlices_verifyPackageName_isEqual() {
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris.get(0).getQueryParameter(MEDIA_PACKAGE_NAME)).isEqualTo(TEST_PACKAGENAME);
    }

    @Test
    public void getSeeMoreIntent_isNull() {
        assertThat(mPanel.getSeeMoreIntent()).isNull();
    }

    @Test
    public void onStart_shouldRegisterCallback() {
        mPanel.onStart();

        verify(mMediaController).registerCallback(any());
        verify(mLocalMediaManager).registerCallback(any());
        verify(mLocalMediaManager).startScan();
    }

    @Test
    public void onStart_activeSession_verifyOnHeaderChanged() {
        mPanel.onStart();

        verify(mCallback).onHeaderChanged();
    }

    @Test
    public void onStart_noMatchedActiveSession_verifyNeverOnHeaderChanged() {
        when(mMediaController.getPackageName()).thenReturn(TEST_PACKAGENAME2);
        mPanel.onStart();

        verify(mCallback, never()).onHeaderChanged();
    }

    @Test
    public void onStop_shouldUnregisterCallback() {
        mPanel.onStop();

        verify(mLocalMediaManager).unregisterCallback(any());
        verify(mLocalMediaManager).stopScan();
    }

    @Test
    public void onSelectedDeviceStateChanged_shouldDispatchCustomButtonStateChanged() {
        mPanel.onSelectedDeviceStateChanged(null, 0);

        verify(mCallback).onCustomizedButtonStateChanged();
    }

    @Test
    public void onDeviceListUpdate_shouldDispatchCustomButtonStateChanged() {
        mPanel.onDeviceListUpdate(null);

        verify(mCallback).onCustomizedButtonStateChanged();
    }

    @Test
    public void onDeviceAttributesChanged_shouldDispatchCustomButtonStateChanged() {
        mPanel.onDeviceAttributesChanged();

        verify(mCallback).onCustomizedButtonStateChanged();
    }

    @Test
    public void currentConnectDeviceIsInfoDevice_useCustomButtonIsTrue() {
        final InfoMediaDevice infoMediaDevice = mock(InfoMediaDevice.class);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(infoMediaDevice);

        mPanel.onDeviceAttributesChanged();

        assertThat(mPanel.isCustomizedButtonUsed()).isTrue();
    }

    @Test
    public void currentConnectDeviceIsNotInfoDevice_useCustomButtonIsFalse() {
        final PhoneMediaDevice phoneMediaDevice = mock(PhoneMediaDevice.class);
        when(mLocalMediaManager.getCurrentConnectedDevice()).thenReturn(phoneMediaDevice);

        mPanel.onDeviceAttributesChanged();

        assertThat(mPanel.isCustomizedButtonUsed()).isFalse();
    }

    @Test
    public void getTitle_withMetadata_returnSongName() {
        mPanel.onStart();
        when(mMediaController.getMetadata()).thenReturn(mMediaMetadata);

        assertThat(mPanel.getTitle()).isEqualTo(TEST_SONG);
    }

    @Test
    public void getTitle_noMetadata_returnDefaultString() {
        when(mMediaController.getMetadata()).thenReturn(null);

        assertThat(mPanel.getTitle()).isEqualTo(mContext.getText(R.string.media_volume_title));
    }
    @Test
    public void getTitle_noPackageName_returnDefaultString() {
        mPanel = MediaOutputPanel.create(mContext, null);
        when(mMediaController.getMetadata()).thenReturn(mMediaMetadata);

        assertThat(mPanel.getTitle()).isEqualTo(mContext.getText(R.string.media_volume_title));
    }

    @Test
    public void getTitle_noController_defaultString() {
        mMediaControllers.clear();
        when(mMediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST)).thenReturn(TEST_ARTIST);
        when(mMediaController.getMetadata()).thenReturn(mMediaMetadata);
        mPanel = MediaOutputPanel.create(mContext, TEST_PACKAGENAME);

        assertThat(mPanel.getTitle()).isEqualTo(mContext.getText(R.string.media_volume_title));
    }

    @Test
    public void getSubTitle_withMetadata_returnArtistName() {
        mPanel.onStart();
        when(mMediaController.getMetadata()).thenReturn(mMediaMetadata);

        assertThat(mPanel.getSubTitle()).isEqualTo(TEST_ARTIST);
    }

    @Test
    public void getSubTitle_noMetadata_returnDefault() {
        when(mMediaController.getPackageName()).thenReturn(TEST_PACKAGENAME);
        when(mMediaController.getMetadata()).thenReturn(null);

        assertThat(mPanel.getSubTitle()).isEqualTo(mContext.getText(
                R.string.media_output_panel_title));
    }

    @Test
    public void getSubTitle_noPackageName_returnDefault() {
        mPanel = MediaOutputPanel.create(mContext, null);
        when(mMediaController.getMetadata()).thenReturn(mMediaMetadata);

        assertThat(mPanel.getSubTitle()).isEqualTo(mContext.getText(
                R.string.media_output_panel_title));
    }

    @Test
    public void getSubTitle_noController_returnDefault() {
        mMediaControllers.clear();
        mPanel = MediaOutputPanel.create(mContext, TEST_PACKAGENAME);
        when(mMediaController.getMetadata()).thenReturn(mMediaMetadata);

        assertThat(mPanel.getSubTitle()).isEqualTo(mContext.getText(
                R.string.media_output_panel_title));
    }

    @Test
    public void onClickCustomizedButton_shouldReleaseSession() {
        mPanel.onClickCustomizedButton();

        verify(mLocalMediaManager).releaseSession();
    }

    @Test
    public void onMetadataChanged_verifyCallOnHeaderChanged() {
        mPanel.onStart();
        verify(mCallback).onHeaderChanged();
        verify(mMediaController).registerCallback(mControllerCbs.capture());
        final MediaController.Callback controllerCallbacks = mControllerCbs.getValue();

        controllerCallbacks.onMetadataChanged(mMediaMetadata);

        verify(mCallback, times(2)).onHeaderChanged();
    }

    @Test
    public void onPlaybackStateChanged_stateFromPlayingToStopped_verifyCallForceClose() {
        mPanel.onStart();
        verify(mMediaController).registerCallback(mControllerCbs.capture());
        final MediaController.Callback controllerCallbacks = mControllerCbs.getValue();
        when(mPlaybackState.getState()).thenReturn(PlaybackState.STATE_PLAYING);
        controllerCallbacks.onPlaybackStateChanged(mPlaybackState);
        verify(mCallback, never()).forceClose();

        when(mPlaybackState.getState()).thenReturn(PlaybackState.STATE_STOPPED);
        controllerCallbacks.onPlaybackStateChanged(mPlaybackState);

        verify(mCallback).forceClose();
    }

    @Test
    public void onPlaybackStateChanged_stateFromPlayingToPaused_verifyCallForceClose() {
        mPanel.onStart();
        verify(mMediaController).registerCallback(mControllerCbs.capture());
        final MediaController.Callback controllerCallbacks = mControllerCbs.getValue();
        when(mPlaybackState.getState()).thenReturn(PlaybackState.STATE_PLAYING);
        controllerCallbacks.onPlaybackStateChanged(mPlaybackState);
        verify(mCallback, never()).forceClose();

        when(mPlaybackState.getState()).thenReturn(PlaybackState.STATE_PAUSED);
        controllerCallbacks.onPlaybackStateChanged(mPlaybackState);

        verify(mCallback).forceClose();
    }

    @Test
    public void getViewType_checkType() {
        assertThat(mPanel.getViewType()).isEqualTo(PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON);
    }

    @Test
    public void getIcon_mediaControllerIsNull_returnNull() {
        mMediaControllers.clear();
        mPanel.onStart();

        assertThat(mPanel.getIcon()).isNull();
    }

    @Test
    public void getIcon_mediaMetadataIsNull_returnNull() {
        mPanel.onStart();
        when(mMediaController.getMetadata()).thenReturn(null);

        assertThat(mPanel.getIcon()).isNull();
    }
}

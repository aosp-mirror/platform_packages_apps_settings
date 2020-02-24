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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settingslib.media.InfoMediaDevice;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.PhoneMediaDevice;

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
public class MediaOutputPanelTest {

    private static final String TEST_PACKAGENAME = "com.test.packagename";
    private static final String TEST_ARTIST = "test_artist";
    private static final String TEST_ALBUM = "test_album";

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

    private MediaOutputPanel mPanel;
    private Context mContext;
    private List<MediaController> mMediaControllers = new ArrayList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);

        mMediaControllers.add(mMediaController);
        when(mMediaController.getPackageName()).thenReturn(TEST_PACKAGENAME);
        when(mMediaSessionManager.getActiveSessions(any())).thenReturn(mMediaControllers);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(MediaSessionManager.class)).thenReturn(mMediaSessionManager);

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

        verify(mLocalMediaManager).registerCallback(any());
        verify(mLocalMediaManager).startScan();
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
    public void getTitle_withMetadata_returnArtistName() {
        when(mMediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST)).thenReturn(TEST_ARTIST);
        when(mMediaController.getMetadata()).thenReturn(mMediaMetadata);

        assertThat(mPanel.getTitle()).isEqualTo(TEST_ARTIST);
    }

    @Test
    public void getTitle_noMetadata_returnDefaultString() {
        when(mMediaController.getMetadata()).thenReturn(null);

        assertThat(mPanel.getTitle()).isEqualTo(mContext.getText(R.string.media_volume_title));
    }

    @Test
    public void getTitle_noPackageName_returnDefaultString() {
        mPanel = MediaOutputPanel.create(mContext, null);
        when(mMediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST)).thenReturn(TEST_ARTIST);
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
    public void getSubTitle_withMetadata_returnAlbumName() {
        when(mMediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM)).thenReturn(TEST_ALBUM);
        when(mMediaController.getMetadata()).thenReturn(mMediaMetadata);

        assertThat(mPanel.getSubTitle()).isEqualTo(TEST_ALBUM);
    }

    @Test
    public void getSubTitle_noMetadata_returnDefaultString() {
        when(mMediaController.getPackageName()).thenReturn(TEST_PACKAGENAME);
        when(mMediaController.getMetadata()).thenReturn(null);

        assertThat(mPanel.getSubTitle()).isEqualTo(mContext.getText(
                R.string.media_output_panel_title));
    }

    @Test
    public void getSubTitle_noPackageName_returnDefaultString() {
        mPanel = MediaOutputPanel.create(mContext, null);
        when(mMediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST)).thenReturn(TEST_ARTIST);
        when(mMediaController.getMetadata()).thenReturn(mMediaMetadata);

        assertThat(mPanel.getSubTitle()).isEqualTo(mContext.getText(
                R.string.media_output_panel_title));
    }

    @Test
    public void getSubTitle_noController_returnDefaultString() {
        mMediaControllers.clear();
        mPanel = MediaOutputPanel.create(mContext, TEST_PACKAGENAME);
        when(mMediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM)).thenReturn(TEST_ALBUM);
        when(mMediaController.getMetadata()).thenReturn(mMediaMetadata);

        assertThat(mPanel.getSubTitle()).isEqualTo(mContext.getText(
                R.string.media_output_panel_title));
    }
}

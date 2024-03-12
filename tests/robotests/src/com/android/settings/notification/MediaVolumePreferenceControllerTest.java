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

import static com.android.settings.slices.CustomSliceRegistry.VOLUME_MEDIA_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.net.Uri;

import androidx.slice.builders.SliceAction;

import com.android.settings.media.MediaOutputIndicatorWorker;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.media.BluetoothMediaDevice;
import com.android.settingslib.media.MediaDevice;
import com.android.settingslib.media.MediaOutputConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = MediaVolumePreferenceControllerTest.ShadowSliceBackgroundWorker.class)
public class MediaVolumePreferenceControllerTest {

    private static final String ACTION_LAUNCH_BROADCAST_DIALOG =
            "android.settings.MEDIA_BROADCAST_DIALOG";
    private static MediaOutputIndicatorWorker sMediaOutputIndicatorWorker;

    private MediaVolumePreferenceController mController;

    private Context mContext;

    @Mock
    private MediaController mMediaController;
    @Mock
    private MediaDevice mDevice1;
    @Mock
    private MediaDevice mDevice2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mController = new MediaVolumePreferenceController(mContext);
        sMediaOutputIndicatorWorker = spy(
                new MediaOutputIndicatorWorker(mContext, VOLUME_MEDIA_URI));
        when(mDevice1.isBLEDevice()).thenReturn(true);
        when(mDevice2.isBLEDevice()).thenReturn(false);
    }

    @Test
    public void isAvailable_byDefault_isTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_whenNotVisible_isFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getAudioStream_shouldReturnMusic() {
        assertThat(mController.getAudioStream()).isEqualTo(AudioManager.STREAM_MUSIC);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final MediaVolumePreferenceController controller = new MediaVolumePreferenceController(
                mContext);
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }

    @Test
    public void isSupportEndItem_withBleDevice_returnsTrue() {
        doReturn(true).when(sMediaOutputIndicatorWorker).isBroadcastSupported();
        doReturn(false).when(sMediaOutputIndicatorWorker).isDeviceBroadcasting();
        doReturn(mDevice1).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();

        assertThat(mController.isSupportEndItem()).isTrue();
    }

    @Test
    public void isSupportEndItem_notSupportedBroadcast_returnsFalse() {
        doReturn(false).when(sMediaOutputIndicatorWorker).isBroadcastSupported();
        doReturn(mDevice1).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();

        assertThat(mController.isSupportEndItem()).isFalse();
    }

    @Test
    public void isSupportEndItem_withNonBleDevice_returnsFalse() {
        doReturn(true).when(sMediaOutputIndicatorWorker).isBroadcastSupported();
        doReturn(false).when(sMediaOutputIndicatorWorker).isDeviceBroadcasting();
        doReturn(mDevice2).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();

        assertThat(mController.isSupportEndItem()).isFalse();
    }

    @Test
    public void isSupportEndItem_deviceIsBroadcastingAndConnectedToNonBleDevice_returnsTrue() {
        doReturn(true).when(sMediaOutputIndicatorWorker).isBroadcastSupported();
        doReturn(true).when(sMediaOutputIndicatorWorker).isDeviceBroadcasting();
        doReturn(mDevice2).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();

        assertThat(mController.isSupportEndItem()).isTrue();
    }

    @Test
    public void isSupportEndItem_deviceIsNotBroadcastingAndConnectedToNonBleDevice_returnsFalse() {
        doReturn(true).when(sMediaOutputIndicatorWorker).isBroadcastSupported();
        doReturn(false).when(sMediaOutputIndicatorWorker).isDeviceBroadcasting();
        doReturn(mDevice2).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();

        assertThat(mController.isSupportEndItem()).isFalse();
    }


    @Test
    public void getSliceEndItem_NotSupportEndItem_getsNullSliceAction() {
        doReturn(true).when(sMediaOutputIndicatorWorker).isBroadcastSupported();
        doReturn(false).when(sMediaOutputIndicatorWorker).isDeviceBroadcasting();
        doReturn(mDevice2).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();

        final SliceAction sliceAction = mController.getSliceEndItem(mContext);

        assertThat(sliceAction).isNull();
    }

    @Test
    public void getSliceEndItem_deviceIsBroadcasting_getsBroadcastIntent() {
        doReturn(true).when(sMediaOutputIndicatorWorker).isBroadcastSupported();
        doReturn(mDevice1).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();
        doReturn(true).when(sMediaOutputIndicatorWorker).isDeviceBroadcasting();
        doReturn(mMediaController).when(sMediaOutputIndicatorWorker)
                .getActiveLocalMediaController();

        final SliceAction sliceAction = mController.getSliceEndItem(mContext);

        final PendingIntent endItemPendingIntent = sliceAction.getAction();
        final PendingIntent expectedToggleIntent = getBroadcastIntent(
                MediaOutputConstants.ACTION_LAUNCH_MEDIA_OUTPUT_BROADCAST_DIALOG);
        assertThat(endItemPendingIntent).isEqualTo(expectedToggleIntent);
    }

    @Test
    public void getSliceEndItem_deviceIsNotBroadcasting_getsActivityIntent() {
        final MediaDevice device = mock(BluetoothMediaDevice.class);
        final CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        when(((BluetoothMediaDevice) device).getCachedDevice()).thenReturn(cachedDevice);
        when(device.isBLEDevice()).thenReturn(true);
        doReturn(true).when(sMediaOutputIndicatorWorker).isBroadcastSupported();
        doReturn(device).when(sMediaOutputIndicatorWorker).getCurrentConnectedMediaDevice();
        doReturn(false).when(sMediaOutputIndicatorWorker).isDeviceBroadcasting();
        doReturn(mMediaController).when(sMediaOutputIndicatorWorker)
                .getActiveLocalMediaController();

        final SliceAction sliceAction = mController.getSliceEndItem(mContext);

        final PendingIntent endItemPendingIntent = sliceAction.getAction();
        final PendingIntent expectedPendingIntent =
                getActivityIntent(ACTION_LAUNCH_BROADCAST_DIALOG);
        assertThat(endItemPendingIntent).isEqualTo(expectedPendingIntent);
    }

    @Implements(SliceBackgroundWorker.class)
    public static class ShadowSliceBackgroundWorker {

        @Implementation
        public static SliceBackgroundWorker getInstance(Uri uri) {
            return sMediaOutputIndicatorWorker;
        }
    }

    private PendingIntent getBroadcastIntent(String action) {
        final Intent intent = new Intent(action);
        intent.setPackage(MediaOutputConstants.SYSTEMUI_PACKAGE_NAME);
        return PendingIntent.getBroadcast(mContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent getActivityIntent(String action) {
        final Intent intent = new Intent(action);
        return PendingIntent.getActivity(mContext, 0 /* requestCode */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

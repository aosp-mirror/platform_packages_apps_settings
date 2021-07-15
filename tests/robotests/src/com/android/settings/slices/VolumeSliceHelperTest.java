/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.slices;

import static com.android.settings.slices.CustomSliceRegistry.VOLUME_SLICES_URI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;

import com.android.settings.notification.MediaVolumePreferenceController;
import com.android.settings.notification.RingVolumePreferenceController;
import com.android.settings.notification.VolumeSeekBarPreferenceController;
import com.android.settingslib.SliceBroadcastRelay;

import org.junit.After;
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
import org.robolectric.annotation.Resetter;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = VolumeSliceHelperTest.ShadowSliceBroadcastRelay.class)
public class VolumeSliceHelperTest {

    @Mock
    private ContentResolver mResolver;

    private Context mContext;
    private Intent mIntent;
    private VolumeSeekBarPreferenceController mMediaController;
    private VolumeSeekBarPreferenceController mRingController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getContentResolver()).thenReturn(mResolver);

        mMediaController = new MediaVolumePreferenceController(mContext);
        mRingController = new RingVolumePreferenceController(mContext);

        mIntent = createIntent(AudioManager.VOLUME_CHANGED_ACTION)
                .putExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 1)
                .putExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, 2)
                .putExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, mMediaController.getAudioStream());
    }

    @After
    public void cleanUp() {
        ShadowSliceBroadcastRelay.reset();
        VolumeSliceHelper.sRegisteredUri.clear();
        VolumeSliceHelper.sIntentFilter = null;
    }

    @Test
    public void registerIntentToUri_volumeController_shouldRegisterReceiver() {
        registerIntentToUri(mMediaController);

        assertThat(ShadowSliceBroadcastRelay.getRegisteredCount()).isEqualTo(1);
        assertThat(VolumeSliceHelper.sRegisteredUri)
                .containsKey((mMediaController.getSliceUri()));
    }

    @Test
    public void registerIntentToUri_doubleVolumeControllers_shouldRegisterReceiverOnce() {
        registerIntentToUri(mMediaController);

        registerIntentToUri(mRingController);

        assertThat(ShadowSliceBroadcastRelay.getRegisteredCount()).isEqualTo(1);
        assertThat(VolumeSliceHelper.sRegisteredUri)
                .containsKey((mRingController.getSliceUri()));
    }

    @Test
    public void unregisterUri_notFinalUri_shouldNotUnregisterReceiver() {
        registerIntentToUri(mMediaController);
        registerIntentToUri(mRingController);

        VolumeSliceHelper.unregisterUri(mContext, mMediaController.getSliceUri());

        assertThat(ShadowSliceBroadcastRelay.getRegisteredCount()).isEqualTo(1);
        assertThat(VolumeSliceHelper.sRegisteredUri)
                .doesNotContainKey((mMediaController.getSliceUri()));
    }

    @Test
    public void unregisterUri_finalUri_shouldUnregisterReceiver() {
        registerIntentToUri(mMediaController);

        VolumeSliceHelper.unregisterUri(mContext, mMediaController.getSliceUri());

        assertThat(ShadowSliceBroadcastRelay.getRegisteredCount()).isEqualTo(0);
        assertThat(VolumeSliceHelper.sRegisteredUri)
                .doesNotContainKey((mMediaController.getSliceUri()));
    }

    @Test
    public void unregisterUri_unregisterTwice_shouldUnregisterReceiverOnce() {
        registerIntentToUri(mMediaController);

        VolumeSliceHelper.unregisterUri(mContext, mMediaController.getSliceUri());
        VolumeSliceHelper.unregisterUri(mContext, mMediaController.getSliceUri());

        assertThat(ShadowSliceBroadcastRelay.getRegisteredCount()).isEqualTo(0);
    }

    @Test
    public void unregisterUri_notRegistered_shouldNotUnregisterReceiver() {
        registerIntentToUri(mMediaController);

        VolumeSliceHelper.unregisterUri(mContext, mRingController.getSliceUri());

        assertThat(ShadowSliceBroadcastRelay.getRegisteredCount()).isEqualTo(1);
        assertThat(VolumeSliceHelper.sRegisteredUri)
                .containsKey((mMediaController.getSliceUri()));
    }

    @Test
    public void onReceive_audioStreamRegistered_shouldNotifyChange() {
        registerIntentToUri(mMediaController);

        VolumeSliceHelper.onReceive(mContext, mIntent);

        verify(mResolver).notifyChange(mMediaController.getSliceUri(), null);
    }

    @Test
    public void onReceive_audioStreamNotRegistered_shouldNotNotifyChange() {
        VolumeSliceHelper.onReceive(mContext, mIntent);

        verify(mResolver, never()).notifyChange(mMediaController.getSliceUri(), null);
    }

    @Test
    public void onReceive_audioStreamNotMatched_shouldNotNotifyChange() {
        registerIntentToUri(mMediaController);
        mIntent.putExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, AudioManager.STREAM_DTMF);

        VolumeSliceHelper.onReceive(mContext, mIntent);

        verify(mResolver, never()).notifyChange(mMediaController.getSliceUri(), null);
    }

    @Test
    public void onReceive_mediaVolumeNotChanged_shouldNotNotifyChange() {
        mIntent.putExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 1)
                .putExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, 1);
        registerIntentToUri(mMediaController);

        VolumeSliceHelper.onReceive(mContext, mIntent);

        verify(mResolver, never()).notifyChange(mMediaController.getSliceUri(), null);
    }

    @Test
    public void onReceive_streamVolumeMuted_shouldNotifyChange() {
        final Intent intent = createIntent(AudioManager.STREAM_MUTE_CHANGED_ACTION)
                .putExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, mMediaController.getAudioStream());
        registerIntentToUri(mMediaController);
        registerIntentToUri(mRingController);

        VolumeSliceHelper.onReceive(mContext, intent);

        verify(mResolver).notifyChange(mMediaController.getSliceUri(), null);
    }

    @Test
    public void onReceive_streamDevicesChanged_shouldNotifyChange() {
        final Intent intent = createIntent(AudioManager.STREAM_DEVICES_CHANGED_ACTION)
                .putExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, mRingController.getAudioStream());
        registerIntentToUri(mMediaController);
        registerIntentToUri(mRingController);

        VolumeSliceHelper.onReceive(mContext, intent);

        verify(mResolver).notifyChange(mRingController.getSliceUri(), null);
    }

    @Test
    public void onReceive_primaryMutedChanged_shouldNotifyChangeAll() {
        final Intent intent = createIntent(AudioManager.MASTER_MUTE_CHANGED_ACTION);
        registerIntentToUri(mMediaController);
        registerIntentToUri(mRingController);

        VolumeSliceHelper.onReceive(mContext, intent);

        verify(mResolver).notifyChange(mMediaController.getSliceUri(), null);
        verify(mResolver).notifyChange(mRingController.getSliceUri(), null);
    }

    private void registerIntentToUri(VolumeSeekBarPreferenceController controller) {
        VolumeSliceHelper.registerIntentToUri(mContext, controller.getIntentFilter(),
                controller.getSliceUri(), controller.getAudioStream());
    }

    private Intent createIntent(String action) {
        return new Intent(action)
                .putExtra(SliceBroadcastRelay.EXTRA_URI, VOLUME_SLICES_URI.toString());
    }

    @Implements(SliceBroadcastRelay.class)
    public static class ShadowSliceBroadcastRelay {

        private static int sRegisteredCount;

        @Implementation
        public static void registerReceiver(Context context, Uri sliceUri,
                Class<? extends BroadcastReceiver> receiver, IntentFilter filter) {
            sRegisteredCount++;
        }

        @Implementation
        public static void unregisterReceivers(Context context, Uri sliceUri) {
            sRegisteredCount--;
        }

        @Resetter
        static void reset() {
            sRegisteredCount = 0;
        }

        static int getRegisteredCount() {
            return sRegisteredCount;
        }
    }
}

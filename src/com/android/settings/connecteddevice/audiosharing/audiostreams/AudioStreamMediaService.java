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

import static java.util.Collections.emptyList;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothVolumeControl;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioStreamMediaService extends Service {
    static final String BROADCAST_ID = "audio_stream_media_service_broadcast_id";
    static final String BROADCAST_TITLE = "audio_stream_media_service_broadcast_title";
    static final String DEVICES = "audio_stream_media_service_devices";
    private static final String TAG = "AudioStreamMediaService";
    private static final int NOTIFICATION_ID = 1;
    private static final int BROADCAST_CONTENT_TEXT = R.string.audio_streams_listening_now;
    @VisibleForTesting static final String LEAVE_BROADCAST_ACTION = "leave_broadcast_action";
    private static final String LEAVE_BROADCAST_TEXT = "Leave Broadcast";
    private static final String CHANNEL_ID = "bluetooth_notification_channel";
    private static final String DEFAULT_DEVICE_NAME = "";
    private static final int STATIC_PLAYBACK_DURATION = 100;
    private static final int STATIC_PLAYBACK_POSITION = 30;
    private static final int ZERO_PLAYBACK_SPEED = 0;
    private final PlaybackState.Builder mPlayStatePlayingBuilder =
            new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SEEK_TO)
                    .setState(
                            PlaybackState.STATE_PLAYING,
                            STATIC_PLAYBACK_POSITION,
                            ZERO_PLAYBACK_SPEED)
                    .addCustomAction(
                            LEAVE_BROADCAST_ACTION,
                            LEAVE_BROADCAST_TEXT,
                            com.android.settings.R.drawable.ic_clear);
    private final PlaybackState.Builder mPlayStatePausingBuilder =
            new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_SEEK_TO)
                    .setState(
                            PlaybackState.STATE_PAUSED,
                            STATIC_PLAYBACK_POSITION,
                            ZERO_PLAYBACK_SPEED)
                    .addCustomAction(
                            LEAVE_BROADCAST_ACTION,
                            LEAVE_BROADCAST_TEXT,
                            com.android.settings.R.drawable.ic_clear);

    private final MetricsFeatureProvider mMetricsFeatureProvider =
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean mIsMuted = new AtomicBoolean(false);
    // Set 25 as default as the volume range from `VolumeControlProfile` is from 0 to 255.
    // If the initial volume from `onDeviceVolumeChanged` is larger than zero (not muted), we will
    // override this value. Otherwise, we raise the volume to 25 when the play button is clicked.
    private final AtomicInteger mLatestPositiveVolume = new AtomicInteger(25);
    private final AtomicBoolean mHasStopped = new AtomicBoolean(false);
    private int mBroadcastId;
    @Nullable private List<BluetoothDevice> mDevices;
    @Nullable private LocalBluetoothManager mLocalBtManager;
    @Nullable private AudioStreamsHelper mAudioStreamsHelper;
    @Nullable private LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    @Nullable private VolumeControlProfile mVolumeControl;
    @Nullable private NotificationManager mNotificationManager;
    @Nullable private MediaSession mLocalSession;
    @VisibleForTesting @Nullable AudioStreamsBroadcastAssistantCallback mBroadcastAssistantCallback;
    @VisibleForTesting @Nullable BluetoothCallback mBluetoothCallback;
    @VisibleForTesting @Nullable BluetoothVolumeControl.Callback mVolumeControlCallback;
    @VisibleForTesting @Nullable MediaSession.Callback mMediaSessionCallback;

    @Override
    public void onCreate() {
        if (!BluetoothUtils.isAudioSharingEnabled()) {
            return;
        }

        super.onCreate();
        mLocalBtManager = Utils.getLocalBtManager(this);
        if (mLocalBtManager == null) {
            Log.w(TAG, "onCreate() : mLocalBtManager is null!");
            return;
        }

        mAudioStreamsHelper = new AudioStreamsHelper(mLocalBtManager);
        mLeBroadcastAssistant = mAudioStreamsHelper.getLeBroadcastAssistant();
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "onCreate() : mLeBroadcastAssistant is null!");
            return;
        }

        mNotificationManager = getSystemService(NotificationManager.class);
        if (mNotificationManager == null) {
            Log.w(TAG, "onCreate() : notificationManager is null!");
            return;
        }

        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            getString(com.android.settings.R.string.bluetooth),
                            NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        mBluetoothCallback = new BtCallback();
        mLocalBtManager.getEventManager().registerCallback(mBluetoothCallback);

        mVolumeControl = mLocalBtManager.getProfileManager().getVolumeControlProfile();
        if (mVolumeControl != null) {
            mVolumeControlCallback = new VolumeControlCallback();
            mVolumeControl.registerCallback(mExecutor, mVolumeControlCallback);
        }

        mBroadcastAssistantCallback = new AssistantCallback();
        mLeBroadcastAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        if (!BluetoothUtils.isAudioSharingEnabled()) {
            return;
        }
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().unregisterCallback(mBluetoothCallback);
        }
        if (mLeBroadcastAssistant != null && mBroadcastAssistantCallback != null) {
            mLeBroadcastAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
        }
        if (mVolumeControl != null && mVolumeControlCallback != null) {
            mVolumeControl.unregisterCallback(mVolumeControlCallback);
        }
        if (mLocalSession != null) {
            mLocalSession.release();
            mLocalSession = null;
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        if (intent == null) {
            Log.w(TAG, "Intent is null. Service will not start.");
            mHasStopped.set(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        mBroadcastId = intent.getIntExtra(BROADCAST_ID, -1);
        if (mBroadcastId == -1) {
            Log.w(TAG, "Invalid broadcast ID. Service will not start.");
            mHasStopped.set(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        var extra = intent.getParcelableArrayListExtra(DEVICES, BluetoothDevice.class);
        if (extra == null || extra.isEmpty()) {
            Log.w(TAG, "No device. Service will not start.");
            mHasStopped.set(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        mDevices = Collections.synchronizedList(extra);
        createLocalMediaSession(intent.getStringExtra(BROADCAST_TITLE));
        startForeground(NOTIFICATION_ID, buildNotification());
        // Reset in case the service is previously stopped but not yet destroyed.
        mHasStopped.set(false);
        return START_NOT_STICKY;
    }

    private void createLocalMediaSession(String title) {
        mLocalSession = new MediaSession(this, TAG);
        mLocalSession.setMetadata(
                new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, STATIC_PLAYBACK_DURATION)
                        .build());
        mLocalSession.setActive(true);
        mLocalSession.setPlaybackState(getPlaybackState());
        mMediaSessionCallback = new MediaSessionCallback();
        mLocalSession.setCallback(mMediaSessionCallback);
    }

    private PlaybackState getPlaybackState() {
        return mIsMuted.get() ? mPlayStatePausingBuilder.build() : mPlayStatePlayingBuilder.build();
    }

    private String getDeviceName() {
        if (mDevices == null || mDevices.isEmpty() || mLocalBtManager == null) {
            return DEFAULT_DEVICE_NAME;
        }

        CachedBluetoothDeviceManager manager = mLocalBtManager.getCachedDeviceManager();
        if (manager == null) {
            return DEFAULT_DEVICE_NAME;
        }

        CachedBluetoothDevice device = manager.findDevice(mDevices.get(0));
        return device != null ? device.getName() : DEFAULT_DEVICE_NAME;
    }

    private Notification buildNotification() {
        String deviceName = getDeviceName();
        Notification.MediaStyle mediaStyle =
                new Notification.MediaStyle()
                        .setMediaSession(
                                mLocalSession != null ? mLocalSession.getSessionToken() : null);
        if (deviceName != null && !deviceName.isEmpty()) {
            mediaStyle.setRemotePlaybackInfo(
                    deviceName, com.android.settingslib.R.drawable.ic_bt_le_audio, null);
        }
        Notification.Builder notificationBuilder =
                new Notification.Builder(this, CHANNEL_ID)
                        .setSmallIcon(com.android.settingslib.R.drawable.ic_bt_le_audio_sharing)
                        .setStyle(mediaStyle)
                        .setContentText(getString(BROADCAST_CONTENT_TEXT))
                        .setSilent(true);
        return notificationBuilder.build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class AssistantCallback extends AudioStreamsBroadcastAssistantCallback {
        @Override
        public void onSourceLost(int broadcastId) {
            super.onSourceLost(broadcastId);
            handleRemoveSource();
        }

        @Override
        public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
            super.onSourceRemoved(sink, sourceId, reason);
            handleRemoveSource();
        }

        private void handleRemoveSource() {
            var unused =
                    ThreadUtils.postOnBackgroundThread(
                            () -> {
                                List<BluetoothLeBroadcastReceiveState> connected =
                                        mAudioStreamsHelper == null
                                                ? emptyList()
                                                : mAudioStreamsHelper.getAllConnectedSources();
                                if (connected.stream()
                                        .map(BluetoothLeBroadcastReceiveState::getBroadcastId)
                                        .noneMatch(id -> id == mBroadcastId)) {
                                    mHasStopped.set(true);
                                    stopSelf();
                                }
                            });
        }
    }

    private class VolumeControlCallback implements BluetoothVolumeControl.Callback {
        @Override
        public void onDeviceVolumeChanged(
                @NonNull BluetoothDevice device, @IntRange(from = -255, to = 255) int volume) {
            if (mDevices == null || mDevices.isEmpty()) {
                Log.w(TAG, "active device or device has source is null!");
                return;
            }
            Log.d(
                    TAG,
                    "onDeviceVolumeChanged() bluetoothDevice : " + device + " volume: " + volume);
            if (mDevices.contains(device)) {
                if (volume == 0) {
                    mIsMuted.set(true);
                } else {
                    mIsMuted.set(false);
                    mLatestPositiveVolume.set(volume);
                }
                updateNotification(getPlaybackState());
            }
        }
    }

    private class BtCallback implements BluetoothCallback {
        @Override
        public void onBluetoothStateChanged(int bluetoothState) {
            if (BluetoothAdapter.STATE_OFF == bluetoothState) {
                Log.d(TAG, "onBluetoothStateChanged() : stopSelf");
                mHasStopped.set(true);
                stopSelf();
            }
        }

        @Override
        public void onProfileConnectionStateChanged(
                @NonNull CachedBluetoothDevice cachedDevice,
                @ConnectionState int state,
                int bluetoothProfile) {
            if (state == BluetoothAdapter.STATE_DISCONNECTED
                    && bluetoothProfile == BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT
                    && mDevices != null) {
                mDevices.remove(cachedDevice.getDevice());
                cachedDevice
                        .getMemberDevice()
                        .forEach(
                                m -> {
                                    // Check nullability to pass NullAway check
                                    if (mDevices != null) {
                                        mDevices.remove(m.getDevice());
                                    }
                                });
            }
            if (mDevices == null || mDevices.isEmpty()) {
                Log.d(TAG, "onProfileConnectionStateChanged() : stopSelf");
                mHasStopped.set(true);
                stopSelf();
            }
        }
    }

    private class MediaSessionCallback extends MediaSession.Callback {
        public void onSeekTo(long pos) {
            Log.d(TAG, "onSeekTo: " + pos);
            updateNotification(getPlaybackState());
        }

        @Override
        public void onPause() {
            if (mDevices == null || mDevices.isEmpty()) {
                Log.w(TAG, "active device or device has source is null!");
                return;
            }
            Log.d(
                    TAG,
                    "onPause() setting volume for device : " + mDevices.get(0) + " volume: " + 0);
            setDeviceVolume(mDevices.get(0), /* volume= */ 0);
        }

        @Override
        public void onPlay() {
            if (mDevices == null || mDevices.isEmpty()) {
                Log.w(TAG, "active device or device has source is null!");
                return;
            }
            Log.d(
                    TAG,
                    "onPlay() setting volume for device : "
                            + mDevices.get(0)
                            + " volume: "
                            + mLatestPositiveVolume.get());
            setDeviceVolume(mDevices.get(0), mLatestPositiveVolume.get());
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            Log.d(TAG, "onCustomAction: " + action);
            if (action.equals(LEAVE_BROADCAST_ACTION) && mAudioStreamsHelper != null) {
                mAudioStreamsHelper.removeSource(mBroadcastId);
                mMetricsFeatureProvider.action(
                        getApplicationContext(),
                        SettingsEnums.ACTION_AUDIO_STREAM_NOTIFICATION_LEAVE_BUTTON_CLICK);
            }
        }

        private void setDeviceVolume(BluetoothDevice device, int volume) {
            int event = SettingsEnums.ACTION_AUDIO_STREAM_NOTIFICATION_MUTE_BUTTON_CLICK;
            var unused =
                    ThreadUtils.postOnBackgroundThread(
                            () -> {
                                if (mVolumeControl != null) {
                                    mVolumeControl.setDeviceVolume(device, volume, true);
                                    mMetricsFeatureProvider.action(
                                            getApplicationContext(), event, volume == 0 ? 1 : 0);
                                }
                            });
        }
    }

    private void updateNotification(PlaybackState playbackState) {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            if (mLocalSession != null) {
                                mLocalSession.setPlaybackState(playbackState);
                                if (mNotificationManager != null && !mHasStopped.get()) {
                                    mNotificationManager.notify(
                                            NOTIFICATION_ID, buildNotification());
                                }
                            }
                        });
    }
}

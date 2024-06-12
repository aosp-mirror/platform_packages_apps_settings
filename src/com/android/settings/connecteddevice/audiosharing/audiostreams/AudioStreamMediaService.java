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

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioStreamMediaService extends Service {
    static final String BROADCAST_ID = "audio_stream_media_service_broadcast_id";
    static final String BROADCAST_TITLE = "audio_stream_media_service_broadcast_title";
    static final String DEVICES = "audio_stream_media_service_devices";
    private static final String TAG = "AudioStreamMediaService";
    private static final int NOTIFICATION_ID = 1;
    private static final int BROADCAST_CONTENT_TEXT = R.string.audio_streams_listening_now;
    private static final String LEAVE_BROADCAST_ACTION = "leave_broadcast_action";
    private static final String LEAVE_BROADCAST_TEXT = "Leave Broadcast";
    private static final String CHANNEL_ID = "bluetooth_notification_channel";
    private static final String DEFAULT_DEVICE_NAME = "";
    private static final int STATIC_PLAYBACK_DURATION = 100;
    private static final int STATIC_PLAYBACK_POSITION = 30;
    private static final int ZERO_PLAYBACK_SPEED = 0;
    private final AudioStreamsBroadcastAssistantCallback mBroadcastAssistantCallback =
            new AudioStreamsBroadcastAssistantCallback() {
                @Override
                public void onSourceLost(int broadcastId) {
                    super.onSourceLost(broadcastId);
                    if (broadcastId == mBroadcastId) {
                        stopSelf();
                    }
                }

                @Override
                public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
                    super.onSourceRemoved(sink, sourceId, reason);
                    if (mAudioStreamsHelper != null
                            && mAudioStreamsHelper.getAllConnectedSources().stream()
                                    .map(BluetoothLeBroadcastReceiveState::getBroadcastId)
                                    .noneMatch(id -> id == mBroadcastId)) {
                        stopSelf();
                    }
                }
            };

    private final BluetoothCallback mBluetoothCallback =
            new BluetoothCallback() {
                @Override
                public void onBluetoothStateChanged(int bluetoothState) {
                    if (BluetoothAdapter.STATE_OFF == bluetoothState) {
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
                        stopSelf();
                    }
                }
            };

    private final BluetoothVolumeControl.Callback mVolumeControlCallback =
            new BluetoothVolumeControl.Callback() {
                @Override
                public void onDeviceVolumeChanged(
                        @NonNull BluetoothDevice device,
                        @IntRange(from = -255, to = 255) int volume) {
                    if (mDevices == null || mDevices.isEmpty()) {
                        Log.w(TAG, "active device or device has source is null!");
                        return;
                    }
                    if (mDevices.contains(device)) {
                        Log.d(
                                TAG,
                                "onDeviceVolumeChanged() bluetoothDevice : "
                                        + device
                                        + " volume: "
                                        + volume);
                        if (volume == 0) {
                            mIsMuted = true;
                        } else {
                            mIsMuted = false;
                            mLatestPositiveVolume = volume;
                        }
                        if (mLocalSession != null) {
                            mLocalSession.setPlaybackState(getPlaybackState());
                            if (mNotificationManager != null) {
                                mNotificationManager.notify(NOTIFICATION_ID, buildNotification());
                            }
                        }
                    }
                }
            };

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
    private int mBroadcastId;
    @Nullable private ArrayList<BluetoothDevice> mDevices;
    @Nullable private LocalBluetoothManager mLocalBtManager;
    @Nullable private AudioStreamsHelper mAudioStreamsHelper;
    @Nullable private LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;
    @Nullable private VolumeControlProfile mVolumeControl;
    @Nullable private NotificationManager mNotificationManager;

    // Set 25 as default as the volume range from `VolumeControlProfile` is from 0 to 255.
    // If the initial volume from `onDeviceVolumeChanged` is larger than zero (not muted), we will
    // override this value. Otherwise, we raise the volume to 25 when the play button is clicked.
    private int mLatestPositiveVolume = 25;
    private boolean mIsMuted = false;
    @Nullable private MediaSession mLocalSession;

    @Override
    public void onCreate() {
        if (!AudioSharingUtils.isFeatureEnabled()) {
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
                            this.getString(com.android.settings.R.string.bluetooth),
                            NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        mLocalBtManager.getEventManager().registerCallback(mBluetoothCallback);

        mVolumeControl = mLocalBtManager.getProfileManager().getVolumeControlProfile();
        if (mVolumeControl != null) {
            mVolumeControl.registerCallback(mExecutor, mVolumeControlCallback);
        }

        mLeBroadcastAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (!AudioSharingUtils.isFeatureEnabled()) {
            return;
        }
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().unregisterCallback(mBluetoothCallback);
        }
        if (mLeBroadcastAssistant != null) {
            mLeBroadcastAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
        }
        if (mVolumeControl != null) {
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

        mBroadcastId = intent != null ? intent.getIntExtra(BROADCAST_ID, -1) : -1;
        if (mBroadcastId == -1) {
            Log.w(TAG, "Invalid broadcast ID. Service will not start.");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            mDevices = intent.getParcelableArrayListExtra(DEVICES, BluetoothDevice.class);
        }
        if (mDevices == null || mDevices.isEmpty()) {
            Log.w(TAG, "No device. Service will not start.");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null) {
            createLocalMediaSession(intent.getStringExtra(BROADCAST_TITLE));
            startForeground(NOTIFICATION_ID, buildNotification());
        }

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
        mLocalSession.setCallback(
                new MediaSession.Callback() {
                    public void onSeekTo(long pos) {
                        Log.d(TAG, "onSeekTo: " + pos);
                        if (mLocalSession != null) {
                            mLocalSession.setPlaybackState(getPlaybackState());
                            if (mNotificationManager != null) {
                                mNotificationManager.notify(NOTIFICATION_ID, buildNotification());
                            }
                        }
                    }

                    @Override
                    public void onPause() {
                        if (mDevices == null || mDevices.isEmpty()) {
                            Log.w(TAG, "active device or device has source is null!");
                            return;
                        }
                        Log.d(
                                TAG,
                                "onPause() setting volume for device : "
                                        + mDevices.get(0)
                                        + " volume: "
                                        + 0);
                        if (mVolumeControl != null) {
                            mVolumeControl.setDeviceVolume(mDevices.get(0), 0, true);
                            mMetricsFeatureProvider.action(
                                    getApplicationContext(),
                                    SettingsEnums
                                            .ACTION_AUDIO_STREAM_NOTIFICATION_MUTE_BUTTON_CLICK,
                                    1);
                        }
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
                                        + mLatestPositiveVolume);
                        if (mVolumeControl != null) {
                            mVolumeControl.setDeviceVolume(
                                    mDevices.get(0), mLatestPositiveVolume, true);
                        }
                        mMetricsFeatureProvider.action(
                                getApplicationContext(),
                                SettingsEnums.ACTION_AUDIO_STREAM_NOTIFICATION_MUTE_BUTTON_CLICK,
                                0);
                    }

                    @Override
                    public void onCustomAction(@NonNull String action, Bundle extras) {
                        Log.d(TAG, "onCustomAction: " + action);
                        if (action.equals(LEAVE_BROADCAST_ACTION) && mAudioStreamsHelper != null) {
                            mAudioStreamsHelper.removeSource(mBroadcastId);
                            mMetricsFeatureProvider.action(
                                    getApplicationContext(),
                                    SettingsEnums
                                            .ACTION_AUDIO_STREAM_NOTIFICATION_LEAVE_BUTTON_CLICK);
                        }
                    }
                });
    }

    private PlaybackState getPlaybackState() {
        return mIsMuted ? mPlayStatePausingBuilder.build() : mPlayStatePlayingBuilder.build();
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
                        .setContentText(this.getString(BROADCAST_CONTENT_TEXT))
                        .setSilent(true);
        return notificationBuilder.build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

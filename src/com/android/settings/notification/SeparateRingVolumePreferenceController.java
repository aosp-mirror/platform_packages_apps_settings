/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.ActivityThread;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.DeviceConfig;
import android.service.notification.NotificationListenerService;

import androidx.lifecycle.OnLifecycleEvent;

import com.android.internal.config.sysui.SystemUiDeviceConfigFlags;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.Set;

/**
 * This slider is used to represent ring volume when ring is separated from notification
 */
public class SeparateRingVolumePreferenceController extends
        RingerModeAffectedVolumePreferenceController {

    private static final String KEY_SEPARATE_RING_VOLUME = "separate_ring_volume";
    private static final String TAG = "SeparateRingVolumePreferenceController";

    private final RingReceiver mReceiver = new RingReceiver();
    private final H mHandler = new H();

    public SeparateRingVolumePreferenceController(Context context) {
        this(context, KEY_SEPARATE_RING_VOLUME);
    }

    public SeparateRingVolumePreferenceController(Context context, String key) {
        super(context, key, TAG);

        mNormalIconId = R.drawable.ic_ring_volume;
        mVibrateIconId = R.drawable.ic_volume_ringer_vibrate;
        mSilentIconId = R.drawable.ic_ring_volume_off;

        mSeparateNotification = isSeparateNotificationConfigEnabled();
        updateRingerMode();
    }

    /**
     * Show/hide settings
     */
    private void onDeviceConfigChange(DeviceConfig.Properties properties) {
        Set<String> changeSet = properties.getKeyset();
        if (changeSet.contains(SystemUiDeviceConfigFlags.VOLUME_SEPARATE_NOTIFICATION)) {
            boolean valueUpdated = readSeparateNotificationVolumeConfig();
            if (valueUpdated) {
                updateEffectsSuppressor();
                selectPreferenceIconState();
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    @Override
    public void onResume() {
        super.onResume();
        mReceiver.register(true);
        readSeparateNotificationVolumeConfig();
        DeviceConfig.addOnPropertiesChangedListener(DeviceConfig.NAMESPACE_SYSTEMUI,
                ActivityThread.currentApplication().getMainExecutor(), this::onDeviceConfigChange);
        updateEffectsSuppressor();
        selectPreferenceIconState();

        if (mPreference != null) {
            mPreference.setVisible(getAvailabilityStatus() == AVAILABLE);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    @Override
    public void onPause() {
        super.onPause();
        mReceiver.register(false);
        DeviceConfig.removeOnPropertiesChangedListener(this::onDeviceConfigChange);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SEPARATE_RING_VOLUME;
    }

    @Override
    public int getAvailabilityStatus() {
        boolean separateNotification = isSeparateNotificationConfigEnabled();
        return separateNotification && !mHelper.isSingleVolume()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getAudioStream() {
        return AudioManager.STREAM_RING;
    }

    @Override
    protected boolean hintsMatch(int hints) {
        return (hints & NotificationListenerService.HINT_HOST_DISABLE_CALL_EFFECTS) != 0
                || (hints & NotificationListenerService.HINT_HOST_DISABLE_EFFECTS) != 0;
    }



    private final class H extends Handler {
        private static final int UPDATE_EFFECTS_SUPPRESSOR = 1;
        private static final int UPDATE_RINGER_MODE = 2;

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_EFFECTS_SUPPRESSOR:
                    updateEffectsSuppressor();
                    break;
                case UPDATE_RINGER_MODE:
                    updateRingerMode();
                    break;
            }
        }
    }

    private class RingReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        public void register(boolean register) {
            if (mRegistered == register) return;
            if (register) {
                final IntentFilter filter = new IntentFilter();
                filter.addAction(NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED);
                filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
                mContext.registerReceiver(this, filter);
            } else {
                mContext.unregisterReceiver(this);
            }
            mRegistered = register;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED.equals(action)) {
                mHandler.sendEmptyMessage(H.UPDATE_EFFECTS_SUPPRESSOR);
            } else if (AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(H.UPDATE_RINGER_MODE);
            }
        }
    }

}

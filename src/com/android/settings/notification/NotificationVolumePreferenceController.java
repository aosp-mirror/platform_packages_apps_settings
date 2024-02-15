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

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.view.View;

import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * Update notification volume icon in Settings in response to user adjusting volume.
 */
public class NotificationVolumePreferenceController extends
        RingerModeAffectedVolumePreferenceController {

    private static final String KEY_NOTIFICATION_VOLUME = "notification_volume";
    private static final String TAG = "NotificationVolumePreferenceController";

    private final RingReceiver mReceiver = new RingReceiver();
    private final H mHandler = new H();

    public NotificationVolumePreferenceController(Context context) {
        this(context, KEY_NOTIFICATION_VOLUME);
    }

    public NotificationVolumePreferenceController(Context context, String key) {
        super(context, key, TAG);

        mNormalIconId =  R.drawable.ic_notifications;
        mVibrateIconId = R.drawable.ic_volume_ringer_vibrate;
        mSilentIconId = R.drawable.ic_notifications_off_24dp;

        if (updateRingerMode()) {
            updateEnabledState();
        }
    }

    /**
     * Allow for notification slider to be enabled in the scenario where the config switches on
     * while settings page is already on the screen by always configuring the preference, even if it
     * is currently inactive.
     */
    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mPreference == null) {
            setupVolPreference(screen);
        }

        updateEffectsSuppressor();
        selectPreferenceIconState();
        updateContentDescription();
        updateEnabledState();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    @Override
    public void onResume() {
        super.onResume();
        mReceiver.register(true);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    @Override
    public void onPause() {
        super.onPause();
        mReceiver.register(false);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_notification_volume)
                && !mHelper.isSingleVolume() ? (mRingerMode == AudioManager.RINGER_MODE_NORMAL
                ? AVAILABLE : DISABLED_DEPENDENT_SETTING) : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NOTIFICATION_VOLUME;
    }

    @Override
    public int getAudioStream() {
        return AudioManager.STREAM_NOTIFICATION;
    }

    @Override
    protected boolean hintsMatch(int hints) {
        boolean allEffectsDisabled =
                (hints & NotificationListenerService.HINT_HOST_DISABLE_EFFECTS) != 0;
        boolean notificationEffectsDisabled =
                (hints & NotificationListenerService.HINT_HOST_DISABLE_NOTIFICATION_EFFECTS) != 0;

        return allEffectsDisabled || notificationEffectsDisabled;
    }

    @Override
    protected int getEffectiveRingerMode() {
        if (mVibrator == null && mRingerMode == AudioManager.RINGER_MODE_VIBRATE) {
            return AudioManager.RINGER_MODE_SILENT;
        } else if (mRingerMode == AudioManager.RINGER_MODE_NORMAL) {
            if (mHelper.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0) {
                // Ring is in normal, but notification is in silent.
                return AudioManager.RINGER_MODE_SILENT;
            }
        }
        return mRingerMode;
    }

    @Override
    protected void updateContentDescription() {
        if (mPreference != null) {
            int ringerMode = getEffectiveRingerMode();
            if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                mPreference.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
                mPreference.updateContentDescription(
                        mContext.getString(
                                R.string.notification_volume_content_description_vibrate_mode));
            } else if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                mPreference.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
                mPreference.updateContentDescription(
                        mContext.getString(R.string.volume_content_description_silent_mode,
                                mPreference.getTitle()));
            } else {
                // Set a11y mode to none in order not to trigger talkback while changing
                // notification volume in normal mode.
                mPreference.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_NONE);
                mPreference.updateContentDescription(mPreference.getTitle());
            }
        }
    }

    private void updateEnabledState() {
        if (mPreference != null) {
            mPreference.setEnabled(mRingerMode == AudioManager.RINGER_MODE_NORMAL);
        }
    }

    private final class H extends Handler {
        private static final int UPDATE_EFFECTS_SUPPRESSOR = 1;
        private static final int UPDATE_RINGER_MODE = 2;
        private static final int NOTIFICATION_VOLUME_CHANGED = 3;

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
                    if (updateRingerMode()) {
                        updateEnabledState();
                    }
                    break;
                case NOTIFICATION_VOLUME_CHANGED:
                    selectPreferenceIconState();
                    updateContentDescription();
                    updateEnabledState();
                    break;
            }
        }
    }

    /**
     * For notification volume icon to be accurate, we need to listen to volume change as well.
     * That is because the icon can change from mute/vibrate to normal without ringer mode changing.
     */
    private class RingReceiver extends BroadcastReceiver {
        private boolean mRegistered;

        public void register(boolean register) {
            if (mRegistered == register) return;
            if (register) {
                final IntentFilter filter = new IntentFilter();
                filter.addAction(NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED);
                filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
                filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
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
            } else if (AudioManager.VOLUME_CHANGED_ACTION.equals(action)) {
                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (streamType == AudioManager.STREAM_NOTIFICATION) {
                    int streamValue = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE,
                            -1);
                    mHandler.obtainMessage(H.NOTIFICATION_VOLUME_CHANGED, streamValue, 0)
                            .sendToTarget();
                }
            }
        }
    }
}

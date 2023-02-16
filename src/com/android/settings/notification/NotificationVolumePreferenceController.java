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

import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.OnLifecycleEvent;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.Objects;

/**
 * Update notification volume icon in Settings in response to user adjusting volume
 */
public class NotificationVolumePreferenceController extends VolumeSeekBarPreferenceController {

    private static final String TAG = "NotificationVolumePreferenceController";
    private static final String KEY_NOTIFICATION_VOLUME = "notification_volume";

    private Vibrator mVibrator;
    private int mRingerMode = AudioManager.RINGER_MODE_NORMAL;
    private ComponentName mSuppressor;
    private final RingReceiver mReceiver = new RingReceiver();
    private final H mHandler = new H();
    private INotificationManager mNoMan;


    private int mMuteIcon;
    private final int mNormalIconId =  R.drawable.ic_notifications;
    private final int mVibrateIconId = R.drawable.ic_volume_ringer_vibrate;
    private final int mSilentIconId = R.drawable.ic_notifications_off_24dp;

    private final boolean mRingNotificationAliased;


    public NotificationVolumePreferenceController(Context context) {
        this(context, KEY_NOTIFICATION_VOLUME);
    }

    public NotificationVolumePreferenceController(Context context, String key) {
        super(context, key);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }

        mRingNotificationAliased = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_alias_ring_notif_stream_types);
        updateRingerMode();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    @Override
    public void onResume() {
        super.onResume();
        mReceiver.register(true);
        updateEffectsSuppressor();
        updatePreferenceIconAndSliderState();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    @Override
    public void onPause() {
        super.onPause();
        mReceiver.register(false);
    }

    @Override
    public int getAvailabilityStatus() {

        // Show separate notification slider if ring/notification are not aliased by AudioManager --
        // if they are, notification volume is controlled by RingVolumePreferenceController.
        return mContext.getResources().getBoolean(R.bool.config_show_notification_volume)
                && (!mRingNotificationAliased || !Utils.isVoiceCapable(mContext))
                && !mHelper.isSingleVolume()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_NOTIFICATION_VOLUME);
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NOTIFICATION_VOLUME;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public int getAudioStream() {
        return AudioManager.STREAM_NOTIFICATION;
    }

    @Override
    public int getMuteIcon() {
        return mMuteIcon;
    }

    private void updateRingerMode() {
        final int ringerMode = mHelper.getRingerModeInternal();
        if (mRingerMode == ringerMode) return;
        mRingerMode = ringerMode;
        updatePreferenceIconAndSliderState();
    }

    private void updateEffectsSuppressor() {
        final ComponentName suppressor = NotificationManager.from(mContext).getEffectsSuppressor();
        if (Objects.equals(suppressor, mSuppressor)) return;

        if (mNoMan == null) {
            mNoMan = INotificationManager.Stub.asInterface(
                    ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        }

        final int hints;
        try {
            hints = mNoMan.getHintsFromListenerNoToken();
        } catch (android.os.RemoteException exception) {
            Log.w(TAG, "updateEffectsSuppressor: " + exception.getLocalizedMessage());
            return;
        }

        if (hintsMatch(hints)) {

            mSuppressor = suppressor;
            if (mPreference != null) {
                final String text = SuppressorHelper.getSuppressionText(mContext, suppressor);
                mPreference.setSuppressionText(text);
            }
        }
    }

    @VisibleForTesting
    boolean hintsMatch(int hints) {
        boolean allEffectsDisabled =
                (hints & NotificationListenerService.HINT_HOST_DISABLE_EFFECTS) != 0;
        boolean notificationEffectsDisabled =
                (hints & NotificationListenerService.HINT_HOST_DISABLE_NOTIFICATION_EFFECTS) != 0;

        return allEffectsDisabled || notificationEffectsDisabled;
    }

    private void updatePreferenceIconAndSliderState() {
        if (mPreference != null) {
            if (mVibrator != null && mRingerMode == AudioManager.RINGER_MODE_VIBRATE) {
                mMuteIcon = mVibrateIconId;
                mPreference.showIcon(mVibrateIconId);
                mPreference.setEnabled(false);

            } else if (mRingerMode == AudioManager.RINGER_MODE_SILENT
                    || mVibrator == null && mRingerMode == AudioManager.RINGER_MODE_VIBRATE) {
                mMuteIcon = mSilentIconId;
                mPreference.showIcon(mSilentIconId);
                mPreference.setEnabled(false);
            } else { // ringmode normal: could be that we are still silent
                mPreference.setEnabled(true);
                if (mHelper.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0) {
                    // ring is in normal, but notification is in silent
                    mMuteIcon = mSilentIconId;
                    mPreference.showIcon(mSilentIconId);
                } else {
                    mPreference.showIcon(mNormalIconId);
                }
            }
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
                    updateRingerMode();
                    break;
                case NOTIFICATION_VOLUME_CHANGED:
                    updatePreferenceIconAndSliderState();
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

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

import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.os.Binder;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.provider.DeviceConfig;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.config.sysui.SystemUiDeviceConfigFlags;

import java.util.Objects;

/**
 * Shared functionality and interfaces for volume controllers whose state can change by ringer mode
 */
public abstract class RingerModeAffectedVolumePreferenceController extends
        VolumeSeekBarPreferenceController {

    private final String mTag;

    protected int mNormalIconId;
    protected int mVibrateIconId;
    protected int mSilentIconId;
    protected int mMuteIcon;

    protected Vibrator mVibrator;
    protected int mRingerMode = AudioManager.RINGER_MODE_NORMAL;
    protected ComponentName mSuppressor;
    protected boolean mSeparateNotification;
    protected INotificationManager mNoMan;

    private static final boolean CONFIG_SEPARATE_NOTIFICATION_DEFAULT_VAL = false;

    public RingerModeAffectedVolumePreferenceController(Context context, String key, String tag) {
        super(context, key);
        mTag = tag;
        mVibrator = mContext.getSystemService(Vibrator.class);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }
    }

    protected void updateEffectsSuppressor() {
        final ComponentName suppressor = NotificationManager.from(mContext).getEffectsSuppressor();
        if (Objects.equals(suppressor, mSuppressor)) return;

        if (mNoMan == null) {
            mNoMan = INotificationManager.Stub.asInterface(
                    ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        }

        final int hints;
        try {
            hints = mNoMan.getHintsFromListenerNoToken();
        } catch (android.os.RemoteException ex) {
            Log.w(mTag, "updateEffectsSuppressor: " + ex.getMessage());
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
    void setPreference(VolumeSeekBarPreference volumeSeekBarPreference) {
        mPreference = volumeSeekBarPreference;
    }

    @VisibleForTesting
    void setVibrator(Vibrator vibrator) {
        mVibrator = vibrator;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public int getMuteIcon() {
        return mMuteIcon;
    }

    protected boolean isSeparateNotificationConfigEnabled() {
        return Binder.withCleanCallingIdentity(()
                -> DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SYSTEMUI,
                SystemUiDeviceConfigFlags.VOLUME_SEPARATE_NOTIFICATION,
                CONFIG_SEPARATE_NOTIFICATION_DEFAULT_VAL));
    }

    /**
     * side effect: updates the cached value of the config
     * @return has the config changed?
     */
    protected boolean readSeparateNotificationVolumeConfig() {
        boolean newVal = isSeparateNotificationConfigEnabled();

        boolean valueUpdated = newVal != mSeparateNotification;
        if (valueUpdated) {
            mSeparateNotification = newVal;
        }

        return valueUpdated;
    }

    /**
     * Updates UI Icon in response to ringer mode changes.
     * @return whether the ringer mode has changed.
     */
    protected boolean updateRingerMode() {
        final int ringerMode = mHelper.getRingerModeInternal();
        if (mRingerMode == ringerMode) {
            return false;
        }
        mRingerMode = ringerMode;
        selectPreferenceIconState();
        return true;
    }

    /**
     * Switching among normal/mute/vibrate
     */
    protected void selectPreferenceIconState() {
        if (mPreference != null) {
            if (mRingerMode == AudioManager.RINGER_MODE_NORMAL) {
                mPreference.showIcon(mNormalIconId);
            } else {
                if (mRingerMode == AudioManager.RINGER_MODE_VIBRATE && mVibrator != null) {
                    mMuteIcon = mVibrateIconId;
                } else {
                    mMuteIcon = mSilentIconId;
                }
                mPreference.showIcon(getMuteIcon());
            }
        }
    }

    protected abstract boolean hintsMatch(int hints);

}

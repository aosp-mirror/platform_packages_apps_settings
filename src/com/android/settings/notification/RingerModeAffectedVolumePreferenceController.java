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
import android.os.ServiceManager;
import android.os.Vibrator;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

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
    protected INotificationManager mNoMan;

    public RingerModeAffectedVolumePreferenceController(Context context, String key, String tag) {
        super(context, key);
        mTag = tag;
        mVibrator = mContext.getSystemService(Vibrator.class);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }
        mVolumePreferenceListener = this::updateContentDescription;
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
        updateContentDescription();
        return true;
    }

    /**
     * Switching among normal/mute/vibrate
     */
    protected void selectPreferenceIconState() {
        if (mPreference != null) {
            int ringerMode = getEffectiveRingerMode();
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                mPreference.showIcon(mNormalIconId);
            } else {
                if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    mMuteIcon = mVibrateIconId;
                } else {
                    mMuteIcon = mSilentIconId;
                }
                mPreference.showIcon(getMuteIcon());
            }
        }
    }

    protected int getEffectiveRingerMode() {
        if (mVibrator == null && mRingerMode == AudioManager.RINGER_MODE_VIBRATE) {
            return AudioManager.RINGER_MODE_SILENT;
        }
        return mRingerMode;
    }

    protected void updateContentDescription() {
        if (mPreference != null) {
            int ringerMode = getEffectiveRingerMode();
            if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                mPreference.updateContentDescription(
                        mContext.getString(R.string.ringer_content_description_vibrate_mode));
            } else if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                mPreference.updateContentDescription(
                        mContext.getString(R.string.ringer_content_description_silent_mode));
            } else {
                mPreference.updateContentDescription(mPreference.getTitle());
            }
        }
    }

    protected abstract boolean hintsMatch(int hints);

}

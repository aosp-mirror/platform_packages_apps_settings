/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.VolumePreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Special preference type that allows configuration of both the ring volume and
 * notification volume.
 */
public class RingerVolumePreference extends VolumePreference implements
        CheckBox.OnCheckedChangeListener {
    private static final String TAG = "RingerVolumePreference";

    private CheckBox mNotificationsUseRingVolumeCheckbox;
    private SeekBarVolumizer mNotificationSeekBarVolumizer;
    private TextView mNotificationVolumeTitle;
    
    public RingerVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // The always visible seekbar is for ring volume
        setStreamType(AudioManager.STREAM_RING);
        
        setDialogLayoutResource(R.layout.preference_dialog_ringervolume);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
     
        mNotificationsUseRingVolumeCheckbox =
                (CheckBox) view.findViewById(R.id.same_notification_volume);
        mNotificationsUseRingVolumeCheckbox.setOnCheckedChangeListener(this);
        mNotificationsUseRingVolumeCheckbox.setChecked(Settings.System.getInt(
                getContext().getContentResolver(),
                Settings.System.NOTIFICATIONS_USE_RING_VOLUME, 1) == 1);
        
        final SeekBar seekBar = (SeekBar) view.findViewById(R.id.notification_volume_seekbar);
        mNotificationSeekBarVolumizer = new SeekBarVolumizer(getContext(), seekBar,
                AudioManager.STREAM_NOTIFICATION);
        
        mNotificationVolumeTitle = (TextView) view.findViewById(R.id.notification_volume_title);
        
        setNotificationVolumeVisibility(!mNotificationsUseRingVolumeCheckbox.isChecked());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        if (!positiveResult && mNotificationSeekBarVolumizer != null) {
            mNotificationSeekBarVolumizer.revertVolume();
        }
        
        cleanup();
    }

    @Override
    public void onActivityStop() {
        super.onActivityStop();
        cleanup();
    }
    
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setNotificationVolumeVisibility(!isChecked);
        
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.NOTIFICATIONS_USE_RING_VOLUME, isChecked ? 1 : 0);
        
        if (isChecked) {
            // The user wants the notification to be same as ring, so do a
            // one-time sync right now
            AudioManager audioManager = (AudioManager) getContext()
                    .getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION,
                    audioManager.getStreamVolume(AudioManager.STREAM_RING), 0);
        }
    }

    @Override
    protected void onSampleStarting(SeekBarVolumizer volumizer) {
        super.onSampleStarting(volumizer);
        
        if (mNotificationSeekBarVolumizer != null && volumizer != mNotificationSeekBarVolumizer) {
            mNotificationSeekBarVolumizer.stopSample();
        }
    }

    private void setNotificationVolumeVisibility(boolean visible) {
        if (mNotificationSeekBarVolumizer != null) {
            mNotificationSeekBarVolumizer.getSeekBar().setVisibility(
                    visible ? View.VISIBLE : View.GONE);
            mNotificationVolumeTitle.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    private void cleanup() {
        if (mNotificationSeekBarVolumizer != null) {
            Dialog dialog = getDialog();
            if (dialog != null && dialog.isShowing()) {
                // Stopped while dialog was showing, revert changes
                mNotificationSeekBarVolumizer.revertVolume();
            }
            mNotificationSeekBarVolumizer.stop();
            mNotificationSeekBarVolumizer = null;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        if (mNotificationSeekBarVolumizer != null) {
            mNotificationSeekBarVolumizer.onSaveInstanceState(myState.getVolumeStore());
        }
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (mNotificationSeekBarVolumizer != null) {
            mNotificationSeekBarVolumizer.onRestoreInstanceState(myState.getVolumeStore());
        }
    }

    private static class SavedState extends BaseSavedState {
        VolumeStore mVolumeStore = new VolumeStore();

        public SavedState(Parcel source) {
            super(source);
            mVolumeStore.volume = source.readInt();
            mVolumeStore.originalVolume = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mVolumeStore.volume);
            dest.writeInt(mVolumeStore.originalVolume);
        }

        VolumeStore getVolumeStore() {
            return mVolumeStore;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}

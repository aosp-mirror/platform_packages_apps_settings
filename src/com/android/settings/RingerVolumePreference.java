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

import static android.os.BatteryManager.BATTERY_STATUS_UNKNOWN;

import com.android.internal.telephony.TelephonyIntents;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.VolumePreference;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Special preference type that allows configuration of both the ring volume and
 * notification volume.
 */
public class RingerVolumePreference extends VolumePreference {
    private static final String TAG = "RingerVolumePreference";
    private static final int MSG_RINGER_MODE_CHANGED = 101;

    private SeekBarVolumizer [] mSeekBarVolumizer;

    // These arrays must all match in length and order
    private static final int[] SEEKBAR_ID = new int[] {
        R.id.media_volume_seekbar,
        R.id.ringer_volume_seekbar,
        R.id.notification_volume_seekbar,
        R.id.alarm_volume_seekbar
    };

    private static final int[] SEEKBAR_TYPE = new int[] {
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_ALARM
    };

    private static final int[] CHECKBOX_VIEW_ID = new int[] {
        R.id.media_mute_button,
        R.id.ringer_mute_button,
        R.id.notification_mute_button,
        R.id.alarm_mute_button
    };

    private static final int[] SEEKBAR_SECTION_ID = new int[] {
        R.id.media_section,
        R.id.ringer_section,
        R.id.notification_section,
        R.id.alarm_section
    };

    private static final int[] SEEKBAR_MUTED_RES_ID = new int[] {
        com.android.internal.R.drawable.ic_audio_vol_mute,
        com.android.internal.R.drawable.ic_audio_ring_notif_mute,
        com.android.internal.R.drawable.ic_audio_notification_mute,
        com.android.internal.R.drawable.ic_audio_alarm_mute
    };

    private static final int[] SEEKBAR_UNMUTED_RES_ID = new int[] {
        com.android.internal.R.drawable.ic_audio_vol,
        com.android.internal.R.drawable.ic_audio_ring_notif,
        com.android.internal.R.drawable.ic_audio_notification,
        com.android.internal.R.drawable.ic_audio_alarm
    };

    private ImageView[] mCheckBoxes = new ImageView[SEEKBAR_MUTED_RES_ID.length];
    private SeekBar[] mSeekBars = new SeekBar[SEEKBAR_ID.length];

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            updateSlidersAndMutedStates();
        }
    };

    @Override
    public void createActionButtons() {
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(null);
    }

    private void updateSlidersAndMutedStates() {
        for (int i = 0; i < SEEKBAR_TYPE.length; i++) {
            int streamType = SEEKBAR_TYPE[i];
            boolean muted = mAudioManager.isStreamMute(streamType);

            if (mCheckBoxes[i] != null) {
                if (((streamType == AudioManager.STREAM_RING) ||
                        (streamType == AudioManager.STREAM_NOTIFICATION)) &&
                        (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
                    mCheckBoxes[i].setImageResource(
                            com.android.internal.R.drawable.ic_audio_ring_notif_vibrate);
                } else {
                    mCheckBoxes[i].setImageResource(
                            muted ? SEEKBAR_MUTED_RES_ID[i] : SEEKBAR_UNMUTED_RES_ID[i]);
                }
            }
            if (mSeekBars[i] != null) {
                final int volume = mAudioManager.getStreamVolume(streamType);
                mSeekBars[i].setProgress(volume);
                if (streamType != mAudioManager.getMasterStreamType() && muted) {
                    mSeekBars[i].setEnabled(false);
                } else {
                    mSeekBars[i].setEnabled(true);
                }
            }
        }
    }

    private BroadcastReceiver mRingModeChangedReceiver;
    private AudioManager mAudioManager;

    //private SeekBarVolumizer mNotificationSeekBarVolumizer;
    //private TextView mNotificationVolumeTitle;

    public RingerVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // The always visible seekbar is for ring volume
        setStreamType(AudioManager.STREAM_RING);

        setDialogLayoutResource(R.layout.preference_dialog_ringervolume);
        setDialogTitle(null);

        mSeekBarVolumizer = new SeekBarVolumizer[SEEKBAR_ID.length];

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            SeekBar seekBar = (SeekBar) view.findViewById(SEEKBAR_ID[i]);
            mSeekBars[i] = seekBar;
            if (SEEKBAR_TYPE[i] == AudioManager.STREAM_MUSIC) {
                mSeekBarVolumizer[i] = new SeekBarVolumizer(getContext(), seekBar,
                        SEEKBAR_TYPE[i], getMediaVolumeUri(getContext()));
            } else {
                mSeekBarVolumizer[i] = new SeekBarVolumizer(getContext(), seekBar,
                        SEEKBAR_TYPE[i]);
            }
        }

        // Register callbacks for mute/unmute buttons
        for (int i = 0; i < mCheckBoxes.length; i++) {
            ImageView checkbox = (ImageView) view.findViewById(CHECKBOX_VIEW_ID[i]);
            mCheckBoxes[i] = checkbox;
        }

        final CheckBox linkCheckBox = (CheckBox) view.findViewById(R.id.link_ring_and_volume);

        final View ringerSection = view.findViewById(R.id.ringer_section);
        final View notificationSection = view.findViewById(R.id.notification_section);
        final View linkVolumesSection = view.findViewById(R.id.link_volumes_section);
        final TextView ringerDesc = (TextView) ringerSection
                .findViewById(R.id.ringer_description_text);

        if (Utils.isVoiceCapable(getContext())) {
            if (System.getInt(getContext().getContentResolver(),
                    System.VOLUME_LINK_NOTIFICATION, 1) == 1) {
                linkCheckBox.setChecked(true);
                notificationSection.setVisibility(View.GONE);
                ringerDesc.setText(R.string.volume_ring_description);
            } else {
                linkCheckBox.setChecked(false);
                notificationSection.setVisibility(View.VISIBLE);
                ringerDesc.setText(R.string.volume_ring_only_description);
            }

            linkCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        notificationSection.setVisibility(View.GONE);
                        ringerDesc.setText(R.string.volume_ring_description);
                        final int volume = mAudioManager.getStreamVolume(AudioSystem.STREAM_RING);
                        mAudioManager.setStreamVolume(AudioSystem.STREAM_NOTIFICATION, volume, 0);
                        Settings.System.putInt(buttonView.getContext().getContentResolver(),
                                Settings.System.VOLUME_LINK_NOTIFICATION, 1);
                    } else {
                        notificationSection.setVisibility(View.VISIBLE);
                        ringerDesc.setText(R.string.volume_ring_only_description);
                        Settings.System.putInt(buttonView.getContext().getContentResolver(),
                                Settings.System.VOLUME_LINK_NOTIFICATION, 0);
                    }
                    updateSlidersAndMutedStates();
                }

            });
        } else {
            ringerSection.setVisibility(View.GONE);
            linkVolumesSection.setVisibility(View.GONE);
        }

        // Load initial states from AudioManager
        updateSlidersAndMutedStates();

        // Listen for updates from AudioManager
        if (mRingModeChangedReceiver == null) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            mRingModeChangedReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    final String action = intent.getAction();
                    if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action)) {
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_RINGER_MODE_CHANGED, intent
                                .getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1), 0));
                    }
                }
            };
            getContext().registerReceiver(mRingModeChangedReceiver, filter);
        }

        boolean useMasterVolume = getContext().getResources().
                getBoolean(com.android.internal.R.bool.config_useMasterVolume);
        if (useMasterVolume) {
            // If config_useMasterVolume is true, all streams are treated as STREAM_MASTER.
            // So hide all except a stream.
            int id;
            if (Utils.isVoiceCapable(getContext())) {
                id = R.id.ringer_section;
            } else {
                id = R.id.media_section;
            }
            for (int i = 0; i < SEEKBAR_SECTION_ID.length; i++) {
                if (SEEKBAR_SECTION_ID[i] != id) {
                    view.findViewById(SEEKBAR_SECTION_ID[i]).setVisibility(View.GONE);
                }
            }
        } else {
            // Disable ringer or notifications if required
            int id = -1;
            if (!Utils.isVoiceCapable(getContext())) {
                id = R.id.ringer_section;
            } else if (System.getInt(getContext().getContentResolver(),
                    System.VOLUME_LINK_NOTIFICATION, 1) == 1) {
                id = R.id.notification_section;
            }
            if (id != -1){
                View hideSection = view.findViewById(id);
                hideSection.setVisibility(View.GONE);
            }
        }
    }

    private Uri getMediaVolumeUri(Context context) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + context.getPackageName()
                + "/" + R.raw.media_volume);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (!positiveResult) {
            for (SeekBarVolumizer vol : mSeekBarVolumizer) {
                if (vol != null) vol.revertVolume();
            }
        }
        cleanup();
    }

    @Override
    public void onActivityStop() {
        super.onActivityStop();

        for (SeekBarVolumizer vol : mSeekBarVolumizer) {
            if (vol != null) vol.stopSample();
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        boolean isdown = (event.getAction() == KeyEvent.ACTION_DOWN);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onSampleStarting(SeekBarVolumizer volumizer) {
        super.onSampleStarting(volumizer);
        for (SeekBarVolumizer vol : mSeekBarVolumizer) {
            if (vol != null && vol != volumizer) vol.stopSample();
        }
    }

    private void cleanup() {
        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            if (mSeekBarVolumizer[i] != null) {
                Dialog dialog = getDialog();
                if (dialog != null && dialog.isShowing()) {
                    // Stopped while dialog was showing, revert changes
                    mSeekBarVolumizer[i].revertVolume();
                }
                mSeekBarVolumizer[i].stop();
                mSeekBarVolumizer[i] = null;
            }
        }
        if (mRingModeChangedReceiver != null) {
            getContext().unregisterReceiver(mRingModeChangedReceiver);
            mRingModeChangedReceiver = null;
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
        VolumeStore[] volumeStore = myState.getVolumeStore(SEEKBAR_ID.length);
        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            SeekBarVolumizer vol = mSeekBarVolumizer[i];
            if (vol != null) {
                vol.onSaveInstanceState(volumeStore[i]);
            }
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
        VolumeStore[] volumeStore = myState.getVolumeStore(SEEKBAR_ID.length);
        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            SeekBarVolumizer vol = mSeekBarVolumizer[i];
            if (vol != null) {
                vol.onRestoreInstanceState(volumeStore[i]);
            }
        }
    }

    private static class SavedState extends BaseSavedState {
        VolumeStore [] mVolumeStore;

        public SavedState(Parcel source) {
            super(source);
            mVolumeStore = new VolumeStore[SEEKBAR_ID.length];
            for (int i = 0; i < SEEKBAR_ID.length; i++) {
                mVolumeStore[i] = new VolumeStore();
                mVolumeStore[i].volume = source.readInt();
                mVolumeStore[i].originalVolume = source.readInt();
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            for (int i = 0; i < SEEKBAR_ID.length; i++) {
                dest.writeInt(mVolumeStore[i].volume);
                dest.writeInt(mVolumeStore[i].originalVolume);
            }
        }

        VolumeStore[] getVolumeStore(int count) {
            if (mVolumeStore == null || mVolumeStore.length != count) {
                mVolumeStore = new VolumeStore[count];
                for (int i = 0; i < count; i++) {
                    mVolumeStore[i] = new VolumeStore();
                }
            }
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

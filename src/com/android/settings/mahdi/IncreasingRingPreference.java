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

package com.android.settings.mahdi;

import android.app.Dialog;
import android.content.Context;
import android.content.ContentResolver;
import android.media.AudioManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.VolumePreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;

public class IncreasingRingPreference extends VolumePreference implements
        CheckBox.OnCheckedChangeListener {
    private static final String TAG = "IncreasingRingPreference";

    private CheckBox mEnabledCheckbox;

    private TextView mMinVolumeTitle;
    private SeekBar mMinVolumeSeekBar;
    private TextView mRingVolumeNotice;

    private TextView mIntervalTitle;
    private Spinner mInterval;
    private int[] mIntervalValues;

    public IncreasingRingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setStreamType(AudioManager.STREAM_RING);

        setDialogLayoutResource(R.layout.preference_dialog_increasing_ring);
        setDialogIcon(R.drawable.ic_settings_sound);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        ContentResolver cr = getContext().getContentResolver();

        mEnabledCheckbox = (CheckBox) view.findViewById(R.id.increasing_ring);
        mMinVolumeTitle = (TextView) view.findViewById(R.id.increasing_ring_min_volume_title);
        mMinVolumeSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mRingVolumeNotice = (TextView) view.findViewById(R.id.increasing_ring_volume_notice);
        mIntervalTitle = (TextView) view.findViewById(R.id.increasing_ring_interval_title);
        mInterval = (Spinner) view.findViewById(R.id.increasing_ring_interval);
        mIntervalValues = getContext().getResources().getIntArray(R.array.increasing_ring_interval_values);

        mEnabledCheckbox.setOnCheckedChangeListener(this);
        mEnabledCheckbox.setChecked(Settings.System.getInt(cr, Settings.System.INCREASING_RING, 0) == 1);
        mMinVolumeSeekBar.setProgress(Settings.System.getInt(
                    cr, Settings.System.INCREASING_RING_MIN_VOLUME, 1));
        int interval = Settings.System.getInt(cr, Settings.System.INCREASING_RING_INTERVAL, 0);
        int index = 0;

        for (int i = 0; i < mIntervalValues.length; i++) {
            if (mIntervalValues[i] == interval) {
                index = i;
                break;
            }
        }
        mInterval.setSelection(index);

        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mMinVolumeSeekBar.setSecondaryProgress(am.getStreamVolume(AudioManager.STREAM_RING));

        updateVolumeNoticeVisibility(mMinVolumeSeekBar.getProgress());
        updateEnabledStates();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(false);

        if (positiveResult) {
            boolean checked = mEnabledCheckbox.isChecked();
            ContentResolver cr = getContext().getContentResolver();

            Settings.System.putInt(cr, Settings.System.INCREASING_RING, checked ? 1 : 0);
            Settings.System.putInt(cr, Settings.System.INCREASING_RING_INTERVAL,
                    mIntervalValues[mInterval.getSelectedItemPosition()]);
            Settings.System.putInt(cr,
                    Settings.System.INCREASING_RING_MIN_VOLUME,
                    mMinVolumeSeekBar.getProgress());
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        updateVolumeNoticeVisibility(mMinVolumeSeekBar.getProgress());
        updateEnabledStates();
    }

    @Override
    public boolean onVolumeChange(SeekBarVolumizer volumizer, int value) {
        boolean result = super.onVolumeChange(volumizer, value);
        if (result) {
            updateVolumeNoticeVisibility(value);
        }
        return result;
    }

    private void updateVolumeNoticeVisibility(int value) {
        boolean visible = value > mMinVolumeSeekBar.getSecondaryProgress();
        if (!mEnabledCheckbox.isChecked()) {
            visible = false;
        }
        mRingVolumeNotice.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateEnabledStates() {
        boolean enable = mEnabledCheckbox.isChecked();
        mMinVolumeTitle.setEnabled(enable);
        mMinVolumeSeekBar.setEnabled(enable);
        mRingVolumeNotice.setEnabled(enable);
        mIntervalTitle.setEnabled(enable);
        mInterval.setEnabled(enable);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        if (mEnabledCheckbox != null) {
            myState.mEnabled = mEnabledCheckbox.isChecked();
        }
        if (mInterval != null) {
            myState.mIntervalSelection = mInterval.getSelectedItemPosition();
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
        if (mEnabledCheckbox != null) {
            mEnabledCheckbox.setChecked(myState.mEnabled);
        }
        if (mInterval != null) {
            mInterval.setSelection(myState.mIntervalSelection);
        }
    }

    private static class SavedState extends BaseSavedState {
        boolean mEnabled;
        int mIntervalSelection;

        public SavedState(Parcel source) {
            super(source);
            mEnabled = source.readInt() != 0;
            mIntervalSelection = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mEnabled ? 1 : 0);
            dest.writeInt(mIntervalSelection);
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

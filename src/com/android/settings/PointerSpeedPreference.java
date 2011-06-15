/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.view.IWindowManager;
import android.view.View;
import android.widget.SeekBar;

public class PointerSpeedPreference extends SeekBarDialogPreference implements
        SeekBar.OnSeekBarChangeListener {
    private SeekBar mSeekBar;

    private int mOldSpeed;
    private boolean mRestoredOldState;

    private boolean mTouchInProgress;

    private static final int MIN_SPEED = -7;
    private static final int MAX_SPEED = 7;

    private ContentObserver mSpeedObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onSpeedChanged();
        }
    };

    public PointerSpeedPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.POINTER_SPEED), true,
                mSpeedObserver);

        mRestoredOldState = false;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = getSeekBar(view);
        mSeekBar.setMax(MAX_SPEED - MIN_SPEED);
        mOldSpeed = getSpeed(0);
        mSeekBar.setProgress(mOldSpeed - MIN_SPEED);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        if (!mTouchInProgress) {
            setSpeed(progress + MIN_SPEED);
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        mTouchInProgress = true;
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        mTouchInProgress = false;
        setSpeed(seekBar.getProgress() + MIN_SPEED);
    }

    private int getSpeed(int defaultValue) {
        int speed = defaultValue;
        try {
            speed = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.POINTER_SPEED);
        } catch (SettingNotFoundException snfe) {
        }
        return speed;
    }

    private void onSpeedChanged() {
        int speed = getSpeed(0);
        mSeekBar.setProgress(speed - MIN_SPEED);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        final ContentResolver resolver = getContext().getContentResolver();

        if (positiveResult) {
            Settings.System.putInt(resolver, Settings.System.POINTER_SPEED,
                    mSeekBar.getProgress() + MIN_SPEED);
        } else {
            restoreOldState();
        }

        resolver.unregisterContentObserver(mSpeedObserver);
    }

    private void restoreOldState() {
        if (mRestoredOldState) return;

        setSpeed(mOldSpeed);
        mRestoredOldState = true;
    }

    private void setSpeed(int speed) {
        try {
            IWindowManager wm = IWindowManager.Stub.asInterface(
                    ServiceManager.getService("window"));
            if (wm != null) {
                wm.setPointerSpeed(speed);
            }
        } catch (RemoteException e) {
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) return superState;

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.progress = mSeekBar.getProgress();
        myState.oldSpeed = mOldSpeed;

        // Restore the old state when the activity or dialog is being paused
        restoreOldState();
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
        mOldSpeed = myState.oldSpeed;
        mSeekBar.setProgress(myState.progress);
        setSpeed(myState.progress + MIN_SPEED);
    }

    private static class SavedState extends BaseSavedState {
        int progress;
        int oldSpeed;

        public SavedState(Parcel source) {
            super(source);
            progress = source.readInt();
            oldSpeed = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(progress);
            dest.writeInt(oldSpeed);
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


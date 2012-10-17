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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

public class BrightnessPreference extends SeekBarDialogPreference implements
        SeekBar.OnSeekBarChangeListener, CheckBox.OnCheckedChangeListener {
    // If true, enables the use of the screen auto-brightness adjustment setting.
    private static final boolean USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT =
            PowerManager.useScreenAutoBrightnessAdjustmentFeature();

    private final int mScreenBrightnessMinimum;
    private final int mScreenBrightnessMaximum;

    private SeekBar mSeekBar;
    private CheckBox mCheckBox;

    private int mOldBrightness;
    private int mOldAutomatic;

    private boolean mAutomaticAvailable;
    private boolean mAutomaticMode;

    private int mCurBrightness = -1;

    private boolean mRestoredOldState;

    private static final int SEEK_BAR_RANGE = 10000;

    private ContentObserver mBrightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mCurBrightness = -1;
            onBrightnessChanged();
        }
    };

    private ContentObserver mBrightnessModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onBrightnessModeChanged();
        }
    };

    public BrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mScreenBrightnessMinimum = pm.getMinimumScreenBrightnessSetting();
        mScreenBrightnessMaximum = pm.getMaximumScreenBrightnessSetting();

        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);

        setDialogLayoutResource(R.layout.preference_dialog_brightness);
        setDialogIcon(R.drawable.ic_settings_display);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true,
                mBrightnessObserver);

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true,
                mBrightnessModeObserver);
        mRestoredOldState = false;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = getSeekBar(view);
        mSeekBar.setMax(SEEK_BAR_RANGE);
        mOldBrightness = getBrightness();
        mSeekBar.setProgress(mOldBrightness);

        mCheckBox = (CheckBox)view.findViewById(R.id.automatic_mode);
        if (mAutomaticAvailable) {
            mCheckBox.setOnCheckedChangeListener(this);
            mOldAutomatic = getBrightnessMode(0);
            mAutomaticMode = mOldAutomatic == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            mCheckBox.setChecked(mAutomaticMode);
            mSeekBar.setEnabled(!mAutomaticMode || USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT);
        } else {
            mCheckBox.setVisibility(View.GONE);
            mSeekBar.setEnabled(true);
        }
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        setBrightness(progress, false);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        // NA
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // NA
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setMode(isChecked ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        mSeekBar.setProgress(getBrightness());
        mSeekBar.setEnabled(!mAutomaticMode || USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT);
        setBrightness(mSeekBar.getProgress(), false);
    }

    private int getBrightness() {
        int mode = getBrightnessMode(0);
        float brightness = 0;
        if (USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT
                && mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            brightness = Settings.System.getFloat(getContext().getContentResolver(),
                    Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, 0);
            brightness = (brightness+1)/2;
        } else {
            if (mCurBrightness < 0) {
                brightness = Settings.System.getInt(getContext().getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 100);
            } else {
                brightness = mCurBrightness;
            }
            brightness = (brightness - mScreenBrightnessMinimum)
                    / (mScreenBrightnessMaximum - mScreenBrightnessMinimum);
        }
        return (int)(brightness*SEEK_BAR_RANGE);
    }

    private int getBrightnessMode(int defaultValue) {
        int brightnessMode = defaultValue;
        try {
            brightnessMode = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (SettingNotFoundException snfe) {
        }
        return brightnessMode;
    }

    private void onBrightnessChanged() {
        mSeekBar.setProgress(getBrightness());
    }

    private void onBrightnessModeChanged() {
        boolean checked = getBrightnessMode(0)
                == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        mCheckBox.setChecked(checked);
        mSeekBar.setProgress(getBrightness());
        mSeekBar.setEnabled(!checked || USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        final ContentResolver resolver = getContext().getContentResolver();

        if (positiveResult) {
            setBrightness(mSeekBar.getProgress(), true);
        } else {
            restoreOldState();
        }

        resolver.unregisterContentObserver(mBrightnessObserver);
        resolver.unregisterContentObserver(mBrightnessModeObserver);
    }

    private void restoreOldState() {
        if (mRestoredOldState) return;

        if (mAutomaticAvailable) {
            setMode(mOldAutomatic);
        }
        setBrightness(mOldBrightness, false);
        mRestoredOldState = true;
        mCurBrightness = -1;
    }

    private void setBrightness(int brightness, boolean write) {
        if (mAutomaticMode) {
            if (USE_SCREEN_AUTO_BRIGHTNESS_ADJUSTMENT) {
                float valf = (((float)brightness*2)/SEEK_BAR_RANGE) - 1.0f;
                try {
                    IPowerManager power = IPowerManager.Stub.asInterface(
                            ServiceManager.getService("power"));
                    if (power != null) {
                        power.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(valf);
                    }
                    if (write) {
                        final ContentResolver resolver = getContext().getContentResolver();
                        Settings.System.putFloat(resolver,
                                Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, valf);
                    }
                } catch (RemoteException doe) {
                }
            }
        } else {
            int range = (mScreenBrightnessMaximum - mScreenBrightnessMinimum);
            brightness = (brightness * range)/SEEK_BAR_RANGE + mScreenBrightnessMinimum;
            try {
                IPowerManager power = IPowerManager.Stub.asInterface(
                        ServiceManager.getService("power"));
                if (power != null) {
                    power.setTemporaryScreenBrightnessSettingOverride(brightness);
                }
                if (write) {
                    mCurBrightness = -1;
                    final ContentResolver resolver = getContext().getContentResolver();
                    Settings.System.putInt(resolver,
                            Settings.System.SCREEN_BRIGHTNESS, brightness);
                } else {
                    mCurBrightness = brightness;
                }
            } catch (RemoteException doe) {
            }
        }
    }

    private void setMode(int mode) {
        mAutomaticMode = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) return superState;

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.automatic = mCheckBox.isChecked();
        myState.progress = mSeekBar.getProgress();
        myState.oldAutomatic = mOldAutomatic == 1;
        myState.oldProgress = mOldBrightness;
        myState.curBrightness = mCurBrightness;

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
        mOldBrightness = myState.oldProgress;
        mOldAutomatic = myState.oldAutomatic ? 1 : 0;
        setMode(myState.automatic ? 1 : 0);
        setBrightness(myState.progress, false);
        mCurBrightness = myState.curBrightness;
    }

    private static class SavedState extends BaseSavedState {

        boolean automatic;
        boolean oldAutomatic;
        int progress;
        int oldProgress;
        int curBrightness;

        public SavedState(Parcel source) {
            super(source);
            automatic = source.readInt() == 1;
            progress = source.readInt();
            oldAutomatic = source.readInt() == 1;
            oldProgress = source.readInt();
            curBrightness = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(automatic ? 1 : 0);
            dest.writeInt(progress);
            dest.writeInt(oldAutomatic ? 1 : 0);
            dest.writeInt(oldProgress);
            dest.writeInt(curBrightness);
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


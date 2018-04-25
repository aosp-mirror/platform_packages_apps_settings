/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settingslib.CustomDialogPreference;

import java.util.ArrayList;

public class HotspotApBandSelectionPreference extends CustomDialogPreference implements
        CompoundButton.OnCheckedChangeListener, DialogInterface.OnShowListener {
    private static final int UNSET = Integer.MIN_VALUE;

    @VisibleForTesting
    static final String KEY_CHECKED_BANDS = "checked_bands";
    @VisibleForTesting
    static final String KEY_HOTSPOT_SUPER_STATE = "hotspot_super_state";

    @VisibleForTesting
    CheckBox mBox2G;
    @VisibleForTesting
    CheckBox mBox5G;
    @VisibleForTesting
    ArrayList<Integer> mRestoredBands;
    @VisibleForTesting
    boolean mShouldRestore;

    private String[] mBandEntries;
    private int mExistingConfigValue = UNSET;

    public HotspotApBandSelectionPreference(Context context) {
        super(context);
    }

    public HotspotApBandSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HotspotApBandSelectionPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HotspotApBandSelectionPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState myState = (SavedState) state;

        super.onRestoreInstanceState(myState.getSuperState());

        mShouldRestore = myState.shouldRestore;
        if (mShouldRestore) {
            mRestoredBands = new ArrayList<>();
            if (myState.enabled2G) {
                mRestoredBands.add(WifiConfiguration.AP_BAND_2GHZ);
            }
            if (myState.enabled5G) {
                mRestoredBands.add(WifiConfiguration.AP_BAND_5GHZ);
            }
        } else {
            mRestoredBands = null;
        }
        updatePositiveButton();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        final Context context = getContext();

        // Register so we can adjust the buttons if needed once the dialog is available.
        setOnShowListener(this);

        mBandEntries = context.getResources().getStringArray(R.array.wifi_ap_band_config_full);
        // add a checkbox for every band entry.
        addApBandViews((LinearLayout) view);
        // try to update the button just in case we already missed the onShow call.
        updatePositiveButton();
        // clear any saved state so it doesn't leak across multiple rotations/dialog closings
        mRestoredBands = null;
        mShouldRestore = false;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        SavedState myState = new SavedState(superState);
        myState.shouldRestore = getDialog() != null;
        myState.enabled2G = mBox2G != null && mBox2G.isChecked();
        myState.enabled5G = mBox5G != null && mBox5G.isChecked();
        return myState;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!(buttonView instanceof CheckBox)) {
            return;
        }
        updatePositiveButton();
    }

    @Override
    protected void onClick(DialogInterface dialog, int which) {
        // we only want to persist our enabled bands if apply is clicked
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mBox2G.isChecked() || mBox5G.isChecked()) {
                int wifiBand = getWifiBand();
                mExistingConfigValue = wifiBand;
                callChangeListener(wifiBand);
            }
        }
    }

    /**
     * Used to set the band selection for the preference if one already exists
     * @param band the band to set it to from {@link WifiConfiguration}
     */
    public void setExistingConfigValue(int band) {
        mExistingConfigValue = band;
    }

    private void addApBandViews(LinearLayout view) {
        mBox2G = view.findViewById(R.id.box_2g);
        mBox2G.setText(mBandEntries[WifiConfiguration.AP_BAND_2GHZ]);
        mBox2G.setChecked(restoreBandIfNeeded(WifiConfiguration.AP_BAND_2GHZ));
        mBox2G.setOnCheckedChangeListener(this);

        mBox5G = view.findViewById(R.id.box_5g);
        mBox5G.setText(mBandEntries[WifiConfiguration.AP_BAND_5GHZ]);
        mBox5G.setChecked(restoreBandIfNeeded(WifiConfiguration.AP_BAND_5GHZ));
        mBox5G.setOnCheckedChangeListener(this);
    }

    private boolean restoreBandIfNeeded(int band) {
        // Only use the provided config if we aren't restoring, restore if state available
        return (isBandPreviouslySelected(band) && !mShouldRestore)
                || (mShouldRestore && mRestoredBands.contains(band));
    }

    private void updatePositiveButton() {
        AlertDialog dialog = (AlertDialog) getDialog();
        Button button = dialog == null ? null : dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null && mBox5G != null && mBox2G != null) {
            button.setEnabled(mBox2G.isChecked() || mBox5G.isChecked());
        }
    }

    @VisibleForTesting
    int getWifiBand() {
        final boolean checked_2g = mBox2G.isChecked();
        final boolean checked_5g = mBox5G.isChecked();
        if (checked_2g && checked_5g) {
            return WifiConfiguration.AP_BAND_ANY;
        } else if (checked_2g && !checked_5g) {
            return WifiConfiguration.AP_BAND_2GHZ;
        } else if (checked_5g && !checked_2g) {
            return WifiConfiguration.AP_BAND_5GHZ;
        } else {
            throw new IllegalStateException("Wifi Config only supports selecting one or all bands");
        }
    }

    private boolean isBandPreviouslySelected(int bandIndex) {
        switch(mExistingConfigValue) {
            case WifiConfiguration.AP_BAND_ANY:
                return true;
            case WifiConfiguration.AP_BAND_2GHZ:
                return bandIndex == 0;
            case WifiConfiguration.AP_BAND_5GHZ:
                return bandIndex == 1;
            case UNSET:
            default:
                return false;
        }
    }

    @Override
    public void onShow(DialogInterface dialog) {
        updatePositiveButton();
    }

    private static class SavedState extends BaseSavedState {
        boolean shouldRestore;
        boolean enabled2G;
        boolean enabled5G;

        public SavedState(Parcelable source) {
            super(source);
        }

        private SavedState(Parcel in) {
            super(in);
            shouldRestore =  in.readByte() == 1;
            enabled2G = in.readByte() == 1;
            enabled5G = in.readByte() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (shouldRestore ? 1 : 0));
            dest.writeByte((byte) (enabled2G ? 1: 0));
            dest.writeByte((byte) (enabled5G ? 1 : 0));
        }

        @Override
        public String toString() {
            return "HotspotApBandSelectionPreference.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " shouldRestore=" + shouldRestore
                    + " enabled2G=" + enabled2G
                    + " enabled5G=" + enabled5G + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}

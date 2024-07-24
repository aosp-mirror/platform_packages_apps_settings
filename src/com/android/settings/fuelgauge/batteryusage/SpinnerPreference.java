/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.widget.SettingsSpinnerAdapter;

/** A preference which contains a spinner. */
public class SpinnerPreference extends Preference {
    private static final String TAG = "SpinnerPreference";

    private AdapterView.OnItemSelectedListener mOnItemSelectedListener;

    @VisibleForTesting Spinner mSpinner;
    @VisibleForTesting String[] mItems;
    @VisibleForTesting int mSavedSpinnerPosition;

    public SpinnerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_spinner);
    }

    void initializeSpinner(
            String[] items, AdapterView.OnItemSelectedListener onItemSelectedListener) {
        mItems = items;
        mOnItemSelectedListener = onItemSelectedListener;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        if (mSpinner != null) {
            return;
        }

        mSpinner = (Spinner) view.findViewById(R.id.spinner);
        mSpinner.setAdapter(new SpinnerAdapter(getContext(), mItems));
        mSpinner.setSelection(mSavedSpinnerPosition);
        if (mOnItemSelectedListener != null) {
            mSpinner.setOnItemSelectedListener(mOnItemSelectedListener);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        if (mSpinner == null) {
            return super.onSaveInstanceState();
        }
        Log.d(TAG, "onSaveInstanceState() spinnerPosition=" + mSpinner.getSelectedItemPosition());
        return new SavedState(super.onSaveInstanceState(), mSpinner.getSelectedItemPosition());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || state == BaseSavedState.EMPTY_STATE) {
            super.onRestoreInstanceState(state);
            return;
        }
        if (!(state instanceof SavedState)) {
            // To avoid the IllegalArgumentException, return the BaseSavedState.EMPTY_STATE.
            super.onRestoreInstanceState(BaseSavedState.EMPTY_STATE);
            return;
        }
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mSavedSpinnerPosition = savedState.getSpinnerPosition();
        if (mOnItemSelectedListener != null) {
            mOnItemSelectedListener.onItemSelected(
                    /* parent= */ null,
                    /* view= */ null,
                    savedState.getSpinnerPosition(),
                    /* id= */ 0);
        }
        Log.d(TAG, "onRestoreInstanceState() spinnerPosition=" + savedState.getSpinnerPosition());
    }

    @VisibleForTesting
    static class SavedState extends BaseSavedState {
        private int mSpinnerPosition;

        SavedState(Parcelable superState, int spinnerPosition) {
            super(superState);
            mSpinnerPosition = spinnerPosition;
        }

        int getSpinnerPosition() {
            return mSpinnerPosition;
        }
    }

    private static class SpinnerAdapter extends SettingsSpinnerAdapter<CharSequence> {
        private final String[] mItems;

        SpinnerAdapter(Context context, String[] items) {
            super(context);
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.length;
        }

        @Override
        public CharSequence getItem(int position) {
            return mItems[position];
        }
    }
}

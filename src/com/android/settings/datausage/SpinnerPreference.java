/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.settingsspinner.SettingsSpinner;

public class SpinnerPreference extends Preference implements CycleAdapter.SpinnerInterface {

    private CycleAdapter mAdapter;
    private AdapterView.OnItemSelectedListener mListener;
    private Object mCurrentObject;
    private int mPosition;

    public SpinnerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.data_usage_cycles);
    }

    @Override
    public void setAdapter(CycleAdapter cycleAdapter) {
        mAdapter = cycleAdapter;
        notifyChanged();
    }

    @Override
    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
        mListener = listener;
    }

    @Override
    public Object getSelectedItem() {
        return mCurrentObject;
    }

    @Override
    public void setSelection(int position) {
        mPosition = position;
        mCurrentObject = mAdapter.getItem(mPosition);
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        SettingsSpinner spinner = (SettingsSpinner) holder.findViewById(R.id.cycles_spinner);
        spinner.setAdapter(mAdapter);
        spinner.setSelection(mPosition);
        spinner.setOnItemSelectedListener(mOnSelectedListener);
    }

    @Override
    protected void performClick(View view) {
        view.findViewById(R.id.cycles_spinner).performClick();
    }

    private final AdapterView.OnItemSelectedListener mOnSelectedListener
            = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (mPosition == position) return;
            mPosition = position;
            mCurrentObject = mAdapter.getItem(position);
            mListener.onItemSelected(parent, view, position, id);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            mListener.onNothingSelected(parent);
        }
    };
}

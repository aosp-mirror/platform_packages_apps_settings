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

import android.annotation.Nullable;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class SpinnerPreference extends Preference implements CycleAdapter.SpinnerInterface {

    private CycleAdapter mAdapter;
    @Nullable
    private AdapterView.OnItemSelectedListener mListener;
    private Object mCurrentObject;
    private int mPosition;
    private View mItemView;
    private boolean mItemViewVisible = false;

    public SpinnerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.data_usage_cycles);
    }

    @Override
    public void setAdapter(CycleAdapter cycleAdapter) {
        mAdapter = cycleAdapter;
        notifyChanged();
    }

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
        mItemView = holder.itemView;
        mItemView.setVisibility(mItemViewVisible ? View.VISIBLE : View.INVISIBLE);
        Spinner spinner = (Spinner) holder.findViewById(R.id.cycles_spinner);
        spinner.setAdapter(mAdapter);
        spinner.setSelection(mPosition);
        spinner.setOnItemSelectedListener(mOnSelectedListener);
    }

    void setHasCycles(boolean hasData) {
        setVisible(hasData);
        if (hasData) {
            mItemViewVisible = true;
            if (mItemView != null) {
                mItemView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void performClick(View view) {
        view.findViewById(R.id.cycles_spinner).performClick();
    }

    private final AdapterView.OnItemSelectedListener mOnSelectedListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(
                        AdapterView<?> parent, View view, int position, long id) {
                    mPosition = position;
                    mCurrentObject = mAdapter.getItem(position);
                    if (mListener != null) {
                        mListener.onItemSelected(parent, view, position, id);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    if (mListener != null) {
                        mListener.onNothingSelected(parent);
                    }
                }
            };
}

/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class DropDownPreference extends ListPreference {

    private final Context mContext;
    private final ArrayAdapter<String> mAdapter;
    private final Spinner mSpinner;

    public DropDownPreference(Context context) {
        this(context, null);
    }

    public DropDownPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mAdapter = new ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_dropdown_item);

        mSpinner = new Spinner(mContext);

        mSpinner.setVisibility(View.INVISIBLE);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position >= 0) {
                    String value = getEntryValues()[position].toString();
                    if (!value.equals(getValue()) && callChangeListener(value)) {
                        setValue(value);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // noop
            }
        });
        setPersistent(false);
        updateEntries();
    }

    @Override
    protected void onClick() {
        mSpinner.performClick();
    }

    public void setDropDownWidth(int dimenResId) {
        mSpinner.setDropDownWidth(mContext.getResources().getDimensionPixelSize(dimenResId));
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);
        updateEntries();
    }

    private void updateEntries() {
        mAdapter.clear();
        if (getEntries() != null) {
            for (CharSequence c : getEntries()) {
                mAdapter.add(c.toString());
            }
        }
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        mSpinner.setSelection(findIndexOfValue(getValue()));
        setSummary(getEntry());
    }

    public void setValueIndex(int index) {
        setValue(getEntryValues()[index].toString());
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (view.equals(mSpinner.getParent())) return;
        if (mSpinner.getParent() != null) {
            ((ViewGroup) mSpinner.getParent()).removeView(mSpinner);
        }
        final ViewGroup vg = (ViewGroup) view.itemView;
        vg.addView(mSpinner, 0);
        final ViewGroup.LayoutParams lp = mSpinner.getLayoutParams();
        lp.width = 0;
        mSpinner.setLayoutParams(lp);
    }
}

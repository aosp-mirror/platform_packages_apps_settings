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

package com.android.settings.notification;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class DropDownPreference extends Preference {
    private final Context mContext;
    private final ArrayAdapter<String> mAdapter;
    private final Spinner mSpinner;

    private Callback mCallback;

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
                setSelectedItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // noop
            }
        });
        setPersistent(false);
        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mSpinner.performClick();
                return true;
            }
        });
    }

    public void setDropDownWidth(int dimenResId) {
        mSpinner.setDropDownWidth(mContext.getResources().getDimensionPixelSize(dimenResId));
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setSelectedItem(int position) {
        if (mCallback != null && !mCallback.onItemSelected(position)) {
            return;
        }
        mSpinner.setSelection(position);
        setSummary(mAdapter.getItem(position));
        final boolean disableDependents = position == 0;
        notifyDependencyChange(disableDependents);
    }

    public void addItem(int resId) {
        mAdapter.add(mContext.getResources().getString(resId));
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (view.equals(mSpinner.getParent())) return;
        if (mSpinner.getParent() != null) {
            ((ViewGroup)mSpinner.getParent()).removeView(mSpinner);
        }
        final ViewGroup vg = (ViewGroup)view;
        vg.addView(mSpinner, 0);
        final ViewGroup.LayoutParams lp = mSpinner.getLayoutParams();
        lp.width = 0;
        mSpinner.setLayoutParams(lp);
    }

    public interface Callback {
        boolean onItemSelected(int pos);
    }
}
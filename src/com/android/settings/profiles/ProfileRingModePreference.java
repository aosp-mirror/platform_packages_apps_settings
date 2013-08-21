/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.profiles;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.android.settings.R;

public class ProfileRingModePreference extends Preference implements
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private boolean mProtectFromCheckedChange = false;

    private CheckBox mCheckBox;

    final static String TAG = "ProfileRingModePreference";

    private ProfileConfig.RingModeItem mRingModeItem;

    final static int defaultChoice = -1;

    private int currentChoice;

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public ProfileRingModePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * @param context
     * @param attrs
     */
    public ProfileRingModePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * @param context
     */
    public ProfileRingModePreference(Context context) {
        super(context);
        init();
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);

        View widget = view.findViewById(R.id.profile_checkbox);
        if ((widget != null) && widget instanceof CheckBox) {
            mCheckBox = (CheckBox) widget;
            mCheckBox.setOnCheckedChangeListener(this);

            mProtectFromCheckedChange = true;
            mCheckBox.setChecked(isChecked());
            mProtectFromCheckedChange = false;
        }

        View textLayout = view.findViewById(R.id.text_layout);
        if ((textLayout != null) && textLayout instanceof LinearLayout) {
            textLayout.setOnClickListener(this);
        }

        return view;
    }

    private void init() {
        setLayoutResource(R.layout.preference_streamvolume);
    }

    public boolean isChecked() {
        return mRingModeItem != null && mRingModeItem.mSettings.isOverride();
    }

    public void setRingModeItem(ProfileConfig.RingModeItem ringModeItem) {
        mRingModeItem = ringModeItem;

        if (mCheckBox != null) {
            mCheckBox.setChecked(mRingModeItem.mSettings.isOverride());
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mProtectFromCheckedChange) {
            return;
        }

        mRingModeItem.mSettings.setOverride(isChecked);

        callChangeListener(isChecked);
    }

    protected Dialog createRingModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final String[] ringModeValues = getContext().getResources().getStringArray(R.array.ring_mode_values);
        String currentValue = mRingModeItem.mSettings.getValue();
        if (currentValue != null) {
            for (int i = 0; i < ringModeValues.length; i++) {
                if (currentValue.equals(ringModeValues[i])) {
                    currentChoice = i;
                    break;
                }
            }
        }

        builder.setTitle(R.string.ring_mode_title);
        builder.setSingleChoiceItems(R.array.ring_mode_entries, currentChoice, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                currentChoice = item;
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (currentChoice != defaultChoice) {
                    String value = ringModeValues[currentChoice];
                    mRingModeItem.mSettings.setValue(value);
                    setSummary(getContext().getResources().getStringArray(R.array.ring_mode_entries)[currentChoice]);
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }

    public ProfileConfig.RingModeItem getRingModeItem() {
        return mRingModeItem;
    }

    @Override
    public void onClick(android.view.View v) {
        if ((v != null) && (R.id.text_layout == v.getId())) {
            createRingModeDialog().show();
        }
    }

    public void setSummary(Context context) {
        String[] entries = context.getResources().getStringArray(R.array.ring_mode_entries);
        String[] values = context.getResources().getStringArray(R.array.ring_mode_values);
        int l = entries.length;
        String value = mRingModeItem.mSettings.getValue();
        for (int i = 0; i < l; i++) {
            if (value.equals(values[i])) {
                setSummary(entries[i]);
                break;
            }
        }
    }
}

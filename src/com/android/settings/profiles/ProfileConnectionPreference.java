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

public class ProfileConnectionPreference extends Preference implements
        CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private boolean mProtectFromCheckedChange = false;

    private CheckBox mCheckBox;

    final static String TAG = "ProfileConnectionPreference";

    private ProfileConfig.ConnectionItem mConnectionItem;

    final static int defaultChoice = -1;

    private int currentChoice;

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public ProfileConnectionPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * @param context
     * @param attrs
     */
    public ProfileConnectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * @param context
     */
    public ProfileConnectionPreference(Context context) {
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
        return mConnectionItem != null && mConnectionItem.mSettings.isOverride();
    }

    public void setConnectionItem(ProfileConfig.ConnectionItem connectionItem) {
        mConnectionItem = connectionItem;

        if (mCheckBox != null) {
            mCheckBox.setChecked(mConnectionItem.mSettings.isOverride());
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mProtectFromCheckedChange) {
            return;
        }

        mConnectionItem.mSettings.setOverride(isChecked);

        callChangeListener(isChecked);
    }

    protected Dialog createConnectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final String[] ConnectionValues = getContext().getResources().getStringArray(R.array.profile_connection_values);
        final String[] connectionNames = getContext().getResources().getStringArray(mConnectionItem.mChoices);

        currentChoice = mConnectionItem.mSettings.getValue();

        builder.setTitle(mConnectionItem.mLabel);
        builder.setSingleChoiceItems(mConnectionItem.mChoices, currentChoice, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                currentChoice = item;
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (currentChoice != defaultChoice) {
                    int value = Integer.parseInt(ConnectionValues[currentChoice]);
                    mConnectionItem.mSettings.setValue(value);
                    setSummary(connectionNames[value]);
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
           }
        });
        return builder.create();
    }

    public ProfileConfig.ConnectionItem getConnectionItem() {
        return mConnectionItem;
    }

    @Override
    public void onClick(android.view.View v) {
        if ((v != null) && (R.id.text_layout == v.getId())) {
            createConnectionDialog().show();
        }
    }
}

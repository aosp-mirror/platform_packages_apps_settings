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

import android.content.Context;
import android.content.Intent;
import android.preference.CheckBoxPreference;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;

/**
 * CheckBox preference that optionally shows an icon for launching a settings
 * {@link android.app.Activity}. The settings activity, if intent for launching
 * it was provided, can be stared only if the CheckBox in is checked.
 */
public class SettingsCheckBoxPreference extends CheckBoxPreference {

    // Integer.MIN_VALUE means not initalized
    private static int sDimAlpha = Integer.MIN_VALUE;

    private final Intent mSettingsIntent;

    /**
     * Creates a new instance.
     *
     * @param context Context for accessing resources.
     * @param settingsIntent Intent to use as settings for the item represented by
     *        this preference. Pass <code>null</code> if there is no associated 
     *        settings activity.
     */
    public SettingsCheckBoxPreference(Context context, Intent settingsIntent) {
        super(context);

        if (sDimAlpha == Integer.MIN_VALUE) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, outValue, true);
            sDimAlpha = (int) (outValue.getFloat() * 255);
        }

        mSettingsIntent = settingsIntent;
        setWidgetLayoutResource(R.layout.preference_settings_checkbox_widget);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        ImageView settingsButton = (ImageView) view.findViewById(R.id.settings_button);
        if (mSettingsIntent != null) {
            CheckBox checkbox = (CheckBox) view.findViewById(com.android.internal.R.id.checkbox);
            if (checkbox.isChecked()) {
                settingsButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View view) {
                        getContext().startActivity(mSettingsIntent);
                    }
                });
            }
            settingsButton.setVisibility(View.VISIBLE);
            if (checkbox.isChecked() && isEnabled()) {
                settingsButton.setAlpha(255);
            } else {
                settingsButton.setAlpha(sDimAlpha);
            }
        } else {
            settingsButton.setVisibility(View.GONE);
            view.findViewById(R.id.divider).setVisibility(View.GONE);
        }
    }
}

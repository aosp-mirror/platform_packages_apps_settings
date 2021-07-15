/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.utils.ColorUtil;

/** A preference with a Gear on the side and mutable Gear color. */
public class MutableGearPreference extends GearPreference {
    private static final int VALUE_ENABLED_ALPHA = 255;

    private ImageView mGear;
    private Context mContext;
    private int mDisabledAlphaValue;

    public MutableGearPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mDisabledAlphaValue = (int) (ColorUtil.getDisabledAlpha(context) * VALUE_ENABLED_ALPHA);
    }

    @Override
    public void setGearEnabled(boolean enabled) {
        if (mGear != null) {
            mGear.setEnabled(enabled);
            mGear.setImageAlpha(enabled ? VALUE_ENABLED_ALPHA : mDisabledAlphaValue);
        }
        mGearState = enabled;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mGear = (ImageView) holder.findViewById(R.id.settings_button);
        setGearEnabled(mGearState);
    }
}

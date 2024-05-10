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

package com.android.settings.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/** A preference with tick icon. */
public class TickButtonPreference extends Preference {
    private ImageView mCheckIcon;
    private boolean mIsSelected = false;

    public TickButtonPreference(Context context) {
        super(context);
        init(context, null);
    }

    public TickButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setWidgetLayoutResource(R.layout.preference_check_icon);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mCheckIcon = (ImageView) holder.findViewById(R.id.check_icon);
        setSelected(mIsSelected);
    }

    /** Set icon state.*/
    public void setSelected(boolean isSelected) {
        if (mCheckIcon != null) {
            mCheckIcon.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        }
        mIsSelected = isSelected;
    }

    /** Return state of presenting icon. */
    public boolean isSelected() {
        return mIsSelected;
    }
}

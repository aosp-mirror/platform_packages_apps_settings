/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;

/**
 * A preference with a Gear on the side
 */
public class GearPreference extends RestrictedPreference implements View.OnClickListener {

    private OnGearClickListener mOnGearClickListener;

    public GearPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnGearClickListener(OnGearClickListener l) {
        mOnGearClickListener = l;
        notifyChanged();
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_gear;
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return mOnGearClickListener == null;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final View gear = holder.findViewById(R.id.settings_button);
        if (mOnGearClickListener != null) {
            gear.setVisibility(View.VISIBLE);
            gear.setOnClickListener(this);
        } else {
            gear.setVisibility(View.GONE);
            gear.setOnClickListener(null);
        }
        gear.setEnabled(true);  // Make gear available even if the preference itself is disabled.
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.settings_button) {
            if (mOnGearClickListener != null) {
                mOnGearClickListener.onGearClick(this);
            }
        }
    }

    public interface OnGearClickListener {
        void onGearClick(GearPreference p);
    }
}

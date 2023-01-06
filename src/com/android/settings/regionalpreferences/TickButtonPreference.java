/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.regionalpreferences;

import android.content.Context;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.TwoTargetPreference;

/** A preference with tick button */
public class TickButtonPreference extends TwoTargetPreference {
    private static final String TAG = TickButtonPreference.class.getSimpleName();
    private boolean mIsTickEnabled;
    private View mWidgetFrame;
    private View mDivider;

    public TickButtonPreference(Context context) {
        super(context, null);
    }

    /** Set this preference to be selected. */
    public void setTickEnable(boolean isEnable) {
        mIsTickEnabled = isEnable;
        if (mWidgetFrame != null) {
            mWidgetFrame.setVisibility(isEnable ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /** Check if this preference is selected. */
    public boolean isTickEnabled() {
        return mIsTickEnabled;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mDivider = holder.findViewById(R.id.two_target_divider);
        mWidgetFrame = holder.findViewById(android.R.id.widget_frame);
        if (mDivider != null) {
            mDivider.setVisibility(View.GONE);
        }
        if (mWidgetFrame != null) {
            mWidgetFrame.setVisibility(mIsTickEnabled ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    protected int getSecondTargetResId() {
        super.getSecondTargetResId();
        return R.layout.preference_widget_tick;
    }
}

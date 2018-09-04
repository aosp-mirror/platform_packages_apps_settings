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

package com.android.settings;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * Provides a summary of a setting page in a preference.  Such as memory or data usage.
 */
public class SummaryPreference extends Preference {

    private static final String TAG = "SummaryPreference";
    private String mAmount;
    private String mUnits;

    private boolean mChartEnabled = true;
    private float mLeftRatio, mMiddleRatio, mRightRatio;
    private String mStartLabel;
    private String mEndLabel;

    public SummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_summary_preference);
    }

    public void setChartEnabled(boolean enabled) {
        if (mChartEnabled != enabled) {
            mChartEnabled = enabled;
            notifyChanged();
        }
    }

    public void setAmount(String amount) {
        mAmount = amount;
        if (mAmount != null && mUnits != null) {
            setTitle(TextUtils.expandTemplate(getContext().getText(R.string.storage_size_large),
                    mAmount, mUnits));
        }
    }

    public void setUnits(String units) {
        mUnits = units;
        if (mAmount != null && mUnits != null) {
            setTitle(TextUtils.expandTemplate(getContext().getText(R.string.storage_size_large),
                    mAmount, mUnits));
        }
    }

    public void setLabels(String start, String end) {
        mStartLabel = start;
        mEndLabel = end;
        notifyChanged();
    }

    public void setRatios(float left, float middle, float right) {
        mLeftRatio = left;
        mMiddleRatio = middle;
        mRightRatio = right;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final ProgressBar colorBar = holder.itemView.findViewById(R.id.color_bar);

        if (mChartEnabled) {
            colorBar.setVisibility(View.VISIBLE);
            int progress = (int) (mLeftRatio * 100);
            colorBar.setProgress(progress);
            colorBar.setSecondaryProgress(progress + (int) (mMiddleRatio * 100));
        } else {
            colorBar.setVisibility(View.GONE);
        }

        if (mChartEnabled && (!TextUtils.isEmpty(mStartLabel) || !TextUtils.isEmpty(mEndLabel))) {
            holder.findViewById(R.id.label_bar).setVisibility(View.VISIBLE);
            ((TextView) holder.findViewById(android.R.id.text1)).setText(mStartLabel);
            ((TextView) holder.findViewById(android.R.id.text2)).setText(mEndLabel);
        } else {
            holder.findViewById(R.id.label_bar).setVisibility(View.GONE);
        }
    }
}

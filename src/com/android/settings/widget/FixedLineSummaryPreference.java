/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * A preference whose summary text will only span one single line.
 */
public class FixedLineSummaryPreference extends Preference {

    private int mSummaryLineCount;

    public FixedLineSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FixedLineSummaryPreference,
                0, 0);
        if (a.hasValue(R.styleable.FixedLineSummaryPreference_summaryLineCount)) {
            mSummaryLineCount = a.getInteger(
                    R.styleable.FixedLineSummaryPreference_summaryLineCount, 1);
        } else {
            mSummaryLineCount = 1;
        }
        a.recycle();
    }

    public void setSummaryLineCount(int count) {
        mSummaryLineCount = count;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView summary = (TextView) holder.findViewById(android.R.id.summary);
        if (summary != null) {
            summary.setMinLines(mSummaryLineCount);
            summary.setMaxLines(mSummaryLineCount);
            summary.setEllipsize(TruncateAt.END);
        }
    }
}

/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.applications;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;

public class ProcessStatsPreference extends Preference {
    private ProcStatsEntry mEntry;
    private int mProgress;
    private CharSequence mProgressText;

    public ProcessStatsPreference(Context context) {
        this(context, null);
    }

    public ProcessStatsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProcessStatsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ProcessStatsPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_app_percentage);
    }

    public void init(Drawable icon, ProcStatsEntry entry) {
        mEntry = entry;
        setIcon(icon != null ? icon : new ColorDrawable(0));
    }

    public ProcStatsEntry getEntry() {
        return mEntry;
    }

    public void setPercent(double percentOfWeight, double percentOfTime) {
        mProgress = (int) Math.ceil(percentOfWeight);
        mProgressText = Utils.formatPercentage((int) percentOfTime);
        notifyChanged();
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        final ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
        progress.setProgress(mProgress);

        final TextView text1 = (TextView) view.findViewById(android.R.id.text1);
        text1.setText(mProgressText);
    }
}

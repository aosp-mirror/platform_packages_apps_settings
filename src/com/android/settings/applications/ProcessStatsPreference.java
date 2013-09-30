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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;

public class ProcessStatsPreference extends Preference {
    private final ProcStatsEntry mEntry;
    private int mProgress;
    private CharSequence mProgressText;

    public ProcessStatsPreference(Context context, Drawable icon, ProcStatsEntry entry) {
        super(context);
        mEntry = entry;
        setLayoutResource(R.layout.app_percentage_item);
        setIcon(icon != null ? icon : new ColorDrawable(0));
    }

    public ProcStatsEntry getEntry() {
        return mEntry;
    }

    public void setPercent(double percentOfWeight, double percentOfTime) {
        mProgress = (int) Math.ceil(percentOfWeight);
        mProgressText = getContext().getResources().getString(
                R.string.percentage, (int) Math.round(percentOfTime));
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

/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import com.android.settings.R;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Creates a percentage bar chart inside a preference.
 */
public class UsageBarPreference extends Preference {
    private PercentageBarChart mChart = null;

    private final Collection<PercentageBarChart.Entry> mEntries = new ArrayList<PercentageBarChart.Entry>();

    public UsageBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_memoryusage);
    }

    public UsageBarPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_memoryusage);
    }

    public UsageBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_memoryusage);
    }

    public void addEntry(float percentage, int color) {
        mEntries.add(PercentageBarChart.createEntry(percentage, color));
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mChart = (PercentageBarChart) view.findViewById(R.id.percentage_bar_chart);

        mChart.setEntries(mEntries);
    }

    public void commit() {
        if (mChart != null) {
            mChart.invalidate();
        }
    }

    public void clear() {
        mEntries.clear();
    }
}

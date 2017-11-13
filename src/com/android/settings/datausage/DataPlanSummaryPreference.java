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

package com.android.settings.datausage;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.widget.DonutView;

/**
 * Provides a summary of data plans as preferences on settings page.
 */
public final class DataPlanSummaryPreference extends Preference {
    private String mName;
    private String mDescription;
    private double mPercentageUsage;
    private int mUsageTextColor;
    private int mMeterBackgroundColor;
    private int mMeterConsumedColor;

    public DataPlanSummaryPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.settings_data_plan_summary_preference);
    }

    public DataPlanSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_data_plan_summary_preference);
    }

    public void setName(String planName) {
        mName = planName;
        notifyChanged();
    }

    public void setDescription(String planDescription) {
        mDescription = planDescription;
        notifyChanged();
    }

    public void setPercentageUsage(double percentageUsage) {
        mPercentageUsage = percentageUsage;
        notifyChanged();
    }

    public void setUsageTextColor(@ColorRes int planUsageTextColor) {
        mUsageTextColor = planUsageTextColor;
        notifyChanged();
    }

    public void setMeterBackgroundColor(@ColorRes int meterBackgroundColor) {
        mMeterBackgroundColor = meterBackgroundColor;
        notifyChanged();
    }

    public void setMeterConsumedColor(@ColorRes int meterConsumedColor) {
        mMeterConsumedColor = meterConsumedColor;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        titleView.setTextColor(mUsageTextColor);
        ((TextView) holder.findViewById(android.R.id.text1)).setText(mName);
        ((TextView) holder.findViewById(android.R.id.text2)).setText(mDescription);
        DonutView donutView = (DonutView) holder.findViewById(R.id.donut);
        donutView.setPercentage(mPercentageUsage);
        donutView.setMeterBackgroundColor(mMeterBackgroundColor);
        donutView.setMeterConsumedColor(mMeterConsumedColor);
    }
}

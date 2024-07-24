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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/** Custom preference for displaying the app power usage time. */
public class PowerUsageTimePreference extends Preference {
    private static final String TAG = "PowerUsageTimePreference";

    @VisibleForTesting CharSequence mTimeTitle;
    @VisibleForTesting CharSequence mTimeSummary;
    @VisibleForTesting CharSequence mAnomalyHintText;

    public PowerUsageTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.power_usage_time);
    }

    void setTimeTitle(CharSequence timeTitle) {
        if (!TextUtils.equals(mTimeTitle, timeTitle)) {
            mTimeTitle = timeTitle;
            notifyChanged();
        }
    }

    void setTimeSummary(CharSequence timeSummary) {
        if (!TextUtils.equals(mTimeSummary, timeSummary)) {
            mTimeSummary = timeSummary;
            notifyChanged();
        }
    }

    void setAnomalyHint(CharSequence anomalyHintText) {
        if (!TextUtils.equals(mAnomalyHintText, anomalyHintText)) {
            mAnomalyHintText = anomalyHintText;
            notifyChanged();
        }
    }

    private void showAnomalyHint(PreferenceViewHolder view) {
        if (TextUtils.isEmpty(mAnomalyHintText)) {
            return;
        }
        final View anomalyHintView = view.findViewById(R.id.anomaly_hints);
        if (anomalyHintView == null) {
            return;
        }
        final TextView warningInfo = anomalyHintView.findViewById(R.id.warning_info);
        if (warningInfo == null) {
            return;
        }
        warningInfo.setText(mAnomalyHintText);
        anomalyHintView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        ((TextView) view.findViewById(R.id.time_title)).setText(mTimeTitle);
        ((TextView) view.findViewById(R.id.time_summary)).setText(mTimeSummary);

        showAnomalyHint(view);
    }
}

/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.datausage;

import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SummaryPreference;
import com.android.settingslib.utils.StringUtil;

import java.util.Objects;

/**
 * Provides a summary of data usage.
 */
public class DataUsageSummaryPreference extends SummaryPreference {

    private int mNumPlans;
    /** The ending time of the billing cycle in milliseconds since epoch. */
    private long mCycleEndTimeMs;
    /** The time of the last update in standard milliseconds since the epoch */
    private long mSnapshotTimeMs;
    /** Name of carrier, or null if not available */
    private CharSequence mCarrierName;
    private String mLimitInfoText;
    private Intent mLaunchIntent;

    public DataUsageSummaryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.data_usage_summary_preference);
    }

    public void setLimitInfo(String text) {
        if (!Objects.equals(text, mLimitInfoText)) {
            mLimitInfoText = text;
            notifyChanged();
        }
    }

    public void setUsageInfo(long cycleEnd, long snapshotTime, CharSequence carrierName,
            int numPlans, Intent launchIntent) {
        mCycleEndTimeMs = cycleEnd;
        mSnapshotTimeMs = snapshotTime;
        mCarrierName = carrierName;
        mNumPlans = numPlans;
        mLaunchIntent = launchIntent;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView usageTitle = (TextView) holder.findViewById(R.id.usage_title);
        usageTitle.setVisibility(mNumPlans > 1 ? View.VISIBLE : View.GONE);

        TextView cycleTime = (TextView) holder.findViewById(R.id.cycle_left_time);
        cycleTime.setText(getContext().getString(R.string.cycle_left_time_text,
                StringUtil.formatElapsedTime(getContext(),
                        mCycleEndTimeMs - System.currentTimeMillis(),false /* withSeconds */)));

        TextView carrierInfo = (TextView) holder.findViewById(R.id.carrier_and_update);
        setCarrierInfo(carrierInfo, mCarrierName, mSnapshotTimeMs);

        Button launchButton = (Button) holder.findViewById(R.id.launch_mdp_app_button);
        launchButton.setOnClickListener((view) -> {
            getContext().sendBroadcast(mLaunchIntent);
        });
        if (mLaunchIntent != null) {
            launchButton.setVisibility(View.VISIBLE);
        } else {
            launchButton.setVisibility(View.GONE);
        }

        TextView limitInfo = (TextView) holder.findViewById(R.id.data_limits);
        limitInfo.setVisibility(
                mLimitInfoText == null || mLimitInfoText.isEmpty() ? View.GONE : View.VISIBLE);
        limitInfo.setText(mLimitInfoText);
    }

    private void setCarrierInfo(TextView carrierInfo, CharSequence carrierName, long updateAge) {
        if (mNumPlans > 0 && updateAge >= 0L) {
            carrierInfo.setVisibility(View.VISIBLE);
            if (carrierName != null) {
                carrierInfo.setText(getContext().getString(R.string.carrier_and_update_text,
                        carrierName, StringUtil.formatRelativeTime(
                                getContext(), updateAge, false /* withSeconds */)));
            } else {
                carrierInfo.setText(getContext().getString(R.string.no_carrier_update_text,
                        StringUtil.formatRelativeTime(
                                getContext(), updateAge, false /* withSeconds */)));
            }
        } else {
            carrierInfo.setVisibility(View.GONE);
        }
    }
}

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

package com.android.settings.fuelgauge.batterytip.tips;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Tip to show general summary about battery life
 */
public class SummaryTip extends BatteryTip {
    private long mAverageTimeMs;

    public SummaryTip(@StateType int state, long averageTimeMs) {
        super(TipType.SUMMARY, state, true /* showDialog */);
        mAverageTimeMs = averageTimeMs;
    }

    @VisibleForTesting
    SummaryTip(Parcel in) {
        super(in);
        mAverageTimeMs = in.readLong();
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_summary_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_summary_summary);
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_battery_status_good_24dp;
    }

    @Override
    public int getIconTintColorId() {
        return R.color.battery_good_color_light;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mAverageTimeMs);
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, SettingsEnums.ACTION_SUMMARY_TIP,
                mState);
    }

    public long getAverageTimeMs() {
        return mAverageTimeMs;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public BatteryTip createFromParcel(Parcel in) {
            return new SummaryTip(in);
        }

        public BatteryTip[] newArray(int size) {
            return new SummaryTip[size];
        }
    };
}

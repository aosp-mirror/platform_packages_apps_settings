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

package com.android.settings.fuelgauge.batterytip.tips;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.List;

/**
 * Tip to show general summary about battery life
 */
public class HighUsageTip extends BatteryTip {

    private final long mLastFullChargeTimeMs;
    @VisibleForTesting
    final List<AppInfo> mHighUsageAppList;

    public HighUsageTip(long lastFullChargeTimeMs, List<AppInfo> appList) {
        super(TipType.HIGH_DEVICE_USAGE, appList.isEmpty() ? StateType.INVISIBLE : StateType.NEW,
                true /* showDialog */);
        mLastFullChargeTimeMs = lastFullChargeTimeMs;
        mHighUsageAppList = appList;
    }

    @VisibleForTesting
    HighUsageTip(Parcel in) {
        super(in);
        mLastFullChargeTimeMs = in.readLong();
        mHighUsageAppList = in.createTypedArrayList(AppInfo.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mLastFullChargeTimeMs);
        dest.writeTypedList(mHighUsageAppList);
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_high_usage_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_high_usage_summary);
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_perm_device_information_red_24dp;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, SettingsEnums.ACTION_HIGH_USAGE_TIP,
                mState);
        for (int i = 0, size = mHighUsageAppList.size(); i < size; i++) {
            final AppInfo appInfo = mHighUsageAppList.get(i);
            metricsFeatureProvider.action(context,
                    SettingsEnums.ACTION_HIGH_USAGE_TIP_LIST,
                    appInfo.packageName);
        }
    }

    public long getLastFullChargeTimeMs() {
        return mLastFullChargeTimeMs;
    }

    public List<AppInfo> getHighUsageAppList() {
        return mHighUsageAppList;
    }

    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder(super.toString());
        stringBuilder.append(" {");
        for (int i = 0, size = mHighUsageAppList.size(); i < size; i++) {
            final AppInfo appInfo = mHighUsageAppList.get(i);
            stringBuilder.append(" " + appInfo.toString() + " ");
        }
        stringBuilder.append('}');

        return stringBuilder.toString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public BatteryTip createFromParcel(Parcel in) {
            return new HighUsageTip(in);
        }

        public BatteryTip[] newArray(int size) {
            return new HighUsageTip[size];
        }
    };

}

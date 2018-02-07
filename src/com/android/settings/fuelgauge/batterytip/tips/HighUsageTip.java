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

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.fuelgauge.batterytip.AppInfo;

import com.android.settingslib.utils.StringUtil;
import java.util.List;

/**
 * Tip to show general summary about battery life
 */
public class HighUsageTip extends BatteryTip {

    private final long mScreenTimeMs;
    @VisibleForTesting
    final List<AppInfo> mHighUsageAppList;

    public HighUsageTip(long screenTimeMs, List<AppInfo> appList) {
        super(TipType.HIGH_DEVICE_USAGE, appList.isEmpty() ? StateType.INVISIBLE : StateType.NEW,
                true /* showDialog */);
        mScreenTimeMs = screenTimeMs;
        mHighUsageAppList = appList;
    }

    @VisibleForTesting
    HighUsageTip(Parcel in) {
        super(in);
        mScreenTimeMs = in.readLong();
        mHighUsageAppList = in.createTypedArrayList(AppInfo.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mScreenTimeMs);
        dest.writeTypedList(mHighUsageAppList);
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_high_usage_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_high_usage_summary,
                StringUtil.formatElapsedTime(context, mScreenTimeMs, false));
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_perm_device_information_red_24dp;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
    }

    public long getScreenTimeMs() {
        return mScreenTimeMs;
    }

    public List<AppInfo> getHighUsageAppList() {
        return mHighUsageAppList;
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

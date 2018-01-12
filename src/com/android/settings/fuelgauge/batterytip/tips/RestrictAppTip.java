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
import android.content.res.Resources;
import android.os.Parcel;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.batterytip.AppInfo;

import java.util.List;

/**
 * Tip to suggest user to restrict some bad apps
 */
public class RestrictAppTip extends BatteryTip {
    private List<AppInfo> mRestrictAppList;

    public RestrictAppTip(@StateType int state, List<AppInfo> highUsageApps) {
        super(TipType.APP_RESTRICTION, state, true /* showDialog */);
        mRestrictAppList = highUsageApps;
    }

    @VisibleForTesting
    RestrictAppTip(Parcel in) {
        super(in);
        mRestrictAppList = in.createTypedArrayList(AppInfo.CREATOR);
    }

    @Override
    public CharSequence getTitle(Context context) {
        final int num = mRestrictAppList.size();
        return context.getResources().getQuantityString(
                mState == StateType.HANDLED
                        ? R.plurals.battery_tip_restrict_handled_title
                        : R.plurals.battery_tip_restrict_title,
                num, num);
    }

    @Override
    public CharSequence getSummary(Context context) {
        final int num = mRestrictAppList.size();
        final CharSequence appLabel = num > 0 ? Utils.getApplicationLabel(context,
                mRestrictAppList.get(0).packageName) : "";
        return mState == StateType.HANDLED
                ? context.getString(R.string.battery_tip_restrict_handled_summary)
                : context.getResources().getQuantityString(R.plurals.battery_tip_restrict_summary,
                num, appLabel, num);
    }

    @Override
    public int getIconId() {
        return mState == StateType.HANDLED
                ? R.drawable.ic_perm_device_information_green_24dp
                : R.drawable.ic_battery_alert_24dp;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
    }

    public List<AppInfo> getRestrictAppList() {
        return mRestrictAppList;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedList(mRestrictAppList);
    }

    public static final Creator CREATOR = new Creator() {
        public BatteryTip createFromParcel(Parcel in) {
            return new RestrictAppTip(in);
        }

        public BatteryTip[] newArray(int size) {
            return new RestrictAppTip[size];
        }
    };
}

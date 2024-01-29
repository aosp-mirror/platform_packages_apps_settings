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

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.AdvancedPowerUsageDetail;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Tip to suggest user to remove app restriction. This is the empty tip and it is only used in
 * {@link AdvancedPowerUsageDetail} to create dialog.
 */
public class UnrestrictAppTip extends BatteryTip {
    private AppInfo mAppInfo;

    public UnrestrictAppTip(@StateType int state, AppInfo appInfo) {
        super(TipType.REMOVE_APP_RESTRICTION, state, true /* showDialog */);
        mAppInfo = appInfo;
    }

    @VisibleForTesting
    UnrestrictAppTip(Parcel in) {
        super(in);
        mAppInfo = in.readParcelable(getClass().getClassLoader());
    }

    @Override
    public CharSequence getTitle(Context context) {
        // Don't need title since this is an empty tip
        return null;
    }

    @Override
    public CharSequence getSummary(Context context) {
        // Don't need summary since this is an empty tip
        return null;
    }

    @Override
    public int getIconId() {
        return 0;
    }

    public String getPackageName() {
        return mAppInfo.packageName;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        // Do nothing
    }

    public AppInfo getUnrestrictAppInfo() {
        return mAppInfo;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(mAppInfo, flags);
    }

    public static final Creator CREATOR =
            new Creator() {
                public BatteryTip createFromParcel(Parcel in) {
                    return new UnrestrictAppTip(in);
                }

                public BatteryTip[] newArray(int size) {
                    return new UnrestrictAppTip[size];
                }
            };
}

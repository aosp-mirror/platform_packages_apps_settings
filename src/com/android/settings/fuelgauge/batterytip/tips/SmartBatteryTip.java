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

import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/** Tip to suggest turn on smart battery if it is not on */
public class SmartBatteryTip extends BatteryTip {

    public SmartBatteryTip(@StateType int state) {
        super(TipType.SMART_BATTERY_MANAGER, state, false /* showDialog */);
    }

    private SmartBatteryTip(Parcel in) {
        super(in);
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_smart_battery_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_smart_battery_summary);
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_perm_device_information_theme;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, SettingsEnums.ACTION_SMART_BATTERY_TIP, mState);
    }

    public static final Creator CREATOR =
            new Creator() {
                public BatteryTip createFromParcel(Parcel in) {
                    return new SmartBatteryTip(in);
                }

                public BatteryTip[] newArray(int size) {
                    return new SmartBatteryTip[size];
                }
            };
}

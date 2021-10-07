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

/**
 * Tip to show early warning if battery couldn't make to usual charging time
 */
public class EarlyWarningTip extends BatteryTip {
    private boolean mPowerSaveModeOn;

    public EarlyWarningTip(@StateType int state, boolean powerSaveModeOn) {
        super(TipType.BATTERY_SAVER, state, false /* showDialog */);
        mPowerSaveModeOn = powerSaveModeOn;
    }

    public EarlyWarningTip(Parcel in) {
        super(in);
        mPowerSaveModeOn = in.readBoolean();
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(
                mState = R.string.battery_tip_early_heads_up_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(
                mState = R.string.battery_tip_early_heads_up_summary);
    }

    @Override
    public int getIconId() {
        return mState = R.drawable.ic_battery_status_bad_24dp;
    }

    @Override
    public int getIconTintColorId() {
        return mState = R.color.battery_bad_color_light;
    }

    @Override
    public void updateState(BatteryTip tip) {
        final EarlyWarningTip earlyWarningTip = (EarlyWarningTip) tip;
        if (earlyWarningTip.mState == StateType.NEW) {
            // Display it if there is early warning
            mState = StateType.NEW;
        } else if (earlyWarningTip.mPowerSaveModeOn) {
            // If powerSaveMode is really on, dismiss it.
            mState = StateType.INVISIBLE;
        } else {
            mState = earlyWarningTip.getState();
        }
        mPowerSaveModeOn = earlyWarningTip.mPowerSaveModeOn;
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, SettingsEnums.ACTION_EARLY_WARNING_TIP,
                mState);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeBoolean(mPowerSaveModeOn);
    }

    public boolean isPowerSaveModeOn() {
        return mPowerSaveModeOn;
    }

    public static final Creator CREATOR = new Creator() {
        public BatteryTip createFromParcel(Parcel in) {
            return new EarlyWarningTip(in);
        }

        public BatteryTip[] newArray(int size) {
            return new EarlyWarningTip[size];
        }
    };
}

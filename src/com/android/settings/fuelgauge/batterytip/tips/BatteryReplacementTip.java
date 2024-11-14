/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.text.Html;

import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Tip to show battery replacement information.
 */
public class BatteryReplacementTip extends BatteryTip {
    public BatteryReplacementTip(int state) {
        super(TipType.BATTERY_REPLACEMENT, state, false /* showDialog */);
    }

    BatteryReplacementTip(Parcel in) {
        super(in);
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_replacement_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return Html.fromHtml(context.getString(R.string.battery_tip_replacement_summary),
                Html.FROM_HTML_MODE_LEGACY);
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_battery_alert_24dp;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
    }

    public static final Creator CREATOR = new Creator() {

        @Override
        public BatteryTip createFromParcel(Parcel source) {
            return new BatteryReplacementTip(source);
        }

        @Override
        public BatteryTip[] newArray(int size) {
            return new BatteryReplacementTip[size];
        }
    };
}

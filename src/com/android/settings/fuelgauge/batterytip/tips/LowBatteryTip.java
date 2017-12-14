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

import android.app.Dialog;
import android.content.Context;

import com.android.settings.R;

/**
 * Tip to show current battery life is short
 */
public class LowBatteryTip extends BatteryTip {

    public LowBatteryTip(@StateType int state) {
        mShowDialog = false;
        mState = state;
        mType = TipType.LOW_BATTERY;
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_low_battery_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_low_battery_summary);
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
    public void action() {
        // do nothing
    }

    @Override
    public Dialog buildDialog() {
        //TODO(b/70570352): create the dialog for low battery tip and add test
        return null;
    }
}

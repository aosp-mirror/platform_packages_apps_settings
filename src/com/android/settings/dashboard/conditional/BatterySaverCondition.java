/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.dashboard.conditional;

import android.graphics.drawable.Icon;
import android.os.PowerManager;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatterySaverSettings;

public class BatterySaverCondition extends Condition {
    public BatterySaverCondition(ConditionManager manager) {
        super(manager);
    }

    @Override
    public void refreshState() {
        PowerManager powerManager = mManager.getContext().getSystemService(PowerManager.class);
        setActive(powerManager.isPowerSaveMode());
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(mManager.getContext(), R.drawable.ic_settings_battery);
    }

    @Override
    public CharSequence getTitle() {
        return mManager.getContext().getString(R.string.condition_battery_title);
    }

    @Override
    public CharSequence getSummary() {
        return mManager.getContext().getString(R.string.condition_battery_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[] { mManager.getContext().getString(R.string.condition_turn_off) };
    }

    @Override
    public void onPrimaryClick() {
        Utils.startWithFragment(mManager.getContext(), BatterySaverSettings.class.getName(), null,
                null, 0, R.string.battery_saver, null, MetricsEvent.DASHBOARD_SUMMARY);
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            mManager.getContext().getSystemService(PowerManager.class).setPowerSaveMode(false);
            refreshState();
        } else {
            throw new IllegalArgumentException("Unexpected index " + index);
        }
    }

    @Override
    public int getMetricsConstant() {
        return MetricsEvent.SETTINGS_CONDITION_BATTERY_SAVER;
    }
}

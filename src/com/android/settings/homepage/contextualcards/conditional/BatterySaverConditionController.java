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

package com.android.settings.homepage.contextualcards.conditional;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.PowerManager;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.fuelgauge.BatterySaverReceiver;
import com.android.settings.fuelgauge.batterysaver.BatterySaverSettings;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

import java.util.Objects;

public class BatterySaverConditionController implements ConditionalCardController,
        BatterySaverReceiver.BatterySaverListener {
    static final int ID = Objects.hash("BatterySaverConditionController");

    private final Context mAppContext;
    private final ConditionManager mConditionManager;
    private final BatterySaverReceiver mReceiver;
    private final PowerManager mPowerManager;

    public BatterySaverConditionController(Context appContext, ConditionManager conditionManager) {
        mAppContext = appContext;
        mConditionManager = conditionManager;
        mPowerManager = appContext.getSystemService(PowerManager.class);
        mReceiver = new BatterySaverReceiver(appContext);
        mReceiver.setBatterySaverListener(this);
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public boolean isDisplayable() {
        return mPowerManager.isPowerSaveMode();
    }

    @Override
    public void onPrimaryClick(Context context) {
        new SubSettingLauncher(context)
                .setDestination(BatterySaverSettings.class.getName())
                .setSourceMetricsCategory(SettingsEnums.DASHBOARD_SUMMARY)
                .setTitleRes(R.string.battery_saver)
                .launch();
    }

    @Override
    public void onActionClick() {
        BatterySaverUtils.setPowerSaveMode(mAppContext, false,
                /*needFirstTimeWarning*/ false);
    }

    @Override
    public ContextualCard buildContextualCard() {
        return new ConditionalContextualCard.Builder()
                .setConditionId(ID)
                .setMetricsConstant(SettingsEnums.SETTINGS_CONDITION_BATTERY_SAVER)
                .setActionText(mAppContext.getText(R.string.condition_turn_off))
                .setName(mAppContext.getPackageName() + "/"
                        + mAppContext.getText(R.string.condition_battery_title))
                .setTitleText(mAppContext.getText(R.string.condition_battery_title).toString())
                .setSummaryText(mAppContext.getText(R.string.condition_battery_summary).toString())
                .setIconDrawable(mAppContext.getDrawable(R.drawable.ic_battery_saver_accent_24dp))
                .setViewType(ConditionContextualCardRenderer.VIEW_TYPE_HALF_WIDTH)
                .build();
    }

    @Override
    public void startMonitoringStateChange() {
        mReceiver.setListening(true);
    }

    @Override
    public void stopMonitoringStateChange() {
        mReceiver.setListening(false);
    }

    @Override
    public void onPowerSaveModeChanged() {
        mConditionManager.onConditionChanged();
    }

    @Override
    public void onBatteryChanged(boolean pluggedIn) {

    }
}

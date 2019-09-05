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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settingslib.WirelessUtils;

import java.util.Objects;

public class AirplaneModeConditionController implements ConditionalCardController {

    static final int ID = Objects.hash("AirplaneModeConditionController");

    private static final IntentFilter AIRPLANE_MODE_FILTER =
            new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);

    private final ConditionManager mConditionManager;
    private final Context mAppContext;
    private final Receiver mReceiver;

    public AirplaneModeConditionController(Context appContext, ConditionManager conditionManager) {
        mAppContext = appContext;
        mConditionManager = conditionManager;
        mReceiver = new Receiver();
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public boolean isDisplayable() {
        return WirelessUtils.isAirplaneModeOn(mAppContext);
    }

    @Override
    public void onPrimaryClick(Context context) {
        context.startActivity(
                new Intent(Settings.ACTION_WIRELESS_SETTINGS));
    }

    @Override
    public void onActionClick() {
        ConnectivityManager.from(mAppContext).setAirplaneMode(false);
    }

    @Override
    public ContextualCard buildContextualCard() {
        return new ConditionalContextualCard.Builder()
                .setConditionId(ID)
                .setMetricsConstant(SettingsEnums.SETTINGS_CONDITION_AIRPLANE_MODE)
                .setActionText(mAppContext.getText(R.string.condition_turn_off))
                .setName(mAppContext.getPackageName() + "/"
                        + mAppContext.getText(R.string.condition_airplane_title))
                .setTitleText(mAppContext.getText(R.string.condition_airplane_title).toString())
                .setSummaryText(mAppContext.getText(R.string.condition_airplane_summary).toString())
                .setIconDrawable(mAppContext.getDrawable(R.drawable.ic_airplanemode_active))
                .setViewType(ConditionContextualCardRenderer.VIEW_TYPE_HALF_WIDTH)
                .build();
    }

    @Override
    public void startMonitoringStateChange() {
        mAppContext.registerReceiver(mReceiver, AIRPLANE_MODE_FILTER);
    }

    @Override
    public void stopMonitoringStateChange() {
        mAppContext.unregisterReceiver(mReceiver);
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
                mConditionManager.onConditionChanged();
            }
        }
    }
}

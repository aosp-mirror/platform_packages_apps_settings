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
import android.telephony.TelephonyManager;

import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.homepage.contextualcards.ContextualCard;

import java.util.Objects;

public class CellularDataConditionController implements ConditionalCardController {

    static final int ID = Objects.hash("CellularDataConditionController");

    private static final IntentFilter DATA_CONNECTION_FILTER =
            new IntentFilter(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);

    private final Context mAppContext;
    private final ConditionManager mConditionManager;
    private final Receiver mReceiver;
    private final TelephonyManager mTelephonyManager;
    private final ConnectivityManager mConnectivityManager;

    public CellularDataConditionController(Context appContext, ConditionManager conditionManager) {
        mAppContext = appContext;
        mConditionManager = conditionManager;
        mReceiver = new Receiver();
        mConnectivityManager = appContext.getSystemService(
                ConnectivityManager.class);
        mTelephonyManager = appContext.getSystemService(TelephonyManager.class);
    }

    @Override
    public long getId() {
        return ID;
    }

    @Override
    public boolean isDisplayable() {
        if (!mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)
                || mTelephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY) {
            return false;
        }
        return !mTelephonyManager.isDataEnabled();
    }

    @Override
    public void onPrimaryClick(Context context) {
        context.startActivity(new Intent(context,
                Settings.DataUsageSummaryActivity.class));
    }

    @Override
    public void onActionClick() {
        mTelephonyManager.setDataEnabled(true);
    }

    @Override
    public ContextualCard buildContextualCard() {
        return new ConditionalContextualCard.Builder()
                .setConditionId(ID)
                .setMetricsConstant(SettingsEnums.SETTINGS_CONDITION_CELLULAR_DATA)
                .setActionText(mAppContext.getText(R.string.condition_turn_on))
                .setName(mAppContext.getPackageName() + "/"
                        + mAppContext.getText(R.string.condition_cellular_title))
                .setTitleText(mAppContext.getText(R.string.condition_cellular_title).toString())
                .setSummaryText(mAppContext.getText(R.string.condition_cellular_summary).toString())
                .setIconDrawable(mAppContext.getDrawable(R.drawable.ic_cellular_off))
                .setViewType(ConditionContextualCardRenderer.VIEW_TYPE_HALF_WIDTH)
                .build();
    }

    @Override
    public void startMonitoringStateChange() {
        mAppContext.registerReceiver(mReceiver, DATA_CONNECTION_FILTER);
    }

    @Override
    public void stopMonitoringStateChange() {
        mAppContext.unregisterReceiver(mReceiver);
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED.equals(
                    intent.getAction())) {
                mConditionManager.onConditionChanged();
            }
        }
    }
}

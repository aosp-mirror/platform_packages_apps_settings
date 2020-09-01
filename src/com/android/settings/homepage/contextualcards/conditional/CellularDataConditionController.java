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
import android.content.Intent;
import android.net.ConnectivityManager;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseDataConnectionState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.network.GlobalSettingsChangeListener;

import java.util.Objects;

public class CellularDataConditionController implements ConditionalCardController {

    static final int ID = Objects.hash("CellularDataConditionController");

    private final Context mAppContext;
    private final ConditionManager mConditionManager;
    private final GlobalSettingsChangeListener mDefaultDataSubscriptionIdListener;
    private final ConnectivityManager mConnectivityManager;

    private int mSubId;
    private TelephonyManager mTelephonyManager;
    private boolean mIsListeningConnectionChange;

    public CellularDataConditionController(Context appContext, ConditionManager conditionManager) {
        mAppContext = appContext;
        mConditionManager = conditionManager;
        mSubId = getDefaultDataSubscriptionId(appContext);
        mTelephonyManager = getTelephonyManager(appContext, mSubId);
        mDefaultDataSubscriptionIdListener = new GlobalSettingsChangeListener(appContext,
                android.provider.Settings.Global.MULTI_SIM_DATA_CALL_SUBSCRIPTION) {
            public void onChanged(String field) {
                final int subId = getDefaultDataSubscriptionId(mAppContext);
                if (subId == mSubId) {
                    return;
                }
                mSubId = subId;
                if (mIsListeningConnectionChange) {
                    restartPhoneStateListener(mAppContext, subId);
                }
            }
        };
        mConnectivityManager = appContext.getSystemService(
                ConnectivityManager.class);
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
        restartPhoneStateListener(mAppContext, mSubId);
    }

    @Override
    public void stopMonitoringStateChange() {
        stopPhoneStateListener();
    }

    private int getDefaultDataSubscriptionId(Context context) {
        final SubscriptionManager subscriptionManager =
                context.getSystemService(SubscriptionManager.class);
        return subscriptionManager.getDefaultDataSubscriptionId();
    }

    private TelephonyManager getTelephonyManager(Context context, int subId) {
        final TelephonyManager telephonyManager =
                context.getSystemService(TelephonyManager.class);
        return telephonyManager.createForSubscriptionId(subId);
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onPreciseDataConnectionStateChanged(
                PreciseDataConnectionState dataConnectionState) {
            mConditionManager.onConditionChanged();
        }
    };

    private void stopPhoneStateListener() {
        mIsListeningConnectionChange = false;
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    // restart monitoring when subscription has been changed
    private void restartPhoneStateListener(Context context, int subId) {
        stopPhoneStateListener();
        mIsListeningConnectionChange = true;

        // switch mTelephonyManager only when subscription been updated to valid ones
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            mTelephonyManager = getTelephonyManager(context, subId);
        }

        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_PRECISE_DATA_CONNECTION_STATE);
    }
}

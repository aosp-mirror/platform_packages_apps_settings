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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.util.Log;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settingslib.WirelessUtils;

public class AirplaneModeCondition extends Condition {
    public static String TAG = "APM_Condition";

    private final Receiver mReceiver;

    private static final IntentFilter AIRPLANE_MODE_FILTER =
        new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);

    public AirplaneModeCondition(ConditionManager conditionManager) {
        super(conditionManager);
        mReceiver = new Receiver();
    }

    @Override
    public void refreshState() {
        Log.d(TAG, "APM condition refreshed");
        setActive(WirelessUtils.isAirplaneModeOn(mManager.getContext()));
    }

    @Override
    protected BroadcastReceiver getReceiver() {
        return mReceiver;
    }

    @Override
    protected IntentFilter getIntentFilter() {
        return AIRPLANE_MODE_FILTER;
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(mManager.getContext(), R.drawable.ic_airplane);
    }

    @Override
    protected void setActive(boolean active) {
        super.setActive(active);
        Log.d(TAG, "setActive was called with " + active);
    }

    @Override
    public CharSequence getTitle() {
        return mManager.getContext().getString(R.string.condition_airplane_title);
    }

    @Override
    public CharSequence getSummary() {
        return mManager.getContext().getString(R.string.condition_airplane_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[] { mManager.getContext().getString(R.string.condition_turn_off) };
    }

    @Override
    public void onPrimaryClick() {
        mManager.getContext().startActivity(new Intent(mManager.getContext(),
                Settings.NetworkDashboardActivity.class));
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            ConnectivityManager.from(mManager.getContext()).setAirplaneMode(false);
            setActive(false);
        } else {
            throw new IllegalArgumentException("Unexpected index " + index);
        }
    }

    @Override
    public int getMetricsConstant() {
        return MetricsEvent.SETTINGS_CONDITION_AIRPLANE_MODE;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
                ConditionManager.get(context).getCondition(AirplaneModeCondition.class)
                        .refreshState();
            }
        }
    }
}

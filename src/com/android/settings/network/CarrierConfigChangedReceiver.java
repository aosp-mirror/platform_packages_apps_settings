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

package com.android.settings.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.CarrierConfigManager;
import android.util.Log;

import java.util.concurrent.CountDownLatch;

/** A receiver listens to the carrier config changes. */
public class CarrierConfigChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "CarrierConfigChangedReceiver";
    private static final String ACTION_CARRIER_CONFIG_CHANGED =
            CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED;

    private final CountDownLatch mLatch;
    private final boolean mIsWaitingForValidSubId;

    /**
     * This is the CarrierConfigChanged receiver. If it receives the carrier config changed, then it
     * call the CountDownLatch.countDown().
     * If the "isWaitingForValidSubId" is true, then the receiver skip the carrier config changed
     * with the subId = -1. The receiver executes the countDown when the CarrierConfigChanged
     * with valid subId.
     * If the "isWaitingForValidSubId" is false, then the receiver executes the countDown when
     * receiving any CarrierConfigChanged.
     */
    public CarrierConfigChangedReceiver(CountDownLatch latch, boolean isWaitingForValidSubId) {
        mLatch = latch;
        mIsWaitingForValidSubId = isWaitingForValidSubId;
    }

    public void registerOn(Context context) {
        context.registerReceiver(this, new IntentFilter(ACTION_CARRIER_CONFIG_CHANGED));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isInitialStickyBroadcast()) {
            return;
        }

        if (ACTION_CARRIER_CONFIG_CHANGED.equals(intent.getAction())) {
            checkSubscriptionIndex(intent);
        }
    }

    private void checkSubscriptionIndex(Intent intent) {
        if (intent.hasExtra(CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX)
                || !mIsWaitingForValidSubId) {
            int subId = intent.getIntExtra(CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX, -1);
            Log.i(TAG, "subId from config changed: " + subId);
            mLatch.countDown();
        }
    }
}

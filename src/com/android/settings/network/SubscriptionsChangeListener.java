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

package com.android.settings.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;

import com.android.internal.telephony.TelephonyIntents;

/** Helper class for listening to changes in availability of telephony subscriptions */
public class SubscriptionsChangeListener extends ContentObserver {

    public interface SubscriptionsChangeListenerClient {
        void onAirplaneModeChanged(boolean airplaneModeEnabled);
        void onSubscriptionsChanged();
    }

    private Context mContext;
    private SubscriptionsChangeListenerClient mClient;
    private SubscriptionManager mSubscriptionManager;
    private OnSubscriptionsChangedListener mSubscriptionsChangedListener;
    private Uri mAirplaneModeSettingUri;
    private BroadcastReceiver mBroadcastReceiver;

    public SubscriptionsChangeListener(Context context, SubscriptionsChangeListenerClient client) {
        super(new Handler(Looper.getMainLooper()));
        mContext = context;
        mClient = client;
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mSubscriptionsChangedListener = new OnSubscriptionsChangedListener(Looper.getMainLooper()) {
            @Override
            public void onSubscriptionsChanged() {
                subscriptionsChangedCallback();
            }
        };
        mAirplaneModeSettingUri = Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!isInitialStickyBroadcast()) {
                    subscriptionsChangedCallback();
                }
            }
        };
    }

    public void start() {
        mSubscriptionManager.addOnSubscriptionsChangedListener(
                mContext.getMainExecutor(), mSubscriptionsChangedListener);
        mContext.getContentResolver()
                .registerContentObserver(mAirplaneModeSettingUri, false, this);
        final IntentFilter radioTechnologyChangedFilter = new IntentFilter(
                TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, radioTechnologyChangedFilter);
    }

    public void stop() {
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mSubscriptionsChangedListener);
        mContext.getContentResolver().unregisterContentObserver(this);
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    public boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void subscriptionsChangedCallback() {
        mClient.onSubscriptionsChanged();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        if (uri.equals(mAirplaneModeSettingUri)) {
            mClient.onAirplaneModeChanged(isAirplaneModeOn());
        }
    }
}

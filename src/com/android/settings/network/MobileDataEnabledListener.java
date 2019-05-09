/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.network;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SubscriptionManager;

/** Helper class to listen for changes in the enabled state of mobile data. */
public class MobileDataEnabledListener extends ContentObserver {
    private Context mContext;
    private Client mClient;
    private int mSubId;

    public interface Client {
        void onMobileDataEnabledChange();
    }

    public MobileDataEnabledListener(Context context, Client client) {
        super(new Handler());
        mContext = context;
        mClient = client;
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    /** Starts listening to changes in the enabled state for data on the given subscription id. */
    public void start(int subId) {
        mSubId = subId;
        Uri uri;
        if (mSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA);
        }  else {
            uri = Settings.Global.getUriFor(Settings.Global.MOBILE_DATA + mSubId);
        }
        mContext.getContentResolver().registerContentObserver(uri, true /*notifyForDescendants*/,
                this);
    }

    public int getSubId() {
        return mSubId;
    }

    public MobileDataEnabledListener stop() {
        mContext.getContentResolver().unregisterContentObserver(this);
        return this;
    }

    @Override
    public void onChange(boolean selfChange) {
        mClient.onMobileDataEnabledChange();
    }
}

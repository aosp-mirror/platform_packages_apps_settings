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

package com.android.settings.network.telephony.cdma;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.TelephonyBasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller related to CDMA category
 */
public abstract class CdmaBasePreferenceController extends TelephonyBasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    protected Preference mPreference;
    protected TelephonyManager mTelephonyManager;
    protected PreferenceManager mPreferenceManager;
    private DataContentObserver mDataContentObserver;

    public CdmaBasePreferenceController(Context context, String key) {
        super(context, key);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mDataContentObserver = new DataContentObserver(new Handler(Looper.getMainLooper()));
    }

    @Override
    public void onStart() {
        mDataContentObserver.register(mContext, mSubId);
    }

    @Override
    public void onStop() {
        mDataContentObserver.unRegister(mContext);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return MobileNetworkUtils.isCdmaOptions(mContext, subId)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    public void init(PreferenceManager preferenceManager, int subId) {
        mPreferenceManager = preferenceManager;
        mSubId = subId;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(mSubId);
    }

    public void init(int subId) {
        init(null, subId);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference instanceof CdmaListPreference) {
            ((CdmaListPreference) mPreference).setSubId(mSubId);
        }
    }

    /**
     * Listener that listens to mobile data state change.
     */
    public class DataContentObserver extends ContentObserver {

        public DataContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateState(mPreference);
        }

        public void register(Context context, int subId) {
            final Uri uri = Settings.Global.getUriFor(
                    Settings.Global.PREFERRED_NETWORK_MODE + subId);
            context.getContentResolver().registerContentObserver(uri, false, this);
        }

        public void unRegister(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }
    }
}

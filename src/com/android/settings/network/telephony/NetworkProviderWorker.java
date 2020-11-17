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

package com.android.settings.network.telephony;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.network.MobileDataContentObserver;
import com.android.settings.network.MobileDataEnabledListener;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.wifi.slice.WifiScanWorker;

import java.util.Collections;
import java.util.concurrent.Executor;


/**
 * BackgroundWorker for Provider Model slice.
 */
public class NetworkProviderWorker extends WifiScanWorker implements
        SignalStrengthListener.Callback, MobileDataEnabledListener.Client,
        DataConnectivityListener.Client,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "NetworkProviderWorker";
    private static final int PROVIDER_MODEL_DEFAULT_EXPANDED_ROW_COUNT = 4;
    private DataContentObserver mMobileDataObserver;
    private SignalStrengthListener mSignalStrengthListener;
    private SubscriptionsChangeListener mSubscriptionsListener;
    private MobileDataEnabledListener mDataEnabledListener;
    private DataConnectivityListener mConnectivityListener;

    private final Context mContext;
    @VisibleForTesting
    final PhoneStateListener mPhoneStateListener;
    private final SubscriptionManager mSubscriptionManager;
    private final TelephonyManager mTelephonyManager;

    public NetworkProviderWorker(Context context, Uri uri) {
        super(context, uri);
        // Mobile data worker
        final Handler handler = new Handler(Looper.getMainLooper());
        mMobileDataObserver = new DataContentObserver(handler, this);

        mContext = context;
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);

        mPhoneStateListener = new NetworkProviderPhoneStateListener(handler::post);
        mSubscriptionsListener = new SubscriptionsChangeListener(context, this);
        mDataEnabledListener = new MobileDataEnabledListener(context, this);
        mConnectivityListener = new DataConnectivityListener(context, this);
        mSignalStrengthListener = new SignalStrengthListener(context, this);
    }

    @Override
    protected void onSlicePinned() {
        mMobileDataObserver.register(mContext,
                getDefaultSubscriptionId(mSubscriptionManager));

        mSubscriptionsListener.start();
        mDataEnabledListener.start(SubscriptionManager.getDefaultDataSubscriptionId());
        mConnectivityListener.start();
        mSignalStrengthListener.resume();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE
                | PhoneStateListener.LISTEN_ACTIVE_DATA_SUBSCRIPTION_ID_CHANGE
                | PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED);

        super.onSlicePinned();
    }

    @Override
    protected void onSliceUnpinned() {
        mMobileDataObserver.unregister(mContext);
        mSubscriptionsListener.stop();
        mDataEnabledListener.stop();
        mConnectivityListener.stop();
        mSignalStrengthListener.pause();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        super.onSliceUnpinned();
    }

    @Override
    public void close() {
        mMobileDataObserver = null;
        super.close();
    }

    @Override
    public int getApRowCount() {
        return PROVIDER_MODEL_DEFAULT_EXPANDED_ROW_COUNT;
    }

    /**
     * To update the Slice.
     */
    public void updateSlice() {
        notifySliceChange();
    }

    @Override
    public void onSubscriptionsChanged() {
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        Log.d(TAG, "onSubscriptionsChanged: defaultDataSubId:" + defaultDataSubId);

        mSignalStrengthListener.updateSubscriptionIds(
                SubscriptionManager.isUsableSubscriptionId(defaultDataSubId)
                        ? Collections.singleton(defaultDataSubId) : Collections.emptySet());
        if (defaultDataSubId != mDataEnabledListener.getSubId()) {
            mDataEnabledListener.stop();
            mDataEnabledListener.start(defaultDataSubId);
        }
        updateSlice();
    }

    @Override
    public void onSignalStrengthChanged() {
        Log.d(TAG, "onSignalStrengthChanged");
        updateSlice();
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        Log.d(TAG, "onAirplaneModeChanged");
        updateSlice();
    }

    @Override
    public void onMobileDataEnabledChange() {
        Log.d(TAG, "onMobileDataEnabledChange");
        updateSlice();
    }

    @Override
    public void onDataConnectivityChange() {
        Log.d(TAG, "onDataConnectivityChange");
        updateSlice();
    }

    /**
     * Listen to update of mobile data change.
     */
    public class DataContentObserver extends ContentObserver {
        private final NetworkProviderWorker mNetworkProviderWorker;

        public DataContentObserver(Handler handler, NetworkProviderWorker backgroundWorker) {
            super(handler);
            mNetworkProviderWorker = backgroundWorker;
        }

        @Override
        public void onChange(boolean selfChange) {
            mNetworkProviderWorker.updateSlice();
        }

        /**
         * To register the observer for mobile data changed.
         * @param context the Context object.
         * @param subId the default data subscription id.
         */
        public void register(Context context, int subId) {
            final Uri uri = MobileDataContentObserver.getObservableUri(context, subId);
            context.getContentResolver().registerContentObserver(uri, false, this);
        }

        /**
         * To unregister the observer for mobile data changed.
         * @param context the Context object.
         */
        public void unregister(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }
    }

    class NetworkProviderPhoneStateListener extends PhoneStateListener {
        NetworkProviderPhoneStateListener(Executor executor) {
            super(executor);
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            Log.d(TAG, "onServiceStateChanged voiceState=" + state.getState()
                    + " dataState=" + state.getDataRegistrationState());
            updateSlice();
        }

        @Override
        public void onActiveDataSubscriptionIdChanged(int subId) {
            Log.d(TAG, "onActiveDataSubscriptionIdChanged: subId=" + subId);
            updateSlice();
        }

        @Override
        public void onDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo) {
            Log.d(TAG, "onDisplayInfoChanged: telephonyDisplayInfo=" + telephonyDisplayInfo);
            updateSlice();
        }
    }

    protected static int getDefaultSubscriptionId(SubscriptionManager subscriptionManager) {
        final SubscriptionInfo defaultSubscription = subscriptionManager.getActiveSubscriptionInfo(
                subscriptionManager.getDefaultDataSubscriptionId());

        if (defaultSubscription == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID; // No default subscription
        }
        return defaultSubscription.getSubscriptionId();
    }
}

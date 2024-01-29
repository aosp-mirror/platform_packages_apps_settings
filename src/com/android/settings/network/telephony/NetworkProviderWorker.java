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

import static com.android.settings.network.InternetUpdater.INTERNET_ETHERNET;
import static com.android.settings.network.MobileIconGroupExtKt.getSummaryForSub;
import static com.android.settingslib.mobile.MobileMappings.getIconKey;
import static com.android.settingslib.mobile.MobileMappings.mapIconSets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.network.InternetUpdater;
import com.android.settings.network.MobileDataContentObserver;
import com.android.settings.network.MobileDataEnabledListener;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.wifi.slice.WifiScanWorker;
import com.android.settingslib.SignalIcon.MobileIconGroup;
import com.android.settingslib.mobile.MobileMappings;
import com.android.settingslib.mobile.MobileMappings.Config;
import com.android.settingslib.mobile.TelephonyIcons;

import java.util.Collections;

/**
 * BackgroundWorker for Provider Model slice.
 */
public class NetworkProviderWorker extends WifiScanWorker implements
        SignalStrengthListener.Callback, MobileDataEnabledListener.Client,
        DataConnectivityListener.Client, InternetUpdater.InternetChangeListener,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "NetworkProviderWorker";
    private static final int PROVIDER_MODEL_DEFAULT_EXPANDED_ROW_COUNT = 6;
    private DataContentObserver mMobileDataObserver;
    private SignalStrengthListener mSignalStrengthListener;
    private SubscriptionsChangeListener mSubscriptionsListener;
    private MobileDataEnabledListener mDataEnabledListener;
    private DataConnectivityListener mConnectivityListener;
    private int mDefaultDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private final Context mContext;
    final Handler mHandler;
    @VisibleForTesting
    final NetworkProviderTelephonyCallback mTelephonyCallback;
    private final BroadcastReceiver mConnectionChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                Log.d(TAG, "ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
                updateListener();
            }
        }
    };

    private TelephonyManager mTelephonyManager;
    private Config mConfig = null;
    private TelephonyDisplayInfo mTelephonyDisplayInfo =
            new TelephonyDisplayInfo(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE);
    private InternetUpdater mInternetUpdater;
    private @InternetUpdater.InternetType int mInternetType;

    public NetworkProviderWorker(Context context, Uri uri) {
        super(context, uri);
        // Mobile data worker
        mHandler = new Handler(Looper.getMainLooper());
        mMobileDataObserver = new DataContentObserver(mHandler, this);

        mContext = context;
        mDefaultDataSubId = getDefaultDataSubscriptionId();
        Log.d(TAG, "Init, SubId: " + mDefaultDataSubId);
        mTelephonyManager = mContext.getSystemService(
                TelephonyManager.class).createForSubscriptionId(mDefaultDataSubId);
        mTelephonyCallback = new NetworkProviderTelephonyCallback();
        mSubscriptionsListener = new SubscriptionsChangeListener(context, this);
        mDataEnabledListener = new MobileDataEnabledListener(context, this);
        mConnectivityListener = new DataConnectivityListener(context, this);
        mSignalStrengthListener = new SignalStrengthListener(context, this);
        mConfig = getConfig(mContext);

        mInternetUpdater = new InternetUpdater(mContext, getLifecycle(), this);
        mInternetType = mInternetUpdater.getInternetType();
    }

    @Override
    protected void onSlicePinned() {
        Log.d(TAG, "onSlicePinned");
        mMobileDataObserver.register(mContext, mDefaultDataSubId);
        mSubscriptionsListener.start();
        mDataEnabledListener.start(mDefaultDataSubId);
        mConnectivityListener.start();
        mSignalStrengthListener.resume();
        mTelephonyManager.registerTelephonyCallback(mHandler::post, mTelephonyCallback);
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mContext.registerReceiver(mConnectionChangeReceiver, filter);
        super.onSlicePinned();
    }

    @Override
    protected void onSliceUnpinned() {
        Log.d(TAG, "onSliceUnpinned");
        mMobileDataObserver.unregister(mContext);
        mSubscriptionsListener.stop();
        mDataEnabledListener.stop();
        mConnectivityListener.stop();
        mSignalStrengthListener.pause();
        mTelephonyManager.unregisterTelephonyCallback(mTelephonyCallback);
        if (mConnectionChangeReceiver != null) {
            mContext.unregisterReceiver(mConnectionChangeReceiver);
        }
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

    private void updateListener() {
        int defaultDataSubId = getDefaultDataSubscriptionId();
        if (mDefaultDataSubId == defaultDataSubId) {
            Log.d(TAG, "DDS: no change");
            return;
        }
        mDefaultDataSubId = defaultDataSubId;
        Log.d(TAG, "DDS: defaultDataSubId:" + mDefaultDataSubId);
        if (SubscriptionManager.isUsableSubscriptionId(defaultDataSubId)) {
            mTelephonyManager.unregisterTelephonyCallback(mTelephonyCallback);
            mMobileDataObserver.unregister(mContext);

            mSignalStrengthListener.updateSubscriptionIds(Collections.singleton(defaultDataSubId));
            mTelephonyManager = mTelephonyManager.createForSubscriptionId(defaultDataSubId);
            mTelephonyManager.registerTelephonyCallback(mHandler::post, mTelephonyCallback);
            mMobileDataObserver.register(mContext, defaultDataSubId);
            mConfig = getConfig(mContext);
        } else {
            mSignalStrengthListener.updateSubscriptionIds(Collections.emptySet());
        }
        updateSlice();
    }

    @Override
    public void onSubscriptionsChanged() {
        Log.d(TAG, "onSubscriptionsChanged");
        updateListener();
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
            Log.d(TAG, "DataContentObserver: init");
            mNetworkProviderWorker = backgroundWorker;
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "DataContentObserver: onChange");
            mNetworkProviderWorker.updateSlice();
        }

        /**
         * To register the observer for mobile data changed.
         *
         * @param context the Context object.
         * @param subId the default data subscription id.
         */
        public void register(Context context, int subId) {
            final Uri uri = MobileDataContentObserver.getObservableUri(context, subId);
            Log.d(TAG, "DataContentObserver: register uri:" + uri);
            context.getContentResolver().registerContentObserver(uri, false, this);
        }

        /**
         * To unregister the observer for mobile data changed.
         *
         * @param context the Context object.
         */
        public void unregister(Context context) {
            Log.d(TAG, "DataContentObserver: unregister");
            context.getContentResolver().unregisterContentObserver(this);
        }
    }

    class NetworkProviderTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.DataConnectionStateListener,
            TelephonyCallback.DisplayInfoListener,
            TelephonyCallback.ServiceStateListener {
        @Override
        public void onServiceStateChanged(ServiceState state) {
            Log.d(TAG, "onServiceStateChanged voiceState=" + state.getState()
                    + " dataState=" + state.getDataRegistrationState());
            updateSlice();
        }

        @Override
        public void onDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo) {
            Log.d(TAG, "onDisplayInfoChanged: telephonyDisplayInfo=" + telephonyDisplayInfo);
            mTelephonyDisplayInfo = telephonyDisplayInfo;
            updateSlice();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            Log.d(TAG,
                    "onDataConnectionStateChanged: networkType=" + networkType + " state=" + state);
            updateSlice();
        }
    }

    @VisibleForTesting
    int getDefaultDataSubscriptionId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    private String updateNetworkTypeName(Context context, Config config,
            TelephonyDisplayInfo telephonyDisplayInfo, int subId) {
        if (mWifiPickerTrackerHelper != null
                && mWifiPickerTrackerHelper.isCarrierNetworkActive()) {
            MobileIconGroup carrierMergedWifiIconGroup = TelephonyIcons.CARRIER_MERGED_WIFI;
            return getSummaryForSub(carrierMergedWifiIconGroup, context, subId);
        }

        String iconKey = getIconKey(telephonyDisplayInfo);
        return getSummaryForSub(mapIconSets(config).get(iconKey), context, subId);
    }

    @VisibleForTesting
    Config getConfig(Context context) {
        return MobileMappings.Config.readConfig(context);
    }

    /**
     * Get currently description of mobile network type.
     */
    public String getNetworkTypeDescription() {
        return updateNetworkTypeName(mContext, mConfig, mTelephonyDisplayInfo,
                mDefaultDataSubId);
    }

    /**
     * Called when internet type is changed.
     *
     * @param internetType the internet type
     */
    public void onInternetTypeChanged(@InternetUpdater.InternetType int internetType) {
        if (mInternetType == internetType) {
            return;
        }
        boolean changeWithEthernet =
                mInternetType == INTERNET_ETHERNET || internetType == INTERNET_ETHERNET;
        mInternetType = internetType;
        if (changeWithEthernet) {
            updateSlice();
        }
    }

    /**
     * Returns the internet type.
     */
    public @InternetUpdater.InternetType int getInternetType() {
        return mInternetType;
    }
}

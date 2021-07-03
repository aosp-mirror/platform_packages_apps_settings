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

package com.android.settings.panel;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.network.AirplaneModePreferenceController;
import com.android.settings.network.InternetUpdater;
import com.android.settings.network.ProviderModelSliceHelper;
import com.android.settings.network.SubscriptionsChangeListener;
import com.android.settings.network.telephony.DataConnectivityListener;
import com.android.settings.slices.CustomSliceRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the Internet Connectivity Panel.
 */
public class InternetConnectivityPanel implements PanelContent, LifecycleObserver,
        InternetUpdater.InternetChangeListener, DataConnectivityListener.Client,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "InternetConnectivityPanel";
    private static final int SUBTITLE_TEXT_NONE = -1;
    private static final int SUBTITLE_TEXT_WIFI_IS_OFF = R.string.wifi_is_off;
    private static final int SUBTITLE_TEXT_TAP_A_NETWORK_TO_CONNECT =
            R.string.tap_a_network_to_connect;
    private static final int SUBTITLE_TEXT_SEARCHING_FOR_NETWORKS =
            R.string.wifi_empty_list_wifi_on;
    private static final int SUBTITLE_TEXT_NON_CARRIER_NETWORK_UNAVAILABLE =
            R.string.non_carrier_network_unavailable;
    private static final int SUBTITLE_TEXT_ALL_CARRIER_NETWORK_UNAVAILABLE =
            R.string.all_network_unavailable;

    private final Context mContext;
    private final WifiManager mWifiManager;
    private final IntentFilter mWifiStateFilter;
    private final NetworkProviderTelephonyCallback mTelephonyCallback;
    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            if (TextUtils.equals(intent.getAction(), WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                updateProgressBar();
                updatePanelTitle();
                return;
            }

            if (TextUtils.equals(intent.getAction(), WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                updateProgressBar();
                updatePanelTitle();
            }
        }
    };

    @VisibleForTesting
    boolean mIsProviderModelEnabled;
    @VisibleForTesting
    InternetUpdater mInternetUpdater;
    @VisibleForTesting
    ProviderModelSliceHelper mProviderModelSliceHelper;

    private int mSubtitle = SUBTITLE_TEXT_NONE;
    private PanelContentCallback mCallback;
    private TelephonyManager mTelephonyManager;
    private SubscriptionsChangeListener mSubscriptionsListener;
    private DataConnectivityListener mConnectivityListener;
    private int mDefaultDataSubid = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    // Wi-Fi scanning progress bar
    protected HandlerInjector mHandlerInjector;
    protected boolean mIsProgressBarVisible;
    protected boolean mIsScanningSubTitleShownOnce;
    protected Runnable mHideProgressBarRunnable = () -> {
        setProgressBarVisible(false);
    };
    protected Runnable mHideScanningSubTitleRunnable = () -> {
        mIsScanningSubTitleShownOnce = true;
        updatePanelTitle();
    };

    /**
     * Wrapper for testing compatibility.
     */
    @VisibleForTesting
    static class HandlerInjector {
        protected final Handler mHandler;

        HandlerInjector(Context context) {
            mHandler = context.getMainThreadHandler();
        }

        public void postDelay(Runnable runnable) {
            mHandler.postDelayed(runnable, 2000 /* delay millis */);
        }

        public void removeCallbacks(Runnable runnable) {
            mHandler.removeCallbacks(runnable);
        }
    }

    private InternetConnectivityPanel(Context context) {
        mContext = context.getApplicationContext();
        mHandlerInjector = new HandlerInjector(context);
        mIsProviderModelEnabled = Utils.isProviderModelEnabled(mContext);
        mInternetUpdater = new InternetUpdater(context, null /* Lifecycle */, this);

        mSubscriptionsListener = new SubscriptionsChangeListener(context, this);
        mConnectivityListener = new DataConnectivityListener(context, this);
        mTelephonyCallback = new NetworkProviderTelephonyCallback();
        mDefaultDataSubid = getDefaultDataSubscriptionId();
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);

        mWifiManager = mContext.getSystemService(WifiManager.class);
        mWifiStateFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        mProviderModelSliceHelper = new ProviderModelSliceHelper(mContext, null);
    }

    /** create the panel */
    public static InternetConnectivityPanel create(Context context) {
        return new InternetConnectivityPanel(context);
    }

    /** @OnLifecycleEvent(ON_RESUME) */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        if (!mIsProviderModelEnabled) {
            return;
        }
        mInternetUpdater.onResume();
        mSubscriptionsListener.start();
        mConnectivityListener.start();
        mTelephonyManager.registerTelephonyCallback(
                new HandlerExecutor(new Handler(Looper.getMainLooper())), mTelephonyCallback);
        mContext.registerReceiver(mWifiStateReceiver, mWifiStateFilter);
        updateProgressBar();
        updatePanelTitle();
    }

    /** @OnLifecycleEvent(ON_PAUSE) */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        if (!mIsProviderModelEnabled) {
            return;
        }
        mInternetUpdater.onPause();
        mSubscriptionsListener.stop();
        mConnectivityListener.stop();
        mTelephonyManager.unregisterTelephonyCallback(mTelephonyCallback);
        mContext.unregisterReceiver(mWifiStateReceiver);
        mHandlerInjector.removeCallbacks(mHideProgressBarRunnable);
        mHandlerInjector.removeCallbacks(mHideScanningSubTitleRunnable);
    }

    /**
     * @return a string for the title of the Panel.
     */
    @Override
    public CharSequence getTitle() {
        if (mIsProviderModelEnabled) {
            return mContext.getText(mInternetUpdater.isAirplaneModeOn()
                    ? R.string.airplane_mode : R.string.provider_internet_settings);
        }
        return mContext.getText(R.string.internet_connectivity_panel_title);
    }

    /**
     * @return a string for the subtitle of the Panel.
     */
    @Override
    public CharSequence getSubTitle() {
        if (mIsProviderModelEnabled && mSubtitle != SUBTITLE_TEXT_NONE) {
            return mContext.getText(mSubtitle);
        }
        return null;
    }

    @Override
    public List<Uri> getSlices() {
        final List<Uri> uris = new ArrayList<>();
        if (mIsProviderModelEnabled) {
            uris.add(CustomSliceRegistry.PROVIDER_MODEL_SLICE_URI);
        } else {
            uris.add(CustomSliceRegistry.WIFI_SLICE_URI);
            uris.add(CustomSliceRegistry.MOBILE_DATA_SLICE_URI);
            uris.add(AirplaneModePreferenceController.SLICE_URI);
        }
        return uris;
    }

    @Override
    public Intent getSeeMoreIntent() {
        // Disable the see more button for provider model design.
        if (mIsProviderModelEnabled) {
            return null;
        }

        // Don't remove the see more intent for non-provider model design. This intent will be
        // used when isCustomizedButtonUsed() returns false.
        return new Intent(Settings.ACTION_WIRELESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    public boolean isProgressBarVisible() {
        return mIsProgressBarVisible;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PANEL_INTERNET_CONNECTIVITY;
    }

    @Override
    public void registerCallback(PanelContentCallback callback) {
        mCallback = callback;
    }

    /**
     * Called when airplane mode state is changed.
     */
    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        log("onAirplaneModeChanged: isAirplaneModeOn:" + isAirplaneModeOn);
        updatePanelTitle();
    }

    /**
     * Called when Wi-Fi enabled is changed.
     */
    @Override
    public void onWifiEnabledChanged(boolean enabled) {
        log("onWifiEnabledChanged: enabled:" + enabled);
        updatePanelTitle();
    }

    @Override
    public void onSubscriptionsChanged() {
        final int defaultDataSubId = getDefaultDataSubscriptionId();
        log("onSubscriptionsChanged: defaultDataSubId:" + defaultDataSubId);
        if (mDefaultDataSubid == defaultDataSubId) {
            return;
        }
        if (SubscriptionManager.isUsableSubscriptionId(defaultDataSubId)) {
            mTelephonyManager.unregisterTelephonyCallback(mTelephonyCallback);
            mTelephonyManager.registerTelephonyCallback(
                    new HandlerExecutor(new Handler(Looper.getMainLooper())), mTelephonyCallback);
        }
        updatePanelTitle();
    }

    @Override
    public void onDataConnectivityChange() {
        log("onDataConnectivityChange");
        updatePanelTitle();
    }

    @VisibleForTesting
    void updatePanelTitle() {
        if (mCallback == null) {
            return;
        }
        updateSubtitleText();
        mCallback.onHeaderChanged();
    }

    @VisibleForTesting
    int getDefaultDataSubscriptionId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    private void updateSubtitleText() {
        mSubtitle = SUBTITLE_TEXT_NONE;
        if (!mInternetUpdater.isWifiEnabled()) {
            if (!mInternetUpdater.isAirplaneModeOn()) {
                // When the airplane mode is off and Wi-Fi is disabled.
                //   Sub-Title: Wi-Fi is off
                log("Airplane mode off + Wi-Fi off.");
                mSubtitle = SUBTITLE_TEXT_WIFI_IS_OFF;
            }
            return;
        }

        if (mInternetUpdater.isAirplaneModeOn()) {
            return;
        }

        final List<ScanResult> wifiList = mWifiManager.getScanResults();
        if (wifiList != null && wifiList.size() != 0) {
            // When the Wi-Fi scan result is not empty
            //   Sub-Title: Tap a network to connect
            mSubtitle = SUBTITLE_TEXT_TAP_A_NETWORK_TO_CONNECT;
            return;
        }

        if (!mIsScanningSubTitleShownOnce && mIsProgressBarVisible) {
            // When the Wi-Fi scan result callback is received
            //   Sub-Title: Searching for networks...
            mSubtitle = SUBTITLE_TEXT_SEARCHING_FOR_NETWORKS;
            return;
        }

        // Sub-Title:
        // show non_carrier_network_unavailable
        //   - while Wi-Fi on + no Wi-Fi item
        //   - while Wi-Fi on + no Wi-Fi item + mobile data off
        // show all_network_unavailable:
        //   - while Wi-Fi on + no Wi-Fi item + no carrier item
        //   - while Wi-Fi on + no Wi-Fi item + service is out of service
        //   - while Wi-Fi on + no Wi-Fi item + mobile data on + no carrier data.
        log("No Wi-Fi item.");
        if (!mProviderModelSliceHelper.hasCarrier()
                || (!mProviderModelSliceHelper.isVoiceStateInService()
                && !mProviderModelSliceHelper.isDataStateInService())) {
            log("no carrier or service is out of service.");
            mSubtitle = SUBTITLE_TEXT_ALL_CARRIER_NETWORK_UNAVAILABLE;
            return;
        }
        if (!mProviderModelSliceHelper.isMobileDataEnabled()) {
            log("mobile data off");
            mSubtitle = SUBTITLE_TEXT_NON_CARRIER_NETWORK_UNAVAILABLE;
            return;
        }
        if (!mProviderModelSliceHelper.isDataSimActive()) {
            log("no carrier data.");
            mSubtitle = SUBTITLE_TEXT_ALL_CARRIER_NETWORK_UNAVAILABLE;
            return;
        }
        mSubtitle = SUBTITLE_TEXT_NON_CARRIER_NETWORK_UNAVAILABLE;
    }

    protected void updateProgressBar() {
        if (mWifiManager == null || !mInternetUpdater.isWifiEnabled()) {
            setProgressBarVisible(false);
            return;
        }

        setProgressBarVisible(true);
        List<ScanResult> wifiScanResults = mWifiManager.getScanResults();
        if (wifiScanResults != null && wifiScanResults.size() > 0) {
            mHandlerInjector.postDelay(mHideProgressBarRunnable);
        } else if (!mIsScanningSubTitleShownOnce) {
            mHandlerInjector.postDelay(mHideScanningSubTitleRunnable);
        }
    }

    protected void setProgressBarVisible(boolean visible) {
        if (mIsProgressBarVisible == visible) {
            return;
        }
        mIsProgressBarVisible = visible;

        if (mCallback == null) {
            return;
        }
        mCallback.onProgressBarVisibleChanged();
        updatePanelTitle();
    }

    private class NetworkProviderTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.DataConnectionStateListener,
            TelephonyCallback.ServiceStateListener {
        @Override
        public void onServiceStateChanged(ServiceState state) {
            log("onServiceStateChanged voiceState=" + state.getState()
                    + " dataState=" + state.getDataRegistrationState());
            updatePanelTitle();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            log("onDataConnectionStateChanged: networkType=" + networkType + " state=" + state);
            updatePanelTitle();
        }
    }

    private static void log(String s) {
        Log.d(TAG, s);
    }
}

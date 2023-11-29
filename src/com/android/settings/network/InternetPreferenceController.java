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

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.android.settings.network.InternetUpdater.INTERNET_CELLULAR;
import static com.android.settings.network.InternetUpdater.INTERNET_ETHERNET;
import static com.android.settings.network.InternetUpdater.INTERNET_NETWORKS_AVAILABLE;
import static com.android.settings.network.InternetUpdater.INTERNET_OFF;
import static com.android.settings.network.InternetUpdater.INTERNET_WIFI;
import static com.android.settingslib.wifi.WifiUtils.getHotspotIconResource;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.telephony.SubscriptionManager;

import androidx.annotation.IdRes;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.widget.SummaryUpdater;
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.settings.wifi.WifiSummaryUpdater;
import com.android.settings.wifi.repository.SharedConnectivityRepository;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.utils.ThreadUtils;
import com.android.wifitrackerlib.HotspotNetworkEntry;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PreferenceController to update the internet state.
 */
public class InternetPreferenceController extends AbstractPreferenceController implements
        LifecycleObserver, SummaryUpdater.OnSummaryChangeListener,
        InternetUpdater.InternetChangeListener, MobileNetworkRepository.MobileNetworkCallback,
        DefaultSubscriptionReceiver.DefaultSubscriptionListener,
        WifiPickerTracker.WifiPickerTrackerCallback {

    public static final String KEY = "internet_settings";

    private Preference mPreference;
    @VisibleForTesting
    WifiSummaryUpdater mSummaryHelper;
    private InternetUpdater mInternetUpdater;
    private @InternetUpdater.InternetType int mInternetType;
    private LifecycleOwner mLifecycleOwner;
    private MobileNetworkRepository mMobileNetworkRepository;
    private List<SubscriptionInfoEntity> mSubInfoEntityList = new ArrayList<>();
    private int mDefaultDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private DefaultSubscriptionReceiver mDataSubscriptionChangedReceiver;
    private boolean mIsHotspotNetworkEnabled = SharedConnectivityRepository.isDeviceConfigEnabled();
    @VisibleForTesting
    WifiPickerTrackerHelper mWifiPickerTrackerHelper;

    @VisibleForTesting
    static Map<Integer, Integer> sIconMap = new HashMap<>();
    static {
        sIconMap.put(INTERNET_OFF, R.drawable.ic_no_internet_unavailable);
        sIconMap.put(INTERNET_NETWORKS_AVAILABLE, R.drawable.ic_no_internet_available);
        sIconMap.put(INTERNET_WIFI, R.drawable.ic_wifi_signal_4);
        sIconMap.put(INTERNET_CELLULAR, R.drawable.ic_network_cell);
        sIconMap.put(INTERNET_ETHERNET, R.drawable.ic_settings_ethernet);
    }

    private static Map<Integer, Integer> sSummaryMap = new HashMap<>();
    static {
        sSummaryMap.put(INTERNET_OFF, R.string.condition_airplane_title);
        sSummaryMap.put(INTERNET_NETWORKS_AVAILABLE, R.string.networks_available);
        sSummaryMap.put(INTERNET_WIFI, 0);
        sSummaryMap.put(INTERNET_CELLULAR, 0);
        sSummaryMap.put(INTERNET_ETHERNET, R.string.to_switch_networks_disconnect_ethernet);
    }

    public InternetPreferenceController(Context context, Lifecycle lifecycle,
            LifecycleOwner lifecycleOwner) {
        super(context);
        if (lifecycle == null) {
            throw new IllegalArgumentException("Lifecycle must be set");
        }
        mSummaryHelper = new WifiSummaryUpdater(mContext, this);
        mInternetUpdater = new InternetUpdater(context, lifecycle, this);
        mInternetType = mInternetUpdater.getInternetType();
        mLifecycleOwner = lifecycleOwner;
        mMobileNetworkRepository = MobileNetworkRepository.getInstance(context);
        mDataSubscriptionChangedReceiver = new DefaultSubscriptionReceiver(context, this);
        if (mIsHotspotNetworkEnabled) {
            mWifiPickerTrackerHelper = new WifiPickerTrackerHelper(lifecycle, context, this);
        }
        lifecycle.addObserver(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY);
    }

    private void drawIcon(int iconResId) {
        Drawable drawable = mContext.getDrawable(iconResId);
        if (drawable != null) {
            drawable.setTintList(Utils.getColorAttr(mContext, android.R.attr.colorControlNormal));
            mPreference.setIcon(drawable);
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (mPreference == null) {
            return;
        }

        if (mInternetType == INTERNET_WIFI && updateHotspotNetwork()) {
            return;
        }

        final @IdRes int icon = sIconMap.get(mInternetType);
        if (icon != 0) {
            drawIcon(icon);
        }

        if (mInternetType == INTERNET_WIFI) {
            mPreference.setSummary(mSummaryHelper.getSummary());
            return;
        }

        if (mInternetType == INTERNET_CELLULAR) {
            updateCellularSummary();
            return;
        }

        final @IdRes int summary = sSummaryMap.get(mInternetType);
        if (summary != 0) {
            mPreference.setSummary(summary);
        }
    }

    @VisibleForTesting
    boolean updateHotspotNetwork() {
        if (mWifiPickerTrackerHelper == null) {
            return false;
        }
        WifiEntry entry = mWifiPickerTrackerHelper.getWifiPickerTracker().getConnectedWifiEntry();
        if (!(entry instanceof HotspotNetworkEntry)) {
            return false;
        }
        drawIcon(getHotspotIconResource(((HotspotNetworkEntry) entry).getDeviceType()));
        mPreference.setSummary(((HotspotNetworkEntry) entry).getAlternateSummary());
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    /** @OnLifecycleEvent(ON_RESUME) */
    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        mMobileNetworkRepository.addRegister(mLifecycleOwner, this,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mMobileNetworkRepository.updateEntity();
        mSummaryHelper.register(true);
        mDataSubscriptionChangedReceiver.registerReceiver();
        mDefaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
    }

    /** @OnLifecycleEvent(ON_PAUSE) */
    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        mMobileNetworkRepository.removeRegister(this);
        mSummaryHelper.register(false);
        mDataSubscriptionChangedReceiver.unRegisterReceiver();
    }

    /**
     * Called when internet type is changed.
     *
     * @param internetType the internet type
     */
    @Override
    public void onInternetTypeChanged(@InternetUpdater.InternetType int internetType) {
        final boolean needUpdate = (internetType != mInternetType);
        mInternetType = internetType;
        if (needUpdate) {
            ThreadUtils.postOnMainThread(() -> {
                updateState(mPreference);
            });
        }
    }

    /**
     * Called when airplane mode state is changed.
     */
    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        ThreadUtils.postOnMainThread(() -> {
            updateState(mPreference);
        });
    }

    @Override
    public void onSummaryChanged(String summary) {
        if (mInternetType == INTERNET_WIFI) {
            updateState(mPreference);
        }
    }

    @VisibleForTesting
    void updateCellularSummary() {
        CharSequence summary = null;
        SubscriptionInfoEntity activeSubInfo = null;
        SubscriptionInfoEntity defaultSubInfo = null;

        for (SubscriptionInfoEntity subInfo : getSubscriptionInfoList()) {
            if (subInfo.isActiveDataSubscriptionId) {
                activeSubInfo = subInfo;
            }
            if (subInfo.getSubId() == getDefaultDataSubscriptionId()) {
                defaultSubInfo = subInfo;
            }
        }
        if (activeSubInfo == null || defaultSubInfo == null) {
            return;
        }
        activeSubInfo = activeSubInfo.isSubscriptionVisible ? activeSubInfo : defaultSubInfo;

        if (activeSubInfo.equals(defaultSubInfo)) {
            // DDS is active
            summary = activeSubInfo.uniqueName;
        } else {
            summary = mContext.getString(
                    R.string.mobile_data_temp_using, activeSubInfo.uniqueName);
        }

        mPreference.setSummary(summary);
    }

    @VisibleForTesting
    protected List<SubscriptionInfoEntity> getSubscriptionInfoList() {
        return mSubInfoEntityList;
    }

    @VisibleForTesting
    protected int getDefaultDataSubscriptionId() {
        return mDefaultDataSubId;
    }

    @Override
    public void onAvailableSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        mSubInfoEntityList = subInfoEntityList;
        updateState(mPreference);
    }

    @Override
    public void onDefaultDataChanged(int defaultDataSubId) {
        mDefaultDataSubId = defaultDataSubId;
        updateState(mPreference);
    }

    @Override
    public void onWifiEntriesChanged() {
        if (mInternetType == INTERNET_WIFI) {
            updateState(mPreference);
        }
    }

    @Override
    public void onWifiStateChanged() {
        // Do nothing
    }

    @Override
    public void onNumSavedNetworksChanged() {
        // Do nothing
    }

    @Override
    public void onNumSavedSubscriptionsChanged() {
        // Do nothing
    }
}

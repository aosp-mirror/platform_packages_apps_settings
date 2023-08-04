/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.wifi.tether;

import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_OPEN;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION;

import static com.android.settings.wifi.repository.WifiHotspotRepository.SPEED_6GHZ;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.wifi.repository.WifiHotspotRepository;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wi-Fi Hotspot Security View Model for {@link WifiHotspotSecuritySettings}
 */
public class WifiHotspotSecurityViewModel extends AndroidViewModel {
    private static final String TAG = "WifiHotspotSecurityViewModel";

    public static final String KEY_SECURITY_WPA3 = "wifi_hotspot_security_wpa3";
    public static final String KEY_SECURITY_WPA2_WPA3 = "wifi_hotspot_security_wpa2_wpa3";
    public static final String KEY_SECURITY_WPA2 = "wifi_hotspot_security_wpa2";
    public static final String KEY_SECURITY_NONE = "wifi_hotspot_security_none";

    protected Map<Integer, ViewItem> mViewItemMap = new HashMap<>();
    protected MutableLiveData<List<ViewItem>> mViewInfoListData;

    protected final WifiHotspotRepository mWifiHotspotRepository;
    protected final Observer<Integer> mSecurityTypeObserver = st -> onSecurityTypeChanged(st);
    protected final Observer<Integer> mSpeedTypeObserver = st -> onSpeedTypeChanged(st);

    public WifiHotspotSecurityViewModel(
            @NotNull Application application) {
        super(application);
        mViewItemMap.put(SECURITY_TYPE_WPA3_SAE, new ViewItem(KEY_SECURITY_WPA3));
        mViewItemMap.put(SECURITY_TYPE_WPA3_SAE_TRANSITION, new ViewItem(KEY_SECURITY_WPA2_WPA3));
        mViewItemMap.put(SECURITY_TYPE_WPA2_PSK, new ViewItem(KEY_SECURITY_WPA2));
        mViewItemMap.put(SECURITY_TYPE_OPEN, new ViewItem(KEY_SECURITY_NONE));

        mWifiHotspotRepository = FeatureFactory.getFeatureFactory().getWifiFeatureProvider()
                .getWifiHotspotRepository();
        mWifiHotspotRepository.getSecurityType().observeForever(mSecurityTypeObserver);
        mWifiHotspotRepository.getSpeedType().observeForever(mSpeedTypeObserver);
    }

    @Override
    protected void onCleared() {
        mWifiHotspotRepository.getSecurityType().removeObserver(mSecurityTypeObserver);
        mWifiHotspotRepository.getSpeedType().removeObserver(mSpeedTypeObserver);
    }

    protected void onSecurityTypeChanged(int securityType) {
        log("onSecurityTypeChanged(), securityType:" + securityType);
        for (Map.Entry<Integer, ViewItem> entry : mViewItemMap.entrySet()) {
            entry.getValue().mIsChecked = entry.getKey().equals(securityType);
        }
        updateViewItemListData();
    }

    protected void onSpeedTypeChanged(Integer speedType) {
        log("onSpeedTypeChanged(), speedType:" + speedType);
        boolean isWpa3Only = (speedType == SPEED_6GHZ);
        for (Map.Entry<Integer, ViewItem> entry : mViewItemMap.entrySet()) {
            if (entry.getKey() != SECURITY_TYPE_WPA3_SAE) {
                entry.getValue().mIsEnabled = !isWpa3Only;
            }
        }
        updateViewItemListData();
    }

    /**
     * Handle RadioButton Clicked
     */
    public void handleRadioButtonClicked(String key) {
        log("handleRadioButtonClicked(), key:" + key);
        for (Map.Entry<Integer, ViewItem> entry : mViewItemMap.entrySet()) {
            ViewItem viewItem = entry.getValue();
            if (viewItem.mKey.equals(key)) {
                mWifiHotspotRepository.setSecurityType(entry.getKey());
                return;
            }
        }
    }

    /**
     * Gets ViewItemList LiveData
     */
    public LiveData<List<ViewItem>> getViewItemListData() {
        if (mViewInfoListData == null) {
            mViewInfoListData = new MutableLiveData<>();
            updateViewItemListData();
            log("getViewItemListData(), mViewInfoListData:" + mViewInfoListData.getValue());
        }
        return mViewInfoListData;
    }

    protected void updateViewItemListData() {
        if (mViewInfoListData == null) {
            return;
        }
        mViewInfoListData.setValue(mViewItemMap.values().stream().toList());
    }

    /**
     * Gets Restarting LiveData
     */
    public LiveData<Boolean> getRestarting() {
        return mWifiHotspotRepository.getRestarting();
    }

    /**
     * Wi-Fi Hotspot View Item
     */
    public static final class ViewItem {
        String mKey;
        boolean mIsChecked;
        boolean mIsEnabled = true;

        public ViewItem(String key) {
            mKey = key;
        }

        @Override
        public String toString() {
            return new StringBuilder("ViewItem:{")
                    .append("Key:").append(mKey)
                    .append(",IsChecked:").append(mIsChecked)
                    .append(",IsEnabled:").append(mIsEnabled)
                    .append('}').toString();
        }
    }

    private void log(String msg) {
        FeatureFactory.getFeatureFactory().getWifiFeatureProvider().verboseLog(TAG, msg);
    }
}

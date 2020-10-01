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
 * limitations under the License.
 */

package com.android.settings.wifi.dpp;

import android.app.Application;
import android.net.wifi.EasyConnectStatusCallback;
import android.net.wifi.WifiManager;
import android.util.SparseArray;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class WifiDppInitiatorViewModel extends AndroidViewModel {
    private MutableLiveData<Integer> mEnrolleeSuccessNetworkId;
    private MutableLiveData<Integer> mStatusCode;
    private boolean mIsWifiDppHandshaking;
    private String mTriedSsid;
    private SparseArray<int[]> mTriedChannels;
    private int[] mBandArray;

    public WifiDppInitiatorViewModel(Application application) {
        super(application);
    }

    MutableLiveData<Integer> getEnrolleeSuccessNetworkId() {
        if (mEnrolleeSuccessNetworkId == null) {
            mEnrolleeSuccessNetworkId = new MutableLiveData<>();
        }

        return mEnrolleeSuccessNetworkId;
    }

    MutableLiveData<Integer> getStatusCode() {
        if (mStatusCode == null) {
            mStatusCode = new MutableLiveData<>();
        }

        return mStatusCode;
    }

    String getTriedSsid() {
        return mTriedSsid;
    }

    SparseArray<int[]> getTriedChannels() {
        return mTriedChannels;
    }

    int[] getBandArray() {
        return mBandArray;
    }

    boolean isWifiDppHandshaking() {
        return mIsWifiDppHandshaking;
    }

    void startEasyConnectAsConfiguratorInitiator(String qrCode, int networkId) {
        mIsWifiDppHandshaking = true;
        final WifiManager wifiManager = getApplication().getSystemService(WifiManager.class);

        wifiManager.startEasyConnectAsConfiguratorInitiator(qrCode, networkId,
                WifiManager.EASY_CONNECT_NETWORK_ROLE_STA, getApplication().getMainExecutor(),
                new EasyConnectDelegateCallback());
    }

    void startEasyConnectAsEnrolleeInitiator(String qrCode) {
        mIsWifiDppHandshaking = true;
        final WifiManager wifiManager = getApplication().getSystemService(WifiManager.class);

        wifiManager.startEasyConnectAsEnrolleeInitiator(qrCode, getApplication().getMainExecutor(),
                new EasyConnectDelegateCallback());
    }

    private class EasyConnectDelegateCallback extends EasyConnectStatusCallback {
        @Override
        public void onEnrolleeSuccess(int newNetworkId) {
            mIsWifiDppHandshaking = false;
            mEnrolleeSuccessNetworkId.setValue(newNetworkId);
        }

        @Override
        public void onConfiguratorSuccess(int code) {
            mIsWifiDppHandshaking = false;
            mStatusCode.setValue(WifiDppUtils.EASY_CONNECT_EVENT_SUCCESS);
        }

        @Override
        public void onFailure(int code, String ssid, SparseArray<int[]> channelListArray,
                int[] operatingClassArray) {
            mIsWifiDppHandshaking = false;
            mTriedSsid = ssid;
            mTriedChannels = channelListArray;
            mBandArray = operatingClassArray;
            mStatusCode.setValue(code);
        }

        @Override
        public void onProgress(int code) {
            // Do nothing
        }
    }
}

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

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.AndroidViewModel;

public class WifiDppInitiatorViewModel extends AndroidViewModel {
    private MutableLiveData<Integer> mEnrolleeSuccessNetworkId;
    private MutableLiveData<Integer> mStatusCode;
    private boolean mIsGoingInitiator;

    public WifiDppInitiatorViewModel(Application application) {
        super(application);
    }

    public MutableLiveData<Integer> getEnrolleeSuccessNetworkId() {
        if (mEnrolleeSuccessNetworkId == null) {
            mEnrolleeSuccessNetworkId = new MutableLiveData<>();
        }

        return mEnrolleeSuccessNetworkId;
    }

    public MutableLiveData<Integer> getStatusCode() {
        if (mStatusCode == null) {
            mStatusCode = new MutableLiveData<>();
        }

        return mStatusCode;
    }

    public boolean isGoingInitiator() {
        return mIsGoingInitiator;
    }

    public void startEasyConnectAsConfiguratorInitiator(String qrCode, int networkId) {
        mIsGoingInitiator = true;
        final WifiManager wifiManager = getApplication().getSystemService(WifiManager.class);

        wifiManager.startEasyConnectAsConfiguratorInitiator(qrCode, networkId,
                WifiManager.EASY_CONNECT_NETWORK_ROLE_STA, getApplication().getMainExecutor(),
                new EasyConnectDelegateCallback());
    }

    public void startEasyConnectAsEnrolleeInitiator(String qrCode) {
        mIsGoingInitiator = true;
        final WifiManager wifiManager = getApplication().getSystemService(WifiManager.class);

        wifiManager.startEasyConnectAsEnrolleeInitiator(qrCode, getApplication().getMainExecutor(),
                new EasyConnectDelegateCallback());
    }

    private class EasyConnectDelegateCallback extends EasyConnectStatusCallback {
        @Override
        public void onEnrolleeSuccess(int newNetworkId) {
            mIsGoingInitiator = false;
            mEnrolleeSuccessNetworkId.setValue(newNetworkId);
        }

        @Override
        public void onConfiguratorSuccess(int code) {
            mIsGoingInitiator = false;
            mStatusCode.setValue(WifiDppUtils.EASY_CONNECT_EVENT_SUCCESS);
        }

        @Override
        public void onFailure(int code) {
            mIsGoingInitiator = false;
            mStatusCode.setValue(code);
        }

        @Override
        public void onProgress(int code) {
            // Do nothing
        }
    }
}

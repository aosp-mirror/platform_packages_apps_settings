/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;


/**
 * Show the current status details of Wifi related fields
 */
public class WifiStatusTest extends Activity {

    private static final String TAG = "WifiStatusTest";

    private Button updateButton;
    private TextView mWifiState;
    private TextView mNetworkState;
    private TextView mSupplicantState;
    private TextView mRSSI;
    private TextView mBSSID;
    private TextView mSSID;
    private TextView mHiddenSSID;
    private TextView mIPAddr;
    private TextView mMACAddr;
    private TextView mNetworkId;
    private TextView mTxLinkSpeed;
    private TextView mRxLinkSpeed;
    private TextView mScanList;


    private TextView mPingHostname;
    private TextView mHttpClientTest;
    private Button pingTestButton;

    private String mPingHostnameResult;
    private String mHttpClientTestResult;


    private WifiManager mWifiManager;
    private IntentFilter mWifiStateFilter;


    //============================
    // Activity lifecycle
    //============================

    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                handleWifiStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                            WifiManager.WIFI_STATE_UNKNOWN));
            } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                handleNetworkStateChanged(
                        (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
            } else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                handleScanResultsAvailable();
            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                /* TODO: handle supplicant connection change later */
            } else if (intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                handleSupplicantStateChanged(
                        (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE),
                        intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR),
                        intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0));
            } else if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {
                handleSignalChanged(intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0));
            } else if (intent.getAction().equals(WifiManager.NETWORK_IDS_CHANGED_ACTION)) {
                /* TODO: handle network id change info later */
            } else {
                Log.e(TAG, "Received an unknown Wifi Intent");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        mWifiStateFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mWifiStateFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mWifiStateFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        registerReceiver(mWifiStateReceiver, mWifiStateFilter);

        setContentView(R.layout.wifi_status_test);

        updateButton = (Button) findViewById(R.id.update);
        updateButton.setOnClickListener(updateButtonHandler);

        mWifiState = (TextView) findViewById(R.id.wifi_state);
        mNetworkState = (TextView) findViewById(R.id.network_state);
        mSupplicantState = (TextView) findViewById(R.id.supplicant_state);
        mRSSI = (TextView) findViewById(R.id.rssi);
        mBSSID = (TextView) findViewById(R.id.bssid);
        mSSID = (TextView) findViewById(R.id.ssid);
        mHiddenSSID = (TextView) findViewById(R.id.hidden_ssid);
        mIPAddr = (TextView) findViewById(R.id.ipaddr);
        mMACAddr = (TextView) findViewById(R.id.macaddr);
        mNetworkId = (TextView) findViewById(R.id.networkid);
        mTxLinkSpeed = (TextView) findViewById(R.id.tx_link_speed);
        mRxLinkSpeed = (TextView) findViewById(R.id.rx_link_speed);
        mScanList = (TextView) findViewById(R.id.scan_list);


        mPingHostname = (TextView) findViewById(R.id.pingHostname);
        mHttpClientTest = (TextView) findViewById(R.id.httpClientTest);

        pingTestButton = (Button) findViewById(R.id.ping_test);
        pingTestButton.setOnClickListener(mPingButtonHandler);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mWifiStateReceiver, mWifiStateFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mWifiStateReceiver);
    }

    OnClickListener mPingButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            updatePingState();
        }
    };

    OnClickListener updateButtonHandler = new OnClickListener() {
        public void onClick(View v) {
            final WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

            setWifiStateText(mWifiManager.getWifiState());
            mBSSID.setText(wifiInfo.getBSSID());
            mHiddenSSID.setText(String.valueOf(wifiInfo.getHiddenSSID()));
            int ipAddr = wifiInfo.getIpAddress();
            StringBuffer ipBuf = new StringBuffer();
            ipBuf.append(ipAddr  & 0xff).append('.').
                append((ipAddr >>>= 8) & 0xff).append('.').
                append((ipAddr >>>= 8) & 0xff).append('.').
                append((ipAddr >>>= 8) & 0xff);

            mIPAddr.setText(ipBuf);
            mTxLinkSpeed.setText(String.valueOf(wifiInfo.getTxLinkSpeedMbps())+" Mbps");
            mRxLinkSpeed.setText(String.valueOf(wifiInfo.getRxLinkSpeedMbps())+" Mbps");
            mMACAddr.setText(wifiInfo.getMacAddress());
            mNetworkId.setText(String.valueOf(wifiInfo.getNetworkId()));
            mRSSI.setText(String.valueOf(wifiInfo.getRssi()));
            mSSID.setText(wifiInfo.getSSID());

            SupplicantState supplicantState = wifiInfo.getSupplicantState();
            setSupplicantStateText(supplicantState);
        }
    };

    private void setSupplicantStateText(SupplicantState supplicantState) {
        if(SupplicantState.FOUR_WAY_HANDSHAKE.equals(supplicantState)) {
            mSupplicantState.setText("FOUR WAY HANDSHAKE");
        } else if(SupplicantState.ASSOCIATED.equals(supplicantState)) {
            mSupplicantState.setText("ASSOCIATED");
        } else if(SupplicantState.ASSOCIATING.equals(supplicantState)) {
            mSupplicantState.setText("ASSOCIATING");
        } else if(SupplicantState.COMPLETED.equals(supplicantState)) {
            mSupplicantState.setText("COMPLETED");
        } else if(SupplicantState.DISCONNECTED.equals(supplicantState)) {
            mSupplicantState.setText("DISCONNECTED");
        } else if(SupplicantState.DORMANT.equals(supplicantState)) {
            mSupplicantState.setText("DORMANT");
        } else if(SupplicantState.GROUP_HANDSHAKE.equals(supplicantState)) {
            mSupplicantState.setText("GROUP HANDSHAKE");
        } else if(SupplicantState.INACTIVE.equals(supplicantState)) {
            mSupplicantState.setText("INACTIVE");
        } else if(SupplicantState.INVALID.equals(supplicantState)) {
            mSupplicantState.setText("INVALID");
        } else if(SupplicantState.SCANNING.equals(supplicantState)) {
            mSupplicantState.setText("SCANNING");
        } else if(SupplicantState.UNINITIALIZED.equals(supplicantState)) {
            mSupplicantState.setText("UNINITIALIZED");
        } else {
            mSupplicantState.setText("BAD");
            Log.e(TAG, "supplicant state is bad");
        }
    }

    private void setWifiStateText(int wifiState) {
        String wifiStateString;
        switch(wifiState) {
            case WifiManager.WIFI_STATE_DISABLING:
                wifiStateString = getString(R.string.wifi_state_disabling);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                wifiStateString = getString(R.string.wifi_state_disabled);
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                wifiStateString = getString(R.string.wifi_state_enabling);
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                wifiStateString = getString(R.string.wifi_state_enabled);
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
                wifiStateString = getString(R.string.wifi_state_unknown);
                break;
            default:
                wifiStateString = "BAD";
                Log.e(TAG, "wifi state is bad");
                break;
        }

        mWifiState.setText(wifiStateString);
    }

    private void handleSignalChanged(int rssi) {
        mRSSI.setText(String.valueOf(rssi));
    }

    private void handleWifiStateChanged(int wifiState) {
        setWifiStateText(wifiState);
    }

    private void handleScanResultsAvailable() {
        List<ScanResult> list = mWifiManager.getScanResults();

        StringBuffer scanList = new StringBuffer();
        if (list != null) {
            for (int i = list.size() - 1; i >= 0; i--) {
                final ScanResult scanResult = list.get(i);

                if (scanResult == null) {
                    continue;
                }

                if (TextUtils.isEmpty(scanResult.SSID)) {
                    continue;
                }

                scanList.append(scanResult.SSID+" ");
            }
        }
        mScanList.setText(scanList);
    }

    private void handleSupplicantStateChanged(SupplicantState state, boolean hasError, int error) {
        if (hasError) {
            mSupplicantState.setText("ERROR AUTHENTICATING");
        } else {
            setSupplicantStateText(state);
        }
    }

    private void handleNetworkStateChanged(NetworkInfo networkInfo) {
        if (mWifiManager.isWifiEnabled()) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            String summary = AccessPoint.getSummary(this, info.getSSID(),
                    networkInfo.getDetailedState(),
                    info.getNetworkId() == WifiConfiguration.INVALID_NETWORK_ID,
                    /* suggestionOrSpecifierPackageName */ null);
            mNetworkState.setText(summary);
        }
    }

    private final void updatePingState() {
        final Handler handler = new Handler();
        // Set all to unknown since the threads will take a few secs to update.
        mPingHostnameResult = getResources().getString(R.string.radioInfo_unknown);
        mHttpClientTestResult = getResources().getString(R.string.radioInfo_unknown);

        mPingHostname.setText(mPingHostnameResult);
        mHttpClientTest.setText(mHttpClientTestResult);

        final Runnable updatePingResults = new Runnable() {
            public void run() {
                mPingHostname.setText(mPingHostnameResult);
                mHttpClientTest.setText(mHttpClientTestResult);
            }
        };

        Thread hostnameThread = new Thread() {
            @Override
            public void run() {
                pingHostname();
                handler.post(updatePingResults);
            }
        };
        hostnameThread.start();

        Thread httpClientThread = new Thread() {
            @Override
            public void run() {
                httpClientTest();
                handler.post(updatePingResults);
            }
        };
        httpClientThread.start();
    }

    private final void pingHostname() {
        try {
            // TODO: Hardcoded for now, make it UI configurable
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 100 www.google.com");
            int status = p.waitFor();
            if (status == 0) {
                mPingHostnameResult = "Pass";
            } else {
                mPingHostnameResult = "Fail: Host unreachable";
            }
        } catch (UnknownHostException e) {
            mPingHostnameResult = "Fail: Unknown Host";
        } catch (IOException e) {
            mPingHostnameResult= "Fail: IOException";
        } catch (InterruptedException e) {
            mPingHostnameResult = "Fail: InterruptedException";
        }
    }

    private void httpClientTest() {
        HttpURLConnection urlConnection = null;
        try {
            // TODO: Hardcoded for now, make it UI configurable
            URL url = new URL("https://www.google.com");
            urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection.getResponseCode() == 200) {
                mHttpClientTestResult = "Pass";
            } else {
                mHttpClientTestResult = "Fail: Code: " + urlConnection.getResponseMessage();
            }
        } catch (IOException e) {
            mHttpClientTestResult = "Fail: IOException";
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

}

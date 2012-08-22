/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import com.android.settings.R;


/**
 * Dialog to show WPS progress.
 */
public class WpsDialog extends AlertDialog {

    private final static String TAG = "WpsDialog";

    private View mView;
    private TextView mTextView;
    private ProgressBar mTimeoutBar;
    private ProgressBar mProgressBar;
    private Button mButton;
    private Timer mTimer;

    private static final int WPS_TIMEOUT_S = 120;

    private WifiManager mWifiManager;
    private WifiManager.WpsListener mWpsListener;
    private int mWpsSetup;

    private final IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    private Context mContext;
    private Handler mHandler = new Handler();

    private enum DialogState {
        WPS_INIT,
        WPS_START,
        WPS_COMPLETE,
        CONNECTED, //WPS + IP config is done
        WPS_FAILED
    }
    DialogState mDialogState = DialogState.WPS_INIT;

    public WpsDialog(Context context, int wpsSetup) {
        super(context);
        mContext = context;
        mWpsSetup = wpsSetup;

        class WpsListener implements WifiManager.WpsListener {
            public void onStartSuccess(String pin) {
                if (pin != null) {
                    updateDialog(DialogState.WPS_START, String.format(
                            mContext.getString(R.string.wifi_wps_onstart_pin), pin));
                } else {
                    updateDialog(DialogState.WPS_START, mContext.getString(
                            R.string.wifi_wps_onstart_pbc));
                }
            }
            public void onCompletion() {
                updateDialog(DialogState.WPS_COMPLETE,
                        mContext.getString(R.string.wifi_wps_complete));
            }

            public void onFailure(int reason) {
                String msg;
                switch (reason) {
                    case WifiManager.WPS_OVERLAP_ERROR:
                        msg = mContext.getString(R.string.wifi_wps_failed_overlap);
                        break;
                    case WifiManager.WPS_WEP_PROHIBITED:
                        msg = mContext.getString(R.string.wifi_wps_failed_wep);
                        break;
                    case WifiManager.WPS_TKIP_ONLY_PROHIBITED:
                        msg = mContext.getString(R.string.wifi_wps_failed_tkip);
                        break;
                    case WifiManager.IN_PROGRESS:
                        msg = mContext.getString(R.string.wifi_wps_in_progress);
                        break;
                    default:
                        msg = mContext.getString(R.string.wifi_wps_failed_generic);
                        break;
                }
                updateDialog(DialogState.WPS_FAILED, msg);
            }
        }

        mWpsListener = new WpsListener();


        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.wifi_wps_dialog, null);

        mTextView = (TextView) mView.findViewById(R.id.wps_dialog_txt);
        mTextView.setText(R.string.wifi_wps_setup_msg);

        mTimeoutBar = ((ProgressBar) mView.findViewById(R.id.wps_timeout_bar));
        mTimeoutBar.setMax(WPS_TIMEOUT_S);
        mTimeoutBar.setProgress(0);

        mProgressBar = ((ProgressBar) mView.findViewById(R.id.wps_progress_bar));
        mProgressBar.setVisibility(View.GONE);

        mButton = ((Button) mView.findViewById(R.id.wps_dialog_btn));
        mButton.setText(R.string.wifi_cancel);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        setView(mView);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        /*
         * increment timeout bar per second.
         */
        mTimer = new Timer(false);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mTimeoutBar.incrementProgressBy(1);
                    }
                });
            }
        }, 1000, 1000);

        mContext.registerReceiver(mReceiver, mFilter);

        WpsInfo wpsConfig = new WpsInfo();
        wpsConfig.setup = mWpsSetup;
        mWifiManager.startWps(wpsConfig, mWpsListener);
    }

    @Override
    protected void onStop() {
        if (mDialogState != DialogState.WPS_COMPLETE) {
            mWifiManager.cancelWps(null);
        }

        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    private void updateDialog(final DialogState state, final String msg) {
        if (mDialogState.ordinal() >= state.ordinal()) {
            //ignore.
            return;
        }
        mDialogState = state;

        mHandler.post(new Runnable() {
                @Override
                public void run() {
                    switch(state) {
                        case WPS_COMPLETE:
                            mTimeoutBar.setVisibility(View.GONE);
                            mProgressBar.setVisibility(View.VISIBLE);
                            break;
                        case CONNECTED:
                        case WPS_FAILED:
                            mButton.setText(mContext.getString(R.string.dlg_ok));
                            mTimeoutBar.setVisibility(View.GONE);
                            mProgressBar.setVisibility(View.GONE);
                            if (mReceiver != null) {
                                mContext.unregisterReceiver(mReceiver);
                                mReceiver = null;
                            }
                            break;
                    }
                    mTextView.setText(msg);
                }
            });
   }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
            final NetworkInfo.DetailedState state = info.getDetailedState();
            if (state == DetailedState.CONNECTED &&
                    mDialogState == DialogState.WPS_COMPLETE) {
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String msg = String.format(mContext.getString(
                            R.string.wifi_wps_connected), wifiInfo.getSSID());
                    updateDialog(DialogState.CONNECTED, msg);
                }
            }
        }
    }

}

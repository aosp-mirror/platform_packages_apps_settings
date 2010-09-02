/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.settings.R;

import android.app.Activity;
import android.net.NetworkInfo.DetailedState;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.EnumMap;

/**
 * WifiSetings Activity specific for SetupWizard with X-Large screen size.
 */
public class WifiSettingsForSetupWizardXL extends Activity implements OnClickListener {

    private static final EnumMap<DetailedState, DetailedState> stateMap =
        new EnumMap<DetailedState, DetailedState>(DetailedState.class);

    static {
        stateMap.put(DetailedState.IDLE, DetailedState.DISCONNECTED);
        stateMap.put(DetailedState.SCANNING, DetailedState.DISCONNECTED);
        stateMap.put(DetailedState.CONNECTING, DetailedState.CONNECTING);
        stateMap.put(DetailedState.AUTHENTICATING, DetailedState.CONNECTING);
        stateMap.put(DetailedState.OBTAINING_IPADDR, DetailedState.CONNECTING);
        stateMap.put(DetailedState.CONNECTED, DetailedState.CONNECTED);
        stateMap.put(DetailedState.SUSPENDED, DetailedState.SUSPENDED);  // ?
        stateMap.put(DetailedState.DISCONNECTING, DetailedState.DISCONNECTED);
        stateMap.put(DetailedState.DISCONNECTED, DetailedState.DISCONNECTED);
        stateMap.put(DetailedState.FAILED, DetailedState.DISCONNECTED);
    }

    private TextView mProgressText;
    private ProgressBar mProgressBar;
    private WifiSettings mWifiSettings;
    private TextView mStatusText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.wifi_settings_for_setup_wizard_xl);
        mWifiSettings =
                (WifiSettings)getFragmentManager().findFragmentById(R.id.wifi_setup_fragment);
        setup();
        // XXX: should we use method?
        getIntent().putExtra(WifiSettings.IN_XL_SETUP_WIZARD, true);
    }

    public void setup() {
        mProgressText = (TextView)findViewById(R.id.scanning_progress_text);
        mProgressBar = (ProgressBar)findViewById(R.id.scanning_progress_bar);
        mProgressBar.setMax(2);
        mProgressBar.setIndeterminate(true);
        mStatusText = (TextView)findViewById(R.id.wifi_setup_status);

        ((Button)findViewById(R.id.wifi_setup_refresh_list)).setOnClickListener(this);
        ((Button)findViewById(R.id.wifi_setup_add_network)).setOnClickListener(this);
        ((Button)findViewById(R.id.wifi_setup_skip_or_next)).setOnClickListener(this);
        ((Button)findViewById(R.id.wifi_setup_connect)).setOnClickListener(this);
        ((Button)findViewById(R.id.wifi_setup_forget)).setOnClickListener(this);
        ((Button)findViewById(R.id.wifi_setup_cancel)).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        switch (id) {
        case R.id.wifi_setup_refresh_list:
            mWifiSettings.refreshAccessPoints();
            break;
        case R.id.wifi_setup_add_network:
            mWifiSettings.onAddNetworkPressed();
            break;
        case R.id.wifi_setup_skip_or_next:
            setResult(Activity.RESULT_OK);
            finish();
            break;
        case R.id.wifi_setup_connect:
            mWifiSettings.submit();
            break;
        case R.id.wifi_setup_forget:
            mWifiSettings.forget();
            break;
        case R.id.wifi_setup_cancel:
            mWifiSettings.detachConfigPreference();
            break;
        }
    }

    public void updateConnectionState(DetailedState originalState) {
        final DetailedState state = stateMap.get(originalState);
        final String message;
        mProgressBar.setIndeterminate(false);
        switch (state) {
        case CONNECTING: {
            message = Summary.get(this, state);
            mProgressBar.setProgress(1);
            mStatusText.setText(R.string.wifi_setup_status_connecting);
            break;
        }
        case CONNECTED: {
            message = Summary.get(this, state);
            mProgressBar.setProgress(2);
            mStatusText.setText(R.string.wifi_setup_status_connected);
            break;
        }
        default:  // Not connected.
            message = getString(R.string.wifi_setup_not_connected);
            mProgressBar.setProgress(0);
            mStatusText.setText(R.string.wifi_setup_status_select_network);
            break;
        }
        mProgressText.setText(message);
    }

    public void onWifiConfigPreferenceAttached(boolean isNewNetwork) {
        if (isNewNetwork) {
            mStatusText.setText(R.string.wifi_setup_status_new_network);
        } else {
            mStatusText.setText(R.string.wifi_setup_status_existing_network);
        }
    }

    public void onForget() {
        mProgressBar.setIndeterminate(false);
        mProgressBar.setProgress(0);
        mProgressText.setText(getString(R.string.wifi_setup_not_connected));
    }

    public void onRefreshAccessPoints() {
        mProgressBar.setIndeterminate(true);
        mProgressText.setText(Summary.get(this, DetailedState.SCANNING));
    }
}

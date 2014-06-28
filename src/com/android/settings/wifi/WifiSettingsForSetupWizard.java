/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;

import com.android.settings.R;

/**
 * This customized version of WifiSettings is shown to the user only during Setup Wizard. Menu
 * selections are limited, clicking on an access point will auto-advance to the next screen (once
 * connected), and, if the user opts to skip ahead without a wifi connection, a warning message
 * alerts of possible carrier data charges or missing software updates.
 */
public class WifiSettingsForSetupWizard extends WifiSettings {

    private static final String TAG = "WifiSettingsForSetupWizard";

    /* Used in Wifi Setup context */

    // this boolean extra specifies whether to auto finish when connection is established
    private static final String EXTRA_AUTO_FINISH_ON_CONNECT = "wifi_auto_finish_on_connect";

    // show a text regarding data charges when wifi connection is required during setup wizard
    protected static final String EXTRA_SHOW_WIFI_REQUIRED_INFO = "wifi_show_wifi_required_info";

    // should activity finish once we have a connection?
    private boolean mAutoFinishOnConnection;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;

    public WifiSettingsForSetupWizard() {
        super();

        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                        WifiManager.EXTRA_NETWORK_INFO);
                if (mAutoFinishOnConnection && info.isConnected()) {
                    Log.d(TAG, "mReceiver.onReceive context=" + context + " intent=" + intent);
                    WifiSetupActivity activity = (WifiSetupActivity) getActivity();
                    activity.finishOrNext(Activity.RESULT_OK);
                }
            }
        };
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.setup_preference, container, false);

        final ListView list = (ListView) view.findViewById(android.R.id.list);
        final View title = view.findViewById(R.id.title);
        if (title == null) {
            final View header = inflater.inflate(R.layout.setup_wizard_header, list, false);
            list.addHeaderView(header);
        }

        final View other = inflater.inflate(R.layout.setup_wifi_add_network, list, false);
        list.addFooterView(other, null, true);
        other.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWifiManager.isWifiEnabled()) {
                    onAddNetworkPressed();
                }
            }
        });

        final Intent intent = getActivity().getIntent();
        if (intent.getBooleanExtra(EXTRA_SHOW_WIFI_REQUIRED_INFO, false)) {
            view.findViewById(R.id.wifi_required_info).setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getView().setSystemUiVisibility(
                View.STATUS_BAR_DISABLE_HOME |
                View.STATUS_BAR_DISABLE_RECENT |
                View.STATUS_BAR_DISABLE_NOTIFICATION_ALERTS |
                View.STATUS_BAR_DISABLE_CLOCK);

        final WifiSetupActivity activity = (WifiSetupActivity) getActivity();
        final Intent intent = activity.getIntent();

        // first if we're supposed to finish once we have a connection
        mAutoFinishOnConnection = intent.getBooleanExtra(EXTRA_AUTO_FINISH_ON_CONNECT, false);

        if (mAutoFinishOnConnection) {
            // Hide the next button
            if (hasNextButton()) {
                getNextButton().setVisibility(View.GONE);
            }

            /*
             * When entering with a savedInstanceState, we may be returning from a later activity in
             * the setup flow. It's not clear yet if there are other possible circumstances. It's
             * not appropriate to refire our activity results, so we skip that here.
             */
            if (savedInstanceState == null) {
                final ConnectivityManager connectivity = (ConnectivityManager)
                        activity.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivity != null &&
                        connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                    Log.d(TAG, "onActivityCreated Auto-finishing");
                    activity.finishOrNext(Activity.RESULT_OK);
                    return;
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void registerForContextMenu(View view) {
        // Suppressed during setup wizard
    }

    @Override
    /* package */ WifiEnabler createWifiEnabler() {
        // Not shown during setup wizard
        return null;
    }

    @Override
    /* package */ void addOptionsMenuItems(Menu menu) {
        final boolean wifiIsEnabled = mWifiManager.isWifiEnabled();
        final TypedArray ta = getActivity().getTheme()
                .obtainStyledAttributes(new int[] {R.attr.ic_wps});
        menu.add(Menu.NONE, MENU_ID_WPS_PBC, 0, R.string.wifi_menu_wps_pbc)
                .setIcon(ta.getDrawable(0))
                .setEnabled(wifiIsEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENU_ID_ADD_NETWORK, 0, R.string.wifi_add_network)
                .setEnabled(wifiIsEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        ta.recycle();
    }
}

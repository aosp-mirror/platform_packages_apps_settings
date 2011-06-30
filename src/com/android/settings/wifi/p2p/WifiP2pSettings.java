/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Switch;

import com.android.settings.wifi.p2p.WifiP2pDialog;
import com.android.settings.wifi.p2p.WifiP2pEnabler;
import com.android.settings.wifi.p2p.WifiP2pPeer;

import java.util.Arrays;
import java.util.List;
import java.util.Collection;

/*
 * Displays Wi-fi p2p settings UI
 */
public class WifiP2pSettings extends SettingsPreferenceFragment {

    private static final String TAG = "WifiP2pSettings";
    private static final int MENU_ID_SEARCH = Menu.FIRST;
    private static final int MENU_ID_CREATE_GROUP = Menu.FIRST + 1;
    private static final int MENU_ID_ADVANCED = Menu.FIRST +2;


    private final IntentFilter mIntentFilter = new IntentFilter();
    private final Handler mHandler = new WifiP2pHandler();
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pEnabler mWifiP2pEnabler;
    private WifiP2pDialog mConnectDialog;
    private OnClickListener mConnectListener;
    private OnClickListener mDisconnectListener;
    private WifiP2pPeer mSelectedWifiPeer;

    private static final int DIALOG_CONNECT     = 1;
    private static final int DIALOG_DISCONNECT  = 2;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                //TODO: nothing right now
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if (mWifiP2pManager != null) mWifiP2pManager.requestPeers();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.wifi_p2p_settings);

        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        final Activity activity = getActivity();
        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (!mWifiP2pManager.connectHandler(activity, mHandler)) {
            //Failure to set up connection
            Log.e(TAG, "Failed to set up connection with wifi p2p service");
            mWifiP2pManager = null;
        }

        Switch actionBarSwitch = new Switch(activity);

        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                actionBarSwitch.setPadding(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            ActionBar.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            }
        }

        mWifiP2pEnabler = new WifiP2pEnabler(activity, actionBarSwitch);

        //connect dialog listener
        mConnectListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    WifiP2pConfig config = mConnectDialog.getConfig();
                    if (mWifiP2pManager != null) {
                        mWifiP2pManager.connect(config);
                    }
                }
            }
        };

        //disconnect dialog listener
        mDisconnectListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    if (mWifiP2pManager != null) {
                        mWifiP2pManager.disconnect();
                    }
                }
            }
        };
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mReceiver, mIntentFilter);
        if (mWifiP2pEnabler != null) {
            mWifiP2pEnabler.resume();
        }
        if (mWifiP2pManager != null) mWifiP2pManager.discoverPeers();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWifiP2pEnabler != null) {
            mWifiP2pEnabler.pause();
        }
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, MENU_ID_SEARCH, 0, R.string.wifi_p2p_menu_search)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_ID_CREATE_GROUP, 0, R.string.wifi_p2p_menu_create_group)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_ID_ADVANCED, 0, R.string.wifi_p2p_menu_advanced)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_SEARCH:
                mWifiP2pManager.discoverPeers();
                return true;
            case MENU_ID_CREATE_GROUP:
                mWifiP2pManager.createGroup();
                return true;
            case MENU_ID_ADVANCED:
                //TODO: add advanced settings for p2p
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference instanceof WifiP2pPeer) {
            mSelectedWifiPeer = (WifiP2pPeer) preference;
            if (mSelectedWifiPeer.device.status == WifiP2pDevice.Status.CONNECTED) {
                showDialog(DIALOG_DISCONNECT);
            } else {
                showDialog(DIALOG_CONNECT);
            }
        }
        return super.onPreferenceTreeClick(screen, preference);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_CONNECT) {
            mConnectDialog = new WifiP2pDialog(getActivity(), mConnectListener,
                mSelectedWifiPeer.device);
            return mConnectDialog;
        } else if (id == DIALOG_DISCONNECT) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("Disconnect ?")
                .setMessage("Do you want to disconnect ?")
                .setPositiveButton(getActivity().getString(R.string.dlg_ok), mDisconnectListener)
                .setNegativeButton(getActivity().getString(R.string.dlg_cancel), null)
                .create();
            return dialog;
        }
        return null;
    }

    private void updatePeers(WifiP2pDeviceList peers) {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();

        for (WifiP2pDevice peer: peers.getDeviceList()) {
            preferenceScreen.addPreference(new WifiP2pPeer(getActivity(), peer));
        }
    }

    private class WifiP2pHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case WifiP2pManager.HANDLER_DISCONNECTION:
                    //Failure to set up connection
                    Log.e(TAG, "Lost connection with wifi p2p service");
                    mWifiP2pManager = null;
                    break;
                case WifiP2pManager.RESPONSE_PEERS:
                    updatePeers(mWifiP2pManager.peersInResponse(message));
                    break;
                default:
                    //Ignore
                    break;
            }
        }
    }

}

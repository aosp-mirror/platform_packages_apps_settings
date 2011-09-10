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

package com.android.settings.wifi.p2p;

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
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Arrays;
import java.util.List;
import java.util.Collection;

/*
 * Displays Wi-fi p2p settings UI
 */
public class WifiP2pSettings extends SettingsPreferenceFragment
        implements PeerListListener {

    private static final String TAG = "WifiP2pSettings";
    private static final int MENU_ID_SEARCH = Menu.FIRST;
    private static final int MENU_ID_CREATE_GROUP = Menu.FIRST + 1;
    private static final int MENU_ID_REMOVE_GROUP = Menu.FIRST + 2;
    private static final int MENU_ID_ADVANCED = Menu.FIRST +3;


    private final IntentFilter mIntentFilter = new IntentFilter();
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pDialog mConnectDialog;
    private OnClickListener mConnectListener;
    private OnClickListener mDisconnectListener;
    private WifiP2pPeer mSelectedWifiPeer;

    private PreferenceGroup mPeersGroup;
    private Preference mThisDevicePref;

    private static final int DIALOG_CONNECT     = 1;
    private static final int DIALOG_DISCONNECT  = 2;

    private WifiP2pDevice mThisDevice;
    private WifiP2pDeviceList mPeers = new WifiP2pDeviceList();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                //TODO: nothing right now
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if (mWifiP2pManager != null) {
                    mWifiP2pManager.requestPeers(mChannel, WifiP2pSettings.this);
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                if (mWifiP2pManager == null) return;
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    Log.d(TAG, "Connected");
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                mThisDevice = (WifiP2pDevice) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                Log.d(TAG, "Update device info: " + mThisDevice);
                updateDevicePref();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.wifi_p2p_settings);

        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        final Activity activity = getActivity();
        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (mWifiP2pManager != null) {
            mChannel = mWifiP2pManager.initialize(activity, getActivity().getMainLooper(), null);
            if (mChannel == null) {
                //Failure to set up connection
                Log.e(TAG, "Failed to set up connection with wifi p2p service");
                mWifiP2pManager = null;
            }
        } else {
            Log.e(TAG, "mWifiP2pManager is null !");
        }

        //connect dialog listener
        mConnectListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    WifiP2pConfig config = mConnectDialog.getConfig();
                    if (mWifiP2pManager != null) {
                        mWifiP2pManager.connect(mChannel, config,
                                new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                Log.d(TAG, " connect success");
                            }
                            public void onFailure(int reason) {
                                Log.d(TAG, " connect fail " + reason);
                            }
                        });
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
                        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                Log.d(TAG, " remove group success");
                            }
                            public void onFailure(int reason) {
                                Log.d(TAG, " remove group fail " + reason);
                            }
                        });
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

        if (mWifiP2pManager != null) {
            mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                Log.d(TAG, " discover success");
                            }
                            public void onFailure(int reason) {
                                Log.d(TAG, " discover fail " + reason);
                            }
                        });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, MENU_ID_SEARCH, 0, R.string.wifi_p2p_menu_search)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_ID_CREATE_GROUP, 0, R.string.wifi_p2p_menu_create_group)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_ID_REMOVE_GROUP, 0, R.string.wifi_p2p_menu_remove_group)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_ID_ADVANCED, 0, R.string.wifi_p2p_menu_advanced)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_SEARCH:
                if (mWifiP2pManager != null) {
                    mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                Log.d(TAG, " discover success");
                            }
                            public void onFailure(int reason) {
                                Log.d(TAG, " discover fail " + reason);
                            }
                        });
                }
                return true;
            case MENU_ID_CREATE_GROUP:
                if (mWifiP2pManager != null) {
                    mWifiP2pManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                Log.d(TAG, " create group success");
                            }
                            public void onFailure(int reason) {
                                Log.d(TAG, " create group fail " + reason);
                            }
                        });
                }
                return true;
            case MENU_ID_REMOVE_GROUP:
                if (mWifiP2pManager != null) {
                    mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                Log.d(TAG, " remove group success");
                            }
                            public void onFailure(int reason) {
                                Log.d(TAG, " remove group fail " + reason);
                            }
                        });
                }
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
            if (mSelectedWifiPeer.device.status == WifiP2pDevice.CONNECTED) {
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

    public void onPeersAvailable(WifiP2pDeviceList peers) {

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();

        preferenceScreen.setOrderingAsAdded(true);

        if (mPeersGroup == null) {
            mPeersGroup = new PreferenceCategory(getActivity());
        } else {
            mPeersGroup.removeAll();
        }

        preferenceScreen.addPreference(mThisDevicePref);

        mPeersGroup.setTitle(R.string.wifi_p2p_available_devices);
        mPeersGroup.setEnabled(true);
        preferenceScreen.addPreference(mPeersGroup);

        mPeers = peers;
        for (WifiP2pDevice peer: peers.getDeviceList()) {
            mPeersGroup.addPreference(new WifiP2pPeer(getActivity(), peer));
        }
    }

    private void updateDevicePref() {
        mThisDevicePref = new Preference(getActivity());

        if (mThisDevice != null) {
            if (TextUtils.isEmpty(mThisDevice.deviceName)) {
                mThisDevicePref.setTitle(mThisDevice.deviceAddress);
            } else {
                mThisDevicePref.setTitle(mThisDevice.deviceName);
            }

            if (mThisDevice.status == WifiP2pDevice.CONNECTED) {
                String[] statusArray = getActivity().getResources().getStringArray(
                        R.array.wifi_p2p_status);
                mThisDevicePref.setSummary(statusArray[mThisDevice.status]);
            }
            mThisDevicePref.setPersistent(false);
            mThisDevicePref.setEnabled(true);
            mThisDevicePref.setSelectable(false);
        }
        onPeersAvailable(mPeers); //update UI
    }
}

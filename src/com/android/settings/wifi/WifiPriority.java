/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.wifi;

import android.app.ListFragment;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.cyanogenmod.TouchInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WifiPriority extends ListFragment {

    private final TouchInterceptor.DropListener mDropListener =
            new TouchInterceptor.DropListener() {
        public void drop(int from, int to) {
            if (from == to) return;

            // Sort networks by user selection
            List<WifiConfiguration> mNetworks = mAdapter.getNetworks();
            WifiConfiguration o = mNetworks.remove(from);
            mNetworks.add(to, o);

            // Set the new priorities of the networks
            int cc = mNetworks.size();
            for (int i = 0; i < cc; i++) {
                WifiConfiguration network = mNetworks.get(i);
                network.priority = cc - i;

                // Update the priority
                mWifiManager.updateNetwork(network);
            }

            // Now, save all the Wi-Fi configuration with its new priorities
            mWifiManager.saveConfiguration();

            // Reload the networks
            mAdapter.reloadNetworks();
            mNetworksListView.invalidateViews();
        }
    };

    private WifiManager mWifiManager;
    private TouchInterceptor mNetworksListView;
    private WifiPriorityAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wifi_network_priority, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Context context = getActivity();
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        // Set the touchable listview
        mNetworksListView = (TouchInterceptor)getListView();
        mNetworksListView.setDropListener(mDropListener);
        mAdapter = new WifiPriorityAdapter(context, mWifiManager);
        setListAdapter(mAdapter);
    }

    @Override
    public void onDestroy() {
        mNetworksListView.setDropListener(null);
        setListAdapter(null);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Reload the networks
        mAdapter.reloadNetworks();
        mNetworksListView.invalidateViews();
    }

    private class WifiPriorityAdapter extends BaseAdapter {

        private final WifiManager mWifiManager;
        private final LayoutInflater mInflater;
        private List<WifiConfiguration> mNetworks;

        public WifiPriorityAdapter(Context ctx, WifiManager wifiManager) {
            mWifiManager = wifiManager;
            mInflater = LayoutInflater.from(ctx);
            reloadNetworks();
        }

        private void reloadNetworks() {
            mNetworks = mWifiManager.getConfiguredNetworks();
            if (mNetworks == null) {
                mNetworks = new ArrayList<WifiConfiguration>();
            }

            // Sort network list by priority (or by network id if the priority is the same)
            Collections.sort(mNetworks, new Comparator<WifiConfiguration>() {
                @Override
                public int compare(WifiConfiguration lhs, WifiConfiguration rhs) {
                    // > priority -- > lower position
                    if (lhs.priority < rhs.priority) return 1;
                    if (lhs.priority > rhs.priority) return -1;
                    // < network id -- > lower position
                    if (lhs.networkId < rhs.networkId) return -1;
                    if (lhs.networkId > rhs.networkId) return 1;
                    return 0;
                }
            });
        }

        /**package**/ List<WifiConfiguration> getNetworks() {
            return mNetworks;
        }

        @Override
        public int getCount() {
            return mNetworks.size();
        }

        @Override
        public Object getItem(int position) {
            return mNetworks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View v;
            if (convertView == null) {
                v = mInflater.inflate(R.layout.wifi_network_priority_list_item, null);
            } else {
                v = convertView;
            }

            WifiConfiguration network = (WifiConfiguration)getItem(position);

            final ImageView icon = (ImageView) v.findViewById(R.id.icon);
            if (network.getAuthType() != WifiConfiguration.KeyMgmt.NONE) {
                icon.setImageDrawable(getActivity().getResources().getDrawable(
                        R.drawable.wifi_signal_lock_dark));
            } else {
                icon.setImageDrawable(getActivity().getResources().getDrawable(
                        R.drawable.wifi_signal_dark));
            }

            final TextView name = (TextView) v.findViewById(R.id.name);
            // wpa_suplicant returns the SSID between double quotes. Remove them if are present.
            name.setText(filterSSID(network.SSID));

            return v;
        }

        private String filterSSID(String ssid) {
            // Filter only if has start and end double quotes
            if (ssid == null || !ssid.startsWith("\"") || !ssid.endsWith("\"")) {
                return ssid;
            }
            return ssid.substring(1, ssid.length()-1);
        }
    }
}

/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.NetworkRequestUserSelectionCallback;
import android.net.wifi.WifiManager.NetworkRequestMatchCallback;
import android.os.Handler;
import android.os.Message;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.internal.PreferenceImageView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.Utils;
import com.android.settingslib.wifi.AccessPoint;

import java.util.ArrayList;
import java.util.List;

public class NetworkRequestDialogFragment extends InstrumentedDialogFragment implements
        DialogInterface.OnClickListener, NetworkRequestMatchCallback {

    /** Message sent to us to stop scanning wifi and pop up timeout dialog. */
    private static final int MESSAGE_STOP_SCAN_WIFI_LIST = 0;

    /** Delayed time to stop scanning wifi. */
    private static final int DELAY_TIME_STOP_SCAN_MS = 30 * 1000;

    private List<AccessPoint> mAccessPointList;
    private AccessPointAdapter mDialogAdapter;
    private NetworkRequestUserSelectionCallback mUserSelectionCallback;

    public static NetworkRequestDialogFragment newInstance() {
        NetworkRequestDialogFragment dialogFragment = new NetworkRequestDialogFragment();
        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();

        // Prepares title.
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View customTitle = inflater.inflate(R.layout.network_request_dialog_title, null);

        final TextView title = customTitle.findViewById(R.id.network_request_title_text);
        title.setText(R.string.network_connection_request_dialog_title);
        final ProgressBar progressBar = customTitle.findViewById(
                R.id.network_request_title_progress);
        progressBar.setVisibility(View.VISIBLE);

        // Prepares adapter.
        mDialogAdapter = new AccessPointAdapter(context,
                R.layout.preference_access_point, getAccessPointList());

        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setCustomTitle(customTitle)
                .setAdapter(mDialogAdapter, this)
                .setPositiveButton(R.string.cancel, (dialog, which) -> getActivity().finish());

        // Clicking list item is to connect wifi ap.
        final AlertDialog dialog = builder.create();
        dialog.getListView()
                .setOnItemClickListener(
                        (parent, view, position, id) -> this.onClick(dialog, position));
        return dialog;
    }

    @NonNull
    List<AccessPoint> getAccessPointList() {
        // Initials list for adapter, in case of display crashing.
        if (mAccessPointList == null) {
            mAccessPointList = new ArrayList<>();
        }
        return mAccessPointList;
    }

    private BaseAdapter getDialogAdapter() {
        return mDialogAdapter;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final List<AccessPoint> accessPointList = getAccessPointList();
        if (accessPointList.size() == 0) {
            return;  // Invalid values.
        }
        if (mUserSelectionCallback == null) {
            return; // Callback is missing or not ready.
        }

        if (which < accessPointList.size()) {
            WifiConfiguration wifiConfig = accessPointList.get(which).getConfig();
            mUserSelectionCallback.select(wifiConfig);
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        // Finishes activity when user clicks back key or outside of dialog.
        getActivity().finish();
    }

    @Override
    public void onPause() {
        super.onPause();

        mHandler.removeMessages(MESSAGE_STOP_SCAN_WIFI_LIST);
        final WifiManager wifiManager = getContext().getApplicationContext()
                .getSystemService(WifiManager.class);
        if (wifiManager != null) {
            wifiManager.unregisterNetworkRequestMatchCallback(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final WifiManager wifiManager = getContext().getApplicationContext()
                .getSystemService(WifiManager.class);
        if (wifiManager != null) {
            wifiManager.registerNetworkRequestMatchCallback(this, mHandler);
        }
        // Sets time-out to stop scanning.
        mHandler.sendEmptyMessageDelayed(MESSAGE_STOP_SCAN_WIFI_LIST, DELAY_TIME_STOP_SCAN_MS);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STOP_SCAN_WIFI_LIST:
                    removeMessages(MESSAGE_STOP_SCAN_WIFI_LIST);
                    stopScanningAndPopTimeoutDialog();
                    break;
                default:
                    // Do nothing.
                    break;
            }
        }
    };

    protected void stopScanningAndPopTimeoutDialog() {
        // Dismisses current dialog.
        dismiss();

        // Throws new timeout dialog.
        final NetworkRequestTimeoutDialogFragment fragment = NetworkRequestTimeoutDialogFragment
                .newInstance();
        fragment.show(getActivity().getSupportFragmentManager(), null);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.WIFI_SCANNING_NEEDED_DIALOG;
    }

    private class AccessPointAdapter extends ArrayAdapter<AccessPoint> {

        private final int mResourceId;
        private final LayoutInflater mInflater;

        public AccessPointAdapter(Context context, int resourceId, List<AccessPoint> objects) {
            super(context, resourceId, objects);
            mResourceId = resourceId;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = mInflater.inflate(mResourceId, parent, false);

                final View divider = view.findViewById(
                        com.android.settingslib.R.id.two_target_divider);
                divider.setVisibility(View.GONE);
            }

            final AccessPoint accessPoint = getItem(position);

            final TextView titleView = view.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setText(accessPoint.getSsidStr());
            }

            final TextView summary = view.findViewById(android.R.id.summary);
            if (summary != null) {
                summary.setText(accessPoint.getSettingsSummary());
            }

            final PreferenceImageView imageView = view.findViewById(android.R.id.icon);
            final int level = accessPoint.getLevel();
            if (imageView != null) {
                final Drawable drawable = getContext().getDrawable(
                        Utils.getWifiIconResource(level));
                drawable.setTintList(
                        Utils.getColorAttr(getContext(), android.R.attr.colorControlNormal));
                imageView.setImageDrawable(drawable);
            }

            return view;
        }
    }

    @Override
    public void onAbort() {
        // TODO(b/117399926): We should have a UI notify user here.
    }

    @Override
    public void onUserSelectionCallbackRegistration(
            NetworkRequestUserSelectionCallback userSelectionCallback) {
        mUserSelectionCallback = userSelectionCallback;
    }

    @Override
    public void onMatch(List<ScanResult> scanResults) {
        // TODO(b/119846365): Checks if we could escalate the converting effort.
        // Converts ScanResult to WifiConfiguration.
        List<WifiConfiguration> wifiConfigurations = null;
        final WifiManager wifiManager = getContext().getApplicationContext()
                .getSystemService(WifiManager.class);
        if (wifiManager != null) {
            wifiConfigurations = wifiManager.getAllMatchingWifiConfigs(scanResults);
        }

        setUpAccessPointList(wifiConfigurations);

        if (getDialogAdapter() != null) {
            getDialogAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onUserSelectionConnectSuccess(WifiConfiguration wificonfiguration) {
        if (getDialogAdapter() != null) {
            updateAccessPointListItem(wificonfiguration);
            getDialogAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onUserSelectionConnectFailure(WifiConfiguration wificonfiguration) {
        if (mDialogAdapter != null) {
            updateAccessPointListItem(wificonfiguration);
            getDialogAdapter().notifyDataSetChanged();
        }
    }

    private void updateAccessPointListItem(WifiConfiguration wificonfiguration) {
        if (wificonfiguration == null) {
            return;
        }

        final List<AccessPoint> accessPointList = getAccessPointList();
        final int accessPointListSize = accessPointList.size();

        for (int i = 0; i < accessPointListSize; i++) {
            AccessPoint accessPoint = accessPointList.get(i);
            // It is the same AccessPoint SSID, and should be replaced to update latest properties.
            if (accessPoint.matches(wificonfiguration)) {
                accessPointList.set(i, new AccessPoint(getContext(), wificonfiguration));
                break;
            }
        }
    }

    private void setUpAccessPointList(List<WifiConfiguration> wifiConfigurations) {
        // Grants for zero size input, since maybe current wifi is off or somethings are wrong.
        if (wifiConfigurations == null) {
            return;
        }

        final List<AccessPoint> accessPointList = getAccessPointList();
        accessPointList.clear();
        for (WifiConfiguration config : wifiConfigurations) {
            accessPointList.add(new AccessPoint(getContext(), config));
        }
    }
}

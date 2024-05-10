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

import static com.android.wifitrackerlib.Utils.getSecurityTypesFromScanResult;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager.NetworkRequestUserSelectionCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.internal.PreferenceImageView;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.Utils;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * The Fragment sets up callback {@link NetworkRequestMatchCallback} with framework. To handle most
 * behaviors of the callback when requesting wifi network, except for error message. When error
 * happens, {@link NetworkRequestErrorDialogFragment} will be called to display error message.
 */
public class NetworkRequestDialogFragment extends NetworkRequestDialogBaseFragment implements
        DialogInterface.OnClickListener, WifiPickerTracker.WifiPickerTrackerCallback {

    private static final String TAG = "NetworkRequestDialogFragment";

    /**
     * Spec defines there should be 5 wifi ap on the list at most or just show all if {@code
     * mShowLimitedItem} is false.
     */
    private static final int MAX_NUMBER_LIST_ITEM = 5;
    private boolean mShowLimitedItem = true;

    private static class MatchWifi {
        String mSsid;
        List<Integer> mSecurityTypes;
    }
    private List<MatchWifi> mMatchWifis = new ArrayList<>();
    @VisibleForTesting List<WifiEntry> mFilteredWifiEntries = new ArrayList<>();
    private WifiEntryAdapter mDialogAdapter;
    private NetworkRequestUserSelectionCallback mUserSelectionCallback;

    @VisibleForTesting WifiPickerTracker mWifiPickerTracker;
    // Worker thread used for WifiPickerTracker work.
    private HandlerThread mWorkerThread;
    // Max age of tracked WifiEntries.
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating WifiPickerTracker scans.
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    public static NetworkRequestDialogFragment newInstance() {
        NetworkRequestDialogFragment dialogFragment = new NetworkRequestDialogFragment();
        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWorkerThread = new HandlerThread(
                TAG + "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        final Clock elapsedRealtimeClock = new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };
        final Context context = getContext();
        mWifiPickerTracker = FeatureFactory.getFeatureFactory()
                .getWifiTrackerLibProvider()
                .createWifiPickerTracker(getSettingsLifecycle(), context,
                        new Handler(Looper.getMainLooper()),
                        mWorkerThread.getThreadHandler(),
                        elapsedRealtimeClock,
                        MAX_SCAN_AGE_MILLIS,
                        SCAN_INTERVAL_MILLIS,
                        this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();

        // Prepares title.
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View customTitle = inflater.inflate(R.layout.network_request_dialog_title, null);

        final TextView title = customTitle.findViewById(R.id.network_request_title_text);
        title.setText(getTitle());
        final TextView summary = customTitle.findViewById(R.id.network_request_summary_text);
        summary.setText(getSummary());

        final ProgressBar progressBar = customTitle.findViewById(
                R.id.network_request_title_progress);
        progressBar.setVisibility(View.VISIBLE);

        // Prepares adapter.
        mDialogAdapter = new WifiEntryAdapter(context,
                com.android.settingslib.R.layout.preference_access_point, mFilteredWifiEntries);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setCustomTitle(customTitle)
                .setAdapter(mDialogAdapter, this)
                .setNegativeButton(R.string.cancel, (dialog, which) -> onCancel(dialog))
                // Do nothings, will replace the onClickListener to avoid auto closing dialog.
                .setNeutralButton(R.string.network_connection_request_dialog_showall,
                        null /* OnClickListener */);

        // Clicking list item is to connect wifi ap.
        final AlertDialog dialog = builder.create();
        dialog.getListView().setOnItemClickListener(
                (parent, view, position, id) -> this.onClick(dialog, position));

        // Don't dismiss dialog when touching outside. User reports it is easy to touch outside.
        // This causes dialog to close.
        setCancelable(false);

        dialog.setOnShowListener((dialogInterface) -> {
            // Replace NeutralButton onClickListener to avoid closing dialog
            final Button neutralBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutralBtn.setVisibility(View.GONE);
            neutralBtn.setOnClickListener(v -> {
                mShowLimitedItem = false;
                updateWifiEntries();
                updateUi();
                neutralBtn.setVisibility(View.GONE);
            });
        });
        return dialog;
    }

    private BaseAdapter getDialogAdapter() {
        return mDialogAdapter;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mFilteredWifiEntries.size() == 0 || which >= mFilteredWifiEntries.size()) {
            return;  // Invalid values.
        }
        if (mUserSelectionCallback == null) {
            return; // Callback is missing or not ready.
        }

        final WifiEntry wifiEntry = mFilteredWifiEntries.get(which);
        WifiConfiguration config = wifiEntry.getWifiConfiguration();
        if (config == null) {
            config = WifiUtils.getWifiConfig(wifiEntry, null /* scanResult */);
        }
        mUserSelectionCallback.select(config);
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);

        if (mUserSelectionCallback != null) {
            mUserSelectionCallback.reject();
        }
    }

    @Override
    public void onDestroy() {
        mWorkerThread.quit();

        super.onDestroy();
    }

    private void showAllButton() {
        final AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog == null) {
            return;
        }

        final Button neutralBtn = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutralBtn != null) {
            neutralBtn.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressIcon() {
        final AlertDialog alertDialog = (AlertDialog) getDialog();
        if (alertDialog == null) {
            return;
        }

        final View progress = alertDialog.findViewById(R.id.network_request_title_progress);
        if (progress != null) {
            progress.setVisibility(View.GONE);
        }
    }

    /** Called when the state of Wifi has changed. */
    @Override
    public void onWifiStateChanged() {
        if (mMatchWifis.size() == 0) {
            return;
        }
        updateWifiEntries();
        updateUi();
    }

    /**
     * Update the results when data changes
     */
    @Override
    public void onWifiEntriesChanged() {
        if (mMatchWifis.size() == 0) {
            return;
        }
        updateWifiEntries();
        updateUi();
    }

    @Override
    public void onNumSavedSubscriptionsChanged() {
        // Do nothing.
    }

    @Override
    public void onNumSavedNetworksChanged() {
        // Do nothing.
    }

    @VisibleForTesting
    void updateWifiEntries() {
        final List<WifiEntry> wifiEntries = new ArrayList<>();
        WifiEntry connectedWifiEntry = mWifiPickerTracker.getConnectedWifiEntry();
        String connectedSsid;
        if (connectedWifiEntry != null) {
            connectedSsid = connectedWifiEntry.getSsid();
            wifiEntries.add(connectedWifiEntry);
        } else {
            connectedSsid = null;
        }
        wifiEntries.addAll(mWifiPickerTracker.getWifiEntries());

        mFilteredWifiEntries.clear();
        mFilteredWifiEntries.addAll(wifiEntries.stream()
                .filter(entry -> isMatchedWifiEntry(entry, connectedSsid))
                .limit(mShowLimitedItem ? MAX_NUMBER_LIST_ITEM : Long.MAX_VALUE)
                .toList());
    }

    private boolean isMatchedWifiEntry(WifiEntry entry, String connectedSsid) {
        if (entry.getConnectedState() == WifiEntry.CONNECTED_STATE_DISCONNECTED
                && TextUtils.equals(entry.getSsid(), connectedSsid)) {
            // WifiPickerTracker may return a duplicate unsaved network that is separate from
            // the connecting app-requested network, so make sure we only show the connected
            // app-requested one.
            return false;
        }
        for (MatchWifi wifi : mMatchWifis) {
            if (!TextUtils.equals(entry.getSsid(), wifi.mSsid)) {
                continue;
            }
            for (Integer security : wifi.mSecurityTypes) {
                if (entry.getSecurityTypes().contains(security)) {
                    return true;
                }
            }
        }
        return false;
    }

    private class WifiEntryAdapter extends ArrayAdapter<WifiEntry> {

        private final int mResourceId;
        private final LayoutInflater mInflater;

        WifiEntryAdapter(Context context, int resourceId, List<WifiEntry> objects) {
            super(context, resourceId, objects);
            mResourceId = resourceId;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = mInflater.inflate(mResourceId, parent, false);

                final View divider = view.findViewById(
                        com.android.settingslib.widget.preference.twotarget.R.id.two_target_divider);
                divider.setVisibility(View.GONE);
            }

            final WifiEntry wifiEntry = getItem(position);

            final TextView titleView = view.findViewById(android.R.id.title);
            if (titleView != null) {
                // Shows whole SSID for better UX.
                titleView.setSingleLine(false);
                titleView.setText(wifiEntry.getTitle());
            }

            final TextView summary = view.findViewById(android.R.id.summary);
            if (summary != null) {
                final String summaryString = wifiEntry.getSummary();
                if (TextUtils.isEmpty(summaryString)) {
                    summary.setVisibility(View.GONE);
                } else {
                    summary.setVisibility(View.VISIBLE);
                    summary.setText(summaryString);
                }
            }

            final PreferenceImageView imageView = view.findViewById(android.R.id.icon);
            final int level = wifiEntry.getLevel();
            if (imageView != null && level != WifiEntry.WIFI_LEVEL_UNREACHABLE) {
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
    public void onUserSelectionCallbackRegistration(
            NetworkRequestUserSelectionCallback userSelectionCallback) {
        mUserSelectionCallback = userSelectionCallback;
    }

    @Override
    public void onMatch(List<ScanResult> scanResults) {
        mMatchWifis.clear();
        for (ScanResult scanResult : scanResults) {
            MatchWifi matchWifi = new MatchWifi();
            matchWifi.mSsid = scanResult.SSID;
            matchWifi.mSecurityTypes = getSecurityTypesFromScanResult(scanResult);
            mMatchWifis.add(matchWifi);
        }

        updateWifiEntries();
        updateUi();
    }

    @VisibleForTesting
    void updateUi() {
        // Update related UI buttons
        if (mShowLimitedItem && mFilteredWifiEntries.size() >= MAX_NUMBER_LIST_ITEM) {
            showAllButton();
        }
        if (mFilteredWifiEntries.size() > 0) {
            hideProgressIcon();
        }

        if (getDialogAdapter() != null) {
            getDialogAdapter().notifyDataSetChanged();
        }
    }
}

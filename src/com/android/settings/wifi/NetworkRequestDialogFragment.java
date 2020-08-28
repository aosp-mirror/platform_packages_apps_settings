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
import android.net.wifi.WifiManager.NetworkRequestMatchCallback;
import android.net.wifi.WifiManager.NetworkRequestUserSelectionCallback;
import android.os.Bundle;
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
import com.android.settingslib.Utils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fragment sets up callback {@link NetworkRequestMatchCallback} with framework. To handle most
 * behaviors of the callback when requesting wifi network, except for error message. When error
 * happens, {@link NetworkRequestErrorDialogFragment} will be called to display error message.
 */
public class NetworkRequestDialogFragment extends NetworkRequestDialogBaseFragment implements
        DialogInterface.OnClickListener{

    /**
     * Spec defines there should be 5 wifi ap on the list at most or just show all if {@code
     * mShowLimitedItem} is false.
     */
    private static final int MAX_NUMBER_LIST_ITEM = 5;
    private boolean mShowLimitedItem = true;

    private List<AccessPoint> mAccessPointList;
    @VisibleForTesting
    FilterWifiTracker mFilterWifiTracker;
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
        title.setText(getTitle());
        final TextView summary = customTitle.findViewById(R.id.network_request_summary_text);
        summary.setText(getSummary());

        final ProgressBar progressBar = customTitle.findViewById(
                R.id.network_request_title_progress);
        progressBar.setVisibility(View.VISIBLE);

        // Prepares adapter.
        mDialogAdapter = new AccessPointAdapter(context,
                R.layout.preference_access_point, getAccessPointList());

        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setCustomTitle(customTitle)
                .setAdapter(mDialogAdapter, this)
                .setNegativeButton(R.string.cancel, (dialog, which) -> onCancel(dialog))
                // Do nothings, will replace the onClickListener to avoid auto closing dialog.
                .setNeutralButton(R.string.network_connection_request_dialog_showall,
                        null /* OnClickListener */);

        // Clicking list item is to connect wifi ap.
        final AlertDialog dialog = builder.create();
        dialog.getListView()
                .setOnItemClickListener(
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
                renewAccessPointList(null /* scanResults */);
                notifyAdapterRefresh();
                neutralBtn.setVisibility(View.GONE);
            });
        });
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
            final AccessPoint selectedAccessPoint = accessPointList.get(which);
            WifiConfiguration wifiConfig = selectedAccessPoint.getConfig();
            if (wifiConfig == null) {
                wifiConfig = WifiUtils.getWifiConfig(selectedAccessPoint, /* scanResult */
                        null, /* password */ null);
            }

            if (wifiConfig != null) {
                mUserSelectionCallback.select(wifiConfig);
            }
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        if (mUserSelectionCallback != null) {
            mUserSelectionCallback.reject();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mFilterWifiTracker != null) {
            mFilterWifiTracker.onPause();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mFilterWifiTracker != null) {
            mFilterWifiTracker.onDestroy();
            mFilterWifiTracker = null;
        }
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

    @Override
    public void onResume() {
        super.onResume();

        if (mFilterWifiTracker == null) {
            mFilterWifiTracker = new FilterWifiTracker(getContext(), getSettingsLifecycle());
        }
        mFilterWifiTracker.onResume();
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
                // Shows whole SSID for better UX.
                titleView.setSingleLine(false);
                titleView.setText(accessPoint.getTitle());
            }

            final TextView summary = view.findViewById(android.R.id.summary);
            if (summary != null) {
                final String summaryString = accessPoint.getSettingsSummary();
                if (TextUtils.isEmpty(summaryString)) {
                    summary.setVisibility(View.GONE);
                } else {
                    summary.setVisibility(View.VISIBLE);
                    summary.setText(summaryString);
                }
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
    public void onUserSelectionCallbackRegistration(
            NetworkRequestUserSelectionCallback userSelectionCallback) {
        mUserSelectionCallback = userSelectionCallback;
    }

    @Override
    public void onMatch(List<ScanResult> scanResults) {
        // Shouldn't need to renew cached list, since input result is empty.
        if (scanResults != null && scanResults.size() > 0) {
            renewAccessPointList(scanResults);

            notifyAdapterRefresh();
        }
    }

    // Updates internal AccessPoint list from WifiTracker. scanResults are used to update key list
    // of AccessPoint, and could be null if there is no necessary to update key list.
    private void renewAccessPointList(List<ScanResult> scanResults) {
        if (mFilterWifiTracker == null) {
            return;
        }

        // TODO(b/119846365): Checks if we could escalate the converting effort.
        // Updates keys of scanResults into FilterWifiTracker for updating matched AccessPoints.
        if (scanResults != null) {
            mFilterWifiTracker.updateKeys(scanResults);
        }

        // Re-gets matched AccessPoints from WifiTracker.
        final List<AccessPoint> list = getAccessPointList();
        list.clear();
        list.addAll(mFilterWifiTracker.getAccessPoints());
    }

    @VisibleForTesting
    void notifyAdapterRefresh() {
        if (getDialogAdapter() != null) {
            getDialogAdapter().notifyDataSetChanged();
        }
    }

    @VisibleForTesting
    final class FilterWifiTracker {
        private final List<String> mAccessPointKeys;
        private final WifiTracker mWifiTracker;
        private final Context mContext;

        public FilterWifiTracker(Context context, Lifecycle lifecycle) {
            mWifiTracker = WifiTrackerFactory.create(context, mWifiListener,
                    lifecycle, /* includeSaved */ true, /* includeScans */ true);
            mAccessPointKeys = new ArrayList<>();
            mContext = context;
        }

        /**
         * Updates key list from input. {@code onMatch()} may be called in multi-times according
         * wifi scanning result, so needs patchwork here.
         */
        public void updateKeys(List<ScanResult> scanResults) {
            for (ScanResult scanResult : scanResults) {
                final String key = AccessPoint.getKey(mContext, scanResult);
                if (!mAccessPointKeys.contains(key)) {
                    mAccessPointKeys.add(key);
                }
            }
        }

        /**
         * Returns only AccessPoints whose key is in {@code mAccessPointKeys}.
         *
         * @return List of matched AccessPoints.
         */
        public List<AccessPoint> getAccessPoints() {
            final List<AccessPoint> allAccessPoints = mWifiTracker.getAccessPoints();
            final List<AccessPoint> result = new ArrayList<>();

            // The order should be kept, because order means wifi score (sorting in WifiTracker).
            int count = 0;
            for (AccessPoint accessPoint : allAccessPoints) {
                final String key = accessPoint.getKey();
                if (mAccessPointKeys.contains(key)) {
                    result.add(accessPoint);

                    count++;
                    // Limits how many count of items could show.
                    if (mShowLimitedItem && count >= MAX_NUMBER_LIST_ITEM) {
                        break;
                    }
                }
            }

            // Update related UI buttons
            if (mShowLimitedItem && (count >= MAX_NUMBER_LIST_ITEM)) {
                showAllButton();
            }
            if (count > 0) {
                hideProgressIcon();
            }

            return result;
        }

        @VisibleForTesting
        WifiTracker.WifiListener mWifiListener = new WifiTracker.WifiListener() {

            @Override
            public void onWifiStateChanged(int state) {
                notifyAdapterRefresh();
            }

            @Override
            public void onConnectedChanged() {
                notifyAdapterRefresh();
            }

            @Override
            public void onAccessPointsChanged() {
                renewAccessPointList(null /* scanResults */);
                notifyAdapterRefresh();
            }
        };

        public void onDestroy() {
            if (mWifiTracker != null) {
                mWifiTracker.onDestroy();
            }
        }

        public void onResume() {
            if (mWifiTracker != null) {
                mWifiTracker.onStart();
            }
        }

        public void onPause() {
            if (mWifiTracker != null) {
                mWifiTracker.onStop();
            }
        }
    }
}

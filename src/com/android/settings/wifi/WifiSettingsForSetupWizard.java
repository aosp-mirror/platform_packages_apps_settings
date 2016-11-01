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

import android.app.Dialog;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SetupWizardUtils;
import com.android.setupwizardlib.SetupWizardListLayout;
import com.android.setupwizardlib.view.NavigationBar;

/**
 * This customized version of WifiSettings is shown to the user only during Setup Wizard. Menu
 * is not shown, clicking on an access point will auto-advance to the next screen (once connected),
 * and, if the user opts to skip ahead without a wifi connection, a warning message alerts of
 * possible carrier data charges or missing software updates.
 */
public class WifiSettingsForSetupWizard extends WifiSettings {

    private static final String TAG = "WifiSettingsForSetupWizard";

    private SetupWizardListLayout mLayout;
    private View mAddOtherNetworkItem;
    private TextView mEmptyFooter;
    private View mMacAddressFooter;
    private boolean mListLastEmpty = false;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mLayout = (SetupWizardListLayout)
                inflater.inflate(R.layout.setup_wifi_layout, container, false);
        final ListView list = mLayout.getListView();

        mAddOtherNetworkItem = inflater.inflate(R.layout.setup_wifi_add_network, list, false);
        list.addFooterView(mAddOtherNetworkItem, null, true);
        mAddOtherNetworkItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWifiManager.isWifiEnabled()) {
                    onAddNetworkPressed();
                }
            }
        });

        mMacAddressFooter = inflater.inflate(R.layout.setup_wifi_mac_address, list, false);
        list.addFooterView(mMacAddressFooter, null, false);

        final NavigationBar navigationBar = mLayout.getNavigationBar();
        if (navigationBar != null) {
            WifiSetupActivity activity = (WifiSetupActivity) getActivity();
            activity.onNavigationBarCreated(navigationBar);
        }

        return mLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (hasNextButton()) {
            getNextButton().setVisibility(View.GONE);
        }

        updateMacAddress();
    }

    @Override
    public void onAccessPointsChanged() {
        super.onAccessPointsChanged();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        updateFooter(preferenceScreen == null || preferenceScreen.getPreferenceCount() == 0);
    }

    @Override
    public void onWifiStateChanged(int state) {
        super.onWifiStateChanged(state);
        updateMacAddress();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Do not show menu during setup wizard
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        final Dialog dialog = super.onCreateDialog(dialogId);
        SetupWizardUtils.applyImmersiveFlags(dialog);
        return dialog;
    }

    @Override
    protected void connect(final WifiConfiguration config, boolean isSavedNetwork) {
        WifiSetupActivity activity = (WifiSetupActivity) getActivity();
        activity.networkSelected();
        super.connect(config, isSavedNetwork);
    }

    @Override
    protected void connect(final int networkId, boolean isSavedNetwork) {
        WifiSetupActivity activity = (WifiSetupActivity) getActivity();
        activity.networkSelected();
        super.connect(networkId, isSavedNetwork);
    }

    @Override
    protected TextView initEmptyTextView() {
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        mEmptyFooter = (TextView) inflater.inflate(R.layout.setup_wifi_empty, getListView(), false);
        return mEmptyFooter;
    }

    protected void updateFooter(boolean isEmpty) {
        if (isEmpty != mListLastEmpty) {
            if (isEmpty) {
                setFooterView(mEmptyFooter);
            } else {
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.addView(mAddOtherNetworkItem);
                layout.addView(mMacAddressFooter);
                setFooterView(layout);
            }
            mListLastEmpty = isEmpty;
        }
    }

    @Override
    public View setPinnedHeaderView(int layoutResId) {
        // Pinned header is not supported in setup wizard
        return null;
    }

    @Override
    public void setPinnedHeaderView(View pinnedHeader) {
        // Pinned header is not supported in setup wizard
    }

    @Override
    protected void setProgressBarVisible(boolean visible) {
        if (mLayout != null) {
            if (visible) {
                mLayout.showProgressBar();
            } else {
                mLayout.hideProgressBar();
            }
        }
    }

    private void updateMacAddress() {
        if (mMacAddressFooter != null) {
            String macAddress = null;
            if (mWifiManager != null) {
                android.net.wifi.WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
                if (connectionInfo != null) {
                    macAddress = connectionInfo.getMacAddress();
                }
            }
            final TextView macAddressTextView =
                    (TextView) mMacAddressFooter.findViewById(R.id.mac_address);
            macAddressTextView.setText(!TextUtils.isEmpty(macAddress) ?
                    macAddress : getString(R.string.status_unavailable));
        }
    }
}

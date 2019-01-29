/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.wifi.details;

import static com.android.settings.wifi.WifiSettings.WIFI_DIALOG_ID;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.wifi.WifiConfigUiBase;
import com.android.settings.wifi.WifiDialog;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.wifi.AccessPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Detail page for the currently connected wifi network.
 *
 * <p>The AccessPoint should be saved to the intent Extras when launching this class via
 * {@link AccessPoint#saveWifiState(Bundle)} in order to properly render this page.
 */
public class WifiNetworkDetailsFragment extends DashboardFragment {

    private static final String TAG = "WifiNetworkDetailsFrg";

    public static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;

    private AccessPoint mAccessPoint;
    private WifiDetailPreferenceController mWifiDetailPreferenceController;

    @Override
    public void onAttach(Context context) {
        mAccessPoint = new AccessPoint(context, getArguments());
        super.onAttach(context);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_NETWORK_DETAILS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.wifi_network_details_fragment;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == WIFI_DIALOG_ID) {
            return SettingsEnums.DIALOG_WIFI_AP_EDIT;
        }
        return 0;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (getActivity() == null || mWifiDetailPreferenceController == null
                || mAccessPoint == null) {
            return null;
        }
        return WifiDialog.createModal(getActivity(), mWifiDetailPreferenceController, mAccessPoint,
                WifiConfigUiBase.MODE_MODIFY);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.add(0, Menu.FIRST, 0, R.string.wifi_modify);
        item.setIcon(com.android.internal.R.drawable.ic_mode_edit);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case Menu.FIRST:
                if (!mWifiDetailPreferenceController.canModifyNetwork()) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                            RestrictedLockUtilsInternal.getDeviceOwner(getContext()));
                } else {
                    showDialog(WIFI_DIALOG_ID);
                }
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        mWifiDetailPreferenceController = WifiDetailPreferenceController.newInstance(
                mAccessPoint,
                cm,
                context,
                this,
                new Handler(Looper.getMainLooper()),  // UI thread.
                getSettingsLifecycle(),
                context.getSystemService(WifiManager.class),
                mMetricsFeatureProvider);

        controllers.add(mWifiDetailPreferenceController);
        controllers.add(new WifiMeteredPreferenceController(context, mAccessPoint.getConfig()));
        WifiPrivacyPreferenceController preferenceController = new WifiPrivacyPreferenceController(
                context);
        preferenceController.setWifiConfiguration(mAccessPoint.getConfig());
        preferenceController.setIsEphemeral(mAccessPoint.isEphemeral());
        controllers.add(preferenceController);

        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS
                && resultCode == Activity.RESULT_OK) {
            mWifiDetailPreferenceController.launchWifiDppConfiguratorActivity();
        }
    }
}

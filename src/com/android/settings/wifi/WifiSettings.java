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

import static android.net.wifi.WifiConfiguration.INVALID_NETWORK_ID;

import com.android.settings.ProgressCategoryBase;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings.Secure;
import android.security.Credentials;
import android.security.KeyStore;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This currently provides three types of UI.
 *
 * Two are for phones with relatively small screens: "for SetupWizard" and "for usual Settings".
 * Users just need to launch WifiSettings Activity as usual. The request will be appropriately
 * handled by ActivityManager, and they will have appropriate look-and-feel with this fragment.
 *
 * Third type is for Setup Wizard with X-Large, landscape UI. Users need to launch
 * {@link WifiSettingsForSetupWizardXL} Activity, which contains this fragment but also has
 * other decorations specific to that screen.
 */
public class WifiSettings extends SettingsPreferenceFragment
        implements DialogInterface.OnClickListener {
    private static final int MENU_ID_SCAN = Menu.FIRST;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 1;
    private static final int MENU_ID_CONNECT = Menu.FIRST + 2;
    private static final int MENU_ID_FORGET = Menu.FIRST + 3;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 4;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;
    private final Scanner mScanner;

    private WifiManager mWifiManager;
    private WifiEnabler mWifiEnabler;
    private CheckBoxPreference mNotifyOpenNetworks;
    private ProgressCategoryBase mAccessPoints;
    private Preference mAddNetwork;
    // An access point being editted is stored here.
    private AccessPoint mSelectedAccessPoint;
    private boolean mEdit;

    private DetailedState mLastState;
    private WifiInfo mLastInfo;

    private AtomicBoolean mConnected = new AtomicBoolean(false);

    private int mKeyStoreNetworkId = INVALID_NETWORK_ID;

    private WifiDialog mDialog;

    /* Used in Wifi Setup context */

    // this boolean extra specifies whether to disable the Next button when not connected
    private static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";

    // Indicates that this fragment is used as a part of Setup Wizard with XL screen settings.
    // This fragment should show information which has been shown as Dialog in combined UI
    // inside this fragment.
    /* package */ static final String IN_XL_SETUP_WIZARD = "in_setup_wizard";

    // should Next button only be enabled when we have a connection?
    private boolean mEnableNextOnConnection;
    private boolean mInXlSetupWizard;

    /* End of "used in Wifi Setup context" */

    public WifiSettings() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(intent);
            }
        };

        mScanner = new Scanner();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // We don't call super.onActivityCreated() here, since it assumes we already set up
        // Preference (probably in onCreate()), while WifiSettings exceptionally set it up in
        // this method.

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        final Activity activity = getActivity();
        final Intent intent = activity.getIntent();


        // if we're supposed to enable/disable the Next button based on our current connection
        // state, start it off in the right state
        mEnableNextOnConnection = intent.getBooleanExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, false);
        mInXlSetupWizard = intent.getBooleanExtra(IN_XL_SETUP_WIZARD, false);

        if (mEnableNextOnConnection) {
            if (mEnableNextOnConnection && hasNextButton()) {
                final ConnectivityManager connectivity = (ConnectivityManager)
                        getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivity != null) {
                    NetworkInfo info = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    changeNextButtonState(info.isConnected());
                }
            }
        }

        if (mInXlSetupWizard) {
            addPreferencesFromResource(R.xml.wifi_access_points_for_wifi_setup_xl);
        } else if (intent.getBooleanExtra("only_access_points", false)) {
            addPreferencesFromResource(R.xml.wifi_access_points);
        } else {
            addPreferencesFromResource(R.xml.wifi_settings);
            mWifiEnabler = new WifiEnabler(activity,
                    (CheckBoxPreference) findPreference("enable_wifi"));
            mNotifyOpenNetworks =
                    (CheckBoxPreference) findPreference("notify_open_networks");
            mNotifyOpenNetworks.setChecked(Secure.getInt(getContentResolver(),
                    Secure.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0) == 1);
        }

        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(savedInstanceState);

        // This may be either ProgressCategory or AccessPointCategoryForXL.
        final ProgressCategoryBase preference =
                (ProgressCategoryBase) findPreference("access_points");
        mAccessPoints = preference;
        mAccessPoints.setOrderingAsAdded(false);
        mAddNetwork = findPreference("add_network");

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWifiEnabler != null) {
            mWifiEnabler.resume();
        }
        getActivity().registerReceiver(mReceiver, mFilter);
        if (mKeyStoreNetworkId != INVALID_NETWORK_ID &&
                KeyStore.getInstance().test() == KeyStore.NO_ERROR) {
            mWifiManager.connectNetwork(mKeyStoreNetworkId);
        }
        mKeyStoreNetworkId = INVALID_NETWORK_ID;
        updateAccessPoints();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWifiEnabler != null) {
            mWifiEnabler.pause();
        }
        getActivity().unregisterReceiver(mReceiver);
        mScanner.pause();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // We don't want menus in Setup Wizard XL.
        if (!mInXlSetupWizard) {
            menu.add(Menu.NONE, MENU_ID_SCAN, 0, R.string.wifi_menu_scan)
                    .setIcon(R.drawable.ic_menu_scan_network);
            menu.add(Menu.NONE, MENU_ID_ADVANCED, 0, R.string.wifi_menu_advanced)
                    .setIcon(android.R.drawable.ic_menu_manage);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_SCAN:
                if (mWifiManager.isWifiEnabled()) {
                    mScanner.resume();
                }
                return true;
            case MENU_ID_ADVANCED:
                startFragment(this, AdvancedSettings.class.getCanonicalName(), -1, null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
        if (info instanceof AdapterContextMenuInfo) {
            Preference preference = (Preference) getListView().getItemAtPosition(
                    ((AdapterContextMenuInfo) info).position);

            if (preference instanceof AccessPoint) {
                mSelectedAccessPoint = (AccessPoint) preference;
                menu.setHeaderTitle(mSelectedAccessPoint.ssid);
                if (mSelectedAccessPoint.getLevel() != -1
                        && mSelectedAccessPoint.getState() == null) {
                    menu.add(Menu.NONE, MENU_ID_CONNECT, 0, R.string.wifi_menu_connect);
                }
                if (mSelectedAccessPoint.networkId != INVALID_NETWORK_ID) {
                    menu.add(Menu.NONE, MENU_ID_FORGET, 0, R.string.wifi_menu_forget);
                    menu.add(Menu.NONE, MENU_ID_MODIFY, 0, R.string.wifi_menu_modify);
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mSelectedAccessPoint == null) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
            case MENU_ID_CONNECT: {
                if (mSelectedAccessPoint.networkId != INVALID_NETWORK_ID) {
                    if (!requireKeyStore(mSelectedAccessPoint.getConfig())) {
                        mWifiManager.connectNetwork(mSelectedAccessPoint.networkId);
                    }
                } else if (mSelectedAccessPoint.security == AccessPoint.SECURITY_NONE) {
                    // Shortcut for open networks.
                    WifiConfiguration config = new WifiConfiguration();
                    config.SSID = AccessPoint.convertToQuotedString(mSelectedAccessPoint.ssid);
                    config.allowedKeyManagement.set(KeyMgmt.NONE);
                    mWifiManager.connectNetwork(config);
                } else {
                    showConfigUi(mSelectedAccessPoint, true);
                }
                return true;
            }
            case MENU_ID_FORGET: {
                mWifiManager.forgetNetwork(mSelectedAccessPoint.networkId);
                return true;
            }
            case MENU_ID_MODIFY: {
                showConfigUi(mSelectedAccessPoint, true);
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference instanceof AccessPoint) {
            mSelectedAccessPoint = (AccessPoint) preference;
            showConfigUi(mSelectedAccessPoint, false);
        } else if (preference == mAddNetwork) {
            onAddNetworkPressed();
        } else if (preference == mNotifyOpenNetworks) {
            Secure.putInt(getContentResolver(),
                    Secure.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON,
                    mNotifyOpenNetworks.isChecked() ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }
        return true;
    }

    /**
     * Shows an appropriate Wifi configuration component.
     * Called when a user clicks "Add network" preference or one of available networks is selected.
     */
    private void showConfigUi(AccessPoint accessPoint, boolean edit) {
        mEdit = edit;
        if (mInXlSetupWizard) {
            ((WifiSettingsForSetupWizardXL)getActivity()).showConfigUi(accessPoint, edit);
        } else {
            showDialog(accessPoint, edit);
        }
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new WifiDialog(getActivity(), this, accessPoint, edit);
        mDialog.show();
    }

    /**
     * Called from {@link WifiSettingsForSetupWizardXL} when the object wants to open
     * {@link WifiDialog} anyway, though usually it prepares its own simplified UI for
     * configuring a wifi network.
     */
    /* package */ void showDialogForSelectedPreference() {
        showDialog(mSelectedAccessPoint, mEdit);
    }

    private boolean requireKeyStore(WifiConfiguration config) {
        if (WifiConfigController.requireKeyStore(config) &&
                KeyStore.getInstance().test() != KeyStore.NO_ERROR) {
            mKeyStoreNetworkId = config.networkId;
            Credentials.getInstance().unlock(getActivity());
            return true;
        }
        return false;
    }

    /**
     * Shows the latest access points available with supplimental information like
     * the strength of network and the security for it.
     */
    private void updateAccessPoints() {
        mAccessPoints.removeAll();

        // AccessPoints are automatically sorted with TreeSet.
        final Collection<AccessPoint> accessPoints = constructAccessPoints();
        if (mInXlSetupWizard) {
            ((WifiSettingsForSetupWizardXL)getActivity()).onAccessPointsUpdated(
                    mAccessPoints, accessPoints);
        } else {
            for (AccessPoint accessPoint : accessPoints) {
                mAccessPoints.addPreference(accessPoint);
            }
        }
    }

    private Collection<AccessPoint> constructAccessPoints() {
        Collection<AccessPoint> accessPoints = new ArrayList<AccessPoint>();

        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                AccessPoint accessPoint = new AccessPoint(getActivity(), config);
                accessPoint.update(mLastInfo, mLastState);
                accessPoints.add(accessPoint);
            }
        }

        final List<ScanResult> results = mWifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0 ||
                        result.capabilities.contains("[IBSS]")) {
                    continue;
                }

                boolean found = false;
                for (AccessPoint accessPoint : accessPoints) {
                    if (accessPoint.update(result)) {
                        found = true;
                    }
                }
                if (!found) {
                    accessPoints.add(new AccessPoint(getActivity(), result));
                }
            }
        }

        return accessPoints;
    }

    private void handleEvent(Intent intent) {
        String action = intent.getAction();
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN));
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) ||
                WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION.equals(action) ||
                WifiManager.LINK_CONFIGURATION_CHANGED_ACTION.equals(action)) {
                updateAccessPoints();
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            //Ignore supplicant state changes when network is connected
            //TODO: we should deprecate SUPPLICANT_STATE_CHANGED_ACTION and
            //introduce a broadcast that combines the supplicant and network
            //network state change events so the apps dont have to worry about
            //ignoring supplicant state change when network is connected
            //to get more fine grained information.
            if (!mConnected.get()) {
                updateConnectionState(WifiInfo.getDetailedStateOf((SupplicantState)
                        intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
            }
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
            mConnected.set(info.isConnected());
            changeNextButtonState(info.isConnected());
            updateConnectionState(info.getDetailedState());
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            updateConnectionState(null);
        }
    }

    private void updateConnectionState(DetailedState state) {
        /* sticky broadcasts can call this when wifi is disabled */
        if (!mWifiManager.isWifiEnabled()) {
            mScanner.pause();
            return;
        }

        if (state == DetailedState.OBTAINING_IPADDR) {
            mScanner.pause();
        } else {
            mScanner.resume();
        }

        mLastInfo = mWifiManager.getConnectionInfo();
        if (state != null) {
            mLastState = state;
        }

        for (int i = mAccessPoints.getPreferenceCount() - 1; i >= 0; --i) {
            // Maybe there's a WifiConfigPreference
            Preference preference = mAccessPoints.getPreference(i);
            if (preference instanceof AccessPoint) {
                final AccessPoint accessPoint = (AccessPoint) preference;
                accessPoint.update(mLastInfo, mLastState);
            }
        }

        if (mInXlSetupWizard) {
            ((WifiSettingsForSetupWizardXL)getActivity()).updateConnectionState(mLastState);
        }
    }

    private void updateWifiState(int state) {
        if (state == WifiManager.WIFI_STATE_ENABLED) {
            mScanner.resume();
        } else {
            mScanner.pause();
            mAccessPoints.removeAll();
        }
    }

    private class Scanner extends Handler {
        private int mRetry = 0;

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void pause() {
            mRetry = 0;
            mAccessPoints.setProgress(false);
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message message) {
            if (mWifiManager.startScanActive()) {
                mRetry = 0;
            } else if (++mRetry >= 3) {
                mRetry = 0;
                Toast.makeText(getActivity(), R.string.wifi_fail_to_scan,
                        Toast.LENGTH_LONG).show();
                return;
            }
            mAccessPoints.setProgress(mRetry != 0);
            // Combo scans can take 5-6s to complete. Increase interval to 10s.
            sendEmptyMessageDelayed(0, 10000);
        }
    }

    /**
     * Renames/replaces "Next" button when appropriate. "Next" button usually exists in
     * Wifi setup screens, not in usual wifi settings screen.
     *
     * @param connected true when the device is connected to a wifi network.
     */
    private void changeNextButtonState(boolean connected) {
        if (mInXlSetupWizard) {
            ((WifiSettingsForSetupWizardXL)getActivity()).changeNextButtonState(connected);
        } else if (mEnableNextOnConnection && hasNextButton()) {
            getNextButton().setEnabled(connected);
        }
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (mInXlSetupWizard) {
            if (button == WifiDialog.BUTTON_FORGET && mSelectedAccessPoint != null) {
                ((WifiSettingsForSetupWizardXL)getActivity()).onForgetButtonPressed();
            } else if (button == WifiDialog.BUTTON_SUBMIT) {
                ((WifiSettingsForSetupWizardXL)getActivity()).onConnectButtonPressed();
            }
        } else {
            if (button == WifiDialog.BUTTON_FORGET && mSelectedAccessPoint != null) {
                forget();
            } else if (button == WifiDialog.BUTTON_SUBMIT) {
                submit(mDialog.getController());
            }
        }

    }

    /* package */ void submit(WifiConfigController configController) {
        switch(configController.chosenNetworkSetupMethod()) {
            case WifiConfigController.WPS_PBC:
                mWifiManager.startWpsPbc(mSelectedAccessPoint.bssid);
                break;
            case WifiConfigController.WPS_PIN_FROM_ACCESS_POINT:
                int apPin = configController.getWpsPin();
                mWifiManager.startWpsWithPinFromAccessPoint(mSelectedAccessPoint.bssid, apPin);
                break;
            case WifiConfigController.WPS_PIN_FROM_DEVICE:
                int pin = mWifiManager.startWpsWithPinFromDevice(mSelectedAccessPoint.bssid);
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.wifi_wps_pin_method_configuration)
                        .setMessage(getResources().getString(R.string.wifi_wps_pin_output, pin))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                break;
            case WifiConfigController.MANUAL:
                final WifiConfiguration config = configController.getConfig();

                if (config == null) {
                    if (mSelectedAccessPoint != null
                            && !requireKeyStore(mSelectedAccessPoint.getConfig())
                            && mSelectedAccessPoint.networkId != INVALID_NETWORK_ID) {
                        mWifiManager.connectNetwork(mSelectedAccessPoint.networkId);
                    }
                } else if (config.networkId != INVALID_NETWORK_ID) {
                    if (mSelectedAccessPoint != null) {
                        mWifiManager.saveNetwork(config);
                    }
                } else {
                    if (configController.isEdit() || requireKeyStore(config)) {
                        mWifiManager.saveNetwork(config);
                    } else {
                        mWifiManager.connectNetwork(config);
                    }
                }
                break;
        }

        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();
    }

    /* package */ void forget() {
        mWifiManager.forgetNetwork(mSelectedAccessPoint.networkId);

        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();

        // We need to rename/replace "Next" button in wifi setup context.
        changeNextButtonState(false);
    }

    /**
     * Refreshes acccess points and ask Wifi module to scan networks again.
     */
    /* package */ void refreshAccessPoints() {
        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }

        mAccessPoints.removeAll();
    }

    /**
     * Called when "add network" button is pressed.
     */
    /* package */ void onAddNetworkPressed() {
        // No exact access point is selected.
        mSelectedAccessPoint = null;
        showConfigUi(null, true);
    }

    /* package */ int getAccessPointsCount() {
        if (mAccessPoints != null) {
            return mAccessPoints.getPreferenceCount();
        } else {
            return 0;
        }
    }

    /**
     * Requests wifi module to pause wifi scan. May be ignored when the module is disabled.
     */
    /* package */ void pauseWifiScan() {
        if (mWifiManager.isWifiEnabled()) {
            mScanner.pause();
        }
    }

    /**
     * Requests wifi module to resume wifi scan. May be ignored when the module is disabled.
     */
    /* package */ void resumeWifiScan() {
        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
    }
}

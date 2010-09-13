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

import com.android.settings.ProgressCategoryBase;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.Activity;
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
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.Toast;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

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

    // Indicates that this fragment is used as a part of Setup Wizard with XL screen settings.
    // This fragment should show information which has been shown as Dialog in combined UI
    // inside this fragment.
    /* package */ static final String IN_XL_SETUP_WIZARD = "in_setup_wizard";

    // this boolean extra specifies whether to disable the Next button when not connected
    // Note: this is only effective in Setup Wizard with XL screen size.
    private static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";

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

    private DetailedState mLastState;
    private WifiInfo mLastInfo;

    private int mKeyStoreNetworkId = -1;

    // should Next button only be enabled when we have a connection?
    private boolean mEnableNextOnConnection;
    private boolean mInXlSetupWizard;


    // TODO: merge into one
    private WifiConfigPreference mConfigPreference;
    private WifiDialog mDialog;

    private boolean mRefrainListUpdate;

    public WifiSettings() {
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_CONFIG_CHANGED_ACTION);
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

        mInXlSetupWizard = intent.getBooleanExtra(IN_XL_SETUP_WIZARD, false);

        // if we're supposed to enable/disable the Next button based on our current connection
        // state, start it off in the right state
        mEnableNextOnConnection = intent.getBooleanExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, false);

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
        mAccessPoints.setOrderingAsAdded(true);
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
        if (mKeyStoreNetworkId != -1 && KeyStore.getInstance().test() == KeyStore.NO_ERROR) {
            mWifiManager.connectNetwork(mKeyStoreNetworkId);
        }
        mKeyStoreNetworkId = -1;
        if (mInXlSetupWizard) {
            // We show "Now scanning"
            final int wifiState = mWifiManager.getWifiState();
            switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED: {
                updateAccessPoints();
                break;
            }
            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
            case WifiManager.WIFI_STATE_UNKNOWN: {
                mWifiManager.setWifiEnabled(true);
            } // $FALL-THROUGH$
            default: {
                mAccessPoints.removeAll();
                Preference preference = new Preference(getActivity());
                preference.setLayoutResource(R.layout.preference_widget_shortcut);
                preference.setSelectable(false);
                preference.setTitle("Connecting");
                preference.setSummary("COONNECTING");
                mAccessPoints.addPreference(preference);
                break;
            }
            }
        } else {
            updateAccessPoints();
        }
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
                startActivity(new Intent(getActivity(), AdvancedSettings.class));
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
                if (mSelectedAccessPoint.networkId != -1) {
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
                if (mSelectedAccessPoint.networkId != -1) {
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
     * Called when a user clicks "Add network" preference or relevant button.
     */
    private void showConfigUi(AccessPoint accessPoint, boolean edit) {
        synchronized (this) {
            mRefrainListUpdate = false;
        }
        if (mInXlSetupWizard) {
            final Activity activity = getActivity();
            activity.findViewById(R.id.wifi_setup_connect).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.wifi_setup_cancel).setVisibility(View.VISIBLE);
            showConfigPreference(accessPoint, edit);
        } else {
            showDialog(accessPoint, edit);
        }
    }

    private void showConfigPreference(AccessPoint accessPoint, boolean edit) {
        // We don't want to show more than one WifiConfigPreference
        if (mConfigPreference != null) {
            mAccessPoints.removePreference(mConfigPreference);
        }

        mConfigPreference = new WifiConfigPreference(this, this, accessPoint, edit);
        toggleButtonsVisibility(false);

        updateAccessPoints();
        mScanner.pause();
    }

    private void toggleButtonsVisibility(boolean firstLayout) {
        final Activity activity = getActivity();
        if (firstLayout) {
            activity.findViewById(R.id.wifi_setup_add_network).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.wifi_setup_refresh_list).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.wifi_setup_skip_or_next).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.wifi_setup_connect).setVisibility(View.GONE);
            activity.findViewById(R.id.wifi_setup_forget).setVisibility(View.GONE);
            activity.findViewById(R.id.wifi_setup_cancel).setVisibility(View.GONE);
        } else {
            activity.findViewById(R.id.wifi_setup_add_network).setVisibility(View.GONE);
            activity.findViewById(R.id.wifi_setup_refresh_list).setVisibility(View.GONE);
            activity.findViewById(R.id.wifi_setup_skip_or_next).setVisibility(View.GONE);

            // made visible from controller.
        }
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = new WifiDialog(getActivity(), this, accessPoint, edit);
        mDialog.show();
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
        synchronized (this) {
            if (mRefrainListUpdate) {
                return;
            }
        }

        if (mConfigPreference != null) {
            mAccessPoints.removeAll();
            final AccessPoint parent = mConfigPreference.getAccessPoint();
            if (parent != null) {
                parent.setSelectable(false);
                mAccessPoints.addPreference(parent);
            }
            mAccessPoints.addPreference(mConfigPreference);
        } else {
            // AccessPoints are automatically sorted with TreeSet.
            final Collection<AccessPoint> accessPoints = constructAccessPoints();
            mAccessPoints.removeAll();
            for (AccessPoint accessPoint : accessPoints) {
                mAccessPoints.addPreference(accessPoint);
            }
        }
    }

    private Collection<AccessPoint> constructAccessPoints() {
        Collection<AccessPoint> accessPoints =
                new TreeSet<AccessPoint>(new AccessPoint.Comparater());

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
                WifiManager.SUPPLICANT_CONFIG_CHANGED_ACTION.equals(action)) {
                updateAccessPoints();
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            updateConnectionState(WifiInfo.getDetailedStateOf((SupplicantState)
                    intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
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
                ((AccessPoint) preference).update(mLastInfo, mLastState);
            }
        }

        final Activity activity = getActivity();
        if (activity instanceof WifiSettingsForSetupWizardXL) {
            ((WifiSettingsForSetupWizardXL)activity).updateConnectionState(mLastState);
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
            synchronized (WifiSettings.this) {
                mRefrainListUpdate = false;
            }
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void pause() {
            mRetry = 0;
            mAccessPoints.setProgress(false);
            synchronized (WifiSettings.this) {
                mRefrainListUpdate = true;
            }
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

    private void changeNextButtonState(boolean wifiAvailable) {
        if (mInXlSetupWizard) {
            final Button button =
                    (Button)getActivity().findViewById(R.id.wifi_setup_skip_or_next);
            button.setEnabled(true);
            if (wifiAvailable) {
                button.setText(R.string.wifi_setup_next);
            } else {
                button.setText(R.string.wifi_setup_skip);
            }
        } else if (mEnableNextOnConnection && hasNextButton()) {
            // Assumes layout for phones has next button inside it.
            getNextButton().setEnabled(wifiAvailable);
        }
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == WifiDialog.BUTTON_FORGET && mSelectedAccessPoint != null) {
            forget();
        } else if (button == WifiDialog.BUTTON_SUBMIT) {
            submit();
        }
    }

    /* package */ void submit() {
        final WifiConfigUiBase uiBase = (mDialog != null ? mDialog : mConfigPreference);
        final WifiConfiguration config = uiBase.getController().getConfig();

        if (config == null) {
            if (mSelectedAccessPoint != null
                    && !requireKeyStore(mSelectedAccessPoint.getConfig())) {
                mWifiManager.connectNetwork(mSelectedAccessPoint.networkId);
            }
        } else if (config.networkId != -1) {
            if (mSelectedAccessPoint != null) {
                mWifiManager.saveNetwork(config);
            }
        } else {
            if (uiBase.isEdit() || requireKeyStore(config)) {
                mWifiManager.saveNetwork(config);
            } else {
                mWifiManager.connectNetwork(config);
            }
        }

        detachConfigPreference();
    }

    /* package */ void forget() {
        mWifiManager.forgetNetwork(mSelectedAccessPoint.networkId);

        detachConfigPreference();

        changeNextButtonState(false);

        final Activity activity = getActivity();
        if (activity instanceof WifiSettingsForSetupWizardXL) {
            ((WifiSettingsForSetupWizardXL)activity).onForget();
        }
    }

    /* package */ void refreshAccessPoints() {
        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }

        mConfigPreference = null;
        mAccessPoints.removeAll();

        final Activity activity = getActivity();
        if (activity instanceof WifiSettingsForSetupWizardXL) {
            ((WifiSettingsForSetupWizardXL)activity).onRefreshAccessPoints();
        }
    }

    /* package */ void detachConfigPreference() {
        if (mConfigPreference != null) {
            if (mWifiManager.isWifiEnabled()) {
                mScanner.resume();
            }
            mAccessPoints.removePreference(mConfigPreference);
            mConfigPreference = null;
            updateAccessPoints();
            toggleButtonsVisibility(true);
        }
    }

    /* package */ void onAddNetworkPressed() {
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
}

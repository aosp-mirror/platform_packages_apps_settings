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
import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Two types of UI are provided here.
 *
 * The first is for "usual Settings", appearing as any other Setup fragment.
 *
 * The second is for Setup Wizard, with a simplified interface that hides the action bar
 * and menus.
 */
public class WifiSettings extends RestrictedSettingsFragment
        implements DialogInterface.OnClickListener, Indexable  {

    private static final String TAG = "WifiSettings";

    /* package */ static final int MENU_ID_WPS_PBC = Menu.FIRST;
    private static final int MENU_ID_WPS_PIN = Menu.FIRST + 1;
    private static final int MENU_ID_SAVED_NETWORK = Menu.FIRST + 2;
    /* package */ static final int MENU_ID_ADD_NETWORK = Menu.FIRST + 3;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 4;
    private static final int MENU_ID_SCAN = Menu.FIRST + 5;
    private static final int MENU_ID_CONNECT = Menu.FIRST + 6;
    private static final int MENU_ID_FORGET = Menu.FIRST + 7;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 8;
    private static final int MENU_ID_WRITE_NFC = Menu.FIRST + 9;

    public static final int WIFI_DIALOG_ID = 1;
    /* package */ static final int WPS_PBC_DIALOG_ID = 2;
    private static final int WPS_PIN_DIALOG_ID = 3;
    private static final int WRITE_NFC_DIALOG_ID = 6;

    // Combo scans can take 5-6s to complete - set to 10s.
    private static final int WIFI_RESCAN_INTERVAL_MS = 10 * 1000;

    // Instance state keys
    private static final String SAVE_DIALOG_EDIT_MODE = "edit_mode";
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";

    private static boolean savedNetworksExist;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;
    private final Scanner mScanner;

    /* package */ WifiManager mWifiManager;
    private WifiManager.ActionListener mConnectListener;
    private WifiManager.ActionListener mSaveListener;
    private WifiManager.ActionListener mForgetListener;

    private WifiEnabler mWifiEnabler;
    // An access point being editted is stored here.
    private AccessPoint mSelectedAccessPoint;

    private NetworkInfo mLastNetworkInfo;
    private WifiInfo mLastInfo;

    private final AtomicBoolean mConnected = new AtomicBoolean(false);

    private WifiDialog mDialog;
    private WriteWifiConfigToNfcDialog mWifiToNfcDialog;

    private TextView mEmptyView;

    // this boolean extra specifies whether to disable the Next button when not connected. Used by
    // account creation outside of setup wizard.
    private static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";
    // This string extra specifies a network to open the connect dialog on, so the user can enter
    // network credentials.  This is used by quick settings for secured networks.
    private static final String EXTRA_START_CONNECT_SSID = "wifi_start_connect_ssid";

    // should Next button only be enabled when we have a connection?
    private boolean mEnableNextOnConnection;

    // Save the dialog details
    private boolean mDlgEdit;
    private AccessPoint mDlgAccessPoint;
    private Bundle mAccessPointSavedState;

    /** verbose logging flag. this flag is set thru developer debugging options
     * and used so as to assist with in-the-field WiFi connectivity debugging  */
    public static int mVerboseLogging = 0;

    /* End of "used in Wifi Setup context" */

    /** A restricted multimap for use in constructAccessPoints */
    private static class Multimap<K,V> {
        private final HashMap<K,List<V>> store = new HashMap<K,List<V>>();
        /** retrieve a non-null list of values with key K */
        List<V> getAll(K key) {
            List<V> values = store.get(key);
            return values != null ? values : Collections.<V>emptyList();
        }

        void put(K key, V val) {
            List<V> curVals = store.get(key);
            if (curVals == null) {
                curVals = new ArrayList<V>(3);
                store.put(key, curVals);
            }
            curVals.add(val);
        }
    }

    private static class Scanner extends Handler {
        private int mRetry = 0;
        private WifiSettings mWifiSettings = null;

        Scanner(WifiSettings wifiSettings) {
            mWifiSettings = wifiSettings;
        }

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void pause() {
            mRetry = 0;
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message message) {
            if (mWifiSettings.mWifiManager.startScan()) {
                mRetry = 0;
            } else if (++mRetry >= 3) {
                mRetry = 0;
                Activity activity = mWifiSettings.getActivity();
                if (activity != null) {
                    Toast.makeText(activity, R.string.wifi_fail_to_scan, Toast.LENGTH_LONG).show();
                }
                return;
            }
            sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
        }
    }

    public WifiSettings() {
        super(DISALLOW_CONFIG_WIFI);
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

        mScanner = new Scanner(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mConnectListener = new WifiManager.ActionListener() {
                                   @Override
                                   public void onSuccess() {
                                   }
                                   @Override
                                   public void onFailure(int reason) {
                                       Activity activity = getActivity();
                                       if (activity != null) {
                                           Toast.makeText(activity,
                                                R.string.wifi_failed_connect_message,
                                                Toast.LENGTH_SHORT).show();
                                       }
                                   }
                               };

        mSaveListener = new WifiManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                }
                                @Override
                                public void onFailure(int reason) {
                                    Activity activity = getActivity();
                                    if (activity != null) {
                                        Toast.makeText(activity,
                                            R.string.wifi_failed_save_message,
                                            Toast.LENGTH_SHORT).show();
                                    }
                                }
                            };

        mForgetListener = new WifiManager.ActionListener() {
                                   @Override
                                   public void onSuccess() {
                                   }
                                   @Override
                                   public void onFailure(int reason) {
                                       Activity activity = getActivity();
                                       if (activity != null) {
                                           Toast.makeText(activity,
                                               R.string.wifi_failed_forget_message,
                                               Toast.LENGTH_SHORT).show();
                                       }
                                   }
                               };

        if (savedInstanceState != null) {
            mDlgEdit = savedInstanceState.getBoolean(SAVE_DIALOG_EDIT_MODE);
            if (savedInstanceState.containsKey(SAVE_DIALOG_ACCESS_POINT_STATE)) {
                mAccessPointSavedState =
                    savedInstanceState.getBundle(SAVE_DIALOG_ACCESS_POINT_STATE);
            }
        }

        // if we're supposed to enable/disable the Next button based on our current connection
        // state, start it off in the right state
        Intent intent = getActivity().getIntent();
        mEnableNextOnConnection = intent.getBooleanExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, false);

        if (mEnableNextOnConnection) {
            if (hasNextButton()) {
                final ConnectivityManager connectivity = (ConnectivityManager)
                        getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivity != null) {
                    NetworkInfo info = connectivity.getNetworkInfo(
                            ConnectivityManager.TYPE_WIFI);
                    changeNextButtonState(info.isConnected());
                }
            }
        }

        addPreferencesFromResource(R.xml.wifi_settings);

        mEmptyView = initEmptyView();
        registerForContextMenu(getListView());
        setHasOptionsMenu(true);

        if (intent.hasExtra(EXTRA_START_CONNECT_SSID)) {
            String ssid = intent.getStringExtra(EXTRA_START_CONNECT_SSID);
            updateAccessPoints();
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
                Preference preference = preferenceScreen.getPreference(i);
                if (preference instanceof AccessPoint) {
                    AccessPoint accessPoint = (AccessPoint) preference;
                    if (ssid.equals(accessPoint.ssid) && accessPoint.networkId == -1
                            && accessPoint.security != AccessPoint.SECURITY_NONE) {
                        onPreferenceTreeClick(preferenceScreen, preference);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mWifiEnabler != null) {
            mWifiEnabler.teardownSwitchBar();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // On/off switch is hidden for Setup Wizard (returns null)
        mWifiEnabler = createWifiEnabler();
    }

    /**
     * @return new WifiEnabler or null (as overridden by WifiSettingsForSetupWizard)
     */
    /* package */ WifiEnabler createWifiEnabler() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        return new WifiEnabler(activity, activity.getSwitchBar());
    }

    @Override
    public void onResume() {
        final Activity activity = getActivity();
        super.onResume();
        if (mWifiEnabler != null) {
            mWifiEnabler.resume(activity);
        }

        activity.registerReceiver(mReceiver, mFilter);
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the user is not allowed to configure wifi, do not show the menu.
        if (isUiRestricted()) return;

        addOptionsMenuItems(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * @param menu
     */
    void addOptionsMenuItems(Menu menu) {
        final boolean wifiIsEnabled = mWifiManager.isWifiEnabled();
        TypedArray ta = getActivity().getTheme().obtainStyledAttributes(
                new int[] {R.attr.ic_menu_add, R.attr.ic_wps});
        menu.add(Menu.NONE, MENU_ID_ADD_NETWORK, 0, R.string.wifi_add_network)
                .setIcon(ta.getDrawable(0))
                .setEnabled(wifiIsEnabled)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        if (savedNetworksExist) {
            menu.add(Menu.NONE, MENU_ID_SAVED_NETWORK, 0, R.string.wifi_saved_access_points_label)
                    .setIcon(ta.getDrawable(0))
                    .setEnabled(wifiIsEnabled)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        menu.add(Menu.NONE, MENU_ID_SCAN, 0, R.string.menu_stats_refresh)
               .setEnabled(wifiIsEnabled)
               .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_ADVANCED, 0, R.string.wifi_menu_advanced)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        ta.recycle();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the dialog is showing, save its state.
        if (mDialog != null && mDialog.isShowing()) {
            outState.putBoolean(SAVE_DIALOG_EDIT_MODE, mDlgEdit);
            if (mDlgAccessPoint != null) {
                mAccessPointSavedState = new Bundle();
                mDlgAccessPoint.saveWifiState(mAccessPointSavedState);
                outState.putBundle(SAVE_DIALOG_ACCESS_POINT_STATE, mAccessPointSavedState);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If the user is not allowed to configure wifi, do not handle menu selections.
        if (isUiRestricted()) return false;

        switch (item.getItemId()) {
            case MENU_ID_WPS_PBC:
                showDialog(WPS_PBC_DIALOG_ID);
                return true;
                /*
            case MENU_ID_P2P:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            WifiP2pSettings.class.getCanonicalName(),
                            null,
                            R.string.wifi_p2p_settings_title, null,
                            this, 0);
                } else {
                    startFragment(this, WifiP2pSettings.class.getCanonicalName(),
                            R.string.wifi_p2p_settings_title, -1, null);
                }
                return true;
                */
            case MENU_ID_WPS_PIN:
                showDialog(WPS_PIN_DIALOG_ID);
                return true;
            case MENU_ID_SCAN:
                if (mWifiManager.isWifiEnabled()) {
                    mScanner.forceScan();
                }
                return true;
            case MENU_ID_ADD_NETWORK:
                if (mWifiManager.isWifiEnabled()) {
                    onAddNetworkPressed();
                }
                return true;
            case MENU_ID_SAVED_NETWORK:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            SavedAccessPointsWifiSettings.class.getCanonicalName(), null,
                            R.string.wifi_saved_access_points_titlebar, null, this, 0);
                } else {
                    startFragment(this, SavedAccessPointsWifiSettings.class.getCanonicalName(),
                            R.string.wifi_saved_access_points_titlebar,
                            -1 /* Do not request a result */, null);
                }
                return true;
            case MENU_ID_ADVANCED:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            AdvancedWifiSettings.class.getCanonicalName(), null,
                            R.string.wifi_advanced_titlebar, null, this, 0);
                } else {
                    startFragment(this, AdvancedWifiSettings.class.getCanonicalName(),
                            R.string.wifi_advanced_titlebar, -1 /* Do not request a results */,
                            null);
                }
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
                if (mSelectedAccessPoint.getLevel() != -1) {
                    if (mSelectedAccessPoint.getState() == null) {
                        menu.add(Menu.NONE, MENU_ID_CONNECT, 0, R.string.wifi_menu_connect);
                    }
                }

                if (ActivityManager.getCurrentUser() == UserHandle.USER_OWNER &&
                        (mSelectedAccessPoint.networkId != INVALID_NETWORK_ID ||
                        (mSelectedAccessPoint.getNetworkInfo() != null &&
                        mSelectedAccessPoint.getNetworkInfo().getState() != State.DISCONNECTED))) {
                    // Allow forgetting a network if the current user is the owner and either the
                    // network is saved or ephemerally connected. (In the latter case, "forget"
                    // blacklists the network so it won't be used again, ephemerally).
                    menu.add(Menu.NONE, MENU_ID_FORGET, 0, R.string.wifi_menu_forget);
                }
                if (mSelectedAccessPoint.networkId != INVALID_NETWORK_ID) {
                    menu.add(Menu.NONE, MENU_ID_MODIFY, 0, R.string.wifi_menu_modify);
                    NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
                    if (nfcAdapter != null && nfcAdapter.isEnabled() &&
                            mSelectedAccessPoint.security != AccessPoint.SECURITY_NONE) {
                        // Only allow writing of NFC tags for password-protected networks.
                        menu.add(Menu.NONE, MENU_ID_WRITE_NFC, 0, R.string.wifi_menu_write_to_nfc);
                    }
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
                    connect(mSelectedAccessPoint.networkId);
                } else if (mSelectedAccessPoint.security == AccessPoint.SECURITY_NONE) {
                    /** Bypass dialog for unsecured networks */
                    mSelectedAccessPoint.generateOpenNetworkConfig();
                    connect(mSelectedAccessPoint.getConfig());
                } else {
                    showDialog(mSelectedAccessPoint, true);
                }
                return true;
            }
            case MENU_ID_FORGET: {
                forget();
                return true;
            }
            case MENU_ID_MODIFY: {
                showDialog(mSelectedAccessPoint, true);
                return true;
            }
            case MENU_ID_WRITE_NFC:
                showDialog(WRITE_NFC_DIALOG_ID);
                return true;

        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        if (preference instanceof AccessPoint) {
            mSelectedAccessPoint = (AccessPoint) preference;
            /** Bypass dialog for unsecured, unsaved, and inactive networks */
            if (mSelectedAccessPoint.security == AccessPoint.SECURITY_NONE &&
                    mSelectedAccessPoint.networkId == INVALID_NETWORK_ID &&
                    !mSelectedAccessPoint.isActive()) {
                mSelectedAccessPoint.generateOpenNetworkConfig();
                if (!savedNetworksExist) {
                    savedNetworksExist = true;
                    getActivity().invalidateOptionsMenu();
                }
                connect(mSelectedAccessPoint.getConfig());
            } else {
                showDialog(mSelectedAccessPoint, false);
            }
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }
        return true;
    }

    private void showDialog(AccessPoint accessPoint, boolean edit) {
        if (mDialog != null) {
            removeDialog(WIFI_DIALOG_ID);
            mDialog = null;
        }

        // Save the access point and edit mode
        mDlgAccessPoint = accessPoint;
        mDlgEdit = edit;

        showDialog(WIFI_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case WIFI_DIALOG_ID:
                AccessPoint ap = mDlgAccessPoint; // For manual launch
                if (ap == null) { // For re-launch from saved state
                    if (mAccessPointSavedState != null) {
                        ap = new AccessPoint(getActivity(), mAccessPointSavedState);
                        // For repeated orientation changes
                        mDlgAccessPoint = ap;
                        // Reset the saved access point data
                        mAccessPointSavedState = null;
                    }
                }
                // If it's null, fine, it's for Add Network
                mSelectedAccessPoint = ap;
                mDialog = new WifiDialog(getActivity(), this, ap, mDlgEdit);
                return mDialog;
            case WPS_PBC_DIALOG_ID:
                return new WpsDialog(getActivity(), WpsInfo.PBC);
            case WPS_PIN_DIALOG_ID:
                return new WpsDialog(getActivity(), WpsInfo.DISPLAY);
            case WRITE_NFC_DIALOG_ID:
                if (mSelectedAccessPoint != null) {
                    mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(
                            getActivity(), mSelectedAccessPoint, mWifiManager);
                    return mWifiToNfcDialog;
                }

        }
        return super.onCreateDialog(dialogId);
    }

    /**
     * Shows the latest access points available with supplemental information like
     * the strength of network and the security for it.
     */
    private void updateAccessPoints() {
        // Safeguard from some delayed event handling
        if (getActivity() == null) return;

        if (isUiRestricted()) {
            addMessagePreference(R.string.wifi_empty_list_user_restricted);
            return;
        }
        final int wifiState = mWifiManager.getWifiState();

        //when we update the screen, check if verbose logging has been turned on or off
        mVerboseLogging = mWifiManager.getVerboseLoggingLevel();

        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                // AccessPoints are automatically sorted with TreeSet.
                final Collection<AccessPoint> accessPoints =
                        constructAccessPoints(getActivity(), mWifiManager, mLastInfo,
                                mLastNetworkInfo);
                getPreferenceScreen().removeAll();
                if (accessPoints.size() == 0) {
                    addMessagePreference(R.string.wifi_empty_list_wifi_on);
                }

                for (AccessPoint accessPoint : accessPoints) {
                    // Ignore access points that are out of range.
                    if (accessPoint.getLevel() != -1) {
                        getPreferenceScreen().addPreference(accessPoint);
                    }
                }
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                getPreferenceScreen().removeAll();
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                addMessagePreference(R.string.wifi_stopping);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                break;
        }
    }

    protected TextView initEmptyView() {
        TextView emptyView = (TextView) getActivity().findViewById(android.R.id.empty);
        getListView().setEmptyView(emptyView);
        return emptyView;
    }

    private void setOffMessage() {
        if (mEmptyView != null) {
            mEmptyView.setText(R.string.wifi_empty_list_wifi_off);
            if (android.provider.Settings.Global.getInt(getActivity().getContentResolver(),
                    android.provider.Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1) {
                mEmptyView.append("\n\n");
                int resId;
                if (android.provider.Settings.Secure.isLocationProviderEnabled(
                        getActivity().getContentResolver(), LocationManager.NETWORK_PROVIDER)) {
                    resId = R.string.wifi_scan_notify_text_location_on;
                } else {
                    resId = R.string.wifi_scan_notify_text_location_off;
                }
                CharSequence charSeq = getText(resId);
                mEmptyView.append(charSeq);
            }
        }
        getPreferenceScreen().removeAll();
    }

    private void addMessagePreference(int messageId) {
        if (mEmptyView != null) mEmptyView.setText(messageId);
        getPreferenceScreen().removeAll();
    }

    /** Returns sorted list of access points */
    private static List<AccessPoint> constructAccessPoints(Context context,
            WifiManager wifiManager, WifiInfo lastInfo, NetworkInfo lastNetworkInfo) {
        ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
        /** Lookup table to more quickly update AccessPoints by only considering objects with the
         * correct SSID.  Maps SSID -> List of AccessPoints with the given SSID.  */
        Multimap<String, AccessPoint> apMap = new Multimap<String, AccessPoint>();

        final List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        if (configs != null) {
            // Update "Saved Networks" menu option.
            if (savedNetworksExist != (configs.size() > 0)) {
                savedNetworksExist = !savedNetworksExist;
                if (context instanceof Activity) {
                    ((Activity) context).invalidateOptionsMenu();
                }
            }
            for (WifiConfiguration config : configs) {
                if (config.selfAdded && config.numAssociation == 0) {
                    continue;
                }
                AccessPoint accessPoint = new AccessPoint(context, config);
                if (lastInfo != null && lastNetworkInfo != null) {
                    accessPoint.update(lastInfo, lastNetworkInfo);
                }
                accessPoints.add(accessPoint);
                apMap.put(accessPoint.ssid, accessPoint);
            }
        }

        final List<ScanResult> results = wifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0 ||
                        result.capabilities.contains("[IBSS]")) {
                    continue;
                }

                boolean found = false;
                for (AccessPoint accessPoint : apMap.getAll(result.SSID)) {
                    if (accessPoint.update(result))
                        found = true;
                }
                if (!found) {
                    AccessPoint accessPoint = new AccessPoint(context, result);
                    if (lastInfo != null && lastNetworkInfo != null) {
                        accessPoint.update(lastInfo, lastNetworkInfo);
                    }
                    accessPoints.add(accessPoint);
                    apMap.put(accessPoint.ssid, accessPoint);
                }
            }
        }

        // Pre-sort accessPoints to speed preference insertion
        Collections.sort(accessPoints);
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
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
            mConnected.set(info.isConnected());
            changeNextButtonState(info.isConnected());
            updateAccessPoints();
            updateNetworkInfo(info);
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            updateNetworkInfo(null);
        }
    }

    private void updateNetworkInfo(NetworkInfo networkInfo) {
        /* sticky broadcasts can call this when wifi is disabled */
        if (!mWifiManager.isWifiEnabled()) {
            mScanner.pause();
            return;
        }

        if (networkInfo != null &&
                networkInfo.getDetailedState() == DetailedState.OBTAINING_IPADDR) {
            mScanner.pause();
        } else {
            mScanner.resume();
        }

        mLastInfo = mWifiManager.getConnectionInfo();
        if (networkInfo != null) {
            mLastNetworkInfo = networkInfo;
        }

        for (int i = getPreferenceScreen().getPreferenceCount() - 1; i >= 0; --i) {
            // Maybe there's a WifiConfigPreference
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof AccessPoint) {
                final AccessPoint accessPoint = (AccessPoint) preference;
                accessPoint.update(mLastInfo, mLastNetworkInfo);
            }
        }
    }

    private void updateWifiState(int state) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }

        switch (state) {
            case WifiManager.WIFI_STATE_ENABLED:
                mScanner.resume();
                return; // not break, to avoid the call to pause() below

            case WifiManager.WIFI_STATE_ENABLING:
                addMessagePreference(R.string.wifi_starting);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                break;
        }

        mLastInfo = null;
        mLastNetworkInfo = null;
        mScanner.pause();
    }

    /**
     * Renames/replaces "Next" button when appropriate. "Next" button usually exists in
     * Wifi setup screens, not in usual wifi settings screen.
     *
     * @param enabled true when the device is connected to a wifi network.
     */
    private void changeNextButtonState(boolean enabled) {
        if (mEnableNextOnConnection && hasNextButton()) {
            getNextButton().setEnabled(enabled);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == WifiDialog.BUTTON_FORGET && mSelectedAccessPoint != null) {
            forget();
        } else if (button == WifiDialog.BUTTON_SUBMIT) {
            if (mDialog != null) {
                submit(mDialog.getController());
            }
        }
    }

    /* package */ void submit(WifiConfigController configController) {

        final WifiConfiguration config = configController.getConfig();

        if (config == null) {
            if (mSelectedAccessPoint != null
                    && mSelectedAccessPoint.networkId != INVALID_NETWORK_ID) {
                connect(mSelectedAccessPoint.networkId);
            }
        } else if (config.networkId != INVALID_NETWORK_ID) {
            if (mSelectedAccessPoint != null) {
                mWifiManager.save(config, mSaveListener);
            }
        } else {
            if (configController.isEdit()) {
                mWifiManager.save(config, mSaveListener);
            } else {
                connect(config);
            }
        }

        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();
    }

    /* package */ void forget() {
        if (mSelectedAccessPoint.networkId == INVALID_NETWORK_ID) {
            if (mSelectedAccessPoint.getNetworkInfo().getState() != State.DISCONNECTED) {
                // Network is active but has no network ID - must be ephemeral.
                mWifiManager.disableEphemeralNetwork(
                        AccessPoint.convertToQuotedString(mSelectedAccessPoint.ssid));
            } else {
                // Should not happen, but a monkey seems to trigger it
                Log.e(TAG, "Failed to forget invalid network " + mSelectedAccessPoint.getConfig());
                return;
            }
        } else {
            mWifiManager.forget(mSelectedAccessPoint.networkId, mForgetListener);
        }


        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }
        updateAccessPoints();

        // We need to rename/replace "Next" button in wifi setup context.
        changeNextButtonState(false);
    }

    protected void connect(final WifiConfiguration config) {
        mWifiManager.connect(config, mConnectListener);
    }

    protected void connect(final int networkId) {
        mWifiManager.connect(networkId, mConnectListener);
    }

    /**
     * Refreshes acccess points and ask Wifi module to scan networks again.
     */
    /* package */ void refreshAccessPoints() {
        if (mWifiManager.isWifiEnabled()) {
            mScanner.resume();
        }

        getPreferenceScreen().removeAll();
    }

    /**
     * Called when "add network" button is pressed.
     */
    /* package */ void onAddNetworkPressed() {
        // No exact access point is selected.
        mSelectedAccessPoint = null;
        showDialog(null, true);
    }

    /* package */ int getAccessPointsCount() {
        final boolean wifiIsEnabled = mWifiManager.isWifiEnabled();
        if (wifiIsEnabled) {
            return getPreferenceScreen().getPreferenceCount();
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

    @Override
    protected int getHelpResource() {
        return R.string.help_url_wifi;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.wifi_settings);
                data.screenTitle = res.getString(R.string.wifi_settings);
                data.keywords = res.getString(R.string.keywords_wifi);
                result.add(data);

                // Add available Wi-Fi access points
                WifiManager wifiManager =
                        (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                final Collection<AccessPoint> accessPoints =
                        constructAccessPoints(context, wifiManager, null, null);
                for (AccessPoint accessPoint : accessPoints) {
                    // We are indexing only the saved Wi-Fi networks.
                    if (accessPoint.getConfig() == null) continue;
                    data = new SearchIndexableRaw(context);
                    data.title = accessPoint.getTitle().toString();
                    data.screenTitle = res.getString(R.string.wifi_settings);
                    data.enabled = enabled;
                    result.add(data);
                }

                return result;
            }
        };
}

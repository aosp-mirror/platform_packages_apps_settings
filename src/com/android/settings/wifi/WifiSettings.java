/*
 * Copyright (C) 2007 The Android Open Source Project
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

import com.android.settings.ProgressCategory;
import com.android.settings.R;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import java.util.Set;
import java.util.WeakHashMap;

/**
 * Settings screen for WiFi. This will be launched from the main system settings.
 */
public class WifiSettings extends PreferenceActivity implements WifiLayer.Callback,
        DialogInterface.OnDismissListener {

    private static final String TAG = "WifiSettings";

    //============================
    // Preference/activity member variables
    //============================

    private static final String INSTANCE_KEY_DIALOG_BUNDLE =
            "com.android.settings.wifi.WifiSettings:dialogBundle";
    /*
     * We don't use Activity's dialog management because AlertDialog isn't fully
     * able to change many of its features after it's been created, and the
     * dialog management only creates once.
     */
    private Dialog mDialog;

    private static final String KEY_ONLY_ACCESS_POINTS = "only_access_points";
    private static final String KEY_ADD_OTHER_NETWORK = "add_other_network";

    private static final int CONTEXT_MENU_ID_CONNECT = Menu.FIRST;
    private static final int CONTEXT_MENU_ID_FORGET = Menu.FIRST + 1;
    private static final int CONTEXT_MENU_ID_CHANGE_PASSWORD = Menu.FIRST + 2;

    private static final int MENU_ID_SCAN = Menu.FIRST;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 1;

    private static final String KEY_WIFI_ENABLED = "wifi_enabled";
    private static final String KEY_OPEN_NETWORK_NOTIFICATIONS_ENABLED =
            "open_network_notifications_enabled";
    private static final String KEY_ACCESS_POINTS = "access_points";

    private ProgressCategory mApCategory;
    private CheckBoxPreference mWifiEnabled;
    private WifiEnabler mWifiEnabler;
    private CheckBoxPreference mOpenNetworkNotificationsEnabled;
    private Preference mAddOtherNetwork;

    private WeakHashMap<AccessPointState, AccessPointPreference> mAps;

    private KeyStore mKeyStore = KeyStore.getInstance();
    private AccessPointState mResumeState = null;
    private int mResumeMode;

    //============================
    // Wifi member variables
    //============================

    private WifiLayer mWifiLayer;

    //============================
    // Activity lifecycle
    //============================

    public WifiSettings() {
        mAps = new WeakHashMap<AccessPointState, AccessPointPreference>();
        mWifiLayer = new WifiLayer(this, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        onCreatePreferences();
        mWifiLayer.onCreate();

        onCreatedWifi();
        mWifiLayer.onCreatedCallback();
    }

    private int getPreferenceResource() {
        if (getIntent().getBooleanExtra(KEY_ONLY_ACCESS_POINTS, false)) {
            return R.xml.wifi_access_points;
        } else {
            return R.xml.wifi_settings;
        }
    }

    /**
     * Shouldn't have any dependency on the wifi layer.
     */
    private void onCreatePreferences() {
        addPreferencesFromResource(getPreferenceResource());

        final PreferenceScreen preferenceScreen = getPreferenceScreen();

        mApCategory = (ProgressCategory) preferenceScreen.findPreference(KEY_ACCESS_POINTS);
        // We don't want the ordering to be the order preferences are added,
        // instead we want*:
        //   1) preferred, visible APs
        //   2) visible APs
        //   3) preferred, APs out of range
        //   * this ordering logic is in AccessPointPreference's compareTo
        mApCategory.setOrderingAsAdded(false);

        if (!getIntent().getBooleanExtra(KEY_ONLY_ACCESS_POINTS, false)) {
            mWifiEnabled = (CheckBoxPreference) preferenceScreen.findPreference(KEY_WIFI_ENABLED);
            mWifiEnabler = new WifiEnabler(this, (WifiManager) getSystemService(WIFI_SERVICE),
                    mWifiEnabled);

            mOpenNetworkNotificationsEnabled = (CheckBoxPreference) preferenceScreen
                    .findPreference(KEY_OPEN_NETWORK_NOTIFICATIONS_ENABLED);
            mOpenNetworkNotificationsEnabled.setChecked(Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0) == 1);
        }

        mAddOtherNetwork = preferenceScreen.findPreference(KEY_ADD_OTHER_NETWORK);

        registerForContextMenu(getListView());
    }

    private void onCreatedWifi() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWifiLayer.onResume();
        if (mWifiEnabler != null) {
            mWifiEnabler.resume();
        }
        // do what we should have after keystore is unlocked.
        if (mResumeState != null) {
            if (mKeyStore.test() == KeyStore.NO_ERROR) {
                showAccessPointDialog(mResumeState, mResumeMode);
            }
            mResumeMode = -1;
            mResumeState = null;
        } else {
            if (mResumeMode == AccessPointDialog.MODE_CONFIGURE) {
                if (mKeyStore.test() == KeyStore.NO_ERROR) {
                    ((AccessPointDialog) mDialog).enableEnterpriseFields();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWifiLayer.onPause();
        if (mWifiEnabler != null) {
            mWifiEnabler.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_ID_SCAN, 0, R.string.scan_wifi)
            .setIcon(R.drawable.ic_menu_scan_network);

        menu.add(0, MENU_ID_ADVANCED, 0, R.string.wifi_menu_advanced)
            .setIcon(android.R.drawable.ic_menu_manage);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {

            case MENU_ID_SCAN:
                mWifiLayer.attemptScan();
                return true;

            case MENU_ID_ADVANCED:
                Intent intent = new Intent(this, AdvancedSettings.class);
                startActivity(intent);
                return true;

            default:
                return false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mDialog != null) {
            Bundle dialogBundle = mDialog.onSaveInstanceState();
            outState.putBundle(INSTANCE_KEY_DIALOG_BUNDLE, dialogBundle);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        Bundle dialogBundle = state.getBundle(INSTANCE_KEY_DIALOG_BUNDLE);
        if (dialogBundle != null) {
            mDialog = new AccessPointDialog(this, mWifiLayer);
            mDialog.onRestoreInstanceState(dialogBundle);
            showDialog(mDialog);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onDismiss(DialogInterface dialog) {
        if (dialog == mDialog) {
            mDialog = null;
            mResumeMode = -1;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AccessPointState state = getStateFromMenuInfo(menuInfo);
        if (state == null) {
            return;
        }

        menu.setHeaderTitle(state.getHumanReadableSsid());

        if (state.isConnectable()) {
            menu.add(0, CONTEXT_MENU_ID_CONNECT, 0, R.string.wifi_context_menu_connect);
        }

        if (state.isForgetable()) {
            menu.add(0, CONTEXT_MENU_ID_FORGET, 1, R.string.wifi_context_menu_forget);

            if (state.hasPassword()) {
                menu.add(0, CONTEXT_MENU_ID_CHANGE_PASSWORD, 2,
                        R.string.wifi_context_menu_change_password);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AccessPointState state = getStateFromMenuInfo(item.getMenuInfo());
        if (state == null) {
            return false;
        }

        switch (item.getItemId()) {

            case CONTEXT_MENU_ID_CONNECT:
                connectToNetwork(state);
                return true;

            case CONTEXT_MENU_ID_FORGET:
                mWifiLayer.forgetNetwork(state);
                return true;

            case CONTEXT_MENU_ID_CHANGE_PASSWORD:
                showAccessPointDialog(state, AccessPointDialog.MODE_CONFIGURE);
                return true;

            default:
                return false;
        }
    }

    /**
     * Decides what needs to happen to connect to a particular access point. If
     * it is secured and doesn't already have a password, it will bring up a
     * password box. Otherwise it will just connect.
     */
    private void connectToNetwork(AccessPointState state) {
        if (state.hasSecurity() && !state.hasPassword()) {
            showAccessPointDialog(state, AccessPointDialog.MODE_INFO);
        } else {
            mWifiLayer.connectToNetwork(state);
        }
    }

    private AccessPointState getStateFromMenuInfo(ContextMenuInfo menuInfo) {
        if ((menuInfo == null) || !(menuInfo instanceof AdapterContextMenuInfo)) {
            return null;
        }

        AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
        Preference pref = (Preference) getPreferenceScreen().getRootAdapter().getItem(
                adapterMenuInfo.position);
        if (pref == null || !(pref instanceof AccessPointPreference)) {
            return null;
        }

        return ((AccessPointPreference) pref).getAccessPointState();
    }

    //============================
    // Preference callbacks
    //============================

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        if (preference == mAddOtherNetwork) {
            showAddOtherNetworkDialog();
        } else if (preference == mOpenNetworkNotificationsEnabled) {
            Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON,
                mOpenNetworkNotificationsEnabled.isChecked() ? 1 : 0);
        } else if (preference instanceof AccessPointPreference) {
            AccessPointState state = ((AccessPointPreference) preference).getAccessPointState();
            showAccessPointDialog(state, AccessPointDialog.MODE_INFO);
        }

        return false;
    }

    //============================
    // Wifi-related
    //============================

    public WifiLayer getWifiLayer() {
        return mWifiLayer;
    }

    private void showAddOtherNetworkDialog() {
        AccessPointDialog dialog = new AccessPointDialog(this, mWifiLayer);
        dialog.setState(new AccessPointState(this));
        dialog.setMode(AccessPointDialog.MODE_CONFIGURE);
        dialog.setTitle(R.string.wifi_add_other_network);
        dialog.setAutoSecurityAllowed(false);
        mResumeMode = AccessPointDialog.MODE_CONFIGURE;
        showDialog(dialog);
    }

    public void showAccessPointDialog(AccessPointState state, int mode) {
        if (state.isEnterprise() && mKeyStore.test() != KeyStore.NO_ERROR) {
            Credentials.getInstance().unlock(this);
            mResumeState = state;
            mResumeMode = mode;
            return;
        }
        AccessPointDialog dialog = new AccessPointDialog(this, mWifiLayer);
        dialog.setMode(mode);
        dialog.setState(state);
        showDialog(dialog);
    }

    private void showDialog(Dialog dialog) {
        // Have only one dialog open at a time
        if (mDialog != null) {
            mDialog.dismiss();
        }

        mDialog = dialog;
        if (dialog != null) {
            dialog.setOnDismissListener(this);
            dialog.show();
        }
    }

    //============================
    // Wifi callbacks
    //============================

    public void onError(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show();
    }

    public void onScanningStatusChanged(boolean started) {
        mApCategory.setProgress(started);
    }

    public void onAccessPointSetChanged(AccessPointState ap, boolean added) {

        AccessPointPreference pref = mAps.get(ap);

        if (WifiLayer.LOGV) {
            Log.v(TAG, "onAccessPointSetChanged with " + ap + " and "
                    + (added ? "added" : "removed") + ", found pref " + pref);
        }

        if (added) {

            if (pref == null) {
                pref = new AccessPointPreference(this, ap);
                mAps.put(ap, pref);
            } else {
                pref.setEnabled(true);
            }

            mApCategory.addPreference(pref);

        } else {

            mAps.remove(ap);

            if (pref != null) {
                mApCategory.removePreference(pref);
            }

        }
    }

    public void onAccessPointsStateChanged(boolean enabled) {
        if (enabled) {
            mApCategory.setEnabled(true);
        } else {
            mApCategory.removeAll();
            mAps.clear();
        }

        mAddOtherNetwork.setEnabled(enabled);
    }

    public void onRetryPassword(AccessPointState ap) {

        if ((mDialog != null) && mDialog.isShowing()) {
            // If we're already showing a dialog, ignore this request
            return;
        }

        showAccessPointDialog(ap, AccessPointDialog.MODE_RETRY_PASSWORD);
    }

}

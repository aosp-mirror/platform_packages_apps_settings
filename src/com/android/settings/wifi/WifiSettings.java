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

import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Process;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPoint.AccessPointListener;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiStatusTracker;
import com.android.settingslib.wifi.WifiTracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

/**
 * Two types of UI are provided here.
 *
 * The first is for "usual Settings", appearing as any other Setup fragment.
 *
 * The second is for Setup Wizard, with a simplified interface that hides the action bar
 * and menus.
 */
public class WifiSettings extends RestrictedSettingsFragment
        implements Indexable, WifiTracker.WifiListener, AccessPointListener,
        WifiDialog.WifiDialogListener {

    private static final String TAG = "WifiSettings";

    /* package */ static final int MENU_ID_WPS_PBC = Menu.FIRST;
    private static final int MENU_ID_WPS_PIN = Menu.FIRST + 1;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 4;
    private static final int MENU_ID_SCAN = Menu.FIRST + 5;
    private static final int MENU_ID_CONNECT = Menu.FIRST + 6;
    private static final int MENU_ID_FORGET = Menu.FIRST + 7;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 8;
    private static final int MENU_ID_WRITE_NFC = Menu.FIRST + 9;
    private static final int MENU_ID_CONFIGURE = Menu.FIRST + 10;

    public static final int WIFI_DIALOG_ID = 1;
    /* package */ static final int WPS_PBC_DIALOG_ID = 2;
    private static final int WPS_PIN_DIALOG_ID = 3;
    private static final int WRITE_NFC_DIALOG_ID = 6;

    // Instance state keys
    private static final String SAVE_DIALOG_MODE = "dialog_mode";
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";
    private static final String SAVED_WIFI_NFC_DIALOG_STATE = "wifi_nfc_dlg_state";

    private static final String PREF_KEY_EMPTY_WIFI_LIST = "wifi_empty_list";

    protected WifiManager mWifiManager;
    private WifiManager.ActionListener mConnectListener;
    private WifiManager.ActionListener mSaveListener;
    private WifiManager.ActionListener mForgetListener;

    private WifiEnabler mWifiEnabler;
    // An access point being editted is stored here.
    private AccessPoint mSelectedAccessPoint;

    private WifiDialog mDialog;
    private WriteWifiConfigToNfcDialog mWifiToNfcDialog;

    private ProgressBar mProgressHeader;

    // this boolean extra specifies whether to disable the Next button when not connected. Used by
    // account creation outside of setup wizard.
    private static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";
    // This string extra specifies a network to open the connect dialog on, so the user can enter
    // network credentials.  This is used by quick settings for secured networks.
    private static final String EXTRA_START_CONNECT_SSID = "wifi_start_connect_ssid";

    // should Next button only be enabled when we have a connection?
    private boolean mEnableNextOnConnection;

    // Save the dialog details
    private int mDialogMode;
    private AccessPoint mDlgAccessPoint;
    private Bundle mAccessPointSavedState;
    private Bundle mWifiNfcDialogSavedState;

    private WifiTracker mWifiTracker;
    private String mOpenSsid;

    private HandlerThread mBgThread;

    private AccessPointPreference.UserBadgeCache mUserBadgeCache;
    private Preference mAddPreference;

    private MenuItem mScanMenuItem;

    /* End of "used in Wifi Setup context" */

    public WifiSettings() {
        super(DISALLOW_CONFIG_WIFI);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Activity activity = getActivity();
        if (activity != null) {
            mProgressHeader = (ProgressBar) setPinnedHeaderView(R.layout.wifi_progress_header);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.wifi_settings);
        mAddPreference = new Preference(getContext());
        mAddPreference.setIcon(R.drawable.ic_menu_add_inset);
        mAddPreference.setTitle(R.string.wifi_add_network);

        mUserBadgeCache = new AccessPointPreference.UserBadgeCache(getPackageManager());

        mBgThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        mBgThread.start();
    }

    @Override
    public void onDestroy() {
        mBgThread.quit();
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWifiTracker =
                new WifiTracker(getActivity(), this, mBgThread.getLooper(), true, true, false);
        mWifiManager = mWifiTracker.getManager();

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
            mDialogMode = savedInstanceState.getInt(SAVE_DIALOG_MODE);
            if (savedInstanceState.containsKey(SAVE_DIALOG_ACCESS_POINT_STATE)) {
                mAccessPointSavedState =
                    savedInstanceState.getBundle(SAVE_DIALOG_ACCESS_POINT_STATE);
            }

            if (savedInstanceState.containsKey(SAVED_WIFI_NFC_DIALOG_STATE)) {
                mWifiNfcDialogSavedState =
                    savedInstanceState.getBundle(SAVED_WIFI_NFC_DIALOG_STATE);
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

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);

        if (intent.hasExtra(EXTRA_START_CONNECT_SSID)) {
            mOpenSsid = intent.getStringExtra(EXTRA_START_CONNECT_SSID);
            onAccessPointsChanged();
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
        removePreference("dummy");
        if (mWifiEnabler != null) {
            mWifiEnabler.resume(activity);
        }

        mWifiTracker.startTracking();
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWifiEnabler != null) {
            mWifiEnabler.pause();
        }

        mWifiTracker.stopTracking();
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
        final boolean wifiIsEnabled = mWifiTracker.isWifiEnabled();
        mScanMenuItem = menu.add(Menu.NONE, MENU_ID_SCAN, 0, R.string.menu_stats_refresh);
        mScanMenuItem.setEnabled(wifiIsEnabled)
               .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_ADVANCED, 0, R.string.wifi_menu_advanced)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_CONFIGURE, 0, R.string.wifi_menu_configure)
                .setIcon(R.drawable.ic_settings_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.WIFI;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the dialog is showing, save its state.
        if (mDialog != null && mDialog.isShowing()) {
            outState.putInt(SAVE_DIALOG_MODE, mDialogMode);
            if (mDlgAccessPoint != null) {
                mAccessPointSavedState = new Bundle();
                mDlgAccessPoint.saveWifiState(mAccessPointSavedState);
                outState.putBundle(SAVE_DIALOG_ACCESS_POINT_STATE, mAccessPointSavedState);
            }
        }

        if (mWifiToNfcDialog != null && mWifiToNfcDialog.isShowing()) {
            Bundle savedState = new Bundle();
            mWifiToNfcDialog.saveState(savedState);
            outState.putBundle(SAVED_WIFI_NFC_DIALOG_STATE, savedState);
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
                MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_FORCE_SCAN);
                mWifiTracker.forceScan();
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
            case MENU_ID_CONFIGURE:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            ConfigureWifiSettings.class.getCanonicalName(), null,
                            R.string.wifi_configure_titlebar, null, this, 0);
                } else {
                    startFragment(this, ConfigureWifiSettings.class.getCanonicalName(),
                            R.string.wifi_configure_titlebar, -1 /* Do not request a results */,
                            null);
                }
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
            Preference preference = (Preference) view.getTag();

            if (preference instanceof LongPressAccessPointPreference) {
                mSelectedAccessPoint =
                        ((LongPressAccessPointPreference) preference).getAccessPoint();
                menu.setHeaderTitle(mSelectedAccessPoint.getSsid());
                if (mSelectedAccessPoint.isConnectable()) {
                    menu.add(Menu.NONE, MENU_ID_CONNECT, 0, R.string.wifi_menu_connect);
                }

                WifiConfiguration config = mSelectedAccessPoint.getConfig();
                // Some configs are ineditable
                if (isEditabilityLockedDown(getActivity(), config)) {
                    return;
                }

                if (mSelectedAccessPoint.isSaved() || mSelectedAccessPoint.isEphemeral()) {
                    // Allow forgetting a network if either the network is saved or ephemerally
                    // connected. (In the latter case, "forget" blacklists the network so it won't
                    // be used again, ephemerally).
                    menu.add(Menu.NONE, MENU_ID_FORGET, 0, R.string.wifi_menu_forget);
                }
                if (mSelectedAccessPoint.isSaved()) {
                    menu.add(Menu.NONE, MENU_ID_MODIFY, 0, R.string.wifi_menu_modify);
                    NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
                    if (nfcAdapter != null && nfcAdapter.isEnabled() &&
                            mSelectedAccessPoint.getSecurity() != AccessPoint.SECURITY_NONE) {
                        // Only allow writing of NFC tags for password-protected networks.
                        menu.add(Menu.NONE, MENU_ID_WRITE_NFC, 0, R.string.wifi_menu_write_to_nfc);
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
                if (mSelectedAccessPoint.isSaved()) {
                    connect(mSelectedAccessPoint.getConfig());
                } else if (mSelectedAccessPoint.getSecurity() == AccessPoint.SECURITY_NONE) {
                    /** Bypass dialog for unsecured networks */
                    mSelectedAccessPoint.generateOpenNetworkConfig();
                    connect(mSelectedAccessPoint.getConfig());
                } else {
                    showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_CONNECT);
                }
                return true;
            }
            case MENU_ID_FORGET: {
                forget();
                return true;
            }
            case MENU_ID_MODIFY: {
                showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_MODIFY);
                return true;
            }
            case MENU_ID_WRITE_NFC:
                showDialog(WRITE_NFC_DIALOG_ID);
                return true;

        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof LongPressAccessPointPreference) {
            mSelectedAccessPoint = ((LongPressAccessPointPreference) preference).getAccessPoint();
            if (mSelectedAccessPoint == null) {
                return false;
            }
            /** Bypass dialog for unsecured, unsaved, and inactive networks */
            if (mSelectedAccessPoint.getSecurity() == AccessPoint.SECURITY_NONE &&
                    !mSelectedAccessPoint.isSaved() && !mSelectedAccessPoint.isActive()) {
                mSelectedAccessPoint.generateOpenNetworkConfig();
                connect(mSelectedAccessPoint.getConfig());
            } else if (mSelectedAccessPoint.isSaved()) {
                showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_VIEW);
            } else {
                showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_CONNECT);
            }
        } else if (preference == mAddPreference) {
            onAddNetworkPressed();
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private void showDialog(AccessPoint accessPoint, int dialogMode) {
        if (accessPoint != null) {
            WifiConfiguration config = accessPoint.getConfig();
            if (isEditabilityLockedDown(getActivity(), config) && accessPoint.isActive()) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(),
                        RestrictedLockUtils.getDeviceOwner(getActivity()));
                return;
            }
        }

        if (mDialog != null) {
            removeDialog(WIFI_DIALOG_ID);
            mDialog = null;
        }

        // Save the access point and edit mode
        mDlgAccessPoint = accessPoint;
        mDialogMode = dialogMode;

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
                mDialog = new WifiDialog(getActivity(), this, ap, mDialogMode,
                        /* no hide submit/connect */ false);
                return mDialog;
            case WPS_PBC_DIALOG_ID:
                return new WpsDialog(getActivity(), WpsInfo.PBC);
            case WPS_PIN_DIALOG_ID:
                return new WpsDialog(getActivity(), WpsInfo.DISPLAY);
            case WRITE_NFC_DIALOG_ID:
                if (mSelectedAccessPoint != null) {
                    mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(
                            getActivity(), mSelectedAccessPoint.getConfig().networkId,
                            mSelectedAccessPoint.getSecurity(),
                            mWifiManager);
                } else if (mWifiNfcDialogSavedState != null) {
                    mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(
                            getActivity(), mWifiNfcDialogSavedState, mWifiManager);
                }

                return mWifiToNfcDialog;
        }
        return super.onCreateDialog(dialogId);
    }

    /**
     * Shows the latest access points available with supplemental information like
     * the strength of network and the security for it.
     */
    @Override
    public void onAccessPointsChanged() {
        // Safeguard from some delayed event handling
        if (getActivity() == null) return;
        if (isUiRestricted()) {
            if (!isUiRestrictedByOnlyAdmin()) {
                addMessagePreference(R.string.wifi_empty_list_user_restricted);
            }
            getPreferenceScreen().removeAll();
            return;
        }
        final int wifiState = mWifiManager.getWifiState();

        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                // AccessPoints are automatically sorted with TreeSet.
                final Collection<AccessPoint> accessPoints =
                        mWifiTracker.getAccessPoints();

                boolean hasAvailableAccessPoints = false;
                int index = 0;
                cacheRemoveAllPrefs(getPreferenceScreen());
                for (AccessPoint accessPoint : accessPoints) {
                    // Ignore access points that are out of range.
                    if (accessPoint.getLevel() != -1) {
                        String key = accessPoint.getBssid();
                        if (TextUtils.isEmpty(key)) {
                            key = accessPoint.getSsidStr();
                        }
                        hasAvailableAccessPoints = true;
                        LongPressAccessPointPreference pref = (LongPressAccessPointPreference)
                                getCachedPreference(key);
                        if (pref != null) {
                            pref.setOrder(index++);
                            continue;
                        }
                        LongPressAccessPointPreference
                                preference = new LongPressAccessPointPreference(accessPoint,
                                getPrefContext(), mUserBadgeCache, false,
                                R.drawable.ic_wifi_signal_0, this);
                        preference.setKey(key);
                        preference.setOrder(index++);
                        if (mOpenSsid != null && mOpenSsid.equals(accessPoint.getSsidStr())
                                && !accessPoint.isSaved()
                                && accessPoint.getSecurity() != AccessPoint.SECURITY_NONE) {
                            onPreferenceTreeClick(preference);
                            mOpenSsid = null;
                        }
                        getPreferenceScreen().addPreference(preference);
                        accessPoint.setListener(this);
                        preference.refresh();
                    }
                }
                removeCachedPrefs(getPreferenceScreen());
                if (!hasAvailableAccessPoints) {
                    setProgressBarVisible(true);
                    Preference pref = new Preference(getContext()) {
                        @Override
                        public void onBindViewHolder(PreferenceViewHolder holder) {
                            super.onBindViewHolder(holder);
                            // Show a line on each side of add network.
                            holder.setDividerAllowedBelow(true);
                        }
                    };
                    pref.setSelectable(false);
                    pref.setSummary(R.string.wifi_empty_list_wifi_on);
                    pref.setOrder(0);
                    pref.setKey(PREF_KEY_EMPTY_WIFI_LIST);
                    getPreferenceScreen().addPreference(pref);
                    mAddPreference.setOrder(1);
                    getPreferenceScreen().addPreference(mAddPreference);
                } else {
                    mAddPreference.setOrder(index++);
                    getPreferenceScreen().addPreference(mAddPreference);
                    setProgressBarVisible(false);
                }
                if (mScanMenuItem != null) {
                    mScanMenuItem.setEnabled(true);
                }
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                getPreferenceScreen().removeAll();
                setProgressBarVisible(true);
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                addMessagePreference(R.string.wifi_stopping);
                setProgressBarVisible(true);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                setProgressBarVisible(false);
                if (mScanMenuItem != null) {
                    mScanMenuItem.setEnabled(false);
                }
                break;
        }
    }

    private void setOffMessage() {
        if (isUiRestricted()) {
            if (!isUiRestrictedByOnlyAdmin()) {
                addMessagePreference(R.string.wifi_empty_list_user_restricted);
            }
            getPreferenceScreen().removeAll();
            return;
        }

        TextView emptyTextView = getEmptyTextView();
        if (emptyTextView == null) {
            return;
        }

        final CharSequence briefText = getText(R.string.wifi_empty_list_wifi_off);

        // Don't use WifiManager.isScanAlwaysAvailable() to check the Wi-Fi scanning mode. Instead,
        // read the system settings directly. Because when the device is in Airplane mode, even if
        // Wi-Fi scanning mode is on, WifiManager.isScanAlwaysAvailable() still returns "off".
        final ContentResolver resolver = getActivity().getContentResolver();
        final boolean wifiScanningMode = Settings.Global.getInt(
                resolver, Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1;

        if (!wifiScanningMode) {
            // Show only the brief text if the user is not allowed to configure scanning settings,
            // or the scanning mode has been turned off.
            emptyTextView.setText(briefText, BufferType.SPANNABLE);
        } else {
            // Append the description of scanning settings with link.
            final StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(briefText);
            contentBuilder.append("\n\n");
            contentBuilder.append(getText(R.string.wifi_scan_notify_text));
            LinkifyUtils.linkify(emptyTextView, contentBuilder, new LinkifyUtils.OnClickListener() {
                @Override
                public void onClick() {
                    final SettingsActivity activity =
                            (SettingsActivity) WifiSettings.this.getActivity();
                    activity.startPreferencePanel(ScanningSettings.class.getName(), null,
                            R.string.location_scanning_screen_title, null, null, 0);
                }
            });
        }
        // Embolden and enlarge the brief description anyway.
        Spannable boldSpan = (Spannable) emptyTextView.getText();
        boldSpan.setSpan(
                new TextAppearanceSpan(getActivity(), android.R.style.TextAppearance_Medium), 0,
                briefText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getPreferenceScreen().removeAll();
    }

    private void addMessagePreference(int messageId) {
        TextView emptyTextView = getEmptyTextView();
        if (emptyTextView != null) emptyTextView.setText(messageId);
        getPreferenceScreen().removeAll();
    }

    protected void setProgressBarVisible(boolean visible) {
        if (mProgressHeader != null) {
            mProgressHeader.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                addMessagePreference(R.string.wifi_starting);
                setProgressBarVisible(true);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                setProgressBarVisible(false);
                break;
        }
    }

    @Override
    public void onConnectedChanged() {
        changeNextButtonState(mWifiTracker.isConnected());
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
    public void onForget(WifiDialog dialog) {
        forget();
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        if (mDialog != null) {
            submit(mDialog.getController());
        }
    }

    /* package */ void submit(WifiConfigController configController) {

        final WifiConfiguration config = configController.getConfig();

        if (config == null) {
            if (mSelectedAccessPoint != null
                    && mSelectedAccessPoint.isSaved()) {
                connect(mSelectedAccessPoint.getConfig());
            }
        } else if (configController.getMode() == WifiConfigUiBase.MODE_MODIFY) {
            mWifiManager.save(config, mSaveListener);
        } else {
            mWifiManager.save(config, mSaveListener);
            if (mSelectedAccessPoint != null) { // Not an "Add network"
                connect(config);
            }
        }

        mWifiTracker.resumeScanning();
    }

    /* package */ void forget() {
        MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_FORGET);
        if (!mSelectedAccessPoint.isSaved()) {
            if (mSelectedAccessPoint.getNetworkInfo() != null &&
                    mSelectedAccessPoint.getNetworkInfo().getState() != State.DISCONNECTED) {
                // Network is active but has no network ID - must be ephemeral.
                mWifiManager.disableEphemeralNetwork(
                        AccessPoint.convertToQuotedString(mSelectedAccessPoint.getSsidStr()));
            } else {
                // Should not happen, but a monkey seems to trigger it
                Log.e(TAG, "Failed to forget invalid network " + mSelectedAccessPoint.getConfig());
                return;
            }
        } else {
            mWifiManager.forget(mSelectedAccessPoint.getConfig().networkId, mForgetListener);
        }

        mWifiTracker.resumeScanning();

        // We need to rename/replace "Next" button in wifi setup context.
        changeNextButtonState(false);
    }

    protected void connect(final WifiConfiguration config) {
        MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_CONNECT);
        mWifiManager.connect(config, mConnectListener);
    }

    protected void connect(final int networkId) {
        MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_CONNECT);
        mWifiManager.connect(networkId, mConnectListener);
    }

    /**
     * Called when "add network" button is pressed.
     */
    /* package */ void onAddNetworkPressed() {
        MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_ADD_NETWORK);
        // No exact access point is selected.
        mSelectedAccessPoint = null;
        showDialog(null, WifiConfigUiBase.MODE_CONNECT);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_wifi;
    }

    @Override
    public void onAccessPointChanged(final AccessPoint accessPoint) {
        View view = getView();
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    Object tag = accessPoint.getTag();
                    if (tag != null) {
                        ((LongPressAccessPointPreference) tag).refresh();
                    }
                }
            });
        }
    }

    @Override
    public void onLevelChanged(AccessPoint accessPoint) {
        ((LongPressAccessPointPreference) accessPoint.getTag()).onLevelChanged();
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<>();
                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.wifi_settings);
                data.screenTitle = res.getString(R.string.wifi_settings);
                data.keywords = res.getString(R.string.keywords_wifi);
                result.add(data);

                // Add saved Wi-Fi access points
                final Collection<AccessPoint> accessPoints =
                        WifiTracker.getCurrentAccessPoints(context, true, false, false);
                for (AccessPoint accessPoint : accessPoints) {
                    data = new SearchIndexableRaw(context);
                    data.title = accessPoint.getSsidStr();
                    data.screenTitle = res.getString(R.string.wifi_settings);
                    data.enabled = enabled;
                    result.add(data);
                }

                return result;
            }
        };

    /**
     * Returns true if the config is not editable through Settings.
     * @param context Context of caller
     * @param config The WiFi config.
     * @return true if the config is not editable through Settings.
     */
    static boolean isEditabilityLockedDown(Context context, WifiConfiguration config) {
        return !canModifyNetwork(context, config);
    }

    /**
     * This method is a stripped version of WifiConfigStore.canModifyNetwork.
     * TODO: refactor to have only one method.
     * @param context Context of caller
     * @param config The WiFi config.
     * @return true if Settings can modify the config.
     */
    static boolean canModifyNetwork(Context context, WifiConfiguration config) {
        if (config == null) {
            return true;
        }

        final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);

        // Check if device has DPM capability. If it has and dpm is still null, then we
        // treat this case with suspicion and bail out.
        final PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN) && dpm == null) {
            return false;
        }

        boolean isConfigEligibleForLockdown = false;
        if (dpm != null) {
            final ComponentName deviceOwner = dpm.getDeviceOwnerComponentOnAnyUser();
            if (deviceOwner != null) {
                final int deviceOwnerUserId = dpm.getDeviceOwnerUserId();
                try {
                    final int deviceOwnerUid = pm.getPackageUidAsUser(deviceOwner.getPackageName(),
                            deviceOwnerUserId);
                    isConfigEligibleForLockdown = deviceOwnerUid == config.creatorUid;
                } catch (NameNotFoundException e) {
                    // don't care
                }
            }
        }
        if (!isConfigEligibleForLockdown) {
            return true;
        }

        final ContentResolver resolver = context.getContentResolver();
        final boolean isLockdownFeatureEnabled = Settings.Global.getInt(resolver,
                Settings.Global.WIFI_DEVICE_OWNER_CONFIGS_LOCKDOWN, 0) != 0;
        return !isLockdownFeatureEnabled;
    }

    private static class SummaryProvider extends BroadcastReceiver
            implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final WifiManager mWifiManager;
        private final WifiStatusTracker mWifiTracker;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mWifiManager = context.getSystemService(WifiManager.class);
            mWifiTracker = new WifiStatusTracker(mWifiManager);
        }

        private CharSequence getSummary() {
            if (!mWifiTracker.enabled) {
                return mContext.getString(R.string.wifi_disabled_generic);
            }
            if (!mWifiTracker.connected) {
                return mContext.getString(R.string.disconnected);
            }
            return mWifiTracker.ssid;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
                filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
                mSummaryLoader.registerReceiver(this, filter);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mWifiTracker.handleBroadcast(intent);
            mSummaryLoader.setSummary(this, getSummary());
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
}

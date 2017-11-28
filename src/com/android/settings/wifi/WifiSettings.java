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

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SummaryUpdater.OnSummaryChangeListener;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPoint.AccessPointListener;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

import java.util.ArrayList;
import java.util.List;

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
    private static final int MENU_ID_CONNECT = Menu.FIRST + 6;
    private static final int MENU_ID_FORGET = Menu.FIRST + 7;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 8;
    private static final int MENU_ID_WRITE_NFC = Menu.FIRST + 9;

    public static final int WIFI_DIALOG_ID = 1;
    /* package */ static final int WPS_PBC_DIALOG_ID = 2;
    private static final int WPS_PIN_DIALOG_ID = 3;
    private static final int WRITE_NFC_DIALOG_ID = 6;

    // Instance state keys
    private static final String SAVE_DIALOG_MODE = "dialog_mode";
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";
    private static final String SAVED_WIFI_NFC_DIALOG_STATE = "wifi_nfc_dlg_state";

    private static final String PREF_KEY_EMPTY_WIFI_LIST = "wifi_empty_list";
    private static final String PREF_KEY_CONNECTED_ACCESS_POINTS = "connected_access_point";
    private static final String PREF_KEY_ACCESS_POINTS = "access_points";
    private static final String PREF_KEY_ADDITIONAL_SETTINGS = "additional_settings";
    private static final String PREF_KEY_CONFIGURE_WIFI_SETTINGS = "configure_settings";
    private static final String PREF_KEY_SAVED_NETWORKS = "saved_networks";

    private final Runnable mUpdateAccessPointsRunnable = () -> {
        updateAccessPointPreferences();
    };
    private final Runnable mHideProgressBarRunnable = () -> {
        setProgressBarVisible(false);
    };

    protected WifiManager mWifiManager;
    private WifiManager.ActionListener mConnectListener;
    private WifiManager.ActionListener mSaveListener;
    private WifiManager.ActionListener mForgetListener;

    /**
     * The state of {@link #isUiRestricted()} at {@link #onCreate(Bundle)}}. This is neccesary to
     * ensure that behavior is consistent if {@link #isUiRestricted()} changes. It could be changed
     * by the Test DPC tool in AFW mode.
     */
    private boolean mIsRestricted;

    private WifiEnabler mWifiEnabler;
    // An access point being editted is stored here.
    private AccessPoint mSelectedAccessPoint;

    private WifiDialog mDialog;
    private WriteWifiConfigToNfcDialog mWifiToNfcDialog;

    private View mProgressHeader;

    // this boolean extra specifies whether to disable the Next button when not connected. Used by
    // account creation outside of setup wizard.
    private static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";
    // This string extra specifies a network to open the connect dialog on, so the user can enter
    // network credentials.  This is used by quick settings for secured networks, among other
    // things.
    public static final String EXTRA_START_CONNECT_SSID = "wifi_start_connect_ssid";

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

    private PreferenceCategory mConnectedAccessPointPreferenceCategory;
    private PreferenceCategory mAccessPointsPreferenceCategory;
    private PreferenceCategory mAdditionalSettingsPreferenceCategory;
    private Preference mAddPreference;
    private Preference mConfigureWifiSettingsPreference;
    private Preference mSavedNetworksPreference;
    private LinkablePreference mStatusMessagePreference;

    // For Search
    private static final String DATA_KEY_REFERENCE = "main_toggle_wifi";

    /**
     * Tracks whether the user initiated a connection via clicking in order to autoscroll to the
     * network once connected.
     */
    private boolean mClickedConnect;

    /* End of "used in Wifi Setup context" */

    public WifiSettings() {
        super(DISALLOW_CONFIG_WIFI);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Activity activity = getActivity();
        if (activity != null) {
            mProgressHeader = setPinnedHeaderView(R.layout.wifi_progress_header)
                    .findViewById(R.id.progress_bar_animation);
            setProgressBarVisible(false);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // TODO(b/37429702): Add animations and preference comparator back after initial screen is
        // loaded (ODR).
        setAnimationAllowed(false);

        addPreferences();

        mIsRestricted = isUiRestricted();

        mBgThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        mBgThread.start();
    }

    private void addPreferences() {
        addPreferencesFromResource(R.xml.wifi_settings);

        mConnectedAccessPointPreferenceCategory =
                (PreferenceCategory) findPreference(PREF_KEY_CONNECTED_ACCESS_POINTS);
        mAccessPointsPreferenceCategory =
                (PreferenceCategory) findPreference(PREF_KEY_ACCESS_POINTS);
        mAdditionalSettingsPreferenceCategory =
                (PreferenceCategory) findPreference(PREF_KEY_ADDITIONAL_SETTINGS);
        mConfigureWifiSettingsPreference = findPreference(PREF_KEY_CONFIGURE_WIFI_SETTINGS);
        mSavedNetworksPreference = findPreference(PREF_KEY_SAVED_NETWORKS);

        Context prefContext = getPrefContext();
        mAddPreference = new Preference(prefContext);
        mAddPreference.setIcon(R.drawable.ic_menu_add_inset);
        mAddPreference.setTitle(R.string.wifi_add_network);
        mStatusMessagePreference = new LinkablePreference(prefContext);

        mUserBadgeCache = new AccessPointPreference.UserBadgeCache(getPackageManager());
    }

    @Override
    public void onDestroy() {
        mBgThread.quit();
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWifiTracker = WifiTrackerFactory.create(
                getActivity(), this, mBgThread.getLooper(), true, true, false);
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
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mWifiEnabler != null) {
            mWifiEnabler.teardownSwitchController();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // On/off switch is hidden for Setup Wizard (returns null)
        mWifiEnabler = createWifiEnabler();

        mWifiTracker.startTracking();

        if (mIsRestricted) {
            restrictUi();
            return;
        }

        onWifiStateChanged(mWifiManager.getWifiState());
    }

    private void restrictUi() {
        if (!isUiRestrictedByOnlyAdmin()) {
            getEmptyTextView().setText(R.string.wifi_empty_list_user_restricted);
        }
        getPreferenceScreen().removeAll();
    }

    /**
     * Only update the AP list if there are not any APs currently shown.
     *
     * <p>Thus forceUpdate will only be called during cold start or when toggling between wifi on
     * and off. In other use cases, the previous APs will remain until the next update is received
     * from {@link WifiTracker}.
     */
    private void conditionallyForceUpdateAPs() {
        if (mAccessPointsPreferenceCategory.getPreferenceCount() > 0
                && mAccessPointsPreferenceCategory.getPreference(0) instanceof
                        AccessPointPreference) {
            // Make sure we don't update due to callbacks initiated by sticky broadcasts in
            // WifiTracker.
            Log.d(TAG, "Did not force update APs due to existing APs displayed");
            getView().removeCallbacks(mUpdateAccessPointsRunnable);
            return;
        }
        setProgressBarVisible(true);
        mWifiTracker.forceUpdate();
        if (WifiTracker.sVerboseLogging) {
            Log.i(TAG, "WifiSettings force update APs: " + mWifiTracker.getAccessPoints());
        }
        getView().removeCallbacks(mUpdateAccessPointsRunnable);
        updateAccessPointPreferences();
    }

    /**
     * @return new WifiEnabler or null (as overridden by WifiSettingsForSetupWizard)
     */
    private WifiEnabler createWifiEnabler() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        return new WifiEnabler(activity, new SwitchBarController(activity.getSwitchBar()),
            mMetricsFeatureProvider);
    }

    @Override
    public void onResume() {
        final Activity activity = getActivity();
        super.onResume();

        // Because RestrictedSettingsFragment's onResume potentially requests authorization,
        // which changes the restriction state, recalculate it.
        final boolean alreadyImmutablyRestricted = mIsRestricted;
        mIsRestricted = isUiRestricted();
        if (!alreadyImmutablyRestricted && mIsRestricted) {
            restrictUi();
        }

        if (mWifiEnabler != null) {
            mWifiEnabler.resume(activity);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mWifiEnabler != null) {
            mWifiEnabler.pause();
        }
    }

    @Override
    public void onStop() {
        mWifiTracker.stopTracking();
        getView().removeCallbacks(mUpdateAccessPointsRunnable);
        getView().removeCallbacks(mHideProgressBarRunnable);
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final boolean formerlyRestricted = mIsRestricted;
        mIsRestricted = isUiRestricted();
        if (formerlyRestricted && !mIsRestricted
                && getPreferenceScreen().getPreferenceCount() == 0) {
            // De-restrict the ui
            addPreferences();
        }
    }

    @Override
    public int getMetricsCategory() {
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
        if (mIsRestricted) {
            return false;
        }

        switch (item.getItemId()) {
            case MENU_ID_WPS_PBC:
                showDialog(WPS_PBC_DIALOG_ID);
                return true;
            case MENU_ID_WPS_PIN:
                showDialog(WPS_PIN_DIALOG_ID);
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
                boolean isSavedNetwork = mSelectedAccessPoint.isSaved();
                if (isSavedNetwork) {
                    connect(mSelectedAccessPoint.getConfig(), isSavedNetwork);
                } else if (mSelectedAccessPoint.getSecurity() == AccessPoint.SECURITY_NONE) {
                    /** Bypass dialog for unsecured networks */
                    mSelectedAccessPoint.generateOpenNetworkConfig();
                    connect(mSelectedAccessPoint.getConfig(), isSavedNetwork);
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
        // If the preference has a fragment set, open that
        if (preference.getFragment() != null) {
            preference.setOnPreferenceClickListener(null);
            return super.onPreferenceTreeClick(preference);
        }

        if (preference instanceof LongPressAccessPointPreference) {
            mSelectedAccessPoint = ((LongPressAccessPointPreference) preference).getAccessPoint();
            if (mSelectedAccessPoint == null) {
                return false;
            }
            if (mSelectedAccessPoint.isActive()) {
                return super.onPreferenceTreeClick(preference);
            }
            /**
             * Bypass dialog and connect to unsecured networks, or previously connected saved
             * networks, or Passpoint provided networks.
             */
            WifiConfiguration config = mSelectedAccessPoint.getConfig();
            if (mSelectedAccessPoint.getSecurity() == AccessPoint.SECURITY_NONE) {
                mSelectedAccessPoint.generateOpenNetworkConfig();
                connect(mSelectedAccessPoint.getConfig(), mSelectedAccessPoint.isSaved());
            } else if (mSelectedAccessPoint.isSaved() && config != null
                    && config.getNetworkSelectionStatus() != null
                    && config.getNetworkSelectionStatus().getHasEverConnected()) {
                connect(config, true /* isSavedNetwork */);
            } else if (mSelectedAccessPoint.isPasspoint()) {
                // Access point provided by an installed Passpoint provider, connect using
                // the associated config.
                connect(config, true /* isSavedNetwork */);
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
                if (mDlgAccessPoint == null && mAccessPointSavedState == null) {
                    // add new network
                    mDialog = WifiDialog
                            .createFullscreen(getActivity(), this, mDlgAccessPoint, mDialogMode);
                } else {
                    // modify network
                    if (mDlgAccessPoint == null) {
                        // restore AP from save state
                        mDlgAccessPoint = new AccessPoint(getActivity(), mAccessPointSavedState);
                        // Reset the saved access point data
                        mAccessPointSavedState = null;
                    }
                    mDialog = WifiDialog
                            .createModal(getActivity(), this, mDlgAccessPoint, mDialogMode);
                }

                mSelectedAccessPoint = mDlgAccessPoint;
                return mDialog;
            case WPS_PBC_DIALOG_ID:
                return new WpsDialog(getActivity(), WpsInfo.PBC);
            case WPS_PIN_DIALOG_ID:
                return new WpsDialog(getActivity(), WpsInfo.DISPLAY);
            case WRITE_NFC_DIALOG_ID:
                if (mSelectedAccessPoint != null) {
                    mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(
                            getActivity(),
                            mSelectedAccessPoint.getSecurity(),
                            new WifiManagerWrapper(mWifiManager));
                } else if (mWifiNfcDialogSavedState != null) {
                    mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(getActivity(),
                            mWifiNfcDialogSavedState, new WifiManagerWrapper(mWifiManager));
                }

                return mWifiToNfcDialog;
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case WIFI_DIALOG_ID:
                return MetricsEvent.DIALOG_WIFI_AP_EDIT;
            case WPS_PBC_DIALOG_ID:
                return MetricsEvent.DIALOG_WIFI_PBC;
            case WPS_PIN_DIALOG_ID:
                return MetricsEvent.DIALOG_WIFI_PIN;
            case WRITE_NFC_DIALOG_ID:
                return MetricsEvent.DIALOG_WIFI_WRITE_NFC;
            default:
                return 0;
        }
    }

    /**
     * Called to indicate the list of AccessPoints has been updated and
     * getAccessPoints should be called to get the latest information.
     */
    @Override
    public void onAccessPointsChanged() {
        Log.d(TAG, "onAccessPointsChanged (WifiTracker) callback initiated");
        updateAccessPointsDelayed();
    }

    /**
     * Updates access points from {@link WifiManager#getScanResults()}. Adds a delay to have
     * progress bar displayed before starting to modify APs.
     */
    private void updateAccessPointsDelayed() {
        // Safeguard from some delayed event handling
        if (getActivity() != null && !mIsRestricted && mWifiManager.isWifiEnabled()) {
            setProgressBarVisible(true);
            getView().postDelayed(mUpdateAccessPointsRunnable, 300 /* delay milliseconds */);
        }
    }

    /** Called when the state of Wifi has changed. */
    @Override
    public void onWifiStateChanged(int state) {
        if (mIsRestricted) {
            return;
        }

        final int wifiState = mWifiManager.getWifiState();
        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                conditionallyForceUpdateAPs();
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                removeConnectedAccessPointPreference();
                mAccessPointsPreferenceCategory.removeAll();
                addMessagePreference(R.string.wifi_starting);
                setProgressBarVisible(true);
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                removeConnectedAccessPointPreference();
                mAccessPointsPreferenceCategory.removeAll();
                addMessagePreference(R.string.wifi_stopping);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                setAdditionalSettingsSummaries();
                setProgressBarVisible(false);
                break;
        }
    }

    /**
     * Called when the connection state of wifi has changed and isConnected
     * should be called to get the updated state.
     */
    @Override
    public void onConnectedChanged() {
        updateAccessPointsDelayed();
        changeNextButtonState(mWifiTracker.isConnected());
    }

    /** Helper method to return whether an AccessPoint is disabled due to a wrong password */
    private static boolean isDisabledByWrongPassword(AccessPoint accessPoint) {
        WifiConfiguration config = accessPoint.getConfig();
        if (config == null) {
            return false;
        }
        WifiConfiguration.NetworkSelectionStatus networkStatus =
                config.getNetworkSelectionStatus();
        if (networkStatus == null || networkStatus.isNetworkEnabled()) {
            return false;
        }
        int reason = networkStatus.getNetworkSelectionDisableReason();
        return WifiConfiguration.NetworkSelectionStatus.DISABLED_BY_WRONG_PASSWORD == reason;
    }

    private void updateAccessPointPreferences() {
        // in case state has changed
        if (!mWifiManager.isWifiEnabled()) {
            return;
        }
        // AccessPoints are sorted by the WifiTracker
        final List<AccessPoint> accessPoints = mWifiTracker.getAccessPoints();
        if (WifiTracker.sVerboseLogging) {
            Log.i(TAG, "updateAccessPoints called for: " + accessPoints);
        }

        boolean hasAvailableAccessPoints = false;
        mAccessPointsPreferenceCategory.removePreference(mStatusMessagePreference);
        cacheRemoveAllPrefs(mAccessPointsPreferenceCategory);

        int index =
                configureConnectedAccessPointPreferenceCategory(accessPoints) ? 1 : 0;
        int numAccessPoints = accessPoints.size();
        for (; index < numAccessPoints; index++) {
            AccessPoint accessPoint = accessPoints.get(index);
            // Ignore access points that are out of range.
            if (accessPoint.isReachable()) {
                String key = AccessPointPreference.generatePreferenceKey(accessPoint);
                hasAvailableAccessPoints = true;
                LongPressAccessPointPreference pref =
                        (LongPressAccessPointPreference) getCachedPreference(key);
                if (pref != null) {
                    pref.setOrder(index);
                    continue;
                }
                LongPressAccessPointPreference preference =
                        createLongPressActionPointPreference(accessPoint);
                preference.setKey(key);
                preference.setOrder(index);
                if (mOpenSsid != null && mOpenSsid.equals(accessPoint.getSsidStr())
                        && accessPoint.getSecurity() != AccessPoint.SECURITY_NONE) {
                    if (!accessPoint.isSaved() || isDisabledByWrongPassword(accessPoint)) {
                        onPreferenceTreeClick(preference);
                        mOpenSsid = null;
                    }
                }
                mAccessPointsPreferenceCategory.addPreference(preference);
                accessPoint.setListener(WifiSettings.this);
                preference.refresh();
            }
        }
        removeCachedPrefs(mAccessPointsPreferenceCategory);
        mAddPreference.setOrder(index);
        mAccessPointsPreferenceCategory.addPreference(mAddPreference);
        setAdditionalSettingsSummaries();

        if (!hasAvailableAccessPoints) {
            setProgressBarVisible(true);
            Preference pref = new Preference(getPrefContext());
            pref.setSelectable(false);
            pref.setSummary(R.string.wifi_empty_list_wifi_on);
            pref.setOrder(index++);
            pref.setKey(PREF_KEY_EMPTY_WIFI_LIST);
            mAccessPointsPreferenceCategory.addPreference(pref);
        } else {
            // Continuing showing progress bar for an additional delay to overlap with animation
            getView().postDelayed(mHideProgressBarRunnable, 1700 /* delay millis */);
        }
    }

    @NonNull
    private LongPressAccessPointPreference createLongPressActionPointPreference(
            AccessPoint accessPoint) {
        return new LongPressAccessPointPreference(accessPoint, getPrefContext(), mUserBadgeCache,
                false, R.drawable.ic_wifi_signal_0, this);
    }

    /**
     * Configure the ConnectedAccessPointPreferenceCategory and return true if the Category was
     * shown.
     */
    private boolean configureConnectedAccessPointPreferenceCategory(
            List<AccessPoint> accessPoints) {
        if (accessPoints.size() == 0) {
            removeConnectedAccessPointPreference();
            return false;
        }

        AccessPoint connectedAp = accessPoints.get(0);
        if (!connectedAp.isActive()) {
            removeConnectedAccessPointPreference();
            return false;
        }

        // Is the preference category empty?
        if (mConnectedAccessPointPreferenceCategory.getPreferenceCount() == 0) {
            addConnectedAccessPointPreference(connectedAp);
            return true;
        }

        // Is the previous currently connected SSID different from the new one?
        AccessPointPreference preference = (AccessPointPreference)
            (mConnectedAccessPointPreferenceCategory.getPreference(0));
        // The AccessPoints need to be the same reference to ensure that updates are reflected
        // in the UI.
        if (preference.getAccessPoint() != connectedAp) {
            removeConnectedAccessPointPreference();
            addConnectedAccessPointPreference(connectedAp);
            return true;
        }

        // Else same AP is connected, simply refresh the connected access point preference
        // (first and only access point in this category).
        ((LongPressAccessPointPreference) mConnectedAccessPointPreferenceCategory.getPreference(0))
                .refresh();
        return true;
    }

    /**
     * Creates a Preference for the given {@link AccessPoint} and adds it to the
     * {@link #mConnectedAccessPointPreferenceCategory}.
     */
    private void addConnectedAccessPointPreference(AccessPoint connectedAp) {
        String key = connectedAp.getBssid();
        LongPressAccessPointPreference pref = (LongPressAccessPointPreference)
                getCachedPreference(key);
        if (pref == null) {
            pref = createLongPressActionPointPreference(connectedAp);
        }

        // Save the state of the current access point in the bundle so that we can restore it
        // in the Wifi Network Details Fragment
        pref.getAccessPoint().saveWifiState(pref.getExtras());
        pref.setFragment(WifiNetworkDetailsFragment.class.getName());
        pref.refresh();

        mConnectedAccessPointPreferenceCategory.addPreference(pref);
        mConnectedAccessPointPreferenceCategory.setVisible(true);
        if (mClickedConnect) {
            mClickedConnect = false;
            scrollToPreference(mConnectedAccessPointPreferenceCategory);
        }
    }

    /** Removes all preferences and hide the {@link #mConnectedAccessPointPreferenceCategory}. */
    private void removeConnectedAccessPointPreference() {
        mConnectedAccessPointPreferenceCategory.removeAll();
        mConnectedAccessPointPreferenceCategory.setVisible(false);
    }

    private void setAdditionalSettingsSummaries() {
        mAdditionalSettingsPreferenceCategory.addPreference(mConfigureWifiSettingsPreference);
        final int defaultWakeupAvailable = getResources().getInteger(
                com.android.internal.R.integer.config_wifi_wakeup_available);
        boolean wifiWakeupAvailable = Settings.Global.getInt(
                getContentResolver(), Settings.Global.WIFI_WAKEUP_AVAILABLE, defaultWakeupAvailable)
                == 1;
        if (wifiWakeupAvailable) {
            mConfigureWifiSettingsPreference.setSummary(getString(
                    isWifiWakeupEnabled()
                    ? R.string.wifi_configure_settings_preference_summary_wakeup_on
                    : R.string.wifi_configure_settings_preference_summary_wakeup_off));
        }
        int numSavedNetworks = mWifiTracker.getNumSavedNetworks();
        if (numSavedNetworks > 0) {
            mAdditionalSettingsPreferenceCategory.addPreference(mSavedNetworksPreference);
            mSavedNetworksPreference.setSummary(
                    getResources().getQuantityString(R.plurals.wifi_saved_access_points_summary,
                            numSavedNetworks, numSavedNetworks));
        } else {
            mAdditionalSettingsPreferenceCategory.removePreference(mSavedNetworksPreference);
        }
    }

    private boolean isWifiWakeupEnabled() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        ContentResolver contentResolver = getContentResolver();
        return Settings.Global.getInt(contentResolver,
                        Settings.Global.WIFI_WAKEUP_ENABLED, 0) == 1
                && Settings.Global.getInt(contentResolver,
                        Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1
                && Settings.Global.getInt(contentResolver,
                        Settings.Global.AIRPLANE_MODE_ON, 0) == 0
                && Settings.Global.getInt(contentResolver,
                        Settings.Global.NETWORK_RECOMMENDATIONS_ENABLED, 0) == 1
                && !powerManager.isPowerSaveMode();
    }

    private void setOffMessage() {
        final CharSequence title = getText(R.string.wifi_empty_list_wifi_off);
        // Don't use WifiManager.isScanAlwaysAvailable() to check the Wi-Fi scanning mode. Instead,
        // read the system settings directly. Because when the device is in Airplane mode, even if
        // Wi-Fi scanning mode is on, WifiManager.isScanAlwaysAvailable() still returns "off".
        final boolean wifiScanningMode = Settings.Global.getInt(getActivity().getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1;
        final CharSequence description = wifiScanningMode ? getText(R.string.wifi_scan_notify_text)
                : getText(R.string.wifi_scan_notify_text_scanning_off);
        final LinkifyUtils.OnClickListener clickListener = new LinkifyUtils.OnClickListener() {
            @Override
            public void onClick() {
                final SettingsActivity activity = (SettingsActivity) getActivity();
                activity.startPreferencePanel(WifiSettings.this,
                        ScanningSettings.class.getName(),
                        null, R.string.location_scanning_screen_title, null, null, 0);
            }
        };
        mStatusMessagePreference.setText(title, description, clickListener);
        removeConnectedAccessPointPreference();
        mAccessPointsPreferenceCategory.removeAll();
        mAccessPointsPreferenceCategory.addPreference(mStatusMessagePreference);
    }

    private void addMessagePreference(int messageId) {
        mStatusMessagePreference.setTitle(messageId);
        removeConnectedAccessPointPreference();
        mAccessPointsPreferenceCategory.removeAll();
        mAccessPointsPreferenceCategory.addPreference(mStatusMessagePreference);
    }

    protected void setProgressBarVisible(boolean visible) {
        if (mProgressHeader != null) {
            mProgressHeader.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
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
                connect(mSelectedAccessPoint.getConfig(), true /* isSavedNetwork */);
            }
        } else if (configController.getMode() == WifiConfigUiBase.MODE_MODIFY) {
            mWifiManager.save(config, mSaveListener);
        } else {
            mWifiManager.save(config, mSaveListener);
            if (mSelectedAccessPoint != null) { // Not an "Add network"
                connect(config, false /* isSavedNetwork */);
            }
        }

        mWifiTracker.resumeScanning();
    }

    /* package */ void forget() {
        mMetricsFeatureProvider.action(getActivity(), MetricsEvent.ACTION_WIFI_FORGET);
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
        } else if (mSelectedAccessPoint.getConfig().isPasspoint()) {
            mWifiManager.removePasspointConfiguration(mSelectedAccessPoint.getConfig().FQDN);
        } else {
            mWifiManager.forget(mSelectedAccessPoint.getConfig().networkId, mForgetListener);
        }

        mWifiTracker.resumeScanning();

        // We need to rename/replace "Next" button in wifi setup context.
        changeNextButtonState(false);
    }

    protected void connect(final WifiConfiguration config, boolean isSavedNetwork) {
        // Log subtype if configuration is a saved network.
        mMetricsFeatureProvider.action(getActivity(), MetricsEvent.ACTION_WIFI_CONNECT,
                isSavedNetwork);
        mWifiManager.connect(config, mConnectListener);
        mClickedConnect = true;
    }

    protected void connect(final int networkId, boolean isSavedNetwork) {
        // Log subtype if configuration is a saved network.
        mMetricsFeatureProvider.action(getActivity(), MetricsEvent.ACTION_WIFI_CONNECT,
                isSavedNetwork);
        mWifiManager.connect(networkId, mConnectListener);
    }

    /**
     * Called when "add network" button is pressed.
     */
    /* package */ void onAddNetworkPressed() {
        mMetricsFeatureProvider.action(getActivity(), MetricsEvent.ACTION_WIFI_ADD_NETWORK);
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
        Log.d(TAG, "onAccessPointChanged (singular) callback initiated");
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
                data.key = DATA_KEY_REFERENCE;
                result.add(data);

                // Add saved Wi-Fi access points
                final List<AccessPoint> accessPoints =
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
    public static boolean isEditabilityLockedDown(Context context, WifiConfiguration config) {
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

    private static class SummaryProvider
            implements SummaryLoader.SummaryProvider, OnSummaryChangeListener {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        @VisibleForTesting
        WifiSummaryUpdater mSummaryHelper;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mSummaryHelper = new WifiSummaryUpdater(mContext, this);
        }


        @Override
        public void setListening(boolean listening) {
            mSummaryHelper.register(listening);
        }

        @Override
        public void onSummaryChanged(String summary) {
            mSummaryLoader.setSummary(this, summary);
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

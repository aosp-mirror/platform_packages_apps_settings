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

import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.NETWORK_SELECTION_ENABLED;
import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.NetworkRequest;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.core.FeatureFlags;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.datausage.DataUsagePreference;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPoint.AccessPointListener;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiSavedConfigUtils;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settingslib.wifi.WifiTrackerFactory;

import java.util.List;

/**
 * Two types of UI are provided here.
 *
 * The first is for "usual Settings", appearing as any other Setup fragment.
 *
 * The second is for Setup Wizard, with a simplified interface that hides the action bar
 * and menus.
 *
 * Migrating from Wi-Fi SettingsLib to to WifiTrackerLib, this object will be removed in the near
 * future, please develop in {@link WifiSettings2}.
 */
@SearchIndexable
public class WifiSettings extends RestrictedSettingsFragment
        implements Indexable, WifiTracker.WifiListener, AccessPointListener,
        WifiDialog.WifiDialogListener, DialogInterface.OnDismissListener {

    private static final String TAG = "WifiSettings";

    private static final int MENU_ID_CONNECT = Menu.FIRST + 6;
    @VisibleForTesting
    static final int MENU_ID_FORGET = Menu.FIRST + 7;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 8;

    public static final int WIFI_DIALOG_ID = 1;

    @VisibleForTesting
    static final int ADD_NETWORK_REQUEST = 2;

    static final int CONFIG_NETWORK_REQUEST = 3;

    // Instance state keys
    private static final String SAVE_DIALOG_MODE = "dialog_mode";
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";

    private static final String PREF_KEY_EMPTY_WIFI_LIST = "wifi_empty_list";
    private static final String PREF_KEY_CONNECTED_ACCESS_POINTS = "connected_access_point";
    private static final String PREF_KEY_ACCESS_POINTS = "access_points";
    private static final String PREF_KEY_CONFIGURE_WIFI_SETTINGS = "configure_wifi_settings";
    private static final String PREF_KEY_SAVED_NETWORKS = "saved_networks";
    private static final String PREF_KEY_STATUS_MESSAGE = "wifi_status_message";
    @VisibleForTesting
    static final String PREF_KEY_DATA_USAGE = "wifi_data_usage";

    private static final int REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER = 0;

    private static boolean isVerboseLoggingEnabled() {
        return WifiTracker.sVerboseLogging || Log.isLoggable(TAG, Log.VERBOSE);
    }

    private final Runnable mUpdateAccessPointsRunnable = () -> {
        updateAccessPointPreferences();
    };
    private final Runnable mHideProgressBarRunnable = () -> {
        setProgressBarVisible(false);
    };

    @VisibleForTesting
    WifiManager mWifiManager;
    @VisibleForTesting
    ConnectivityManager mConnectivityManager;
    private WifiManager.ActionListener mConnectListener;
    private WifiManager.ActionListener mSaveListener;
    private WifiManager.ActionListener mForgetListener;
    @VisibleForTesting
    CaptivePortalNetworkCallback mCaptivePortalNetworkCallback;
    private Network mLastNetworkCaptivePortalAppStarted;

    /**
     * The state of {@link #isUiRestricted()} at {@link #onCreate(Bundle)}}. This is neccesary to
     * ensure that behavior is consistent if {@link #isUiRestricted()} changes. It could be changed
     * by the Test DPC tool in AFW mode.
     */
    private boolean mIsRestricted;

    private WifiEnabler mWifiEnabler;
    // An access point being edited is stored here.
    private AccessPoint mSelectedAccessPoint;

    private WifiDialog mDialog;

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

    @VisibleForTesting
    WifiTracker mWifiTracker;
    private String mOpenSsid;

    private AccessPointPreference.UserBadgeCache mUserBadgeCache;

    private PreferenceCategory mConnectedAccessPointPreferenceCategory;
    private PreferenceCategory mAccessPointsPreferenceCategory;
    @VisibleForTesting
    AddWifiNetworkPreference mAddWifiNetworkPreference;
    @VisibleForTesting
    Preference mConfigureWifiSettingsPreference;
    @VisibleForTesting
    Preference mSavedNetworksPreference;
    @VisibleForTesting
    DataUsagePreference mDataUsagePreference;
    private LinkablePreference mStatusMessagePreference;

    /**
     * Tracks whether the user initiated a connection via clicking in order to autoscroll to the
     * network once connected.
     */
    private boolean mClickedConnect;
    @ConnectSource int mConnectSource = CONNECT_SOURCE_UNSPECIFIED;

    private static final int CONNECT_SOURCE_UNSPECIFIED = 0;
    private static final int CONNECT_SOURCE_NETWORK_MENU_ITEM_CLICK = 1;
    private static final int CONNECT_SOURCE_NETWORK_LIST_ITEM_CLICK = 2;

    @IntDef({CONNECT_SOURCE_UNSPECIFIED, CONNECT_SOURCE_NETWORK_MENU_ITEM_CLICK,
        CONNECT_SOURCE_NETWORK_LIST_ITEM_CLICK})
    private @interface ConnectSource {}

    /* End of "used in Wifi Setup context" */

    public WifiSettings() {
        super(DISALLOW_CONFIG_WIFI);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Activity activity = getActivity();
        if (activity != null) {
            mProgressHeader = setPinnedHeaderView(R.layout.progress_header)
                    .findViewById(R.id.progress_bar_animation);
            setProgressBarVisible(false);
        }
        ((SettingsActivity) activity).getSwitchBar().setSwitchBarText(
                R.string.wifi_settings_master_switch_title,
                R.string.wifi_settings_master_switch_title);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (FeatureFlagUtils.isEnabled(getContext(), FeatureFlagUtils.SETTINGS_WIFITRACKER2)) {
            final Intent intent = new Intent("android.settings.WIFI_SETTINGS2");
            final Bundle extras = getActivity().getIntent().getExtras();
            if (extras != null) {
                intent.putExtras(extras);
            }
            getContext().startActivity(intent);
            finish();
            return;
        }

        // TODO(b/37429702): Add animations and preference comparator back after initial screen is
        // loaded (ODR).
        setAnimationAllowed(false);

        addPreferences();

        mIsRestricted = isUiRestricted();
    }

    private void addPreferences() {
        addPreferencesFromResource(R.xml.wifi_settings);

        mConnectedAccessPointPreferenceCategory =
                (PreferenceCategory) findPreference(PREF_KEY_CONNECTED_ACCESS_POINTS);
        mAccessPointsPreferenceCategory =
                (PreferenceCategory) findPreference(PREF_KEY_ACCESS_POINTS);
        mConfigureWifiSettingsPreference = findPreference(PREF_KEY_CONFIGURE_WIFI_SETTINGS);
        mSavedNetworksPreference = findPreference(PREF_KEY_SAVED_NETWORKS);
        mAddWifiNetworkPreference = new AddWifiNetworkPreference(getPrefContext());
        mStatusMessagePreference = (LinkablePreference) findPreference(PREF_KEY_STATUS_MESSAGE);
        mUserBadgeCache = new AccessPointPreference.UserBadgeCache(getPackageManager());
        mDataUsagePreference = findPreference(PREF_KEY_DATA_USAGE);
        mDataUsagePreference.setVisible(DataUsageUtils.hasWifiRadio(getContext()));
        mDataUsagePreference.setTemplate(NetworkTemplate.buildTemplateWifiWildcard(),
                0 /*subId*/,
                null /*service*/);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWifiTracker = WifiTrackerFactory.create(
                getActivity(), this, getSettingsLifecycle(), true, true);
        mWifiManager = mWifiTracker.getManager();

        final Activity activity = getActivity();
        if (activity != null) {
            mConnectivityManager = getActivity().getSystemService(ConnectivityManager.class);
        }

        mConnectListener = new WifiConnectListener(getActivity());

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
        getView().removeCallbacks(mUpdateAccessPointsRunnable);
        getView().removeCallbacks(mHideProgressBarRunnable);
        unregisterCaptivePortalNetworkCallback();
        super.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_NETWORK_REQUEST) {
            handleAddNetworkRequest(resultCode, data);
            return;
        } else if (requestCode == REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER) {
            if (resultCode == Activity.RESULT_OK) {
                if (mDialog != null) {
                    mDialog.dismiss();
                }
                mWifiTracker.resumeScanning();
            }
            return;
        } else if (requestCode == CONFIG_NETWORK_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                handleConfigNetworkSubmitEvent(data);
            }
            return;
        }

        final boolean formerlyRestricted = mIsRestricted;
        mIsRestricted = isUiRestricted();
        if (formerlyRestricted && !mIsRestricted
                && getPreferenceScreen().getPreferenceCount() == 0) {
            // De-restrict the ui
            addPreferences();
        }
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        final RecyclerView.Adapter adapter = super.onCreateAdapter(preferenceScreen);
        adapter.setHasStableIds(true);
        return adapter;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // If dialog has been shown, save its state.
        if (mDialog != null) {
            outState.putInt(SAVE_DIALOG_MODE, mDialogMode);
            if (mDlgAccessPoint != null) {
                mAccessPointSavedState = new Bundle();
                mDlgAccessPoint.saveWifiState(mAccessPointSavedState);
                outState.putBundle(SAVE_DIALOG_ACCESS_POINT_STATE, mAccessPointSavedState);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
        Preference preference = (Preference) view.getTag();

        if (preference instanceof LongPressAccessPointPreference) {
            mSelectedAccessPoint =
                    ((LongPressAccessPointPreference) preference).getAccessPoint();
            menu.setHeaderTitle(mSelectedAccessPoint.getTitle());
            if (mSelectedAccessPoint.isConnectable()) {
                menu.add(Menu.NONE, MENU_ID_CONNECT, 0 /* order */, R.string.wifi_connect);
            }

            WifiConfiguration config = mSelectedAccessPoint.getConfig();
            // Some configs are ineditable
            if (WifiUtils.isNetworkLockedDown(getActivity(), config)) {
                return;
            }

            // "forget" for normal saved network. And "disconnect" for ephemeral network because it
            // could only be disconnected and be put in blacklists so it won't be used again.
            if (mSelectedAccessPoint.isSaved() || mSelectedAccessPoint.isEphemeral()) {
                final int stringId = mSelectedAccessPoint.isEphemeral() ?
                        R.string.wifi_disconnect_button_text : R.string.forget;
                menu.add(Menu.NONE, MENU_ID_FORGET, 0 /* order */, stringId);
            }

            if (mSelectedAccessPoint.isSaved() && !mSelectedAccessPoint.isActive()) {
                menu.add(Menu.NONE, MENU_ID_MODIFY, 0 /* order */, R.string.wifi_modify);
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
                    connect(mSelectedAccessPoint.getConfig(), isSavedNetwork,
                            CONNECT_SOURCE_NETWORK_MENU_ITEM_CLICK);
                } else if ((mSelectedAccessPoint.getSecurity() == AccessPoint.SECURITY_NONE) ||
                        (mSelectedAccessPoint.getSecurity() == AccessPoint.SECURITY_OWE)) {
                    /** Bypass dialog for unsecured networks */
                    mSelectedAccessPoint.generateOpenNetworkConfig();
                    connect(mSelectedAccessPoint.getConfig(), isSavedNetwork,
                            CONNECT_SOURCE_NETWORK_MENU_ITEM_CLICK);
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
            switch (WifiUtils.getConnectingType(mSelectedAccessPoint)) {
                case WifiUtils.CONNECT_TYPE_OSU_PROVISION:
                    mSelectedAccessPoint.startOsuProvisioning(mConnectListener);
                    mClickedConnect = true;
                    break;

                case WifiUtils.CONNECT_TYPE_OPEN_NETWORK:
                    mSelectedAccessPoint.generateOpenNetworkConfig();
                    connect(mSelectedAccessPoint.getConfig(),
                            mSelectedAccessPoint.isSaved(),
                            CONNECT_SOURCE_NETWORK_LIST_ITEM_CLICK);
                    break;

                case WifiUtils.CONNECT_TYPE_SAVED_NETWORK:
                    connect(mSelectedAccessPoint.getConfig(),
                            true /* isSavedNetwork */,
                            CONNECT_SOURCE_NETWORK_LIST_ITEM_CLICK);
                    break;

                default:
                    final Bundle bundle = ((LongPressAccessPointPreference) preference).getExtras();
                    mSelectedAccessPoint.saveWifiState(bundle);
                    launchConfigNewNetworkFragment(mSelectedAccessPoint,
                            WifiConfigUiBase.MODE_CONNECT, bundle);
                    break;
            }
        } else if (preference == mAddWifiNetworkPreference) {
            onAddNetworkPressed();
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private void showDialog(AccessPoint accessPoint, int dialogMode) {
        if (accessPoint != null) {
            WifiConfiguration config = accessPoint.getConfig();
            if (WifiUtils.isNetworkLockedDown(getActivity(), config) && accessPoint.isActive()) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(),
                        RestrictedLockUtilsInternal.getDeviceOwner(getActivity()));
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
                // modify network
                if (mDlgAccessPoint == null && mAccessPointSavedState != null) {
                    // restore AP from save state
                    mDlgAccessPoint = new AccessPoint(getActivity(), mAccessPointSavedState);
                    // Reset the saved access point data
                    mAccessPointSavedState = null;
                }
                mDialog = WifiDialog
                        .createModal(getActivity(), this, mDlgAccessPoint, mDialogMode);
                mSelectedAccessPoint = mDlgAccessPoint;
                return mDialog;
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onDialogShowing() {
        super.onDialogShowing();
        setOnDismissListener(this);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // We don't keep any dialog object when dialog was dismissed.
        mDialog = null;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case WIFI_DIALOG_ID:
                return SettingsEnums.DIALOG_WIFI_AP_EDIT;
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
            final View view = getView();
            final Handler handler = view.getHandler();
            if (handler != null && handler.hasCallbacks(mUpdateAccessPointsRunnable)) {
                return;
            }
            setProgressBarVisible(true);
            view.postDelayed(mUpdateAccessPointsRunnable, 300 /* delay milliseconds */);
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
                updateAccessPointPreferences();
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                removeConnectedAccessPointPreference();
                removeAccessPointPreference();
                addMessagePreference(R.string.wifi_starting);
                setProgressBarVisible(true);
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                removeConnectedAccessPointPreference();
                removeAccessPointPreference();
                addMessagePreference(R.string.wifi_stopping);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                setAdditionalSettingsSummaries();
                setProgressBarVisible(false);
                mConnectSource = CONNECT_SOURCE_UNSPECIFIED;
                mClickedConnect = false;
                break;
        }
    }

    /**
     * Called when the connection state of wifi has changed.
     */
    @Override
    public void onConnectedChanged() {
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
        if (networkStatus == null
                || networkStatus.getNetworkSelectionStatus() == NETWORK_SELECTION_ENABLED) {
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
        if (isVerboseLoggingEnabled()) {
            Log.i(TAG, "updateAccessPoints called for: " + accessPoints);
        }

        boolean hasAvailableAccessPoints = false;
        mStatusMessagePreference.setVisible(false);
        mConnectedAccessPointPreferenceCategory.setVisible(true);
        mAccessPointsPreferenceCategory.setVisible(true);

        cacheRemoveAllPrefs(mAccessPointsPreferenceCategory);

        int index =
                configureConnectedAccessPointPreferenceCategory(accessPoints) ? 1 : 0;
        int numAccessPoints = accessPoints.size();
        for (; index < numAccessPoints; index++) {
            AccessPoint accessPoint = accessPoints.get(index);
            // Ignore access points that are out of range.
            if (accessPoint.isReachable()) {
                String key = accessPoint.getKey();
                hasAvailableAccessPoints = true;
                LongPressAccessPointPreference pref =
                        (LongPressAccessPointPreference) getCachedPreference(key);
                if (pref != null) {
                    pref.setOrder(index);
                    continue;
                }
                LongPressAccessPointPreference preference =
                        createLongPressAccessPointPreference(accessPoint);
                preference.setKey(key);
                preference.setOrder(index);
                if (mOpenSsid != null && mOpenSsid.equals(accessPoint.getSsidStr())
                        && (accessPoint.getSecurity() != AccessPoint.SECURITY_NONE &&
                        accessPoint.getSecurity() != AccessPoint.SECURITY_OWE)) {
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
        mAddWifiNetworkPreference.setOrder(index);
        mAccessPointsPreferenceCategory.addPreference(mAddWifiNetworkPreference);
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
    private LongPressAccessPointPreference createLongPressAccessPointPreference(
            AccessPoint accessPoint) {
        return new LongPressAccessPointPreference(accessPoint, getPrefContext(), mUserBadgeCache,
                false /* forSavedNetworks */, R.drawable.ic_wifi_signal_0, this);
    }

    @NonNull
    @VisibleForTesting
    ConnectedAccessPointPreference createConnectedAccessPointPreference(
            AccessPoint accessPoint, Context context) {
        return new ConnectedAccessPointPreference(accessPoint, context, mUserBadgeCache,
                R.drawable.ic_wifi_signal_0, false /* forSavedNetworks */, this);
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
        ConnectedAccessPointPreference preference =
                (ConnectedAccessPointPreference)
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
        preference.refresh();
        // Update any potential changes to the connected network and ensure that the callback is
        // registered after an onStop lifecycle event.
        registerCaptivePortalNetworkCallback(getCurrentWifiNetwork(), preference);
        return true;
    }

    /**
     * Creates a Preference for the given {@link AccessPoint} and adds it to the
     * {@link #mConnectedAccessPointPreferenceCategory}.
     */
    private void addConnectedAccessPointPreference(AccessPoint connectedAp) {
        final ConnectedAccessPointPreference pref =
                createConnectedAccessPointPreference(connectedAp, getPrefContext());
        registerCaptivePortalNetworkCallback(getCurrentWifiNetwork(), pref);

        // Launch details page or captive portal on click.
        pref.setOnPreferenceClickListener(
                preference -> {
                    pref.getAccessPoint().saveWifiState(pref.getExtras());
                    if (mCaptivePortalNetworkCallback != null
                            && mCaptivePortalNetworkCallback.isCaptivePortal()) {
                        startCaptivePortalApp(
                                mCaptivePortalNetworkCallback.getNetwork());
                    } else {
                        launchNetworkDetailsFragment(pref);
                    }
                    return true;
                });

        pref.setOnGearClickListener(
                preference -> {
                    pref.getAccessPoint().saveWifiState(pref.getExtras());
                    launchNetworkDetailsFragment(pref);
                });

        pref.refresh();

        mConnectedAccessPointPreferenceCategory.addPreference(pref);
        mConnectedAccessPointPreferenceCategory.setVisible(true);
        if (mClickedConnect) {
            mClickedConnect = false;
            scrollToPreference(mConnectedAccessPointPreferenceCategory);
        }
    }

    private void registerCaptivePortalNetworkCallback(
            Network wifiNetwork, ConnectedAccessPointPreference pref) {
        if (wifiNetwork == null || pref == null) {
            Log.w(TAG, "Network or Preference were null when registering callback.");
            return;
        }

        if (mCaptivePortalNetworkCallback != null
                && mCaptivePortalNetworkCallback.isSameNetworkAndPreference(wifiNetwork, pref)) {
            return;
        }

        unregisterCaptivePortalNetworkCallback();

        mCaptivePortalNetworkCallback = new CaptivePortalNetworkCallback(wifiNetwork, pref) {
            @Override
            public void onCaptivePortalCapabilityChanged() {
                checkStartCaptivePortalApp();
            }
        };
        mConnectivityManager.registerNetworkCallback(
                new NetworkRequest.Builder()
                        .clearCapabilities()
                        .addTransportType(TRANSPORT_WIFI)
                        .build(),
                mCaptivePortalNetworkCallback,
                new Handler(Looper.getMainLooper()));
    }

    private void unregisterCaptivePortalNetworkCallback() {
        if (mCaptivePortalNetworkCallback != null) {
            try {
                mConnectivityManager.unregisterNetworkCallback(mCaptivePortalNetworkCallback);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unregistering CaptivePortalNetworkCallback failed.", e);
            }
            mCaptivePortalNetworkCallback = null;
        }
    }

    private void launchAddNetworkFragment() {
        new SubSettingLauncher(getContext())
                .setTitleRes(R.string.wifi_add_network)
                .setDestination(AddNetworkFragment.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(this, ADD_NETWORK_REQUEST)
                .launch();
    }

    private void launchNetworkDetailsFragment(ConnectedAccessPointPreference pref) {
        final AccessPoint accessPoint = pref.getAccessPoint();
        final Context context = getContext();
        final CharSequence title =
                FeatureFlagUtils.isEnabled(context, FeatureFlags.WIFI_DETAILS_DATAUSAGE_HEADER)
                        ? accessPoint.getTitle()
                        : context.getText(R.string.pref_title_network_details);

        new SubSettingLauncher(getContext())
                .setTitleText(title)
                .setDestination(WifiNetworkDetailsFragment.class.getName())
                .setArguments(pref.getExtras())
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    private Network getCurrentWifiNetwork() {
        return mWifiManager != null ? mWifiManager.getCurrentNetwork() : null;
    }

    /** Removes all preferences and hide the {@link #mConnectedAccessPointPreferenceCategory}. */
    private void removeConnectedAccessPointPreference() {
        mConnectedAccessPointPreferenceCategory.removeAll();
        mConnectedAccessPointPreferenceCategory.setVisible(false);
        unregisterCaptivePortalNetworkCallback();
    }

    private void removeAccessPointPreference() {
        mAccessPointsPreferenceCategory.removeAll();
        mAccessPointsPreferenceCategory.setVisible(false);
    }

    @VisibleForTesting
    void setAdditionalSettingsSummaries() {
        mConfigureWifiSettingsPreference.setSummary(getString(
                isWifiWakeupEnabled()
                        ? R.string.wifi_configure_settings_preference_summary_wakeup_on
                        : R.string.wifi_configure_settings_preference_summary_wakeup_off));

        final List<AccessPoint> savedNetworks =
                WifiSavedConfigUtils.getAllConfigs(getContext(), mWifiManager);
        final int numSavedNetworks = (savedNetworks != null) ? savedNetworks.size() : 0;
        mSavedNetworksPreference.setVisible(numSavedNetworks > 0);
        if (numSavedNetworks > 0) {
            mSavedNetworksPreference.setSummary(
                    getSavedNetworkSettingsSummaryText(savedNetworks, numSavedNetworks));
        }
    }

    private String getSavedNetworkSettingsSummaryText(
            List<AccessPoint> savedNetworks, int numSavedNetworks) {
        int numSavedPasspointNetworks = 0;
        for (AccessPoint savedNetwork : savedNetworks) {
            if (savedNetwork.isPasspointConfig() || savedNetwork.isPasspoint()) {
                numSavedPasspointNetworks++;
            }
        }
        final int numSavedNormalNetworks = numSavedNetworks - numSavedPasspointNetworks;

        if (numSavedNetworks == numSavedNormalNetworks) {
            return getResources().getQuantityString(R.plurals.wifi_saved_access_points_summary,
                    numSavedNormalNetworks, numSavedNormalNetworks);
        } else if (numSavedNetworks == numSavedPasspointNetworks) {
            return getResources().getQuantityString(
                    R.plurals.wifi_saved_passpoint_access_points_summary,
                    numSavedPasspointNetworks, numSavedPasspointNetworks);
        } else {
            return getResources().getQuantityString(R.plurals.wifi_saved_all_access_points_summary,
                    numSavedNetworks, numSavedNetworks);
        }
    }

    private boolean isWifiWakeupEnabled() {
        final Context context = getContext();
        final PowerManager powerManager = context.getSystemService(PowerManager.class);
        final ContentResolver contentResolver = context.getContentResolver();
        return mWifiManager.isAutoWakeupEnabled()
                && mWifiManager.isScanAlwaysAvailable()
                && Settings.Global.getInt(contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0) == 0
                && !powerManager.isPowerSaveMode();
    }

    private void setOffMessage() {
        final CharSequence title = getText(R.string.wifi_empty_list_wifi_off);
        // Don't use WifiManager.isScanAlwaysAvailable() to check the Wi-Fi scanning mode. Instead,
        // read the system settings directly. Because when the device is in Airplane mode, even if
        // Wi-Fi scanning mode is on, WifiManager.isScanAlwaysAvailable() still returns "off".
        // TODO(b/149421497): Fix this?
        final boolean wifiScanningMode = mWifiManager.isScanAlwaysAvailable();
        final CharSequence description = wifiScanningMode ? getText(R.string.wifi_scan_notify_text)
                : getText(R.string.wifi_scan_notify_text_scanning_off);
        final LinkifyUtils.OnClickListener clickListener =
                () -> new SubSettingLauncher(getContext())
                        .setDestination(ScanningSettings.class.getName())
                        .setTitleRes(R.string.location_scanning_screen_title)
                        .setSourceMetricsCategory(getMetricsCategory())
                        .launch();
        mStatusMessagePreference.setText(title, description, clickListener);
        removeConnectedAccessPointPreference();
        removeAccessPointPreference();
        mStatusMessagePreference.setVisible(true);
    }

    private void addMessagePreference(int messageId) {
        mStatusMessagePreference.setTitle(messageId);
        mStatusMessagePreference.setVisible(true);

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

    @Override
    public void onScan(WifiDialog dialog, String ssid) {
        // Launch QR code scanner to join a network.
        startActivityForResult(WifiDppUtils.getEnrolleeQrCodeScannerIntent(ssid),
                REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER);
    }

    /* package */ void submit(WifiConfigController configController) {

        final WifiConfiguration config = configController.getConfig();

        if (config == null) {
            if (mSelectedAccessPoint != null
                    && mSelectedAccessPoint.isSaved()) {
                connect(mSelectedAccessPoint.getConfig(),
                        true /* isSavedNetwork */,
                        CONNECT_SOURCE_UNSPECIFIED);
            }
        } else if (configController.getMode() == WifiConfigUiBase.MODE_MODIFY) {
            mWifiManager.save(config, mSaveListener);
        } else {
            mWifiManager.save(config, mSaveListener);
            if (mSelectedAccessPoint != null) { // Not an "Add network"
                connect(config, false /* isSavedNetwork */,
                        CONNECT_SOURCE_UNSPECIFIED);
            }
        }

        mWifiTracker.resumeScanning();
    }

    /* package */ void forget() {
        mMetricsFeatureProvider.action(getActivity(), SettingsEnums.ACTION_WIFI_FORGET);
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
            try {
                mWifiManager.removePasspointConfiguration(mSelectedAccessPoint.getConfig().FQDN);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to remove Passpoint configuration with error: " + e);
                return;
            }
        } else {
            mWifiManager.forget(mSelectedAccessPoint.getConfig().networkId, mForgetListener);
        }

        mWifiTracker.resumeScanning();

        // We need to rename/replace "Next" button in wifi setup context.
        changeNextButtonState(false);
    }

    protected void connect(final WifiConfiguration config,
            boolean isSavedNetwork, @ConnectSource int connectSource) {
        // Log subtype if configuration is a saved network.
        mMetricsFeatureProvider.action(getContext(), SettingsEnums.ACTION_WIFI_CONNECT,
                isSavedNetwork);
        mConnectSource = connectSource;
        mWifiManager.connect(config, mConnectListener);
        mClickedConnect = true;
    }

    @VisibleForTesting
    void handleAddNetworkRequest(int result, Intent data) {
        if (result == Activity.RESULT_OK) {
            handleAddNetworkSubmitEvent(data);
        }
        mWifiTracker.resumeScanning();
    }

    private void handleAddNetworkSubmitEvent(Intent data) {
        final WifiConfiguration wifiConfiguration = data.getParcelableExtra(
                AddNetworkFragment.WIFI_CONFIG_KEY);
        if (wifiConfiguration != null) {
            mWifiManager.save(wifiConfiguration, mSaveListener);
        }
    }

    /**
     * Called when "add network" button is pressed.
     */
    private void onAddNetworkPressed() {
        // No exact access point is selected.
        mSelectedAccessPoint = null;
        launchAddNetworkFragment();
    }

    @Override
    public int getHelpResource() {
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
                        ((AccessPointPreference) tag).refresh();
                    }
                }
            });
        }
    }

    @Override
    public void onLevelChanged(AccessPoint accessPoint) {
        ((AccessPointPreference) accessPoint.getTag()).onLevelChanged();
    }

    private void handleConfigNetworkSubmitEvent(Intent data) {
        final WifiConfiguration wifiConfiguration = data.getParcelableExtra(
                ConfigureAccessPointFragment.NETWORK_CONFIG_KEY);
        if (wifiConfiguration != null) {
            mWifiManager.save(wifiConfiguration, mSaveListener);

            if (mSelectedAccessPoint != null) {
                connect(wifiConfiguration, false /*isSavedNetwork*/,
                        CONNECT_SOURCE_UNSPECIFIED);
            }
            mWifiTracker.resumeScanning();
        }
    }

    private void launchConfigNewNetworkFragment(AccessPoint accessPoint, int dialogMode,
            Bundle bundleForArguments) {
        mDialogMode = dialogMode;
        final CharSequence title = accessPoint.getTitle();
        new SubSettingLauncher(getContext())
                .setTitleText(title)
                .setDestination(ConfigureAccessPointFragment.class.getName())
                .setArguments(bundleForArguments)
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(this, CONFIG_NETWORK_REQUEST)
                .launch();
    }

    /**
     * Starts the captive portal for current network if it's been clicked from the available
     * networks (or contextual menu). We only do it *once* for a picked network, to avoid connecting
     * again on bg/fg or if user dismisses Captive Portal before connecting (otherwise, coming back
     * to this screen while connected to the same network but not signed in would open CP again).
     */
    private void checkStartCaptivePortalApp() {
        Network currentNetwork = getCurrentWifiNetwork();
        if (mCaptivePortalNetworkCallback == null || currentNetwork == null
                || !currentNetwork.equals(mCaptivePortalNetworkCallback.getNetwork())
                || !mCaptivePortalNetworkCallback.isCaptivePortal()) {
            return;
        }

        if (mConnectSource != CONNECT_SOURCE_NETWORK_LIST_ITEM_CLICK
                && mConnectSource != CONNECT_SOURCE_NETWORK_MENU_ITEM_CLICK) {
            return;
        }

        if (mLastNetworkCaptivePortalAppStarted != null
                && mLastNetworkCaptivePortalAppStarted.equals(currentNetwork)) {
            // We already auto-opened CP for same network
            return;
        }

        startCaptivePortalApp(currentNetwork);
    }

    private void startCaptivePortalApp(Network network) {
        if (mConnectivityManager == null || network == null) {
            return;
        }
        mLastNetworkCaptivePortalAppStarted = network;
        mConnectivityManager.startCaptivePortalApp(network);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.wifi_settings) {
                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);

                    final WifiManager wifiManager = context.getSystemService(WifiManager.class);
                    if (WifiSavedConfigUtils.getAllConfigsCount(context, wifiManager) == 0) {
                        keys.add(PREF_KEY_SAVED_NETWORKS);
                    }

                    if (!DataUsageUtils.hasWifiRadio(context)) {
                        keys.add(PREF_KEY_DATA_USAGE);
                    }
                    return keys;
                }
            };
}

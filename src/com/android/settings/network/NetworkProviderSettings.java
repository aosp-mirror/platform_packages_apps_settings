/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import static android.net.wifi.WifiConfiguration.NetworkSelectionStatus.NETWORK_SELECTION_ENABLED;
import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.AirplaneModeEnabler;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.datausage.DataUsagePreference;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.location.WifiScanningFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.wifi.AddNetworkFragment;
import com.android.settings.wifi.AddWifiNetworkPreference;
import com.android.settings.wifi.ConfigureWifiEntryFragment;
import com.android.settings.wifi.ConnectedWifiEntryPreference;
import com.android.settings.wifi.LongPressWifiEntryPreference;
import com.android.settings.wifi.WifiConfigUiBase2;
import com.android.settings.wifi.WifiDialog2;
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.settings.wifi.WifiUtils;
import com.android.settings.wifi.details.WifiNetworkDetailsFragment;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.wifi.WifiEnterpriseRestrictionUtils;
import com.android.settingslib.wifi.WifiSavedConfigUtils;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiEntry.ConnectCallback;
import com.android.wifitrackerlib.WifiPickerTracker;

import java.util.List;
import java.util.Optional;

/**
 * UI for Mobile network and Wi-Fi network settings.
 *
 * TODO(b/167474581): Define the intent android.settings.NETWORK_PROVIDER_SETTINGS in Settings.java.
 */
@SearchIndexable
public class NetworkProviderSettings extends RestrictedSettingsFragment
        implements Indexable, WifiPickerTracker.WifiPickerTrackerCallback,
        WifiDialog2.WifiDialog2Listener, DialogInterface.OnDismissListener,
        AirplaneModeEnabler.OnAirplaneModeChangedListener, InternetUpdater.InternetChangeListener {

    public static final String ACTION_NETWORK_PROVIDER_SETTINGS =
            "android.settings.NETWORK_PROVIDER_SETTINGS";

    private static final String TAG = "NetworkProviderSettings";
    // IDs of context menu
    static final int MENU_ID_CONNECT = Menu.FIRST + 1;
    @VisibleForTesting
    static final int MENU_ID_DISCONNECT = Menu.FIRST + 2;
    @VisibleForTesting
    static final int MENU_ID_FORGET = Menu.FIRST + 3;
    static final int MENU_ID_MODIFY = Menu.FIRST + 4;
    static final int MENU_FIX_CONNECTIVITY = Menu.FIRST + 5;
    static final int MENU_ID_SHARE = Menu.FIRST + 6;

    @VisibleForTesting
    static final int ADD_NETWORK_REQUEST = 2;
    static final int CONFIG_NETWORK_REQUEST = 3;
    static final int MANAGE_SUBSCRIPTION = 4;

    private static final String PREF_KEY_AIRPLANE_MODE_MSG = "airplane_mode_message";
    private static final String PREF_KEY_EMPTY_WIFI_LIST = "wifi_empty_list";
    @VisibleForTesting
    static final String PREF_KEY_WIFI_TOGGLE = "main_toggle_wifi";
    // TODO(b/70983952): Rename these to use WifiEntry instead of AccessPoint.
    @VisibleForTesting
    static final String PREF_KEY_CONNECTED_ACCESS_POINTS = "connected_access_point";
    @VisibleForTesting
    static final String PREF_KEY_FIRST_ACCESS_POINTS = "first_access_points";
    private static final String PREF_KEY_ACCESS_POINTS = "access_points";
    @VisibleForTesting
    static final String PREF_KEY_ADD_WIFI_NETWORK = "add_wifi_network";
    private static final String PREF_KEY_CONFIGURE_NETWORK_SETTINGS = "configure_network_settings";
    private static final String PREF_KEY_SAVED_NETWORKS = "saved_networks";
    @VisibleForTesting
    static final String PREF_KEY_DATA_USAGE = "non_carrier_data_usage";
    private static final String PREF_KEY_RESET_INTERNET = "resetting_your_internet";
    private static final String PREF_KEY_WIFI_STATUS_MESSAGE = "wifi_status_message_footer";

    private static final int REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER = 0;

    public static final int WIFI_DIALOG_ID = 1;

    // Instance state keys
    private static final String SAVE_DIALOG_MODE = "dialog_mode";
    private static final String SAVE_DIALOG_WIFIENTRY_KEY = "wifi_ap_key";

    // Cache at onCreateContextMenu and use at onContextItemSelected. Don't use it in other methods.
    private WifiEntry mSelectedWifiEntry;

    // Save the dialog details
    private int mDialogMode;
    private String mDialogWifiEntryKey;
    private WifiEntry mDialogWifiEntry;

    // This boolean extra specifies whether to enable the Next button when connected. Used by
    // account creation outside of setup wizard.
    private static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";

    // Enable the Next button when a Wi-Fi network is connected.
    private boolean mEnableNextOnConnection;

    // This string extra specifies a network to open the connect dialog on, so the user can enter
    // network credentials.  This is used by quick settings for secured networks, among other
    // things.
    private static final String EXTRA_START_CONNECT_SSID = "wifi_start_connect_ssid";
    private String mOpenSsid;

    private boolean mIsViewLoading;
    @VisibleForTesting
    final Runnable mRemoveLoadingRunnable = () -> {
        if (mIsViewLoading) {
            setLoading(false, false);
            mIsViewLoading = false;
        }
    };

    @VisibleForTesting
    final Runnable mUpdateWifiEntryPreferencesRunnable = () -> {
        updateWifiEntryPreferences();
        View view = getView();
        if (view != null) {
            view.postDelayed(mRemoveLoadingRunnable, 10);
        }
    };
    @VisibleForTesting
    final Runnable mHideProgressBarRunnable = () -> {
        setProgressBarVisible(false);
    };

    protected WifiManager mWifiManager;
    private WifiManager.ActionListener mSaveListener;

    protected InternetResetHelper mInternetResetHelper;

    /**
     * The state of {@link #isUiRestricted()} at {@link #onCreate(Bundle)}}. This is necessary to
     * ensure that behavior is consistent if {@link #isUiRestricted()} changes. It could be changed
     * by the Test DPC tool in AFW mode.
     */
    protected boolean mIsRestricted;
    @VisibleForTesting
    boolean mIsAdmin = true;
    @VisibleForTesting
    boolean mIsGuest = false;

    @VisibleForTesting
    AirplaneModeEnabler mAirplaneModeEnabler;
    @VisibleForTesting
    WifiPickerTracker mWifiPickerTracker;
    private WifiPickerTrackerHelper mWifiPickerTrackerHelper;
    @VisibleForTesting
    InternetUpdater mInternetUpdater;

    private WifiDialog2 mDialog;

    @VisibleForTesting
    PreferenceCategory mConnectedWifiEntryPreferenceCategory;
    @VisibleForTesting
    PreferenceCategory mFirstWifiEntryPreferenceCategory;
    @VisibleForTesting
    PreferenceCategory mWifiEntryPreferenceCategory;
    @VisibleForTesting
    AddWifiNetworkPreference mAddWifiNetworkPreference;
    private WifiSwitchPreferenceController mWifiSwitchPreferenceController;
    @VisibleForTesting
    Preference mConfigureWifiSettingsPreference;
    @VisibleForTesting
    Preference mSavedNetworksPreference;
    @VisibleForTesting
    DataUsagePreference mDataUsagePreference;
    @VisibleForTesting
    Preference mAirplaneModeMsgPreference;
    @VisibleForTesting
    LayoutPreference mResetInternetPreference;
    @VisibleForTesting
    ConnectedEthernetNetworkController mConnectedEthernetNetworkController;
    @VisibleForTesting
    FooterPreference mWifiStatusMessagePreference;
    @VisibleForTesting
    MenuProvider mMenuProvider;

    /**
     * Mobile networks list for provider model
     */
    private static final String PREF_KEY_PROVIDER_MOBILE_NETWORK = "provider_model_mobile_network";
    private NetworkMobileProviderController mNetworkMobileProviderController;

    /**
     * Tracks whether the user initiated a connection via clicking in order to autoscroll to the
     * network once connected.
     */
    private boolean mClickedConnect;

    public NetworkProviderSettings() {
        super(DISALLOW_CONFIG_WIFI);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        setPinnedHeaderView(com.android.settingslib.widget.progressbar.R.layout.progress_header);
        setProgressBarVisible(false);

        if (hasWifiManager()) {
            setLoading(true, false);
            mIsViewLoading = true;
        }
    }

    private boolean hasWifiManager() {
        if (mWifiManager != null) return true;

        Context context = getContext();
        if (context == null) return false;

        mWifiManager = context.getSystemService(WifiManager.class);
        return (mWifiManager != null);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mAirplaneModeEnabler = new AirplaneModeEnabler(getContext(), this);

        // TODO(b/37429702): Add animations and preference comparator back after initial screen is
        // loaded (ODR).
        setAnimationAllowed(false);

        addPreferences();

        mIsRestricted = isUiRestricted();
        updateUserType();

        mMenuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                MenuItem fixConnectivityItem = menu.add(0, MENU_FIX_CONNECTIVITY, 0,
                        R.string.fix_connectivity);
                fixConnectivityItem.setIcon(R.drawable.ic_repair_24dp);
                fixConnectivityItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == MENU_FIX_CONNECTIVITY) {
                    if (isPhoneOnCall()) {
                        showResetInternetDialog();
                        return true;
                    }
                    fixConnectivity();
                    return true;
                }
                return false;
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                MenuProvider.super.onPrepareMenu(menu);

                boolean isWifiEnabled = mWifiPickerTracker != null
                        && mWifiPickerTracker.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
                boolean isAirplaneModeOn =
                        mAirplaneModeEnabler != null && mAirplaneModeEnabler.isAirplaneModeOn();
                MenuItem fixConnectivityItem = menu.findItem(MENU_FIX_CONNECTIVITY);
                if (fixConnectivityItem == null) {
                    return;
                }
                fixConnectivityItem.setVisible(!mIsGuest && (!isAirplaneModeOn || isWifiEnabled));
            }
        };
    }

    private void updateUserType() {
        UserManager userManager = getSystemService(UserManager.class);
        if (userManager == null) return;
        mIsAdmin = userManager.isAdminUser();
        mIsGuest = userManager.isGuestUser();
    }

    private void addPreferences() {
        addPreferencesFromResource(R.xml.network_provider_settings);

        mAirplaneModeMsgPreference = findPreference(PREF_KEY_AIRPLANE_MODE_MSG);
        updateAirplaneModeMsgPreference(mAirplaneModeEnabler.isAirplaneModeOn() /* visible */);
        mConnectedWifiEntryPreferenceCategory = findPreference(PREF_KEY_CONNECTED_ACCESS_POINTS);
        mFirstWifiEntryPreferenceCategory = findPreference(PREF_KEY_FIRST_ACCESS_POINTS);
        mWifiEntryPreferenceCategory = findPreference(PREF_KEY_ACCESS_POINTS);
        mConfigureWifiSettingsPreference = findPreference(PREF_KEY_CONFIGURE_NETWORK_SETTINGS);
        mSavedNetworksPreference = findPreference(PREF_KEY_SAVED_NETWORKS);
        mAddWifiNetworkPreference = findPreference(PREF_KEY_ADD_WIFI_NETWORK);
        // Hide mAddWifiNetworkPreference by default. updateWifiEntryPreferences() will add it back
        // later when appropriate.
        mWifiEntryPreferenceCategory.removePreference(mAddWifiNetworkPreference);
        mDataUsagePreference = findPreference(PREF_KEY_DATA_USAGE);
        mDataUsagePreference.setVisible(DataUsageUtils.hasWifiRadio(getContext()));
        mDataUsagePreference.setTemplate(new NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI)
                        .build(), 0 /*subId*/);
        mResetInternetPreference = findPreference(PREF_KEY_RESET_INTERNET);
        if (mResetInternetPreference != null) {
            mResetInternetPreference.setVisible(false);
        }
        addNetworkMobileProviderController();
        addConnectedEthernetNetworkController();
        addWifiSwitchPreferenceController();
        mWifiStatusMessagePreference = findPreference(PREF_KEY_WIFI_STATUS_MESSAGE);

        checkConnectivityRecovering();
    }

    private void updateAirplaneModeMsgPreference(boolean visible) {
        if (mAirplaneModeMsgPreference != null) {
            mAirplaneModeMsgPreference.setVisible(visible);
        }
    }

    /**
     * Whether to show any UI which is SIM related.
     */
    @VisibleForTesting
    boolean showAnySubscriptionInfo(Context context) {
        return (context != null) && SubscriptionUtil.isSimHardwareVisible(context);
    }

    private void addNetworkMobileProviderController() {
        if (!showAnySubscriptionInfo(getContext())) {
            return;
        }
        if (mNetworkMobileProviderController == null) {
            mNetworkMobileProviderController = new NetworkMobileProviderController(
                    getContext(), PREF_KEY_PROVIDER_MOBILE_NETWORK);
        }
        mNetworkMobileProviderController.init(getSettingsLifecycle());
        mNetworkMobileProviderController.displayPreference(getPreferenceScreen());
    }

    private void addConnectedEthernetNetworkController() {
        if (mConnectedEthernetNetworkController == null) {
            mConnectedEthernetNetworkController =
                    new ConnectedEthernetNetworkController(getContext(), getSettingsLifecycle());
        }
        mConnectedEthernetNetworkController.displayPreference(getPreferenceScreen());
    }

    private void addWifiSwitchPreferenceController() {
        if (!hasWifiManager()) return;
        if (mWifiSwitchPreferenceController == null) {
            mWifiSwitchPreferenceController =
                    new WifiSwitchPreferenceController(getContext(), getSettingsLifecycle());
        }
        mWifiSwitchPreferenceController.displayPreference(getPreferenceScreen());
    }

    private void checkConnectivityRecovering() {
        mInternetResetHelper = new InternetResetHelper(getContext(), getLifecycle(),
                mNetworkMobileProviderController,
                findPreference(WifiSwitchPreferenceController.KEY),
                mConnectedWifiEntryPreferenceCategory,
                mFirstWifiEntryPreferenceCategory,
                mWifiEntryPreferenceCategory,
                mResetInternetPreference);
        mInternetResetHelper.checkRecovering();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (hasWifiManager()) {
            mWifiPickerTrackerHelper =
                    new WifiPickerTrackerHelper(getSettingsLifecycle(), getContext(), this);
            mWifiPickerTracker = mWifiPickerTrackerHelper.getWifiPickerTracker();
        }
        mInternetUpdater = new InternetUpdater(getContext(), getSettingsLifecycle(), this);

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

        if (savedInstanceState != null) {
            mDialogMode = savedInstanceState.getInt(SAVE_DIALOG_MODE);
            mDialogWifiEntryKey = savedInstanceState.getString(SAVE_DIALOG_WIFIENTRY_KEY);
        }

        // If we're supposed to enable/disable the Next button based on our current connection
        // state, start it off in the right state.
        final Intent intent = getActivity().getIntent();
        mEnableNextOnConnection = intent.getBooleanExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, false);

        if (intent.hasExtra(EXTRA_START_CONNECT_SSID)) {
            mOpenSsid = intent.getStringExtra(EXTRA_START_CONNECT_SSID);
        }

        if (mNetworkMobileProviderController != null) {
            mNetworkMobileProviderController.setWifiPickerTrackerHelper(mWifiPickerTrackerHelper);
        }

        requireActivity().addMenuProvider(mMenuProvider);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mIsViewLoading) {
            final long delayMillis = (hasWifiManager() && mWifiManager.isWifiEnabled())
                    ? 1000 : 100;
            getView().postDelayed(mRemoveLoadingRunnable, delayMillis);
        }
        if (mIsRestricted) {
            restrictUi();
            return;
        }
        mAirplaneModeEnabler.start();
    }

    private void restrictUi() {
        if (!isUiRestrictedByOnlyAdmin()) {
            getEmptyTextView().setText(R.string.wifi_empty_list_user_restricted);
        }
        getPreferenceScreen().removeAll();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Disable the animation of the preference list
        final RecyclerView prefListView = getListView();
        if (prefListView != null) {
            prefListView.setItemAnimator(null);
        }

        // Because RestrictedSettingsFragment's onResume potentially requests authorization,
        // which changes the restriction state, recalculate it.
        final boolean alreadyImmutablyRestricted = mIsRestricted;
        mIsRestricted = isUiRestricted();
        if (!alreadyImmutablyRestricted && mIsRestricted) {
            restrictUi();
        }

        changeNextButtonState(mWifiPickerTracker != null
                && mWifiPickerTracker.getConnectedWifiEntry() != null);
    }

    @Override
    public void onStop() {
        getView().removeCallbacks(mRemoveLoadingRunnable);
        getView().removeCallbacks(mUpdateWifiEntryPreferencesRunnable);
        getView().removeCallbacks(mHideProgressBarRunnable);
        mAirplaneModeEnabler.stop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mAirplaneModeEnabler != null) {
            mAirplaneModeEnabler.close();
        }
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (!hasWifiManager()) {
            // Do nothing
        } else if (requestCode == ADD_NETWORK_REQUEST) {
            handleAddNetworkRequest(resultCode, data);
            return;
        } else if (requestCode == REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER) {
            if (resultCode == Activity.RESULT_OK) {
                if (mDialog != null) {
                    mDialog.dismiss();
                }
            }
            return;
        } else if (requestCode == CONFIG_NETWORK_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                final WifiConfiguration wifiConfiguration = data.getParcelableExtra(
                        ConfigureWifiEntryFragment.NETWORK_CONFIG_KEY);
                if (wifiConfiguration != null) {
                    mWifiManager.connect(wifiConfiguration,
                            new WifiConnectActionListener());
                }
            }
            return;
        } else if (requestCode == MANAGE_SUBSCRIPTION) {
            //Do nothing
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
            outState.putString(SAVE_DIALOG_WIFIENTRY_KEY, mDialogWifiEntryKey);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
        Preference preference = (Preference) view.getTag();
        if (!(preference instanceof LongPressWifiEntryPreference)) {
            // Do nothing.
            return;
        }

        // Cache the WifiEntry for onContextItemSelected. Don't use it in other methods.
        mSelectedWifiEntry = ((LongPressWifiEntryPreference) preference).getWifiEntry();

        menu.setHeaderTitle(mSelectedWifiEntry.getTitle());
        if (mSelectedWifiEntry.canConnect()) {
            menu.add(Menu.NONE, MENU_ID_CONNECT, 0 /* order */, R.string.wifi_connect);
        }

        if (mSelectedWifiEntry.canDisconnect()) {
            if (mSelectedWifiEntry.canShare()) {
                addShareMenuIfSuitable(menu);
            }
            menu.add(Menu.NONE, MENU_ID_DISCONNECT, 1 /* order */,
                    R.string.wifi_disconnect_button_text);
        }

        // "forget" for normal saved network. And "disconnect" for ephemeral network because it
        // could only be disconnected and be put in blocklists so it won't be used again.
        if (canForgetNetwork()) {
            addForgetMenuIfSuitable(menu);
        }

        WifiConfiguration config = mSelectedWifiEntry.getWifiConfiguration();
        // Some configs are ineditable
        if (WifiUtils.isNetworkLockedDown(getActivity(), config)) {
            return;
        }

        addModifyMenuIfSuitable(menu, mSelectedWifiEntry);
    }

    @VisibleForTesting
    void addShareMenuIfSuitable(ContextMenu menu) {
        if (mIsAdmin) {
            menu.add(Menu.NONE, MENU_ID_SHARE, 0 /* order */, R.string.share);
            return;
        }
        Log.w(TAG, "Don't add the Wi-Fi share menu because the user is not an admin.");
        EventLog.writeEvent(0x534e4554, "206986392", -1 /* UID */, "User is not an admin");
    }

    @VisibleForTesting
    void addForgetMenuIfSuitable(ContextMenu menu) {
        if (mIsAdmin) {
            menu.add(Menu.NONE, MENU_ID_FORGET, 0 /* order */, R.string.forget);
        }
    }

    @VisibleForTesting
    void addModifyMenuIfSuitable(ContextMenu menu, WifiEntry wifiEntry) {
        if (mIsAdmin && wifiEntry.isSaved()
                && wifiEntry.getConnectedState() != WifiEntry.CONNECTED_STATE_CONNECTED) {
            menu.add(Menu.NONE, MENU_ID_MODIFY, 0 /* order */, R.string.wifi_modify);
        }
    }

    private boolean canForgetNetwork() {
        return mSelectedWifiEntry.canForget() && !WifiUtils.isNetworkLockedDown(getActivity(),
                mSelectedWifiEntry.getWifiConfiguration());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_CONNECT:
                connect(mSelectedWifiEntry, true /* editIfNoConfig */, false /* fullScreenEdit */);
                return true;
            case MENU_ID_DISCONNECT:
                mSelectedWifiEntry.disconnect(null /* callback */);
                return true;
            case MENU_ID_FORGET:
                forget(mSelectedWifiEntry);
                return true;
            case MENU_ID_SHARE:
                WifiDppUtils.showLockScreen(getContext(),
                        () -> launchWifiDppConfiguratorActivity(mSelectedWifiEntry));
                return true;
            case MENU_ID_MODIFY:
                if (!mIsAdmin) {
                    Log.e(TAG, "Can't modify Wi-Fi because the user isn't admin.");
                    EventLog.writeEvent(0x534e4554, "237672190", UserHandle.myUserId(),
                            "User isn't admin");
                    return true;
                }
                showDialog(mSelectedWifiEntry, WifiConfigUiBase2.MODE_MODIFY);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // If the preference has a fragment set, open that
        if (preference.getFragment() != null) {
            preference.setOnPreferenceClickListener(null);
            return super.onPreferenceTreeClick(preference);
        }

        if (preference instanceof LongPressWifiEntryPreference) {
            onSelectedWifiPreferenceClick((LongPressWifiEntryPreference) preference);
        } else if (preference == mAddWifiNetworkPreference) {
            onAddNetworkPressed();
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    @VisibleForTesting
    void onSelectedWifiPreferenceClick(LongPressWifiEntryPreference preference) {
        final WifiEntry selectedEntry = preference.getWifiEntry();

        if (selectedEntry.shouldEditBeforeConnect()) {
            launchConfigNewNetworkFragment(selectedEntry);
            return;
        }

        if (selectedEntry.canConnect()) {
            connect(selectedEntry, true /* editIfNoConfig */, true /* fullScreenEdit */);
            return;
        }

        if (selectedEntry.isSaved()) {
            launchNetworkDetailsFragment(preference);
        }
    }

    private void launchWifiDppConfiguratorActivity(WifiEntry wifiEntry) {
        final Intent intent = WifiDppUtils.getConfiguratorQrCodeGeneratorIntentOrNull(getContext(),
                mWifiManager, wifiEntry);

        if (intent == null) {
            Log.e(TAG, "Launch Wi-Fi DPP QR code generator with a wrong Wi-Fi network!");
        } else {
            mMetricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN,
                    SettingsEnums.ACTION_SETTINGS_SHARE_WIFI_QR_CODE,
                    SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR,
                    /* key */ null,
                    /* value */ Integer.MIN_VALUE);

            startActivity(intent);
        }
    }

    private void showDialog(WifiEntry wifiEntry, int dialogMode) {
        if (WifiUtils.isNetworkLockedDown(getActivity(), wifiEntry.getWifiConfiguration())
                && wifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(),
                    RestrictedLockUtilsInternal.getDeviceOwner(getActivity()));
            return;
        }

        if (mDialog != null) {
            removeDialog(WIFI_DIALOG_ID);
            mDialog = null;
        }

        // Save the access point and edit mode
        mDialogWifiEntry = wifiEntry;
        mDialogWifiEntryKey = wifiEntry.getKey();
        mDialogMode = dialogMode;

        showDialog(WIFI_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == WIFI_DIALOG_ID) {  // modify network
            mDialog = new WifiDialog2(requireContext(), this, mDialogWifiEntry, mDialogMode);
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
        mDialogWifiEntry = null;
        mDialogWifiEntryKey = null;
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

    @Override
    public void onInternetTypeChanged(@InternetUpdater.InternetType int internetType) {
        ThreadUtils.postOnMainThread(() -> {
            onWifiStateChanged();
        });
    }

    /** Called when the state of Wifi has changed. */
    @Override
    public void onWifiStateChanged() {
        if (mIsRestricted || !hasWifiManager()) {
            return;
        }
        final int wifiState = mWifiPickerTracker.getWifiState();

        if (mWifiPickerTracker.isVerboseLoggingEnabled()) {
            Log.i(TAG, "onWifiStateChanged called with wifi state: " + wifiState);
        }

        if (isFinishingOrDestroyed()) {
            Log.w(TAG, "onWifiStateChanged shouldn't run when fragment is finishing or destroyed");
            return;
        }

        if (isAdded()) {
            // update the menu item
            requireActivity().invalidateMenu();
        }

        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                setWifiScanMessage(/* isWifiEnabled */ true);
                updateWifiEntryPreferences();
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                removeConnectedWifiEntryPreference();
                removeWifiEntryPreference();
                setProgressBarVisible(true);
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                removeConnectedWifiEntryPreference();
                removeWifiEntryPreference();
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setWifiScanMessage(/* isWifiEnabled */ false);
                removeConnectedWifiEntryPreference();
                removeWifiEntryPreference();
                setAdditionalSettingsSummaries();
                setProgressBarVisible(false);
                mClickedConnect = false;
                break;
        }
    }

    @Override
    public void onScanRequested() {
        setProgressBarVisible(true);
    }

    @VisibleForTesting
    void setWifiScanMessage(boolean isWifiEnabled) {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        final LocationManager locationManager = context.getSystemService(LocationManager.class);
        if (!hasWifiManager() || isWifiEnabled || !locationManager.isLocationEnabled()
                || !mWifiManager.isScanAlwaysAvailable()) {
            mWifiStatusMessagePreference.setVisible(false);
            return;
        }
        if (TextUtils.isEmpty(mWifiStatusMessagePreference.getTitle())) {
            mWifiStatusMessagePreference.setTitle(R.string.wifi_scan_notify_message);
            mWifiStatusMessagePreference.setLearnMoreText(
                    context.getString(R.string.wifi_scan_change));
            mWifiStatusMessagePreference.setLearnMoreAction(v -> launchWifiScanningFragment());
        }
        mWifiStatusMessagePreference.setVisible(true);
    }

    private void launchWifiScanningFragment() {
        new SubSettingLauncher(getContext())
            .setDestination(WifiScanningFragment.class.getName())
            .setSourceMetricsCategory(SettingsEnums.SETTINGS_NETWORK_CATEGORY)
            .launch();
    }

    @Override
    public void onWifiEntriesChanged(@WifiPickerTracker.WifiEntriesChangedReason int reason) {
        updateWifiEntryPreferences();
        if (reason == WifiPickerTracker.WIFI_ENTRIES_CHANGED_REASON_SCAN_RESULTS) {
            setProgressBarVisible(false);
        }
        changeNextButtonState(mWifiPickerTracker != null
                && mWifiPickerTracker.getConnectedWifiEntry() != null);

        // Edit the Wi-Fi network of specified SSID.
        if (mOpenSsid != null && mWifiPickerTracker != null) {
            Optional<WifiEntry> matchedWifiEntry = mWifiPickerTracker.getWifiEntries().stream()
                    .filter(wifiEntry -> TextUtils.equals(mOpenSsid, wifiEntry.getSsid()))
                    .filter(wifiEntry -> wifiEntry.getSecurity() != WifiEntry.SECURITY_NONE
                            && wifiEntry.getSecurity() != WifiEntry.SECURITY_OWE)
                    .filter(wifiEntry -> !wifiEntry.isSaved()
                            || isDisabledByWrongPassword(wifiEntry))
                    .findFirst();
            if (matchedWifiEntry.isPresent()) {
                mOpenSsid = null;
                launchConfigNewNetworkFragment(matchedWifiEntry.get());
            }
        }
    }

    @Override
    public void onNumSavedNetworksChanged() {
        if (isFinishingOrDestroyed()) {
            return;
        }
        setAdditionalSettingsSummaries();
    }

    @Override
    public void onNumSavedSubscriptionsChanged() {
        if (isFinishingOrDestroyed()) {
            return;
        }
        setAdditionalSettingsSummaries();
    }

    protected void updateWifiEntryPreferences() {
        // bypass the update if the activity and the view are not ready, or it's restricted UI.
        if (getActivity() == null || getView() == null || mIsRestricted) {
            return;
        }
        // in case state has changed
        if (mWifiPickerTracker == null
                || mWifiPickerTracker.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            return;
        }

        boolean hasAvailableWifiEntries = false;
        mWifiEntryPreferenceCategory.setVisible(true);

        final WifiEntry connectedEntry = mWifiPickerTracker.getConnectedWifiEntry();
        PreferenceCategory connectedWifiPreferenceCategory = getConnectedWifiPreferenceCategory();
        connectedWifiPreferenceCategory.setVisible(connectedEntry != null);
        if (connectedEntry != null) {
            final LongPressWifiEntryPreference connectedPref =
                    connectedWifiPreferenceCategory.findPreference(connectedEntry.getKey());
            if (connectedPref == null || connectedPref.getWifiEntry() != connectedEntry) {
                connectedWifiPreferenceCategory.removeAll();
                final ConnectedWifiEntryPreference pref =
                        createConnectedWifiEntryPreference(connectedEntry);
                pref.setKey(connectedEntry.getKey());
                pref.refresh();
                connectedWifiPreferenceCategory.addPreference(pref);
                pref.setOnPreferenceClickListener(preference -> {
                    if (connectedEntry.canSignIn()) {
                        connectedEntry.signIn(null /* callback */);
                    } else {
                        launchNetworkDetailsFragment(pref);
                    }
                    return true;
                });
                pref.setOnGearClickListener(preference -> {
                    launchNetworkDetailsFragment(pref);
                });

                if (mClickedConnect) {
                    mClickedConnect = false;
                    scrollToPreference(connectedWifiPreferenceCategory);
                }
            }
        } else {
            connectedWifiPreferenceCategory.removeAll();
        }

        int index = 0;
        cacheRemoveAllPrefs(mWifiEntryPreferenceCategory);
        List<WifiEntry> wifiEntries = mWifiPickerTracker.getWifiEntries();
        for (WifiEntry wifiEntry : wifiEntries) {
            hasAvailableWifiEntries = true;

            String key = wifiEntry.getKey();
            LongPressWifiEntryPreference pref =
                    (LongPressWifiEntryPreference) getCachedPreference(key);
            if (pref != null) {
                if (pref.getWifiEntry() == wifiEntry) {
                    pref.setOrder(index++);
                    continue;
                } else {
                    // Create a new preference if the underlying WifiEntry object has changed
                    removePreference(key);
                }
            }

            pref = createLongPressWifiEntryPreference(wifiEntry);
            pref.setKey(wifiEntry.getKey());
            pref.setOrder(index++);
            pref.refresh();

            if (wifiEntry.getHelpUriString() != null) {
                pref.setOnButtonClickListener(preference -> {
                    openSubscriptionHelpPage(wifiEntry);
                });
            }
            mWifiEntryPreferenceCategory.addPreference(pref);
        }
        removeCachedPrefs(mWifiEntryPreferenceCategory);

        if (!hasAvailableWifiEntries) {
            Preference pref = new Preference(getPrefContext());
            pref.setSelectable(false);
            pref.setSummary(R.string.wifi_empty_list_wifi_on);
            pref.setOrder(index++);
            pref.setKey(PREF_KEY_EMPTY_WIFI_LIST);
            mWifiEntryPreferenceCategory.addPreference(pref);
        }

        mAddWifiNetworkPreference.setOrder(index++);
        mWifiEntryPreferenceCategory.addPreference(mAddWifiNetworkPreference);
        setAdditionalSettingsSummaries();
    }

    @VisibleForTesting
    PreferenceCategory getConnectedWifiPreferenceCategory() {
        if (mInternetUpdater.getInternetType() == InternetUpdater.INTERNET_WIFI) {
            mFirstWifiEntryPreferenceCategory.setVisible(false);
            mFirstWifiEntryPreferenceCategory.removeAll();
            return mConnectedWifiEntryPreferenceCategory;
        }

        mConnectedWifiEntryPreferenceCategory.setVisible(false);
        mConnectedWifiEntryPreferenceCategory.removeAll();
        return mFirstWifiEntryPreferenceCategory;
    }

    @VisibleForTesting
    ConnectedWifiEntryPreference createConnectedWifiEntryPreference(WifiEntry wifiEntry) {
        if (mInternetUpdater.getInternetType() == InternetUpdater.INTERNET_WIFI) {
            return new ConnectedWifiEntryPreference(getPrefContext(), wifiEntry, this);
        }
        return new FirstWifiEntryPreference(getPrefContext(), wifiEntry, this);
    }

    @VisibleForTesting
    void launchNetworkDetailsFragment(LongPressWifiEntryPreference pref) {
        final WifiEntry wifiEntry = pref.getWifiEntry();
        final Context context = requireContext();

        final Bundle bundle = new Bundle();
        bundle.putString(WifiNetworkDetailsFragment.KEY_CHOSEN_WIFIENTRY_KEY, wifiEntry.getKey());

        new SubSettingLauncher(context)
                .setTitleText(context.getText(R.string.pref_title_network_details))
                .setDestination(WifiNetworkDetailsFragment.class.getName())
                .setArguments(bundle)
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    @VisibleForTesting
    LongPressWifiEntryPreference createLongPressWifiEntryPreference(WifiEntry wifiEntry) {
        return new LongPressWifiEntryPreference(getPrefContext(), wifiEntry, this);
    }

    private void launchAddNetworkFragment() {
        new SubSettingLauncher(getContext())
                .setTitleRes(R.string.wifi_add_network)
                .setDestination(AddNetworkFragment.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(this, ADD_NETWORK_REQUEST)
                .launch();
    }

    /** Removes all preferences and hide the {@link #mConnectedWifiEntryPreferenceCategory} and
     *  {@link #mFirstWifiEntryPreferenceCategory}. */
    private void removeConnectedWifiEntryPreference() {
        mConnectedWifiEntryPreferenceCategory.removeAll();
        mConnectedWifiEntryPreferenceCategory.setVisible(false);
        mFirstWifiEntryPreferenceCategory.setVisible(false);
        mFirstWifiEntryPreferenceCategory.removeAll();
    }

    private void removeWifiEntryPreference() {
        mWifiEntryPreferenceCategory.removeAll();
        mWifiEntryPreferenceCategory.setVisible(false);
    }

    @VisibleForTesting
    void setAdditionalSettingsSummaries() {
        mConfigureWifiSettingsPreference.setSummary(getString(
                isWifiWakeupEnabled()
                        ? R.string.wifi_configure_settings_preference_summary_wakeup_on
                        : R.string.wifi_configure_settings_preference_summary_wakeup_off));

        final int numSavedNetworks = mWifiPickerTracker == null ? 0 :
                mWifiPickerTracker.getNumSavedNetworks();
        final int numSavedSubscriptions = mWifiPickerTracker == null ? 0 :
                mWifiPickerTracker.getNumSavedSubscriptions();
        if (numSavedNetworks + numSavedSubscriptions > 0) {
            mSavedNetworksPreference.setVisible(true);
            mSavedNetworksPreference.setSummary(
                    getSavedNetworkSettingsSummaryText(numSavedNetworks, numSavedSubscriptions));
        } else {
            mSavedNetworksPreference.setVisible(false);
        }
    }

    private String getSavedNetworkSettingsSummaryText(
            int numSavedNetworks, int numSavedSubscriptions) {
        if (getContext() == null) {
            Log.w(TAG, "getSavedNetworkSettingsSummaryText shouldn't run if resource is not ready");
            return null;
        }

        if (numSavedSubscriptions == 0) {
            return StringUtil.getIcuPluralsString(getContext(), numSavedNetworks,
                    R.string.wifi_saved_access_points_summary);
        } else if (numSavedNetworks == 0) {
            return StringUtil.getIcuPluralsString(getContext(), numSavedSubscriptions,
                    R.string.wifi_saved_passpoint_access_points_summary);
        } else {
            final int numTotalEntries = numSavedNetworks + numSavedSubscriptions;
            return StringUtil.getIcuPluralsString(getContext(), numTotalEntries,
                    R.string.wifi_saved_all_access_points_summary);
        }
    }

    private boolean isWifiWakeupEnabled() {
        final Context context = getContext();
        final PowerManager powerManager = context.getSystemService(PowerManager.class);
        final ContentResolver contentResolver = context.getContentResolver();
        return hasWifiManager()
                && mWifiManager.isAutoWakeupEnabled()
                && mWifiManager.isScanAlwaysAvailable()
                && Settings.Global.getInt(contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0) == 0
                && !powerManager.isPowerSaveMode();
    }

    protected void setProgressBarVisible(boolean visible) {
        showPinnedHeader(visible);
    }

    @VisibleForTesting
    void handleAddNetworkRequest(int result, Intent data) {
        if (result == Activity.RESULT_OK) {
            handleAddNetworkSubmitEvent(data);
        }
    }

    private void handleAddNetworkSubmitEvent(Intent data) {
        final WifiConfiguration wifiConfiguration = data.getParcelableExtra(
                AddNetworkFragment.WIFI_CONFIG_KEY);
        if (wifiConfiguration != null && hasWifiManager()) {
            mWifiManager.save(wifiConfiguration, mSaveListener);
        }
    }

    /**
     * Called when "add network" button is pressed.
     */
    private void onAddNetworkPressed() {
        launchAddNetworkFragment();
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_wifi;
    }

    /**
     * Renames/replaces "Next" button when appropriate. "Next" button usually exists in
     * Wi-Fi setup screens, not in usual wifi settings screen.
     *
     * @param enabled true when the device is connected to a wifi network.
     */
    @VisibleForTesting
    void changeNextButtonState(boolean enabled) {
        if (mEnableNextOnConnection && hasNextButton()) {
            getNextButton().setEnabled(enabled);
        }
    }

    @Override
    public void onForget(WifiDialog2 dialog) {
        forget(dialog.getWifiEntry());
    }

    @Override
    public void onSubmit(WifiDialog2 dialog) {
        if (!hasWifiManager()) return;

        final int dialogMode = dialog.getMode();
        final WifiConfiguration config = dialog.getController().getConfig();
        final WifiEntry wifiEntry = dialog.getWifiEntry();

        if (dialogMode == WifiConfigUiBase2.MODE_MODIFY) {
            if (config == null) {
                Toast.makeText(getContext(), R.string.wifi_failed_save_message,
                        Toast.LENGTH_SHORT).show();
            } else {
                mWifiManager.save(config, mSaveListener);
            }
        } else if (dialogMode == WifiConfigUiBase2.MODE_CONNECT
                || (dialogMode == WifiConfigUiBase2.MODE_VIEW && wifiEntry.canConnect())) {
            if (config == null) {
                connect(wifiEntry, false /* editIfNoConfig */,
                        false /* fullScreenEdit*/);
            } else {
                mWifiManager.connect(config, new WifiConnectActionListener());
            }
        }
    }

    @Override
    public void onScan(WifiDialog2 dialog, String ssid) {
        // Launch QR code scanner to join a network.
        startActivityForResult(
                WifiDppUtils.getEnrolleeQrCodeScannerIntent(dialog.getContext(), ssid),
                REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER);
    }

    private void forget(WifiEntry wifiEntry) {
        mMetricsFeatureProvider.action(getActivity(), SettingsEnums.ACTION_WIFI_FORGET);
        wifiEntry.forget(null /* callback */);
    }

    @VisibleForTesting
    void connect(WifiEntry wifiEntry, boolean editIfNoConfig, boolean fullScreenEdit) {
        mMetricsFeatureProvider.action(getActivity(), SettingsEnums.ACTION_WIFI_CONNECT,
                wifiEntry.isSaved());

        // If it's an unsaved secure WifiEntry, it will callback
        // ConnectCallback#onConnectResult with ConnectCallback#CONNECT_STATUS_FAILURE_NO_CONFIG
        wifiEntry.connect(new WifiEntryConnectCallback(wifiEntry, editIfNoConfig,
                fullScreenEdit));
    }

    private class WifiConnectActionListener implements WifiManager.ActionListener {
        @Override
        public void onSuccess() {
            mClickedConnect = true;
        }

        @Override
        public void onFailure(int reason) {
            if (isFinishingOrDestroyed()) {
                return;
            }
            Toast.makeText(getContext(), R.string.wifi_failed_connect_message, Toast.LENGTH_SHORT)
                    .show();
        }
    };

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new SearchIndexProvider(R.xml.network_provider_settings);

    @VisibleForTesting
    static class SearchIndexProvider extends BaseSearchIndexProvider {

        private final WifiRestriction mWifiRestriction;

        SearchIndexProvider(int xmlRes) {
            super(xmlRes);
            mWifiRestriction = new WifiRestriction();
        }

        @VisibleForTesting
        SearchIndexProvider(int xmlRes, WifiRestriction wifiRestriction) {
            super(xmlRes);
            mWifiRestriction = wifiRestriction;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final List<String> keys = super.getNonIndexableKeys(context);

            if (!mWifiRestriction.isChangeWifiStateAllowed(context)) {
                keys.add(PREF_KEY_WIFI_TOGGLE);
            }

            final WifiManager wifiManager = context.getSystemService(WifiManager.class);
            if (wifiManager == null) return keys;

            if (WifiSavedConfigUtils.getAllConfigsCount(context, wifiManager) == 0) {
                keys.add(PREF_KEY_SAVED_NETWORKS);
            }
            if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
                keys.add(PREF_KEY_ADD_WIFI_NETWORK);
            }

            if (!DataUsageUtils.hasWifiRadio(context)) {
                keys.add(PREF_KEY_DATA_USAGE);
            }
            return keys;
        }
    }

    @VisibleForTesting
    static class WifiRestriction {
        public boolean isChangeWifiStateAllowed(@Nullable Context context) {
            if (context == null) return true;
            return WifiEnterpriseRestrictionUtils.isChangeWifiStateAllowed(context);
        }
    }

    private class WifiEntryConnectCallback implements ConnectCallback {
        final WifiEntry mConnectWifiEntry;
        final boolean mEditIfNoConfig;
        final boolean mFullScreenEdit;

        WifiEntryConnectCallback(WifiEntry connectWifiEntry, boolean editIfNoConfig,
                boolean fullScreenEdit) {
            mConnectWifiEntry = connectWifiEntry;
            mEditIfNoConfig = editIfNoConfig;
            mFullScreenEdit = fullScreenEdit;
        }

        @Override
        public void onConnectResult(@ConnectStatus int status) {
            if (isFinishingOrDestroyed()) {
                return;
            }

            if (status == ConnectCallback.CONNECT_STATUS_SUCCESS) {
                mClickedConnect = true;
            } else if (status == ConnectCallback.CONNECT_STATUS_FAILURE_NO_CONFIG) {
                if (mEditIfNoConfig) {
                    // Edit an unsaved secure Wi-Fi network.
                    if (mFullScreenEdit) {
                        launchConfigNewNetworkFragment(mConnectWifiEntry);
                    } else {
                        showDialog(mConnectWifiEntry, WifiConfigUiBase2.MODE_CONNECT);
                    }
                }
            } else if (status == CONNECT_STATUS_FAILURE_UNKNOWN) {
                Toast.makeText(getContext(), R.string.wifi_failed_connect_message,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @VisibleForTesting
    void launchConfigNewNetworkFragment(WifiEntry wifiEntry) {
        if (mIsRestricted) {
            Log.e(TAG, "Can't configure Wi-Fi because NetworkProviderSettings is restricted.");
            EventLog.writeEvent(0x534e4554, "246301667", -1 /* UID */, "Fragment is restricted.");
            return;
        }

        final Bundle bundle = new Bundle();
        bundle.putString(WifiNetworkDetailsFragment.KEY_CHOSEN_WIFIENTRY_KEY,
                wifiEntry.getKey());
        new SubSettingLauncher(getContext())
                .setTitleText(wifiEntry.getTitle())
                .setDestination(ConfigureWifiEntryFragment.class.getName())
                .setArguments(bundle)
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(NetworkProviderSettings.this, CONFIG_NETWORK_REQUEST)
                .launch();
    }

    /** Helper method to return whether a WifiEntry is disabled due to a wrong password */
    private static boolean isDisabledByWrongPassword(WifiEntry wifiEntry) {
        WifiConfiguration config = wifiEntry.getWifiConfiguration();
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

    @VisibleForTesting
    void openSubscriptionHelpPage(WifiEntry wifiEntry) {
        final Intent intent = getHelpIntent(getContext(), wifiEntry.getHelpUriString());
        if (intent != null) {
            try {
                startActivityForResult(intent, MANAGE_SUBSCRIPTION);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "Activity was not found for intent, " + intent.toString());
            }
        }
    }

    @VisibleForTesting
    Intent getHelpIntent(Context context, String helpUrlString) {
        return HelpUtils.getHelpIntent(context, helpUrlString, context.getClass().getName());
    }

    @VisibleForTesting
    void showResetInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        DialogInterface.OnClickListener resetInternetClickListener =
                new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fixConnectivity();
                    }
                };
        builder.setTitle(R.string.reset_your_internet_title)
                .setMessage(R.string.reset_internet_text)
                .setPositiveButton(R.string.tts_reset, resetInternetClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    @VisibleForTesting
    boolean isPhoneOnCall() {
        TelephonyManager mTelephonyManager = getActivity().getSystemService(TelephonyManager.class);
        int state = mTelephonyManager.getCallState();
        return state != TelephonyManager.CALL_STATE_IDLE;
    }

    private void fixConnectivity() {
        if (mIsGuest) {
            Log.e(TAG, "Can't reset network because the user is a guest.");
            EventLog.writeEvent(0x534e4554, "252995826", UserHandle.myUserId(), "User is a guest");
            return;
        }
        mInternetResetHelper.restart();
    }

    /**
     * Called when airplane mode status is changed.
     *
     * @param isAirplaneModeOn The airplane mode is on
     */
    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        updateAirplaneModeMsgPreference(isAirplaneModeOn /* visible */);
        if (isAdded()) {
            // update the menu item
            requireActivity().invalidateMenu();
        }
    }

    /**
     * A Wi-Fi preference for the connected Wi-Fi network without internet access.
     *
     * Override the icon color attribute by {@link ConnectedWifiEntryPreference#getIconColorAttr()}
     * and show the icon color to android.R.attr.colorControlNormal for the preference.
     */
    public class FirstWifiEntryPreference extends ConnectedWifiEntryPreference {
        public FirstWifiEntryPreference(Context context, WifiEntry wifiEntry,
                Fragment fragment) {
            super(context, wifiEntry, fragment);
        }

        @Override
        protected int getIconColorAttr() {
            return android.R.attr.colorControlNormal;
        }
    }
}

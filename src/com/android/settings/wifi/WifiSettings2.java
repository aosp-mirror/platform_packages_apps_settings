/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.net.ConnectivityManager;
import android.net.NetworkScoreManager;
import android.net.NetworkTemplate;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.os.SimpleClock;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

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
import com.android.settings.wifi.details2.WifiNetworkDetailsFragment2;
import com.android.settings.wifi.dpp.WifiDppUtils;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.wifi.LongPressWifiEntryPreference;
import com.android.settingslib.wifi.WifiSavedConfigUtils;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiEntry.ConnectCallback;
import com.android.wifitrackerlib.WifiPickerTracker;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * UI for Wi-Fi settings screen
 */
@SearchIndexable
public class WifiSettings2 extends RestrictedSettingsFragment
        implements Indexable, WifiPickerTracker.WifiPickerTrackerCallback,
        WifiDialog2.WifiDialog2Listener, DialogInterface.OnDismissListener {

    private static final String TAG = "WifiSettings2";

    // IDs of context menu
    static final int MENU_ID_CONNECT = Menu.FIRST + 1;
    @VisibleForTesting
    static final int MENU_ID_DISCONNECT = Menu.FIRST + 2;
    @VisibleForTesting
    static final int MENU_ID_FORGET = Menu.FIRST + 3;
    static final int MENU_ID_MODIFY = Menu.FIRST + 4;

    // Max age of tracked WifiEntries
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating WifiPickerTracker scans
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    @VisibleForTesting
    static final int ADD_NETWORK_REQUEST = 2;
    static final int CONFIG_NETWORK_REQUEST = 3;
    static final int MANAGE_SUBSCRIPTION = 4;

    private static final String PREF_KEY_EMPTY_WIFI_LIST = "wifi_empty_list";
    // TODO(b/70983952): Rename these to use WifiEntry instead of AccessPoint.
    private static final String PREF_KEY_CONNECTED_ACCESS_POINTS = "connected_access_point";
    private static final String PREF_KEY_ACCESS_POINTS = "access_points";
    private static final String PREF_KEY_CONFIGURE_WIFI_SETTINGS = "configure_wifi_settings";
    private static final String PREF_KEY_SAVED_NETWORKS = "saved_networks";
    private static final String PREF_KEY_STATUS_MESSAGE = "wifi_status_message";
    @VisibleForTesting
    static final String PREF_KEY_DATA_USAGE = "wifi_data_usage";

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

    private static boolean isVerboseLoggingEnabled() {
        return WifiPickerTracker.isVerboseLoggingEnabled();
    }

    private final Runnable mUpdateWifiEntryPreferencesRunnable = () -> {
        updateWifiEntryPreferences();
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

    // Worker thread used for WifiPickerTracker work
    private HandlerThread mWorkerThread;

    @VisibleForTesting
    WifiPickerTracker mWifiPickerTracker;

    private WifiDialog2 mDialog;

    private View mProgressHeader;

    private PreferenceCategory mConnectedWifiEntryPreferenceCategory;
    private PreferenceCategory mWifiEntryPreferenceCategory;
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

    public WifiSettings2() {
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

        // TODO(b/37429702): Add animations and preference comparator back after initial screen is
        // loaded (ODR).
        setAnimationAllowed(false);

        addPreferences();

        mIsRestricted = isUiRestricted();
    }

    private void addPreferences() {
        addPreferencesFromResource(R.xml.wifi_settings2);

        mConnectedWifiEntryPreferenceCategory = findPreference(PREF_KEY_CONNECTED_ACCESS_POINTS);
        mWifiEntryPreferenceCategory = findPreference(PREF_KEY_ACCESS_POINTS);
        mConfigureWifiSettingsPreference = findPreference(PREF_KEY_CONFIGURE_WIFI_SETTINGS);
        mSavedNetworksPreference = findPreference(PREF_KEY_SAVED_NETWORKS);
        mAddWifiNetworkPreference = new AddWifiNetworkPreference(getPrefContext());
        mStatusMessagePreference = findPreference(PREF_KEY_STATUS_MESSAGE);
        mDataUsagePreference = findPreference(PREF_KEY_DATA_USAGE);
        mDataUsagePreference.setVisible(DataUsageUtils.hasWifiRadio(getContext()));
        mDataUsagePreference.setTemplate(NetworkTemplate.buildTemplateWifiWildcard(),
                0 /*subId*/,
                null /*service*/);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = getContext();
        mWorkerThread = new HandlerThread(TAG +
                "{" + Integer.toHexString(System.identityHashCode(this)) + "}",
                Process.THREAD_PRIORITY_BACKGROUND);
        mWorkerThread.start();
        final Clock elapsedRealtimeClock = new SimpleClock(ZoneOffset.UTC) {
            @Override
            public long millis() {
                return SystemClock.elapsedRealtime();
            }
        };
        mWifiPickerTracker = new WifiPickerTracker(getSettingsLifecycle(), context,
                context.getSystemService(WifiManager.class),
                context.getSystemService(ConnectivityManager.class),
                context.getSystemService(NetworkScoreManager.class),
                new Handler(Looper.getMainLooper()),
                mWorkerThread.getThreadHandler(),
                elapsedRealtimeClock,
                MAX_SCAN_AGE_MILLIS,
                SCAN_INTERVAL_MILLIS,
                this);

        final Activity activity = getActivity();

        if (activity != null) {
            mWifiManager = getActivity().getSystemService(WifiManager.class);
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
        registerForContextMenu(getListView());
        setHasOptionsMenu(true);

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
    }

    @Override
    public void onDestroyView() {
        if (mWifiEnabler != null) {
            mWifiEnabler.teardownSwitchController();
        }
        mWorkerThread.quit();

        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();

        mWifiEnabler = createWifiEnabler();

        if (mIsRestricted) {
            restrictUi();
        }
    }

    private void restrictUi() {
        if (!isUiRestrictedByOnlyAdmin()) {
            getEmptyTextView().setText(R.string.wifi_empty_list_user_restricted);
        }
        getPreferenceScreen().removeAll();
    }

    /**
     * @return new WifiEnabler
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

        changeNextButtonState(mWifiPickerTracker.getConnectedWifiEntry() != null);
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
        getView().removeCallbacks(mUpdateWifiEntryPreferencesRunnable);
        getView().removeCallbacks(mHideProgressBarRunnable);
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
            menu.add(Menu.NONE, MENU_ID_DISCONNECT, 0 /* order */,
                    R.string.wifi_disconnect_button_text);
        }

        // "forget" for normal saved network. And "disconnect" for ephemeral network because it
        // could only be disconnected and be put in blacklists so it won't be used again.
        if (canForgetNetwork()) {
            menu.add(Menu.NONE, MENU_ID_FORGET, 0 /* order */, R.string.forget);
        }

        WifiConfiguration config = mSelectedWifiEntry.getWifiConfiguration();
        // Some configs are ineditable
        if (WifiUtils.isNetworkLockedDown(getActivity(), config)) {
            return;
        }

        if (mSelectedWifiEntry.isSaved() && mSelectedWifiEntry.getConnectedState()
                != WifiEntry.CONNECTED_STATE_CONNECTED) {
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
            case MENU_ID_MODIFY:
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
            final WifiEntry selectedEntry =
                    ((LongPressWifiEntryPreference) preference).getWifiEntry();

            if (selectedEntry.shouldEditBeforeConnect()) {
                launchConfigNewNetworkFragment(selectedEntry);
                return true;
            }

            connect(selectedEntry, true /* editIfNoConfig */, true /* fullScreenEdit */);
        } else if (preference == mAddWifiNetworkPreference) {
            onAddNetworkPressed();
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
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
        switch (dialogId) {
            case WIFI_DIALOG_ID:
                // modify network
                mDialog = WifiDialog2
                        .createModal(getActivity(), this, mDialogWifiEntry, mDialogMode);
                return mDialog;
            default:
                return super.onCreateDialog(dialogId);
        }
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

    /** Called when the state of Wifi has changed. */
    @Override
    public void onWifiStateChanged() {
        if (mIsRestricted) {
            return;
        }
        final int wifiState = mWifiPickerTracker.getWifiState();

        if (isVerboseLoggingEnabled()) {
            Log.i(TAG, "onWifiStateChanged called with wifi state: " + wifiState);
        }

        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                updateWifiEntryPreferences();
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                removeConnectedWifiEntryPreference();
                removeWifiEntryPreference();
                addMessagePreference(R.string.wifi_starting);
                setProgressBarVisible(true);
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                removeConnectedWifiEntryPreference();
                removeWifiEntryPreference();
                addMessagePreference(R.string.wifi_stopping);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                setAdditionalSettingsSummaries();
                setProgressBarVisible(false);
                mClickedConnect = false;
                break;
        }
    }

    @Override
    public void onWifiEntriesChanged() {
        updateWifiEntryPreferencesDelayed();
        changeNextButtonState(mWifiPickerTracker.getConnectedWifiEntry() != null);

        // Edit the Wi-Fi network of specified SSID.
        if (mOpenSsid != null) {
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

    /**
     * Updates WifiEntries from {@link WifiPickerTracker#getWifiEntries()}. Adds a delay to have
     * progress bar displayed before starting to modify entries.
     */
    private void updateWifiEntryPreferencesDelayed() {
        // Safeguard from some delayed event handling
        if (getActivity() != null && !mIsRestricted &&
                mWifiPickerTracker.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            final View view = getView();
            final Handler handler = view.getHandler();
            if (handler != null && handler.hasCallbacks(mUpdateWifiEntryPreferencesRunnable)) {
                return;
            }
            setProgressBarVisible(true);
            view.postDelayed(mUpdateWifiEntryPreferencesRunnable, 300);
        }
    }

    private void updateWifiEntryPreferences() {
        // in case state has changed
        if (mWifiPickerTracker.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            return;
        }

        boolean hasAvailableWifiEntries = false;
        mStatusMessagePreference.setVisible(false);
        mWifiEntryPreferenceCategory.setVisible(true);

        final WifiEntry connectedEntry = mWifiPickerTracker.getConnectedWifiEntry();
        mConnectedWifiEntryPreferenceCategory.setVisible(connectedEntry != null);
        if (connectedEntry != null) {
            final LongPressWifiEntryPreference connectedPref =
                    mConnectedWifiEntryPreferenceCategory.findPreference(connectedEntry.getKey());
            if (connectedPref == null || connectedPref.getWifiEntry() != connectedEntry) {
                mConnectedWifiEntryPreferenceCategory.removeAll();
                final ConnectedWifiEntryPreference pref =
                        new ConnectedWifiEntryPreference(getPrefContext(), connectedEntry, this);
                pref.setKey(connectedEntry.getKey());
                pref.refresh();
                mConnectedWifiEntryPreferenceCategory.addPreference(pref);
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
                    scrollToPreference(mConnectedWifiEntryPreferenceCategory);
                }
            }
        } else {
            mConnectedWifiEntryPreferenceCategory.removeAll();
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
            setProgressBarVisible(true);
            Preference pref = new Preference(getPrefContext());
            pref.setSelectable(false);
            pref.setSummary(R.string.wifi_empty_list_wifi_on);
            pref.setOrder(index++);
            pref.setKey(PREF_KEY_EMPTY_WIFI_LIST);
            mWifiEntryPreferenceCategory.addPreference(pref);
        } else {
            // Continuing showing progress bar for an additional delay to overlap with animation
            getView().postDelayed(mHideProgressBarRunnable, 1700 /* delay millis */);
        }

        mAddWifiNetworkPreference.setOrder(index++);
        mWifiEntryPreferenceCategory.addPreference(mAddWifiNetworkPreference);
        setAdditionalSettingsSummaries();
    }

    private void launchNetworkDetailsFragment(LongPressWifiEntryPreference pref) {
        final WifiEntry wifiEntry = pref.getWifiEntry();
        final Context context = getContext();
        final CharSequence title =
                FeatureFlagUtils.isEnabled(context, FeatureFlags.WIFI_DETAILS_DATAUSAGE_HEADER)
                        ? wifiEntry.getTitle()
                        : context.getText(R.string.pref_title_network_details);

        final Bundle bundle = new Bundle();
        bundle.putString(WifiNetworkDetailsFragment2.KEY_CHOSEN_WIFIENTRY_KEY, wifiEntry.getKey());

        new SubSettingLauncher(context)
                .setTitleText(title)
                .setDestination(WifiNetworkDetailsFragment2.class.getName())
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

    /** Removes all preferences and hide the {@link #mConnectedWifiEntryPreferenceCategory}. */
    private void removeConnectedWifiEntryPreference() {
        mConnectedWifiEntryPreferenceCategory.removeAll();
        mConnectedWifiEntryPreferenceCategory.setVisible(false);
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

        final int numSavedNetworks = mWifiPickerTracker.getNumSavedNetworks();
        final int numSavedSubscriptions = mWifiPickerTracker.getNumSavedSubscriptions();
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
        if (numSavedSubscriptions == 0) {
            return getResources().getQuantityString(R.plurals.wifi_saved_access_points_summary,
                    numSavedNetworks, numSavedNetworks);
        } else if (numSavedNetworks == 0) {
            return getResources().getQuantityString(
                    R.plurals.wifi_saved_passpoint_access_points_summary,
                    numSavedSubscriptions, numSavedSubscriptions);
        } else {
            final int numTotalEntries = numSavedNetworks + numSavedSubscriptions;
            return getResources().getQuantityString(R.plurals.wifi_saved_all_access_points_summary,
                    numTotalEntries, numTotalEntries);
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
        removeConnectedWifiEntryPreference();
        removeWifiEntryPreference();
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

    @VisibleForTesting
    void handleAddNetworkRequest(int result, Intent data) {
        if (result == Activity.RESULT_OK) {
            handleAddNetworkSubmitEvent(data);
        }
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
        startActivityForResult(WifiDppUtils.getEnrolleeQrCodeScannerIntent(ssid),
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

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.wifi_settings2) {
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

    private void launchConfigNewNetworkFragment(WifiEntry wifiEntry) {
        final Bundle bundle = new Bundle();
        bundle.putString(WifiNetworkDetailsFragment2.KEY_CHOSEN_WIFIENTRY_KEY,
                wifiEntry.getKey());
        new SubSettingLauncher(getContext())
                .setTitleText(wifiEntry.getTitle())
                .setDestination(ConfigureWifiEntryFragment.class.getName())
                .setArguments(bundle)
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(WifiSettings2.this, CONFIG_NETWORK_REQUEST)
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
}

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

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.datausage.DataUsagePreference;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SwitchBarController;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.wifi.LongPressWifiEntryPreference;
import com.android.wifitrackerlib.WifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * UI for Wi-Fi settings screen
 */
@SearchIndexable
public class WifiSettings2 extends RestrictedSettingsFragment
        implements Indexable, WifiPickerTracker.WifiPickerTrackerCallback {

    private static final String TAG = "WifiSettings2";

    // Max age of tracked WifiEntries
    private static final long MAX_SCAN_AGE_MILLIS = 15_000;
    // Interval between initiating WifiPickerTracker scans
    private static final long SCAN_INTERVAL_MILLIS = 10_000;

    @VisibleForTesting
    static final int ADD_NETWORK_REQUEST = 2;

    private static final String PREF_KEY_EMPTY_WIFI_LIST = "wifi_empty_list";
    // TODO(b/70983952): Rename these to use WifiEntry instead of AccessPoint.
    private static final String PREF_KEY_CONNECTED_ACCESS_POINTS = "connected_access_point";
    private static final String PREF_KEY_ACCESS_POINTS = "access_points";
    private static final String PREF_KEY_CONFIGURE_WIFI_SETTINGS = "configure_settings";
    private static final String PREF_KEY_SAVED_NETWORKS = "saved_networks";
    private static final String PREF_KEY_STATUS_MESSAGE = "wifi_status_message";
    @VisibleForTesting
    static final String PREF_KEY_DATA_USAGE = "wifi_data_usage";

    private static final int REQUEST_CODE_WIFI_DPP_ENROLLEE_QR_CODE_SCANNER = 0;

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

    private WifiDialog mDialog;

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

    // For Search
    public static final String DATA_KEY_REFERENCE = "main_toggle_wifi";

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
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
        // TODO(b/70983952): Add context menu options here. This should be driven by the appropriate
        // "can do action" APIs from WifiEntry.
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // TODO(b/70983952): Add context menu selection logic here. This should simply call the
        // appropriate WifiEntry action APIs (connect, forget, disconnect, etc).
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // If the preference has a fragment set, open that
        if (preference.getFragment() != null) {
            preference.setOnPreferenceClickListener(null);
            return super.onPreferenceTreeClick(preference);
        }

        // TODO(b/70983952) Add WifiEntry click logic. This should be as simple as calling
        // WifiEntry.connect().
        if (preference instanceof LongPressWifiEntryPreference) {
            final WifiEntry selectedEntry =
                    ((LongPressWifiEntryPreference) preference).getWifiEntry();
            selectedEntry.connect();
        } else if (preference == mAddWifiNetworkPreference) {
            onAddNetworkPressed();
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
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
                break;
        }
    }

    @Override
    public void onWifiEntriesChanged() {
        updateWifiEntryPreferencesDelayed();
    }

    @Override
    public void onNumSavedNetworksChanged() {
        setAdditionalSettingsSummaries();
    }

    @Override
    public void onNumSavedSubscriptionsChanged() {
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
        mConnectedWifiEntryPreferenceCategory.setVisible(true);
        mWifiEntryPreferenceCategory.setVisible(true);

        int index = 0;
        cacheRemoveAllPrefs(mWifiEntryPreferenceCategory);
        List<WifiEntry> wifiEntries = mWifiPickerTracker.getWifiEntries();
        for (WifiEntry wifiEntry : wifiEntries) {
            hasAvailableWifiEntries = true;

            String key = wifiEntry.getKey();
            LongPressWifiEntryPreference pref =
                    (LongPressWifiEntryPreference) getCachedPreference(key);
            if (pref != null) {
                pref.setOrder(index++);
                continue;
            }

            pref = createLongPressWifiEntryPreference(wifiEntry);
            pref.setKey(wifiEntry.getKey());
            pref.setOrder(index++);
            pref.refresh();
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

    private LongPressWifiEntryPreference createLongPressWifiEntryPreference(WifiEntry wifiEntry) {
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
        return Settings.Global.getInt(contentResolver,
                Settings.Global.WIFI_WAKEUP_ENABLED, 0) == 1
                && Settings.Global.getInt(contentResolver,
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1
                && Settings.Global.getInt(contentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0) == 0
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

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<>();
                    final Resources res = context.getResources();

                    // Add fragment title if we are showing this fragment
                    if (res.getBoolean(R.bool.config_show_wifi_settings)) {
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.wifi_settings);
                        data.screenTitle = res.getString(R.string.wifi_settings);
                        data.keywords = res.getString(R.string.keywords_wifi);
                        data.key = DATA_KEY_REFERENCE;
                        result.add(data);
                    }

                    return result;
                }
            };
}

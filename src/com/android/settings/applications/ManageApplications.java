/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceFrameLayout;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Spinner;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.AppHeader;
import com.android.settings.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.Settings.AllApplicationsActivity;
import com.android.settings.Settings.DomainsURLsAppListActivity;
import com.android.settings.Settings.HighPowerApplicationsActivity;
import com.android.settings.Settings.NotificationAppListActivity;
import com.android.settings.Settings.StorageUseActivity;
import com.android.settings.Settings.UsageAccessSettingsActivity;
import com.android.settings.Settings.OverlaySettingsActivity;
import com.android.settings.Settings.WriteSettingsActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateUsageBridge.UsageState;
import com.android.settings.fuelgauge.HighPowerDetail;
import com.android.settings.fuelgauge.PowerWhitelistBackend;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.applications.ApplicationsState.CompoundFilter;
import com.android.settingslib.applications.ApplicationsState.VolumeFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Activity to pick an application that will be used to display installation information and
 * options to uninstall/delete user data for system applications. This activity
 * can be launched through Settings or via the ACTION_MANAGE_PACKAGE_STORAGE
 * intent.
 */
public class ManageApplications extends InstrumentedFragment
        implements OnItemClickListener, OnItemSelectedListener {

    static final String TAG = "ManageApplications";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    // Intent extras.
    public static final String EXTRA_CLASSNAME = "classname";
    // Used for storage only.
    public static final String EXTRA_VOLUME_UUID = "volumeUuid";
    public static final String EXTRA_VOLUME_NAME = "volumeName";

    private static final String EXTRA_SORT_ORDER = "sortOrder";
    private static final String EXTRA_SHOW_SYSTEM = "showSystem";
    private static final String EXTRA_HAS_ENTRIES = "hasEntries";

    // attributes used as keys when passing values to InstalledAppDetails activity
    public static final String APP_CHG = "chg";

    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;
    private static final int ADVANCED_SETTINGS = 2;

    public static final int SIZE_TOTAL = 0;
    public static final int SIZE_INTERNAL = 1;
    public static final int SIZE_EXTERNAL = 2;

    // Filter options used for displayed list of applications
    // The order which they appear is the order they will show when spinner is present.
    public static final int FILTER_APPS_POWER_WHITELIST         = 0;
    public static final int FILTER_APPS_POWER_WHITELIST_ALL     = 1;
    public static final int FILTER_APPS_ALL                     = 2;
    public static final int FILTER_APPS_ENABLED                 = 3;
    public static final int FILTER_APPS_DISABLED                = 4;
    public static final int FILTER_APPS_BLOCKED                 = 5;
    public static final int FILTER_APPS_PRIORITY                = 6;
    public static final int FILTER_APPS_NO_PEEKING              = 7;
    public static final int FILTER_APPS_SENSITIVE               = 8;
    public static final int FILTER_APPS_PERSONAL                = 9;
    public static final int FILTER_APPS_WORK                    = 10;
    public static final int FILTER_APPS_WITH_DOMAIN_URLS        = 11;
    public static final int FILTER_APPS_USAGE_ACCESS            = 12;
    public static final int FILTER_APPS_WITH_OVERLAY            = 13;
    public static final int FILTER_APPS_WRITE_SETTINGS          = 14;

    // This is the string labels for the filter modes above, the order must be kept in sync.
    public static final int[] FILTER_LABELS = new int[] {
        R.string.high_power_filter_on, // High power whitelist, on
        R.string.filter_all_apps,      // All apps label, but personal filter (for high power);
        R.string.filter_all_apps,      // All apps
        R.string.filter_enabled_apps,  // Enabled
        R.string.filter_apps_disabled, // Disabled
        R.string.filter_notif_blocked_apps,   // Blocked Notifications
        R.string.filter_notif_priority_apps,  // Priority Notifications
        R.string.filter_notif_no_peeking,     // No peeking Notifications
        R.string.filter_notif_sensitive_apps, // Sensitive Notifications
        R.string.filter_personal_apps, // Personal
        R.string.filter_work_apps,     // Work
        R.string.filter_with_domain_urls_apps,     // Domain URLs
        R.string.filter_all_apps,      // Usage access screen, never displayed
        R.string.filter_overlay_apps,   // Apps with overlay permission
        R.string.filter_write_settings_apps,   // Apps that can write system settings
    };
    // This is the actual mapping to filters from FILTER_ constants above, the order must
    // be kept in sync.
    public static final AppFilter[] FILTERS = new AppFilter[] {
        new CompoundFilter(AppStatePowerBridge.FILTER_POWER_WHITELISTED,
                ApplicationsState.FILTER_ALL_ENABLED),     // High power whitelist, on
        new CompoundFilter(ApplicationsState.FILTER_PERSONAL_WITHOUT_DISABLED_UNTIL_USED,
                ApplicationsState.FILTER_ALL_ENABLED),     // All apps label, but personal filter
        ApplicationsState.FILTER_EVERYTHING,  // All apps
        ApplicationsState.FILTER_ALL_ENABLED, // Enabled
        ApplicationsState.FILTER_DISABLED,    // Disabled
        AppStateNotificationBridge.FILTER_APP_NOTIFICATION_BLOCKED,   // Blocked Notifications
        AppStateNotificationBridge.FILTER_APP_NOTIFICATION_PRIORITY,  // Priority Notifications
        AppStateNotificationBridge.FILTER_APP_NOTIFICATION_NO_PEEK,   // No peeking Notifications
        AppStateNotificationBridge.FILTER_APP_NOTIFICATION_SENSITIVE, // Sensitive Notifications
        ApplicationsState.FILTER_PERSONAL,    // Personal
        ApplicationsState.FILTER_WORK,        // Work
        ApplicationsState.FILTER_WITH_DOMAIN_URLS,   // Apps with Domain URLs
        AppStateUsageBridge.FILTER_APP_USAGE, // Apps with Domain URLs
        AppStateOverlayBridge.FILTER_SYSTEM_ALERT_WINDOW,   // Apps that can draw overlays
        AppStateWriteSettingsBridge.FILTER_WRITE_SETTINGS,  // Apps that can write system settings
    };

    // sort order
    private int mSortOrder = R.id.sort_order_alpha;

    // whether showing system apps.
    private boolean mShowSystem;

    private ApplicationsState mApplicationsState;

    public int mListType;
    public int mFilter;

    public ApplicationsAdapter mApplications;

    private View mLoadingContainer;

    private View mListContainer;

    // ListView used to display list
    private ListView mListView;

    // Size resource used for packages whose size computation failed for some reason
    CharSequence mInvalidSizeStr;

    // layout inflater object used to inflate views
    private LayoutInflater mInflater;

    private String mCurrentPkgName;
    private int mCurrentUid;
    private boolean mFinishAfterDialog;

    private Menu mOptionsMenu;

    public static final int LIST_TYPE_MAIN         = 0;
    public static final int LIST_TYPE_NOTIFICATION = 1;
    public static final int LIST_TYPE_DOMAINS_URLS = 2;
    public static final int LIST_TYPE_STORAGE      = 3;
    public static final int LIST_TYPE_USAGE_ACCESS = 4;
    public static final int LIST_TYPE_HIGH_POWER   = 5;
    public static final int LIST_TYPE_OVERLAY      = 6;
    public static final int LIST_TYPE_WRITE_SETTINGS = 7;

    private View mRootView;

    private View mSpinnerHeader;
    private Spinner mFilterSpinner;
    private FilterSpinnerAdapter mFilterAdapter;
    private NotificationBackend mNotifBackend;
    private ResetAppsHelper mResetAppsHelper;
    private String mVolumeUuid;
    private String mVolumeName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());

        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();
        String className = args != null ? args.getString(EXTRA_CLASSNAME) : null;
        if (className == null) {
            className = intent.getComponent().getClassName();
        }
        if (className.equals(AllApplicationsActivity.class.getName())) {
            mShowSystem = true;
        } else if (className.equals(NotificationAppListActivity.class.getName())) {
            mListType = LIST_TYPE_NOTIFICATION;
            mNotifBackend = new NotificationBackend();
        } else if (className.equals(DomainsURLsAppListActivity.class.getName())) {
            mListType = LIST_TYPE_DOMAINS_URLS;
        } else if (className.equals(StorageUseActivity.class.getName())) {
            if (args != null && args.containsKey(EXTRA_VOLUME_UUID)) {
                mVolumeUuid = args.getString(EXTRA_VOLUME_UUID);
                mVolumeName = args.getString(EXTRA_VOLUME_NAME);
                mListType = LIST_TYPE_STORAGE;
            } else {
                // No volume selected, display a normal list, sorted by size.
                mListType = LIST_TYPE_MAIN;
            }
            mSortOrder = R.id.sort_order_size;
        } else if (className.equals(UsageAccessSettingsActivity.class.getName())) {
            mListType = LIST_TYPE_USAGE_ACCESS;
            getActivity().getActionBar().setTitle(R.string.usage_access_title);
        } else if (className.equals(HighPowerApplicationsActivity.class.getName())) {
            mListType = LIST_TYPE_HIGH_POWER;
            // Default to showing system.
            mShowSystem = true;
        } else if (className.equals(OverlaySettingsActivity.class.getName())) {
            mListType = LIST_TYPE_OVERLAY;
            getActivity().getActionBar().setTitle(R.string.system_alert_window_access_title);
        } else if (className.equals(WriteSettingsActivity.class.getName())) {
            mListType = LIST_TYPE_WRITE_SETTINGS;
            getActivity().getActionBar().setTitle(R.string.write_settings_title);
        } else {
            mListType = LIST_TYPE_MAIN;
        }
        mFilter = getDefaultFilter();

        if (savedInstanceState != null) {
            mSortOrder = savedInstanceState.getInt(EXTRA_SORT_ORDER, mSortOrder);
            mShowSystem = savedInstanceState.getBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
        }

        mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);

        mResetAppsHelper = new ResetAppsHelper(getActivity());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // initialize the inflater
        mInflater = inflater;

        mRootView = inflater.inflate(R.layout.manage_applications_apps, null);
        mLoadingContainer = mRootView.findViewById(R.id.loading_container);
        mLoadingContainer.setVisibility(View.VISIBLE);
        mListContainer = mRootView.findViewById(R.id.list_container);
        if (mListContainer != null) {
            // Create adapter and list view here
            View emptyView = mListContainer.findViewById(com.android.internal.R.id.empty);
            ListView lv = (ListView) mListContainer.findViewById(android.R.id.list);
            if (emptyView != null) {
                lv.setEmptyView(emptyView);
            }
            lv.setOnItemClickListener(this);
            lv.setSaveEnabled(true);
            lv.setItemsCanFocus(true);
            lv.setTextFilterEnabled(true);
            mListView = lv;
            mApplications = new ApplicationsAdapter(mApplicationsState, this, mFilter);
            if (savedInstanceState != null) {
                mApplications.mHasReceivedLoadEntries =
                        savedInstanceState.getBoolean(EXTRA_HAS_ENTRIES, false);
            }
            mListView.setAdapter(mApplications);
            mListView.setRecyclerListener(mApplications);

            Utils.prepareCustomPreferencesList(container, mRootView, mListView, false);
        }

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) mRootView.getLayoutParams()).removeBorders = true;
        }

        createHeader();

        mResetAppsHelper.onRestoreInstanceState(savedInstanceState);

        return mRootView;
    }

    private void createHeader() {
        Activity activity = getActivity();
        FrameLayout pinnedHeader = (FrameLayout) mRootView.findViewById(R.id.pinned_header);
        mSpinnerHeader = (ViewGroup) activity.getLayoutInflater()
                .inflate(R.layout.apps_filter_spinner, pinnedHeader, false);
        mFilterSpinner = (Spinner) mSpinnerHeader.findViewById(R.id.filter_spinner);
        mFilterAdapter = new FilterSpinnerAdapter(this);
        mFilterSpinner.setAdapter(mFilterAdapter);
        mFilterSpinner.setOnItemSelectedListener(this);
        pinnedHeader.addView(mSpinnerHeader, 0);

        mFilterAdapter.enableFilter(getDefaultFilter());
        if (mListType == LIST_TYPE_MAIN || mListType == LIST_TYPE_NOTIFICATION) {
            if (UserManager.get(getActivity()).getUserProfiles().size() > 1) {
                mFilterAdapter.enableFilter(FILTER_APPS_PERSONAL);
                mFilterAdapter.enableFilter(FILTER_APPS_WORK);
            }
        }
        if (mListType == LIST_TYPE_NOTIFICATION) {
            mFilterAdapter.enableFilter(FILTER_APPS_BLOCKED);
            mFilterAdapter.enableFilter(FILTER_APPS_PRIORITY);
            mFilterAdapter.enableFilter(FILTER_APPS_SENSITIVE);
            mFilterAdapter.enableFilter(FILTER_APPS_NO_PEEKING);
        }
        if (mListType == LIST_TYPE_HIGH_POWER) {
            mFilterAdapter.enableFilter(FILTER_APPS_POWER_WHITELIST_ALL);
        }
        if (mListType == LIST_TYPE_STORAGE) {
            mApplications.setOverrideFilter(new VolumeFilter(mVolumeUuid));
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mListType == LIST_TYPE_STORAGE) {
            FrameLayout pinnedHeader = (FrameLayout) mRootView.findViewById(R.id.pinned_header);
            AppHeader.createAppHeader(getActivity(), null, mVolumeName, null, pinnedHeader);
        }
    }

    private int getDefaultFilter() {
        switch (mListType) {
            case LIST_TYPE_DOMAINS_URLS:
                return FILTER_APPS_WITH_DOMAIN_URLS;
            case LIST_TYPE_USAGE_ACCESS:
                return FILTER_APPS_USAGE_ACCESS;
            case LIST_TYPE_HIGH_POWER:
                return FILTER_APPS_POWER_WHITELIST;
            case LIST_TYPE_OVERLAY:
                return FILTER_APPS_WITH_OVERLAY;
            case LIST_TYPE_WRITE_SETTINGS:
                return FILTER_APPS_WRITE_SETTINGS;
            default:
                return FILTER_APPS_ALL;
        }
    }

    @Override
    protected int getMetricsCategory() {
        switch (mListType) {
            case LIST_TYPE_MAIN:
                return MetricsLogger.MANAGE_APPLICATIONS;
            case LIST_TYPE_NOTIFICATION:
                return MetricsLogger.MANAGE_APPLICATIONS_NOTIFICATIONS;
            case LIST_TYPE_DOMAINS_URLS:
                return MetricsLogger.MANAGE_DOMAIN_URLS;
            case LIST_TYPE_STORAGE:
                return MetricsLogger.APPLICATIONS_STORAGE_APPS;
            case LIST_TYPE_USAGE_ACCESS:
                return MetricsLogger.USAGE_ACCESS;
            case LIST_TYPE_HIGH_POWER:
                return MetricsLogger.APPLICATIONS_HIGH_POWER_APPS;
            case LIST_TYPE_OVERLAY:
                return MetricsLogger.SYSTEM_ALERT_WINDOW_APPS;
            case LIST_TYPE_WRITE_SETTINGS:
                return MetricsLogger.SYSTEM_ALERT_WINDOW_APPS;
            default:
                return MetricsLogger.VIEW_UNKNOWN;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
        updateOptionsMenu();
        if (mApplications != null) {
            mApplications.resume(mSortOrder);
            mApplications.updateLoading();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mResetAppsHelper.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SORT_ORDER, mSortOrder);
        outState.putBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
        outState.putBoolean(EXTRA_HAS_ENTRIES, mApplications.mHasReceivedLoadEntries);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mApplications != null) {
            mApplications.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mResetAppsHelper.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mApplications != null) {
            mApplications.release();
        }
        mRootView = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INSTALLED_APP_DETAILS && mCurrentPkgName != null) {
            if (mListType == LIST_TYPE_NOTIFICATION) {
                mApplications.mExtraInfoBridge.forceUpdate(mCurrentPkgName, mCurrentUid);
            } else if (mListType == LIST_TYPE_HIGH_POWER || mListType == LIST_TYPE_OVERLAY
                    || mListType == LIST_TYPE_WRITE_SETTINGS) {
                if (mFinishAfterDialog) {
                    getActivity().onBackPressed();
                } else {
                    mApplications.mExtraInfoBridge.forceUpdate(mCurrentPkgName, mCurrentUid);
                }
            } else {
                mApplicationsState.requestSize(mCurrentPkgName, UserHandle.getUserId(mCurrentUid));
            }
        }
    }

    // utility method used to start sub activity
    private void startApplicationDetailsActivity() {
        switch (mListType) {
            case LIST_TYPE_NOTIFICATION:
                startAppInfoFragment(AppNotificationSettings.class,
                        R.string.app_notifications_title);
                break;
            case LIST_TYPE_DOMAINS_URLS:
                startAppInfoFragment(AppLaunchSettings.class, R.string.auto_launch_label);
                break;
            case LIST_TYPE_USAGE_ACCESS:
                startAppInfoFragment(UsageAccessDetails.class, R.string.usage_access);
                break;
            case LIST_TYPE_STORAGE:
                startAppInfoFragment(AppStorageSettings.class, R.string.storage_settings);
                break;
            case LIST_TYPE_HIGH_POWER:
                HighPowerDetail.show(this, mCurrentPkgName, INSTALLED_APP_DETAILS,
                        mFinishAfterDialog);
                break;
            case LIST_TYPE_OVERLAY:
                startAppInfoFragment(DrawOverlayDetails.class, R.string.overlay_settings);
                break;
            case LIST_TYPE_WRITE_SETTINGS:
                startAppInfoFragment(WriteSettingsDetails.class, R.string.write_system_settings);
                break;
            // TODO: Figure out if there is a way where we can spin up the profile's settings
            // process ahead of time, to avoid a long load of data when user clicks on a managed app.
            // Maybe when they load the list of apps that contains managed profile apps.
            default:
                startAppInfoFragment(InstalledAppDetails.class, R.string.application_info_label);
                break;
        }
    }

    private void startAppInfoFragment(Class<?> fragment, int titleRes) {
        AppInfoBase.startAppInfoFragment(fragment, titleRes, mCurrentPkgName, mCurrentUid, this,
                INSTALLED_APP_DETAILS);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mListType == LIST_TYPE_DOMAINS_URLS) {
            return;
        }
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, mListType == LIST_TYPE_MAIN
                ? R.string.help_uri_apps : R.string.help_uri_notifications, getClass().getName());
        mOptionsMenu = menu;
        inflater.inflate(R.menu.manage_apps, menu);
        updateOptionsMenu();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    @Override
    public void onDestroyOptionsMenu() {
        mOptionsMenu = null;
    }

    void updateOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }
        mOptionsMenu.findItem(R.id.advanced).setVisible(mListType == LIST_TYPE_MAIN);

        mOptionsMenu.findItem(R.id.sort_order_alpha).setVisible(mListType == LIST_TYPE_STORAGE
                && mSortOrder != R.id.sort_order_alpha);
        mOptionsMenu.findItem(R.id.sort_order_size).setVisible(mListType == LIST_TYPE_STORAGE
                && mSortOrder != R.id.sort_order_size);

        mOptionsMenu.findItem(R.id.show_system).setVisible(!mShowSystem
                && mListType != LIST_TYPE_HIGH_POWER);
        mOptionsMenu.findItem(R.id.hide_system).setVisible(mShowSystem
                && mListType != LIST_TYPE_HIGH_POWER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        switch(item.getItemId()) {
            case R.id.sort_order_alpha:
            case R.id.sort_order_size:
                mSortOrder = menuId;
                if (mApplications != null) {
                    mApplications.rebuild(mSortOrder);
                }
                break;
            case R.id.show_system:
            case R.id.hide_system:
                mShowSystem = !mShowSystem;
                mApplications.rebuild(false);
                break;
            case R.id.reset_app_preferences:
                mResetAppsHelper.buildResetDialog();
                return true;
            case R.id.advanced:
                ((SettingsActivity) getActivity()).startPreferencePanel(
                        AdvancedAppSettings.class.getName(), null, R.string.configure_apps,
                        null, this, ADVANCED_SETTINGS);
                return true;
            default:
                // Handle the home button
                return false;
        }
        updateOptionsMenu();
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mApplications != null && mApplications.getCount() > position) {
            ApplicationsState.AppEntry entry = mApplications.getAppEntry(position);
            mCurrentPkgName = entry.info.packageName;
            mCurrentUid = entry.info.uid;
            startApplicationDetailsActivity();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mFilter = mFilterAdapter.getFilter(position);
        mApplications.setFilter(mFilter);
        if (DEBUG) Log.d(TAG, "Selecting filter " + mFilter);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void updateView() {
        updateOptionsMenu();
        final Activity host = getActivity();
        if (host != null) {
            host.invalidateOptionsMenu();
        }
    }

    public void setHasDisabled(boolean hasDisabledApps) {
        if (mListType == LIST_TYPE_HIGH_POWER) {
            return;
        }
        mFilterAdapter.setFilterEnabled(FILTER_APPS_ENABLED, hasDisabledApps);
        mFilterAdapter.setFilterEnabled(FILTER_APPS_DISABLED, hasDisabledApps);
    }

    static class FilterSpinnerAdapter extends ArrayAdapter<CharSequence> {

        private final ManageApplications mManageApplications;

        // Use ArrayAdapter for view logic, but have our own list for managing
        // the options available.
        private final ArrayList<Integer> mFilterOptions = new ArrayList<>();

        public FilterSpinnerAdapter(ManageApplications manageApplications) {
            super(manageApplications.getActivity(), R.layout.filter_spinner_item);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mManageApplications = manageApplications;
        }

        public int getFilter(int position) {
            return mFilterOptions.get(position);
        }

        public void setFilterEnabled(int filter, boolean enabled) {
            if (enabled) {
                enableFilter(filter);
            } else {
                disableFilter(filter);
            }
        }

        public void enableFilter(int filter) {
            if (mFilterOptions.contains(filter)) return;
            if (DEBUG) Log.d(TAG, "Enabling filter " + filter);
            mFilterOptions.add(filter);
            Collections.sort(mFilterOptions);
            mManageApplications.mSpinnerHeader.setVisibility(
                    mFilterOptions.size() > 1 ? View.VISIBLE : View.GONE);
            notifyDataSetChanged();
            if (mFilterOptions.size() == 1) {
                if (DEBUG) Log.d(TAG, "Auto selecting filter " + filter);
                mManageApplications.mFilterSpinner.setSelection(0);
                mManageApplications.onItemSelected(null, null, 0, 0);
            }
        }

        public void disableFilter(int filter) {
            if (!mFilterOptions.remove((Integer) filter)) {
                return;
            }
            if (DEBUG) Log.d(TAG, "Disabling filter " + filter);
            Collections.sort(mFilterOptions);
            mManageApplications.mSpinnerHeader.setVisibility(
                    mFilterOptions.size() > 1 ? View.VISIBLE : View.GONE);
            notifyDataSetChanged();
            if (mManageApplications.mFilter == filter) {
                if (mFilterOptions.size() > 0) {
                    if (DEBUG) Log.d(TAG, "Auto selecting filter " + mFilterOptions.get(0));
                    mManageApplications.mFilterSpinner.setSelection(0);
                    mManageApplications.onItemSelected(null, null, 0, 0);
                }
            }
        }

        @Override
        public int getCount() {
            return mFilterOptions.size();
        }

        @Override
        public CharSequence getItem(int position) {
            return getFilterString(mFilterOptions.get(position));
        }

        private CharSequence getFilterString(int filter) {
            return mManageApplications.getString(FILTER_LABELS[filter]);
        }

    }

    /*
     * Custom adapter implementation for the ListView
     * This adapter maintains a map for each displayed application and its properties
     * An index value on each AppInfo object indicates the correct position or index
     * in the list. If the list gets updated dynamically when the user is viewing the list of
     * applications, we need to return the correct index of position. This is done by mapping
     * the getId methods via the package name into the internal maps and indices.
     * The order of applications in the list is mirrored in mAppLocalList
     */
    static class ApplicationsAdapter extends BaseAdapter implements Filterable,
            ApplicationsState.Callbacks, AppStateBaseBridge.Callback,
            AbsListView.RecyclerListener {
        private final ApplicationsState mState;
        private final ApplicationsState.Session mSession;
        private final ManageApplications mManageApplications;
        private final Context mContext;
        private final ArrayList<View> mActive = new ArrayList<View>();
        private final AppStateBaseBridge mExtraInfoBridge;
        private int mFilterMode;
        private ArrayList<ApplicationsState.AppEntry> mBaseEntries;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private boolean mResumed;
        private int mLastSortMode=-1;
        private int mWhichSize = SIZE_TOTAL;
        CharSequence mCurFilterPrefix;
        private PackageManager mPm;
        private AppFilter mOverrideFilter;
        private boolean mHasReceivedLoadEntries;
        private boolean mHasReceivedBridgeCallback;

        private Filter mFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<ApplicationsState.AppEntry> entries
                        = applyPrefixFilter(constraint, mBaseEntries);
                FilterResults fr = new FilterResults();
                fr.values = entries;
                fr.count = entries.size();
                return fr;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mCurFilterPrefix = constraint;
                mEntries = (ArrayList<ApplicationsState.AppEntry>) results.values;
                notifyDataSetChanged();
            }
        };

        public ApplicationsAdapter(ApplicationsState state, ManageApplications manageApplications,
                int filterMode) {
            mState = state;
            mSession = state.newSession(this);
            mManageApplications = manageApplications;
            mContext = manageApplications.getActivity();
            mPm = mContext.getPackageManager();
            mFilterMode = filterMode;
            if (mManageApplications.mListType == LIST_TYPE_NOTIFICATION) {
                mExtraInfoBridge = new AppStateNotificationBridge(mContext.getPackageManager(),
                        mState, this, manageApplications.mNotifBackend);
            } else if (mManageApplications.mListType == LIST_TYPE_USAGE_ACCESS) {
                mExtraInfoBridge = new AppStateUsageBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_HIGH_POWER) {
                mExtraInfoBridge = new AppStatePowerBridge(mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_OVERLAY) {
                mExtraInfoBridge = new AppStateOverlayBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_WRITE_SETTINGS) {
                mExtraInfoBridge = new AppStateWriteSettingsBridge(mContext, mState, this);
            } else {
                mExtraInfoBridge = null;
            }
        }

        public void setOverrideFilter(AppFilter overrideFilter) {
            mOverrideFilter = overrideFilter;
            rebuild(true);
        }

        public void setFilter(int filter) {
            mFilterMode = filter;
            rebuild(true);
        }

        public void resume(int sort) {
            if (DEBUG) Log.i(TAG, "Resume!  mResumed=" + mResumed);
            if (!mResumed) {
                mResumed = true;
                mSession.resume();
                mLastSortMode = sort;
                if (mExtraInfoBridge != null) {
                    mExtraInfoBridge.resume();
                }
                rebuild(true);
            } else {
                rebuild(sort);
            }
        }

        public void pause() {
            if (mResumed) {
                mResumed = false;
                mSession.pause();
                if (mExtraInfoBridge != null) {
                    mExtraInfoBridge.pause();
                }
            }
        }

        public void release() {
            mSession.release();
            if (mExtraInfoBridge != null) {
                mExtraInfoBridge.release();
            }
        }

        public void rebuild(int sort) {
            if (sort == mLastSortMode) {
                return;
            }
            mLastSortMode = sort;
            rebuild(true);
        }

        public void rebuild(boolean eraseold) {
            if (!mHasReceivedLoadEntries
                    && (mExtraInfoBridge == null || mHasReceivedBridgeCallback)) {
                // Don't rebuild the list until all the app entries are loaded.
                return;
            }
            if (DEBUG) Log.i(TAG, "Rebuilding app list...");
            ApplicationsState.AppFilter filterObj;
            Comparator<AppEntry> comparatorObj;
            boolean emulated = Environment.isExternalStorageEmulated();
            if (emulated) {
                mWhichSize = SIZE_TOTAL;
            } else {
                mWhichSize = SIZE_INTERNAL;
            }
            filterObj = FILTERS[mFilterMode];
            if (mOverrideFilter != null) {
                filterObj = mOverrideFilter;
            }
            if (!mManageApplications.mShowSystem) {
                filterObj = new CompoundFilter(filterObj,
                        ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER);
            }
            switch (mLastSortMode) {
                case R.id.sort_order_size:
                    switch (mWhichSize) {
                        case SIZE_INTERNAL:
                            comparatorObj = ApplicationsState.INTERNAL_SIZE_COMPARATOR;
                            break;
                        case SIZE_EXTERNAL:
                            comparatorObj = ApplicationsState.EXTERNAL_SIZE_COMPARATOR;
                            break;
                        default:
                            comparatorObj = ApplicationsState.SIZE_COMPARATOR;
                            break;
                    }
                    break;
                default:
                    comparatorObj = ApplicationsState.ALPHA_COMPARATOR;
                    break;
            }
            ArrayList<ApplicationsState.AppEntry> entries
                    = mSession.rebuild(filterObj, comparatorObj);
            if (entries == null && !eraseold) {
                // Don't have new list yet, but can continue using the old one.
                return;
            }
            mBaseEntries = entries;
            if (mBaseEntries != null) {
                mEntries = applyPrefixFilter(mCurFilterPrefix, mBaseEntries);
            } else {
                mEntries = null;
            }
            notifyDataSetChanged();

            if (mSession.getAllApps().size() != 0
                    && mManageApplications.mListContainer.getVisibility() != View.VISIBLE) {
                Utils.handleLoadingContainer(mManageApplications.mLoadingContainer,
                        mManageApplications.mListContainer, true, true);
            }
            if (mManageApplications.mListType == LIST_TYPE_USAGE_ACCESS) {
                // No enabled or disabled filters for usage access.
                return;
            }

            mManageApplications.setHasDisabled(mState.haveDisabledApps());
        }

        private void updateLoading() {
            Utils.handleLoadingContainer(mManageApplications.mLoadingContainer,
                    mManageApplications.mListContainer,
                    mHasReceivedLoadEntries && mSession.getAllApps().size() != 0, false);
        }

        ArrayList<ApplicationsState.AppEntry> applyPrefixFilter(CharSequence prefix,
                ArrayList<ApplicationsState.AppEntry> origEntries) {
            if (prefix == null || prefix.length() == 0) {
                return origEntries;
            } else {
                String prefixStr = ApplicationsState.normalize(prefix.toString());
                final String spacePrefixStr = " " + prefixStr;
                ArrayList<ApplicationsState.AppEntry> newEntries
                        = new ArrayList<ApplicationsState.AppEntry>();
                for (int i=0; i<origEntries.size(); i++) {
                    ApplicationsState.AppEntry entry = origEntries.get(i);
                    String nlabel = entry.getNormalizedLabel();
                    if (nlabel.startsWith(prefixStr) || nlabel.indexOf(spacePrefixStr) != -1) {
                        newEntries.add(entry);
                    }
                }
                return newEntries;
            }
        }

        @Override
        public void onExtraInfoUpdated() {
            mHasReceivedBridgeCallback = true;
            rebuild(false);
        }

        @Override
        public void onRunningStateChanged(boolean running) {
            mManageApplications.getActivity().setProgressBarIndeterminateVisibility(running);
        }

        @Override
        public void onRebuildComplete(ArrayList<AppEntry> apps) {
            if (mManageApplications.mLoadingContainer.getVisibility() == View.VISIBLE) {
                mManageApplications.mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                        mContext, android.R.anim.fade_out));
                mManageApplications.mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        mContext, android.R.anim.fade_in));
            }
            mManageApplications.mListContainer.setVisibility(View.VISIBLE);
            mManageApplications.mLoadingContainer.setVisibility(View.GONE);
            mBaseEntries = apps;
            mEntries = applyPrefixFilter(mCurFilterPrefix, mBaseEntries);
            notifyDataSetChanged();
        }

        @Override
        public void onPackageListChanged() {
            rebuild(false);
        }

        @Override
        public void onPackageIconChanged() {
            // We ensure icons are loaded when their item is displayed, so
            // don't care about icons loaded in the background.
        }

        @Override
        public void onLoadEntriesCompleted() {
            mHasReceivedLoadEntries = true;
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            for (int i=0; i<mActive.size(); i++) {
                AppViewHolder holder = (AppViewHolder)mActive.get(i).getTag();
                if (holder.entry.info.packageName.equals(packageName)) {
                    synchronized (holder.entry) {
                        updateSummary(holder);
                    }
                    if (holder.entry.info.packageName.equals(mManageApplications.mCurrentPkgName)
                            && mLastSortMode == R.id.sort_order_size) {
                        // We got the size information for the last app the
                        // user viewed, and are sorting by size...  they may
                        // have cleared data, so we immediately want to resort
                        // the list with the new size to reflect it to the user.
                        rebuild(false);
                    }
                    return;
                }
            }
        }

        @Override
        public void onLauncherInfoChanged() {
            if (!mManageApplications.mShowSystem) {
                rebuild(false);
            }
        }

        @Override
        public void onAllSizesComputed() {
            if (mLastSortMode == R.id.sort_order_size) {
                rebuild(false);
            }
        }

        public int getCount() {
            return mEntries != null ? mEntries.size() : 0;
        }

        public Object getItem(int position) {
            return mEntries.get(position);
        }

        public ApplicationsState.AppEntry getAppEntry(int position) {
            return mEntries.get(position);
        }

        public long getItemId(int position) {
            return mEntries.get(position).id;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            if (mManageApplications.mListType != LIST_TYPE_HIGH_POWER) {
                return true;
            }
            ApplicationsState.AppEntry entry = mEntries.get(position);
            return !PowerWhitelistBackend.getInstance().isSysWhitelisted(entry.info.packageName);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unnecessary calls
            // to findViewById() on each row.
            AppViewHolder holder = AppViewHolder.createOrRecycle(mManageApplications.mInflater,
                    convertView);
            convertView = holder.rootView;

            // Bind the data efficiently with the holder
            ApplicationsState.AppEntry entry = mEntries.get(position);
            synchronized (entry) {
                holder.entry = entry;
                if (entry.label != null) {
                    holder.appName.setText(entry.label);
                }
                mState.ensureIcon(entry);
                if (entry.icon != null) {
                    holder.appIcon.setImageDrawable(entry.icon);
                }
                updateSummary(holder);
                if ((entry.info.flags&ApplicationInfo.FLAG_INSTALLED) == 0) {
                    holder.disabled.setVisibility(View.VISIBLE);
                    holder.disabled.setText(R.string.not_installed);
                } else if (!entry.info.enabled) {
                    holder.disabled.setVisibility(View.VISIBLE);
                    holder.disabled.setText(R.string.disabled);
                } else {
                    holder.disabled.setVisibility(View.GONE);
                }
            }
            mActive.remove(convertView);
            mActive.add(convertView);
            convertView.setEnabled(isEnabled(position));
            return convertView;
        }

        private void updateSummary(AppViewHolder holder) {
            switch (mManageApplications.mListType) {
                case LIST_TYPE_NOTIFICATION:
                    if (holder.entry.extraInfo != null) {
                        holder.summary.setText(InstalledAppDetails.getNotificationSummary(
                                (AppRow) holder.entry.extraInfo, mContext));
                    } else {
                        holder.summary.setText(null);
                    }
                    break;

                case LIST_TYPE_DOMAINS_URLS:
                    holder.summary.setText(getDomainsSummary(holder.entry.info.packageName));
                    break;

                case LIST_TYPE_USAGE_ACCESS:
                    if (holder.entry.extraInfo != null) {
                        holder.summary.setText((new UsageState((PermissionState)holder.entry
                                .extraInfo)).isPermissible() ? R.string.switch_on_text :
                                R.string.switch_off_text);
                    } else {
                        holder.summary.setText(null);
                    }
                    break;

                case LIST_TYPE_HIGH_POWER:
                    holder.summary.setText(HighPowerDetail.getSummary(mContext, holder.entry));
                    break;

                case LIST_TYPE_OVERLAY:
                    holder.summary.setText(DrawOverlayDetails.getSummary(mContext, holder.entry));
                    break;

                case LIST_TYPE_WRITE_SETTINGS:
                    holder.summary.setText(WriteSettingsDetails.getSummary(mContext,
                            holder.entry));
                    break;

                default:
                    holder.updateSizeText(mManageApplications.mInvalidSizeStr, mWhichSize);
                    break;
            }
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        @Override
        public void onMovedToScrapHeap(View view) {
            mActive.remove(view);
        }

        private CharSequence getDomainsSummary(String packageName) {
            // If the user has explicitly said "no" for this package, that's the
            // string we should show.
            int domainStatus = mPm.getIntentVerificationStatus(packageName, UserHandle.myUserId());
            if (domainStatus == PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER) {
                return mContext.getString(R.string.domain_urls_summary_none);
            }
            // Otherwise, ask package manager for the domains for this package,
            // and show the first one (or none if there aren't any).
            ArraySet<String> result = Utils.getHandledDomains(mPm, packageName);
            if (result.size() == 0) {
                return mContext.getString(R.string.domain_urls_summary_none);
            } else if (result.size() == 1) {
                return mContext.getString(R.string.domain_urls_summary_one, result.valueAt(0));
            } else {
                return mContext.getString(R.string.domain_urls_summary_some, result.valueAt(0));
            }
        }
    }
}

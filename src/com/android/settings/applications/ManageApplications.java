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

import android.annotation.IdRes;
import android.annotation.Nullable;
import android.annotation.StringRes;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.icu.text.AlphabeticIndex;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.LocaleList;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceFrameLayout;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.SectionIndexer;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Settings.AllApplicationsActivity;
import com.android.settings.Settings.GamesStorageActivity;
import com.android.settings.Settings.HighPowerApplicationsActivity;
import com.android.settings.Settings.ManageExternalSourcesActivity;
import com.android.settings.Settings.MoviesStorageActivity;
import com.android.settings.Settings.NotificationAppListActivity;
import com.android.settings.Settings.OverlaySettingsActivity;
import com.android.settings.Settings.StorageUseActivity;
import com.android.settings.Settings.UsageAccessSettingsActivity;
import com.android.settings.Settings.WriteSettingsActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateUsageBridge.UsageState;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.fuelgauge.HighPowerDetail;
import com.android.settings.fuelgauge.PowerWhitelistBackend;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.applications.ApplicationsState.CompoundFilter;
import com.android.settingslib.applications.ApplicationsState.VolumeFilter;
import com.android.settingslib.applications.StorageStatsSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

/**
 * Activity to pick an application that will be used to display installation information and
 * options to uninstall/delete user data for system applications. This activity
 * can be launched through Settings or via the ACTION_MANAGE_PACKAGE_STORAGE
 * intent.
 */
public class ManageApplications extends InstrumentedPreferenceFragment
        implements OnItemClickListener, OnItemSelectedListener {

    static final String TAG = "ManageApplications";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    // Intent extras.
    public static final String EXTRA_CLASSNAME = "classname";
    // Used for storage only.
    public static final String EXTRA_VOLUME_UUID = "volumeUuid";
    public static final String EXTRA_VOLUME_NAME = "volumeName";
    public static final String EXTRA_STORAGE_TYPE = "storageType";
    public static final String EXTRA_WORK_ONLY = "workProfileOnly";
    public static final String EXTRA_WORK_ID = "workId";

    private static final String EXTRA_SORT_ORDER = "sortOrder";
    private static final String EXTRA_SHOW_SYSTEM = "showSystem";
    private static final String EXTRA_HAS_ENTRIES = "hasEntries";
    private static final String EXTRA_HAS_BRIDGE = "hasBridge";

    // attributes used as keys when passing values to InstalledAppDetails activity
    public static final String APP_CHG = "chg";

    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;
    private static final int ADVANCED_SETTINGS = 2;

    public static final int SIZE_TOTAL = 0;
    public static final int SIZE_INTERNAL = 1;
    public static final int SIZE_EXTERNAL = 2;

    // Filter options used for displayed list of applications
    // Filters will appear sorted based on their value defined here.
    public static final int FILTER_APPS_POWER_WHITELIST = 0;
    public static final int FILTER_APPS_POWER_WHITELIST_ALL = 1;
    public static final int FILTER_APPS_ALL = 2;
    public static final int FILTER_APPS_ENABLED = 3;
    public static final int FILTER_APPS_INSTANT = 4;
    public static final int FILTER_APPS_DISABLED = 5;
    public static final int FILTER_APPS_BLOCKED = 6;
    public static final int FILTER_APPS_PERSONAL = 7;
    public static final int FILTER_APPS_WORK = 8;
    public static final int FILTER_APPS_USAGE_ACCESS = 9;
    public static final int FILTER_APPS_WITH_OVERLAY = 10;
    public static final int FILTER_APPS_WRITE_SETTINGS = 11;
    public static final int FILTER_APPS_INSTALL_SOURCES = 12;
    public static final int FILTER_APPS_COUNT = 13;  // This should always be the last entry

    // Mapping to string labels for the FILTER_APPS_* constants above.
    @IdRes
    public static final int[] FILTER_LABELS = new int[FILTER_APPS_COUNT];

    // Mapping to filters for the FILTER_APPS_* constants above.
    public static final AppFilter[] FILTERS = new AppFilter[FILTER_APPS_COUNT];

    static {
        // High power whitelist, on
        FILTER_LABELS[FILTER_APPS_POWER_WHITELIST] = R.string.high_power_filter_on;
        FILTERS[FILTER_APPS_POWER_WHITELIST] = new CompoundFilter(
                AppStatePowerBridge.FILTER_POWER_WHITELISTED,
                ApplicationsState.FILTER_ALL_ENABLED);

        // Without disabled until used
        FILTER_LABELS[FILTER_APPS_POWER_WHITELIST_ALL] = R.string.filter_all_apps;
        FILTERS[FILTER_APPS_POWER_WHITELIST_ALL] = new CompoundFilter(
                ApplicationsState.FILTER_WITHOUT_DISABLED_UNTIL_USED,
                ApplicationsState.FILTER_ALL_ENABLED);

        // All apps
        FILTER_LABELS[FILTER_APPS_ALL] = R.string.filter_all_apps;
        FILTERS[FILTER_APPS_ALL] = ApplicationsState.FILTER_EVERYTHING;

        // Enabled
        FILTER_LABELS[FILTER_APPS_ENABLED] = R.string.filter_enabled_apps;
        FILTERS[FILTER_APPS_ENABLED] = ApplicationsState.FILTER_ALL_ENABLED;

        // Disabled
        FILTER_LABELS[FILTER_APPS_DISABLED] = R.string.filter_apps_disabled;
        FILTERS[FILTER_APPS_DISABLED] = ApplicationsState.FILTER_DISABLED;

        // Instant
        FILTER_LABELS[FILTER_APPS_INSTANT] = R.string.filter_instant_apps;
        FILTERS[FILTER_APPS_INSTANT] = ApplicationsState.FILTER_INSTANT;

        // Blocked Notifications
        FILTER_LABELS[FILTER_APPS_BLOCKED] = R.string.filter_notif_blocked_apps;
        FILTERS[FILTER_APPS_BLOCKED] = AppStateNotificationBridge.FILTER_APP_NOTIFICATION_BLOCKED;

        // Personal
        FILTER_LABELS[FILTER_APPS_PERSONAL] = R.string.filter_personal_apps;
        FILTERS[FILTER_APPS_PERSONAL] = ApplicationsState.FILTER_PERSONAL;

        // Work
        FILTER_LABELS[FILTER_APPS_WORK] = R.string.filter_work_apps;
        FILTERS[FILTER_APPS_WORK] = ApplicationsState.FILTER_WORK;

        // Usage access screen, never displayed.
        FILTER_LABELS[FILTER_APPS_USAGE_ACCESS] = R.string.filter_all_apps;
        FILTERS[FILTER_APPS_USAGE_ACCESS] = AppStateUsageBridge.FILTER_APP_USAGE;

        // Apps that can draw overlays
        FILTER_LABELS[FILTER_APPS_WITH_OVERLAY] = R.string.filter_overlay_apps;
        FILTERS[FILTER_APPS_WITH_OVERLAY] = AppStateOverlayBridge.FILTER_SYSTEM_ALERT_WINDOW;

        // Apps that can write system settings
        FILTER_LABELS[FILTER_APPS_WRITE_SETTINGS] = R.string.filter_write_settings_apps;
        FILTERS[FILTER_APPS_WRITE_SETTINGS] = AppStateWriteSettingsBridge.FILTER_WRITE_SETTINGS;

        // Apps that are trusted sources of apks
        FILTER_LABELS[FILTER_APPS_INSTALL_SOURCES] = R.string.filter_install_sources_apps;
        FILTERS[FILTER_APPS_INSTALL_SOURCES] = AppStateInstallAppsBridge.FILTER_APP_SOURCES;
    }

    // Storage types. Used to determine what the extra item in the list of preferences is.
    public static final int STORAGE_TYPE_DEFAULT = 0; // Show all apps that are not categorized.
    public static final int STORAGE_TYPE_MUSIC = 1;
    public static final int STORAGE_TYPE_LEGACY = 2; // Show apps even if they can be categorized.
    public static final int STORAGE_TYPE_PHOTOS_VIDEOS = 3;

    private static final int NO_USER_SPECIFIED = -1;

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

    public static final int LIST_TYPE_MAIN = 0;
    public static final int LIST_TYPE_NOTIFICATION = 1;
    public static final int LIST_TYPE_STORAGE = 3;
    public static final int LIST_TYPE_USAGE_ACCESS = 4;
    public static final int LIST_TYPE_HIGH_POWER = 5;
    public static final int LIST_TYPE_OVERLAY = 6;
    public static final int LIST_TYPE_WRITE_SETTINGS = 7;
    public static final int LIST_TYPE_MANAGE_SOURCES = 8;
    public static final int LIST_TYPE_GAMES = 9;
    public static final int LIST_TYPE_MOVIES = 10;
    public static final int LIST_TYPE_PHOTOGRAPHY = 11;


    // List types that should show instant apps.
    public static final Set<Integer> LIST_TYPES_WITH_INSTANT = new ArraySet<>(Arrays.asList(
            LIST_TYPE_MAIN,
            LIST_TYPE_STORAGE));

    private View mRootView;

    private View mSpinnerHeader;
    private Spinner mFilterSpinner;
    private FilterSpinnerAdapter mFilterAdapter;
    private NotificationBackend mNotifBackend;
    private ResetAppsHelper mResetAppsHelper;
    private String mVolumeUuid;
    private int mStorageType;
    private boolean mIsWorkOnly;
    private int mWorkUserId;

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
        } else if (className.equals(NotificationAppListActivity.class.getName())
                || this instanceof NotificationApps) {
            mListType = LIST_TYPE_NOTIFICATION;
            mNotifBackend = new NotificationBackend();
        } else if (className.equals(StorageUseActivity.class.getName())) {
            if (args != null && args.containsKey(EXTRA_VOLUME_UUID)) {
                mVolumeUuid = args.getString(EXTRA_VOLUME_UUID);
                mStorageType = args.getInt(EXTRA_STORAGE_TYPE, STORAGE_TYPE_DEFAULT);
                mListType = LIST_TYPE_STORAGE;
            } else {
                // No volume selected, display a normal list, sorted by size.
                mListType = LIST_TYPE_MAIN;
            }
            mSortOrder = R.id.sort_order_size;
        } else if (className.equals(UsageAccessSettingsActivity.class.getName())) {
            mListType = LIST_TYPE_USAGE_ACCESS;
        } else if (className.equals(HighPowerApplicationsActivity.class.getName())) {
            mListType = LIST_TYPE_HIGH_POWER;
            // Default to showing system.
            mShowSystem = true;
        } else if (className.equals(OverlaySettingsActivity.class.getName())) {
            mListType = LIST_TYPE_OVERLAY;
        } else if (className.equals(WriteSettingsActivity.class.getName())) {
            mListType = LIST_TYPE_WRITE_SETTINGS;
        } else if (className.equals(ManageExternalSourcesActivity.class.getName())) {
            mListType = LIST_TYPE_MANAGE_SOURCES;
        } else if (className.equals(GamesStorageActivity.class.getName())) {
            mListType = LIST_TYPE_GAMES;
            mSortOrder = R.id.sort_order_size;
        } else if (className.equals(MoviesStorageActivity.class.getName())) {
            mListType = LIST_TYPE_MOVIES;
            mSortOrder = R.id.sort_order_size;
        } else if (className.equals(Settings.PhotosStorageActivity.class.getName())) {
            mListType = LIST_TYPE_PHOTOGRAPHY;
            mSortOrder = R.id.sort_order_size;
            mStorageType = args.getInt(EXTRA_STORAGE_TYPE, STORAGE_TYPE_DEFAULT);
        } else {
            mListType = LIST_TYPE_MAIN;
        }
        mFilter = getDefaultFilter();
        mIsWorkOnly = args != null ? args.getBoolean(EXTRA_WORK_ONLY) : false;
        mWorkUserId = args != null ? args.getInt(EXTRA_WORK_ID) : NO_USER_SPECIFIED;

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
                mApplications.mHasReceivedBridgeCallback =
                        savedInstanceState.getBoolean(EXTRA_HAS_BRIDGE, false);
            }
            int userId = mIsWorkOnly ? mWorkUserId : UserHandle.getUserId(mCurrentUid);
            if (mStorageType == STORAGE_TYPE_MUSIC) {
                Context context = getContext();
                mApplications.setExtraViewController(
                        new MusicViewHolderController(
                                context,
                                new StorageStatsSource(context),
                                mVolumeUuid,
                                UserHandle.of(userId)));
            } else if (mStorageType == STORAGE_TYPE_PHOTOS_VIDEOS) {
                Context context = getContext();
                mApplications.setExtraViewController(
                        new PhotosViewHolderController(
                                context,
                                new StorageStatsSource(context),
                                mVolumeUuid,
                                UserHandle.of(userId)));
            }
            mListView.setAdapter(mApplications);
            mListView.setRecyclerListener(mApplications);
            mListView.setFastScrollEnabled(isFastScrollEnabled());

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

    @VisibleForTesting
    void createHeader() {
        Activity activity = getActivity();
        FrameLayout pinnedHeader = (FrameLayout) mRootView.findViewById(R.id.pinned_header);
        mSpinnerHeader = activity.getLayoutInflater()
                .inflate(R.layout.apps_filter_spinner, pinnedHeader, false);
        mFilterSpinner = (Spinner) mSpinnerHeader.findViewById(R.id.filter_spinner);
        mFilterAdapter = new FilterSpinnerAdapter(this);
        mFilterSpinner.setAdapter(mFilterAdapter);
        mFilterSpinner.setOnItemSelectedListener(this);
        pinnedHeader.addView(mSpinnerHeader, 0);

        mFilterAdapter.enableFilter(getDefaultFilter());
        if (mListType == LIST_TYPE_MAIN) {
            if (UserManager.get(getActivity()).getUserProfiles().size() > 1) {
                mFilterAdapter.enableFilter(FILTER_APPS_PERSONAL);
                mFilterAdapter.enableFilter(FILTER_APPS_WORK);
            }
        }
        if (mListType == LIST_TYPE_NOTIFICATION) {
            mFilterAdapter.enableFilter(FILTER_APPS_BLOCKED);
        }
        if (mListType == LIST_TYPE_HIGH_POWER) {
            mFilterAdapter.enableFilter(FILTER_APPS_POWER_WHITELIST_ALL);
        }

        AppFilter compositeFilter = getCompositeFilter(mListType, mStorageType, mVolumeUuid);
        if (mIsWorkOnly) {
            compositeFilter = new CompoundFilter(compositeFilter, FILTERS[FILTER_APPS_WORK]);
        }
        if (compositeFilter != null) {
            mApplications.setCompositeFilter(compositeFilter);
        }
    }

    @VisibleForTesting
    @Nullable
    static AppFilter getCompositeFilter(int listType, int storageType, String volumeUuid) {
        AppFilter filter = new VolumeFilter(volumeUuid);
        if (listType == LIST_TYPE_STORAGE) {
            if (storageType == STORAGE_TYPE_MUSIC) {
                filter = new CompoundFilter(ApplicationsState.FILTER_AUDIO, filter);
            } else if (storageType == STORAGE_TYPE_DEFAULT) {
                filter = new CompoundFilter(ApplicationsState.FILTER_OTHER_APPS, filter);
            }
            return filter;
        }
        if (listType == LIST_TYPE_GAMES) {
            return new CompoundFilter(ApplicationsState.FILTER_GAMES, filter);
        } else if (listType == LIST_TYPE_MOVIES) {
            return new CompoundFilter(ApplicationsState.FILTER_MOVIES, filter);
        } else if (listType == LIST_TYPE_PHOTOGRAPHY) {
            return new CompoundFilter(ApplicationsState.FILTER_PHOTOS, filter);
        }

        return null;
    }

    private int getDefaultFilter() {
        switch (mListType) {
            case LIST_TYPE_USAGE_ACCESS:
                return FILTER_APPS_USAGE_ACCESS;
            case LIST_TYPE_HIGH_POWER:
                return FILTER_APPS_POWER_WHITELIST;
            case LIST_TYPE_OVERLAY:
                return FILTER_APPS_WITH_OVERLAY;
            case LIST_TYPE_WRITE_SETTINGS:
                return FILTER_APPS_WRITE_SETTINGS;
            case LIST_TYPE_MANAGE_SOURCES:
                return FILTER_APPS_INSTALL_SOURCES;
            default:
                return FILTER_APPS_ALL;
        }
    }

    private boolean isFastScrollEnabled() {
        switch (mListType) {
            case LIST_TYPE_MAIN:
            case LIST_TYPE_NOTIFICATION:
            case LIST_TYPE_STORAGE:
            case LIST_TYPE_GAMES:
            case LIST_TYPE_MOVIES:
            case LIST_TYPE_PHOTOGRAPHY:
                return mSortOrder == R.id.sort_order_alpha;
            default:
                return false;
        }
    }

    @Override
    public int getMetricsCategory() {
        switch (mListType) {
            case LIST_TYPE_MAIN:
                return MetricsEvent.MANAGE_APPLICATIONS;
            case LIST_TYPE_NOTIFICATION:
                return MetricsEvent.MANAGE_APPLICATIONS_NOTIFICATIONS;
            case LIST_TYPE_STORAGE:
                if (mStorageType == STORAGE_TYPE_MUSIC) {
                    return MetricsEvent.APPLICATIONS_STORAGE_MUSIC;
                }
                return MetricsEvent.APPLICATIONS_STORAGE_APPS;
            case LIST_TYPE_GAMES:
                return MetricsEvent.APPLICATIONS_STORAGE_GAMES;
            case LIST_TYPE_MOVIES:
                return MetricsEvent.APPLICATIONS_STORAGE_MOVIES;
            case LIST_TYPE_PHOTOGRAPHY:
                return MetricsEvent.APPLICATIONS_STORAGE_PHOTOS;
            case LIST_TYPE_USAGE_ACCESS:
                return MetricsEvent.USAGE_ACCESS;
            case LIST_TYPE_HIGH_POWER:
                return MetricsEvent.APPLICATIONS_HIGH_POWER_APPS;
            case LIST_TYPE_OVERLAY:
                return MetricsEvent.SYSTEM_ALERT_WINDOW_APPS;
            case LIST_TYPE_WRITE_SETTINGS:
                return MetricsEvent.SYSTEM_ALERT_WINDOW_APPS;
            case LIST_TYPE_MANAGE_SOURCES:
                return MetricsEvent.MANAGE_EXTERNAL_SOURCES;
            default:
                return MetricsEvent.VIEW_UNKNOWN;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateView();
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
        outState.putBoolean(EXTRA_HAS_BRIDGE, mApplications.mHasReceivedBridgeCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mApplications != null) {
            mApplications.pause();
        }
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
                startAppInfoFragment(AppNotificationSettings.class, R.string.notifications_title);
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
            case LIST_TYPE_MANAGE_SOURCES:
                startAppInfoFragment(ExternalSourcesDetails.class, R.string.install_other_apps);
                break;
            case LIST_TYPE_GAMES:
                startAppInfoFragment(AppStorageSettings.class, R.string.game_storage_settings);
                break;
            case LIST_TYPE_MOVIES:
                startAppInfoFragment(AppStorageSettings.class, R.string.storage_movies_tv);
                break;
            case LIST_TYPE_PHOTOGRAPHY:
                startAppInfoFragment(AppStorageSettings.class, R.string.storage_photos_videos);
                break;
            // TODO: Figure out if there is a way where we can spin up the profile's settings
            // process ahead of time, to avoid a long load of data when user clicks on a managed
            // app. Maybe when they load the list of apps that contains managed profile apps.
            default:
                startAppInfoFragment(InstalledAppDetails.class, R.string.application_info_label);
                break;
        }
    }

    private void startAppInfoFragment(Class<?> fragment, int titleRes) {
        AppInfoBase.startAppInfoFragment(fragment, titleRes, mCurrentPkgName, mCurrentUid, this,
                INSTALLED_APP_DETAILS, getMetricsCategory());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, getHelpResource(), getClass().getName());
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

    @StringRes
    int getHelpResource() {
        if (mListType == LIST_TYPE_MAIN) {
            return R.string.help_uri_apps;
        } else if (mListType == LIST_TYPE_USAGE_ACCESS) {
            return R.string.help_url_usage_access;
        } else {
            return R.string.help_uri_notifications;
        }
    }

    void updateOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }
        mOptionsMenu.findItem(R.id.advanced).setVisible(false);

        mOptionsMenu.findItem(R.id.sort_order_alpha).setVisible(mListType == LIST_TYPE_STORAGE
                && mSortOrder != R.id.sort_order_alpha);
        mOptionsMenu.findItem(R.id.sort_order_size).setVisible(mListType == LIST_TYPE_STORAGE
                && mSortOrder != R.id.sort_order_size);

        mOptionsMenu.findItem(R.id.show_system).setVisible(!mShowSystem
                && mListType != LIST_TYPE_HIGH_POWER);
        mOptionsMenu.findItem(R.id.hide_system).setVisible(mShowSystem
                && mListType != LIST_TYPE_HIGH_POWER);

        mOptionsMenu.findItem(R.id.reset_app_preferences).setVisible(mListType == LIST_TYPE_MAIN);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        switch (item.getItemId()) {
            case R.id.sort_order_alpha:
            case R.id.sort_order_size:
                mSortOrder = menuId;
                mListView.setFastScrollEnabled(isFastScrollEnabled());
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
                if (mListType == LIST_TYPE_NOTIFICATION) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(this,
                            ConfigureNotificationSettings.class.getName(), null,
                            R.string.configure_notification_settings, null, this,
                            ADVANCED_SETTINGS);
                } else {
                    ((SettingsActivity) getActivity()).startPreferencePanel(this,
                            DefaultAppSettings.class.getName(), null, R.string.configure_apps,
                            null, this, ADVANCED_SETTINGS);
                }
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
        if (mApplications == null) {
            return;
        }

        if (mApplications.getApplicationCount() > position) {
            ApplicationsState.AppEntry entry = mApplications.getAppEntry(position);
            mCurrentPkgName = entry.info.packageName;
            mCurrentUid = entry.info.uid;
            startApplicationDetailsActivity();
        } else {
            mApplications.mExtraViewController.onClick(this);
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
        if (mListType != LIST_TYPE_MAIN) {
            return;
        }
        mFilterAdapter.setFilterEnabled(FILTER_APPS_ENABLED, hasDisabledApps);
        mFilterAdapter.setFilterEnabled(FILTER_APPS_DISABLED, hasDisabledApps);
    }

    public void setHasInstant(boolean haveInstantApps) {
        if (LIST_TYPES_WITH_INSTANT.contains(mListType)) {
            mFilterAdapter.setFilterEnabled(FILTER_APPS_INSTANT, haveInstantApps);
        }
    }

    static class FilterSpinnerAdapter extends ArrayAdapter<CharSequence> {

        private final ManageApplications mManageApplications;

        // Use ArrayAdapter for view logic, but have our own list for managing
        // the options available.
        private final ArrayList<Integer> mFilterOptions = new ArrayList<>();

        public FilterSpinnerAdapter(ManageApplications manageApplications) {
            super(manageApplications.mFilterSpinner.getContext(), R.layout.filter_spinner_item);
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
            AbsListView.RecyclerListener, SectionIndexer {

        // how long to wait for app list to populate without showing the loading container
        private static final long DELAY_SHOW_LOADING_CONTAINER_THRESHOLD_MS = 100L;

        private static final SectionInfo[] EMPTY_SECTIONS = new SectionInfo[0];

        private final ApplicationsState mState;
        private final ApplicationsState.Session mSession;
        private final ManageApplications mManageApplications;
        private final Context mContext;
        private final ArrayList<View> mActive = new ArrayList<View>();
        private final AppStateBaseBridge mExtraInfoBridge;
        private final Handler mBgHandler;
        private final Handler mFgHandler;
        private final LoadingViewController mLoadingViewController;

        private int mFilterMode;
        private ArrayList<ApplicationsState.AppEntry> mBaseEntries;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private boolean mResumed;
        private int mLastSortMode = -1;
        private int mWhichSize = SIZE_TOTAL;
        CharSequence mCurFilterPrefix;
        private PackageManager mPm;
        private AppFilter mCompositeFilter;
        private boolean mHasReceivedLoadEntries;
        private boolean mHasReceivedBridgeCallback;
        private FileViewHolderController mExtraViewController;

        // These two variables are used to remember and restore the last scroll position when this
        // fragment is paused. We need this special handling because app entries are added gradually
        // when we rebuild the list after the user made some changes, like uninstalling an app.
        private int mLastIndex = -1;
        private int mLastTop;

        private AlphabeticIndex.ImmutableIndex<Locale> mIndex;
        private SectionInfo[] mSections = EMPTY_SECTIONS;
        private int[] mPositionToSectionIndex;

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
                rebuildSections();
                notifyDataSetChanged();
            }
        };


        public ApplicationsAdapter(ApplicationsState state, ManageApplications manageApplications,
                int filterMode) {
            mState = state;
            mFgHandler = new Handler();
            mBgHandler = new Handler(mState.getBackgroundLooper());
            mSession = state.newSession(this);
            mManageApplications = manageApplications;
            mLoadingViewController = new LoadingViewController(
                    mManageApplications.mLoadingContainer,
                    mManageApplications.mListContainer
            );
            mContext = manageApplications.getActivity();
            mPm = mContext.getPackageManager();
            mFilterMode = filterMode;
            if (mManageApplications.mListType == LIST_TYPE_NOTIFICATION) {
                mExtraInfoBridge = new AppStateNotificationBridge(mContext, mState, this,
                        manageApplications.mNotifBackend);
            } else if (mManageApplications.mListType == LIST_TYPE_USAGE_ACCESS) {
                mExtraInfoBridge = new AppStateUsageBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_HIGH_POWER) {
                mExtraInfoBridge = new AppStatePowerBridge(mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_OVERLAY) {
                mExtraInfoBridge = new AppStateOverlayBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_WRITE_SETTINGS) {
                mExtraInfoBridge = new AppStateWriteSettingsBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_MANAGE_SOURCES) {
                mExtraInfoBridge = new AppStateInstallAppsBridge(mContext, mState, this);
            } else {
                mExtraInfoBridge = null;
            }
        }

        public void setCompositeFilter(AppFilter compositeFilter) {
            mCompositeFilter = compositeFilter;
            rebuild(true);
        }

        public void setFilter(int filter) {
            mFilterMode = filter;
            rebuild(true);
        }

        public void setExtraViewController(FileViewHolderController extraViewController) {
            mExtraViewController = extraViewController;
            mBgHandler.post(() -> {
                mExtraViewController.queryStats();
                mFgHandler.post(() -> {
                    onExtraViewCompleted();
                });
            });
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
                rebuild(false);
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
            // Record the current scroll position before pausing.
            mLastIndex = mManageApplications.mListView.getFirstVisiblePosition();
            View v = mManageApplications.mListView.getChildAt(0);
            mLastTop =
                    (v == null) ? 0 : (v.getTop() - mManageApplications.mListView.getPaddingTop());
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
                    || (mExtraInfoBridge != null && !mHasReceivedBridgeCallback)) {
                // Don't rebuild the list until all the app entries are loaded.
                return;
            }
            ApplicationsState.AppFilter filterObj;
            Comparator<AppEntry> comparatorObj;
            boolean emulated = Environment.isExternalStorageEmulated();
            if (emulated) {
                mWhichSize = SIZE_TOTAL;
            } else {
                mWhichSize = SIZE_INTERNAL;
            }
            filterObj = FILTERS[mFilterMode];
            if (mCompositeFilter != null) {
                filterObj = new CompoundFilter(filterObj, mCompositeFilter);
            }
            if (!mManageApplications.mShowSystem) {
                if (LIST_TYPES_WITH_INSTANT.contains(mManageApplications.mListType)) {
                    filterObj = new CompoundFilter(filterObj,
                            ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER_AND_INSTANT);
                } else {
                    filterObj = new CompoundFilter(filterObj,
                            ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER);
                }
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

            filterObj = new CompoundFilter(filterObj, ApplicationsState.FILTER_NOT_HIDE);
            AppFilter finalFilterObj = filterObj;
            mBgHandler.post(() -> {
                final ArrayList<AppEntry> entries = mSession.rebuild(finalFilterObj,
                        comparatorObj, false);
                if (entries != null) {
                    mFgHandler.post(() -> onRebuildComplete(entries));
                }
            });
        }


        static private boolean packageNameEquals(PackageItemInfo info1, PackageItemInfo info2) {
            if (info1 == null || info2 == null) {
                return false;
            }
            if (info1.packageName == null || info2.packageName == null) {
                return false;
            }
            return info1.packageName.equals(info2.packageName);
        }

        private ArrayList<ApplicationsState.AppEntry> removeDuplicateIgnoringUser(
                ArrayList<ApplicationsState.AppEntry> entries) {
            int size = entries.size();
            // returnList will not have more entries than entries
            ArrayList<ApplicationsState.AppEntry> returnEntries = new
                    ArrayList<ApplicationsState.AppEntry>(size);

            // assume appinfo of same package but different users are grouped together
            PackageItemInfo lastInfo = null;
            for (int i = 0; i < size; i++) {
                AppEntry appEntry = entries.get(i);
                PackageItemInfo info = appEntry.info;
                if (!packageNameEquals(lastInfo, appEntry.info)) {
                    returnEntries.add(appEntry);
                }
                lastInfo = info;
            }
            returnEntries.trimToSize();
            return returnEntries;
        }

        @Override
        public void onRebuildComplete(ArrayList<AppEntry> entries) {
            if (mFilterMode == FILTER_APPS_POWER_WHITELIST ||
                    mFilterMode == FILTER_APPS_POWER_WHITELIST_ALL) {
                entries = removeDuplicateIgnoringUser(entries);
            }
            mBaseEntries = entries;
            if (mBaseEntries != null) {
                mEntries = applyPrefixFilter(mCurFilterPrefix, mBaseEntries);
                rebuildSections();
            } else {
                mEntries = null;
                mSections = EMPTY_SECTIONS;
                mPositionToSectionIndex = null;
            }

            notifyDataSetChanged();
            // Restore the last scroll position if the number of entries added so far is bigger than
            // it.
            if (mLastIndex != -1 && getCount() > mLastIndex) {
                mManageApplications.mListView.setSelectionFromTop(mLastIndex, mLastTop);
                mLastIndex = -1;
            }

            if (mSession.getAllApps().size() != 0
                    && mManageApplications.mListContainer.getVisibility() != View.VISIBLE) {
                mLoadingViewController.showContent(true /* animate */);
            }
            if (mManageApplications.mListType == LIST_TYPE_USAGE_ACCESS) {
                // No enabled or disabled filters for usage access.
                return;
            }

            mManageApplications.setHasDisabled(mState.haveDisabledApps());
            mManageApplications.setHasInstant(mState.haveInstantApps());
        }

        private void rebuildSections() {
            if (mEntries != null && mManageApplications.mListView.isFastScrollEnabled()) {
                // Rebuild sections
                if (mIndex == null) {
                    LocaleList locales = mContext.getResources().getConfiguration().getLocales();
                    if (locales.size() == 0) {
                        locales = new LocaleList(Locale.ENGLISH);
                    }
                    AlphabeticIndex<Locale> index = new AlphabeticIndex<>(locales.get(0));
                    int localeCount = locales.size();
                    for (int i = 1; i < localeCount; i++) {
                        index.addLabels(locales.get(i));
                    }
                    // Ensure we always have some base English locale buckets
                    index.addLabels(Locale.ENGLISH);
                    mIndex = index.buildImmutableIndex();
                }

                ArrayList<SectionInfo> sections = new ArrayList<>();
                int lastSecId = -1;
                int totalEntries = mEntries.size();
                mPositionToSectionIndex = new int[totalEntries];

                for (int pos = 0; pos < totalEntries; pos++) {
                    String label = mEntries.get(pos).label;
                    int secId = mIndex.getBucketIndex(TextUtils.isEmpty(label) ? "" : label);
                    if (secId != lastSecId) {
                        lastSecId = secId;
                        sections.add(new SectionInfo(mIndex.getBucket(secId).getLabel(), pos));
                    }
                    mPositionToSectionIndex[pos] = sections.size() - 1;
                }
                mSections = sections.toArray(EMPTY_SECTIONS);
            } else {
                mSections = EMPTY_SECTIONS;
                mPositionToSectionIndex = null;
            }
        }

        @VisibleForTesting
        void updateLoading() {
            final boolean appLoaded = mHasReceivedLoadEntries && mSession.getAllApps().size() != 0;
            if (appLoaded) {
                mLoadingViewController.showContent(false /* animate */);
            } else {
                mLoadingViewController.showLoadingViewDelayed();
            }
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
                for (int i = 0; i < origEntries.size(); i++) {
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
            // We may have been skipping rebuilds until this came in, trigger one now.
            rebuild(false);
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            for (int i = 0; i < mActive.size(); i++) {
                AppViewHolder holder = (AppViewHolder) mActive.get(i).getTag();
                if (holder == null || holder.entry == null) {
                    continue;
                }
                ApplicationInfo info = holder.entry.info;
                if (info == null) {
                    continue;
                }
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

        public void onExtraViewCompleted() {
            int size = mActive.size();
            // If we have no elements, don't do anything.
            if (size < 1) {
                return;
            }
            AppViewHolder holder = (AppViewHolder) mActive.get(size - 1).getTag();

            // HACK: The extra view has no AppEntry -- and should be the only element without one.
            // Thus, if the last active element has no AppEntry, it is the extra view.
            if (holder == null || holder.entry != null) {
                return;
            }

            mExtraViewController.setupView(holder);
        }

        public int getCount() {
            if (mEntries == null) {
                return 0;
            }
            int extraViewAddition =
                    (mExtraViewController != null && mExtraViewController.shouldShow()) ? 1 : 0;
            return mEntries.size() + extraViewAddition;
        }

        public int getApplicationCount() {
            return mEntries != null ? mEntries.size() : 0;
        }

        public Object getItem(int position) {
            if (position == mEntries.size()) {
                return mExtraViewController;
            }
            return mEntries.get(position);
        }

        public ApplicationsState.AppEntry getAppEntry(int position) {
            return mEntries.get(position);
        }

        public long getItemId(int position) {
            if (position == mEntries.size()) {
                return -1;
            }
            return mEntries.get(position).id;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            if (position == mEntries.size() && mExtraViewController != null &&
                    mExtraViewController.shouldShow()) {
                return true;
            }

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

            // Handle the extra view if it is the last entry.
            if (mEntries != null && mExtraViewController != null && position == mEntries.size()) {
                mExtraViewController.setupView(holder);
                convertView.setEnabled(true);
            } else {
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
                    updateDisableView(holder.disabled, entry.info);
                }
                convertView.setEnabled(isEnabled(position));
            }

            mActive.remove(convertView);
            mActive.add(convertView);
            return convertView;
        }

        @VisibleForTesting
        void updateDisableView(TextView view, ApplicationInfo info) {
            if ((info.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
                view.setVisibility(View.VISIBLE);
                view.setText(R.string.not_installed);
            } else if (!info.enabled || info.enabledSetting
                    == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
                view.setVisibility(View.VISIBLE);
                view.setText(R.string.disabled);
            } else {
                view.setVisibility(View.GONE);
            }
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

                case LIST_TYPE_USAGE_ACCESS:
                    if (holder.entry.extraInfo != null) {
                        holder.summary.setText((new UsageState((PermissionState) holder.entry
                                .extraInfo)).isPermissible()
                                ? R.string.app_permission_summary_allowed
                                : R.string.app_permission_summary_not_allowed);
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

                case LIST_TYPE_MANAGE_SOURCES:
                    holder.summary.setText(ExternalSourcesDetails.getPreferenceSummary(mContext,
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

        @Override
        public Object[] getSections() {
            return mSections;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return mSections[sectionIndex].position;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mPositionToSectionIndex[position];
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mLoader;
        private ApplicationsState.Session mSession;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                new InstalledAppCounter(mContext, InstalledAppCounter.IGNORE_INSTALL_REASON,
                        new PackageManagerWrapperImpl(mContext.getPackageManager())) {
                    @Override
                    protected void onCountComplete(int num) {
                        mLoader.setSummary(SummaryProvider.this,
                                mContext.getString(R.string.apps_summary, num));
                    }
                }.execute();
            }
        }
    }

    private static class SectionInfo {
        final String label;
        final int position;

        public SectionInfo(String label, int position) {
            this.label = label;
            this.position = position;
        }

        @Override
        public String toString() {
            return label;
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

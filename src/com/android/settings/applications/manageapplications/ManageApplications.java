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

package com.android.settings.applications.manageapplications;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_ALL;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_BLOCKED;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_DISABLED;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_ENABLED;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_FREQUENT;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_INSTANT;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_PERSONAL;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_POWER_ALLOWLIST;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_POWER_ALLOWLIST_ALL;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_RECENT;
import static com.android.settings.applications.manageapplications.AppFilterRegistry.FILTER_APPS_WORK;
import static com.android.settings.search.actionbar.SearchMenuController.MENU_SEARCH;

import android.annotation.Nullable;
import android.annotation.StringRes;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.app.usage.IUsageStatsManager;
import android.compat.annotation.ChangeId;
import android.compat.annotation.LoggingOnly;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceFrameLayout;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.compat.IPlatformCompat;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Settings.GamesStorageActivity;
import com.android.settings.Settings.HighPowerApplicationsActivity;
import com.android.settings.Settings.ManageExternalSourcesActivity;
import com.android.settings.Settings.OverlaySettingsActivity;
import com.android.settings.Settings.StorageUseActivity;
import com.android.settings.Settings.UsageAccessSettingsActivity;
import com.android.settings.Settings.WriteSettingsActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.AppStateAlarmsAndRemindersBridge;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.applications.AppStateInstallAppsBridge;
import com.android.settings.applications.AppStateLocaleBridge;
import com.android.settings.applications.AppStateManageExternalStorageBridge;
import com.android.settings.applications.AppStateMediaManagementAppsBridge;
import com.android.settings.applications.AppStateNotificationBridge;
import com.android.settings.applications.AppStateNotificationBridge.NotificationsSentState;
import com.android.settings.applications.AppStateOverlayBridge;
import com.android.settings.applications.AppStatePowerBridge;
import com.android.settings.applications.AppStateUsageBridge;
import com.android.settings.applications.AppStateUsageBridge.UsageState;
import com.android.settings.applications.AppStateWriteSettingsBridge;
import com.android.settings.applications.AppStorageSettings;
import com.android.settings.applications.UsageAccessDetails;
import com.android.settings.applications.appinfo.AlarmsAndRemindersDetails;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.applications.appinfo.AppLocaleDetails;
import com.android.settings.applications.appinfo.DrawOverlayDetails;
import com.android.settings.applications.appinfo.ExternalSourcesDetails;
import com.android.settings.applications.appinfo.ManageExternalStorageDetails;
import com.android.settings.applications.appinfo.MediaManagementAppsDetails;
import com.android.settings.applications.appinfo.WriteSettingsDetails;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.fuelgauge.HighPowerDetail;
import com.android.settings.localepicker.AppLocalePickerActivity;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.app.AppNotificationSettings;
import com.android.settings.widget.LoadingViewController;
import com.android.settings.wifi.AppStateChangeWifiStateBridge;
import com.android.settings.wifi.ChangeWifiStateDetails;
import com.android.settingslib.applications.AppIconCacheManager;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.applications.ApplicationsState.CompoundFilter;
import com.android.settingslib.applications.ApplicationsState.VolumeFilter;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.SettingsSpinnerAdapter;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

/**
 * Activity to pick an application that will be used to display installation information and
 * options to uninstall/delete user data for system applications. This activity
 * can be launched through Settings or via the ACTION_MANAGE_PACKAGE_STORAGE
 * intent.
 */
public class ManageApplications extends InstrumentedFragment
        implements View.OnClickListener, OnItemSelectedListener, SearchView.OnQueryTextListener,
        MenuItem.OnActionExpandListener {

    static final String TAG = "ManageApplications";
    static final boolean DEBUG = Build.IS_DEBUGGABLE;

    // Intent extras.
    public static final String EXTRA_CLASSNAME = "classname";
    // Used for storage only.
    public static final String EXTRA_VOLUME_UUID = "volumeUuid";
    public static final String EXTRA_VOLUME_NAME = "volumeName";
    public static final String EXTRA_STORAGE_TYPE = "storageType";
    public static final String EXTRA_WORK_ID = "workId";

    private static final String EXTRA_SORT_ORDER = "sortOrder";
    private static final String EXTRA_SHOW_SYSTEM = "showSystem";
    private static final String EXTRA_HAS_ENTRIES = "hasEntries";
    private static final String EXTRA_HAS_BRIDGE = "hasBridge";
    private static final String EXTRA_FILTER_TYPE = "filterType";
    @VisibleForTesting
    static final String EXTRA_EXPAND_SEARCH_VIEW = "expand_search_view";

    // attributes used as keys when passing values to AppInfoDashboardFragment activity
    public static final String APP_CHG = "chg";

    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;
    private static final int ADVANCED_SETTINGS = 2;

    public static final int SIZE_TOTAL = 0;
    public static final int SIZE_INTERNAL = 1;
    public static final int SIZE_EXTERNAL = 2;

    // Storage types. Used to determine what the extra item in the list of preferences is.
    public static final int STORAGE_TYPE_DEFAULT = 0; // Show all apps that are not categorized.
    public static final int STORAGE_TYPE_LEGACY = 1;  // Show apps even if they can be categorized.

    /**
     * Intents with action {@code android.settings.MANAGE_APP_OVERLAY_PERMISSION}
     * and data URI scheme {@code package} don't go to the app-specific screen for managing the
     * permission anymore. Instead, they redirect to this screen for managing all the apps that have
     * requested such permission.
     */
    @ChangeId
    @LoggingOnly
    private static final long CHANGE_RESTRICT_SAW_INTENT = 135920175L;

    // sort order
    @VisibleForTesting
    int mSortOrder = R.id.sort_order_alpha;

    // whether showing system apps.
    private boolean mShowSystem;

    private ApplicationsState mApplicationsState;

    public int mListType;
    private AppFilterItem mFilter;
    private ApplicationsAdapter mApplications;

    private View mLoadingContainer;
    private SearchView mSearchView;

    // Size resource used for packages whose size computation failed for some reason
    CharSequence mInvalidSizeStr;

    private String mCurrentPkgName;
    private int mCurrentUid;

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
    public static final int LIST_TYPE_WIFI_ACCESS = 10;
    public static final int LIST_MANAGE_EXTERNAL_STORAGE = 11;
    public static final int LIST_TYPE_ALARMS_AND_REMINDERS = 12;
    public static final int LIST_TYPE_MEDIA_MANAGEMENT_APPS = 13;
    public static final int LIST_TYPE_APPS_LOCALE = 14;

    // List types that should show instant apps.
    public static final Set<Integer> LIST_TYPES_WITH_INSTANT = new ArraySet<>(Arrays.asList(
            LIST_TYPE_MAIN,
            LIST_TYPE_STORAGE));

    @VisibleForTesting
    View mSpinnerHeader;
    @VisibleForTesting
    FilterSpinnerAdapter mFilterAdapter;
    @VisibleForTesting
    RecyclerView mRecyclerView;
    // Whether or not search view is expanded.
    @VisibleForTesting
    boolean mExpandSearch;

    private View mRootView;
    private Spinner mFilterSpinner;
    private IUsageStatsManager mUsageStatsManager;
    private UserManager mUserManager;
    private NotificationBackend mNotificationBackend;
    private ResetAppsHelper mResetAppsHelper;
    private String mVolumeUuid;
    private int mStorageType;
    private boolean mIsWorkOnly;
    private int mWorkUserId;
    private boolean mIsPersonalOnly;
    private View mEmptyView;
    private int mFilterType;
    private AppBarLayout mAppBarLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        final Activity activity = getActivity();
        mUserManager = activity.getSystemService(UserManager.class);
        mApplicationsState = ApplicationsState.getInstance(activity.getApplication());

        Intent intent = activity.getIntent();
        Bundle args = getArguments();
        final int screenTitle = getTitleResId(intent, args);
        String className = args != null ? args.getString(EXTRA_CLASSNAME) : null;
        if (className == null) {
            className = intent.getComponent().getClassName();
        }
        if (className.equals(StorageUseActivity.class.getName())) {
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

            reportIfRestrictedSawIntent(intent);
        } else if (className.equals(WriteSettingsActivity.class.getName())) {
            mListType = LIST_TYPE_WRITE_SETTINGS;
        } else if (className.equals(ManageExternalSourcesActivity.class.getName())) {
            mListType = LIST_TYPE_MANAGE_SOURCES;
        } else if (className.equals(GamesStorageActivity.class.getName())) {
            mListType = LIST_TYPE_GAMES;
            mSortOrder = R.id.sort_order_size;
        } else if (className.equals(Settings.ChangeWifiStateActivity.class.getName())) {
            mListType = LIST_TYPE_WIFI_ACCESS;
        } else if (className.equals(Settings.ManageExternalStorageActivity.class.getName())) {
            mListType = LIST_MANAGE_EXTERNAL_STORAGE;
        } else if (className.equals(Settings.MediaManagementAppsActivity.class.getName())) {
            mListType = LIST_TYPE_MEDIA_MANAGEMENT_APPS;
        } else if (className.equals(Settings.AlarmsAndRemindersActivity.class.getName())) {
            mListType = LIST_TYPE_ALARMS_AND_REMINDERS;
        } else if (className.equals(Settings.NotificationAppListActivity.class.getName())
                || className.equals(
                        Settings.NotificationReviewPermissionsActivity.class.getName())) {
            mListType = LIST_TYPE_NOTIFICATION;
            mUsageStatsManager = IUsageStatsManager.Stub.asInterface(
                    ServiceManager.getService(Context.USAGE_STATS_SERVICE));
            mNotificationBackend = new NotificationBackend();
            mSortOrder = R.id.sort_order_recent_notification;
            if (className.equals(Settings.NotificationReviewPermissionsActivity.class.getName())) {
                // Special-case for a case where a user is directed to the all apps notification
                // preferences page via a notification prompt to review permissions settings.
                android.provider.Settings.Global.putInt(getContext().getContentResolver(),
                        android.provider.Settings.Global.REVIEW_PERMISSIONS_NOTIFICATION_STATE,
                        1);  // USER_INTERACTED
            }
        } else if (className.equals(AppLocaleDetails.class.getName())) {
            mListType = LIST_TYPE_APPS_LOCALE;
        } else {
            mListType = LIST_TYPE_MAIN;
        }
        final AppFilterRegistry appFilterRegistry = AppFilterRegistry.getInstance();
        mFilter = appFilterRegistry.get(appFilterRegistry.getDefaultFilterType(mListType));
        mIsPersonalOnly = args != null ? args.getInt(ProfileSelectFragment.EXTRA_PROFILE)
                == ProfileSelectFragment.ProfileType.PERSONAL : false;
        mIsWorkOnly = args != null ? args.getInt(ProfileSelectFragment.EXTRA_PROFILE)
                == ProfileSelectFragment.ProfileType.WORK : false;
        mWorkUserId = args != null ? args.getInt(EXTRA_WORK_ID) : UserHandle.myUserId();
        if (mIsWorkOnly && mWorkUserId == UserHandle.myUserId()) {
            mWorkUserId = Utils.getManagedProfileId(mUserManager, UserHandle.myUserId());
        }

        mExpandSearch = activity.getIntent().getBooleanExtra(EXTRA_EXPAND_SEARCH_VIEW, false);

        if (savedInstanceState != null) {
            mSortOrder = savedInstanceState.getInt(EXTRA_SORT_ORDER, mSortOrder);
            mShowSystem = savedInstanceState.getBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
            mFilterType =
                    savedInstanceState.getInt(EXTRA_FILTER_TYPE, AppFilterRegistry.FILTER_APPS_ALL);
            mExpandSearch = savedInstanceState.getBoolean(EXTRA_EXPAND_SEARCH_VIEW);
        }

        mInvalidSizeStr = activity.getText(R.string.invalid_size_value);

        mResetAppsHelper = new ResetAppsHelper(activity);

        if (screenTitle > 0) {
            activity.setTitle(screenTitle);
        }
    }

    private void reportIfRestrictedSawIntent(Intent intent) {
        try {
            Uri data = intent.getData();
            if (data == null || !TextUtils.equals("package", data.getScheme())) {
                // Not a restricted intent
                return;
            }
            IBinder activityToken = getActivity().getActivityToken();
            int callingUid = ActivityManager.getService().getLaunchedFromUid(activityToken);
            if (callingUid == -1) {
                Log.w(TAG, "Error obtaining calling uid");
                return;
            }
            IPlatformCompat platformCompat = IPlatformCompat.Stub.asInterface(
                    ServiceManager.getService(Context.PLATFORM_COMPAT_SERVICE));
            if (platformCompat == null) {
                Log.w(TAG, "Error obtaining IPlatformCompat service");
                return;
            }
            platformCompat.reportChangeByUid(CHANGE_RESTRICT_SAW_INTENT, callingUid);
        } catch (RemoteException e) {
            Log.w(TAG, "Error reporting SAW intent restriction", e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (mListType == LIST_TYPE_OVERLAY && !Utils.isSystemAlertWindowEnabled(getContext())) {
            mRootView = inflater.inflate(R.layout.manage_applications_apps_unsupported, null);
            setHasOptionsMenu(false);
            return mRootView;
        }

        mRootView = inflater.inflate(R.layout.manage_applications_apps, null);
        mLoadingContainer = mRootView.findViewById(R.id.loading_container);
        mEmptyView = mRootView.findViewById(android.R.id.empty);
        mRecyclerView = mRootView.findViewById(R.id.apps_list);

        mApplications = new ApplicationsAdapter(mApplicationsState, this, mFilter,
                savedInstanceState);
        if (savedInstanceState != null) {
            mApplications.mHasReceivedLoadEntries =
                    savedInstanceState.getBoolean(EXTRA_HAS_ENTRIES, false);
            mApplications.mHasReceivedBridgeCallback =
                    savedInstanceState.getBoolean(EXTRA_HAS_BRIDGE, false);
        }
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(
                getContext(), RecyclerView.VERTICAL, false /* reverseLayout */));
        mRecyclerView.setAdapter(mApplications);

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) mRootView.getLayoutParams()).removeBorders = true;
        }

        createHeader();

        mResetAppsHelper.onRestoreInstanceState(savedInstanceState);

        mAppBarLayout = getActivity().findViewById(R.id.app_bar);
        disableToolBarScrollableBehavior();

        return mRootView;
    }

    @VisibleForTesting
    void createHeader() {
        final Activity activity = getActivity();
        final FrameLayout pinnedHeader = mRootView.findViewById(R.id.pinned_header);
        mSpinnerHeader = activity.getLayoutInflater()
                .inflate(R.layout.manage_apps_filter_spinner, pinnedHeader, false);
        mFilterSpinner = mSpinnerHeader.findViewById(R.id.filter_spinner);
        mFilterAdapter = new FilterSpinnerAdapter(this);
        mFilterSpinner.setAdapter(mFilterAdapter);
        mFilterSpinner.setOnItemSelectedListener(this);
        pinnedHeader.addView(mSpinnerHeader, 0);

        final AppFilterRegistry appFilterRegistry = AppFilterRegistry.getInstance();
        final int filterType = appFilterRegistry.getDefaultFilterType(mListType);
        mFilterAdapter.enableFilter(filterType);

        if (mListType == LIST_TYPE_MAIN) {
            if (UserManager.get(getActivity()).getUserProfiles().size() > 1 && !mIsWorkOnly
                    && !mIsPersonalOnly) {
                mFilterAdapter.enableFilter(FILTER_APPS_PERSONAL);
                mFilterAdapter.enableFilter(FILTER_APPS_WORK);
            }
        }
        if (mListType == LIST_TYPE_NOTIFICATION) {
            mFilterAdapter.enableFilter(FILTER_APPS_RECENT);
            mFilterAdapter.enableFilter(FILTER_APPS_FREQUENT);
            mFilterAdapter.enableFilter(FILTER_APPS_BLOCKED);
            mFilterAdapter.enableFilter(FILTER_APPS_ALL);
        }
        if (mListType == LIST_TYPE_HIGH_POWER) {
            mFilterAdapter.enableFilter(FILTER_APPS_POWER_ALLOWLIST_ALL);
        }

        setCompositeFilter();
    }

    @VisibleForTesting
    @Nullable
    static AppFilter getCompositeFilter(int listType, int storageType, String volumeUuid) {
        AppFilter filter = new VolumeFilter(volumeUuid);
        if (listType == LIST_TYPE_STORAGE) {
            if (storageType == STORAGE_TYPE_DEFAULT) {
                filter = new CompoundFilter(ApplicationsState.FILTER_APPS_EXCEPT_GAMES, filter);
            }
            return filter;
        }
        if (listType == LIST_TYPE_GAMES) {
            return new CompoundFilter(ApplicationsState.FILTER_GAMES, filter);
        }
        return null;
    }

    @Override
    public int getMetricsCategory() {
        switch (mListType) {
            case LIST_TYPE_MAIN:
                return SettingsEnums.MANAGE_APPLICATIONS;
            case LIST_TYPE_NOTIFICATION:
                return SettingsEnums.MANAGE_APPLICATIONS_NOTIFICATIONS;
            case LIST_TYPE_STORAGE:
                return SettingsEnums.APPLICATIONS_STORAGE_APPS;
            case LIST_TYPE_GAMES:
                return SettingsEnums.APPLICATIONS_STORAGE_GAMES;
            case LIST_TYPE_USAGE_ACCESS:
                return SettingsEnums.USAGE_ACCESS;
            case LIST_TYPE_HIGH_POWER:
                return SettingsEnums.APPLICATIONS_HIGH_POWER_APPS;
            case LIST_TYPE_OVERLAY:
                return SettingsEnums.SYSTEM_ALERT_WINDOW_APPS;
            case LIST_TYPE_WRITE_SETTINGS:
                return SettingsEnums.SYSTEM_ALERT_WINDOW_APPS;
            case LIST_TYPE_MANAGE_SOURCES:
                return SettingsEnums.MANAGE_EXTERNAL_SOURCES;
            case LIST_TYPE_WIFI_ACCESS:
                return SettingsEnums.CONFIGURE_WIFI;
            case LIST_MANAGE_EXTERNAL_STORAGE:
                return SettingsEnums.MANAGE_EXTERNAL_STORAGE;
            case LIST_TYPE_ALARMS_AND_REMINDERS:
                return SettingsEnums.ALARMS_AND_REMINDERS;
            case LIST_TYPE_MEDIA_MANAGEMENT_APPS:
                return SettingsEnums.MEDIA_MANAGEMENT_APPS;
            case LIST_TYPE_APPS_LOCALE:
                return SettingsEnums.APPS_LOCALE_LIST;
            default:
                return SettingsEnums.PAGE_UNKNOWN;
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
        outState.putInt(EXTRA_FILTER_TYPE, mFilter.getFilterType());
        outState.putBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
        if (mSearchView != null) {
            outState.putBoolean(EXTRA_EXPAND_SEARCH_VIEW, !mSearchView.isIconified());
        }
        if (mApplications != null) {
            outState.putBoolean(EXTRA_HAS_ENTRIES, mApplications.mHasReceivedLoadEntries);
            outState.putBoolean(EXTRA_HAS_BRIDGE, mApplications.mHasReceivedBridgeCallback);
            mApplications.onSaveInstanceState(outState);
        }
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
        AppIconCacheManager.getInstance().release();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INSTALLED_APP_DETAILS && mCurrentPkgName != null) {
            if (mListType == LIST_TYPE_NOTIFICATION) {
                mApplications.mExtraInfoBridge.forceUpdate(mCurrentPkgName, mCurrentUid);
            } else if (mListType == LIST_TYPE_HIGH_POWER || mListType == LIST_TYPE_OVERLAY
                    || mListType == LIST_TYPE_WRITE_SETTINGS) {
                mApplications.mExtraInfoBridge.forceUpdate(mCurrentPkgName, mCurrentUid);
            } else {
                mApplicationsState.requestSize(mCurrentPkgName, UserHandle.getUserId(mCurrentUid));
            }
        }
    }

    private void setCompositeFilter() {
        AppFilter compositeFilter = getCompositeFilter(mListType, mStorageType, mVolumeUuid);
        if (compositeFilter == null) {
            compositeFilter = mFilter.getFilter();
        }
        if (mIsWorkOnly) {
            compositeFilter = new CompoundFilter(compositeFilter, ApplicationsState.FILTER_WORK);
        }
        if (mIsPersonalOnly) {
            compositeFilter = new CompoundFilter(compositeFilter,
                    ApplicationsState.FILTER_PERSONAL);
        }
        mApplications.setCompositeFilter(compositeFilter);
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
                HighPowerDetail.show(this, mCurrentUid, mCurrentPkgName, INSTALLED_APP_DETAILS);
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
            case LIST_TYPE_WIFI_ACCESS:
                startAppInfoFragment(ChangeWifiStateDetails.class,
                        R.string.change_wifi_state_title);
                break;
            case LIST_MANAGE_EXTERNAL_STORAGE:
                startAppInfoFragment(ManageExternalStorageDetails.class,
                        R.string.manage_external_storage_title);
                break;
            case LIST_TYPE_ALARMS_AND_REMINDERS:
                startAppInfoFragment(AlarmsAndRemindersDetails.class,
                        R.string.alarms_and_reminders_label);
                break;
            case LIST_TYPE_MEDIA_MANAGEMENT_APPS:
                startAppInfoFragment(MediaManagementAppsDetails.class,
                        R.string.media_management_apps_title);
                break;
            case LIST_TYPE_APPS_LOCALE:
                Intent intent = new Intent(getContext(), AppLocalePickerActivity.class);
                intent.setData(Uri.parse("package:" + mCurrentPkgName));
                intent.putExtra(AppInfoBase.ARG_PACKAGE_UID, mCurrentUid);
                startActivity(intent);
                break;
            // TODO: Figure out if there is a way where we can spin up the profile's settings
            // process ahead of time, to avoid a long load of data when user clicks on a managed
            // app. Maybe when they load the list of apps that contains managed profile apps.
            default:
                startAppInfoFragment(
                        AppInfoDashboardFragment.class, R.string.application_info_label);
                break;
        }
    }

    private void startAppInfoFragment(Class<?> fragment, int titleRes) {
        AppInfoBase.startAppInfoFragment(fragment, getString(titleRes), mCurrentPkgName,
                mCurrentUid, this, INSTALLED_APP_DETAILS, getMetricsCategory());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mOptionsMenu = menu;
        inflater.inflate(R.menu.manage_apps, menu);

        final MenuItem searchMenuItem = menu.findItem(R.id.search_app_list_menu);
        if (searchMenuItem != null) {
            searchMenuItem.setOnActionExpandListener(this);
            mSearchView = (SearchView) searchMenuItem.getActionView();
            mSearchView.setQueryHint(getText(R.string.search_settings));
            mSearchView.setOnQueryTextListener(this);
            if (mExpandSearch) {
                searchMenuItem.expandActionView();
            }
        }

        updateOptionsMenu();
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        // To prevent a large space on tool bar.
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        // To prevent user can expand the collapsing tool bar view.
        ViewCompat.setNestedScrollingEnabled(mRecyclerView, false);
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        // We keep the collapsed status after user cancel the search function.
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        ViewCompat.setNestedScrollingEnabled(mRecyclerView, true);
        return true;
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
        switch (mListType) {
            case LIST_TYPE_NOTIFICATION:
                return R.string.help_uri_notifications;
            case LIST_TYPE_USAGE_ACCESS:
                return R.string.help_url_usage_access;
            case LIST_TYPE_STORAGE:
                return R.string.help_uri_apps_storage;
            case LIST_TYPE_HIGH_POWER:
                return R.string.help_uri_apps_high_power;
            case LIST_TYPE_OVERLAY:
                return R.string.help_uri_apps_overlay;
            case LIST_TYPE_WRITE_SETTINGS:
                return R.string.help_uri_apps_write_settings;
            case LIST_TYPE_MANAGE_SOURCES:
                return R.string.help_uri_apps_manage_sources;
            case LIST_TYPE_GAMES:
                return R.string.help_uri_apps_overlay;
            case LIST_TYPE_WIFI_ACCESS:
                return R.string.help_uri_apps_wifi_access;
            case LIST_MANAGE_EXTERNAL_STORAGE:
                return R.string.help_uri_manage_external_storage;
            case LIST_TYPE_ALARMS_AND_REMINDERS:
                return R.string.help_uri_alarms_and_reminders;
            case LIST_TYPE_MEDIA_MANAGEMENT_APPS:
                return R.string.help_uri_media_management_apps;
            default:
            case LIST_TYPE_MAIN:
                return R.string.help_uri_apps;
        }
    }

    void updateOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }
        mOptionsMenu.findItem(R.id.advanced).setVisible(false);

        mOptionsMenu.findItem(R.id.sort_order_alpha).setVisible(
                (mListType == LIST_TYPE_STORAGE || mListType == LIST_TYPE_MAIN)
                && mSortOrder != R.id.sort_order_alpha);
        mOptionsMenu.findItem(R.id.sort_order_size).setVisible(
                (mListType == LIST_TYPE_STORAGE || mListType == LIST_TYPE_MAIN)
                && mSortOrder != R.id.sort_order_size);

        mOptionsMenu.findItem(R.id.show_system).setVisible(!mShowSystem
                && mListType != LIST_TYPE_HIGH_POWER && mListType != LIST_TYPE_APPS_LOCALE);
        mOptionsMenu.findItem(R.id.hide_system).setVisible(mShowSystem
                && mListType != LIST_TYPE_HIGH_POWER && mListType != LIST_TYPE_APPS_LOCALE);

        mOptionsMenu.findItem(R.id.reset_app_preferences).setVisible(mListType == LIST_TYPE_MAIN);

        // Hide notification menu items, because sorting happens when filtering
        mOptionsMenu.findItem(R.id.sort_order_recent_notification).setVisible(false);
        mOptionsMenu.findItem(R.id.sort_order_frequent_notification).setVisible(false);
        final MenuItem searchItem = mOptionsMenu.findItem(MENU_SEARCH);
        if (searchItem != null) {
            searchItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        int i = item.getItemId();
        if (i == R.id.sort_order_alpha || i == R.id.sort_order_size) {
            if (mApplications != null) {
                mApplications.rebuild(menuId, false);
            }
        } else if (i == R.id.show_system || i == R.id.hide_system) {
            mShowSystem = !mShowSystem;
            mApplications.rebuild();
        } else if (i == R.id.reset_app_preferences) {
            mResetAppsHelper.buildResetDialog();
            return true;
        } else if (i == R.id.advanced) {
            if (mListType == LIST_TYPE_NOTIFICATION) {
                new SubSettingLauncher(getContext())
                        .setDestination(ConfigureNotificationSettings.class.getName())
                        .setTitleRes(R.string.configure_notification_settings)
                        .setSourceMetricsCategory(getMetricsCategory())
                        .setResultListener(this, ADVANCED_SETTINGS)
                        .launch();
            } else {
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                startActivityForResult(intent, ADVANCED_SETTINGS);
            }
            return true;
        } else {// Handle the home button
            return false;
        }
        updateOptionsMenu();
        return true;
    }

    @Override
    public void onClick(View view) {
        if (mApplications == null) {
            return;
        }
        final int applicationPosition =
                ApplicationsAdapter.getApplicationPosition(
                        mListType, mRecyclerView.getChildAdapterPosition(view));

        if (applicationPosition == RecyclerView.NO_POSITION) {
            Log.w(TAG, "Cannot find position for child, skipping onClick handling");
            return;
        }
        if (mApplications.getApplicationCount() > applicationPosition) {
            ApplicationsState.AppEntry entry = mApplications.getAppEntry(applicationPosition);
            mCurrentPkgName = entry.info.packageName;
            mCurrentUid = entry.info.uid;
            startApplicationDetailsActivity();
            // We disable the scrolling ability in onMenuItemActionCollapse, we should recover it
            // if user selects any app item.
            ViewCompat.setNestedScrollingEnabled(mRecyclerView, true);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mFilter = mFilterAdapter.getFilter(position);
        setCompositeFilter();
        mApplications.setFilter(mFilter);

        if (DEBUG) {
            Log.d(TAG, "Selecting filter " + getContext().getText(mFilter.getTitle()));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mApplications.filterSearch(newText);
        return false;
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

    private void disableToolBarScrollableBehavior() {
        final CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();
        final AppBarLayout.Behavior behavior = new AppBarLayout.Behavior();
        behavior.setDragCallback(
                new AppBarLayout.Behavior.DragCallback() {
                    @Override
                    public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                        return false;
                    }
                });
        params.setBehavior(behavior);
    }

    /**
     * Returns a resource ID of title based on what type of app list is
     * @param intent the intent of the activity that might include a specified title
     * @param args the args that includes a class name of app list
     */
    public static int getTitleResId(@NonNull Intent intent, Bundle args) {
        int screenTitle = intent.getIntExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, R.string.all_apps);
        String className = args != null ? args.getString(EXTRA_CLASSNAME) : null;
        if (className == null) {
            className = intent.getComponent().getClassName();
        }
        if (className.equals(Settings.UsageAccessSettingsActivity.class.getName())) {
            screenTitle = R.string.usage_access;
        } else if (className.equals(Settings.HighPowerApplicationsActivity.class.getName())) {
            screenTitle = R.string.high_power_apps;
        } else if (className.equals(Settings.OverlaySettingsActivity.class.getName())) {
            screenTitle = R.string.system_alert_window_settings;
        } else if (className.equals(Settings.WriteSettingsActivity.class.getName())) {
            screenTitle = R.string.write_settings;
        } else if (className.equals(Settings.ManageExternalSourcesActivity.class.getName())) {
            screenTitle = R.string.install_other_apps;
        } else if (className.equals(Settings.ChangeWifiStateActivity.class.getName())) {
            screenTitle = R.string.change_wifi_state_title;
        } else if (className.equals(Settings.ManageExternalStorageActivity.class.getName())) {
            screenTitle = R.string.manage_external_storage_title;
        }  else if (className.equals(Settings.MediaManagementAppsActivity.class.getName())) {
            screenTitle = R.string.media_management_apps_title;
        } else if (className.equals(Settings.AlarmsAndRemindersActivity.class.getName())) {
            screenTitle = R.string.alarms_and_reminders_title;
        } else if (className.equals(Settings.NotificationAppListActivity.class.getName())
                || className.equals(
                        Settings.NotificationReviewPermissionsActivity.class.getName())) {
            screenTitle = R.string.app_notifications_title;
        } else if (className.equals(AppLocaleDetails.class.getName())) {
            screenTitle = R.string.app_locales_picker_menu_title;
        } else {
            if (screenTitle == -1) {
                screenTitle = R.string.all_apps;
            }
        }
        return screenTitle;
    }

    static class FilterSpinnerAdapter extends SettingsSpinnerAdapter<CharSequence> {

        private final ManageApplications mManageApplications;
        private final Context mContext;

        // Use ArrayAdapter for view logic, but have our own list for managing
        // the options available.
        private final ArrayList<AppFilterItem> mFilterOptions = new ArrayList<>();

        public FilterSpinnerAdapter(ManageApplications manageApplications) {
            super(manageApplications.getContext());
            mContext = manageApplications.getContext();
            mManageApplications = manageApplications;
        }

        public AppFilterItem getFilter(int position) {
            return mFilterOptions.get(position);
        }

        public void setFilterEnabled(@AppFilterRegistry.FilterType int filter, boolean enabled) {
            if (enabled) {
                enableFilter(filter);
            } else {
                disableFilter(filter);
            }
        }

        public void enableFilter(@AppFilterRegistry.FilterType int filterType) {
            final AppFilterItem filter = AppFilterRegistry.getInstance().get(filterType);
            if (mFilterOptions.contains(filter)) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "Enabling filter " + mContext.getText(filter.getTitle()));
            }
            mFilterOptions.add(filter);
            Collections.sort(mFilterOptions);
            updateFilterView(mFilterOptions.size() > 1);
            notifyDataSetChanged();
            if (mFilterOptions.size() == 1) {
                if (DEBUG) {
                    Log.d(TAG, "Auto selecting filter " + filter + " " + mContext.getText(
                            filter.getTitle()));
                }
                mManageApplications.mFilterSpinner.setSelection(0);
                mManageApplications.onItemSelected(null, null, 0, 0);
            }
            if (mFilterOptions.size() > 1) {
                final AppFilterItem previousFilter = AppFilterRegistry.getInstance().get(
                        mManageApplications.mFilterType);
                final int index = mFilterOptions.indexOf(previousFilter);
                if (index != -1) {
                    mManageApplications.mFilterSpinner.setSelection(index);
                    mManageApplications.onItemSelected(null, null, index, 0);
                }
            }
        }

        public void disableFilter(@AppFilterRegistry.FilterType int filterType) {
            final AppFilterItem filter = AppFilterRegistry.getInstance().get(filterType);
            if (!mFilterOptions.remove(filter)) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "Disabling filter " + filter + " " + mContext.getText(
                        filter.getTitle()));
            }
            Collections.sort(mFilterOptions);
            updateFilterView(mFilterOptions.size() > 1);
            notifyDataSetChanged();
            if (mManageApplications.mFilter == filter) {
                if (mFilterOptions.size() > 0) {
                    if (DEBUG) {
                        Log.d(TAG, "Auto selecting filter " + mFilterOptions.get(0)
                                + mContext.getText(mFilterOptions.get(0).getTitle()));
                    }
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
            return mContext.getText(mFilterOptions.get(position).getTitle());
        }

        @VisibleForTesting
        void updateFilterView(boolean hasFilter) {
            // If we need to add a floating filter in this screen, we should have an extra top
            // padding for putting floating filter view. Otherwise, the content of list will be
            // overlapped by floating filter.
            if (hasFilter) {
                mManageApplications.mSpinnerHeader.setVisibility(View.VISIBLE);
            } else {
                mManageApplications.mSpinnerHeader.setVisibility(View.GONE);
            }
        }
    }

    static class ApplicationsAdapter extends RecyclerView.Adapter<ApplicationViewHolder>
            implements ApplicationsState.Callbacks, AppStateBaseBridge.Callback {

        private static final String STATE_LAST_SCROLL_INDEX = "state_last_scroll_index";
        private static final int VIEW_TYPE_APP = 0;
        private static final int VIEW_TYPE_EXTRA_VIEW = 1;
        private static final int VIEW_TYPE_APP_HEADER = 2;

        private final ApplicationsState mState;
        private final ApplicationsState.Session mSession;
        private final ManageApplications mManageApplications;
        private final Context mContext;
        private final AppStateBaseBridge mExtraInfoBridge;
        private final LoadingViewController mLoadingViewController;
        private final IconDrawableFactory mIconDrawableFactory;

        private AppFilterItem mAppFilter;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private ArrayList<ApplicationsState.AppEntry> mOriginalEntries;
        private boolean mResumed;
        private int mLastSortMode = -1;
        private int mWhichSize = SIZE_TOTAL;
        private AppFilter mCompositeFilter;
        private boolean mHasReceivedLoadEntries;
        private boolean mHasReceivedBridgeCallback;
        private SearchFilter mSearchFilter;
        private PowerAllowlistBackend mBackend;

        // This is to remember and restore the last scroll position when this
        // fragment is paused. We need this special handling because app entries are added gradually
        // when we rebuild the list after the user made some changes, like uninstalling an app.
        private int mLastIndex = -1;

        @VisibleForTesting
        OnScrollListener mOnScrollListener;
        private RecyclerView mRecyclerView;


        public ApplicationsAdapter(ApplicationsState state, ManageApplications manageApplications,
                AppFilterItem appFilter, Bundle savedInstanceState) {
            setHasStableIds(true);
            mState = state;
            mSession = state.newSession(this);
            mManageApplications = manageApplications;
            mLoadingViewController = new LoadingViewController(
                    mManageApplications.mLoadingContainer,
                    mManageApplications.mRecyclerView,
                    mManageApplications.mEmptyView
            );
            mContext = manageApplications.getActivity();
            mIconDrawableFactory = IconDrawableFactory.newInstance(mContext);
            mAppFilter = appFilter;
            mBackend = PowerAllowlistBackend.getInstance(mContext);
            if (mManageApplications.mListType == LIST_TYPE_NOTIFICATION) {
                mExtraInfoBridge = new AppStateNotificationBridge(mContext, mState, this,
                        manageApplications.mUsageStatsManager,
                        manageApplications.mUserManager,
                        manageApplications.mNotificationBackend);
            } else if (mManageApplications.mListType == LIST_TYPE_USAGE_ACCESS) {
                mExtraInfoBridge = new AppStateUsageBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_HIGH_POWER) {
                mBackend.refreshList();
                mExtraInfoBridge = new AppStatePowerBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_OVERLAY) {
                mExtraInfoBridge = new AppStateOverlayBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_WRITE_SETTINGS) {
                mExtraInfoBridge = new AppStateWriteSettingsBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_MANAGE_SOURCES) {
                mExtraInfoBridge = new AppStateInstallAppsBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_WIFI_ACCESS) {
                mExtraInfoBridge = new AppStateChangeWifiStateBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_MANAGE_EXTERNAL_STORAGE) {
                mExtraInfoBridge = new AppStateManageExternalStorageBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_ALARMS_AND_REMINDERS) {
                mExtraInfoBridge = new AppStateAlarmsAndRemindersBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_MEDIA_MANAGEMENT_APPS) {
                mExtraInfoBridge = new AppStateMediaManagementAppsBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_APPS_LOCALE) {
                mExtraInfoBridge = new AppStateLocaleBridge(mContext, mState, this);
            } else {
                mExtraInfoBridge = null;
            }
            if (savedInstanceState != null) {
                mLastIndex = savedInstanceState.getInt(STATE_LAST_SCROLL_INDEX);
            }
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            mRecyclerView = recyclerView;
            mOnScrollListener = new OnScrollListener(this);
            mRecyclerView.addOnScrollListener(mOnScrollListener);
        }

        @Override
        public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            mRecyclerView.removeOnScrollListener(mOnScrollListener);
            mOnScrollListener = null;
            mRecyclerView = null;
        }

        public void setCompositeFilter(AppFilter compositeFilter) {
            mCompositeFilter = compositeFilter;
            rebuild();
        }

        public void setFilter(AppFilterItem appFilter) {
            mAppFilter = appFilter;

            // Notification filters require resorting the list
            if (mManageApplications.mListType == LIST_TYPE_NOTIFICATION) {
                if (FILTER_APPS_FREQUENT == appFilter.getFilterType()) {
                    rebuild(R.id.sort_order_frequent_notification, false);
                } else if (FILTER_APPS_RECENT == appFilter.getFilterType()) {
                    rebuild(R.id.sort_order_recent_notification, false);
                } else if (FILTER_APPS_BLOCKED == appFilter.getFilterType()) {
                    rebuild(R.id.sort_order_alpha, true);
                } else {
                    rebuild(R.id.sort_order_alpha, true);
                }
            } else {
                rebuild();
            }
        }

        public void resume(int sort) {
            if (DEBUG) Log.i(TAG, "Resume!  mResumed=" + mResumed);
            if (!mResumed) {
                mResumed = true;
                mSession.onResume();
                mLastSortMode = sort;
                if (mExtraInfoBridge != null) {
                    mExtraInfoBridge.resume(false /* forceLoadAllApps */);
                }
                rebuild();
            } else {
                rebuild(sort, false);
            }
        }

        public void pause() {
            if (mResumed) {
                mResumed = false;
                mSession.onPause();
                if (mExtraInfoBridge != null) {
                    mExtraInfoBridge.pause();
                }
            }
        }

        public void onSaveInstanceState(Bundle outState) {
            // Record the current scroll position before pausing.
            final LinearLayoutManager layoutManager =
                    (LinearLayoutManager) mManageApplications.mRecyclerView.getLayoutManager();
            outState.putInt(STATE_LAST_SCROLL_INDEX, layoutManager.findFirstVisibleItemPosition());
        }

        public void release() {
            mSession.onDestroy();
            if (mExtraInfoBridge != null) {
                mExtraInfoBridge.release();
            }
        }

        public void rebuild(int sort, boolean force) {
            if (sort == mLastSortMode && !force) {
                return;
            }
            mManageApplications.mSortOrder = sort;
            mLastSortMode = sort;
            rebuild();
        }

        @Override
        public ApplicationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view;
            if (mManageApplications.mListType == LIST_TYPE_APPS_LOCALE
                    && viewType == VIEW_TYPE_APP_HEADER) {
                view = ApplicationViewHolder.newHeader(parent,
                        R.string.desc_app_locale_selection_supported);
            } else if (mManageApplications.mListType == LIST_TYPE_NOTIFICATION) {
                view = ApplicationViewHolder.newView(parent, true /* twoTarget */);
            } else {
                view = ApplicationViewHolder.newView(parent, false /* twoTarget */);
            }
            return new ApplicationViewHolder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 && mManageApplications.mListType == LIST_TYPE_APPS_LOCALE) {
                return VIEW_TYPE_APP_HEADER;
            }
            return VIEW_TYPE_APP;
        }

        public void rebuild() {
            if (!mHasReceivedLoadEntries
                    || (mExtraInfoBridge != null && !mHasReceivedBridgeCallback)) {
                // Don't rebuild the list until all the app entries are loaded.
                if (DEBUG) {
                    Log.d(TAG, "Not rebuilding until all the app entries loaded."
                            + " !mHasReceivedLoadEntries=" + !mHasReceivedLoadEntries
                            + " !mExtraInfoBridgeNull=" + (mExtraInfoBridge != null)
                            + " !mHasReceivedBridgeCallback=" + !mHasReceivedBridgeCallback);
                }
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
            filterObj = mAppFilter.getFilter();
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
            if (mLastSortMode == R.id.sort_order_size) {
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
            } else if (mLastSortMode == R.id.sort_order_recent_notification) {
                comparatorObj = AppStateNotificationBridge.RECENT_NOTIFICATION_COMPARATOR;
            } else if (mLastSortMode == R.id.sort_order_frequent_notification) {
                comparatorObj = AppStateNotificationBridge.FREQUENCY_NOTIFICATION_COMPARATOR;
            } else {
                comparatorObj = ApplicationsState.ALPHA_COMPARATOR;
            }

            final AppFilter finalFilterObj = new CompoundFilter(filterObj,
                    ApplicationsState.FILTER_NOT_HIDE);
            ThreadUtils.postOnBackgroundThread(() -> {
                mSession.rebuild(finalFilterObj, comparatorObj, false);
            });
        }

        @VisibleForTesting
        void filterSearch(String query) {
            if (mSearchFilter == null) {
                mSearchFilter = new SearchFilter();
            }
            // If we haven't load apps list completely, don't filter anything.
            if (mOriginalEntries == null) {
                Log.w(TAG, "Apps haven't loaded completely yet, so nothing can be filtered");
                return;
            }
            mSearchFilter.filter(query);
        }

        private static boolean packageNameEquals(PackageItemInfo info1, PackageItemInfo info2) {
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
            ArrayList<ApplicationsState.AppEntry> returnEntries = new ArrayList<>(size);

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
            if (DEBUG) {
                Log.d(TAG, "onRebuildComplete size=" + entries.size());
            }

            // Preload top visible icons of app list.
            AppUtils.preloadTopIcons(mContext, entries,
                    mContext.getResources().getInteger(R.integer.config_num_visible_app_icons));

            final int filterType = mAppFilter.getFilterType();
            if (filterType == FILTER_APPS_POWER_ALLOWLIST
                    || filterType == FILTER_APPS_POWER_ALLOWLIST_ALL) {
                entries = removeDuplicateIgnoringUser(entries);
            }
            mEntries = entries;
            mOriginalEntries = entries;
            notifyDataSetChanged();
            if (getItemCount() == 0) {
                mLoadingViewController.showEmpty(false /* animate */);
            } else {
                mLoadingViewController.showContent(false /* animate */);

                if (mManageApplications.mSearchView != null
                        && mManageApplications.mSearchView.isVisibleToUser()) {
                    final CharSequence query = mManageApplications.mSearchView.getQuery();
                    if (!TextUtils.isEmpty(query)) {
                        filterSearch(query.toString());
                    }
                }
            }
            // Restore the last scroll position if the number of entries added so far is bigger than
            // it.
            if (mLastIndex != -1 && getItemCount() > mLastIndex) {
                mManageApplications.mRecyclerView.getLayoutManager().scrollToPosition(mLastIndex);
                mLastIndex = -1;
            }

            if (mManageApplications.mListType == LIST_TYPE_USAGE_ACCESS) {
                // No enabled or disabled filters for usage access.
                return;
            }

            mManageApplications.setHasDisabled(mState.haveDisabledApps());
            mManageApplications.setHasInstant(mState.haveInstantApps());
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

        @Override
        public void onExtraInfoUpdated() {
            mHasReceivedBridgeCallback = true;
            rebuild();
        }

        @Override
        public void onRunningStateChanged(boolean running) {
            mManageApplications.getActivity().setProgressBarIndeterminateVisibility(running);
        }

        @Override
        public void onPackageListChanged() {
            rebuild();
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
            rebuild();
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            if (mEntries == null) {
                return;
            }
            final int size = mEntries.size();
            for (int i = 0; i < size; i++) {
                final AppEntry entry = mEntries.get(i);
                final ApplicationInfo info = entry.info;
                if (info == null && !TextUtils.equals(packageName, info.packageName)) {
                    continue;
                }
                if (TextUtils.equals(mManageApplications.mCurrentPkgName, info.packageName)) {
                    // We got the size information for the last app the
                    // user viewed, and are sorting by size...  they may
                    // have cleared data, so we immediately want to resort
                    // the list with the new size to reflect it to the user.
                    rebuild();
                    return;
                } else {
                    mOnScrollListener.postNotifyItemChange(i);
                }
            }
        }

        @Override
        public void onLauncherInfoChanged() {
            if (!mManageApplications.mShowSystem) {
                rebuild();
            }
        }

        @Override
        public void onAllSizesComputed() {
            if (mLastSortMode == R.id.sort_order_size) {
                rebuild();
            }
        }

        /**
         * Item count include all items. If UI has a header on the app list, it shall shift 1 to
         * application count for the total item count.
         */
        @Override
        public int getItemCount() {
            int count = getApplicationCount();
            if (count != 0 && mManageApplications.mListType == LIST_TYPE_APPS_LOCALE) {
                count++;
            }
            return count;
        }

        public int getApplicationCount() {
            return mEntries != null ? mEntries.size() : 0;
        }

        public AppEntry getAppEntry(int applicationPosition) {
            return mEntries.get(applicationPosition);
        }

        /**
         * Item Id follows all item on the app list. If UI has a header on the list, it shall
         * shift 1 to the position for correct app entry.
         */
        @Override
        public long getItemId(int position) {
            int applicationPosition =
                    getApplicationPosition(mManageApplications.mListType, position);
            if (applicationPosition == mEntries.size()
                    || applicationPosition == RecyclerView.NO_POSITION) {
                return -1;
            }
            return mEntries.get(applicationPosition).id;
        }

        /**
         * Check item in the list shall enable or disable.
         * @param position The item position in the list
         */
        public boolean isEnabled(int position) {
            int itemViewType = getItemViewType(position);
            if (itemViewType == VIEW_TYPE_EXTRA_VIEW || itemViewType == VIEW_TYPE_APP_HEADER
                    || mManageApplications.mListType != LIST_TYPE_HIGH_POWER) {
                return true;
            }

            int applicationPosition =
                    getApplicationPosition(mManageApplications.mListType, position);
            if (applicationPosition == RecyclerView.NO_POSITION) {
                return true;
            }
            ApplicationsState.AppEntry entry = mEntries.get(applicationPosition);

            return !mBackend.isSysAllowlisted(entry.info.packageName)
                    && !mBackend.isDefaultActiveApp(entry.info.packageName);
        }

        @Override
        public void onBindViewHolder(ApplicationViewHolder holder, int position) {
            if (getItemViewType(position) == VIEW_TYPE_APP_HEADER) {
                // It does not bind holder here, due to header view.
                return;
            }

            int applicationPosition =
                    getApplicationPosition(mManageApplications.mListType, position);
            if (applicationPosition == RecyclerView.NO_POSITION) {
                return;
            }
            // Bind the data efficiently with the holder
            // If there is a header on the list, the position shall be shifted. Thus, it shall use
            // #getApplicationPosition to get real application position for the app entry.
            final ApplicationsState.AppEntry entry = mEntries.get(applicationPosition);

            synchronized (entry) {
                mState.ensureLabelDescription(entry);
                holder.setTitle(entry.label, entry.labelDescription);
                updateIcon(holder, entry);
                updateSummary(holder, entry);
                updateSwitch(holder, entry);
                holder.updateDisableView(entry.info);
            }
            holder.setEnabled(isEnabled(position));

            holder.itemView.setOnClickListener(mManageApplications);
        }

        private void updateIcon(ApplicationViewHolder holder, AppEntry entry) {
            final Drawable cachedIcon = AppUtils.getIconFromCache(entry);
            if (cachedIcon != null && entry.mounted) {
                holder.setIcon(cachedIcon);
            } else {
                ThreadUtils.postOnBackgroundThread(() -> {
                    final Drawable icon = AppUtils.getIcon(mContext, entry);
                    if (icon != null) {
                        ThreadUtils.postOnMainThread(() -> holder.setIcon(icon));
                    }
                });
            }
        }

        private void updateSummary(ApplicationViewHolder holder, AppEntry entry) {
            switch (mManageApplications.mListType) {
                case LIST_TYPE_NOTIFICATION:
                    if (entry.extraInfo != null
                            && entry.extraInfo instanceof NotificationsSentState) {
                        holder.setSummary(AppStateNotificationBridge.getSummary(mContext,
                                (NotificationsSentState) entry.extraInfo, mLastSortMode));
                    } else {
                        holder.setSummary(null);
                    }
                    break;
                case LIST_TYPE_USAGE_ACCESS:
                    if (entry.extraInfo != null) {
                        holder.setSummary(
                                (new UsageState((PermissionState) entry.extraInfo)).isPermissible()
                                        ? R.string.app_permission_summary_allowed
                                        : R.string.app_permission_summary_not_allowed);
                    } else {
                        holder.setSummary(null);
                    }
                    break;
                case LIST_TYPE_HIGH_POWER:
                    holder.setSummary(HighPowerDetail.getSummary(mContext, entry));
                    break;
                case LIST_TYPE_OVERLAY:
                    holder.setSummary(DrawOverlayDetails.getSummary(mContext, entry));
                    break;
                case LIST_TYPE_WRITE_SETTINGS:
                    holder.setSummary(WriteSettingsDetails.getSummary(mContext, entry));
                    break;
                case LIST_TYPE_MANAGE_SOURCES:
                    holder.setSummary(ExternalSourcesDetails.getPreferenceSummary(mContext, entry));
                    break;
                case LIST_TYPE_WIFI_ACCESS:
                    holder.setSummary(ChangeWifiStateDetails.getSummary(mContext, entry));
                    break;
                case LIST_MANAGE_EXTERNAL_STORAGE:
                    holder.setSummary(ManageExternalStorageDetails.getSummary(mContext, entry));
                    break;
                case LIST_TYPE_ALARMS_AND_REMINDERS:
                    holder.setSummary(AlarmsAndRemindersDetails.getSummary(mContext, entry));
                    break;
                case LIST_TYPE_MEDIA_MANAGEMENT_APPS:
                    holder.setSummary(MediaManagementAppsDetails.getSummary(mContext, entry));
                    break;
                case LIST_TYPE_APPS_LOCALE:
                    holder.setSummary(AppLocaleDetails.getSummary(mContext, entry));
                    break;
                default:
                    holder.updateSizeText(entry, mManageApplications.mInvalidSizeStr, mWhichSize);
                    break;
            }
        }

        private void updateSwitch(ApplicationViewHolder holder, AppEntry entry) {
            switch (mManageApplications.mListType) {
                case LIST_TYPE_NOTIFICATION:
                    holder.updateSwitch(((AppStateNotificationBridge) mExtraInfoBridge)
                                    .getSwitchOnCheckedListener(entry),
                            AppStateNotificationBridge.enableSwitch(entry),
                            AppStateNotificationBridge.checkSwitch(entry));
                    if (entry.extraInfo != null
                            && entry.extraInfo instanceof NotificationsSentState) {
                        holder.setSummary(AppStateNotificationBridge.getSummary(mContext,
                                (NotificationsSentState) entry.extraInfo, mLastSortMode));
                    } else {
                        holder.setSummary(null);
                    }
                    break;
            }
        }

        /**
         * Adjusts position if this list adds a header.
         * TODO(b/232533002) Add a header view on adapter of RecyclerView may not a good idea since
         * ManageApplication is a generic purpose. In the future, here shall look for
         * a better way to add a header without using recyclerView or any other ways
         * to achieve the goal.
         */
        public static int getApplicationPosition(int listType, int position) {
            int applicationPosition = position;
            // Adjust position due to header added.
            if (listType == LIST_TYPE_APPS_LOCALE) {
                applicationPosition = position > 0 ? position - 1 : RecyclerView.NO_POSITION;
            }
            return applicationPosition;
        }

        public static class OnScrollListener extends RecyclerView.OnScrollListener {
            private int mScrollState = SCROLL_STATE_IDLE;
            private boolean mDelayNotifyDataChange;
            private ApplicationsAdapter mAdapter;

            public OnScrollListener(ApplicationsAdapter adapter) {
                mAdapter = adapter;
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                mScrollState = newState;
                if (mScrollState == SCROLL_STATE_IDLE && mDelayNotifyDataChange) {
                    mDelayNotifyDataChange = false;
                    mAdapter.notifyDataSetChanged();
                }
            }

            public void postNotifyItemChange(int index) {
                if (mScrollState == SCROLL_STATE_IDLE) {
                    mAdapter.notifyItemChanged(index);
                } else {
                    mDelayNotifyDataChange = true;
                }
            }
        }

        /**
         * An array filter that constrains the content of the array adapter with a substring.
         * Item that does not contains the specified substring will be removed from the list.</p>
         */
        private class SearchFilter extends Filter {
            @WorkerThread
            @Override
            protected FilterResults performFiltering(CharSequence query) {
                final ArrayList<ApplicationsState.AppEntry> matchedEntries;
                if (TextUtils.isEmpty(query)) {
                    matchedEntries = mOriginalEntries;
                } else {
                    matchedEntries = new ArrayList<>();
                    for (ApplicationsState.AppEntry entry : mOriginalEntries) {
                        if (entry.label.toLowerCase().contains(query.toString().toLowerCase())) {
                            matchedEntries.add(entry);
                        }
                    }
                }
                final FilterResults results = new FilterResults();
                results.values = matchedEntries;
                results.count = matchedEntries.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mEntries = (ArrayList<ApplicationsState.AppEntry>) results.values;
                notifyDataSetChanged();
            }
        }
    }
}

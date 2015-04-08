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
import android.content.pm.IPackageManager;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
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
import android.widget.ListView;
import android.widget.Spinner;

import com.android.internal.content.PackageHelper;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.Settings.AllApplicationsActivity;
import com.android.settings.Settings.NotificationAppListActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.ApplicationsState.AppEntry;
import com.android.settings.applications.ApplicationsState.AppFilter;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settings.Settings.DomainsURLsAppListActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class CanBeOnSdCardChecker {
    final IPackageManager mPm;
    int mInstallLocation;

    CanBeOnSdCardChecker() {
        mPm = IPackageManager.Stub.asInterface(
                ServiceManager.getService("package"));
    }

    void init() {
        try {
            mInstallLocation = mPm.getInstallLocation();
        } catch (RemoteException e) {
            Log.e("CanBeOnSdCardChecker", "Is Package Manager running?");
            return;
        }
    }

    boolean check(ApplicationInfo info) {
        boolean canBe = false;
        if ((info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
            canBe = true;
        } else {
            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                if (info.installLocation == PackageInfo.INSTALL_LOCATION_PREFER_EXTERNAL ||
                        info.installLocation == PackageInfo.INSTALL_LOCATION_AUTO) {
                    canBe = true;
                } else if (info.installLocation
                        == PackageInfo.INSTALL_LOCATION_UNSPECIFIED) {
                    if (mInstallLocation == PackageHelper.APP_INSTALL_EXTERNAL) {
                        // For apps with no preference and the default value set
                        // to install on sdcard.
                        canBe = true;
                    }
                }
            }
        }
        return canBe;
    }
}

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

    private static final String EXTRA_SORT_ORDER = "sortOrder";

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
    public static final int FILTER_APPS_DOWNLOADED_AND_LAUNCHER = 0;
    public static final int FILTER_APPS_ALL                     = 1;
    public static final int FILTER_APPS_ENABLED                 = 2;
    public static final int FILTER_APPS_DISABLED                = 3;
    public static final int FILTER_APPS_BLOCKED                 = 4;
    public static final int FILTER_APPS_PRIORITY                = 5;
    public static final int FILTER_APPS_SENSITIVE               = 6;
    public static final int FILTER_APPS_PERSONAL                = 7;
    public static final int FILTER_APPS_WORK                    = 8;
    public static final int FILTER_APPS_WITH_DOMAIN_URLS        = 9;

    // This is the string labels for the filter modes above, the order must be kept in sync.
    public static final int[] FILTER_LABELS = new int[] {
        R.string.filter_all_apps,      // Downloaded and launcher, spinner not shown in this case
        R.string.filter_all_apps,      // All apps
        R.string.filter_enabled_apps,  // Enabled
        R.string.filter_apps_disabled, // Disabled
        R.string.filter_notif_blocked_apps,   // Blocked Notifications
        R.string.filter_notif_priority_apps,  // Priority Notifications
        R.string.filter_notif_sensitive_apps, // Sensitive Notifications
        R.string.filter_personal_apps, // Personal
        R.string.filter_work_apps,     // Work
        R.string.filter_with_domain_urls_apps,     // Domain URLs
    };
    // This is the actual mapping to filters from FILTER_ constants above, the order must
    // be kept in sync.
    public static final AppFilter[] FILTERS = new AppFilter[] {
        ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER, // Downloaded and launcher
        ApplicationsState.FILTER_EVERYTHING,  // All apps
        ApplicationsState.FILTER_ALL_ENABLED, // Enabled
        ApplicationsState.FILTER_DISABLED,    // Disabled
        AppStateNotificationBridge.FILTER_APP_NOTIFICATION_BLOCKED,   // Blocked Notifications
        AppStateNotificationBridge.FILTER_APP_NOTIFICATION_PRIORITY,  // Priority Notifications
        AppStateNotificationBridge.FILTER_APP_NOTIFICATION_SENSITIVE, // Sensitive Notifications
        ApplicationsState.FILTER_PERSONAL,    // Personal
        ApplicationsState.FILTER_WORK,        // Work
        ApplicationsState.FILTER_WITH_DOMAIN_URLS,   // Apps with Domain URLs
    };

    // sort order that can be changed through the menu can be sorted alphabetically
    // or size(descending)
    private static final int MENU_OPTIONS_BASE = 0;
    public static final int SORT_ORDER_ALPHA = MENU_OPTIONS_BASE + 1;
    public static final int SORT_ORDER_SIZE = MENU_OPTIONS_BASE + 2;
    public static final int RESET_APP_PREFERENCES = MENU_OPTIONS_BASE + 3;
    // sort order
    private int mSortOrder = SORT_ORDER_ALPHA;

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

    private Menu mOptionsMenu;

    public static final int LIST_TYPE_MAIN = 0;
    public static final int LIST_TYPE_ALL = 1;
    public static final int LIST_TYPE_NOTIFICATION = 2;
    public static final int LIST_TYPE_DOMAINS_URLS = 3;

    private View mRootView;

    private View mSpinnerHeader;
    private Spinner mFilterSpinner;
    private FilterSpinnerAdapter mFilterAdapter;
    private NotificationBackend mNotifBackend;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());

        Intent intent = getActivity().getIntent();
        String className = getArguments() != null
                ? getArguments().getString("classname") : null;
        if (className == null) {
            className = intent.getComponent().getClassName();
        }
        if (className.equals(AllApplicationsActivity.class.getName())) {
            mListType = LIST_TYPE_ALL;
        } else if (className.equals(NotificationAppListActivity.class.getName())) {
            mListType = LIST_TYPE_NOTIFICATION;
            mNotifBackend = new NotificationBackend();
        } else if (className.equals(DomainsURLsAppListActivity.class.getName())) {
            mListType = LIST_TYPE_DOMAINS_URLS;
        } else {
            mListType = LIST_TYPE_MAIN;
        }
        mFilter = getDefaultFilter();

        if (savedInstanceState != null) {
            mSortOrder = savedInstanceState.getInt(EXTRA_SORT_ORDER, mSortOrder);
        }

        mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // initialize the inflater
        mInflater = inflater;

        createHeader();

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
            mListView.setAdapter(mApplications);
            mListView.setRecyclerListener(mApplications);

            Utils.prepareCustomPreferencesList(container, mRootView, mListView, false);
        }

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) mRootView.getLayoutParams()).removeBorders = true;
        }

        return mRootView;
    }

    private void createHeader() {
        Activity activity = getActivity();
        View content = activity.findViewById(R.id.main_content);
        ViewGroup contentParent = (ViewGroup) content.getParent();
        mSpinnerHeader = (ViewGroup) activity.getLayoutInflater()
                .inflate(R.layout.apps_filter_spinner, contentParent, false);
        mFilterSpinner = (Spinner) mSpinnerHeader.findViewById(R.id.filter_spinner);
        mFilterAdapter = new FilterSpinnerAdapter(this);
        mFilterSpinner.setAdapter(mFilterAdapter);
        mFilterSpinner.setOnItemSelectedListener(this);
        contentParent.addView(mSpinnerHeader, 0);

        mFilterAdapter.enableFilter(getDefaultFilter());
        if (mListType != LIST_TYPE_MAIN) {
            if (UserManager.get(getActivity()).getUserProfiles().size() > 1) {
                mFilterAdapter.enableFilter(FILTER_APPS_PERSONAL);
                mFilterAdapter.enableFilter(FILTER_APPS_WORK);
            }
        }
        if (mListType == LIST_TYPE_NOTIFICATION) {
            mFilterAdapter.enableFilter(FILTER_APPS_BLOCKED);
            mFilterAdapter.enableFilter(FILTER_APPS_PRIORITY);
            mFilterAdapter.enableFilter(FILTER_APPS_SENSITIVE);
        } else if (mListType == LIST_TYPE_DOMAINS_URLS) {
            mFilterAdapter.disableFilter(FILTER_APPS_ALL);
            mFilterAdapter.enableFilter(FILTER_APPS_WITH_DOMAIN_URLS);
        }
    }

    private int getDefaultFilter() {
        if (mListType == LIST_TYPE_MAIN) {
            return FILTER_APPS_DOWNLOADED_AND_LAUNCHER;
        } else if (mListType == LIST_TYPE_DOMAINS_URLS) {
            return FILTER_APPS_WITH_DOMAIN_URLS;
        }
        return FILTER_APPS_ALL;
    }

    @Override
    protected int getMetricsCategory() {
        switch (mListType) {
            case LIST_TYPE_MAIN:
                return MetricsLogger.MANAGE_APPLICATIONS;
            case LIST_TYPE_ALL:
                return MetricsLogger.MANAGE_APPLICATIONS_ALL;
            case LIST_TYPE_NOTIFICATION:
                return MetricsLogger.MANAGE_APPLICATIONS_NOTIFICATIONS;
            case LIST_TYPE_DOMAINS_URLS:
                return MetricsLogger.MANAGE_DOMAIN_URLS;
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
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SORT_ORDER, mSortOrder);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mApplications != null) {
            mApplications.pause();
        }
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
                mApplications.mNotifBridge.forceUpdate(mCurrentPkgName, mCurrentUid);
            } else {
                mApplicationsState.requestSize(mCurrentPkgName, UserHandle.getUserId(mCurrentUid));
            }
        }
    }

    // utility method used to start sub activity
    private void startApplicationDetailsActivity() {
        Activity activity = getActivity();
        if (mListType == LIST_TYPE_NOTIFICATION) {
            activity.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, mCurrentPkgName)
                    .putExtra(Settings.EXTRA_APP_UID, mCurrentUid));
        } else if (mListType == LIST_TYPE_DOMAINS_URLS) {
            final String title = getString(R.string.auto_launch_label);
            startAppInfoFragment(AppLaunchSettings.class, title);
        } else {
            // TODO: Figure out if there is a way where we can spin up the profile's settings
            // process ahead of time, to avoid a long load of data when user clicks on a managed app.
            // Maybe when they load the list of apps that contains managed profile apps.
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", mCurrentPkgName, null));
            activity.startActivityAsUser(intent, new UserHandle(UserHandle.getUserId(mCurrentUid)));
        }
    }

    private void startAppInfoFragment(Class<? extends AppInfoBase> fragment, CharSequence title) {
        Bundle args = new Bundle();
        args.putString("package", mCurrentPkgName);

        SettingsActivity sa = (SettingsActivity) getActivity();
        sa.startPreferencePanel(fragment.getName(), args, -1, title, this, 0);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mListType == LIST_TYPE_DOMAINS_URLS) {
            // No option menu
            return;
        }
        mOptionsMenu = menu;
        if (mListType == LIST_TYPE_MAIN) {
            // Only show advanced options when in the main app list (from dashboard).
            inflater.inflate(R.menu.manage_apps, menu);
        }
        menu.add(0, SORT_ORDER_ALPHA, 1, R.string.sort_order_alpha)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, SORT_ORDER_SIZE, 2, R.string.sort_order_size)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
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
        if (mListType != LIST_TYPE_MAIN) {
            // Allow sorting except on main apps list.
            mOptionsMenu.findItem(SORT_ORDER_ALPHA).setVisible(mSortOrder != SORT_ORDER_ALPHA);
            mOptionsMenu.findItem(SORT_ORDER_SIZE).setVisible(mSortOrder != SORT_ORDER_SIZE);
        } else {
            mOptionsMenu.findItem(SORT_ORDER_ALPHA).setVisible(false);
            mOptionsMenu.findItem(SORT_ORDER_SIZE).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        switch(item.getItemId()) {
            case SORT_ORDER_ALPHA:
            case SORT_ORDER_SIZE:
                mSortOrder = menuId;
                if (mApplications != null) {
                    mApplications.rebuild(mSortOrder);
                }
                break;
            case R.id.advanced:
                ((SettingsActivity) getActivity()).startPreferencePanel(
                        AdvancedAppSettings.class.getName(), null, R.string.advanced_apps,
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
            if (isAppEntryViewEnabled(entry)) {
                startApplicationDetailsActivity();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mApplications.setFilter(mFilterAdapter.getFilter(position));
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
        if (mListType == LIST_TYPE_MAIN) {
            // No filtering on main app list.
            return;
        }
        if (hasDisabledApps) {
            mFilterAdapter.enableFilter(FILTER_APPS_ENABLED);
            mFilterAdapter.enableFilter(FILTER_APPS_DISABLED);
        } else {
            mFilterAdapter.disableFilter(FILTER_APPS_ENABLED);
            mFilterAdapter.disableFilter(FILTER_APPS_DISABLED);
        }
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

        public void enableFilter(int filter) {
            if (mFilterOptions.contains(filter)) return;
            mFilterOptions.add(filter);
            Collections.sort(mFilterOptions);
            mManageApplications.mSpinnerHeader.setVisibility(
                    mFilterOptions.size() > 1 ? View.VISIBLE : View.GONE);
            notifyDataSetChanged();
        }

        public void disableFilter(int filter) {
            if (!mFilterOptions.remove((Integer) filter)) {
                return;
            }
            Collections.sort(mFilterOptions);
            mManageApplications.mSpinnerHeader.setVisibility(
                    mFilterOptions.size() > 1 ? View.VISIBLE : View.GONE);
            notifyDataSetChanged();
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

    private static boolean isAppEntryViewEnabled(AppEntry entry) {
        if ((entry.info.flags&ApplicationInfo.FLAG_INSTALLED) == 0 || !entry.info.enabled) {
            return false;
        }
        return true;
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
            ApplicationsState.Callbacks, AppStateNotificationBridge.Callback,
            AbsListView.RecyclerListener {
        private final ApplicationsState mState;
        private final ApplicationsState.Session mSession;
        private final ManageApplications mManageApplications;
        private final Context mContext;
        private final ArrayList<View> mActive = new ArrayList<View>();
        private final AppStateNotificationBridge mNotifBridge;
        private int mFilterMode;
        private ArrayList<ApplicationsState.AppEntry> mBaseEntries;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private boolean mResumed;
        private int mLastSortMode=-1;
        private int mWhichSize = SIZE_TOTAL;
        CharSequence mCurFilterPrefix;
        private PackageManager mPm;

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
                mNotifBridge = new AppStateNotificationBridge(
                        mContext.getPackageManager(), mState,
                        manageApplications.mNotifBackend, this);
            } else {
                mNotifBridge = null;
            }
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
                if (mNotifBridge != null) {
                    mNotifBridge.resume();
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
                if (mNotifBridge != null) {
                    mNotifBridge.pause();
                }
            }
        }

        public void release() {
            mSession.release();
            if (mNotifBridge != null) {
                mNotifBridge.release();
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
            switch (mLastSortMode) {
                case SORT_ORDER_SIZE:
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

            if (entries == null) {
                mManageApplications.mListContainer.setVisibility(View.INVISIBLE);
                mManageApplications.mLoadingContainer.setVisibility(View.VISIBLE);
            } else {
                mManageApplications.mListContainer.setVisibility(View.VISIBLE);
                mManageApplications.mLoadingContainer.setVisibility(View.GONE);
            }

            mManageApplications.setHasDisabled(hasDisabledApps());
        }

        private boolean hasDisabledApps() {
            ArrayList<AppEntry> allApps = mSession.getAllApps();
            for (int i = 0; i < allApps.size(); i++) {
                if (!allApps.get(i).info.enabled) {
                    return true;
                }
            }
            return false;
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
        public void onNotificationInfoUpdated() {
            if (mFilterMode != mManageApplications.getDefaultFilter()) {
                rebuild(false);
            }
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
            // No op.
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            for (int i=0; i<mActive.size(); i++) {
                AppViewHolder holder = (AppViewHolder)mActive.get(i).getTag();
                if (holder.entry.info.packageName.equals(packageName)) {
                    synchronized (holder.entry) {
                        if (mManageApplications.mListType != LIST_TYPE_NOTIFICATION) {
                            holder.updateSizeText(mManageApplications.mInvalidSizeStr, mWhichSize);
                        }
                    }
                    if (holder.entry.info.packageName.equals(mManageApplications.mCurrentPkgName)
                            && mLastSortMode == SORT_ORDER_SIZE) {
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
            if (mFilterMode == FILTER_APPS_DOWNLOADED_AND_LAUNCHER) {
                rebuild(false);
            }
        }

        @Override
        public void onAllSizesComputed() {
            if (mLastSortMode == SORT_ORDER_SIZE) {
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
                switch (mManageApplications.mListType) {
                    case LIST_TYPE_NOTIFICATION:
                        if (entry.extraInfo != null) {
                            holder.summary.setText(InstalledAppDetails.getNotificationSummary(
                                    (AppRow) entry.extraInfo, mContext));
                        } else {
                            holder.summary.setText("");
                        }
                        break;

                    case LIST_TYPE_DOMAINS_URLS:
                        holder.summary.setText(getDomainsSummary(entry.info.packageName));
                        break;

                    default:
                        holder.updateSizeText(mManageApplications.mInvalidSizeStr, mWhichSize);
                        break;
                }
                convertView.setEnabled(isAppEntryViewEnabled(entry));
                if ((entry.info.flags&ApplicationInfo.FLAG_INSTALLED) == 0) {
                    holder.disabled.setVisibility(View.VISIBLE);
                    holder.disabled.setText(R.string.not_installed);
                } else if (!entry.info.enabled) {
                    holder.disabled.setVisibility(View.VISIBLE);
                    holder.disabled.setText(R.string.disabled);
                } else {
                    holder.disabled.setVisibility(View.GONE);
                }
                holder.checkBox.setVisibility(View.GONE);
            }
            mActive.remove(convertView);
            mActive.add(convertView);
            return convertView;
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
            ArraySet<String> result = new ArraySet<>();
            List<IntentFilterVerificationInfo> list =
                    mPm.getIntentFilterVerifications(packageName);
            for (IntentFilterVerificationInfo ivi : list) {
                for (String host : ivi.getDomains()) {
                    result.add(host);
                }
            }
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

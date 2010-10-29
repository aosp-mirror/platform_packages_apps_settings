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

import com.android.internal.content.PackageHelper;
import com.android.settings.R;
import com.android.settings.applications.ApplicationsState.AppEntry;

import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.Comparator;

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
            if ((info.flags & ApplicationInfo.FLAG_FORWARD_LOCK) == 0 &&
                    (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
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
public class ManageApplications extends TabActivity implements
        OnItemClickListener, DialogInterface.OnCancelListener,
        TabHost.TabContentFactory, TabHost.OnTabChangeListener {
    static final String TAG = "ManageApplications";
    static final boolean DEBUG = false;
    
    // attributes used as keys when passing values to InstalledAppDetails activity
    public static final String APP_CHG = "chg";
    
    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;

    // sort order that can be changed through the menu can be sorted alphabetically
    // or size(descending)
    private static final int MENU_OPTIONS_BASE = 0;
    // Filter options used for displayed list of applications
    public static final int FILTER_APPS_ALL = MENU_OPTIONS_BASE + 0;
    public static final int FILTER_APPS_THIRD_PARTY = MENU_OPTIONS_BASE + 1;
    public static final int FILTER_APPS_SDCARD = MENU_OPTIONS_BASE + 2;

    public static final int SORT_ORDER_ALPHA = MENU_OPTIONS_BASE + 4;
    public static final int SORT_ORDER_SIZE = MENU_OPTIONS_BASE + 5;
    public static final int SHOW_RUNNING_SERVICES = MENU_OPTIONS_BASE + 6;
    public static final int SHOW_BACKGROUND_PROCESSES = MENU_OPTIONS_BASE + 7;
    // sort order
    private int mSortOrder = SORT_ORDER_ALPHA;
    // Filter value
    private int mFilterApps = FILTER_APPS_THIRD_PARTY;
    
    private ApplicationsState mApplicationsState;
    private ApplicationsAdapter mApplicationsAdapter;
    
    // Size resource used for packages whose size computation failed for some reason
    private CharSequence mInvalidSizeStr;
    private CharSequence mComputingSizeStr;
    
    // layout inflater object used to inflate views
    private LayoutInflater mInflater;
    
    private String mCurrentPkgName;
    
    private View mLoadingContainer;

    private View mListContainer;

    // ListView used to display list
    private ListView mListView;
    // Custom view used to display running processes
    private RunningProcessesView mRunningProcessesView;
    
    LinearColorBar mColorBar;
    TextView mStorageChartLabel;
    TextView mUsedStorageText;
    TextView mFreeStorageText;

    // These are for keeping track of activity and tab switch state.
    private int mCurView;
    private boolean mCreatedRunning;

    private boolean mResumedRunning;
    private boolean mActivityResumed;
    private Object mNonConfigInstance;
    
    private StatFs mDataFileStats;
    private StatFs mSDCardFileStats;
    private boolean mLastShowedInternalStorage = true;
    private long mLastUsedStorage, mLastAppStorage, mLastFreeStorage;

    final Runnable mRunningProcessesAvail = new Runnable() {
        public void run() {
            handleRunningProcessesAvail();
        }
    };

    // View Holder used when displaying views
    static class AppViewHolder {
        ApplicationsState.AppEntry entry;
        TextView appName;
        ImageView appIcon;
        TextView appSize;
        TextView disabled;
        CheckBox checkBox;
        
        void updateSizeText(ManageApplications ma) {
            if (DEBUG) Log.i(TAG, "updateSizeText of " + entry.label + " " + entry
                    + ": " + entry.sizeStr);
            if (entry.sizeStr != null) {
                appSize.setText(entry.sizeStr);
            } else if (entry.size == ApplicationsState.SIZE_INVALID) {
                appSize.setText(ma.mInvalidSizeStr);
            }
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
    class ApplicationsAdapter extends BaseAdapter implements Filterable,
            ApplicationsState.Callbacks, AbsListView.RecyclerListener {
        private final ApplicationsState mState;
        private final ArrayList<View> mActive = new ArrayList<View>();
        private ArrayList<ApplicationsState.AppEntry> mBaseEntries;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private boolean mResumed;
        private int mLastFilterMode=-1, mLastSortMode=-1;
        private boolean mWaitingForData;
        CharSequence mCurFilterPrefix;

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
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mCurFilterPrefix = constraint;
                mEntries = (ArrayList<ApplicationsState.AppEntry>)results.values;
                notifyDataSetChanged();
                updateStorageUsage();
            }
        };

        public ApplicationsAdapter(ApplicationsState state) {
            mState = state;
        }

        public void resume(int filter, int sort) {
            if (DEBUG) Log.i(TAG, "Resume!  mResumed=" + mResumed);
            if (!mResumed) {
                mResumed = true;
                mState.resume(this);
                mLastFilterMode = filter;
                mLastSortMode = sort;
                rebuild(true);
            } else {
                rebuild(filter, sort);
            }
        }

        public void pause() {
            if (mResumed) {
                mResumed = false;
                mState.pause();
            }
        }

        public void rebuild(int filter, int sort) {
            if (filter == mLastFilterMode && sort == mLastSortMode) {
                return;
            }
            mLastFilterMode = filter;
            mLastSortMode = sort;
            rebuild(true);
        }
        
        public void rebuild(boolean eraseold) {
            if (DEBUG) Log.i(TAG, "Rebuilding app list...");
            ApplicationsState.AppFilter filterObj;
            Comparator<AppEntry> comparatorObj;
            switch (mLastFilterMode) {
                case FILTER_APPS_THIRD_PARTY:
                    filterObj = ApplicationsState.THIRD_PARTY_FILTER;
                    break;
                case FILTER_APPS_SDCARD:
                    filterObj = ApplicationsState.ON_SD_CARD_FILTER;
                    break;
                default:
                    filterObj = null;
                    break;
            }
            switch (mLastSortMode) {
                case SORT_ORDER_SIZE:
                    comparatorObj = ApplicationsState.SIZE_COMPARATOR;
                    break;
                default:
                    comparatorObj = ApplicationsState.ALPHA_COMPARATOR;
                    break;
            }
            ArrayList<ApplicationsState.AppEntry> entries
                    = mState.rebuild(filterObj, comparatorObj);
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
            updateStorageUsage();

            if (entries == null) {
                mWaitingForData = true;
                mListContainer.setVisibility(View.INVISIBLE);
                mLoadingContainer.setVisibility(View.VISIBLE);
            } else {
                mListContainer.setVisibility(View.VISIBLE);
                mLoadingContainer.setVisibility(View.GONE);
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
        public void onRunningStateChanged(boolean running) {
            setProgressBarIndeterminateVisibility(running);
        }

        @Override
        public void onRebuildComplete(ArrayList<AppEntry> apps) {
            if (mLoadingContainer.getVisibility() == View.VISIBLE) {
                mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                        ManageApplications.this, android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        ManageApplications.this, android.R.anim.fade_in));
            }
            mListContainer.setVisibility(View.VISIBLE);
            mLoadingContainer.setVisibility(View.GONE);
            mWaitingForData = false;
            mBaseEntries = apps;
            mEntries = applyPrefixFilter(mCurFilterPrefix, mBaseEntries);
            notifyDataSetChanged();
            updateStorageUsage();
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
        public void onPackageSizeChanged(String packageName) {
            for (int i=0; i<mActive.size(); i++) {
                AppViewHolder holder = (AppViewHolder)mActive.get(i).getTag();
                if (holder.entry.info.packageName.equals(packageName)) {
                    synchronized (holder.entry) {
                        holder.updateSizeText(ManageApplications.this);
                    }
                    if (holder.entry.info.packageName.equals(mCurrentPkgName)
                            && mLastSortMode == SORT_ORDER_SIZE) {
                        // We got the size information for the last app the
                        // user viewed, and are sorting by size...  they may
                        // have cleared data, so we immediately want to resort
                        // the list with the new size to reflect it to the user.
                        rebuild(false);
                    }
                    updateStorageUsage();
                    return;
                }
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
        
        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unnecessary calls
            // to findViewById() on each row.
            AppViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied
            // by ListView is null.
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.manage_applications_item, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new AppViewHolder();
                holder.appName = (TextView) convertView.findViewById(R.id.app_name);
                holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                holder.appSize = (TextView) convertView.findViewById(R.id.app_size);
                holder.disabled = (TextView) convertView.findViewById(R.id.app_disabled);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.app_on_sdcard);
                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (AppViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder
            ApplicationsState.AppEntry entry = mEntries.get(position);
            synchronized (entry) {
                holder.entry = entry;
                if (entry.label != null) {
                    holder.appName.setText(entry.label);
                    holder.appName.setTextColor(getResources().getColorStateList(
                            entry.info.enabled ? android.R.color.primary_text_dark
                                    : android.R.color.secondary_text_dark));
                }
                mState.ensureIcon(entry);
                if (entry.icon != null) {
                    holder.appIcon.setImageDrawable(entry.icon);
                }
                holder.updateSizeText(ManageApplications.this);
                if (InstalledAppDetails.SUPPORT_DISABLE_APPS) {
                    holder.disabled.setVisibility(entry.info.enabled ? View.GONE : View.VISIBLE);
                } else {
                    holder.disabled.setVisibility(View.GONE);
                }
                if (mLastFilterMode == FILTER_APPS_SDCARD) {
                    holder.checkBox.setVisibility(View.VISIBLE);
                    holder.checkBox.setChecked((entry.info.flags
                            & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0);
                } else {
                    holder.checkBox.setVisibility(View.GONE);
                }
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
    }
    
    static final String TAB_DOWNLOADED = "Downloaded";
    static final String TAB_RUNNING = "Running";
    static final String TAB_ALL = "All";
    static final String TAB_SDCARD = "OnSdCard";
    private View mRootView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplicationsState = ApplicationsState.getInstance(getApplication());
        mApplicationsAdapter = new ApplicationsAdapter(mApplicationsState);
        Intent intent = getIntent();
        String action = intent.getAction();
        String defaultTabTag = TAB_DOWNLOADED;
        if (intent.getComponent().getClassName().equals(
                "com.android.settings.RunningServices")) {
            defaultTabTag = TAB_RUNNING;
        } else if (intent.getComponent().getClassName().equals(
                "com.android.settings.applications.StorageUse")
                || action.equals(Intent.ACTION_MANAGE_PACKAGE_STORAGE)) {
            mSortOrder = SORT_ORDER_SIZE;
            mFilterApps = FILTER_APPS_ALL;
            defaultTabTag = TAB_ALL;
        } else if (action.equals(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)) {
            // Select the all-apps tab, with the default sorting
            defaultTabTag = TAB_ALL;
        }
        
        if (savedInstanceState != null) {
            mSortOrder = savedInstanceState.getInt("sortOrder", mSortOrder);
            mFilterApps = savedInstanceState.getInt("filterApps", mFilterApps);
            String tmp = savedInstanceState.getString("defaultTabTag");
            if (tmp != null) defaultTabTag = tmp;
        }
        
        mNonConfigInstance = getLastNonConfigurationInstance();
        
        mDataFileStats = new StatFs("/data");
        mSDCardFileStats = new StatFs(Environment.getExternalStorageDirectory().toString());

        // initialize some window features
        requestWindowFeature(Window.FEATURE_RIGHT_ICON);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        mInvalidSizeStr = getText(R.string.invalid_size_value);
        mComputingSizeStr = getText(R.string.computing_size);
        // initialize the inflater
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = mInflater.inflate(R.layout.manage_applications, null);
        mLoadingContainer = mRootView.findViewById(R.id.loading_container);
        mListContainer = mRootView.findViewById(R.id.list_container);
        // Create adapter and list view here
        ListView lv = (ListView) mListContainer.findViewById(android.R.id.list);
        View emptyView = mListContainer.findViewById(com.android.internal.R.id.empty);
        if (emptyView != null) {
            lv.setEmptyView(emptyView);
        }
        lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        lv.setOnItemClickListener(this);
        lv.setTextFilterEnabled(true);
        mListView = lv;
        lv.setRecyclerListener(mApplicationsAdapter);
        mListView.setAdapter(mApplicationsAdapter);
        mColorBar = (LinearColorBar)mListContainer.findViewById(R.id.storage_color_bar);
        mStorageChartLabel = (TextView)mListContainer.findViewById(R.id.storageChartLabel);
        mUsedStorageText = (TextView)mListContainer.findViewById(R.id.usedStorageText);
        mFreeStorageText = (TextView)mListContainer.findViewById(R.id.freeStorageText);
        mRunningProcessesView = (RunningProcessesView)mRootView.findViewById(
                R.id.running_processes);

        final TabHost tabHost = getTabHost();
        tabHost.addTab(tabHost.newTabSpec(TAB_DOWNLOADED)
                .setIndicator(getString(R.string.filter_apps_third_party),
                        getResources().getDrawable(R.drawable.ic_tab_download))
                .setContent(this));
        tabHost.addTab(tabHost.newTabSpec(TAB_ALL)
                .setIndicator(getString(R.string.filter_apps_all),
                        getResources().getDrawable(R.drawable.ic_tab_all))
                .setContent(this));
        tabHost.addTab(tabHost.newTabSpec(TAB_SDCARD)
                .setIndicator(getString(R.string.filter_apps_onsdcard),
                        getResources().getDrawable(R.drawable.ic_tab_sdcard))
                .setContent(this));
        tabHost.addTab(tabHost.newTabSpec(TAB_RUNNING)
                .setIndicator(getString(R.string.filter_apps_running),
                        getResources().getDrawable(R.drawable.ic_tab_running))
                .setContent(this));
        tabHost.setCurrentTabByTag(defaultTabTag);
        tabHost.setOnTabChangedListener(this);
    }
    
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActivityResumed = true;
        showCurrentTab();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("sortOrder", mSortOrder);
        outState.putInt("filterApps", mFilterApps);
        outState.putString("defautTabTag", getTabHost().getCurrentTabTag());
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mRunningProcessesView.doRetainNonConfigurationInstance();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mActivityResumed = false;
        mApplicationsAdapter.pause();
        if (mResumedRunning) {
            mRunningProcessesView.doPause();
            mResumedRunning = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (requestCode == INSTALLED_APP_DETAILS && mCurrentPkgName != null) {
            mApplicationsState.requestSize(mCurrentPkgName);
        }
    }
    
    // utility method used to start sub activity
    private void startApplicationDetailsActivity() {
        // Create intent to start new activity
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", mCurrentPkgName, null));
        // start new activity to display extended information
        startActivityForResult(intent, INSTALLED_APP_DETAILS);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, SORT_ORDER_ALPHA, 1, R.string.sort_order_alpha)
                .setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        menu.add(0, SORT_ORDER_SIZE, 2, R.string.sort_order_size)
                .setIcon(android.R.drawable.ic_menu_sort_by_size); 
        menu.add(0, SHOW_RUNNING_SERVICES, 3, R.string.show_running_services);
        menu.add(0, SHOW_BACKGROUND_PROCESSES, 3, R.string.show_background_processes);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /*
         * The running processes screen doesn't use the mApplicationsAdapter
         * so bringing up this menu in that case doesn't make any sense.
         */
        if (mCurView == VIEW_RUNNING) {
            boolean showingBackground = mRunningProcessesView.mAdapter.getShowBackground();
            menu.findItem(SORT_ORDER_ALPHA).setVisible(false);
            menu.findItem(SORT_ORDER_SIZE).setVisible(false);
            menu.findItem(SHOW_RUNNING_SERVICES).setVisible(showingBackground);
            menu.findItem(SHOW_BACKGROUND_PROCESSES).setVisible(!showingBackground);
        } else {
            menu.findItem(SORT_ORDER_ALPHA).setVisible(mSortOrder != SORT_ORDER_ALPHA);
            menu.findItem(SORT_ORDER_SIZE).setVisible(mSortOrder != SORT_ORDER_SIZE);
            menu.findItem(SHOW_RUNNING_SERVICES).setVisible(false);
            menu.findItem(SHOW_BACKGROUND_PROCESSES).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        if ((menuId == SORT_ORDER_ALPHA) || (menuId == SORT_ORDER_SIZE)) {
            mSortOrder = menuId;
            if (mCurView != VIEW_RUNNING) {
                mApplicationsAdapter.rebuild(mFilterApps, mSortOrder);
            }
        } else if (menuId == SHOW_RUNNING_SERVICES) {
            mRunningProcessesView.mAdapter.setShowBackground(false);
        } else if (menuId == SHOW_BACKGROUND_PROCESSES) {
            mRunningProcessesView.mAdapter.setShowBackground(true);
        }
        return true;
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH && event.isTracking()) {
            if (mCurView != VIEW_RUNNING) {
                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInputUnchecked(0, null);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        ApplicationsState.AppEntry entry = mApplicationsAdapter.getAppEntry(position);
        mCurrentPkgName = entry.info.packageName;
        startApplicationDetailsActivity();
    }
    
    // Finish the activity if the user presses the back button to cancel the activity
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    public View createTabContent(String tag) {
        return mRootView;
    }

    static final int VIEW_NOTHING = 0;
    static final int VIEW_LIST = 1;
    static final int VIEW_RUNNING = 2;

    void updateStorageUsage() {
        if (mCurView == VIEW_RUNNING) {
            return;
        }

        long freeStorage = 0;
        long appStorage = 0;
        long totalStorage = 0;
        CharSequence newLabel = null;

        if (mFilterApps == FILTER_APPS_SDCARD) {
            if (mLastShowedInternalStorage) {
                mLastShowedInternalStorage = false;
            }
            newLabel = this.getText(R.string.sd_card_storage);
            mSDCardFileStats.restat(Environment.getExternalStorageDirectory().toString());
            try {
                totalStorage = (long)mSDCardFileStats.getBlockCount() *
                        mSDCardFileStats.getBlockSize();
                freeStorage = (long) mSDCardFileStats.getAvailableBlocks() *
                mSDCardFileStats.getBlockSize();
            } catch (IllegalArgumentException e) {
                // use the old value of mFreeMem
            }
        } else {
            if (!mLastShowedInternalStorage) {
                mLastShowedInternalStorage = true;
            }
            newLabel = this.getText(R.string.internal_storage);
            mDataFileStats.restat("/data");
            try {
                totalStorage = (long)mDataFileStats.getBlockCount() *
                        mDataFileStats.getBlockSize();
                freeStorage = (long) mDataFileStats.getAvailableBlocks() *
                    mDataFileStats.getBlockSize();
            } catch (IllegalArgumentException e) {
            }
            final int N = mApplicationsAdapter.getCount();
            for (int i=0; i<N; i++) {
                ApplicationsState.AppEntry ae = mApplicationsAdapter.getAppEntry(i);
                appStorage += ae.codeSize + ae.dataSize;
            }
            freeStorage += mApplicationsState.sumCacheSizes();
        }
        if (newLabel != null) {
            mStorageChartLabel.setText(newLabel);
        }
        if (totalStorage > 0) {
            mColorBar.setRatios((totalStorage-freeStorage-appStorage)/(float)totalStorage,
                    appStorage/(float)totalStorage, freeStorage/(float)totalStorage);
            long usedStorage = totalStorage - freeStorage;
            if (mLastUsedStorage != usedStorage) {
                mLastUsedStorage = usedStorage;
                String sizeStr = Formatter.formatShortFileSize(this, usedStorage);
                mUsedStorageText.setText(getResources().getString(
                        R.string.service_foreground_processes, sizeStr));
            }
            if (mLastFreeStorage != freeStorage) {
                mLastFreeStorage = freeStorage;
                String sizeStr = Formatter.formatShortFileSize(this, freeStorage);
                mFreeStorageText.setText(getResources().getString(
                        R.string.service_background_processes, sizeStr));
            }
        } else {
            mColorBar.setRatios(0, 0, 0);
            if (mLastUsedStorage != -1) {
                mLastUsedStorage = -1;
                mUsedStorageText.setText("");
            }
            if (mLastFreeStorage != -1) {
                mLastFreeStorage = -1;
                mFreeStorageText.setText("");
            }
        }
    }

    private void selectView(int which) {
        if (which == VIEW_LIST) {
            if (mResumedRunning) {
                mRunningProcessesView.doPause();
                mResumedRunning = false;
            }
            if (mCurView != which) {
                mRunningProcessesView.setVisibility(View.GONE);
                mListContainer.setVisibility(View.VISIBLE);
                mLoadingContainer.setVisibility(View.GONE);
            }
            if (mActivityResumed) {
                mApplicationsAdapter.resume(mFilterApps, mSortOrder);
            }
        } else if (which == VIEW_RUNNING) {
            if (!mCreatedRunning) {
                mRunningProcessesView.doCreate(null, mNonConfigInstance);
                mCreatedRunning = true;
            }
            boolean haveData = true;
            if (mActivityResumed && !mResumedRunning) {
                haveData = mRunningProcessesView.doResume(mRunningProcessesAvail);
                mResumedRunning = true;
            }
            mApplicationsAdapter.pause();
            if (mCurView != which) {
                if (haveData) {
                    mRunningProcessesView.setVisibility(View.VISIBLE);
                } else {
                    mLoadingContainer.setVisibility(View.VISIBLE);
                }
                mListContainer.setVisibility(View.GONE);
            }
        }
        mCurView = which;
    }

    void handleRunningProcessesAvail() {
        if (mCurView == VIEW_RUNNING) {
            mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                    this, android.R.anim.fade_out));
            mRunningProcessesView.startAnimation(AnimationUtils.loadAnimation(
                    this, android.R.anim.fade_in));
            mRunningProcessesView.setVisibility(View.VISIBLE);
            mLoadingContainer.setVisibility(View.GONE);
        }
    }

    public void showCurrentTab() {
        String tabId = getTabHost().getCurrentTabTag();
        int newOption;
        if (TAB_DOWNLOADED.equalsIgnoreCase(tabId)) {
            newOption = FILTER_APPS_THIRD_PARTY;
        } else if (TAB_ALL.equalsIgnoreCase(tabId)) {
            newOption = FILTER_APPS_ALL;
        } else if (TAB_SDCARD.equalsIgnoreCase(tabId)) {
            newOption = FILTER_APPS_SDCARD;
        } else if (TAB_RUNNING.equalsIgnoreCase(tabId)) {
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
            selectView(VIEW_RUNNING);
            return;
        } else {
            // Invalid option. Do nothing
            return;
        }
        
        mFilterApps = newOption;
        selectView(VIEW_LIST);
        updateStorageUsage();
    }

    public void onTabChanged(String tabId) {
        showCurrentTab();
    }
}

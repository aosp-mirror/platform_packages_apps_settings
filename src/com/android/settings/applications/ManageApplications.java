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

import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.INotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.NetworkPolicyManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFrameLayout;
import android.provider.Settings;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.text.format.Formatter;
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
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.app.IMediaContainerService;
import com.android.internal.content.PackageHelper;
import com.android.settings.R;
import com.android.settings.Settings.RunningServicesActivity;
import com.android.settings.Settings.StorageUseActivity;
import com.android.settings.applications.ApplicationsState.AppEntry;
import com.android.settings.deviceinfo.StorageMeasurement;
import com.android.settings.Utils;

import java.util.ArrayList;
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

interface AppClickListener {
    void onItemClick(ManageApplications.TabInfo tab, AdapterView<?> parent,
            View view, int position, long id);
}

/**
 * Activity to pick an application that will be used to display installation information and
 * options to uninstall/delete user data for system applications. This activity
 * can be launched through Settings or via the ACTION_MANAGE_PACKAGE_STORAGE
 * intent.
 */
public class ManageApplications extends Fragment implements
        AppClickListener, DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener {

    static final String TAG = "ManageApplications";
    static final boolean DEBUG = false;

    private static final String EXTRA_SORT_ORDER = "sortOrder";
    private static final String EXTRA_SHOW_BACKGROUND = "showBackground";
    private static final String EXTRA_DEFAULT_LIST_TYPE = "defaultListType";
    private static final String EXTRA_RESET_DIALOG = "resetDialog";

    // attributes used as keys when passing values to InstalledAppDetails activity
    public static final String APP_CHG = "chg";

    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;

    public static final int SIZE_TOTAL = 0;
    public static final int SIZE_INTERNAL = 1;
    public static final int SIZE_EXTERNAL = 2;

    // sort order that can be changed through the menu can be sorted alphabetically
    // or size(descending)
    private static final int MENU_OPTIONS_BASE = 0;
    // Filter options used for displayed list of applications
    public static final int FILTER_APPS_ALL = MENU_OPTIONS_BASE + 0;
    public static final int FILTER_APPS_THIRD_PARTY = MENU_OPTIONS_BASE + 1;
    public static final int FILTER_APPS_SDCARD = MENU_OPTIONS_BASE + 2;
    public static final int FILTER_APPS_DISABLED = MENU_OPTIONS_BASE + 3;

    public static final int SORT_ORDER_ALPHA = MENU_OPTIONS_BASE + 4;
    public static final int SORT_ORDER_SIZE = MENU_OPTIONS_BASE + 5;
    public static final int SHOW_RUNNING_SERVICES = MENU_OPTIONS_BASE + 6;
    public static final int SHOW_BACKGROUND_PROCESSES = MENU_OPTIONS_BASE + 7;
    public static final int RESET_APP_PREFERENCES = MENU_OPTIONS_BASE + 8;
    // sort order
    private int mSortOrder = SORT_ORDER_ALPHA;
    
    private ApplicationsState mApplicationsState;

    public static class TabInfo implements OnItemClickListener {
        public final ManageApplications mOwner;
        public final ApplicationsState mApplicationsState;
        public final CharSequence mLabel;
        public final int mListType;
        public final int mFilter;
        public final AppClickListener mClickListener;
        public final CharSequence mInvalidSizeStr;
        public final CharSequence mComputingSizeStr;
        private final Bundle mSavedInstanceState;

        public ApplicationsAdapter mApplications;
        public LayoutInflater mInflater;
        public View mRootView;

        private IMediaContainerService mContainerService;

        private View mLoadingContainer;

        private View mListContainer;

        // ListView used to display list
        private ListView mListView;
        // Custom view used to display running processes
        private RunningProcessesView mRunningProcessesView;
        
        private LinearColorBar mColorBar;
        private TextView mStorageChartLabel;
        private TextView mUsedStorageText;
        private TextView mFreeStorageText;
        private long mFreeStorage = 0, mAppStorage = 0, mTotalStorage = 0;
        private long mLastUsedStorage, mLastAppStorage, mLastFreeStorage;

        final Runnable mRunningProcessesAvail = new Runnable() {
            public void run() {
                handleRunningProcessesAvail();
            }
        };

        public TabInfo(ManageApplications owner, ApplicationsState apps,
                CharSequence label, int listType, AppClickListener clickListener,
                Bundle savedInstanceState) {
            mOwner = owner;
            mApplicationsState = apps;
            mLabel = label;
            mListType = listType;
            switch (listType) {
                case LIST_TYPE_DOWNLOADED: mFilter = FILTER_APPS_THIRD_PARTY; break;
                case LIST_TYPE_SDCARD: mFilter = FILTER_APPS_SDCARD; break;
                case LIST_TYPE_DISABLED: mFilter = FILTER_APPS_DISABLED; break;
                default: mFilter = FILTER_APPS_ALL; break;
            }
            mClickListener = clickListener;
            mInvalidSizeStr = owner.getActivity().getText(R.string.invalid_size_value);
            mComputingSizeStr = owner.getActivity().getText(R.string.computing_size);
            mSavedInstanceState = savedInstanceState;
        }

        public void setContainerService(IMediaContainerService containerService) {
            mContainerService = containerService;
            updateStorageUsage();
        }

        public View build(LayoutInflater inflater, ViewGroup contentParent, View contentChild) {
            if (mRootView != null) {
                return mRootView;
            }

            mInflater = inflater;
            mRootView = inflater.inflate(mListType == LIST_TYPE_RUNNING
                    ? R.layout.manage_applications_running
                    : R.layout.manage_applications_apps, null);
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
                mColorBar = (LinearColorBar)mListContainer.findViewById(R.id.storage_color_bar);
                mStorageChartLabel = (TextView)mListContainer.findViewById(R.id.storageChartLabel);
                mUsedStorageText = (TextView)mListContainer.findViewById(R.id.usedStorageText);
                mFreeStorageText = (TextView)mListContainer.findViewById(R.id.freeStorageText);
                Utils.prepareCustomPreferencesList(contentParent, contentChild, mListView, false);
                if (mFilter == FILTER_APPS_SDCARD) {
                    mStorageChartLabel.setText(mOwner.getActivity().getText(
                            R.string.sd_card_storage));
                } else {
                    mStorageChartLabel.setText(mOwner.getActivity().getText(
                            R.string.internal_storage));
                }
                applyCurrentStorage();
            }
            mRunningProcessesView = (RunningProcessesView)mRootView.findViewById(
                    R.id.running_processes);
            if (mRunningProcessesView != null) {
                mRunningProcessesView.doCreate(mSavedInstanceState);
            }

            return mRootView;
        }

        public void detachView() {
            if (mRootView != null) {
                ViewGroup group = (ViewGroup)mRootView.getParent();
                if (group != null) {
                    group.removeView(mRootView);
                }
            }
        }

        public void resume(int sortOrder) {
            if (mApplications != null) {
                mApplications.resume(sortOrder);
            }
            if (mRunningProcessesView != null) {
                boolean haveData = mRunningProcessesView.doResume(mOwner, mRunningProcessesAvail);
                if (haveData) {
                    mRunningProcessesView.setVisibility(View.VISIBLE);
                    mLoadingContainer.setVisibility(View.INVISIBLE);
                } else {
                    mLoadingContainer.setVisibility(View.VISIBLE);
                }
            }
        }

        public void pause() {
            if (mApplications != null) {
                mApplications.pause();
            }
            if (mRunningProcessesView != null) {
                mRunningProcessesView.doPause();
            }
        }

        void updateStorageUsage() {
            // Make sure a callback didn't come at an inopportune time.
            if (mOwner.getActivity() == null) return;
            // Doesn't make sense for stuff that is not an app list.
            if (mApplications == null) return;

            mFreeStorage = 0;
            mAppStorage = 0;
            mTotalStorage = 0;

            if (mFilter == FILTER_APPS_SDCARD) {
                if (mContainerService != null) {
                    try {
                        final long[] stats = mContainerService.getFileSystemStats(
                                Environment.getExternalStorageDirectory().getPath());
                        mTotalStorage = stats[0];
                        mFreeStorage = stats[1];
                    } catch (RemoteException e) {
                        Log.w(TAG, "Problem in container service", e);
                    }
                }

                if (mApplications != null) {
                    final int N = mApplications.getCount();
                    for (int i=0; i<N; i++) {
                        ApplicationsState.AppEntry ae = mApplications.getAppEntry(i);
                        mAppStorage += ae.externalCodeSize + ae.externalDataSize
                                + ae.externalCacheSize;
                    }
                }
            } else {
                if (mContainerService != null) {
                    try {
                        final long[] stats = mContainerService.getFileSystemStats(
                                Environment.getDataDirectory().getPath());
                        mTotalStorage = stats[0];
                        mFreeStorage = stats[1];
                    } catch (RemoteException e) {
                        Log.w(TAG, "Problem in container service", e);
                    }
                }

                final boolean emulatedStorage = Environment.isExternalStorageEmulated();
                if (mApplications != null) {
                    final int N = mApplications.getCount();
                    for (int i=0; i<N; i++) {
                        ApplicationsState.AppEntry ae = mApplications.getAppEntry(i);
                        mAppStorage += ae.codeSize + ae.dataSize;
                        if (emulatedStorage) {
                            mAppStorage += ae.externalCodeSize + ae.externalDataSize;
                        }
                    }
                }
                mFreeStorage += mApplicationsState.sumCacheSizes();
            }

            applyCurrentStorage();
        }

        void applyCurrentStorage() {
            // If view hierarchy is not yet created, no views to update.
            if (mRootView == null) {
                return;
            }
            if (mTotalStorage > 0) {
                mColorBar.setRatios((mTotalStorage-mFreeStorage-mAppStorage)/(float)mTotalStorage,
                        mAppStorage/(float)mTotalStorage, mFreeStorage/(float)mTotalStorage);
                long usedStorage = mTotalStorage - mFreeStorage;
                if (mLastUsedStorage != usedStorage) {
                    mLastUsedStorage = usedStorage;
                    String sizeStr = Formatter.formatShortFileSize(
                            mOwner.getActivity(), usedStorage);
                    mUsedStorageText.setText(mOwner.getActivity().getResources().getString(
                            R.string.service_foreground_processes, sizeStr));
                }
                if (mLastFreeStorage != mFreeStorage) {
                    mLastFreeStorage = mFreeStorage;
                    String sizeStr = Formatter.formatShortFileSize(
                            mOwner.getActivity(), mFreeStorage);
                    mFreeStorageText.setText(mOwner.getActivity().getResources().getString(
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

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mClickListener.onItemClick(this, parent, view, position, id);
        }

        void handleRunningProcessesAvail() {
            mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                    mOwner.getActivity(), android.R.anim.fade_out));
            mRunningProcessesView.startAnimation(AnimationUtils.loadAnimation(
                    mOwner.getActivity(), android.R.anim.fade_in));
            mRunningProcessesView.setVisibility(View.VISIBLE);
            mLoadingContainer.setVisibility(View.GONE);
        }
    }
    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
    private int mNumTabs;
    TabInfo mCurTab = null;

    // Size resource used for packages whose size computation failed for some reason
    CharSequence mInvalidSizeStr;
    private CharSequence mComputingSizeStr;
    
    // layout inflater object used to inflate views
    private LayoutInflater mInflater;
    
    private String mCurrentPkgName;
    
    private Menu mOptionsMenu;

    // These are for keeping track of activity and spinner switch state.
    private boolean mActivityResumed;
    
    static final int LIST_TYPE_DOWNLOADED = 0;
    static final int LIST_TYPE_RUNNING = 1;
    static final int LIST_TYPE_SDCARD = 2;
    static final int LIST_TYPE_ALL = 3;
    static final int LIST_TYPE_DISABLED = 4;

    private boolean mShowBackground = false;
    
    private int mDefaultListType = -1;

    private ViewGroup mContentContainer;
    private View mRootView;
    private ViewPager mViewPager;

    AlertDialog mResetDialog;

    class MyPagerAdapter extends PagerAdapter
            implements ViewPager.OnPageChangeListener {
        int mCurPos = 0;

        @Override
        public int getCount() {
            return mNumTabs;
        }
        
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            TabInfo tab = mTabs.get(position);
            View root = tab.build(mInflater, mContentContainer, mRootView);
            container.addView(root);
            root.setTag(R.id.name, tab);
            return root;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
            //return ((TabInfo)((View)object).getTag(R.id.name)).mListType;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabs.get(position).mLabel;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mCurPos = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                updateCurrentTab(mCurPos);
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
    static class ApplicationsAdapter extends BaseAdapter implements Filterable,
            ApplicationsState.Callbacks, AbsListView.RecyclerListener {
        private final ApplicationsState mState;
        private final ApplicationsState.Session mSession;
        private final TabInfo mTab;
        private final Context mContext;
        private final ArrayList<View> mActive = new ArrayList<View>();
        private final int mFilterMode;
        private ArrayList<ApplicationsState.AppEntry> mBaseEntries;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private boolean mResumed;
        private int mLastSortMode=-1;
        private boolean mWaitingForData;
        private int mWhichSize = SIZE_TOTAL;
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
                mTab.updateStorageUsage();
            }
        };

        public ApplicationsAdapter(ApplicationsState state, TabInfo tab, int filterMode) {
            mState = state;
            mSession = state.newSession(this);
            mTab = tab;
            mContext = tab.mOwner.getActivity();
            mFilterMode = filterMode;
        }

        public void resume(int sort) {
            if (DEBUG) Log.i(TAG, "Resume!  mResumed=" + mResumed);
            if (!mResumed) {
                mResumed = true;
                mSession.resume();
                mLastSortMode = sort;
                rebuild(true);
            } else {
                rebuild(sort);
            }
        }

        public void pause() {
            if (mResumed) {
                mResumed = false;
                mSession.pause();
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
            switch (mFilterMode) {
                case FILTER_APPS_THIRD_PARTY:
                    filterObj = ApplicationsState.THIRD_PARTY_FILTER;
                    break;
                case FILTER_APPS_SDCARD:
                    filterObj = ApplicationsState.ON_SD_CARD_FILTER;
                    if (!emulated) {
                        mWhichSize = SIZE_EXTERNAL;
                    }
                    break;
                case FILTER_APPS_DISABLED:
                    filterObj = ApplicationsState.DISABLED_FILTER;
                    break;
                default:
                    filterObj = ApplicationsState.ALL_ENABLED_FILTER;
                    break;
            }
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
            mTab.updateStorageUsage();

            if (entries == null) {
                mWaitingForData = true;
                mTab.mListContainer.setVisibility(View.INVISIBLE);
                mTab.mLoadingContainer.setVisibility(View.VISIBLE);
            } else {
                mTab.mListContainer.setVisibility(View.VISIBLE);
                mTab.mLoadingContainer.setVisibility(View.GONE);
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
            mTab.mOwner.getActivity().setProgressBarIndeterminateVisibility(running);
        }

        @Override
        public void onRebuildComplete(ArrayList<AppEntry> apps) {
            if (mTab.mLoadingContainer.getVisibility() == View.VISIBLE) {
                mTab.mLoadingContainer.startAnimation(AnimationUtils.loadAnimation(
                        mContext, android.R.anim.fade_out));
                mTab.mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        mContext, android.R.anim.fade_in));
            }
            mTab.mListContainer.setVisibility(View.VISIBLE);
            mTab.mLoadingContainer.setVisibility(View.GONE);
            mWaitingForData = false;
            mBaseEntries = apps;
            mEntries = applyPrefixFilter(mCurFilterPrefix, mBaseEntries);
            notifyDataSetChanged();
            mTab.updateStorageUsage();
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
                        holder.updateSizeText(mTab.mInvalidSizeStr, mWhichSize);
                    }
                    if (holder.entry.info.packageName.equals(mTab.mOwner.mCurrentPkgName)
                            && mLastSortMode == SORT_ORDER_SIZE) {
                        // We got the size information for the last app the
                        // user viewed, and are sorting by size...  they may
                        // have cleared data, so we immediately want to resort
                        // the list with the new size to reflect it to the user.
                        rebuild(false);
                    }
                    mTab.updateStorageUsage();
                    return;
                }
            }
        }

        @Override
        public void onAllSizesComputed() {
            if (mLastSortMode == SORT_ORDER_SIZE) {
                rebuild(false);
            }
            mTab.updateStorageUsage();
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
            AppViewHolder holder = AppViewHolder.createOrRecycle(mTab.mInflater, convertView);
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
                holder.updateSizeText(mTab.mInvalidSizeStr, mWhichSize);
                if ((entry.info.flags&ApplicationInfo.FLAG_INSTALLED) == 0) {
                    holder.disabled.setVisibility(View.VISIBLE);
                    holder.disabled.setText(R.string.not_installed);
                } else if (!entry.info.enabled) {
                    holder.disabled.setVisibility(View.VISIBLE);
                    holder.disabled.setText(R.string.disabled);
                } else {
                    holder.disabled.setVisibility(View.GONE);
                }
                if (mFilterMode == FILTER_APPS_SDCARD) {
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
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        Intent intent = getActivity().getIntent();
        String action = intent.getAction();
        int defaultListType = LIST_TYPE_DOWNLOADED;
        String className = getArguments() != null
                ? getArguments().getString("classname") : null;
        if (className == null) {
            className = intent.getComponent().getClassName();
        }
        if (className.equals(RunningServicesActivity.class.getName())
                || className.endsWith(".RunningServices")) {
            defaultListType = LIST_TYPE_RUNNING;
        } else if (className.equals(StorageUseActivity.class.getName())
                || Intent.ACTION_MANAGE_PACKAGE_STORAGE.equals(action)
                || className.endsWith(".StorageUse")) {
            mSortOrder = SORT_ORDER_SIZE;
            defaultListType = LIST_TYPE_ALL;
        } else if (Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS.equals(action)) {
            // Select the all-apps list, with the default sorting
            defaultListType = LIST_TYPE_ALL;
        }

        if (savedInstanceState != null) {
            mSortOrder = savedInstanceState.getInt(EXTRA_SORT_ORDER, mSortOrder);
            int tmp = savedInstanceState.getInt(EXTRA_DEFAULT_LIST_TYPE, -1);
            if (tmp != -1) defaultListType = tmp;
            mShowBackground = savedInstanceState.getBoolean(EXTRA_SHOW_BACKGROUND, false);
        }

        mDefaultListType = defaultListType;

        final Intent containerIntent = new Intent().setComponent(
                StorageMeasurement.DEFAULT_CONTAINER_COMPONENT);
        getActivity().bindService(containerIntent, mContainerConnection, Context.BIND_AUTO_CREATE);

        mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);
        mComputingSizeStr = getActivity().getText(R.string.computing_size);

        TabInfo tab = new TabInfo(this, mApplicationsState,
                getActivity().getString(R.string.filter_apps_third_party),
                LIST_TYPE_DOWNLOADED, this, savedInstanceState);
        mTabs.add(tab);

        if (!Environment.isExternalStorageEmulated()) {
            tab = new TabInfo(this, mApplicationsState,
                    getActivity().getString(R.string.filter_apps_onsdcard),
                    LIST_TYPE_SDCARD, this, savedInstanceState);
            mTabs.add(tab);
        }

        tab = new TabInfo(this, mApplicationsState,
                getActivity().getString(R.string.filter_apps_running),
                LIST_TYPE_RUNNING, this, savedInstanceState);
        mTabs.add(tab);

        tab = new TabInfo(this, mApplicationsState,
                getActivity().getString(R.string.filter_apps_all),
                LIST_TYPE_ALL, this, savedInstanceState);
        mTabs.add(tab);

        tab = new TabInfo(this, mApplicationsState,
                getActivity().getString(R.string.filter_apps_disabled),
                LIST_TYPE_DISABLED, this, savedInstanceState);
        mTabs.add(tab);

        mNumTabs = mTabs.size();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // initialize the inflater
        mInflater = inflater;

        View rootView = mInflater.inflate(R.layout.manage_applications_content,
                container, false);
        mContentContainer = container;
        mRootView = rootView;

        mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        MyPagerAdapter adapter = new MyPagerAdapter();
        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(adapter);
        PagerTabStrip tabs = (PagerTabStrip) rootView.findViewById(R.id.tabs);
        tabs.setTabIndicatorColorResource(android.R.color.holo_blue_light);

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) rootView.getLayoutParams()).removeBorders = true;
        }

        if (savedInstanceState != null && savedInstanceState.getBoolean(EXTRA_RESET_DIALOG)) {
            buildResetDialog();
        }

        if (savedInstanceState == null) {
            // First time init: make sure view pager is showing the correct tab.
            for (int i = 0; i < mTabs.size(); i++) {
                TabInfo tab = mTabs.get(i);
                if (tab.mListType == mDefaultListType) {
                    mViewPager.setCurrentItem(i);
                    break;
                }
            }
        }

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivityResumed = true;
        updateCurrentTab(mViewPager.getCurrentItem());
        updateNumTabs();
        updateOptionsMenu();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SORT_ORDER, mSortOrder);
        if (mDefaultListType != -1) {
            outState.putInt(EXTRA_DEFAULT_LIST_TYPE, mDefaultListType);
        }
        outState.putBoolean(EXTRA_SHOW_BACKGROUND, mShowBackground);
        if (mResetDialog != null) {
            outState.putBoolean(EXTRA_RESET_DIALOG, true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mActivityResumed = false;
        for (int i=0; i<mTabs.size(); i++) {
            mTabs.get(i).pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mResetDialog != null) {
            mResetDialog.dismiss();
            mResetDialog = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // We are going to keep the tab data structures around, but they
        // are no longer attached to their view hierarchy.
        for (int i=0; i<mTabs.size(); i++) {
            mTabs.get(i).detachView();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INSTALLED_APP_DETAILS && mCurrentPkgName != null) {
            mApplicationsState.requestSize(mCurrentPkgName);
        }
    }

    private void updateNumTabs() {
        int newNum = mApplicationsState.haveDisabledApps() ? mTabs.size() : (mTabs.size()-1);
        if (newNum != mNumTabs) {
            mNumTabs = newNum;
            if (mViewPager != null) {
                mViewPager.getAdapter().notifyDataSetChanged();
            }
        }
    }

    TabInfo tabForType(int type) {
        for (int i = 0; i < mTabs.size(); i++) {
            TabInfo tab = mTabs.get(i);
            if (tab.mListType == type) {
                return tab;
            }
        }
        return null;
    }

    // utility method used to start sub activity
    private void startApplicationDetailsActivity() {
        // start new fragment to display extended information
        Bundle args = new Bundle();
        args.putString(InstalledAppDetails.ARG_PACKAGE_NAME, mCurrentPkgName);

        PreferenceActivity pa = (PreferenceActivity)getActivity();
        pa.startPreferencePanel(InstalledAppDetails.class.getName(), args,
                R.string.application_info_label, null, this, INSTALLED_APP_DETAILS);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mOptionsMenu = menu;
        // note: icons removed for now because the cause the new action
        // bar UI to be very confusing.
        menu.add(0, SORT_ORDER_ALPHA, 1, R.string.sort_order_alpha)
                //.setIcon(android.R.drawable.ic_menu_sort_alphabetically)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, SORT_ORDER_SIZE, 2, R.string.sort_order_size)
                //.setIcon(android.R.drawable.ic_menu_sort_by_size)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, SHOW_RUNNING_SERVICES, 3, R.string.show_running_services)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, SHOW_BACKGROUND_PROCESSES, 3, R.string.show_background_processes)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, RESET_APP_PREFERENCES, 4, R.string.reset_app_preferences)
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

    @Override
    public void onDestroy() {
        getActivity().unbindService(mContainerConnection);
        super.onDestroy();
    }

    void updateOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }
        
        /*
         * The running processes screen doesn't use the mApplicationsAdapter
         * so bringing up this menu in that case doesn't make any sense.
         */
        if (mCurTab != null && mCurTab.mListType == LIST_TYPE_RUNNING) {
            TabInfo tab = tabForType(LIST_TYPE_RUNNING);
            boolean showingBackground = tab != null && tab.mRunningProcessesView != null
                    ? tab.mRunningProcessesView.mAdapter.getShowBackground() : false;
            mOptionsMenu.findItem(SORT_ORDER_ALPHA).setVisible(false);
            mOptionsMenu.findItem(SORT_ORDER_SIZE).setVisible(false);
            mOptionsMenu.findItem(SHOW_RUNNING_SERVICES).setVisible(showingBackground);
            mOptionsMenu.findItem(SHOW_BACKGROUND_PROCESSES).setVisible(!showingBackground);
            mOptionsMenu.findItem(RESET_APP_PREFERENCES).setVisible(false);
        } else {
            mOptionsMenu.findItem(SORT_ORDER_ALPHA).setVisible(mSortOrder != SORT_ORDER_ALPHA);
            mOptionsMenu.findItem(SORT_ORDER_SIZE).setVisible(mSortOrder != SORT_ORDER_SIZE);
            mOptionsMenu.findItem(SHOW_RUNNING_SERVICES).setVisible(false);
            mOptionsMenu.findItem(SHOW_BACKGROUND_PROCESSES).setVisible(false);
            mOptionsMenu.findItem(RESET_APP_PREFERENCES).setVisible(true);
        }
    }

    void buildResetDialog() {
        if (mResetDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.reset_app_preferences_title);
            builder.setMessage(R.string.reset_app_preferences_desc);
            builder.setPositiveButton(R.string.reset_app_preferences_button, this);
            builder.setNegativeButton(R.string.cancel, null);
            mResetDialog = builder.show();
            mResetDialog.setOnDismissListener(this);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mResetDialog == dialog) {
            mResetDialog = null;
        }
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mResetDialog == dialog) {
            final PackageManager pm = getActivity().getPackageManager();
            final IPackageManager mIPm = IPackageManager.Stub.asInterface(
                            ServiceManager.getService("package"));
            final INotificationManager nm = INotificationManager.Stub.asInterface(
                    ServiceManager.getService(Context.NOTIFICATION_SERVICE));
            final NetworkPolicyManager npm = NetworkPolicyManager.from(getActivity());
            final Handler handler = new Handler(getActivity().getMainLooper());
            (new AsyncTask<Void, Void, Void>() {
                @Override protected Void doInBackground(Void... params) {
                    List<ApplicationInfo> apps = pm.getInstalledApplications(
                            PackageManager.GET_DISABLED_COMPONENTS);
                    for (int i=0; i<apps.size(); i++) {
                        ApplicationInfo app = apps.get(i);
                        try {
                            if (DEBUG) Log.v(TAG, "Enabling notifications: " + app.packageName);
                            nm.setNotificationsEnabledForPackage(app.packageName, app.uid, true);
                        } catch (android.os.RemoteException ex) {
                        }
                        if (!app.enabled) {
                            if (DEBUG) Log.v(TAG, "Enabling app: " + app.packageName);
                            if (pm.getApplicationEnabledSetting(app.packageName)
                                    == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                                pm.setApplicationEnabledSetting(app.packageName,
                                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                                        PackageManager.DONT_KILL_APP);
                            }
                        }
                    }
                    try {
                        mIPm.resetPreferredActivities(UserHandle.myUserId());
                    } catch (RemoteException e) {
                    }
                    final int[] restrictedUids = npm.getUidsWithPolicy(
                            POLICY_REJECT_METERED_BACKGROUND);
                    final int currentUserId = ActivityManager.getCurrentUser();
                    for (int uid : restrictedUids) {
                        // Only reset for current user
                        if (UserHandle.getUserId(uid) == currentUserId) {
                            if (DEBUG) Log.v(TAG, "Clearing data policy: " + uid);
                            npm.setUidPolicy(uid, POLICY_NONE);
                        }
                    }
                    handler.post(new Runnable() {
                        @Override public void run() {
                            if (DEBUG) Log.v(TAG, "Done clearing");
                            if (getActivity() != null && mActivityResumed) {
                                if (DEBUG) Log.v(TAG, "Updating UI!");
                                for (int i=0; i<mTabs.size(); i++) {
                                    TabInfo tab = mTabs.get(i);
                                    if (tab.mApplications != null) {
                                        tab.mApplications.pause();
                                    }
                                }
                                if (mCurTab != null) {
                                    mCurTab.resume(mSortOrder);
                                }
                            }
                        }
                    });
                    return null;
                }
            }).execute();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        if ((menuId == SORT_ORDER_ALPHA) || (menuId == SORT_ORDER_SIZE)) {
            mSortOrder = menuId;
            if (mCurTab != null && mCurTab.mApplications != null) {
                mCurTab.mApplications.rebuild(mSortOrder);
            }
        } else if (menuId == SHOW_RUNNING_SERVICES) {
            mShowBackground = false;
            if (mCurTab != null && mCurTab.mRunningProcessesView != null) {
                mCurTab.mRunningProcessesView.mAdapter.setShowBackground(false);
            }
        } else if (menuId == SHOW_BACKGROUND_PROCESSES) {
            mShowBackground = true;
            if (mCurTab != null && mCurTab.mRunningProcessesView != null) {
                mCurTab.mRunningProcessesView.mAdapter.setShowBackground(true);
            }
        } else if (menuId == RESET_APP_PREFERENCES) {
            buildResetDialog();
        } else {
            // Handle the home button
            return false;
        }
        updateOptionsMenu();
        return true;
    }
    
    public void onItemClick(TabInfo tab, AdapterView<?> parent, View view, int position,
            long id) {
        if (tab.mApplications != null && tab.mApplications.getCount() > position) {
            ApplicationsState.AppEntry entry = tab.mApplications.getAppEntry(position);
            mCurrentPkgName = entry.info.packageName;
            startApplicationDetailsActivity();
        }
    }

    public void updateCurrentTab(int position) {
        TabInfo tab = mTabs.get(position);
        mCurTab = tab;

        // Put things in the correct paused/resumed state.
        if (mActivityResumed) {
            mCurTab.build(mInflater, mContentContainer, mRootView);
            mCurTab.resume(mSortOrder);
        } else {
            mCurTab.pause();
        }
        for (int i=0; i<mTabs.size(); i++) {
            TabInfo t = mTabs.get(i);
            if (t != mCurTab) {
                t.pause();
            }
        }

        mCurTab.updateStorageUsage();
        updateOptionsMenu();
        final Activity host = getActivity();
        if (host != null) {
            host.invalidateOptionsMenu();
        }
    }

    private volatile IMediaContainerService mContainerService;

    private final ServiceConnection mContainerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mContainerService = IMediaContainerService.Stub.asInterface(service);
            for (int i=0; i<mTabs.size(); i++) {
                mTabs.get(i).setContainerService(mContainerService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mContainerService = null;
        }
    };
}

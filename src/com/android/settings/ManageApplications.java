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

package com.android.settings;

import com.android.settings.R;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Activity to pick an application that will be used to display installation information and
 * options to uninstall/delete user data for system applications. This activity
 * can be launched through Settings or via the ACTION_MANAGE_PACKAGE_STORAGE
 * intent.
 *  Initially a compute in progress message is displayed while the application retrieves
 *  the list of application information from the PackageManager. The size information
 *  for each package is refreshed to the screen. The resource(app description and
 *  icon) information for each package is not available yet, so some default values for size
 *  icon and descriptions are used initially. Later the resource information for each 
 *  application is retrieved and dynamically updated on the screen.
 *  A Broadcast receiver registers for package additions or deletions when the activity is
 *  in focus. If the user installs or deletes packages when the activity has focus, the receiver
 *  gets notified and proceeds to add/delete these packages from the list on the screen.
 *  This is an unlikely scenario but could happen. The entire list gets created every time
 *  the activity's onStart gets invoked. This is to avoid having the receiver for the entire
 *  life cycle of the application.
 *  The applications can be sorted either alphabetically or 
 *  based on size(descending). If this activity gets launched under low memory
 *  situations(A low memory notification dispatches intent 
 *  ACTION_MANAGE_PACKAGE_STORAGE) the list is sorted per size.
 *  If the user selects an application, extended info(like size, uninstall/clear data options,
 *  permissions info etc.,) is displayed via the InstalledAppDetails activity.
 */
public class ManageApplications extends ListActivity implements
        OnItemClickListener, DialogInterface.OnCancelListener,
        DialogInterface.OnClickListener {
    // TAG for this activity
    private static final String TAG = "ManageApplications";
    
    // Log information boolean
    private boolean localLOGV = Config.LOGV || false;
    
    // attributes used as keys when passing values to InstalledAppDetails activity
    public static final String APP_PKG_PREFIX = "com.android.settings.";
    public static final String APP_PKG_NAME = APP_PKG_PREFIX+"ApplicationPkgName";
    public static final String APP_CHG = APP_PKG_PREFIX+"changed";
    
    // attribute name used in receiver for tagging names of added/deleted packages
    private static final String ATTR_PKG_NAME="PackageName";
    private static final String ATTR_APP_PKG_STATS="ApplicationPackageStats";
    
    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;
    
    // sort order that can be changed through the menu can be sorted alphabetically
    // or size(descending)
    private static final int MENU_OPTIONS_BASE = 0;
    public static final int SORT_ORDER_ALPHA = MENU_OPTIONS_BASE + 0;
    public static final int SORT_ORDER_SIZE = MENU_OPTIONS_BASE + 1;
    // Filter options used for displayed list of applications
    public static final int FILTER_APPS_ALL = MENU_OPTIONS_BASE + 2;
    public static final int FILTER_APPS_THIRD_PARTY = MENU_OPTIONS_BASE + 3;
    public static final int FILTER_APPS_RUNNING = MENU_OPTIONS_BASE + 4;
    public static final int FILTER_OPTIONS = MENU_OPTIONS_BASE + 5;
    // Alert Dialog presented to user to find out the filter option
    AlertDialog mAlertDlg;
    // sort order
    private int mSortOrder = SORT_ORDER_ALPHA;
    // Filter value
    int mFilterApps = FILTER_APPS_ALL;
    
    // Custom Adapter used for managing items in the list
    private AppInfoAdapter mAppInfoAdapter;
    
    // messages posted to the handler
    private static final int HANDLER_MESSAGE_BASE = 0;
    private static final int INIT_PKG_INFO = HANDLER_MESSAGE_BASE+1;
    private static final int COMPUTE_PKG_SIZE_DONE = HANDLER_MESSAGE_BASE+2;
    private static final int REMOVE_PKG = HANDLER_MESSAGE_BASE+3;
    private static final int REORDER_LIST = HANDLER_MESSAGE_BASE+4;
    private static final int ADD_PKG_START = HANDLER_MESSAGE_BASE+5;
    private static final int ADD_PKG_DONE = HANDLER_MESSAGE_BASE+6;
    private static final int REFRESH_ICONS = HANDLER_MESSAGE_BASE+7;
    private static final int NEXT_LOAD_STEP = HANDLER_MESSAGE_BASE+8;
    
    // observer object used for computing pkg sizes
    private PkgSizeObserver mObserver;
    // local handle to PackageManager
    private PackageManager mPm;
    // Broadcast Receiver object that receives notifications for added/deleted
    // packages
    private PackageIntentReceiver mReceiver;
    // atomic variable used to track if computing pkg sizes is in progress. should be volatile?
    
    private boolean mComputeSizes = false;
    // default icon thats used when displaying applications initially before resource info is
    // retrieved
    private Drawable mDefaultAppIcon;
    
    // temporary dialog displayed while the application info loads
    private static final int DLG_BASE = 0;
    private static final int DLG_LOADING = DLG_BASE + 1;
    
    // compute index used to track the application size computations
    private int mComputeIndex;
    
    // Size resource used for packages whose size computation failed for some reason
    private CharSequence mInvalidSizeStr;
    private CharSequence mComputingSizeStr;
    
    // map used to store list of added and removed packages. Immutable Boolean
    // variables indicate if a package has been added or removed. If a package is
    // added or deleted multiple times a single entry with the latest operation will
    // be recorded in the map.
    private Map<String, Boolean> mAddRemoveMap;
    
    // layout inflater object used to inflate views
    private LayoutInflater mInflater;
    
    // invalid size value used initially and also when size retrieval through PackageManager
    // fails for whatever reason
    private static final int SIZE_INVALID = -1;
    
    // debug boolean variable to test delays from PackageManager API's
    private boolean DEBUG_PKG_DELAY = false;
    
    // Thread to load resources
    ResourceLoaderThread mResourceThread;
    
    String mCurrentPkgName;
    
    //TODO implement a cache system
    private Map<String, AppInfo> mAppPropCache;
    
    // empty message displayed when list is empty
    private TextView mEmptyView;
    
    // Boolean variables indicating state
    private boolean mLoadLabels = false;
    private boolean mSizesFirst = false;
    // ListView used to display list
    private ListView mListView;
    // State variables used to figure out menu options and also
    // initiate the first computation and loading of resources
    private boolean mJustCreated = true;
    private boolean mFirst = false;
    
    /*
     * Handler class to handle messages for various operations
     * Most of the operations that effect Application related data
     * are posted as messages to the handler to avoid synchronization
     * when accessing these structures.
     * When the size retrieval gets kicked off for the first time, a COMPUTE_PKG_SIZE_START
     * message is posted to the handler which invokes the getSizeInfo for the pkg at index 0
     * When the PackageManager's asynchronous call back through
     * PkgSizeObserver.onGetStatsCompleted gets invoked, the application resources like
     * label, description, icon etc., is loaded in the same thread and these values are
     * set on the observer. The observer then posts a COMPUTE_PKG_SIZE_DONE message
     * to the handler. This information is updated on the AppInfoAdapter associated with
     * the list view of this activity and size info retrieval is initiated for the next package as 
     * indicated by mComputeIndex
     * When a package gets added while the activity has focus, the PkgSizeObserver posts
     * ADD_PKG_START message to the handler.  If the computation is not in progress, the size
     * is retrieved for the newly added package through the observer object and the newly
     * installed app info is updated on the screen. If the computation is still in progress
     * the package is added to an internal structure and action deferred till the computation
     * is done for all the packages. 
     * When a package gets deleted, REMOVE_PKG is posted to the handler
     *  if computation is not in progress(as indicated by
     * mDoneIniting), the package is deleted from the displayed list of apps. If computation is
     * still in progress the package is added to an internal structure and action deferred till
     * the computation is done for all packages.
     * When the sizes of all packages is computed, the newly
     * added or removed packages are processed in order.
     * If the user changes the order in  which these applications are viewed by hitting the
     * menu key, REORDER_LIST message is posted to the handler. this sorts the list
     * of items based on the sort order.
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            PackageStats ps;
            ApplicationInfo info;
            Bundle data;
            String pkgName = null;
            AppInfo appInfo;
            data = msg.getData();
            if(data != null) {
                pkgName = data.getString(ATTR_PKG_NAME);
            }
            switch (msg.what) {
            case INIT_PKG_INFO:
                if(localLOGV) Log.i(TAG, "Message INIT_PKG_INFO");
                if (!mJustCreated) {
                    // Add or delete newly created packages by comparing lists
                    List<ApplicationInfo> newList = getInstalledApps(FILTER_APPS_ALL);
                    int oldCount = mAppPropCache.size();
                    boolean idxArr[] = new boolean[oldCount];
                    for ( int i = 0; i < oldCount; i++) {
                        idxArr[i] = false;
                    }
                        
                    if (newList != null) {
                        for (ApplicationInfo app : newList) {
                            AppInfo aInfo = mAppPropCache.get(app.packageName);
                            if ( aInfo == null) {
                                // New package. post an ADD_PKG message
                                if(localLOGV) Log.i(TAG, "Adding pkg: "+app.packageName);
                                updatePackageList(Intent.ACTION_PACKAGE_ADDED, app.packageName);
                            } else {
                                idxArr[aInfo.index] = true;
                            }
                        }
                        Set<String> keyList = mAppPropCache.keySet();
                        for (String key : keyList) {
                            AppInfo aInfo = mAppPropCache.get(key);
                            int idx = aInfo.index;
                            if (!idxArr[idx]) {
                                String pkg = aInfo.pkgName; 
                                if(localLOGV) Log.i(TAG, "Deleting pkg: " + pkg);
                                updatePackageList(Intent.ACTION_PACKAGE_REMOVED, pkg);
                            }
                        }
                    }
                }
                // Retrieve the package list and init some structures
                initAppList(mFilterApps);
                mHandler.sendEmptyMessage(NEXT_LOAD_STEP);
                break;
            case COMPUTE_PKG_SIZE_DONE:
                if(localLOGV) Log.i(TAG, "Message COMPUTE_PKG_SIZE_DONE");
                if(pkgName == null) {
                     Log.w(TAG, "Ignoring message");
                     break;
                }
                ps = data.getParcelable(ATTR_APP_PKG_STATS);
                if(ps == null) {
                    Log.i(TAG, "Invalid package stats for package:"+pkgName);
                } else {
                    int pkgId = mAppInfoAdapter.getIndex(pkgName);
                    if(mComputeIndex != pkgId) {
                        //spurious call from stale observer
                        Log.w(TAG, "Stale call back from PkgSizeObserver");
                        break;
                    }
                    mAppInfoAdapter.updateAppSize(pkgName, ps);
                }
                mComputeIndex++;
                if (mComputeIndex < mAppInfoAdapter.getCount()) {
                    // initiate compute package size for next pkg in list
                    mObserver.invokeGetSizeInfo(mAppInfoAdapter.getApplicationInfo(
                            mComputeIndex), 
                            COMPUTE_PKG_SIZE_DONE);
                } else {
                    // check for added/removed packages
                    Set<String> keys =  mAddRemoveMap.keySet();
                    Iterator<String> iter = keys.iterator();
                    List<String> removeList = new ArrayList<String>();
                    boolean added = false;
                    boolean removed = false;
                    while (iter.hasNext()) {
                        String key = iter.next();
                        if (mAddRemoveMap.get(key) == Boolean.TRUE) {
                            // add
                            try {
                                info = mPm.getApplicationInfo(key, 0);
                                mAppInfoAdapter.addApplicationInfo(info);
                                added = true;
                            } catch (NameNotFoundException e) {
                                Log.w(TAG, "Invalid added package:"+key+" Ignoring entry");
                            }   
                        } else {
                            // remove
                            removeList.add(key);
                            removed = true;
                        }
                    }
                    // remove uninstalled packages from list
                    if (removed) {
                        mAppInfoAdapter.removeFromList(removeList);
                    }
                    // handle newly installed packages
                    if (added) {
                        mObserver.invokeGetSizeInfo(mAppInfoAdapter.getApplicationInfo(
                                mComputeIndex), 
                                COMPUTE_PKG_SIZE_DONE);
                    } else {
                        // end computation here
                        mComputeSizes = true;
                        mFirst = true;
                        mAppInfoAdapter.sortList(mSortOrder);
                        mHandler.sendEmptyMessage(NEXT_LOAD_STEP);
                    }
                }
                break;
            case REMOVE_PKG:
                if(localLOGV) Log.i(TAG, "Message REMOVE_PKG");
                if(pkgName == null) {
                    Log.w(TAG, "Ignoring message:REMOVE_PKG for null pkgName");
                    break;
                }
                if (!mComputeSizes) {
                    Boolean currB = mAddRemoveMap.get(pkgName);
                    if (currB == null || (currB.equals(Boolean.TRUE))) {
                        mAddRemoveMap.put(pkgName, Boolean.FALSE);
                    }
                    break;
                }
                List<String> pkgList = new ArrayList<String>();
                pkgList.add(pkgName);
                mAppInfoAdapter.removeFromList(pkgList);
                break;
            case REORDER_LIST:
                if(localLOGV) Log.i(TAG, "Message REORDER_LIST");
                int menuOption = msg.arg1;
                if((menuOption == SORT_ORDER_ALPHA) || 
                        (menuOption == SORT_ORDER_SIZE)) {
                    // Option to sort list
                    if (menuOption != mSortOrder) {
                        mSortOrder = menuOption;
                        if (localLOGV) Log.i(TAG, "Changing sort order to "+mSortOrder);
                        mAppInfoAdapter.sortList(mSortOrder);
                    }
                } else if(menuOption != mFilterApps) {
                    // Option to filter list
                    mFilterApps = menuOption;
                    boolean ret = mAppInfoAdapter.resetAppList(mFilterApps, 
                            getInstalledApps(mFilterApps));
                    if(!ret) {
                        // Reset cache
                        mAppPropCache = null;
                        mFilterApps = FILTER_APPS_ALL;
                        mHandler.sendEmptyMessage(INIT_PKG_INFO);
                        sendMessageToHandler(REORDER_LIST, menuOption);
                    }
                }
                break;
            case ADD_PKG_START:
                if(localLOGV) Log.i(TAG, "Message ADD_PKG_START");
                if(pkgName == null) {
                    Log.w(TAG, "Ignoring message:ADD_PKG_START for null pkgName");
                    break;
                }
                if (!mComputeSizes) {
                    Boolean currB = mAddRemoveMap.get(pkgName);
                    if (currB == null || (currB.equals(Boolean.FALSE))) {
                        mAddRemoveMap.put(pkgName, Boolean.TRUE);
                    }
                    break;
                }
                try {
                        info = mPm.getApplicationInfo(pkgName, 0);
                    } catch (NameNotFoundException e) {
                        Log.w(TAG, "Couldnt find application info for:"+pkgName);
                        break;
                    }
                mObserver.invokeGetSizeInfo(info, ADD_PKG_DONE);
                break;
            case ADD_PKG_DONE:
                if(localLOGV) Log.i(TAG, "Message COMPUTE_PKG_SIZE_DONE");
                if(pkgName == null) {
                    Log.w(TAG, "Ignoring message:ADD_PKG_START for null pkgName");
                    break;
                }
                ps = data.getParcelable(ATTR_APP_PKG_STATS);
                mAppInfoAdapter.addToList(pkgName, ps);
                break;
            case REFRESH_ICONS:
                Map<String, AppInfo> iconMap = (Map<String, AppInfo>) msg.obj;
                if(iconMap == null) {
                    Log.w(TAG, "Error loading icons for applications");
                } else {
                    mAppInfoAdapter.updateAppsResourceInfo(iconMap);   
                }
                mLoadLabels = true;
                mHandler.sendEmptyMessage(NEXT_LOAD_STEP);
                break;
            case NEXT_LOAD_STEP:
                if (mComputeSizes && mLoadLabels) {
                    doneLoadingData();
                } else if (!mComputeSizes && !mLoadLabels) {
                     // Either load the package labels or initiate get size info
                    if (mSizesFirst) {
                        initComputeSizes();
                    } else {
                        initResourceThread();
                    }
                } else {
                    // Create list view from the adapter here. Wait till the sort order
                    // of list is defined. its either by label or by size. so atleast one of the
                    // first steps should be complete before filling the list
                    if (mJustCreated) {
                        // Set the adapter here.
                        mJustCreated = false;
                        mListView.setAdapter(mAppInfoAdapter);
                        dismissLoadingMsg();
                    }
                    if (!mComputeSizes) {
                        initComputeSizes();
                    } else if (!mLoadLabels) {
                        initResourceThread();
                    }
                }
                break;
            default:
                break;
            }
        }
    };
    
    
    
    private void doneLoadingData() {
        setProgressBarIndeterminateVisibility(false);
    }
    
    List<ApplicationInfo> getInstalledApps(int filterOption) {
        List<ApplicationInfo> installedAppList = mPm.getInstalledApplications(
                PackageManager.GET_UNINSTALLED_PACKAGES);
        if (installedAppList == null) {
            return new ArrayList<ApplicationInfo> ();
        }
        if (filterOption == FILTER_APPS_THIRD_PARTY) {
            List<ApplicationInfo> appList =new ArrayList<ApplicationInfo> ();
            for (ApplicationInfo appInfo : installedAppList) {
                boolean flag = false;
                if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    // Updated system app
                    flag = true;
                } else if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    // Non-system app
                    flag = true;
                }
                if (flag) {
                    appList.add(appInfo);
                }
            }
            return appList;
        } else if (filterOption == FILTER_APPS_RUNNING) {
            List<ApplicationInfo> appList =new ArrayList<ApplicationInfo> ();
            List<ActivityManager.RunningAppProcessInfo> procList = getRunningAppProcessesList();
            if ((procList == null) || (procList.size() == 0)) {
                return appList;
            }
            // Retrieve running processes from ActivityManager
            for (ActivityManager.RunningAppProcessInfo appProcInfo : procList) {
                if ((appProcInfo != null)  && (appProcInfo.pkgList != null)){
                    int size = appProcInfo.pkgList.length;
                    for (int i = 0; i < size; i++) {
                        ApplicationInfo appInfo = null;
                        try {
                            appInfo = mPm.getApplicationInfo(appProcInfo.pkgList[i], 
                                    PackageManager.GET_UNINSTALLED_PACKAGES);
                        } catch (NameNotFoundException e) {
                           Log.w(TAG, "Error retrieving ApplicationInfo for pkg:"+appProcInfo.pkgList[i]);
                           continue;
                        }
                        if(appInfo != null) {
                            appList.add(appInfo);
                        }
                    }
                }
            }
            return appList;
        } else {
            return installedAppList;
        }
    }
    
    private List<ActivityManager.RunningAppProcessInfo> getRunningAppProcessesList() {
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        return am.getRunningAppProcesses();
    }
    
    // some initialization code used when kicking off the size computation
    private void initAppList(int filterOption) {
        // Initialize lists
        List<ApplicationInfo> appList = getInstalledApps(filterOption);
        initAppList(appList, filterOption);
    }
    
     // some initialization code used when kicking off the size computation
    private void initAppList(List<ApplicationInfo> appList, int filterOption) {
        setProgressBarIndeterminateVisibility(true);
        mComputeIndex = 0;
        mComputeSizes = false;
        mLoadLabels = false;
        // Initialize lists
        mAddRemoveMap = new TreeMap<String, Boolean>();
        mAppInfoAdapter.resetAppList(filterOption, appList);
    }
    
    // Utility method to start a thread to read application labels and icons
    private void initResourceThread() {
        //load resources now
        if(mResourceThread.isAlive()) {
            mResourceThread.interrupt();
        }
        mResourceThread.loadAllResources(mAppInfoAdapter.getAppList());
    }
    
    private void initComputeSizes() {
         // initiate compute pkg sizes
        if (localLOGV) Log.i(TAG, "Initiating compute sizes for first time");
        if (mAppInfoAdapter.getCount() > 0) {
            mObserver.invokeGetSizeInfo(mAppInfoAdapter.getApplicationInfo(0),
                    COMPUTE_PKG_SIZE_DONE);
        } else {
            mComputeSizes = true;
        }
    }
    
    private void showEmptyViewIfListEmpty() {
        if (localLOGV) Log.i(TAG, "Checking for empty view");
        if (mAppInfoAdapter.getCount() > 0) {
            mListView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        } else {
            mListView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }
    
    // internal structure used to track added and deleted packages when
    // the activity has focus
    class AddRemoveInfo {
        String pkgName;
        boolean add;
        public AddRemoveInfo(String pPkgName, boolean pAdd) {
            pkgName = pPkgName;
            add = pAdd;
        }
    }
    
    class ResourceLoaderThread extends Thread {
        List<ApplicationInfo> mAppList;
        
        void loadAllResources(List<ApplicationInfo> appList) {
            mAppList = appList;
            start();
        }

        public void run() {
            Map<String, AppInfo> iconMap = new HashMap<String, AppInfo>();
            if(mAppList == null || mAppList.size() <= 0) {
                Log.w(TAG, "Empty or null application list");
            } else {
                for (ApplicationInfo appInfo : mAppList) {
                    CharSequence appName = appInfo.loadLabel(mPm);
                    Drawable appIcon = appInfo.loadIcon(mPm);
                    iconMap.put(appInfo.packageName, 
                            new AppInfo(appInfo.packageName, appName, appIcon));
                }
            }
            Message msg = mHandler.obtainMessage(REFRESH_ICONS);
            msg.obj = iconMap;
            mHandler.sendMessage(msg);
        }
    }
    
    /* Internal class representing an application or packages displayable attributes
     * 
     */
    class AppInfo {
        public String pkgName;
        int index;
        public  CharSequence appName;
        public  Drawable appIcon;
        public CharSequence appSize;
        public PackageStats appStats;
        
        public void refreshIcon(AppInfo pInfo) {
            appName = pInfo.appName;
            appIcon = pInfo.appIcon;
        }

        public AppInfo(String pName, CharSequence aName, Drawable aIcon) {
            index = -1;
            pkgName = pName;
            appName = aName;
            appIcon = aIcon;
            appStats = null;
            appSize = mComputingSizeStr;
        }
        
        public AppInfo(String pName, int pIndex, CharSequence aName, Drawable aIcon, 
                PackageStats ps) {
            index = pIndex;
            pkgName = pName;
            appName = aName;
            appIcon = aIcon;
            if(ps == null) {
                appSize = mComputingSizeStr;
            } else {
                appStats = ps;
                appSize = getSizeStr();
            }
        }
        public void setSize(PackageStats ps) {
            appStats = ps;
            if (ps != null) {
                appSize = getSizeStr();
            }
        }
        public long getTotalSize() {
            PackageStats ps = appStats;
            if (ps != null) {
                return ps.cacheSize+ps.codeSize+ps.dataSize;
            }
            return SIZE_INVALID;
        }
        
        private String getSizeStr() {
            PackageStats ps = appStats;
            String retStr = "";
            // insert total size information into map to display in view
            // at this point its guaranteed that ps is not null. but checking anyway
            if (ps != null) {
                long size = getTotalSize();
                if (size == SIZE_INVALID) {
                    return mInvalidSizeStr.toString();
                }
                return Formatter.formatFileSize(ManageApplications.this, size);
            }
            return retStr;
        }
    }
    
    // View Holder used when displaying views
    static class AppViewHolder {
        TextView appName;
        ImageView appIcon;
        TextView appSize;
    }
    
    /* Custom adapter implementation for the ListView
     * This adapter maintains a map for each displayed application and its properties
     * An index value on each AppInfo object indicates the correct position or index
     * in the list. If the list gets updated dynamically when the user is viewing the list of
     * applications, we need to return the correct index of position. This is done by mapping
     * the getId methods via the package name into the internal maps and indices.
     * The order of applications in the list is mirrored in mAppLocalList
     */
    class AppInfoAdapter extends BaseAdapter {
        private Map<String, AppInfo> mAppPropMap;
        private List<ApplicationInfo> mAppLocalList;
        ApplicationInfo.DisplayNameComparator mAlphaComparator;
        AppInfoComparator mSizeComparator;
        
        private AppInfo getFromCache(String packageName) {
            if(mAppPropCache == null) {
                return null;
            }
            return mAppPropCache.get(packageName);
        }
        
        public AppInfoAdapter(Context c, List<ApplicationInfo> appList) {
            mAppLocalList = appList;
            boolean useCache = false;
            int sortOrder = SORT_ORDER_ALPHA;
            int imax = mAppLocalList.size();
            if(mAppPropCache != null) {
                useCache = true;
                // Activity has been resumed. can use the cache to populate values initially
                mAppPropMap = mAppPropCache;
                sortOrder = mSortOrder;
            }
            sortAppList(sortOrder);
            // Recreate property map
            mAppPropMap = new TreeMap<String, AppInfo>();
            for (int i = 0; i < imax; i++) {
                ApplicationInfo info = mAppLocalList.get(i);
                AppInfo aInfo = getFromCache(info.packageName);
                if(aInfo == null){
                    aInfo = new AppInfo(info.packageName, i, 
                            info.packageName, mDefaultAppIcon, null);   
                } else {
                    aInfo.index = i;
                }
                mAppPropMap.put(info.packageName, aInfo);
            }
        }
        
        public int getCount() {
            return mAppLocalList.size();
        }
        
        public Object getItem(int position) {
            return mAppLocalList.get(position);
        }
        
        /*
         * This method returns the index of the package position in the application list
         */
        public int getIndex(String pkgName) {
            if(pkgName == null) {
                Log.w(TAG, "Getting index of null package in List Adapter");
            }
            int imax = mAppLocalList.size();
            ApplicationInfo appInfo;
            for(int i = 0; i < imax; i++) {
                appInfo = mAppLocalList.get(i);
                if(appInfo.packageName.equalsIgnoreCase(pkgName)) {
                    return i;
                }
            }
            return -1;
        }
        
        public ApplicationInfo getApplicationInfo(int position) {
            int imax = mAppLocalList.size();
            if( (position < 0) || (position >= imax)) {
                Log.w(TAG, "Position out of bounds in List Adapter");
                return null;
            }
            return mAppLocalList.get(position);
        }
        
        public void addApplicationInfo(ApplicationInfo info) {
            if(info == null) {
                Log.w(TAG, "Ignoring null add in List Adapter");
                return;
            }
            mAppLocalList.add(info);
        }

        public long getItemId(int position) {
            int imax = mAppLocalList.size();
            if( (position < 0) || (position >= imax)) {
                Log.w(TAG, "Position out of bounds in List Adapter");
                return -1;
            }
            return mAppPropMap.get(mAppLocalList.get(position).packageName).index;
        }
        
        public List<ApplicationInfo> getAppList() {
            return mAppLocalList;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position >= mAppLocalList.size()) {
                Log.w(TAG, "Invalid view position:"+position+", actual size is:"+mAppLocalList.size());
                return null;
            }
            // A ViewHolder keeps references to children views to avoid unneccessary calls
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
                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (AppViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder
            ApplicationInfo appInfo = mAppLocalList.get(position);
            AppInfo mInfo = mAppPropMap.get(appInfo.packageName);
            if(mInfo != null) {
                if(mInfo.appName != null) {
                    holder.appName.setText(mInfo.appName);
                }
                if(mInfo.appIcon != null) {
                    holder.appIcon.setImageDrawable(mInfo.appIcon);
                }
                if (mInfo.appSize != null) {
                    holder.appSize.setText(mInfo.appSize);
                }
            } else {
                Log.w(TAG, "No info for package:"+appInfo.packageName+" in property map");
            }
            return convertView;
        }
        
        private void adjustIndex() {
            int imax = mAppLocalList.size();
            ApplicationInfo info;
            for (int i = 0; i < imax; i++) {
                info = mAppLocalList.get(i);
                mAppPropMap.get(info.packageName).index = i;
            }
        }
        
        public void sortAppList(int sortOrder) {
            Collections.sort(mAppLocalList, getAppComparator(sortOrder));
        }
        
        public void sortList(int sortOrder) {
            sortAppList(sortOrder);
            adjustIndex();
            notifyDataSetChanged();
        }
        
        public boolean resetAppList(int filterOption, List<ApplicationInfo> appList) {
           // Create application list based on the filter value
           mAppLocalList = appList;
           // Check for all properties in map before sorting. Populate values from cache
           for(ApplicationInfo applicationInfo : mAppLocalList) {
               AppInfo appInfo = mAppPropMap.get(applicationInfo.packageName);
               if(appInfo == null) {
                   AppInfo rInfo = getFromCache(applicationInfo.packageName);
                   if(rInfo == null) {
                       // Need to load resources again. Inconsistency somewhere
                       return false;
                   }
                   mAppPropMap.put(applicationInfo.packageName, rInfo);
               }
           }
           if (mAppLocalList.size() > 0) {
               sortList(mSortOrder);
           } else {
               notifyDataSetChanged();
           }
           showEmptyViewIfListEmpty();
           return true;
        }
        
        private Comparator<ApplicationInfo> getAppComparator(int sortOrder) {
            if (sortOrder == SORT_ORDER_ALPHA) {
                // Lazy initialization
                if (mAlphaComparator == null) {
                    mAlphaComparator = new ApplicationInfo.DisplayNameComparator(mPm);
                }
                return mAlphaComparator;
            }
            // Lazy initialization
            if(mSizeComparator == null) {
                mSizeComparator = new AppInfoComparator(mAppPropMap);
            }
            return mSizeComparator;
        }

        /*
         * This method updates resource information in the package map.
         * 
         *  @param iconMap a map of package names and attributes
         *  @return A boolean value to indicate if the property map has to be
         *  refreshed completely
         */
        public boolean updateAppsResourceInfo(Map<String, AppInfo> iconMap) {
            if(iconMap == null) {
                Log.w(TAG, "Null iconMap when refreshing icon in List Adapter");
                return true;
            }
            boolean changed = false;
            for (ApplicationInfo info : mAppLocalList) {
                AppInfo pInfo = iconMap.get(info.packageName);
                if(pInfo != null) {
                    AppInfo aInfo = mAppPropMap.get(info.packageName);
                    if (aInfo != null) {
                        aInfo.refreshIcon(pInfo);
                    } else {
                        return false;
                    }
                    changed = true;
                }
            }
            if(changed) {
                notifyDataSetChanged();
            }
            return true;
        }
        
        private boolean shouldBeInList(int filterOption, ApplicationInfo info) {
            // Match filter here
            boolean addToCurrList = false;
            if (filterOption == FILTER_APPS_RUNNING) {
                List<ApplicationInfo> runningList = getInstalledApps(FILTER_APPS_RUNNING);
                for (ApplicationInfo running : runningList) {
                    if (running.packageName.equalsIgnoreCase(info.packageName)) {
                        addToCurrList = true;
                        break;
                    }
                }
            } else if (filterOption == FILTER_APPS_THIRD_PARTY) {
                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    addToCurrList = true;
                } else if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    addToCurrList = true;
                }
            } else {
                return true;
            }
            return addToCurrList;
        }
        
        /*
         * Add a package to the current list.
         * The package is only added to the displayed list
         * based on the filter value. The package is always added to the property map.
         * @param pkgName name of package to be added
         * @param ps PackageStats of new package
         */
        public void addToList(String pkgName, PackageStats ps) {
            if(pkgName == null) {
                Log.w(TAG, "Adding null pkg to List Adapter");
                return;
            }
            boolean notInList = true;
            int newIdx = getIndex(pkgName);
            if (newIdx != -1) {
                notInList = false;
                if (mAppPropMap.get(pkgName) != null) {
                    // weird. just ignore entry
                    Log.i(TAG, "Package:"+pkgName+" already added");
                    return;
                }
            }
            // Get ApplicationInfo
            ApplicationInfo info = null;
            try {
                info = mPm.getApplicationInfo(pkgName, 0);
            } catch (NameNotFoundException e) {
                Log.w(TAG, "Ignoring non-existent package:"+pkgName);
                return;
            }
            if(info == null) {
                // Nothing to do log error message and return
                Log.i(TAG, "Null ApplicationInfo for package:"+pkgName);
                return;
            }
            // Add entry to map
            mAppPropMap.put(pkgName, new AppInfo(pkgName, newIdx,
                    info.loadLabel(mPm), info.loadIcon(mPm), ps));
            // Add to list
            if (notInList && (shouldBeInList(mFilterApps, info))) {
                // Binary search returns a negative index (ie -index) of the position where
                // this might be inserted. 
                newIdx = Collections.binarySearch(mAppLocalList, info, 
                        getAppComparator(mSortOrder));
                if(newIdx >= 0) {
                    Log.i(TAG, "Strange. Package:"+pkgName+" is not new");
                    return;
                }
                // New entry
                newIdx = -newIdx-1;
                mAppLocalList.add(newIdx, info);
                // Adjust index
                adjustIndex();
                notifyDataSetChanged();
            }
        }
        
        public void removeFromList(List<String> pkgNames) {
            if(pkgNames == null) {
                Log.w(TAG, "Removing null pkg list from List Adapter");
                return;
            }
            int imax = mAppLocalList.size();
            boolean found = false;
            ApplicationInfo info;
            int i, k;
            String pkgName;
            int kmax = pkgNames.size();
            if(kmax  <= 0) {
                Log.w(TAG, "Removing empty pkg list from List Adapter");
                return;
            }
            int idxArr[] = new int[kmax];
            for (k = 0; k < kmax; k++) {
                idxArr[k] = -1;
            }
            for (i = 0; i < imax; i++) {
                info = mAppLocalList.get(i);
                for (k = 0; k < kmax; k++) {
                    pkgName = pkgNames.get(k);
                    if (info.packageName.equalsIgnoreCase(pkgName)) {
                        idxArr[k] = i;
                        found = true;
                        break;
                    }
                }
            }
            // Sort idxArr
            Arrays.sort(idxArr);
            // remove the packages based on decending indices
            for (k = kmax-1; k >= 0; k--) {
                // Check if package has been found in the list of existing apps first
                if(idxArr[k] == -1) {
                    break;
                }
                info = mAppLocalList.get(idxArr[k]);
                mAppLocalList.remove(idxArr[k]);
                mAppPropMap.remove(info.packageName);
                if (localLOGV) Log.i(TAG, "Removed pkg:"+info.packageName+ " list");
            }
            if (found) {
                adjustIndex();
                notifyDataSetChanged();
            }
        }   
        
        public void updateAppSize(String pkgName, PackageStats ps) {
            if(pkgName == null) {
                return;
            }
            AppInfo entry = mAppPropMap.get(pkgName);
            if (entry == null) {
                Log.w(TAG, "Entry for package:"+pkgName+"doesnt exist in map");
                return;
            }
            // Copy the index into the newly updated entry
            entry.setSize(ps);
            notifyDataSetChanged();
        }

        public PackageStats getAppStats(String pkgName) {
            if(pkgName == null) {
                return null;
            }
            AppInfo entry = mAppPropMap.get(pkgName);
            if (entry == null) {
                return null;
            }
            return entry.appStats;
        }
    }
    
    /*
     * Utility method to clear messages to Handler
     * We need'nt synchronize on the Handler since posting messages is guaranteed
     * to be thread safe. Even if the other thread that retrieves package sizes
     * posts a message, we do a cursory check of validity on mAppInfoAdapter's applist
     */
    private void clearMessagesInHandler() {
        mHandler.removeMessages(INIT_PKG_INFO);
        mHandler.removeMessages(COMPUTE_PKG_SIZE_DONE);
        mHandler.removeMessages(REMOVE_PKG);
        mHandler.removeMessages(REORDER_LIST);
        mHandler.removeMessages(ADD_PKG_START);
        mHandler.removeMessages(ADD_PKG_DONE);
    }
    
    private void sendMessageToHandler(int msgId, int arg1) {
        Message msg = mHandler.obtainMessage(msgId);
        msg.arg1 = arg1;
        mHandler.sendMessage(msg);
    }
    
    private void sendMessageToHandler(int msgId, Bundle data) {
        Message msg = mHandler.obtainMessage(msgId);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }
    
    private void sendMessageToHandler(int msgId) {
        mHandler.sendEmptyMessage(msgId);
    }
    
    /*
     * Stats Observer class used to compute package sizes and retrieve size information
     * PkgSizeOberver is the call back thats used when invoking getPackageSizeInfo on
     * PackageManager. The values in call back onGetStatsCompleted are validated
     * and the specified message is passed to mHandler. The package name
     * and the AppInfo object corresponding to the package name are set on the message
     */
    class PkgSizeObserver extends IPackageStatsObserver.Stub {
        private ApplicationInfo mAppInfo;
        private int mMsgId; 
        public void onGetStatsCompleted(PackageStats pStats, boolean pSucceeded) {
            if(DEBUG_PKG_DELAY) {
                try {
                    Thread.sleep(10*1000);
                } catch (InterruptedException e) {
                }
            }
            AppInfo appInfo = null;
            Bundle data = new Bundle();
            data.putString(ATTR_PKG_NAME, mAppInfo.packageName);
            if(pSucceeded && pStats != null) {
                if (localLOGV) Log.i(TAG, "onGetStatsCompleted::"+pStats.packageName+", ("+
                        pStats.cacheSize+","+
                        pStats.codeSize+", "+pStats.dataSize);
                data.putParcelable(ATTR_APP_PKG_STATS, pStats);
            } else {
                Log.w(TAG, "Invalid package stats from PackageManager");
            }
            //post message to Handler
            Message msg = mHandler.obtainMessage(mMsgId, data);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }

        public void invokeGetSizeInfo(ApplicationInfo pAppInfo, int msgId) {
            if(pAppInfo == null || pAppInfo.packageName == null) {
                return;
            }
            if(localLOGV) Log.i(TAG, "Invoking getPackageSizeInfo for package:"+
                    pAppInfo.packageName);
            mMsgId = msgId;
            mAppInfo = pAppInfo;
            mPm.getPackageSizeInfo(pAppInfo.packageName, this);
        }
    }
    
    /**
     * Receives notifications when applications are added/removed.
     */
    private class PackageIntentReceiver extends BroadcastReceiver {
         void registerReceiver() {
             IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
             filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
             filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
             filter.addDataScheme("package");
             ManageApplications.this.registerReceiver(this, filter);
         }
        @Override
        public void onReceive(Context context, Intent intent) {
            String actionStr = intent.getAction();
            Uri data = intent.getData();
            String pkgName = data.getEncodedSchemeSpecificPart();
            if (localLOGV) Log.i(TAG, "action:"+actionStr+", for package:"+pkgName);
            updatePackageList(actionStr, pkgName);
        }
    }
    
    private void updatePackageList(String actionStr, String pkgName) {
        // technically we dont have to invoke handler since onReceive is invoked on
        // the main thread but doing it here for better clarity
        if (Intent.ACTION_PACKAGE_ADDED.equalsIgnoreCase(actionStr)) {
            Bundle data = new Bundle();
            data.putString(ATTR_PKG_NAME, pkgName);
            sendMessageToHandler(ADD_PKG_START, data);
        } else if (Intent.ACTION_PACKAGE_REMOVED.equalsIgnoreCase(actionStr)) {
            Bundle data = new Bundle();
            data.putString(ATTR_PKG_NAME, pkgName);
            sendMessageToHandler(REMOVE_PKG, data);
        }
    }
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent lIntent = getIntent();
        String action = lIntent.getAction();
        if (action.equals(Intent.ACTION_MANAGE_PACKAGE_STORAGE)) {
            mSortOrder = SORT_ORDER_SIZE;
            mSizesFirst = true;
        }
        mPm = getPackageManager();
        // initialize some window features
        requestWindowFeature(Window.FEATURE_RIGHT_ICON);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.compute_sizes);
        mDefaultAppIcon =Resources.getSystem().getDrawable(
                com.android.internal.R.drawable.sym_def_app_icon);
        mInvalidSizeStr = getText(R.string.invalid_size_value);
        mComputingSizeStr = getText(R.string.computing_size);
        // initialize the inflater
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mReceiver = new PackageIntentReceiver();
        mEmptyView = (TextView) findViewById(R.id.empty_view);
        mObserver = new PkgSizeObserver();
        // Create adapter and list view here
        List<ApplicationInfo> appList = getInstalledApps(mSortOrder);
        mAppInfoAdapter = new AppInfoAdapter(this, appList);
        ListView lv= (ListView) findViewById(android.R.id.list);
        lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        lv.setOnItemClickListener(this);
        mListView = lv;
        showLoadingMsg();
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DLG_LOADING) {
            ProgressDialog dlg = new ProgressDialog(this);
            dlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dlg.setMessage(getText(R.string.loading));
            dlg.setIndeterminate(true);        
            dlg.setOnCancelListener(this);
            return dlg;
        }
        return null;
    }
    
    
    private void showLoadingMsg() {
        showDialog(DLG_LOADING);
        if(localLOGV) Log.i(TAG, "Displaying Loading message");
    }
    
    private void dismissLoadingMsg() {
        if(localLOGV) Log.i(TAG, "Dismissing Loading message");
        dismissDialog(DLG_LOADING);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Create a thread to load resources
        mResourceThread = new ResourceLoaderThread();
        sendMessageToHandler(INIT_PKG_INFO);
        // register receiver
        mReceiver.registerReceiver();
    }

    @Override
    public void onStop() {
        super.onStop();
        // clear all messages related to application list
        clearMessagesInHandler();
        // register receiver here
        unregisterReceiver(mReceiver);        
        mAppPropCache = mAppInfoAdapter.mAppPropMap;
    }
    
    // Avoid the restart and pause when orientation changes
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    /*
     * comparator class used to sort AppInfo objects based on size
     */
    public static class AppInfoComparator implements Comparator<ApplicationInfo> {
        public AppInfoComparator(Map<String, AppInfo> pAppPropMap) {
            mAppPropMap= pAppPropMap;
        }

        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            AppInfo ainfo = mAppPropMap.get(a.packageName);
            AppInfo binfo = mAppPropMap.get(b.packageName);
            long atotal = ainfo.getTotalSize();
            long btotal = binfo.getTotalSize();
            long ret = atotal - btotal;
            // negate result to sort in descending order
            if (ret < 0) {
                return 1;
            }
            if (ret == 0) {
                return 0;
            }
            return -1;
        }
        private Map<String, AppInfo> mAppPropMap;
    }
     
    // utility method used to start sub activity
    private void startApplicationDetailsActivity() {
        // Create intent to start new activity
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, InstalledAppDetails.class);
        intent.putExtra(APP_PKG_NAME, mCurrentPkgName);
        // start new activity to display extended information
        startActivityForResult(intent, INSTALLED_APP_DETAILS);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, SORT_ORDER_ALPHA, 1, R.string.sort_order_alpha)
                .setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        menu.add(0, SORT_ORDER_SIZE, 2, R.string.sort_order_size)
                .setIcon(android.R.drawable.ic_menu_sort_by_size); 
        menu.add(0, FILTER_OPTIONS, 3, R.string.filter)
                .setIcon(R.drawable.ic_menu_filter_settings);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mFirst) {
            menu.findItem(SORT_ORDER_ALPHA).setVisible(mSortOrder != SORT_ORDER_ALPHA);
            menu.findItem(SORT_ORDER_SIZE).setVisible(mSortOrder != SORT_ORDER_SIZE);
            menu.findItem(FILTER_OPTIONS).setVisible(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        if ((menuId == SORT_ORDER_ALPHA) || (menuId == SORT_ORDER_SIZE)) {
            sendMessageToHandler(REORDER_LIST, menuId);
        } else if (menuId == FILTER_OPTIONS) {
            if (mAlertDlg == null) {
                mAlertDlg = new AlertDialog.Builder(this).
                        setTitle(R.string.filter_dlg_title).
                        setNeutralButton(R.string.cancel, this).
                        setSingleChoiceItems(new CharSequence[] {getText(R.string.filter_apps_all),
                                getText(R.string.filter_apps_running),
                                getText(R.string.filter_apps_third_party)},
                                -1, this).
                        create();
            }
            mAlertDlg.show();
        }
        return true;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        ApplicationInfo info = (ApplicationInfo)mAppInfoAdapter.getItem(position);
        mCurrentPkgName = info.packageName;
        startApplicationDetailsActivity();
    }
    
    // Finish the activity if the user presses the back button to cancel the activity
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    public void onClick(DialogInterface dialog, int which) {
        int newOption;
        switch (which) {
        // Make sure that values of 0, 1, 2 match options all, running, third_party when
        // created via the AlertDialog.Builder
        case 0:
            newOption = FILTER_APPS_ALL;
            break;
        case 1:
            newOption = FILTER_APPS_RUNNING;
            break;
        case 2:
            newOption = FILTER_APPS_THIRD_PARTY;
            break;
        default:
            return;
        }
        mAlertDlg.dismiss();
        sendMessageToHandler(REORDER_LIST, newOption);
    }
}

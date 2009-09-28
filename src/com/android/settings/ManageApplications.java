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
import android.content.SharedPreferences;
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
import android.os.SystemClock;
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
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

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
    private static final String PREFS_NAME = "ManageAppsInfo.prefs";
    private static final String PREF_DISABLE_CACHE = "disableCache";
    
    // Log information boolean
    private boolean localLOGV = Config.LOGV || false;
    private static final boolean DEBUG_SIZE = false;
    private static final boolean DEBUG_TIME = false;
    
    // attributes used as keys when passing values to InstalledAppDetails activity
    public static final String APP_PKG_PREFIX = "com.android.settings.";
    public static final String APP_PKG_NAME = APP_PKG_PREFIX+"ApplicationPkgName";
    public static final String APP_CHG = APP_PKG_PREFIX+"changed";
    
    // attribute name used in receiver for tagging names of added/deleted packages
    private static final String ATTR_PKG_NAME="p";
    private static final String ATTR_PKGS="ps";
    private static final String ATTR_STATS="ss";
    private static final String ATTR_SIZE_STRS="fs";
    
    private static final String ATTR_GET_SIZE_STATUS="passed";
    private static final String ATTR_PKG_STATS="s";
    private static final String ATTR_PKG_SIZE_STR="f";
    
    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;
    
    // sort order that can be changed through the menu can be sorted alphabetically
    // or size(descending)
    private static final int MENU_OPTIONS_BASE = 0;
    // Filter options used for displayed list of applications
    public static final int FILTER_APPS_ALL = MENU_OPTIONS_BASE + 0;
    public static final int FILTER_APPS_RUNNING = MENU_OPTIONS_BASE + 1;
    public static final int FILTER_APPS_THIRD_PARTY = MENU_OPTIONS_BASE + 2;
    public static final int FILTER_OPTIONS = MENU_OPTIONS_BASE + 3;
    public static final int SORT_ORDER_ALPHA = MENU_OPTIONS_BASE + 4;
    public static final int SORT_ORDER_SIZE = MENU_OPTIONS_BASE + 5;
    // Alert Dialog presented to user to find out the filter option
    private AlertDialog mAlertDlg;
    // sort order
    private int mSortOrder = SORT_ORDER_ALPHA;
    // Filter value
    private int mFilterApps = FILTER_APPS_THIRD_PARTY;
    
    // Custom Adapter used for managing items in the list
    private AppInfoAdapter mAppInfoAdapter;
    
    // messages posted to the handler
    private static final int HANDLER_MESSAGE_BASE = 0;
    private static final int INIT_PKG_INFO = HANDLER_MESSAGE_BASE+1;
    private static final int COMPUTE_BULK_SIZE = HANDLER_MESSAGE_BASE+2;
    private static final int REMOVE_PKG = HANDLER_MESSAGE_BASE+3;
    private static final int REORDER_LIST = HANDLER_MESSAGE_BASE+4;
    private static final int ADD_PKG_START = HANDLER_MESSAGE_BASE+5;
    private static final int ADD_PKG_DONE = HANDLER_MESSAGE_BASE+6;
    private static final int REFRESH_LABELS = HANDLER_MESSAGE_BASE+7;
    private static final int REFRESH_DONE = HANDLER_MESSAGE_BASE+8;
    private static final int NEXT_LOAD_STEP = HANDLER_MESSAGE_BASE+9;
    private static final int COMPUTE_END = HANDLER_MESSAGE_BASE+10;
    private static final int REFRESH_ICONS = HANDLER_MESSAGE_BASE+11;
    
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
    private static Drawable mDefaultAppIcon;
    
    // temporary dialog displayed while the application info loads
    private static final int DLG_BASE = 0;
    private static final int DLG_LOADING = DLG_BASE + 1;
    
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
    private TaskRunner mSizeComputor;
    
    String mCurrentPkgName;
    
    // Cache application attributes
    private AppInfoCache mCache = new AppInfoCache();
    
    // Boolean variables indicating state
    private boolean mLoadLabels = false;
    private boolean mSizesFirst = false;
    // ListView used to display list
    private ListView mListView;
    // State variables used to figure out menu options and also
    // initiate the first computation and loading of resources
    private boolean mJustCreated = true;
    private boolean mFirst = false;
    private long mLoadTimeStart;
    private boolean mSetListViewLater = true;
    
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
            boolean status;
            long size;
            String formattedSize;
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
                if(localLOGV) Log.i(TAG, "Message INIT_PKG_INFO, justCreated = " + mJustCreated);
                List<ApplicationInfo> newList = null;
                if (!mJustCreated) {
                    if (localLOGV) Log.i(TAG, "List already created");
                    // Add or delete newly created packages by comparing lists
                    newList = getInstalledApps(FILTER_APPS_ALL);
                    updateAppList(newList);
                }
                // Retrieve the package list and init some structures
                initAppList(newList, mFilterApps);
                mHandler.sendEmptyMessage(NEXT_LOAD_STEP);
                break;
            case COMPUTE_BULK_SIZE:
                if(localLOGV) Log.i(TAG, "Message COMPUTE_BULK_PKG_SIZE");
                String[] pkgs = data.getStringArray(ATTR_PKGS);
                long[] sizes = data.getLongArray(ATTR_STATS);
                String[] formatted = data.getStringArray(ATTR_SIZE_STRS);
                if(pkgs == null || sizes == null || formatted == null) {
                     Log.w(TAG, "Ignoring message");
                     break;
                }
                mAppInfoAdapter.bulkUpdateSizes(pkgs, sizes, formatted);
                break;
            case COMPUTE_END :
                mComputeSizes = true;
                mFirst = true;
                mHandler.sendEmptyMessage(NEXT_LOAD_STEP);
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
                    boolean ret = mAppInfoAdapter.resetAppList(mFilterApps);
                    if(!ret) {
                        // Reset cache
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
                if (!mComputeSizes || !mLoadLabels) {
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
                mObserver.invokeGetSizeInfo(pkgName);
                break;
            case ADD_PKG_DONE:
                if(localLOGV) Log.i(TAG, "Message ADD_PKG_DONE");
                if(pkgName == null) {
                    Log.w(TAG, "Ignoring message:ADD_PKG_START for null pkgName");
                    break;
                }
                status = data.getBoolean(ATTR_GET_SIZE_STATUS);
                if (status) {
                    size = data.getLong(ATTR_PKG_STATS);
                    formattedSize = data.getString(ATTR_PKG_SIZE_STR);
                    if (!mAppInfoAdapter.isInstalled(pkgName)) {
                        mAppInfoAdapter.addToList(pkgName, size, formattedSize);
                    } else {
                        mAppInfoAdapter.updatePackage(pkgName, size, formattedSize);
                    }
                }
                break;
            case REFRESH_LABELS:
                Map<String, CharSequence> labelMap = (Map<String, CharSequence>) msg.obj;
                if (labelMap != null) {
                    mAppInfoAdapter.bulkUpdateLabels(labelMap);
                }
                break;
            case REFRESH_ICONS:
                Map<String, Drawable> iconMap = (Map<String, Drawable>) msg.obj;
                if (iconMap != null) {
                    mAppInfoAdapter.bulkUpdateIcons(iconMap);
                }
                break;
            case REFRESH_DONE:
                mLoadLabels = true;
                mHandler.sendEmptyMessage(NEXT_LOAD_STEP);
                break;
            case NEXT_LOAD_STEP:
                if (!mCache.isEmpty() && mSetListViewLater) {
                    if (localLOGV) Log.i(TAG, "Using cache to populate list view");
                    initListView();
                    mSetListViewLater = false;
                    mFirst = true;
                }
                if (mComputeSizes && mLoadLabels) {
                    doneLoadingData();
                    // Check for added/removed packages
                    Set<String> keys =  mAddRemoveMap.keySet();
                    for (String key : keys) {
                        if (mAddRemoveMap.get(key) == Boolean.TRUE) {
                            // Add the package
                            updatePackageList(Intent.ACTION_PACKAGE_ADDED, key);
                        } else {
                            // Remove the package
                            updatePackageList(Intent.ACTION_PACKAGE_REMOVED, key);
                        }
                    }
                    mAddRemoveMap.clear();
                } else if (!mComputeSizes && !mLoadLabels) {
                     // Either load the package labels or initiate get size info
                    if (mSizesFirst) {
                        initComputeSizes();
                    } else {
                        initResourceThread();
                    }
                } else {
                    if (mSetListViewLater) {
                        if (localLOGV) Log.i(TAG, "Initing list view for very first time");
                        initListView();
                        mSetListViewLater = false;
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
    
    private void initListView() {
       // Create list view from the adapter here. Wait till the sort order
        // of list is defined. its either by label or by size. So atleast one of the
        // first steps should have been completed before the list gets filled.
        mAppInfoAdapter.sortBaseList(mSortOrder);
        if (mJustCreated) {
            // Set the adapter here.
            mJustCreated = false;
            mListView.setAdapter(mAppInfoAdapter);
            dismissLoadingMsg();
        }
    }

   class SizeObserver extends IPackageStatsObserver.Stub {
       private CountDownLatch mCount;
       PackageStats stats;
       boolean succeeded;
       
       public void invokeGetSize(String packageName, CountDownLatch count) {
           mCount = count;
           mPm.getPackageSizeInfo(packageName, this);
       }
       
        public void onGetStatsCompleted(PackageStats pStats, boolean pSucceeded) {
            succeeded = pSucceeded;
            stats = pStats;
            mCount.countDown();
        }
    }

    class TaskRunner extends Thread {
        private List<ApplicationInfo> mPkgList;
        private SizeObserver mSizeObserver;
        private static final int END_MSG = COMPUTE_END;
        private static final int SEND_PKG_SIZES = COMPUTE_BULK_SIZE;
        volatile boolean abort = false;
        static final int MSG_PKG_SIZE = 8;
        
        TaskRunner(List<ApplicationInfo> appList) {
           mPkgList = appList;
           mSizeObserver = new SizeObserver();
           start();
        }
        
        public void setAbort() {
            abort = true;
        }

        public void run() {
            long startTime;
            if (DEBUG_SIZE || DEBUG_TIME) {
               startTime =  SystemClock.elapsedRealtime();
            }
            int size = mPkgList.size();
            int numMsgs = size / MSG_PKG_SIZE;
            if (size > (numMsgs * MSG_PKG_SIZE)) {
                numMsgs++;
            }
            int endi = 0;
            for (int j = 0; j < size; j += MSG_PKG_SIZE) {
                long sizes[];
                String formatted[];
                String packages[];
                endi += MSG_PKG_SIZE;
                if (endi > size) {
                    endi = size;
                }
                sizes = new long[endi-j];
                formatted = new String[endi-j];
                packages = new String[endi-j];
                for (int i = j; i < endi; i++) {
                    if (abort) {
                        // Exit if abort has been set.
                        break;
                    }
                    CountDownLatch count = new CountDownLatch(1);
                    String packageName = mPkgList.get(i).packageName;
                    mSizeObserver.invokeGetSize(packageName, count);
                    try {
                        count.await();
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Failed computing size for pkg : "+packageName);
                    }
                    // Process the package statistics
                    PackageStats pStats = mSizeObserver.stats;
                    boolean succeeded = mSizeObserver.succeeded;
                    long total;
                    if(succeeded && pStats != null) {
                        total = getTotalSize(pStats);
                    } else {
                        total = SIZE_INVALID;
                    }
                    sizes[i-j] = total;
                    formatted[i-j] = getSizeStr(total).toString();
                    packages[i-j] = packageName;
                }
                // Post update message
                Bundle data = new Bundle();
                data.putStringArray(ATTR_PKGS, packages);
                data.putLongArray(ATTR_STATS, sizes);
                data.putStringArray(ATTR_SIZE_STRS, formatted);
                Message msg = mHandler.obtainMessage(SEND_PKG_SIZES, data);
                msg.setData(data);
                mHandler.sendMessage(msg);
            }
            if (DEBUG_SIZE || DEBUG_TIME) Log.i(TAG, "Took "+
                    (SystemClock.elapsedRealtime() - startTime)+
                    " ms to compute sizes of all packages ");
            mHandler.sendEmptyMessage(END_MSG);
        }
    }
    
    /*
     * This method compares the current cache against a new list of
     * installed applications and tries to update the list with add or remove
     * messages.
     */
    private boolean updateAppList(List<ApplicationInfo> newList) {
        if ((newList == null) || mCache.isEmpty()) {
            return false;
        }
        Set<String> existingList = new HashSet<String>();
        boolean ret = false;
        // Loop over new list and find out common elements between old and new lists
        int N = newList.size();
        for (int i = (N-1); i >= 0; i--) {
            ApplicationInfo info = newList.get(i);
            String pkgName = info.packageName;
            AppInfo aInfo = mCache.getEntry(pkgName);
            if (aInfo != null) {
                existingList.add(pkgName);
            } else {
                // New package. update info by refreshing
                if (localLOGV) Log.i(TAG, "New pkg :"+pkgName+" installed when paused");
                updatePackageList(Intent.ACTION_PACKAGE_ADDED, pkgName);
                // Remove from current list so that the newly added package can
                // be handled later
                newList.remove(i);
                ret = true;
            }
        }

        // Loop over old list and figure out stale entries
        List<String> deletedList = null;
        Set<String> staleList = mCache.getPkgList();
        for (String pkgName : staleList) {
            if (!existingList.contains(pkgName)) {
                if (localLOGV) Log.i(TAG, "Pkg :"+pkgName+" deleted when paused");
                if (deletedList == null) {
                    deletedList = new ArrayList<String>();
                    deletedList.add(pkgName);
                }
                ret = true;
            }
        }
        // Delete right away
        if (deletedList != null) {
            if (localLOGV) Log.i(TAG, "Deleting right away");
            mAppInfoAdapter.removeFromList(deletedList);
        }
        return ret;
    }
    
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

    private static boolean matchFilter(boolean filter, Map<String, String> filterMap, String pkg) {
        boolean add = true;
        if (filter) {
            if (filterMap == null || !filterMap.containsKey(pkg)) {
                add = false;
            }
        }
        return add;
    }
    
    /*
     * Utility method used to figure out list of apps based on filterOption
     * If the framework supports an additional flag to indicate running apps
     *  we can get away with some code here.
     */
    List<ApplicationInfo> getFilteredApps(List<ApplicationInfo> pAppList, int filterOption, boolean filter,
            Map<String, String> filterMap) {
        List<ApplicationInfo> retList = new ArrayList<ApplicationInfo>();
        if(pAppList == null) {
            return retList;
        }
        if (filterOption == FILTER_APPS_THIRD_PARTY) {
            for (ApplicationInfo appInfo : pAppList) {
                boolean flag = false;
                if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    // Updated system app
                    flag = true;
                } else if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    // Non-system app
                    flag = true;
                }
                if (flag) {
                    if (matchFilter(filter, filterMap, appInfo.packageName)) {
                        retList.add(appInfo);
                    }
                }
            }
            return retList;
        } else if (filterOption == FILTER_APPS_RUNNING) {
            List<ActivityManager.RunningAppProcessInfo> procList = getRunningAppProcessesList();
            if ((procList == null) || (procList.size() == 0)) {
                return retList;
            }
            // Retrieve running processes from ActivityManager
            HashMap<String, ActivityManager.RunningAppProcessInfo> runningMap = 
                new HashMap<String, ActivityManager.RunningAppProcessInfo>();
            for (ActivityManager.RunningAppProcessInfo appProcInfo : procList) {
                if ((appProcInfo != null)  && (appProcInfo.pkgList != null)){
                    int size = appProcInfo.pkgList.length;
                    for (int i = 0; i < size; i++) {
                        runningMap.put(appProcInfo.pkgList[i], appProcInfo);
                    }
                }
            }
            // Query list to find running processes in current list
            for (ApplicationInfo appInfo : pAppList) {
                if (runningMap.get(appInfo.packageName) != null) {
                    if (matchFilter(filter, filterMap, appInfo.packageName)) {
                        retList.add(appInfo);
                    }
                }
            }
            return retList;
        } else {
            for (ApplicationInfo appInfo : pAppList) {
                if (matchFilter(filter, filterMap, appInfo.packageName)) {
                    retList.add(appInfo);
                }
            }
            return retList;
        }
    }

    private List<ActivityManager.RunningAppProcessInfo> getRunningAppProcessesList() {
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        return am.getRunningAppProcesses();
    }

     // Some initialization code used when kicking off the size computation
    private void initAppList(List<ApplicationInfo> appList, int filterOption) {
        setProgressBarIndeterminateVisibility(true);
        mComputeSizes = false;
        mLoadLabels = false;
        // Initialize lists
        mAddRemoveMap = new TreeMap<String, Boolean>();
        mAppInfoAdapter.initMapFromList(appList, filterOption);
    }

    // Utility method to start a thread to read application labels and icons
    private void initResourceThread() {
        if ((mResourceThread != null) && mResourceThread.isAlive()) {
            mResourceThread.setAbort();
        }
        mResourceThread = new ResourceLoaderThread();
        List<ApplicationInfo> appList = mAppInfoAdapter.getBaseAppList();
        if ((appList != null) && (appList.size()) > 0) {
            mResourceThread.loadAllResources(appList);
        }
    }

    private void initComputeSizes() {
         // Initiate compute package sizes
        if (localLOGV) Log.i(TAG, "Initiating compute sizes for first time");
        if ((mSizeComputor != null) && (mSizeComputor.isAlive())) {
            mSizeComputor.setAbort();
        }
        List<ApplicationInfo> appList = mAppInfoAdapter.getBaseAppList();
        if ((appList != null) && (appList.size()) > 0) {
            mSizeComputor = new TaskRunner(appList);
        } else {
            mComputeSizes = true;
        }
    }
    
    // internal structure used to track added and deleted packages when
    // the activity has focus
    static class AddRemoveInfo {
        String pkgName;
        boolean add;
        public AddRemoveInfo(String pPkgName, boolean pAdd) {
            pkgName = pPkgName;
            add = pAdd;
        }
    }
    
    class ResourceLoaderThread extends Thread {
        List<ApplicationInfo> mAppList;
        volatile boolean abort = false;
        static final int MSG_PKG_SIZE = 8;
        
        public void setAbort() {
            abort = true;
        }
        void loadAllResources(List<ApplicationInfo> appList) {
            mAppList = appList;
            start();
        }

        public void run() {
            long start;
            if (DEBUG_TIME) {
                start = SystemClock.elapsedRealtime();
            }
            int imax;
            if(mAppList == null || (imax = mAppList.size()) <= 0) {
                Log.w(TAG, "Empty or null application list");
            } else {
                int size = mAppList.size();
                int numMsgs = size / MSG_PKG_SIZE;
                if (size > (numMsgs * MSG_PKG_SIZE)) {
                    numMsgs++;
                }
                int endi = 0;
                for (int j = 0; j < size; j += MSG_PKG_SIZE) {
                    Map<String, CharSequence> map = new HashMap<String, CharSequence>();
                    endi += MSG_PKG_SIZE;
                    if (endi > size) {
                        endi = size;
                    }
                    for (int i = j; i < endi; i++) {
                        if (abort) {
                            // Exit if abort has been set.
                            break;
                        }
                        ApplicationInfo appInfo = mAppList.get(i);
                        map.put(appInfo.packageName, appInfo.loadLabel(mPm));
                    }
                    // Post update message
                    Message msg = mHandler.obtainMessage(REFRESH_LABELS);
                    msg.obj = map;
                    mHandler.sendMessage(msg);
                }
                Message doneMsg = mHandler.obtainMessage(REFRESH_DONE);
                mHandler.sendMessage(doneMsg);
                if (DEBUG_TIME) Log.i(TAG, "Took "+(SystemClock.elapsedRealtime()-start)+
                        " ms to load app labels");
                long startIcons;
                if (DEBUG_TIME) {
                    startIcons = SystemClock.elapsedRealtime();
                }
                Map<String, Drawable> map = new HashMap<String, Drawable>();
                for (int i = (imax-1); i >= 0; i--) {
                    if (abort) {
                        return;
                    }
                    ApplicationInfo appInfo = mAppList.get(i);
                    map.put(appInfo.packageName, appInfo.loadIcon(mPm));
                }
                Message msg = mHandler.obtainMessage(REFRESH_ICONS);
                msg.obj = map;
                mHandler.sendMessage(msg);
                if (DEBUG_TIME) Log.i(TAG, "Took "+(SystemClock.elapsedRealtime()-startIcons)+" ms to load app icons");
            }
            if (DEBUG_TIME) Log.i(TAG, "Took "+(SystemClock.elapsedRealtime()-start)+" ms to load app resources");
        }
    }
    
    /* Internal class representing an application or packages displayable attributes
     * 
     */
    static private class AppInfo {
        public String pkgName;
        int index;
        public  CharSequence appName;
        public  Drawable appIcon;
        public CharSequence appSize;
        long size;

        public void refreshIcon(Drawable icon) {
            if (icon == null) {
                return;
            }
            appIcon = icon;
        }
        public void refreshLabel(CharSequence label) {
            if (label == null) {
                return;
            }
            appName = label;
        }

        public AppInfo(String pName, int pIndex, CharSequence aName,
                long pSize,
                CharSequence pSizeStr) {
            this(pName, pIndex, aName, mDefaultAppIcon, pSize, pSizeStr);
        }
 
        public AppInfo(String pName, int pIndex, CharSequence aName, Drawable aIcon,
                long pSize,
                CharSequence pSizeStr) {
            index = pIndex;
            pkgName = pName;
            appName = aName;
            appIcon = aIcon;
            size = pSize;
            appSize = pSizeStr;
        }
 
        public boolean setSize(long newSize, String formattedSize) {
            if (size != newSize) {
                size = newSize;
                appSize = formattedSize;
                return true;
            }
            return false;
        }
    }
    
    private long getTotalSize(PackageStats ps) {
        if (ps != null) {
            return ps.cacheSize+ps.codeSize+ps.dataSize;
        }
        return SIZE_INVALID;
    }

    private CharSequence getSizeStr(long size) {
        CharSequence appSize = null;
        if (size == SIZE_INVALID) {
             return mInvalidSizeStr;
        }
        appSize = Formatter.formatFileSize(ManageApplications.this, size);
        return appSize;
    }

    // View Holder used when displaying views
    static class AppViewHolder {
        TextView appName;
        ImageView appIcon;
        TextView appSize;
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
    class AppInfoAdapter extends BaseAdapter implements Filterable {   
        private List<ApplicationInfo> mAppList;
        private List<ApplicationInfo> mAppLocalList;
        private Map<String, String> mFilterMap = new HashMap<String, String>();
        AlphaComparator mAlphaComparator = new AlphaComparator();
        SizeComparator mSizeComparator = new SizeComparator();
        private Filter mAppFilter = new AppFilter();
        final private Object mFilterLock = new Object();
        private Map<String, String> mCurrentFilterMap = null;

        private void generateFilterListLocked(List<ApplicationInfo> list) {
            mAppLocalList = new ArrayList<ApplicationInfo>(list);
            synchronized(mFilterLock) {
                for (ApplicationInfo info : mAppLocalList) {
                    String label = info.packageName;
                    AppInfo aInfo = mCache.getEntry(info.packageName);
                    if ((aInfo != null) && (aInfo.appName != null)) {
                        label = aInfo.appName.toString();
                    }
                    mFilterMap.put(info.packageName, label.toLowerCase());
                }
            }
        }

        private void addFilterListLocked(int newIdx, ApplicationInfo info, CharSequence pLabel) {
            mAppLocalList.add(newIdx, info);
            synchronized (mFilterLock) {
                String label = info.packageName;
                if (pLabel != null) {
                    label = pLabel.toString();
                }
                mFilterMap.put(info.packageName, label.toLowerCase());
            }
        }

        private boolean removeFilterListLocked(String removePkg) {
            // Remove from filtered list
            int N = mAppLocalList.size();
            int i;
            for (i = (N-1); i >= 0; i--) {
                ApplicationInfo info = mAppLocalList.get(i);
                if (info.packageName.equalsIgnoreCase(removePkg)) {
                    if (localLOGV) Log.i(TAG, "Removing " + removePkg + " from local list");
                    mAppLocalList.remove(i);
                    synchronized (mFilterLock) {
                        mFilterMap.remove(removePkg);
                    }
                    return true;
                }
            }
            return false;
        }

        private void reverseGenerateList() {
            generateFilterListLocked(getFilteredApps(mAppList, mFilterApps, mCurrentFilterMap!= null, mCurrentFilterMap));
            sortListInner(mSortOrder);
        }

        // Make sure the cache or map contains entries for all elements
        // in appList for a valid sort.
        public void initMapFromList(List<ApplicationInfo> pAppList, int filterOption) {
            boolean notify = false;
            List<ApplicationInfo> appList = null;
            if (pAppList == null) {
                // Just refresh the list
                appList = mAppList;
            } else {
                mAppList = new ArrayList<ApplicationInfo>(pAppList);
                appList = pAppList;
                notify = true;
            }
            generateFilterListLocked(getFilteredApps(appList, filterOption, mCurrentFilterMap!= null, mCurrentFilterMap));
            // This loop verifies and creates new entries for new packages in list
            int imax = appList.size();
            for (int i = 0; i < imax; i++) {
                ApplicationInfo info  = appList.get(i);
                AppInfo aInfo = mCache.getEntry(info.packageName);
                if(aInfo == null){
                    aInfo = new AppInfo(info.packageName, i, 
                            info.packageName, -1, mComputingSizeStr);
                    if (localLOGV) Log.i(TAG, "Creating entry pkg:"+info.packageName+" to map");
                    mCache.addEntry(aInfo);
                }
            }
            sortListInner(mSortOrder);
            if (notify) {
                notifyDataSetChanged();
            }
        }
        
        public AppInfoAdapter(Context c, List<ApplicationInfo> appList) {
           mAppList = appList;
        }
        
        public int getCount() {
            return mAppLocalList.size();
        }
        
        public Object getItem(int position) {
            return mAppLocalList.get(position);
        }
        
        public boolean isInstalled(String pkgName) {
            if(pkgName == null) {
                if (localLOGV) Log.w(TAG, "Null pkg name when checking if installed");
                return false;
            }
            for (ApplicationInfo info : mAppList) {
                if (info.packageName.equalsIgnoreCase(pkgName)) {
                    return true;
                }
            }
            return false;
        }

        public ApplicationInfo getApplicationInfo(int position) {
            int imax = mAppLocalList.size();
            if( (position < 0) || (position >= imax)) {
                Log.w(TAG, "Position out of bounds in List Adapter");
                return null;
            }
            return mAppLocalList.get(position);
        }

        public long getItemId(int position) {
            int imax = mAppLocalList.size();
            if( (position < 0) || (position >= imax)) {
                Log.w(TAG, "Position out of bounds in List Adapter");
                return -1;
            }
            AppInfo aInfo = mCache.getEntry(mAppLocalList.get(position).packageName);
            if (aInfo == null) {
                return -1;
            }
            return aInfo.index;
        }
        
        public List<ApplicationInfo> getBaseAppList() {
            return mAppList;
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
            AppInfo mInfo = mCache.getEntry(appInfo.packageName);
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
            for (int i = 0; i < imax; i++) {
                ApplicationInfo info = mAppLocalList.get(i);
                mCache.getEntry(info.packageName).index = i;
            }
        }
        
        public void sortAppList(List<ApplicationInfo> appList, int sortOrder) {
            Collections.sort(appList, getAppComparator(sortOrder));
        }
        
        public void sortBaseList(int sortOrder) {
            if (localLOGV) Log.i(TAG, "Sorting base list based on sortOrder = "+sortOrder);
            sortAppList(mAppList, sortOrder);
            generateFilterListLocked(getFilteredApps(mAppList, mFilterApps, mCurrentFilterMap!= null, mCurrentFilterMap));
            adjustIndex();
        }

        private void sortListInner(int sortOrder) {
            sortAppList(mAppLocalList, sortOrder);
            adjustIndex(); 
        }
        
        public void sortList(int sortOrder) {
            if (localLOGV) Log.i(TAG, "sortOrder = "+sortOrder);
            sortListInner(sortOrder);
            notifyDataSetChanged();
        }
        
        /*
         * Reset the application list associated with this adapter.
         * @param filterOption Sort the list based on this value
         * @param appList the actual application list that is used to reset
         * @return Return a boolean value to indicate inconsistency
         */
        public boolean resetAppList(int filterOption) {
           // Change application list based on filter option
           generateFilterListLocked(getFilteredApps(mAppList, filterOption, mCurrentFilterMap!= null, mCurrentFilterMap));
           // Check for all properties in map before sorting. Populate values from cache
           for(ApplicationInfo applicationInfo : mAppLocalList) {
               AppInfo appInfo = mCache.getEntry(applicationInfo.packageName);
               if(appInfo == null) {
                  Log.i(TAG, " Entry does not exist for pkg:  " + applicationInfo.packageName);
               }
           }
           if (mAppLocalList.size() > 0) {
               sortList(mSortOrder);
           } else {
               notifyDataSetChanged();
           }
           return true;
        }
        
        private Comparator<ApplicationInfo> getAppComparator(int sortOrder) {
            if (sortOrder == SORT_ORDER_ALPHA) {
                return mAlphaComparator;
            }
            return mSizeComparator;
        }

        public void bulkUpdateIcons(Map<String, Drawable> icons) {
            if (icons == null) {
                return;
            }
            Set<String> keys = icons.keySet();
            boolean changed = false;
            for (String key : keys) {
                Drawable ic = icons.get(key);
                if (ic != null) {
                    AppInfo aInfo = mCache.getEntry(key);
                    if (aInfo != null) {
                        aInfo.refreshIcon(ic);
                        changed = true;
                    }
                }
            }
            if (changed) {
                notifyDataSetChanged();
            }
        }

        public void bulkUpdateLabels(Map<String, CharSequence> map) {
            if (map == null) {
                return;
            }
            Set<String> keys = map.keySet();
            boolean changed = false;
            for (String key : keys) {
                CharSequence label = map.get(key);
                AppInfo aInfo = mCache.getEntry(key);
                if (aInfo != null) {
                    aInfo.refreshLabel(label);
                    changed = true;
                }
            }
            if (changed) {
                notifyDataSetChanged();
            }
        }

        private boolean shouldBeInList(int filterOption, ApplicationInfo info) {
            // Match filter here
            if (filterOption == FILTER_APPS_RUNNING) {
                List<ApplicationInfo> runningList = getInstalledApps(FILTER_APPS_RUNNING);
                for (ApplicationInfo running : runningList) {
                    if (running.packageName.equalsIgnoreCase(info.packageName)) {
                        return true;
                    }
                }
            } else if (filterOption == FILTER_APPS_THIRD_PARTY) {
                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    return true;
                } else if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    return true;
                }
            } else {
                return true;
            }
            return false;
        }
        
        /*
         * Add a package to the current list.
         * The package is only added to the displayed list
         * based on the filter value. The package is always added to the property map.
         * @param pkgName name of package to be added
         * @param ps PackageStats of new package
         */
        public void addToList(String pkgName, long size, String formattedSize) {
            if (pkgName == null) {
                return;
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
            // Add entry to base list
            mAppList.add(info);
            // Add entry to map. Note that the index gets adjusted later on based on
            // whether the newly added package is part of displayed list
            CharSequence label = info.loadLabel(mPm);
            mCache.addEntry(new AppInfo(pkgName, -1,
                    label, info.loadIcon(mPm), size, formattedSize));
            if (addLocalEntry(info, label)) {
                notifyDataSetChanged();
            }
        }

        private boolean addLocalEntry(ApplicationInfo info, CharSequence label) {
            String pkgName = info.packageName;
            // Add to list
            if (shouldBeInList(mFilterApps, info)) {
                // Binary search returns a negative index (ie -index) of the position where
                // this might be inserted. 
                int newIdx = Collections.binarySearch(mAppLocalList, info, 
                        getAppComparator(mSortOrder));
                if(newIdx >= 0) {
                    if (localLOGV) Log.i(TAG, "Strange. Package:" + pkgName + " is not new");
                    return false;
                }
                // New entry
                newIdx = -newIdx-1;
                addFilterListLocked(newIdx, info, label);
                // Adjust index
                adjustIndex();
                return true;
            }
            return false;
        }

        public void updatePackage(String pkgName,
                long size, String formattedSize) {
            ApplicationInfo info = null;
            try {
                info = mPm.getApplicationInfo(pkgName,
                        PackageManager.GET_UNINSTALLED_PACKAGES);
            } catch (NameNotFoundException e) {
                return;
            }
            AppInfo aInfo = mCache.getEntry(pkgName);
            if (aInfo != null) {
                CharSequence label = info.loadLabel(mPm);
                aInfo.refreshLabel(label);
                aInfo.refreshIcon(info.loadIcon(mPm));
                aInfo.setSize(size, formattedSize);
                // Check if the entry has to be added to the displayed list
                addLocalEntry(info, label);
                // Refresh list since size might have changed
                notifyDataSetChanged();
            }
        }

        private void removePkgBase(String pkgName) {
            int imax = mAppList.size();
            for (int i = 0; i < imax; i++) {
                ApplicationInfo app = mAppList.get(i);
                if (app.packageName.equalsIgnoreCase(pkgName)) {
                    if (localLOGV) Log.i(TAG, "Removing pkg: "+pkgName+" from base list");
                    mAppList.remove(i);
                    return;
                }
            }
        }
 
        public void removeFromList(List<String> pkgNames) {
            if(pkgNames == null) {
                return;
            }
            if(pkgNames.size()  <= 0) {
                return;
            }
            boolean found = false;
            for (String pkg : pkgNames) {
                // Remove from the base application list
                removePkgBase(pkg);
                // Remove from cache
                if (localLOGV) Log.i(TAG, "Removing " + pkg + " from cache");
                mCache.removeEntry(pkg);
                // Remove from filtered list
                if (removeFilterListLocked(pkg)) {
                    found = true;
                }
            }
            // Adjust indices of list entries
            if (found) {
                adjustIndex();
                if (localLOGV) Log.i(TAG, "adjusting index and notifying list view");
                notifyDataSetChanged();
            }
        }

        public void bulkUpdateSizes(String pkgs[], long sizes[], String formatted[]) {
            if(pkgs == null || sizes == null || formatted == null) {
                return;
            }
            boolean changed = false;
            for (int i = 0; i < pkgs.length; i++) {
                AppInfo entry = mCache.getEntry(pkgs[i]);
                if (entry == null) {
                    if (localLOGV) Log.w(TAG, "Entry for package:"+ pkgs[i] +"doesn't exist in map");
                    continue;
                }
                if (entry.setSize(sizes[i], formatted[i])) {
                    changed = true;
                }
            }
            if (changed) {
                notifyDataSetChanged();
            }
        }

        public Filter getFilter() {
            return mAppFilter;
        }

        private class AppFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence prefix) {
                FilterResults results = new FilterResults();
                if (prefix == null || prefix.length() == 0) {
                    synchronized (mFilterLock) {
                        results.values = new HashMap<String, String>(mFilterMap);
                        results.count = mFilterMap.size();
                    }
                } else {
                    final String prefixString = prefix.toString().toLowerCase();
                    Map<String, String> newMap = new HashMap<String, String>();
                    synchronized (mFilterLock) {
                        Map<String, String> localMap = mFilterMap;
                        Set<String> keys = mFilterMap.keySet();
                        for (String key : keys) {
                            String label = localMap.get(key);
                            if (label.indexOf(prefixString) != -1) {
                                newMap.put(key, label);
                            }
                        }
                    }
                    results.values = newMap;
                    results.count = newMap.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mCurrentFilterMap = (Map<String, String>) results.values;
                reverseGenerateList();
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
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
        mHandler.removeMessages(COMPUTE_BULK_SIZE);
        mHandler.removeMessages(REMOVE_PKG);
        mHandler.removeMessages(REORDER_LIST);
        mHandler.removeMessages(ADD_PKG_START);
        mHandler.removeMessages(ADD_PKG_DONE);
        mHandler.removeMessages(REFRESH_LABELS);
        mHandler.removeMessages(REFRESH_DONE);
        mHandler.removeMessages(NEXT_LOAD_STEP);
        mHandler.removeMessages(COMPUTE_END);
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
        String pkgName;
        public void onGetStatsCompleted(PackageStats pStats, boolean pSucceeded) {
            if(DEBUG_PKG_DELAY) {
                try {
                    Thread.sleep(10*1000);
                } catch (InterruptedException e) {
                }
            }
            Bundle data = new Bundle();
            data.putString(ATTR_PKG_NAME, pkgName);
            data.putBoolean(ATTR_GET_SIZE_STATUS, pSucceeded);
            if(pSucceeded && pStats != null) {
                if (localLOGV) Log.i(TAG, "onGetStatsCompleted::"+pkgName+", ("+
                        pStats.cacheSize+","+
                        pStats.codeSize+", "+pStats.dataSize);
                long total = getTotalSize(pStats);
                data.putLong(ATTR_PKG_STATS, total);
                CharSequence sizeStr = getSizeStr(total);
                data.putString(ATTR_PKG_SIZE_STR, sizeStr.toString());
            } else {
                Log.w(TAG, "Invalid package stats from PackageManager");
            }
            // Post message to Handler
            Message msg = mHandler.obtainMessage(ADD_PKG_DONE, data);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }

        public void invokeGetSizeInfo(String packageName) {
            if (packageName == null) {
                return;
            }
            pkgName = packageName;
            if(localLOGV) Log.i(TAG, "Invoking getPackageSizeInfo for package:"+
                    packageName);
            mPm.getPackageSizeInfo(packageName, this);
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
        if(localLOGV) Log.i(TAG, "Activity created");
        long sCreate;
        if (DEBUG_TIME) {
            sCreate = SystemClock.elapsedRealtime();
        }
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_MANAGE_PACKAGE_STORAGE)) {
            mSortOrder = SORT_ORDER_SIZE;
            mFilterApps = FILTER_APPS_ALL;
            mSizesFirst = true;
        }
        mPm = getPackageManager();
        // initialize some window features
        requestWindowFeature(Window.FEATURE_RIGHT_ICON);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.compute_sizes);
        showLoadingMsg();
        mDefaultAppIcon =Resources.getSystem().getDrawable(
                com.android.internal.R.drawable.sym_def_app_icon);
        mInvalidSizeStr = getText(R.string.invalid_size_value);
        mComputingSizeStr = getText(R.string.computing_size);
        // initialize the inflater
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mReceiver = new PackageIntentReceiver();
        mObserver = new PkgSizeObserver();
        // Create adapter and list view here
        List<ApplicationInfo> appList = getInstalledApps(mSortOrder);
        mAppInfoAdapter = new AppInfoAdapter(this, appList);
        ListView lv= (ListView) findViewById(android.R.id.list);
        lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        lv.setOnItemClickListener(this);
        lv.setTextFilterEnabled(true);
        mListView = lv;
        if (DEBUG_TIME) {
            Log.i(TAG, "Total time in Activity.create:: " +
                    (SystemClock.elapsedRealtime() - sCreate)+ " ms");
        }
        // Get initial info from file for the very first time this activity started
        long sStart;
        if (DEBUG_TIME) {
            sStart = SystemClock.elapsedRealtime();
        }
        mCache.loadCache();
        if (DEBUG_TIME) {
            Log.i(TAG, "Took " + (SystemClock.elapsedRealtime()-sStart) + " ms to init cache");
        }
    }
    
    protected void onDestroy() {
        // Persist values in cache
        mCache.updateCache();
        super.onDestroy();
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
        if (DEBUG_TIME) {
            mLoadTimeStart = SystemClock.elapsedRealtime();
        }
        showDialog(DLG_LOADING); 
        if(localLOGV) Log.i(TAG, "Displaying Loading message");
    }
    
    private void dismissLoadingMsg() {
        if(localLOGV) Log.i(TAG, "Dismissing Loading message");
        dismissDialog(DLG_LOADING);
        if (DEBUG_TIME) Log.i(TAG, "Displayed loading message for "+
                (SystemClock.elapsedRealtime() - mLoadTimeStart) + " ms");
    }

    class AppInfoCache {
        final static boolean FILE_CACHE = true;
        private static final String mFileCacheName="ManageAppsInfo.txt";
        private static final int FILE_BUFFER_SIZE = 1024;
        private static final boolean DEBUG_CACHE = false;
        private static final boolean DEBUG_CACHE_TIME = false;
        private Map<String, AppInfo> mAppPropCache = new HashMap<String, AppInfo>();

        private boolean isEmpty() {
            return (mAppPropCache.size() == 0);
        }

        private AppInfo getEntry(String pkgName) {
            return mAppPropCache.get(pkgName);
        }

        private Set<String> getPkgList() {
            return mAppPropCache.keySet();
        }

        public void addEntry(AppInfo aInfo) {
            if ((aInfo != null) && (aInfo.pkgName != null)) {
                mAppPropCache.put(aInfo.pkgName, aInfo);
            }
        }

        public void removeEntry(String pkgName) {
            if (pkgName != null) {
                mAppPropCache.remove(pkgName);
            }
        }

        private void readFromFile() {
            File cacheFile = new File(getFilesDir(), mFileCacheName);
            if (!cacheFile.exists()) {
                return;
            }
            FileInputStream fis = null;
            boolean err = false;
            try {
                fis = new FileInputStream(cacheFile);
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Error opening file for read operation : " + cacheFile
                        + " with exception " + e);
                return;
            }
            try {
                byte[] byteBuff = new byte[FILE_BUFFER_SIZE];
                byte[] lenBytes = new byte[2];
                mAppPropCache.clear();
                while(fis.available() > 0) {
                    fis.read(lenBytes, 0, 2);
                    int buffLen = (lenBytes[0] << 8) | lenBytes[1];
                    if ((buffLen <= 0) || (buffLen > byteBuff.length)) {
                        err = true;
                        break;
                    }
                    // Buffer length cannot be great then max.
                    fis.read(byteBuff, 0, buffLen);
                    String buffStr = new String(byteBuff);
                    if (DEBUG_CACHE) {
                        Log.i(TAG, "Read string of len= " + buffLen + " :: " + buffStr + " from file");
                    }
                    // Parse string for sizes
                    String substrs[] = buffStr.split(",");
                    if (substrs.length < 4) {
                        // Something wrong. Bail out and let recomputation proceed.
                        err = true;
                        break;
                    }
                    long size = -1;
                    int idx = -1;
                    try {
                        size = Long.parseLong(substrs[1]);
                    } catch (NumberFormatException e) {
                        err = true;
                        break;
                    }
                    if (DEBUG_CACHE) {
                        Log.i(TAG, "Creating entry(" + substrs[0] + ", " + idx+"," + size + ", " + substrs[2] + ")");
                    }
                    AppInfo aInfo = new AppInfo(substrs[0], idx, substrs[3], size, substrs[2]);
                    mAppPropCache.put(aInfo.pkgName, aInfo);
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed reading from file : " + cacheFile + " with exception : " + e);
                err = true;
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Failed to close file " + cacheFile + " with exception : " +e);
                        err = true;
                    }
                }
                if (err) {
                    Log.i(TAG, "Failed to load cache. Not using cache for now.");
                    // Clear cache and bail out
                    mAppPropCache.clear();
                }
            }
        }

        boolean writeToFile() {
            File cacheFile = new File(getFilesDir(), mFileCacheName);
            FileOutputStream fos = null;
            try {
                long opStartTime = SystemClock.uptimeMillis();
                fos = new FileOutputStream(cacheFile);
                Set<String> keys = mAppPropCache.keySet();
                byte[] lenBytes = new byte[2];
                for (String key : keys) {
                    AppInfo aInfo = mAppPropCache.get(key);
                    StringBuilder buff = new StringBuilder(aInfo.pkgName);
                    buff.append(",");
                    buff.append(aInfo.size);
                    buff.append(",");
                    buff.append(aInfo.appSize);
                    buff.append(",");
                    buff.append(aInfo.appName);
                    if (DEBUG_CACHE) {
                        Log.i(TAG, "Writing str : " + buff.toString() + " to file of length:" +
                                buff.toString().length());
                    }
                    try {
                        byte[] byteBuff = buff.toString().getBytes();
                        int len = byteBuff.length;
                        if (byteBuff.length >= FILE_BUFFER_SIZE) {
                            // Truncate the output
                            len = FILE_BUFFER_SIZE;
                        }
                        // Use 2 bytes to write length
                        lenBytes[1] = (byte) (len & 0x00ff);
                        lenBytes[0] = (byte) ((len & 0x00ff00) >> 8);
                        fos.write(lenBytes, 0, 2);
                        fos.write(byteBuff, 0, len);
                    } catch (IOException e) {
                        Log.w(TAG, "Failed to write to file : " + cacheFile + " with exception : " + e);
                        return false;
                    }
                }
                if (DEBUG_CACHE_TIME) {
                    Log.i(TAG, "Took " + (SystemClock.uptimeMillis() - opStartTime) + " ms to write and process from file");
                }
                return true;
            } catch (FileNotFoundException e) {
                Log.w(TAG, "Error opening file for write operation : " + cacheFile+
                        " with exception : " + e);
                return false;
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Failed closing file : " + cacheFile + " with exception : " + e);
                        return false;
                    }
                }
            }
        }
        private void loadCache() {
             // Restore preferences
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            boolean disable = settings.getBoolean(PREF_DISABLE_CACHE, true);
            if (disable) Log.w(TAG, "Cache has been disabled");
            // Disable cache till the data is loaded successfully
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PREF_DISABLE_CACHE, true);
            editor.commit();
            if (FILE_CACHE && !disable) {
                readFromFile();
                // Enable cache since the file has been read successfully
                editor.putBoolean(PREF_DISABLE_CACHE, false);
                editor.commit();
            }
        }

        private void updateCache() {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PREF_DISABLE_CACHE, true);
            editor.commit();
            if (FILE_CACHE) {
                boolean writeStatus = writeToFile();
                mAppPropCache.clear();
                if (writeStatus) {
                    // Enable cache since the file has been read successfully
                    editor.putBoolean(PREF_DISABLE_CACHE, false);
                    editor.commit();
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register receiver
        mReceiver.registerReceiver();
        sendMessageToHandler(INIT_PKG_INFO);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop the background threads
        if (mResourceThread != null) {
            mResourceThread.setAbort();
        }
        if (mSizeComputor != null) {
            mSizeComputor.setAbort();
        }
        // clear all messages related to application list
        clearMessagesInHandler();
        // register receiver here
        unregisterReceiver(mReceiver);
    }
    
    // Avoid the restart and pause when orientation changes
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    /*
     * comparator class used to sort AppInfo objects based on size
     */
    class SizeComparator implements Comparator<ApplicationInfo> {
        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            AppInfo ainfo = mCache.getEntry(a.packageName);
            AppInfo binfo = mCache.getEntry(b.packageName);
            long atotal = ainfo.size;
            long btotal = binfo.size;
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
    }

    /*
     * Customized comparator class to compare labels.
     * Don't use the one defined in ApplicationInfo since that loads the labels again.
     */
    class AlphaComparator implements Comparator<ApplicationInfo> {
        private final Collator   sCollator = Collator.getInstance();

        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            AppInfo ainfo = mCache.getEntry(a.packageName);
            AppInfo binfo = mCache.getEntry(b.packageName);
            return sCollator.compare(ainfo.appName.toString(), binfo.appName.toString());
        }
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
            // Pick up the selection value from the list of added choice items.
            int selection = mFilterApps - MENU_OPTIONS_BASE;
            if (mAlertDlg == null) {
                mAlertDlg = new AlertDialog.Builder(this).
                        setTitle(R.string.filter_dlg_title).
                        setNeutralButton(R.string.cancel, this).
                        setSingleChoiceItems(new CharSequence[] {getText(R.string.filter_apps_all),
                                getText(R.string.filter_apps_running),
                                getText(R.string.filter_apps_third_party)},
                                selection, this).
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
        case FILTER_APPS_ALL:
            break;
        case FILTER_APPS_RUNNING:
            break;
        case FILTER_APPS_THIRD_PARTY:
            break;
        default:
            return;
        }
        newOption = which;
        mAlertDlg.dismiss();
        sendMessageToHandler(REORDER_LIST, newOption);
    }
}

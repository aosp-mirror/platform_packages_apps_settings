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
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Config;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Activity to pick an application that will be used to display installation information and
 * options to upgrade/uninstall/delete user data for system applications.
 *  Initially a compute in progress message is displayed while the application retrieves
 *  the size information of installed packages which is done asynchronously through a 
 *  handler. Once the computation is done package resource information is retrieved 
 *  and then the information is displayed on the screen. All 
 *  messages are passed through a Handler object.
 *  Known issue: There could be some ordering issues when installing/uninstalling
 *  applications when the application list is being scanned.
 */
public class ManageApplications extends Activity implements SimpleAdapter.ViewBinder, OnItemClickListener {
    private static final String TAG = "ManageApplications";
    //Application prefix information
    public static final String APP_PKG_PREFIX="com.android.settings.";
    public static final String APP_PKG_NAME=APP_PKG_PREFIX+"ApplicationPkgName";
    public static final String APP_PKG_SIZE= APP_PKG_PREFIX+"size";
    public static final String APP_CHG=APP_PKG_PREFIX+"changed";
    
    //constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;
     //application attributes passed to sub activity that displays more app info
    private static final String KEY_APP_NAME = "ApplicationName";
    private static final String KEY_APP_ICON = "ApplicationIcon";
    private static final String KEY_APP_DESC = "ApplicationDescription";
    private static final String KEY_APP_SIZE= "ApplicationSize";
    //sort order that can be changed through the menu
    public static final int SORT_ORDER_ALPHA = 0;
    public static final int SORT_ORDER_SIZE = 1;
   //key and resource values used in constructing map for SimpleAdapter
    private static final String sKeys[] = new String[] { KEY_APP_NAME, KEY_APP_ICON, 
            KEY_APP_DESC, KEY_APP_SIZE};
    private static final int sResourceIds[] = new int[] { R.id.app_name, R.id.app_icon, 
            R.id.app_description, R.id.app_size};
    //List of ApplicationInfo objects for various applications
    private List<ApplicationInfo> mAppList;
    //SimpleAdapter used for managing items in the list
    private SimpleAdapter mAppAdapter;
    //map used to store size information which is used for displaying size information
    //in this activity as well as the subactivity. this is to avoid invoking package manager
    //api to retrieve size information
    private HashMap<String, PackageStats> mSizeMap;
    private HashMap<String, Map<String, ?> > mAppAdapterMap;
    //sort order
    private int mSortOrder = SORT_ORDER_ALPHA;
    //log information boolean
    private boolean localLOGV = Config.LOGV || false;
    private ApplicationInfo mCurrentPkg;
    private int mCurrentPkgIdx = 0;
    private static final int COMPUTE_PKG_SIZE_START = 1;
    private static final int COMPUTE_PKG_SIZE_DONE = 2;
    private static final int REMOVE_PKG=3;
    private static final int REORDER_LIST=4;
    private static final int ADD_PKG=5;
    private static final String ATTR_APP_IDX="ApplicationIndex";
    private static final String ATTR_CHAINED="Chained";
    private static final String ATTR_PKG_NAME="PackageName";
    private PkgSizeObserver mObserver;
    private PackageManager mPm;
    private PackageIntentReceiver mReceiver;
    private boolean mDoneIniting = false;
    private String mKbStr;
    private String  mMbStr;
    private String mBStr;
    
    /*
     * Handler class to handle messages for various operations
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            PackageStats ps;
            ApplicationInfo info;
            Bundle data;
            String pkgName;
            int idx;
            int size;
            boolean chained = false;
            data = msg.getData();
            switch (msg.what) {
            case COMPUTE_PKG_SIZE_START:
                mDoneIniting = false;
                //initialize lists
                mAppList = new ArrayList<ApplicationInfo>();
                mSizeMap = new HashMap<String, PackageStats>();
                mAppAdapterMap = new HashMap<String, Map<String, ?> >();
                //update application list from PackageManager
                mAppList = mPm.getInstalledApplications(0);
                if(mAppList.size() == 0) {
                    return;
                }
                mCurrentPkgIdx = 0;
                mCurrentPkg = mAppList.get(0);
                if(localLOGV) Log.i(TAG, "Initiating compute sizes for first time");
                //register receiver
                mReceiver = new PackageIntentReceiver();
                mReceiver.registerReceiver();
                pkgName = mCurrentPkg.packageName;
                mObserver = new PkgSizeObserver(0);
                mObserver.invokeGetSizeInfo(pkgName, true);
                break;
            case COMPUTE_PKG_SIZE_DONE:
                ps = mObserver.ps;
                info = mObserver.appInfo;
                chained = data.getBoolean(ATTR_CHAINED);
                if(!mObserver.succeeded) {
                    if(chained) {
                        removePackageFromAppList(ps.packageName);
                    } else {
                        //do not go to adding phase
                        break;
                    }
                } else {
                    //insert size value
                    mSizeMap.put(ps.packageName, ps);
                    Map<String, Object> entry = createMapEntry(mPm.getApplicationLabel(info), 
                            mPm.getApplicationIcon(info), 
                            info.loadDescription(mPm), 
                            getSizeStr(ps));
                    mAppAdapterMap.put(ps.packageName, entry);
                }
                if(chained) {
                    //here app list is precomputed
                    idx = data.getInt(ATTR_APP_IDX);
                    //increment only if succeded
                    if(mObserver.succeeded) {
                        idx++;
                    }
                    if(idx <  mAppList.size()) {
                        pkgName = mAppList.get(idx).packageName;
                        //increment record index and invoke getSizeInfo for next record
                        mObserver.invokeGetSizeInfo(pkgName, true);
                    } else {
                        sortAppList();
                        createListFromValues();
                        mDoneIniting = true;
                    }
                } else {
                    //add app info object as well
                    mAppList.add(info);
                    sortAppList();
                    size = mAppList.size();
                    int i;
                    for(i = 0; i < size; i++) {
                        if(mAppList.get(i).packageName.equalsIgnoreCase(mCurrentPkg.packageName)) {
                            if(i > mCurrentPkgIdx) {
                                mCurrentPkgIdx = i;
                            }
                            break;
                        }
                    }
                    createListFromValues();
                }
                break;
            case REMOVE_PKG:
                if(!mDoneIniting) {
                    //insert message again after some delay
                    sendMessageToHandler(REMOVE_PKG, data, 10*1000);
                    break;
                }
                pkgName = data.getString(ATTR_PKG_NAME);
                removePackageFromAppList(pkgName);
                if(mSizeMap.remove(pkgName) == null) {
                    Log.i(TAG, "Coudnt remove from size map package:"+pkgName);
                }
                if(mAppAdapterMap.remove(pkgName) == null) {
                    Log.i(TAG, "Coudnt remove from app adapter map package:"+pkgName);
                }
                if(mCurrentPkg.packageName.equalsIgnoreCase(pkgName)) {
                    if(mCurrentPkgIdx == (mAppList.size()-1)) {
                        mCurrentPkgIdx--;
                    }
                    mCurrentPkg = mAppList.get(mCurrentPkgIdx);
                }
                createListFromValues();
                break;
            case REORDER_LIST:
                int sortOrder = msg.arg1;
                if(sortOrder != mSortOrder) {
                    mSortOrder = sortOrder;
                    if(localLOGV) Log.i(TAG, "Changing sort order to "+mSortOrder);
                    sortAppList();
                    mCurrentPkgIdx  = 0;
                    mCurrentPkg = mAppList.get(mCurrentPkgIdx);
                    createListFromValues();
                }
                break;
            case ADD_PKG:
                pkgName = data.getString(ATTR_PKG_NAME);
                if(!mDoneIniting) {
                   //insert message again after some delay
                    sendMessageToHandler(ADD_PKG, data, 10*1000);
                    break;
                }
                mObserver.invokeGetSizeInfo(pkgName, false);
                break;
            default:
                break;
            }
        }
    };
    
    private void removePackageFromAppList(String pkgName) {
        int size = mAppList.size();
        for(int i = 0; i < size; i++) {
            if(mAppList.get(i).packageName.equalsIgnoreCase(pkgName)) {
                mAppList.remove(i);
                break;
            }
        }
    }
    
    private void clearMessages() {
        synchronized(mHandler) {
            mHandler.removeMessages(COMPUTE_PKG_SIZE_START);
            mHandler.removeMessages(COMPUTE_PKG_SIZE_DONE);
            mHandler.removeMessages(REMOVE_PKG);
            mHandler.removeMessages(REORDER_LIST);
            mHandler.removeMessages(ADD_PKG);
        }
    }
    
    private void sendMessageToHandler(int msgId, Bundle data, long delayMillis) {
        synchronized(mHandler) {
            Message msg = mHandler.obtainMessage(msgId);
            msg.setData(data);
            if(delayMillis == 0) {
                mHandler.sendMessage(msg);
            } else {
                mHandler.sendMessageDelayed(msg, delayMillis);
            }
        }
    }
    
    private void sendMessageToHandler(int msgId, int arg1) {
        synchronized(mHandler) {
            Message msg = mHandler.obtainMessage(msgId);
            msg.arg1 = arg1;
            mHandler.sendMessage(msg);
        }
    }
    
    private void sendMessageToHandler(int msgId) {
        synchronized(mHandler) {
            mHandler.sendEmptyMessage(msgId);
        }
    }
    
    class PkgSizeObserver extends IPackageStatsObserver.Stub {
        public PackageStats ps;
        public ApplicationInfo appInfo;
        public Drawable appIcon;
        public CharSequence appName;
        public CharSequence appDesc = "";
        private int mIdx = 0;
        private boolean mChained = false;
        public boolean succeeded;
        PkgSizeObserver(int i) {
            mIdx = i;
        }
        
        private void getAppDetails() {
            try {
                appInfo = mPm.getApplicationInfo(ps.packageName, 0);
            } catch (NameNotFoundException e) {
                return;
            }
            appName = appInfo.loadLabel(mPm);
            appIcon = appInfo.loadIcon(mPm);
        }
        
        public void onGetStatsCompleted(PackageStats pStats, boolean pSucceeded) {
            Bundle data = new Bundle();
            ps = pStats;
            succeeded = pSucceeded;
            if(mChained) {
                data.putInt(ATTR_APP_IDX, mIdx);
                if(succeeded) {
                    mIdx++;
                }
            }
            data.putBoolean(ATTR_CHAINED, mChained);
            getAppDetails();
            if(localLOGV) Log.i(TAG, "onGetStatsCompleted::"+appInfo.packageName+", ("+ps.cacheSize+","+
                    ps.codeSize+", "+ps.dataSize);
            sendMessageToHandler(COMPUTE_PKG_SIZE_DONE, data, 0);
        }
        
        public void invokeGetSizeInfo(String packageName, boolean chained) {
             mChained = chained;
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
            if(localLOGV) Log.i(TAG, "action:"+actionStr+", for package:"+pkgName);
            updatePackageList(actionStr, pkgName);
        }
    }
    
    private void updatePackageList(String actionStr, String pkgName) {
        //technically we dont have to invoke handler since onReceive is invoked on
        //the main thread but doing it here for better clarity
        if(Intent.ACTION_PACKAGE_ADDED.equalsIgnoreCase(actionStr)) {
            Bundle data = new Bundle();
            data.putString(ATTR_PKG_NAME, pkgName);
            sendMessageToHandler(ADD_PKG, data, 0);
        } else if(Intent.ACTION_PACKAGE_REMOVED.equalsIgnoreCase(actionStr)) {
            Bundle data = new Bundle();
            data.putString(ATTR_PKG_NAME, pkgName);
            sendMessageToHandler(REMOVE_PKG, data, 0);
        } else if(Intent.ACTION_PACKAGE_CHANGED.equalsIgnoreCase(actionStr)) {
            //force adapter to draw the list again. TODO derive from SimpleAdapter
            //to avoid this
           
        }   
    }
    
    /*
     * Utility method to create an array of map objects from a map of map objects
     *  for displaying list items to be used in SimpleAdapter.
     */
    private void createListFromValues() {
        findViewById(R.id.center_text).setVisibility(View.GONE);
        populateAdapterList();
        mAppAdapter.setViewBinder(this);
        ListView lv= (ListView) findViewById(android.R.id.list);
        lv.setOnItemClickListener(this);
        lv.setAdapter(mAppAdapter);
        if(mCurrentPkgIdx != -1) {
            lv.setSelection(mCurrentPkgIdx);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String action = getIntent().getAction();
        if(action.equals(Intent.ACTION_MANAGE_PACKAGE_STORAGE)) {
            mSortOrder = SORT_ORDER_SIZE;
        }
        mPm = getPackageManager();
        //load strings from resources
        mBStr = getString(R.string.b_text);
        mKbStr = getString(R.string.kb_text);
        mMbStr = getString(R.string.mb_text);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        setContentView(R.layout.compute_sizes);
        //clear all messages related to application list
        clearMessages();
        sendMessageToHandler(COMPUTE_PKG_SIZE_START);
    }

    @Override
    public void onStop() {
        super.onStop();
        //register receiver here
        unregisterReceiver(mReceiver);        
    }
    
    public static class AppInfoComparator implements Comparator<ApplicationInfo> {
        public AppInfoComparator(HashMap<String, PackageStats> pSizeMap) {
            mSizeMap= pSizeMap;
        }

        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            PackageStats aps, bps;
            aps = mSizeMap.get(a.packageName);
            bps = mSizeMap.get(b.packageName);
            if (aps == null && bps == null) {
                return 0;
            } else if (aps == null) {
                return 1;
            } else if (bps == null) {
                return -1;
            }
            long atotal = aps.dataSize+aps.codeSize+aps.cacheSize;
            long btotal = bps.dataSize+bps.codeSize+bps.cacheSize;
            long ret = atotal-btotal;
            //negate result to sort in descending order
            if(ret < 0) {
                return 1;
            }
            if(ret == 0) {
                return 0;
            }
            return -1;
        }
        private HashMap<String, PackageStats> mSizeMap;
    }

    /*
     * Have to extract elements form map and populate a list ot be used by
     * SimpleAdapter when displaying list elements. The sort order has to follow
     * the order of elements in mAppList.
     */
     private List<Map<String, ?>> createAdapterListFromMap() {
         //get the index from mAppInfo which gives the correct sort position
         int imax = mAppList.size();
         if(localLOGV) Log.i(TAG, "Creating new adapter list");
         List<Map<String, ?>> adapterList = new ArrayList<Map<String, ?>>();
         ApplicationInfo tmpInfo;
         for(int i = 0; i < imax; i++) {
             tmpInfo = mAppList.get(i);
             Map<String, Object>newObj = new TreeMap<String, Object>(
                     mAppAdapterMap.get(tmpInfo.packageName));
             adapterList.add(newObj);
         }
         return adapterList;
     }
     
    private void populateAdapterList() {
        mAppAdapter = new SimpleAdapter(this, createAdapterListFromMap(),
                    R.layout.manage_applications_item, sKeys, sResourceIds);
    }
    
    private String getSizeStr(PackageStats ps) {
        String retStr = "";
        //insert total size information into map to display in view
        //at this point its guaranteed that ps is not null. but checking anyway
        if(ps != null) {
            long size = ps.cacheSize+ps.codeSize+ps.dataSize;
            if(size < 1024) {
                return String.valueOf(size)+mBStr;
            }
            long kb, mb, rem;
            kb = size >> 10;
            rem = size - (kb << 10);
            if(kb < 1024) {
                if(rem > 512) {
                    kb++;
                }
                retStr += String.valueOf(kb)+mKbStr;
                return retStr;
            }
            mb = kb >> 10;
            if(kb >= 512) {
                //round off
                mb++;
            }
            retStr += String.valueOf(mb)+ mMbStr;
            return retStr;
        } else {
            Log.w(TAG, "Something fishy, cannot find size info for package:"+ps.packageName);
        }
        return retStr;
    }
    
    public void sortAppList() {
        // Sort application list
        if(mSortOrder == SORT_ORDER_ALPHA) {
            Collections.sort(mAppList, new ApplicationInfo.DisplayNameComparator(mPm));
        } else if(mSortOrder == SORT_ORDER_SIZE) {
            Collections.sort(mAppList, new AppInfoComparator(mSizeMap));
        }
    }
    
    private Map<String, Object> createMapEntry(CharSequence appName, 
            Drawable appIcon, CharSequence appDesc, String sizeStr) {
        Map<String, Object> map = new TreeMap<String, Object>();
        map.put(KEY_APP_NAME, appName);
        //the icon cannot be null. if the application hasnt set it, the default icon is returned.
        map.put(KEY_APP_ICON, appIcon);
        if(appDesc == null) {
            appDesc="";
        }
        map.put(KEY_APP_DESC, appDesc);
        map.put(KEY_APP_SIZE, sizeStr);
        return map;
    }
    
    private void startApplicationDetailsActivity(ApplicationInfo info, PackageStats ps) {
        //Create intent to start new activity
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, InstalledAppDetails.class);
        intent.putExtra(APP_PKG_NAME, info.packageName);
        if(localLOGV) Log.i(TAG, "code="+ps.codeSize+", cache="+ps.cacheSize+", data="+ps.dataSize);
        intent.putExtra(APP_PKG_SIZE,  ps);
        if(localLOGV) Log.i(TAG, "Starting sub activity to display info for app:"+info
                +" with intent:"+intent);
        //start new activity to display extended information
        if ((info.flags&ApplicationInfo.FLAG_SYSTEM) != 0) {
        }
        startActivityForResult(intent, INSTALLED_APP_DETAILS);
    }
    
    public boolean setViewValue(View view, Object data, String textRepresentation) {
        if(data == null) {
            return false;
        }
        int id = view.getId();
        switch(id) {
        case R.id.app_name:
            ((TextView)view).setText((String)data);
            break;
        case R.id.app_icon:
            ((ImageView)view).setImageDrawable((Drawable)data);
            break;
        case R.id.app_description:
            ((TextView)view).setText((String)data);
            break;
        case R.id.app_size:
            ((TextView)view).setText((String)data);
            break;
        default:
                break;
        }
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, SORT_ORDER_ALPHA, 0, R.string.sort_order_alpha)
                .setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        menu.add(0, SORT_ORDER_SIZE, 0, R.string.sort_order_size)
                .setIcon(android.R.drawable.ic_menu_sort_by_size);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(mDoneIniting) {
            menu.findItem(SORT_ORDER_ALPHA).setVisible(mSortOrder != SORT_ORDER_ALPHA);
            menu.findItem(SORT_ORDER_SIZE).setVisible(mSortOrder!= SORT_ORDER_SIZE);
            return true;
        } 
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        sendMessageToHandler(REORDER_LIST, menuId);
        return true;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        mCurrentPkgIdx=position;
        ApplicationInfo info = mAppList.get(position);
        mCurrentPkg = info;        
        PackageStats ps = mSizeMap.get(info.packageName);
        startApplicationDetailsActivity(info, ps);
    }
}

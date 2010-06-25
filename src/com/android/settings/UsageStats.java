

/**
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings;

import com.android.internal.app.IUsageStats;
import com.android.settings.R;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.os.PkgUsageStats;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Activity to display package usage statistics.
 */
public class UsageStats extends Activity implements OnItemSelectedListener {
    private static final String TAG="UsageStatsActivity";
    private static final boolean localLOGV = false;
    private Spinner mTypeSpinner;
    private ListView mListView;
    private IUsageStats mUsageStatsService;
    private LayoutInflater mInflater;
    private UsageStatsAdapter mAdapter;
    private PackageManager mPm;
    
    public static class AppNameComparator implements Comparator<PkgUsageStats> {
        Map<String, CharSequence> mAppLabelList;
        AppNameComparator(Map<String, CharSequence> appList) {
            mAppLabelList = appList;
        }
        public final int compare(PkgUsageStats a, PkgUsageStats b) {
            String alabel = mAppLabelList.get(a.packageName).toString();
            String blabel = mAppLabelList.get(b.packageName).toString();
            return alabel.compareTo(blabel);
        }
    }
    
    public static class LaunchCountComparator implements Comparator<PkgUsageStats> {
        public final int compare(PkgUsageStats a, PkgUsageStats b) {
            // return by descending order
            return b.launchCount - a.launchCount;
        }
    }
    
    public static class UsageTimeComparator implements Comparator<PkgUsageStats> {
        public final int compare(PkgUsageStats a, PkgUsageStats b) {
            long ret = a.usageTime-b.usageTime;
            if (ret == 0) {
                return 0;
            }
            if (ret < 0) {
                return 1;
            }
            return -1;
        }
    }
    
     // View Holder used when displaying views
    static class AppViewHolder {
        TextView pkgName;
        TextView launchCount;
        TextView usageTime;
    }
    
    class UsageStatsAdapter extends BaseAdapter {
         // Constants defining order for display order
        private static final int _DISPLAY_ORDER_USAGE_TIME = 0;
        private static final int _DISPLAY_ORDER_LAUNCH_COUNT = 1;
        private static final int _DISPLAY_ORDER_APP_NAME = 2;
        
        private int mDisplayOrder = _DISPLAY_ORDER_USAGE_TIME;
        private List<PkgUsageStats> mUsageStats;
        private LaunchCountComparator mLaunchCountComparator;
        private UsageTimeComparator mUsageTimeComparator;
        private AppNameComparator mAppLabelComparator;
        private HashMap<String, CharSequence> mAppLabelMap;
        
        UsageStatsAdapter() {
            mUsageStats = new ArrayList<PkgUsageStats>();
            mAppLabelMap = new HashMap<String, CharSequence>();
            PkgUsageStats[] stats;
            try {
                stats = mUsageStatsService.getAllPkgUsageStats();
            } catch (RemoteException e) {
                Log.e(TAG, "Failed initializing usage stats service");
                return;
            }
           if (stats == null) {
               return;
           }
           for (PkgUsageStats ps : stats) {
               mUsageStats.add(ps);
               // load application labels for each application
               CharSequence label;
               try {
                   ApplicationInfo appInfo = mPm.getApplicationInfo(ps.packageName, 0);
                   label = appInfo.loadLabel(mPm);
                } catch (NameNotFoundException e) {
                    label = ps.packageName;
                }
                mAppLabelMap.put(ps.packageName, label);
           }
           // Sort list
           mLaunchCountComparator = new LaunchCountComparator();
           mUsageTimeComparator = new UsageTimeComparator();
           mAppLabelComparator = new AppNameComparator(mAppLabelMap);
           sortList();
        }
        public int getCount() {
            return mUsageStats.size();
        }

        public Object getItem(int position) {
            return mUsageStats.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unneccessary calls
            // to findViewById() on each row.
            AppViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied
            // by ListView is null.
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.usage_stats_item, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new AppViewHolder();
                holder.pkgName = (TextView) convertView.findViewById(R.id.package_name);
                holder.launchCount = (TextView) convertView.findViewById(R.id.launch_count);
                holder.usageTime = (TextView) convertView.findViewById(R.id.usage_time);
                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (AppViewHolder) convertView.getTag();
            }

            // Bind the data efficiently with the holder
            PkgUsageStats pkgStats = mUsageStats.get(position);
            if (pkgStats != null) {
                CharSequence label = mAppLabelMap.get(pkgStats.packageName);
                holder.pkgName.setText(label);
                holder.launchCount.setText(String.valueOf(pkgStats.launchCount));
                holder.usageTime.setText(String.valueOf(pkgStats.usageTime)+" ms");
            } else {
                Log.w(TAG, "No usage stats info for package:" + position);
            }
            return convertView;
        }
        
        void sortList(int sortOrder) {
            if (mDisplayOrder == sortOrder) {
                // do nothing
                return;
            }
            mDisplayOrder= sortOrder;
            sortList();
        }
        private void sortList() {
            if (mDisplayOrder == _DISPLAY_ORDER_USAGE_TIME) {
                if (localLOGV) Log.i(TAG, "Sorting by usage time");
                Collections.sort(mUsageStats, mUsageTimeComparator);
            } else if (mDisplayOrder == _DISPLAY_ORDER_LAUNCH_COUNT) {
                if (localLOGV) Log.i(TAG, "Sorting launch count");
                Collections.sort(mUsageStats, mLaunchCountComparator);
            } else if (mDisplayOrder == _DISPLAY_ORDER_APP_NAME) {
                if (localLOGV) Log.i(TAG, "Sorting by application name");
                Collections.sort(mUsageStats, mAppLabelComparator);
            }
            notifyDataSetChanged();
        }
    }

    /** Called when the activity is first created. */
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mUsageStatsService = IUsageStats.Stub.asInterface(ServiceManager.getService("usagestats"));
        if (mUsageStatsService == null) {
            Log.e(TAG, "Failed to retrieve usagestats service");
            return;
        }
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPm = getPackageManager();
        
        setContentView(R.layout.usage_stats);
        mTypeSpinner = (Spinner) findViewById(R.id.typeSpinner);
        mTypeSpinner.setOnItemSelectedListener(this);
        
        mListView = (ListView) findViewById(R.id.pkg_list);
        // Initialize the inflater
        
        mAdapter = new UsageStatsAdapter();
        mListView.setAdapter(mAdapter);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        mAdapter.sortList(position);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }
}


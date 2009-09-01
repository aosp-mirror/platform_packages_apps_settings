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
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Config;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class RunningServices extends ListActivity
        implements AbsListView.RecyclerListener,
        DialogInterface.OnClickListener {
    static final String TAG = "RunningServices";
    
    /** Maximum number of services to retrieve */
    static final int MAX_SERVICES = 100;
    
    static final int MSG_UPDATE_TIMES = 1;
    static final int MSG_UPDATE_CONTENTS = 2;
    
    static final long TIME_UPDATE_DELAY = 1000;
    static final long CONTENTS_UPDATE_DELAY = 2000;
    
    final HashMap<View, ActiveItem> mActiveItems = new HashMap<View, ActiveItem>();
    
    ActivityManager mAm;
    
    State mState;
    
    StringBuilder mBuilder = new StringBuilder(128);
    
    BaseItem mCurSelected;
    
    int mProcessBgColor;
    
    Dialog mCurDialog;
    
    class ActiveItem {
        View mRootView;
        BaseItem mItem;
        ActivityManager.RunningServiceInfo mService;
        ViewHolder mHolder;
        long mFirstRunTime;
        
        void updateTime(Context context) {
            if (mItem.mActiveSince >= 0) {
                mHolder.size.setText(DateUtils.formatElapsedTime(mBuilder,
                        (SystemClock.uptimeMillis()-mFirstRunTime)/1000));
            } else {
                mHolder.size.setText(context.getResources().getText(
                        R.string.service_restarting));
            }
        }
    }
    
    static class BaseItem {
        boolean mIsProcess;
        PackageItemInfo mPackageInfo;
        CharSequence mDisplayLabel;
        String mLabel;
        String mName;
        
        int mCurSeq;
        
        long mActiveSince;
        long mSize;
        String mSizeStr;
        boolean mNeedDivider;
    }
    
    static class ServiceItem extends BaseItem {
        ActivityManager.RunningServiceInfo mRunningService;
        ServiceInfo mServiceInfo;
    }
    
    static class ProcessItem extends BaseItem {
        final HashMap<ComponentName, ServiceItem> mServices
                = new HashMap<ComponentName, ServiceItem>();
        int mUid;
        int mPid;
        
        boolean updateService(PackageManager pm,
                ActivityManager.RunningServiceInfo service) {
            boolean changed = false;
            ServiceItem si = mServices.get(service.service);
            if (si == null) {
                changed = true;
                si = new ServiceItem();
                si.mRunningService = service;
                try {
                    si.mServiceInfo = pm.getServiceInfo(service.service, 0);
                } catch (PackageManager.NameNotFoundException e) {
                }
                if (si.mServiceInfo != null && (si.mServiceInfo.labelRes != 0
                        || si.mServiceInfo.nonLocalizedLabel != null)) {
                    si.mDisplayLabel = si.mServiceInfo.loadLabel(pm);
                    si.mLabel = si.mDisplayLabel.toString();
                } else {
                    si.mLabel = si.mRunningService.service.getClassName();
                    int tail = si.mLabel.lastIndexOf('.');
                    if (tail >= 0) {
                        si.mLabel = si.mLabel.substring(tail+1, si.mLabel.length());
                    }
                    si.mDisplayLabel = si.mLabel;
                }
                si.mPackageInfo = si.mServiceInfo;
                mServices.put(service.service, si);
            }
            si.mCurSeq = mCurSeq;
            si.mRunningService = service;
            long activeSince = service.restarting == 0 ? service.activeSince : -1;
            if (si.mActiveSince != activeSince) {
                si.mActiveSince = activeSince;
                changed = true;
            }
            
            return changed;
        }
    }
    
    static class State {
        final SparseArray<HashMap<String, ProcessItem>> mProcesses
                = new SparseArray<HashMap<String, ProcessItem>>();
        
        final ArrayList<BaseItem> mItems = new ArrayList<BaseItem>();
        
        int mSequence = 0;
        
        boolean update(Context context, ActivityManager am) {
            final PackageManager pm = context.getPackageManager();
            
            mSequence++;
            
            boolean changed = false;
            
            List<ActivityManager.RunningServiceInfo> services 
                    = am.getRunningServices(MAX_SERVICES);
            if (services == null) {
                return false;
            }
            final int NS = services.size();
            for (int i=0; i<NS; i++) {
                ActivityManager.RunningServiceInfo si = services.get(i);
                // We are not interested in non-started services, because
                // there is nothing the user can do about them.
                if (!si.started) {
                    continue;
                }
                // We likewise don't care about services running in a
                // persistent process like the system or phone.
                if ((si.flags&ActivityManager.RunningServiceInfo.FLAG_PERSISTENT_PROCESS)
                        != 0) {
                    continue;
                }
                
                HashMap<String, ProcessItem> procs = mProcesses.get(si.uid);
                if (procs == null) {
                    procs = new HashMap<String, ProcessItem>();
                    mProcesses.put(si.uid, procs);
                }
                ProcessItem proc = procs.get(si.process);
                if (proc == null) {
                    changed = true;
                    proc = new ProcessItem();
                    proc.mIsProcess = true;
                    proc.mName = si.process;
                    proc.mUid = si.uid;
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(si.process, 0);
                        if (ai.uid == si.uid) {
                            proc.mDisplayLabel = ai.loadLabel(context.getPackageManager());
                            proc.mLabel = proc.mDisplayLabel.toString();
                            proc.mPackageInfo = ai;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                    procs.put(si.process, proc);
                }
                
                if (proc.mCurSeq != mSequence) {
                    int pid = si.restarting == 0 ? si.pid : 0;
                    if (pid != proc.mPid) {
                        changed = true;
                        proc.mPid = pid;
                    }
                    proc.mSize = 0;
                    if (proc.mPid != 0) {
                        Debug.MemoryInfo mi = new Debug.MemoryInfo();
                        // XXX This is a hack...  I really don't want to be
                        // doing a synchronous call into the app, but can't
                        // figure out any other way to get the pss.
                        try {
                            ActivityManagerNative.getDefault().getProcessMemoryInfo(
                                    proc.mPid, mi);
                            proc.mSize = (mi.dalvikPss + mi.nativePss
                                    + mi.otherPss) * 1024;
                            String sizeStr = Formatter.formatFileSize(
                                    context, proc.mSize);
                            if (!sizeStr.equals(proc.mSizeStr)){
                                changed = true;
                                proc.mSizeStr = sizeStr;
                            }
                        } catch (RemoteException e) {
                        }
                    }
                    proc.mCurSeq = mSequence;
                }
                changed |= proc.updateService(context.getPackageManager(), si);
                
                if (proc.mLabel == null) {
                    // If we couldn't get information about the overall
                    // process, try to find something about the uid.
                    String[] pkgs = pm.getPackagesForUid(proc.mUid);
                    for (String name : pkgs) {
                        try {
                            PackageInfo pi = pm.getPackageInfo(name, 0);
                            if (pi.sharedUserLabel != 0) {
                                CharSequence nm = pm.getText(name,
                                        pi.sharedUserLabel, pi.applicationInfo);
                                if (nm != null) {
                                    proc.mDisplayLabel = nm;
                                    proc.mLabel = nm.toString();
                                    proc.mPackageInfo = pi.applicationInfo;
                                    break;
                                }
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                    }
                    
                    // If still don't have anything to display, just use the
                    // service info.
                    if (proc.mLabel == null) {
                        proc.mPackageInfo = proc.mServices.get(si.service)
                                .mServiceInfo.applicationInfo;
                        proc.mDisplayLabel = proc.mPackageInfo.loadLabel(pm);
                        proc.mLabel = proc.mDisplayLabel.toString();
                    }
                }
            }
            
            // Look for anything that no longer exists...
            for (int i=0; i<mProcesses.size(); i++) {
                HashMap<String, ProcessItem> procs = mProcesses.valueAt(i);
                Iterator<ProcessItem> pit = procs.values().iterator();
                while (pit.hasNext()) {
                    ProcessItem pi = pit.next();
                    if (pi.mCurSeq != mSequence) {
                        changed = true;
                        pit.remove();
                        if (procs.size() == 0) {
                            mProcesses.remove(mProcesses.keyAt(i));
                        }
                        continue;
                    }
                    Iterator<ServiceItem> sit = pi.mServices.values().iterator();
                    while (sit.hasNext()) {
                        ServiceItem si = sit.next();
                        if (si.mCurSeq != mSequence) {
                            changed = true;
                            sit.remove();
                        }
                    }
                }
            }
            
            if (changed) {
                mItems.clear();
                for (int i=0; i<mProcesses.size(); i++) {
                    for (ProcessItem pi : mProcesses.valueAt(i).values()) {
                        pi.mNeedDivider = false;
                        mItems.add(pi);
                        boolean needDivider = false;
                        for (ServiceItem si : pi.mServices.values()) {
                            si.mNeedDivider = needDivider;
                            needDivider = true;
                            mItems.add(si);
                        }
                    }
                }
            }
            
            return changed;
        }
    }
    
    static class TimeTicker extends TextView {
        public TimeTicker(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }
    
    static class ViewHolder {
        ImageView separator;
        ImageView icon;
        TextView name;
        TextView runTime;
        TextView size;
    }
    
    class ServiceListAdapter extends BaseAdapter {
        final State mState;
        final LayoutInflater mInflater;
        
        ServiceListAdapter(State state) {
            mState = state;
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public boolean hasStableIds() {
            return true;
        }
        
        public int getCount() {
            return mState.mItems.size();
        }

        public Object getItem(int position) {
            return mState.mItems.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return !mState.mItems.get(position).mIsProcess;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent);
            } else {
                v = convertView;
            }
            bindView(v, position);
            return v;
        }
        
        public View newView(ViewGroup parent) {
            View v = mInflater.inflate(R.layout.running_services_item, parent, false);
            ViewHolder h = new ViewHolder();
            h.separator = (ImageView)v.findViewById(R.id.separator);
            h.icon = (ImageView)v.findViewById(R.id.icon);
            h.name = (TextView)v.findViewById(R.id.name);
            h.runTime = (TextView)v.findViewById(R.id.run_time);
            h.size = (TextView)v.findViewById(R.id.size);
            v.setTag(h);
            return v;
        }
        
        public void bindView(View view, int position) {
            ViewHolder vh = (ViewHolder) view.getTag();
            BaseItem item = mState.mItems.get(position);
            vh.name.setText(item.mDisplayLabel);
            vh.separator.setVisibility(item.mNeedDivider
                    ? View.VISIBLE : View.INVISIBLE);
            if (item.mIsProcess) {
                view.setBackgroundColor(mProcessBgColor);
                vh.icon.setImageDrawable(item.mPackageInfo.loadIcon(getPackageManager()));
                vh.runTime.setText(item.mName);
                vh.size.setText(item.mSizeStr);
                mActiveItems.remove(view);
            } else {
                view.setBackgroundDrawable(null);
                vh.icon.setImageDrawable(null);
                vh.runTime.setText("");
                ActiveItem ai = new ActiveItem();
                ai.mRootView = view;
                ai.mItem = item;
                ai.mHolder = vh;
                ai.mFirstRunTime = item.mActiveSince;
                ai.updateTime(RunningServices.this);
                mActiveItems.put(view, ai);
            }
        }
    }
    
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_TIMES:
                    for (ActiveItem ai : mActiveItems.values()) {
                        ai.updateTime(RunningServices.this);
                    }
                    removeMessages(MSG_UPDATE_TIMES);
                    msg = obtainMessage(MSG_UPDATE_TIMES);
                    sendMessageDelayed(msg, TIME_UPDATE_DELAY);
                    break;
                case MSG_UPDATE_CONTENTS:
                    updateList();
                    removeMessages(MSG_UPDATE_CONTENTS);
                    msg = obtainMessage(MSG_UPDATE_CONTENTS);
                    sendMessageDelayed(msg, CONTENTS_UPDATE_DELAY);
                    break;
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAm = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        mState = (State)getLastNonConfigurationInstance();
        if (mState == null) {
            mState = new State();
        }
        mProcessBgColor = 0xff505050;
        getListView().setDivider(null);
        getListView().setAdapter(new ServiceListAdapter(mState));
    }

    void updateList() {
        if (mState.update(this, mAm)) {
            ((ServiceListAdapter)(getListView().getAdapter())).notifyDataSetChanged();
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        BaseItem bi = (BaseItem)l.getAdapter().getItem(position);
        if (!bi.mIsProcess) {
            mCurSelected = bi;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.confirm_stop_service);
            builder.setMessage(R.string.confirm_stop_service_msg);
            builder.setPositiveButton(R.string.confirm_stop_stop, this);
            builder.setNegativeButton(R.string.confirm_stop_cancel, null);
            builder.setCancelable(true);
            mCurDialog = builder.show();
        } else {
            mCurSelected = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (mCurSelected != null) {
            stopService(new Intent().setComponent(
                    ((ServiceItem)mCurSelected).mRunningService.service));
            updateList();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeMessages(MSG_UPDATE_TIMES);
        mHandler.removeMessages(MSG_UPDATE_CONTENTS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList();
        mHandler.removeMessages(MSG_UPDATE_TIMES);
        Message msg = mHandler.obtainMessage(MSG_UPDATE_TIMES);
        mHandler.sendMessageDelayed(msg, TIME_UPDATE_DELAY);
        mHandler.removeMessages(MSG_UPDATE_CONTENTS);
        msg = mHandler.obtainMessage(MSG_UPDATE_CONTENTS);
        mHandler.sendMessageDelayed(msg, CONTENTS_UPDATE_DELAY);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mState;
    }

    public void onMovedToScrapHeap(View view) {
        mActiveItems.remove(view);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCurDialog != null) {
            mCurDialog.dismiss();
        }
    }
}

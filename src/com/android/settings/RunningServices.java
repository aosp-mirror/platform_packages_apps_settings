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
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    static final int MSG_REFRESH_UI = 3;
    
    static final long TIME_UPDATE_DELAY = 1000;
    static final long CONTENTS_UPDATE_DELAY = 2000;
    
    // Memory pages are 4K.
    static final long PAGE_SIZE = 4*1024;
    
    long SECONDARY_SERVER_MEM;
    
    final HashMap<View, ActiveItem> mActiveItems = new HashMap<View, ActiveItem>();
    
    ActivityManager mAm;
    
    State mState;
    
    StringBuilder mBuilder = new StringBuilder(128);
    
    BaseItem mCurSelected;
    
    int mProcessBgColor;
    
    LinearColorBar mColorBar;
    TextView mBackgroundProcessText;
    TextView mForegroundProcessText;
    
    int mLastNumBackgroundProcesses = -1;
    int mLastNumForegroundProcesses = -1;
    int mLastNumServiceProcesses = -1;
    long mLastBackgroundProcessMemory = -1;
    long mLastForegroundProcessMemory = -1;
    long mLastServiceProcessMemory = -1;
    long mLastAvailMemory = -1;
    
    Dialog mCurDialog;
    
    byte[] mBuffer = new byte[1024];
    
    class ActiveItem {
        View mRootView;
        BaseItem mItem;
        ActivityManager.RunningServiceInfo mService;
        ViewHolder mHolder;
        long mFirstRunTime;
        
        void updateTime(Context context) {
            if (mItem.mIsProcess) {
                String size = mItem.mSizeStr != null ? mItem.mSizeStr : "";
                if (!size.equals(mItem.mCurSizeStr)) {
                    mItem.mCurSizeStr = size;
                    mHolder.size.setText(size);
                }
            } else {
                if (mItem.mActiveSince >= 0) {
                    mHolder.size.setText(DateUtils.formatElapsedTime(mBuilder,
                            (SystemClock.uptimeMillis()-mFirstRunTime)/1000));
                } else {
                    mHolder.size.setText(context.getResources().getText(
                            R.string.service_restarting));
                }
            }
        }
    }
    
    static class BaseItem {
        final boolean mIsProcess;
        
        PackageItemInfo mPackageInfo;
        CharSequence mDisplayLabel;
        String mLabel;
        String mDescription;
        
        int mCurSeq;
        
        long mActiveSince;
        long mSize;
        String mSizeStr;
        String mCurSizeStr;
        boolean mNeedDivider;
        
        public BaseItem(boolean isProcess) {
            mIsProcess = isProcess;
        }
    }
    
    static class ServiceItem extends BaseItem {
        ActivityManager.RunningServiceInfo mRunningService;
        ServiceInfo mServiceInfo;
        boolean mShownAsStarted;
        
        public ServiceItem() {
            super(false);
        }
    }
    
    static class ProcessItem extends BaseItem {
        final HashMap<ComponentName, ServiceItem> mServices
                = new HashMap<ComponentName, ServiceItem>();
        final SparseArray<ProcessItem> mDependentProcesses
                = new SparseArray<ProcessItem>();
        
        final int mUid;
        final String mProcessName;
        int mPid;
        
        ProcessItem mClient;
        int mLastNumDependentProcesses;
        
        int mRunningSeq;
        ActivityManager.RunningAppProcessInfo mRunningProcessInfo;
        
        // Purely for sorting.
        boolean mIsSystem;
        boolean mIsStarted;
        long mActiveSince;
        
        public ProcessItem(Context context, int uid, String processName) {
            super(true);
            mDescription = context.getResources().getString(
                    R.string.service_process_name, processName);
            mUid = uid;
            mProcessName = processName;
        }
        
        void ensureLabel(PackageManager pm) {
            if (mLabel != null) {
                return;
            }
            
            try {
                ApplicationInfo ai = pm.getApplicationInfo(mProcessName, 0);
                if (ai.uid == mUid) {
                    mDisplayLabel = ai.loadLabel(pm);
                    mLabel = mDisplayLabel.toString();
                    mPackageInfo = ai;
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
            
            // If we couldn't get information about the overall
            // process, try to find something about the uid.
            String[] pkgs = pm.getPackagesForUid(mUid);
            
            // If there is one package with this uid, that is what we want.
            if (pkgs.length == 1) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkgs[0], 0);
                    mDisplayLabel = ai.loadLabel(pm);
                    mLabel = mDisplayLabel.toString();
                    mPackageInfo = ai;
                    return;
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            
            // If there are multiple, see if one gives us the official name
            // for this uid.
            for (String name : pkgs) {
                try {
                    PackageInfo pi = pm.getPackageInfo(name, 0);
                    if (pi.sharedUserLabel != 0) {
                        CharSequence nm = pm.getText(name,
                                pi.sharedUserLabel, pi.applicationInfo);
                        if (nm != null) {
                            mDisplayLabel = nm;
                            mLabel = nm.toString();
                            mPackageInfo = pi.applicationInfo;
                            return;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            
            // If still don't have anything to display, just use the
            // service info.
            if (mServices.size() > 0) {
                mPackageInfo = mServices.values().iterator().next()
                        .mServiceInfo.applicationInfo;
                mDisplayLabel = mPackageInfo.loadLabel(pm);
                mLabel = mDisplayLabel.toString();
                return;
            }
            
            // Finally... whatever, just pick the first package's name.
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkgs[0], 0);
                mDisplayLabel = ai.loadLabel(pm);
                mLabel = mDisplayLabel.toString();
                mPackageInfo = ai;
                return;
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        
        boolean updateService(Context context,
                ActivityManager.RunningServiceInfo service) {
            final PackageManager pm = context.getPackageManager();
            
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
                si.mPackageInfo = si.mServiceInfo.applicationInfo;
                mServices.put(service.service, si);
            }
            si.mCurSeq = mCurSeq;
            si.mRunningService = service;
            long activeSince = service.restarting == 0 ? service.activeSince : -1;
            if (si.mActiveSince != activeSince) {
                si.mActiveSince = activeSince;
                changed = true;
            }
            if (service.clientPackage != null && service.clientLabel != 0) {
                if (si.mShownAsStarted) {
                    si.mShownAsStarted = false;
                    changed = true;
                }
                try {
                    Resources clientr = pm.getResourcesForApplication(service.clientPackage);
                    String label = clientr.getString(service.clientLabel);
                    si.mDescription = context.getResources().getString(
                            R.string.service_client_name, label);
                } catch (PackageManager.NameNotFoundException e) {
                    si.mDescription = null;
                }
            } else {
                if (!si.mShownAsStarted) {
                    si.mShownAsStarted = true;
                    changed = true;
                }
                si.mDescription = context.getResources().getString(
                        R.string.service_started_by_app);
            }
            
            return changed;
        }
        
        boolean updateSize(Context context, Debug.MemoryInfo mem, int curSeq) {
            mSize = ((long)mem.getTotalPss()) * 1024;
            if (mCurSeq == curSeq) {
                String sizeStr = Formatter.formatShortFileSize(
                        context, mSize);
                if (!sizeStr.equals(mSizeStr)){
                    mSizeStr = sizeStr;
                    // We update this on the second tick where we update just
                    // the text in the current items, so no need to say we
                    // changed here.
                    return false;
                }
            }
            return false;
        }
        
        boolean buildDependencyChain(Context context, PackageManager pm, int curSeq) {
            final int NP = mDependentProcesses.size();
            boolean changed = false;
            for (int i=0; i<NP; i++) {
                ProcessItem proc = mDependentProcesses.valueAt(i);
                if (proc.mClient != this) {
                    changed = true;
                    proc.mClient = this;
                }
                proc.mCurSeq = curSeq;
                proc.ensureLabel(pm);
                changed |= proc.buildDependencyChain(context, pm, curSeq);
            }
            
            if (mLastNumDependentProcesses != mDependentProcesses.size()) {
                changed = true;
                mLastNumDependentProcesses = mDependentProcesses.size();
            }
            
            return changed;
        }
        
        void addDependentProcesses(ArrayList<BaseItem> dest,
                ArrayList<ProcessItem> destProc) {
            final int NP = mDependentProcesses.size();
            for (int i=0; i<NP; i++) {
                ProcessItem proc = mDependentProcesses.valueAt(i);
                proc.addDependentProcesses(dest, destProc);
                dest.add(proc);
                if (proc.mPid > 0) {
                    destProc.add(proc);
                }
            }
        }
    }
    
    static class ServiceProcessComparator implements Comparator<ProcessItem> {
        public int compare(ProcessItem object1, ProcessItem object2) {
            if (object1.mIsStarted != object2.mIsStarted) {
                // Non-started processes go last.
                return object1.mIsStarted ? -1 : 1;
            }
            if (object1.mIsSystem != object2.mIsSystem) {
                // System processes go below non-system.
                return object1.mIsSystem ? 1 : -1;
            }
            if (object1.mActiveSince != object2.mActiveSince) {
                // Remaining ones are sorted with the longest running
                // services last.
                return (object1.mActiveSince > object2.mActiveSince) ? -1 : 1;
            }
            return 0;
        }
    }
    
    static class State {
        final SparseArray<HashMap<String, ProcessItem>> mProcesses
                = new SparseArray<HashMap<String, ProcessItem>>();
        final SparseArray<ProcessItem> mActiveProcesses
                = new SparseArray<ProcessItem>();
        final ServiceProcessComparator mServiceProcessComparator
                = new ServiceProcessComparator();
        
        // Temporary for finding process dependencies.
        final SparseArray<ProcessItem> mRunningProcesses
                = new SparseArray<ProcessItem>();
        
        final ArrayList<ProcessItem> mProcessItems = new ArrayList<ProcessItem>();
        final ArrayList<ProcessItem> mAllProcessItems = new ArrayList<ProcessItem>();
        
        int mSequence = 0;
        
        // ----- following protected by mLock -----
        
        // Lock for protecting the state that will be shared between the
        // background update thread and the UI thread.
        final Object mLock = new Object();
        
        ArrayList<BaseItem> mItems = new ArrayList<BaseItem>();
        
        int mNumBackgroundProcesses;
        long mBackgroundProcessMemory;
        int mNumForegroundProcesses;
        long mForegroundProcessMemory;
        int mNumServiceProcesses;
        long mServiceProcessMemory;
        
        boolean update(Context context, ActivityManager am) {
            final PackageManager pm = context.getPackageManager();
            
            mSequence++;
            
            boolean changed = false;
            
            List<ActivityManager.RunningServiceInfo> services 
                    = am.getRunningServices(MAX_SERVICES);
            final int NS = services != null ? services.size() : 0;
            for (int i=0; i<NS; i++) {
                ActivityManager.RunningServiceInfo si = services.get(i);
                // We are not interested in services that have not been started
                // and don't have a known client, because
                // there is nothing the user can do about them.
                if (!si.started && si.clientLabel == 0) {
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
                    proc = new ProcessItem(context, si.uid, si.process);
                    procs.put(si.process, proc);
                }
                
                if (proc.mCurSeq != mSequence) {
                    int pid = si.restarting == 0 ? si.pid : 0;
                    if (pid != proc.mPid) {
                        changed = true;
                        if (proc.mPid != pid) {
                            if (proc.mPid != 0) {
                                mActiveProcesses.remove(proc.mPid);
                            }
                            if (pid != 0) {
                                mActiveProcesses.put(pid, proc);
                            }
                            proc.mPid = pid;
                        }
                    }
                    proc.mDependentProcesses.clear();
                    proc.mCurSeq = mSequence;
                }
                changed |= proc.updateService(context, si);
            }
            
            // Now update the map of other processes that are running (but
            // don't have services actively running inside them).
            List<ActivityManager.RunningAppProcessInfo> processes
                    = am.getRunningAppProcesses();
            final int NP = processes != null ? processes.size() : 0;
            for (int i=0; i<NP; i++) {
                ActivityManager.RunningAppProcessInfo pi = processes.get(i);
                ProcessItem proc = mActiveProcesses.get(pi.pid);
                if (proc == null) {
                    // This process is not one that is a direct container
                    // of a service, so look for it in the secondary
                    // running list.
                    proc = mRunningProcesses.get(pi.pid);
                    if (proc == null) {
                        proc = new ProcessItem(context, pi.uid, pi.processName);
                        proc.mPid = pi.pid;
                        mRunningProcesses.put(pi.pid, proc);
                    }
                    proc.mDependentProcesses.clear();
                }
                proc.mRunningSeq = mSequence;
                proc.mRunningProcessInfo = pi;
            }
            
            // Build the chains from client processes to the process they are
            // dependent on; also remove any old running processes.
            int NRP = mRunningProcesses.size();
            for (int i=0; i<NRP; i++) {
                ProcessItem proc = mRunningProcesses.valueAt(i);
                if (proc.mRunningSeq == mSequence) {
                    int clientPid = proc.mRunningProcessInfo.importanceReasonPid;
                    if (clientPid != 0) {
                        ProcessItem client = mActiveProcesses.get(clientPid);
                        if (client == null) {
                            client = mRunningProcesses.get(clientPid);
                        }
                        if (client != null) {
                            client.mDependentProcesses.put(proc.mPid, proc);
                        }
                    } else {
                        // In this pass the process doesn't have a client.
                        // Clear to make sure if it later gets the same one
                        // that we will detect the change.
                        proc.mClient = null;
                    }
                } else {
                    mRunningProcesses.remove(mRunningProcesses.keyAt(i));
                }
            }
            
            // Follow the tree from all primary service processes to all
            // processes they are dependent on, marking these processes as
            // still being active and determining if anything has changed.
            final int NAP = mActiveProcesses.size();
            for (int i=0; i<NAP; i++) {
                ProcessItem proc = mActiveProcesses.valueAt(i);
                if (proc.mCurSeq == mSequence) {
                    changed |= proc.buildDependencyChain(context, pm, mSequence);
                }
            }
            
            // Look for services and their primary processes that no longer exist...
            for (int i=0; i<mProcesses.size(); i++) {
                HashMap<String, ProcessItem> procs = mProcesses.valueAt(i);
                Iterator<ProcessItem> pit = procs.values().iterator();
                while (pit.hasNext()) {
                    ProcessItem pi = pit.next();
                    if (pi.mCurSeq == mSequence) {
                        pi.ensureLabel(pm);
                        if (pi.mPid == 0) {
                            // Sanity: a non-process can't be dependent on
                            // anything.
                            pi.mDependentProcesses.clear();
                        }
                    } else {
                        changed = true;
                        pit.remove();
                        if (procs.size() == 0) {
                            mProcesses.remove(mProcesses.keyAt(i));
                        }
                        if (pi.mPid != 0) {
                            mActiveProcesses.remove(pi.mPid);
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
                // First determine an order for the services.
                ArrayList<ProcessItem> sortedProcesses = new ArrayList<ProcessItem>();
                for (int i=0; i<mProcesses.size(); i++) {
                    for (ProcessItem pi : mProcesses.valueAt(i).values()) {
                        pi.mIsSystem = false;
                        pi.mIsStarted = true;
                        pi.mActiveSince = Long.MAX_VALUE;
                        for (ServiceItem si : pi.mServices.values()) {
                            if (si.mServiceInfo != null
                                    && (si.mServiceInfo.applicationInfo.flags
                                            & ApplicationInfo.FLAG_SYSTEM) != 0) {
                                pi.mIsSystem = true;
                            }
                            if (si.mRunningService != null
                                    && si.mRunningService.clientLabel != 0) {
                                pi.mIsStarted = false;
                                if (pi.mActiveSince > si.mRunningService.activeSince) {
                                    pi.mActiveSince = si.mRunningService.activeSince;
                                }
                            }
                        }
                        sortedProcesses.add(pi);
                    }
                }
                
                Collections.sort(sortedProcesses, mServiceProcessComparator);
                
                ArrayList<BaseItem> newItems = new ArrayList<BaseItem>();
                mProcessItems.clear();
                for (int i=0; i<sortedProcesses.size(); i++) {
                    ProcessItem pi = sortedProcesses.get(i);
                    pi.mNeedDivider = false;
                    // First add processes we are dependent on.
                    pi.addDependentProcesses(newItems, mProcessItems);
                    // And add the process itself.
                    newItems.add(pi);
                    if (pi.mPid > 0) {
                        mProcessItems.add(pi);
                    }
                    // And finally the services running in it.
                    boolean needDivider = false;
                    for (ServiceItem si : pi.mServices.values()) {
                        si.mNeedDivider = needDivider;
                        needDivider = true;
                        newItems.add(si);
                    }
                }
                synchronized (mLock) {
                    mItems = newItems;
                }
            }
            
            // Count number of interesting other (non-active) processes, and
            // build a list of all processes we will retrieve memory for.
            mAllProcessItems.clear();
            mAllProcessItems.addAll(mProcessItems);
            int numBackgroundProcesses = 0;
            int numForegroundProcesses = 0;
            int numServiceProcesses = 0;
            NRP = mRunningProcesses.size();
            for (int i=0; i<NRP; i++) {
                ProcessItem proc = mRunningProcesses.valueAt(i);
                if (proc.mCurSeq != mSequence) {
                    // We didn't hit this process as a dependency on one
                    // of our active ones, so add it up if needed.
                    if (proc.mRunningProcessInfo.importance >=
                            ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                        numBackgroundProcesses++;
                        mAllProcessItems.add(proc);
                    } else if (proc.mRunningProcessInfo.importance <=
                            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                        numForegroundProcesses++;
                        mAllProcessItems.add(proc);
                    } else {
                        Log.i(TAG, "Unknown non-service process: "
                                + proc.mProcessName + " #" + proc.mPid);
                    }
                } else {
                    numServiceProcesses++;
                }
            }
            
            long backgroundProcessMemory = 0;
            long foregroundProcessMemory = 0;
            long serviceProcessMemory = 0;
            try {
                final int numProc = mAllProcessItems.size();
                int[] pids = new int[numProc];
                for (int i=0; i<numProc; i++) {
                    pids[i] = mAllProcessItems.get(i).mPid;
                }
                Debug.MemoryInfo[] mem = ActivityManagerNative.getDefault()
                        .getProcessMemoryInfo(pids);
                for (int i=pids.length-1; i>=0; i--) {
                    ProcessItem proc = mAllProcessItems.get(i);
                    changed |= proc.updateSize(context, mem[i], mSequence);
                    if (proc.mCurSeq == mSequence) {
                        serviceProcessMemory += proc.mSize;
                    } else if (proc.mRunningProcessInfo.importance >=
                            ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                        backgroundProcessMemory += proc.mSize;
                    } else if (proc.mRunningProcessInfo.importance <=
                            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                        foregroundProcessMemory += proc.mSize;
                    }
                }
            } catch (RemoteException e) {
            }
            
            synchronized (mLock) {
                mNumBackgroundProcesses = numBackgroundProcesses;
                mNumForegroundProcesses = numForegroundProcesses;
                mNumServiceProcesses = numServiceProcesses;
                mBackgroundProcessMemory = backgroundProcessMemory;
                mForegroundProcessMemory = foregroundProcessMemory;
                mServiceProcessMemory = serviceProcessMemory;
            }
            
            return changed;
        }
        
        ArrayList<BaseItem> getCurrentItems() {
            synchronized (mLock) {
                return mItems;
            }
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
        TextView description;
        TextView size;
    }
    
    class ServiceListAdapter extends BaseAdapter {
        final State mState;
        final LayoutInflater mInflater;
        ArrayList<BaseItem> mItems;
        
        ServiceListAdapter(State state) {
            mState = state;
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            refreshItems();
        }

        void refreshItems() {
            ArrayList<BaseItem> newItems = mState.getCurrentItems();
            if (mItems != newItems) {
                mItems = newItems;
            }
            if (mItems == null) {
                mItems = new ArrayList<BaseItem>();
            }
        }
        
        public boolean hasStableIds() {
            return true;
        }
        
        public int getCount() {
            return mItems.size();
        }

        public Object getItem(int position) {
            return mItems.get(position);
        }

        public long getItemId(int position) {
            return mItems.get(position).hashCode();
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return !mItems.get(position).mIsProcess;
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
            h.description = (TextView)v.findViewById(R.id.description);
            h.size = (TextView)v.findViewById(R.id.size);
            v.setTag(h);
            return v;
        }
        
        public void bindView(View view, int position) {
            synchronized (mState.mLock) {
                ViewHolder vh = (ViewHolder) view.getTag();
                if (position >= mItems.size()) {
                    // List must have changed since we last reported its
                    // size...  ignore here, we will be doing a data changed
                    // to refresh the entire list.
                    return;
                }
                BaseItem item = mItems.get(position);
                vh.name.setText(item.mDisplayLabel);
                vh.separator.setVisibility(item.mNeedDivider
                        ? View.VISIBLE : View.INVISIBLE);
                ActiveItem ai = new ActiveItem();
                ai.mRootView = view;
                ai.mItem = item;
                ai.mHolder = vh;
                ai.mFirstRunTime = item.mActiveSince;
                vh.description.setText(item.mDescription);
                if (item.mIsProcess) {
                    view.setBackgroundColor(mProcessBgColor);
                    vh.icon.setImageDrawable(null);
                    vh.icon.setVisibility(View.GONE);
                    vh.description.setText(item.mDescription);
                    item.mCurSizeStr = null;
                } else {
                    view.setBackgroundDrawable(null);
                    vh.icon.setImageDrawable(item.mPackageInfo.loadIcon(getPackageManager()));
                    vh.icon.setVisibility(View.VISIBLE);
                    vh.description.setText(item.mDescription);
                    ai.mFirstRunTime = item.mActiveSince;
                }
                ai.updateTime(RunningServices.this);
                mActiveItems.put(view, ai);
            }
        }
    }
    
    public static class LinearColorBar extends LinearLayout {
        private float mRedRatio;
        private float mYellowRatio;
        private float mGreenRatio;
        
        final Rect mRect = new Rect();
        final Paint mPaint = new Paint();
        
        public LinearColorBar(Context context, AttributeSet attrs) {
            super(context, attrs);
            setWillNotDraw(false);
            mPaint.setStyle(Paint.Style.FILL);
        }

        public void setRatios(float red, float yellow, float green) {
            mRedRatio = red;
            mYellowRatio = yellow;
            mGreenRatio = green;
            invalidate();
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            int width = getWidth();
            mRect.top = 0;
            mRect.bottom = getHeight();
            
            int left = 0;
            
            int right = left + (int)(width*mRedRatio);
            if (left < right) {
                mRect.left = left;
                mRect.right = right;
                mPaint.setColor(0xffff8080);
                canvas.drawRect(mRect, mPaint);
                width -= (right-left);
                left = right;
            }
            
            right = left + (int)(width*mYellowRatio);
            if (left < right) {
                mRect.left = left;
                mRect.right = right;
                mPaint.setColor(0xffffff00);
                canvas.drawRect(mRect, mPaint);
                width -= (right-left);
                left = right;
            }
            
            right = left + width;
            if (left < right) {
                mRect.left = left;
                mRect.right = right;
                mPaint.setColor(0xff80ff80);
                canvas.drawRect(mRect, mPaint);
            }
        }
    }
    
    HandlerThread mBackgroundThread;
    final class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_CONTENTS:
                    Message cmd = mHandler.obtainMessage(MSG_REFRESH_UI);
                    cmd.arg1 = mState.update(RunningServices.this, mAm) ? 1 : 0;
                    mHandler.sendMessage(cmd);
                    removeMessages(MSG_UPDATE_CONTENTS);
                    msg = obtainMessage(MSG_UPDATE_CONTENTS);
                    sendMessageDelayed(msg, CONTENTS_UPDATE_DELAY);
                    break;
            }
        }
    };
    BackgroundHandler mBackgroundHandler;
    
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_TIMES:
                    Iterator<ActiveItem> it = mActiveItems.values().iterator();
                    while (it.hasNext()) {
                        ActiveItem ai = it.next();
                        if (ai.mRootView.getWindowToken() == null) {
                            // Clean out any dead views, just in case.
                            it.remove();
                            continue;
                        }
                        ai.updateTime(RunningServices.this);
                    }
                    removeMessages(MSG_UPDATE_TIMES);
                    msg = obtainMessage(MSG_UPDATE_TIMES);
                    sendMessageDelayed(msg, TIME_UPDATE_DELAY);
                    break;
                case MSG_REFRESH_UI:
                    refreshUi(msg.arg1 != 0);
                    break;
            }
        }
    };
    
    private boolean matchText(byte[] buffer, int index, String text) {
        int N = text.length();
        if ((index+N) >= buffer.length) {
            return false;
        }
        for (int i=0; i<N; i++) {
            if (buffer[index+i] != text.charAt(i)) {
                return false;
            }
        }
        return true;
    }
    
    private long extractMemValue(byte[] buffer, int index) {
        while (index < buffer.length && buffer[index] != '\n') {
            if (buffer[index] >= '0' && buffer[index] <= '9') {
                int start = index;
                index++;
                while (index < buffer.length && buffer[index] >= '0'
                    && buffer[index] <= '9') {
                    index++;
                }
                String str = new String(buffer, start, index-start);
                return ((long)Integer.parseInt(str)) * 1024;
            }
            index++;
        }
        return 0;
    }
    
    private long readAvailMem() {
        try {
            long memFree = 0;
            long memCached = 0;
            FileInputStream is = new FileInputStream("/proc/meminfo");
            int len = is.read(mBuffer);
            is.close();
            final int BUFLEN = mBuffer.length;
            for (int i=0; i<len && (memFree == 0 || memCached == 0); i++) {
                if (matchText(mBuffer, i, "MemFree")) {
                    i += 7;
                    memFree = extractMemValue(mBuffer, i);
                } else if (matchText(mBuffer, i, "Cached")) {
                    i += 6;
                    memCached = extractMemValue(mBuffer, i);
                }
                while (i < BUFLEN && mBuffer[i] != '\n') {
                    i++;
                }
            }
            return memFree + memCached;
        } catch (java.io.FileNotFoundException e) {
        } catch (java.io.IOException e) {
        }
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAm = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        mState = (State)getLastNonConfigurationInstance();
        if (mState == null) {
            mState = new State();
        }
        mProcessBgColor = 0xff505050;
        setContentView(R.layout.running_services);
        getListView().setDivider(null);
        getListView().setAdapter(new ServiceListAdapter(mState));
        mColorBar = (LinearColorBar)findViewById(R.id.color_bar);
        mBackgroundProcessText = (TextView)findViewById(R.id.backgroundText);
        mForegroundProcessText = (TextView)findViewById(R.id.foregroundText);
        
        // Magic!  Implementation detail!  Don't count on this!
        SECONDARY_SERVER_MEM =
            Integer.valueOf(SystemProperties.get("ro.SECONDARY_SERVER_MEM"))*PAGE_SIZE;
    }

    void refreshUi(boolean dataChanged) {
        if (dataChanged) {
            ServiceListAdapter adapter = (ServiceListAdapter)(getListView().getAdapter());
            adapter.refreshItems();
            adapter.notifyDataSetChanged();
        }
        
        // This is the amount of available memory until we start killing
        // background services.
        long availMem = readAvailMem() - SECONDARY_SERVER_MEM;
        if (availMem < 0) {
            availMem = 0;
        }
        
        synchronized (mState.mLock) {
            if (mLastNumBackgroundProcesses != mState.mNumBackgroundProcesses
                    || mLastBackgroundProcessMemory != mState.mBackgroundProcessMemory
                    || mLastAvailMemory != availMem) {
                mLastNumBackgroundProcesses = mState.mNumBackgroundProcesses;
                mLastBackgroundProcessMemory = mState.mBackgroundProcessMemory;
                mLastAvailMemory = availMem;
                String availStr = availMem != 0
                        ? Formatter.formatShortFileSize(this, availMem) : "0";
                String sizeStr = Formatter.formatShortFileSize(this, mLastBackgroundProcessMemory);
                mBackgroundProcessText.setText(getResources().getString(
                        R.string.service_background_processes,
                        mLastNumBackgroundProcesses, availStr, sizeStr));
            }
            if (mLastNumForegroundProcesses != mState.mNumForegroundProcesses
                    || mLastForegroundProcessMemory != mState.mForegroundProcessMemory) {
                mLastNumForegroundProcesses = mState.mNumForegroundProcesses;
                mLastForegroundProcessMemory = mState.mForegroundProcessMemory;
                String sizeStr = Formatter.formatShortFileSize(this, mLastForegroundProcessMemory);
                mForegroundProcessText.setText(getResources().getString(
                        R.string.service_foreground_processes, mLastNumForegroundProcesses, sizeStr));
            }
            mLastNumServiceProcesses = mState.mNumServiceProcesses;
            mLastServiceProcessMemory = mState.mServiceProcessMemory;
            
            float totalMem = availMem + mLastBackgroundProcessMemory
                    + mLastForegroundProcessMemory + mLastServiceProcessMemory;
            mColorBar.setRatios(mLastForegroundProcessMemory/totalMem,
                    mLastServiceProcessMemory/totalMem,
                    (availMem+mLastBackgroundProcessMemory)/totalMem);
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        BaseItem bi = (BaseItem)l.getAdapter().getItem(position);
        if (!bi.mIsProcess) {
            ServiceItem si = (ServiceItem)bi;
            if (si.mRunningService.clientLabel != 0) {
                mCurSelected = null;
                PendingIntent pi = mAm.getRunningServiceControlPanel(
                        si.mRunningService.service);
                if (pi != null) {
                    try {
                        this.startIntentSender(pi.getIntentSender(), null,
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET,
                                Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.w(TAG, e);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, e);
                    } catch (ActivityNotFoundException e) {
                        Log.w(TAG, e);
                    }
                }
            } else {
                mCurSelected = bi;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.confirm_stop_service);
                String msg = getResources().getString(
                        R.string.confirm_stop_service_msg,
                        si.mPackageInfo.loadLabel(getPackageManager()));
                builder.setMessage(msg);
                builder.setPositiveButton(R.string.confirm_stop_stop, this);
                builder.setNegativeButton(R.string.confirm_stop_cancel, null);
                builder.setCancelable(true);
                mCurDialog = builder.show();
            }
        } else {
            mCurSelected = null;
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (mCurSelected != null) {
            stopService(new Intent().setComponent(
                    ((ServiceItem)mCurSelected).mRunningService.service));
            if (mBackgroundHandler != null) {
                mBackgroundHandler.sendEmptyMessage(MSG_UPDATE_CONTENTS);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeMessages(MSG_UPDATE_TIMES);
        if (mBackgroundThread != null) {
            mBackgroundThread.quit();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi(mState.update(this, mAm));
        mBackgroundThread = new HandlerThread("RunningServices");
        mBackgroundThread.start();
        mBackgroundHandler = new BackgroundHandler(mBackgroundThread.getLooper());
        mHandler.removeMessages(MSG_UPDATE_TIMES);
        Message msg = mHandler.obtainMessage(MSG_UPDATE_TIMES);
        mHandler.sendMessageDelayed(msg, TIME_UPDATE_DELAY);
        mBackgroundHandler.removeMessages(MSG_UPDATE_CONTENTS);
        msg = mBackgroundHandler.obtainMessage(MSG_UPDATE_CONTENTS);
        mBackgroundHandler.sendMessageDelayed(msg, CONTENTS_UPDATE_DELAY);
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

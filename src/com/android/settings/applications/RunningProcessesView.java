/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.settings.R;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class RunningProcessesView extends FrameLayout
        implements AdapterView.OnItemClickListener {
    
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
    
    RunningState mState;
    
    StringBuilder mBuilder = new StringBuilder(128);
    
    RunningState.BaseItem mCurSelected;
    
    ListView mListView;
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
    
    public static class ActiveItem {
        View mRootView;
        RunningState.BaseItem mItem;
        ActivityManager.RunningServiceInfo mService;
        ViewHolder mHolder;
        long mFirstRunTime;
        
        void updateTime(Context context, StringBuilder builder) {
            TextView uptimeView = null;
            
            if (mItem instanceof RunningState.ServiceItem) {
                // If we are displaying a service, then the service
                // uptime goes at the top.
                uptimeView = mHolder.size;
                
            } else {
                String size = mItem.mSizeStr != null ? mItem.mSizeStr : "";
                if (!size.equals(mItem.mCurSizeStr)) {
                    mItem.mCurSizeStr = size;
                    mHolder.size.setText(size);
                }
                
                if (mItem instanceof RunningState.MergedItem) {
                    // This item represents both services and proceses,
                    // so show the service uptime below.
                    uptimeView = mHolder.uptime;
                }
            }
            
            if (uptimeView != null) {
                if (mItem.mActiveSince >= 0) {
                    uptimeView.setText(DateUtils.formatElapsedTime(builder,
                            (SystemClock.uptimeMillis()-mFirstRunTime)/1000));
                } else {
                    boolean isService = false;
                    if (mItem instanceof RunningState.MergedItem) {
                        isService = ((RunningState.MergedItem)mItem).mServices.size() > 0;
                    }
                    if (isService) {
                        uptimeView.setText(context.getResources().getText(
                                R.string.service_restarting));
                    } else {
                        uptimeView.setText("");
                    }
                }
            }
        }
    }
    
    public static class ViewHolder {
        public View rootView;
        public ImageView icon;
        public TextView name;
        public TextView description;
        public TextView size;
        public TextView uptime;
        
        public ViewHolder(View v) {
            rootView = v;
            icon = (ImageView)v.findViewById(R.id.icon);
            name = (TextView)v.findViewById(R.id.name);
            description = (TextView)v.findViewById(R.id.description);
            size = (TextView)v.findViewById(R.id.size);
            uptime = (TextView)v.findViewById(R.id.uptime);
            v.setTag(this);
        }
        
        public ActiveItem bind(RunningState state, RunningState.BaseItem item,
                StringBuilder builder) {
            synchronized (state.mLock) {
                name.setText(item.mDisplayLabel);
                ActiveItem ai = new ActiveItem();
                ai.mRootView = rootView;
                ai.mItem = item;
                ai.mHolder = this;
                ai.mFirstRunTime = item.mActiveSince;
                description.setText(item.mDescription);
                item.mCurSizeStr = null;
                icon.setImageDrawable(item.mPackageInfo.loadIcon(
                        rootView.getContext().getPackageManager()));
                icon.setVisibility(View.VISIBLE);
                ai.updateTime(rootView.getContext(), builder);
                return ai;
            }
        }
    }
    
    static class TimeTicker extends TextView {
        public TimeTicker(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }
    
    class ServiceListAdapter extends BaseAdapter {
        final RunningState mState;
        final LayoutInflater mInflater;
        ArrayList<RunningState.MergedItem> mItems;
        
        ServiceListAdapter(RunningState state) {
            mState = state;
            mInflater = (LayoutInflater)getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            refreshItems();
        }

        void refreshItems() {
            ArrayList<RunningState.MergedItem> newItems = mState.getCurrentMergedItems();
            if (mItems != newItems) {
                mItems = newItems;
            }
            if (mItems == null) {
                mItems = new ArrayList<RunningState.MergedItem>();
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
            View v = mInflater.inflate(R.layout.running_processes_item, parent, false);
            new ViewHolder(v);
            return v;
        }
        
        public void bindView(View view, int position) {
            synchronized (mState.mLock) {
                if (position >= mItems.size()) {
                    // List must have changed since we last reported its
                    // size...  ignore here, we will be doing a data changed
                    // to refresh the entire list.
                    return;
                }
                ViewHolder vh = (ViewHolder) view.getTag();
                RunningState.MergedItem item = mItems.get(position);
                ActiveItem ai = vh.bind(mState, item, mBuilder);
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
                    cmd.arg1 = mState.update(getContext(), mAm) ? 1 : 0;
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
                        ai.updateTime(getContext(), mBuilder);
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
                String str = new String(buffer, 0, start, index-start);
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

    
    void refreshUi(boolean dataChanged) {
        if (dataChanged) {
            ServiceListAdapter adapter = (ServiceListAdapter)(mListView.getAdapter());
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
                String sizeStr = Formatter.formatShortFileSize(getContext(),
                        mLastAvailMemory + mLastBackgroundProcessMemory);
                mBackgroundProcessText.setText(getResources().getString(
                        R.string.service_background_processes, sizeStr));
            }
            if (mLastNumForegroundProcesses != mState.mNumForegroundProcesses
                    || mLastForegroundProcessMemory != mState.mForegroundProcessMemory
                    || mLastNumServiceProcesses != mState.mNumServiceProcesses
                    || mLastServiceProcessMemory != mState.mServiceProcessMemory) {
                mLastNumForegroundProcesses = mState.mNumForegroundProcesses;
                mLastForegroundProcessMemory = mState.mForegroundProcessMemory;
                mLastNumServiceProcesses = mState.mNumServiceProcesses;
                mLastServiceProcessMemory = mState.mServiceProcessMemory;
                String sizeStr = Formatter.formatShortFileSize(getContext(),
                        mLastForegroundProcessMemory + mLastServiceProcessMemory);
                mForegroundProcessText.setText(getResources().getString(
                        R.string.service_foreground_processes, sizeStr));
            }
            
            float totalMem = availMem + mLastBackgroundProcessMemory
                    + mLastForegroundProcessMemory + mLastServiceProcessMemory;
            mColorBar.setRatios(mLastForegroundProcessMemory/totalMem,
                    mLastServiceProcessMemory/totalMem,
                    (availMem+mLastBackgroundProcessMemory)/totalMem);
        }
    }
    
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        ListView l = (ListView)parent;
        RunningState.MergedItem mi = (RunningState.MergedItem)l.getAdapter().getItem(position);
        mCurSelected = mi;
        Intent intent = new Intent();
        intent.putExtra(RunningServiceDetails.KEY_UID, mi.mProcess.mUid);
        intent.putExtra(RunningServiceDetails.KEY_PROCESS, mi.mProcess.mProcessName);
        intent.setClass(getContext(), RunningServiceDetails.class);
        getContext().startActivity(intent);
    }

    public void onMovedToScrapHeap(View view) {
        mActiveItems.remove(view);
    }

    public RunningProcessesView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void doCreate(Bundle savedInstanceState, Object nonConfigurationInstace) {
        mAm = (ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE);
        mState = (RunningState)nonConfigurationInstace;
        if (mState == null) {
            mState = new RunningState();
        }
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.running_processes_view, this);
        mListView = (ListView)findViewById(android.R.id.list);
        View emptyView = findViewById(com.android.internal.R.id.empty);
        if (emptyView != null) {
            mListView.setEmptyView(emptyView);
        }
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(new ServiceListAdapter(mState));
        mColorBar = (LinearColorBar)findViewById(R.id.color_bar);
        mBackgroundProcessText = (TextView)findViewById(R.id.backgroundText);
        mForegroundProcessText = (TextView)findViewById(R.id.foregroundText);
        
        // Magic!  Implementation detail!  Don't count on this!
        SECONDARY_SERVER_MEM =
            Integer.valueOf(SystemProperties.get("ro.SECONDARY_SERVER_MEM"))*PAGE_SIZE;
    }
    
    public void doPause() {
        mHandler.removeMessages(MSG_UPDATE_TIMES);
        if (mBackgroundThread != null) {
            mBackgroundThread.quit();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
    }

    public void doResume() {
        refreshUi(mState.update(getContext(), mAm));
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

    public Object doRetainNonConfigurationInstance() {
        return mState;
    }
}

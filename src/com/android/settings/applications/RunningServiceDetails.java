package com.android.settings.applications;

import com.android.settings.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class RunningServiceDetails extends Activity {
    static final String TAG = "RunningServicesDetails";
    
    static final String KEY_UID = "uid";
    static final String KEY_PROCESS = "process";
    
    static final int MSG_UPDATE_TIMES = 1;
    static final int MSG_UPDATE_CONTENTS = 2;
    static final int MSG_REFRESH_UI = 3;
    
    ActivityManager mAm;
    LayoutInflater mInflater;
    
    RunningState mState;
    
    int mUid;
    String mProcessName;
    
    RunningState.MergedItem mMergedItem;
    
    ViewGroup mAllDetails;
    ViewGroup mSnippet;
    RunningProcessesView.ActiveItem mSnippetActiveItem;
    RunningProcessesView.ViewHolder mSnippetViewHolder;
    
    int mNumServices, mNumProcesses;
    
    TextView mServicesHeader;
    TextView mProcessesHeader;
    final ArrayList<ActiveDetail> mActiveDetails = new ArrayList<ActiveDetail>();
    
    class ActiveDetail implements View.OnClickListener {
        View mRootView;
        RunningProcessesView.ActiveItem mActiveItem;
        RunningProcessesView.ViewHolder mViewHolder;
        PendingIntent mManageIntent;

        public void onClick(View v) {
            if (mManageIntent != null) {
                try {
                    startIntentSender(mManageIntent.getIntentSender(), null,
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
            } else if (mActiveItem.mItem instanceof RunningState.ServiceItem) {
                RunningState.ServiceItem si = (RunningState.ServiceItem)mActiveItem.mItem;
                stopService(new Intent().setComponent(si.mRunningService.service));
                if (mMergedItem == null || mMergedItem.mServices.size() <= 1) {
                    // If there was only one service, we are finishing it,
                    // so no reason for the UI to stick around.
                    finish();
                } else {
                    if (mBackgroundHandler != null) {
                        mBackgroundHandler.sendEmptyMessage(MSG_UPDATE_CONTENTS);
                    }
                }
            } else {
                // Heavy-weight process.  We'll do a force-stop on it.
                mAm.forceStopPackage(mActiveItem.mItem.mPackageInfo.packageName);
                finish();
            }
        }
    }
    
    StringBuilder mBuilder = new StringBuilder(128);
    
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
                    cmd.arg1 = mState.update(RunningServiceDetails.this, mAm) ? 1 : 0;
                    mHandler.sendMessage(cmd);
                    removeMessages(MSG_UPDATE_CONTENTS);
                    msg = obtainMessage(MSG_UPDATE_CONTENTS);
                    sendMessageDelayed(msg, RunningProcessesView.CONTENTS_UPDATE_DELAY);
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
                    if (mSnippetActiveItem != null) {
                        mSnippetActiveItem.updateTime(RunningServiceDetails.this, mBuilder);
                    }
                    for (int i=0; i<mActiveDetails.size(); i++) {
                        mActiveDetails.get(i).mActiveItem.updateTime(
                                RunningServiceDetails.this, mBuilder);
                    }
                    removeMessages(MSG_UPDATE_TIMES);
                    msg = obtainMessage(MSG_UPDATE_TIMES);
                    sendMessageDelayed(msg, RunningProcessesView.TIME_UPDATE_DELAY);
                    break;
                case MSG_REFRESH_UI:
                    refreshUi(msg.arg1 != 0);
                    break;
            }
        }
    };
    
    boolean findMergedItem() {
        RunningState.MergedItem item = null;
        ArrayList<RunningState.MergedItem> newItems = mState.getCurrentMergedItems();
        if (newItems != null) {
            for (int i=0; i<newItems.size(); i++) {
                RunningState.MergedItem mi = newItems.get(i);
                if (mi.mProcess.mUid == mUid
                        && mi.mProcess.mProcessName.equals(mProcessName)) {
                    item = mi;
                    break;
                }
            }
        }
        if (mMergedItem != item) {
            mMergedItem = item;
            return true;
        }
        return false;
    }
    
    void addServiceDetailsView(RunningState.ServiceItem si, RunningState.MergedItem mi) {
        if (mNumServices == 0) {
            mServicesHeader = (TextView)mInflater.inflate(R.layout.separator_label,
                    mAllDetails, false);
            mServicesHeader.setText(R.string.runningservicedetails_services_title);
            mAllDetails.addView(mServicesHeader);
        }
        mNumServices++;
        
        RunningState.BaseItem bi = si != null ? si : mi;
        
        ActiveDetail detail = new ActiveDetail();
        View root = mInflater.inflate(R.layout.running_service_details_service,
                mAllDetails, false);
        mAllDetails.addView(root);
        detail.mRootView = root;
        detail.mViewHolder = new RunningProcessesView.ViewHolder(root);
        detail.mActiveItem = detail.mViewHolder.bind(mState, bi, mBuilder);
        
        if (si != null && si.mRunningService.clientLabel != 0) {
            detail.mManageIntent = mAm.getRunningServiceControlPanel(
                    si.mRunningService.service);
        }
        
        TextView description = (TextView)root.findViewById(R.id.comp_description);
        if (si != null && si.mServiceInfo.descriptionRes != 0) {
            description.setText(getPackageManager().getText(
                    si.mServiceInfo.packageName, si.mServiceInfo.descriptionRes,
                    si.mServiceInfo.applicationInfo));
        } else {
            if (detail.mManageIntent != null) {
                try {
                    Resources clientr = getPackageManager().getResourcesForApplication(
                            si.mRunningService.clientPackage);
                    String label = clientr.getString(si.mRunningService.clientLabel);
                    description.setText(getString(R.string.service_manage_description,
                            label));
                } catch (PackageManager.NameNotFoundException e) {
                }
            } else {
                description.setText(getText(si != null
                        ? R.string.service_stop_description
                        : R.string.heavy_weight_stop_description));
            }
        }
        
        View button = root.findViewById(R.id.right_button);
        button.setOnClickListener(detail);
        ((TextView)button).setText(getText(detail.mManageIntent != null
                ? R.string.service_manage : R.string.service_stop));
        root.findViewById(R.id.left_button).setVisibility(View.INVISIBLE);
        
        mActiveDetails.add(detail);
    }
    
    void addProcessDetailsView(RunningState.ProcessItem pi, boolean isMain) {
        if (mNumProcesses == 0) {
            mProcessesHeader = (TextView)mInflater.inflate(R.layout.separator_label,
                    mAllDetails, false);
            mProcessesHeader.setText(R.string.runningservicedetails_processes_title);
            mAllDetails.addView(mProcessesHeader);
        }
        mNumProcesses++;
        
        ActiveDetail detail = new ActiveDetail();
        View root = mInflater.inflate(R.layout.running_service_details_process,
                mAllDetails, false);
        mAllDetails.addView(root);
        detail.mRootView = root;
        detail.mViewHolder = new RunningProcessesView.ViewHolder(root);
        detail.mActiveItem = detail.mViewHolder.bind(mState, pi, mBuilder);
        
        TextView description = (TextView)root.findViewById(R.id.comp_description);
        if (isMain) {
            description.setText(R.string.main_running_process_description);
        } else {
            int textid = 0;
            CharSequence label = null;
            ActivityManager.RunningAppProcessInfo rpi = pi.mRunningProcessInfo;
            final ComponentName comp = rpi.importanceReasonComponent;
            //Log.i(TAG, "Secondary proc: code=" + rpi.importanceReasonCode
            //        + " pid=" + rpi.importanceReasonPid + " comp=" + comp);
            switch (rpi.importanceReasonCode) {
                case ActivityManager.RunningAppProcessInfo.REASON_PROVIDER_IN_USE:
                    textid = R.string.process_provider_in_use_description;
                    List<ProviderInfo> providers = null;
                    if (comp != null) {
                        providers = getPackageManager()
                                .queryContentProviders(comp.getPackageName(),
                                        rpi.uid, 0);
                    }
                    if (providers != null) {
                        for (int j=0; j<providers.size(); j++) {
                            ProviderInfo prov = providers.get(j);
                            if (comp.getClassName().equals(prov.name)) {
                                label = RunningState.makeLabel(getPackageManager(),
                                        prov.name, prov);
                                break;
                            }
                        }
                    }
                    break;
                case ActivityManager.RunningAppProcessInfo.REASON_SERVICE_IN_USE:
                    textid = R.string.process_service_in_use_description;
                    if (rpi.importanceReasonComponent != null) {
                        try {
                            ServiceInfo serv = getPackageManager().getServiceInfo(
                                    rpi.importanceReasonComponent, 0);
                            label = RunningState.makeLabel(getPackageManager(),
                                    serv.name, serv);
                        } catch (NameNotFoundException e) {
                        }
                    }
                    break;
            }
            if (textid != 0 && label != null) {
                description.setText(getString(textid, label));
            }
        }
        
        mActiveDetails.add(detail);
    }
    
    void addDetailViews() {
        for (int i=mActiveDetails.size()-1; i>=0; i--) {
            mAllDetails.removeView(mActiveDetails.get(i).mRootView);
        }
        mActiveDetails.clear();
        
        if (mServicesHeader != null) {
            mAllDetails.removeView(mServicesHeader);
            mServicesHeader = null;
        }
        
        if (mProcessesHeader != null) {
            mAllDetails.removeView(mProcessesHeader);
            mProcessesHeader = null;
        }
        
        mNumServices = mNumProcesses = 0;
        
        if (mMergedItem != null) {
            for (int i=0; i<mMergedItem.mServices.size(); i++) {
                addServiceDetailsView(mMergedItem.mServices.get(i), mMergedItem);
            }
            
            if (mMergedItem.mServices.size() <= 0) {
                // This item does not have any services, so it must be
                // a heavy-weight process...  we will put a fake service
                // entry for it, to allow the user to "stop" it.
                addServiceDetailsView(null, mMergedItem);
            }
            
            for (int i=-1; i<mMergedItem.mOtherProcesses.size(); i++) {
                RunningState.ProcessItem pi = i < 0 ? mMergedItem.mProcess
                        : mMergedItem.mOtherProcesses.get(i);
                if (pi.mPid <= 0) {
                    continue;
                }
                
                addProcessDetailsView(pi, i < 0);
            }
        }
    }
    
    void refreshUi(boolean dataChanged) {
        if (findMergedItem()) {
            dataChanged = true;
        }
        if (dataChanged) {
            if (mMergedItem != null) {
                mSnippetActiveItem = mSnippetViewHolder.bind(mState,
                        mMergedItem, mBuilder);
            } else if (mSnippetActiveItem != null) {
                // Clear whatever is currently being shown.
                mSnippetActiveItem.mHolder.size.setText("");
                mSnippetActiveItem.mHolder.uptime.setText("");
                mSnippetActiveItem.mHolder.description.setText(R.string.no_services);
            } else {
                // No merged item, never had one.  Nothing to do.
                finish();
                return;
            }
            addDetailViews();
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mUid = getIntent().getIntExtra(KEY_UID, 0);
        mProcessName = getIntent().getStringExtra(KEY_PROCESS);
        
        mAm = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mState = (RunningState)getLastNonConfigurationInstance();
        if (mState == null) {
            mState = new RunningState();
        }
        
        setContentView(R.layout.running_service_details);
        
        mAllDetails = (ViewGroup)findViewById(R.id.all_details);
        mSnippet = (ViewGroup)findViewById(R.id.snippet);
        mSnippet.setBackgroundResource(com.android.internal.R.drawable.title_bar_medium);
        mSnippet.setPadding(0, mSnippet.getPaddingTop(), 0, mSnippet.getPaddingBottom());
        mSnippetViewHolder = new RunningProcessesView.ViewHolder(mSnippet);
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
        mHandler.sendMessageDelayed(msg, RunningProcessesView.TIME_UPDATE_DELAY);
        mBackgroundHandler.removeMessages(MSG_UPDATE_CONTENTS);
        msg = mBackgroundHandler.obtainMessage(MSG_UPDATE_CONTENTS);
        mBackgroundHandler.sendMessageDelayed(msg, RunningProcessesView.CONTENTS_UPDATE_DELAY);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mState;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}

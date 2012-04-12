package com.android.settings.applications;

import com.android.settings.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ApplicationErrorReport;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class RunningServiceDetails extends Fragment
        implements RunningState.OnRefreshUiListener {
    static final String TAG = "RunningServicesDetails";
    
    static final String KEY_UID = "uid";
    static final String KEY_PROCESS = "process";
    static final String KEY_BACKGROUND = "background";
    
    static final int DIALOG_CONFIRM_STOP = 1;
    
    ActivityManager mAm;
    LayoutInflater mInflater;
    
    RunningState mState;
    boolean mHaveData;
    
    int mUid;
    String mProcessName;
    boolean mShowBackground;
    
    RunningState.MergedItem mMergedItem;
    
    View mRootView;
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
        Button mStopButton;
        Button mReportButton;
        RunningState.ServiceItem mServiceItem;
        RunningProcessesView.ActiveItem mActiveItem;
        RunningProcessesView.ViewHolder mViewHolder;
        PendingIntent mManageIntent;
        ComponentName mInstaller;

        void stopActiveService(boolean confirmed) {
            RunningState.ServiceItem si = mServiceItem;
            if (!confirmed) {
                if ((si.mServiceInfo.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM) != 0) {
                    showConfirmStopDialog(si.mRunningService.service);
                    return;
                }
            }
            getActivity().stopService(new Intent().setComponent(si.mRunningService.service));
            if (mMergedItem == null) {
                // If this is gone, we are gone.
                mState.updateNow();
                finish();
            } else if (!mShowBackground && mMergedItem.mServices.size() <= 1) {
                // If there was only one service, we are finishing it,
                // so no reason for the UI to stick around.
                mState.updateNow();
                finish();
            } else {
                mState.updateNow();
            }
        }
        
        public void onClick(View v) {
            if (v == mReportButton) {
                ApplicationErrorReport report = new ApplicationErrorReport();
                report.type = ApplicationErrorReport.TYPE_RUNNING_SERVICE;
                report.packageName = mServiceItem.mServiceInfo.packageName;
                report.installerPackageName = mInstaller.getPackageName();
                report.processName = mServiceItem.mRunningService.process;
                report.time = System.currentTimeMillis();
                report.systemApp = (mServiceItem.mServiceInfo.applicationInfo.flags
                        & ApplicationInfo.FLAG_SYSTEM) != 0;
                ApplicationErrorReport.RunningServiceInfo info
                        = new ApplicationErrorReport.RunningServiceInfo();
                if (mActiveItem.mFirstRunTime >= 0) {
                    info.durationMillis = SystemClock.elapsedRealtime()-mActiveItem.mFirstRunTime;
                } else {
                    info.durationMillis = -1;
                }
                ComponentName comp = new ComponentName(mServiceItem.mServiceInfo.packageName,
                        mServiceItem.mServiceInfo.name);
                File filename = getActivity().getFileStreamPath("service_dump.txt");
                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(filename);
                    Debug.dumpService("activity", output.getFD(),
                            new String[] { "-a", "service", comp.flattenToString() });
                } catch (IOException e) {
                    Log.w(TAG, "Can't dump service: " + comp, e);
                } finally {
                    if (output != null) try { output.close(); } catch (IOException e) {}
                }
                FileInputStream input = null;
                try {
                    input = new FileInputStream(filename);
                    byte[] buffer = new byte[(int) filename.length()];
                    input.read(buffer);
                    info.serviceDetails = new String(buffer);
                } catch (IOException e) {
                    Log.w(TAG, "Can't read service dump: " + comp, e);
                } finally {
                    if (input != null) try { input.close(); } catch (IOException e) {}
                }
                filename.delete();
                Log.i(TAG, "Details: " + info.serviceDetails);
                report.runningServiceInfo = info;
                Intent result = new Intent(Intent.ACTION_APP_ERROR);
                result.setComponent(mInstaller);
                result.putExtra(Intent.EXTRA_BUG_REPORT, report);
                result.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(result);
                return;
            }

            if (mManageIntent != null) {
                try {
                    getActivity().startIntentSender(mManageIntent.getIntentSender(), null,
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
            } else if (mServiceItem != null) {
                stopActiveService(false);
            } else if (mActiveItem.mItem.mBackground) {
                // Background process.  Just kill it.
                mAm.killBackgroundProcesses(mActiveItem.mItem.mPackageInfo.packageName);
                finish();
            } else {
                // Heavy-weight process.  We'll do a force-stop on it.
                mAm.forceStopPackage(mActiveItem.mItem.mPackageInfo.packageName);
                finish();
            }
        }
    }
    
    StringBuilder mBuilder = new StringBuilder(128);
    
    boolean findMergedItem() {
        RunningState.MergedItem item = null;
        ArrayList<RunningState.MergedItem> newItems = mShowBackground
                ? mState.getCurrentBackgroundItems() : mState.getCurrentMergedItems();
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
        detail.mServiceItem = si;
        detail.mViewHolder = new RunningProcessesView.ViewHolder(root);
        detail.mActiveItem = detail.mViewHolder.bind(mState, bi, mBuilder);
        
        if (si != null && si.mRunningService.clientLabel != 0) {
            detail.mManageIntent = mAm.getRunningServiceControlPanel(
                    si.mRunningService.service);
        }
        
        TextView description = (TextView)root.findViewById(R.id.comp_description);
        if (si != null && si.mServiceInfo.descriptionRes != 0) {
            description.setText(getActivity().getPackageManager().getText(
                    si.mServiceInfo.packageName, si.mServiceInfo.descriptionRes,
                    si.mServiceInfo.applicationInfo));
        } else {
            if (mi.mBackground) {
                description.setText(R.string.background_process_stop_description);
            } else if (detail.mManageIntent != null) {
                try {
                    Resources clientr = getActivity().getPackageManager().getResourcesForApplication(
                            si.mRunningService.clientPackage);
                    String label = clientr.getString(si.mRunningService.clientLabel);
                    description.setText(getActivity().getString(R.string.service_manage_description,
                            label));
                } catch (PackageManager.NameNotFoundException e) {
                }
            } else {
                description.setText(getActivity().getText(si != null
                        ? R.string.service_stop_description
                        : R.string.heavy_weight_stop_description));
            }
        }
        
        detail.mStopButton = (Button)root.findViewById(R.id.left_button);
        detail.mStopButton.setOnClickListener(detail);
        detail.mStopButton.setText(getActivity().getText(detail.mManageIntent != null
                ? R.string.service_manage : R.string.service_stop));

        detail.mReportButton = (Button)root.findViewById(R.id.right_button);
        detail.mReportButton.setOnClickListener(detail);
        detail.mReportButton.setText(com.android.internal.R.string.report);
        // check if error reporting is enabled in secure settings
        int enabled = Settings.Secure.getInt(getActivity().getContentResolver(),
                Settings.Secure.SEND_ACTION_APP_ERROR, 0);
        if (enabled != 0 && si != null) {
            detail.mInstaller = ApplicationErrorReport.getErrorReportReceiver(
                    getActivity(), si.mServiceInfo.packageName,
                    si.mServiceInfo.applicationInfo.flags);
            detail.mReportButton.setEnabled(detail.mInstaller != null);
        } else {
            detail.mReportButton.setEnabled(false);
        }
        
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
                    if (rpi.importanceReasonComponent != null) {
                        try {
                            ProviderInfo prov = getActivity().getPackageManager().getProviderInfo(
                                    rpi.importanceReasonComponent, 0);
                            label = RunningState.makeLabel(getActivity().getPackageManager(),
                                    prov.name, prov);
                        } catch (NameNotFoundException e) {
                        }
                    }
                    break;
                case ActivityManager.RunningAppProcessInfo.REASON_SERVICE_IN_USE:
                    textid = R.string.process_service_in_use_description;
                    if (rpi.importanceReasonComponent != null) {
                        try {
                            ServiceInfo serv = getActivity().getPackageManager().getServiceInfo(
                                    rpi.importanceReasonComponent, 0);
                            label = RunningState.makeLabel(getActivity().getPackageManager(),
                                    serv.name, serv);
                        } catch (NameNotFoundException e) {
                        }
                    }
                    break;
            }
            if (textid != 0 && label != null) {
                description.setText(getActivity().getString(textid, label));
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
                // another interesting process...  we will put a fake service
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
    
    private void finish() {
        (new Handler()).post(new Runnable() {
            @Override
            public void run() {
                Activity a = getActivity();
                if (a != null) {
                    a.onBackPressed();
                }
            }
        });
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mUid = getArguments().getInt(KEY_UID, 0);
        mProcessName = getArguments().getString(KEY_PROCESS);
        mShowBackground = getArguments().getBoolean(KEY_BACKGROUND, false);
        
        mAm = (ActivityManager)getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        mState = RunningState.getInstance(getActivity());
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = mRootView = inflater.inflate(R.layout.running_service_details, null);
        
        mAllDetails = (ViewGroup)view.findViewById(R.id.all_details);
        mSnippet = (ViewGroup)view.findViewById(R.id.snippet);
        mSnippet.setPadding(0, mSnippet.getPaddingTop(), 0, mSnippet.getPaddingBottom());
        mSnippetViewHolder = new RunningProcessesView.ViewHolder(mSnippet);
        
        // We want to retrieve the data right now, so any active managed
        // dialog that gets created can find it.
        ensureData();
        
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        mHaveData = false;
        mState.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        ensureData();
    }

    ActiveDetail activeDetailForService(ComponentName comp) {
        for (int i=0; i<mActiveDetails.size(); i++) {
            ActiveDetail ad = mActiveDetails.get(i);
            if (ad.mServiceItem != null && ad.mServiceItem.mRunningService != null
                    && comp.equals(ad.mServiceItem.mRunningService.service)) {
                return ad;
            }
        }
        return null;
    }
    
    private void showConfirmStopDialog(ComponentName comp) {
        DialogFragment newFragment = MyAlertDialogFragment.newConfirmStop(
                DIALOG_CONFIRM_STOP, comp);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "confirmstop");
    }
    
    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newConfirmStop(int id, ComponentName comp) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putParcelable("comp", comp);
            frag.setArguments(args);
            return frag;
        }

        RunningServiceDetails getOwner() {
            return (RunningServiceDetails)getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DIALOG_CONFIRM_STOP: {
                    final ComponentName comp = (ComponentName)getArguments().getParcelable("comp");
                    if (getOwner().activeDetailForService(comp) == null) {
                        return null;
                    }
                    
                    return new AlertDialog.Builder(getActivity())
                            .setTitle(getActivity().getString(R.string.runningservicedetails_stop_dlg_title))
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage(getActivity().getString(R.string.runningservicedetails_stop_dlg_text))
                            .setPositiveButton(R.string.dlg_ok,
                                    new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    ActiveDetail ad = getOwner().activeDetailForService(comp);
                                    if (ad != null) {
                                        ad.stopActiveService(true);
                                    }
                                }
                            })
                            .setNegativeButton(R.string.dlg_cancel, null)
                            .create();
                }
            }
            throw new IllegalArgumentException("unknown id " + id);
        }
    }

    void ensureData() {
        if (!mHaveData) {
            mHaveData = true;
            mState.resume(this);

            // We want to go away if the service being shown no longer exists,
            // so we need to ensure we have done the initial data retrieval before
            // showing our ui.
            mState.waitForData();

            // And since we know we have the data, let's show the UI right away
            // to avoid flicker.
            refreshUi(true);
        }
    }
    
    void updateTimes() {
        if (mSnippetActiveItem != null) {
            mSnippetActiveItem.updateTime(getActivity(), mBuilder);
        }
        for (int i=0; i<mActiveDetails.size(); i++) {
            mActiveDetails.get(i).mActiveItem.updateTime(getActivity(), mBuilder);
        }
    }

    @Override
    public void onRefreshUi(int what) {
        switch (what) {
            case REFRESH_TIME:
                updateTimes();
                break;
            case REFRESH_DATA:
                refreshUi(false);
                updateTimes();
                break;
            case REFRESH_STRUCTURE:
                refreshUi(true);
                updateTimes();
                break;
        }
    }
}

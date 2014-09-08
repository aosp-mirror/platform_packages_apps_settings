/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.android.settings.Utils.prepareCustomPreferencesList;

public class ProcessStatsDetail extends Fragment implements Button.OnClickListener {
    private static final String TAG = "ProcessStatsDetail";

    public static final int ACTION_FORCE_STOP = 1;

    public static final String EXTRA_ENTRY = "entry";
    public static final String EXTRA_USE_USS = "use_uss";
    public static final String EXTRA_MAX_WEIGHT = "max_weight";
    public static final String EXTRA_TOTAL_TIME = "total_time";

    private PackageManager mPm;
    private DevicePolicyManager mDpm;

    private ProcStatsEntry mEntry;
    private boolean mUseUss;
    private long mMaxWeight;
    private long mTotalTime;

    private View mRootView;
    private TextView mTitleView;
    private ViewGroup mTwoButtonsPanel;
    private Button mForceStopButton;
    private Button mReportButton;
    private ViewGroup mDetailsParent;
    private ViewGroup mServicesParent;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPm = getActivity().getPackageManager();
        mDpm = (DevicePolicyManager)getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        final Bundle args = getArguments();
        mEntry = (ProcStatsEntry)args.getParcelable(EXTRA_ENTRY);
        mEntry.retrieveUiData(mPm);
        mUseUss = args.getBoolean(EXTRA_USE_USS);
        mMaxWeight = args.getLong(EXTRA_MAX_WEIGHT);
        mTotalTime = args.getLong(EXTRA_TOTAL_TIME);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.process_stats_details, container, false);
        prepareCustomPreferencesList(container, view, view, false);

        mRootView = view;
        createDetails();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkForceStop();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void createDetails() {
        final double percentOfWeight = (((double)mEntry.mWeight) / mMaxWeight) * 100;

        int appLevel = (int) Math.ceil(percentOfWeight);
        String appLevelText = Utils.formatPercentage(mEntry.mDuration, mTotalTime);

        // Set all values in the header.
        final TextView summary = (TextView) mRootView.findViewById(android.R.id.summary);
        summary.setText(mEntry.mName);
        summary.setVisibility(View.VISIBLE);
        mTitleView = (TextView) mRootView.findViewById(android.R.id.title);
        mTitleView.setText(mEntry.mUiBaseLabel);
        final TextView text1 = (TextView)mRootView.findViewById(android.R.id.text1);
        text1.setText(appLevelText);
        final ProgressBar progress = (ProgressBar) mRootView.findViewById(android.R.id.progress);
        progress.setProgress(appLevel);
        final ImageView icon = (ImageView) mRootView.findViewById(android.R.id.icon);
        if (mEntry.mUiTargetApp != null) {
            icon.setImageDrawable(mEntry.mUiTargetApp.loadIcon(mPm));
        }

        mTwoButtonsPanel = (ViewGroup)mRootView.findViewById(R.id.two_buttons_panel);
        mForceStopButton = (Button)mRootView.findViewById(R.id.right_button);
        mReportButton = (Button)mRootView.findViewById(R.id.left_button);
        mForceStopButton.setEnabled(false);
        mReportButton.setVisibility(View.INVISIBLE);

        mDetailsParent = (ViewGroup)mRootView.findViewById(R.id.details);
        mServicesParent = (ViewGroup)mRootView.findViewById(R.id.services);

        fillDetailsSection();
        fillServicesSection();

        if (mEntry.mUid >= android.os.Process.FIRST_APPLICATION_UID) {
            mForceStopButton.setText(R.string.force_stop);
            mForceStopButton.setTag(ACTION_FORCE_STOP);
            mForceStopButton.setOnClickListener(this);
            mTwoButtonsPanel.setVisibility(View.VISIBLE);
        } else {
            mTwoButtonsPanel.setVisibility(View.GONE);
        }
    }

    public void onClick(View v) {
        doAction((Integer) v.getTag());
    }

    private void doAction(int action) {
        switch (action) {
            case ACTION_FORCE_STOP:
                killProcesses();
                break;
        }
    }

    private void addPackageHeaderItem(ViewGroup parent, String packageName) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup item = (ViewGroup) inflater.inflate(R.layout.running_processes_item,
                null);
        parent.addView(item);
        final ImageView icon = (ImageView) item.findViewById(R.id.icon);
        TextView nameView = (TextView) item.findViewById(R.id.name);
        TextView descriptionView = (TextView) item.findViewById(R.id.description);
        try {
            ApplicationInfo ai = mPm.getApplicationInfo(packageName, 0);
            icon.setImageDrawable(ai.loadIcon(mPm));
            nameView.setText(ai.loadLabel(mPm));
        } catch (PackageManager.NameNotFoundException e) {
        }
        descriptionView.setText(packageName);
    }

    private void addDetailsItem(ViewGroup parent, CharSequence label, CharSequence value) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup item = (ViewGroup) inflater.inflate(R.layout.power_usage_detail_item_text,
                null);
        parent.addView(item);
        TextView labelView = (TextView) item.findViewById(R.id.label);
        TextView valueView = (TextView) item.findViewById(R.id.value);
        labelView.setText(label);
        valueView.setText(value);
    }

    private void fillDetailsSection() {
        addDetailsItem(mDetailsParent, getResources().getText(R.string.process_stats_avg_ram_use),
                Formatter.formatShortFileSize(getActivity(),
                        (mUseUss ? mEntry.mAvgUss : mEntry.mAvgPss) * 1024));
        addDetailsItem(mDetailsParent, getResources().getText(R.string.process_stats_max_ram_use),
                Formatter.formatShortFileSize(getActivity(),
                        (mUseUss ? mEntry.mMaxUss : mEntry.mMaxPss) * 1024));
        addDetailsItem(mDetailsParent, getResources().getText(R.string.process_stats_run_time),
                Utils.formatPercentage(mEntry.mDuration, mTotalTime));
    }

    final static Comparator<ProcStatsEntry.Service> sServiceCompare
            = new Comparator<ProcStatsEntry.Service>() {
        @Override
        public int compare(ProcStatsEntry.Service lhs, ProcStatsEntry.Service rhs) {
            if (lhs.mDuration < rhs.mDuration) {
                return 1;
            } else if (lhs.mDuration > rhs.mDuration) {
                return -1;
            }
            return 0;
        }
    };

    final static Comparator<ArrayList<ProcStatsEntry.Service>> sServicePkgCompare
            = new Comparator<ArrayList<ProcStatsEntry.Service>>() {
        @Override
        public int compare(ArrayList<ProcStatsEntry.Service> lhs,
                ArrayList<ProcStatsEntry.Service> rhs) {
            long topLhs = lhs.size() > 0 ? lhs.get(0).mDuration : 0;
            long topRhs = rhs.size() > 0 ? rhs.get(0).mDuration : 0;
            if (topLhs < topRhs) {
                return 1;
            } else if (topLhs > topRhs) {
                return -1;
            }
            return 0;
        }
    };

    private void fillServicesSection() {
        if (mEntry.mServices.size() > 0) {
            boolean addPackageSections = false;
            // Sort it all.
            ArrayList<ArrayList<ProcStatsEntry.Service>> servicePkgs
                    = new ArrayList<ArrayList<ProcStatsEntry.Service>>();
            for (int ip=0; ip<mEntry.mServices.size(); ip++) {
                ArrayList<ProcStatsEntry.Service> services =
                        (ArrayList<ProcStatsEntry.Service>)mEntry.mServices.valueAt(ip).clone();
                Collections.sort(services, sServiceCompare);
                servicePkgs.add(services);
            }
            if (mEntry.mServices.size() > 1
                    || !mEntry.mServices.valueAt(0).get(0).mPackage.equals(mEntry.mPackage)) {
                addPackageSections = true;
                // Sort these so that the one(s) with the longest run durations are on top.
                Collections.sort(servicePkgs, sServicePkgCompare);
            }
            for (int ip=0; ip<servicePkgs.size(); ip++) {
                ArrayList<ProcStatsEntry.Service> services = servicePkgs.get(ip);
                if (addPackageSections) {
                    addPackageHeaderItem(mServicesParent, services.get(0).mPackage);
                }
                for (int is=0; is<services.size(); is++) {
                    ProcStatsEntry.Service service = services.get(is);
                    String label = service.mName;
                    int tail = label.lastIndexOf('.');
                    if (tail >= 0 && tail < (label.length()-1)) {
                        label = label.substring(tail+1);
                    }
                    String percentage = Utils.formatPercentage(service.mDuration, mTotalTime);
                    addDetailsItem(mServicesParent, label, percentage);
                }
            }
        }
    }

    private void killProcesses() {
        ActivityManager am = (ActivityManager)getActivity().getSystemService(
                Context.ACTIVITY_SERVICE);
        am.forceStopPackage(mEntry.mUiPackage);
        checkForceStop();
    }

    private final BroadcastReceiver mCheckKillProcessesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mForceStopButton.setEnabled(getResultCode() != Activity.RESULT_CANCELED);
        }
    };

    private void checkForceStop() {
        if (mEntry.mUiPackage == null || mEntry.mUid < Process.FIRST_APPLICATION_UID) {
            mForceStopButton.setEnabled(false);
            return;
        }
        if (mDpm.packageHasActiveAdmins(mEntry.mUiPackage)) {
            mForceStopButton.setEnabled(false);
            return;
        }
        try {
            ApplicationInfo info = mPm.getApplicationInfo(mEntry.mUiPackage, 0);
            if ((info.flags&ApplicationInfo.FLAG_STOPPED) == 0) {
                mForceStopButton.setEnabled(true);
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                Uri.fromParts("package", mEntry.mUiPackage, null));
        intent.putExtra(Intent.EXTRA_PACKAGES, new String[] { mEntry.mUiPackage });
        intent.putExtra(Intent.EXTRA_UID, mEntry.mUid);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, UserHandle.getUserId(mEntry.mUid));
        getActivity().sendOrderedBroadcast(intent, null, mCheckKillProcessesReceiver, null,
                Activity.RESULT_CANCELED, null, null);
    }
}
